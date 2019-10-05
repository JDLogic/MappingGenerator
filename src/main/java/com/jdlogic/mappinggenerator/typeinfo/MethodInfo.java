package com.jdlogic.mappinggenerator.typeinfo;

import com.jdlogic.mappinggenerator.Utils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.objectweb.asm.Opcodes.*;

public class MethodInfo implements IAccessInfo
{
    private final ClassInfo parent;
    private final String name;
    private final String desc;
    private final int access;
    private final boolean isLambda;
    private final boolean isAccessCheck;
    private final String bounceKey;
    private MethodInfo bouncer = null;
    private MethodInfo override = null;

    public MethodInfo(ClassInfo parent, MethodNode node)
    {
        this.parent = parent;
        this.name = node.name;
        this.desc = node.desc;
        this.access = node.access;
        this.isLambda = hasModifier(ACC_SYNTHETIC) && hasModifier(ACC_PRIVATE);

        boolean isAccessCheck = false;
        String bounceKey = null;

        // TODO ask lex for permission to use the following
        // taken from extract inheritance task here:
        // https://github.com/MinecraftForge/MCPConfig/blob/master/buildSrc/src/main/java/net/minecraftforge/lex/ExtractInheritance.java
        if (!isLambda && hasModifier(ACC_SYNTHETIC | ACC_BRIDGE))
        {
            if (!hasModifier(ACC_STATIC))
            {
                AbstractInsnNode start = node.instructions.getFirst();
                while (start instanceof LabelNode || start instanceof LineNumberNode)
                {
                    start = start.getNext();
                }

                if (start instanceof VarInsnNode)
                {
                    VarInsnNode n = (VarInsnNode)start;
                    if (n.var == 0 && (hasModifier(ACC_STATIC) || n.getOpcode() == ALOAD))
                    {
                        AbstractInsnNode end = node.instructions.getLast();
                        if (end instanceof LabelNode)
                            end = end.getPrevious();

                        if (end.getOpcode() >= IRETURN && end.getOpcode() <= RETURN)
                            end = end.getPrevious();

                        if (end instanceof MethodInsnNode)
                        {
                            while (start != end)
                            {
                                if (!(start instanceof VarInsnNode) && start.getOpcode() != INSTANCEOF && start.getOpcode() != CHECKCAST)
                                {
                                    end = null;
                                    break;
                                }
                                start = start.getNext();
                            }

                            MethodInsnNode mtd = (MethodInsnNode)end;
                            if (end != null && mtd.owner.equals(parent.getName()) &&
                                    Type.getArgumentsAndReturnSizes(node.desc) == Type.getArgumentsAndReturnSizes(mtd.desc))
                            {
                                bounceKey = mtd.name + " " + mtd.desc;
                            }
                        }
                    }
                }
            }
            else
            {
                AbstractInsnNode end = node.instructions.getLast();
                if (end instanceof LabelNode)
                    end = end.getPrevious();

                if (end.getOpcode() >= IRETURN && end.getOpcode() <= RETURN)
                {
                    end = end.getPrevious();
                    if (end instanceof FieldInsnNode || end instanceof MethodInsnNode)
                        isAccessCheck = true;
                }
            }
        }

        this.isAccessCheck = isAccessCheck;
        this.bounceKey = bounceKey;
    }

    public void initBouncer()
    {
        if (bounceKey != null)
            parent.getMethod(bounceKey).bouncer = this;
    }

    public void initOverride(Map<String, ClassInfo> classes, List<Set<MethodInfo>> multiOverrides)
    {
        if (Utils.isConstructor(name) || isLambda)
            return;

        String key = getKey();
        Set<MethodInfo> overrides = getOverrides(classes.get(parent.getSuperCls()), key, classes);
        for (String intf : parent.getInterfaces())
        {
            overrides.addAll(getOverrides(classes.get(intf), key, classes));
        }

        if (!overrides.isEmpty())
        {
            if (this.override == null)
                this.override = overrides.iterator().next();
            else
            {
                overrides.add(this.override);
            }

            if (overrides.size() > 1)
            {
                List<Set<MethodInfo>> toUpdate = multiOverrides.stream()
                        .filter(s -> overrides.stream().anyMatch(s::contains))
                        .collect(Collectors.toList());
                if (toUpdate.isEmpty())
                    multiOverrides.add(overrides);
                else
                    toUpdate.forEach(s -> s.addAll(overrides)); //TODO check for multiple sets and merger?
            }
        }
    }

    public ClassInfo getParent()
    {
        return parent;
    }

    public String getName()
    {
        return name;
    }

    public String getDesc()
    {
        return desc;
    }

    @Override
    public int getAccess()
    {
        return access;
    }

    public boolean isLambda()
    {
        return isLambda;
    }

    public boolean isAccessCheck()
    {
        return isAccessCheck;
    }

    public String getBounceKey()
    {
        return bounceKey;
    }

    public MethodInfo getBouncer()
    {
        return bouncer;
    }

    public MethodInfo getOverride()
    {
        return override;
    }

    void setOverride(MethodInfo override)
    {
        this.override = override;
    }

    public String getKey()
    {
        return name + " " + desc;
    }

    private static Set<MethodInfo> getOverrides(ClassInfo cls, String key, Map<String, ClassInfo> classes)
    {
        Set<MethodInfo> ret = new HashSet<>();
        if (cls != null)
        {
            ret.addAll(getOverrides(classes.get(cls.getSuperCls()), key, classes));
            for (String intf : cls.getInterfaces())
            {
                ret.addAll(getOverrides(classes.get(intf), key, classes));
            }

            if(ret.isEmpty())
            {
                MethodInfo info = cls.getMethod(key);
                if (info != null && !info.hasModifier(ACC_PRIVATE | ACC_FINAL | ACC_STATIC))
                {
                    ret.add(info);
                }
            }
        }
        return ret;
    }

    @Override
    public String toString()
    {
        return parent.getName() + "." + name + " " + desc;
    }
}
