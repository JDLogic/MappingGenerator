package com.jdlogic.mappinggenerator.typeinfo;

import com.jdlogic.mappinggenerator.Constants;
import org.objectweb.asm.tree.ClassNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.objectweb.asm.Opcodes.*;

public class ClassInfo implements IAccessInfo
{
    private final String name;
    private final String superCls;
    private final List<String> interfaces;
    private final int access;
    private final boolean isAnon;
    private final List<FieldInfo> fields;
    private final Map<String, MethodInfo> methods;

    public ClassInfo(ClassNode node)
    {
        this.name = node.name;
        this.superCls = node.superName;
        this.interfaces = node.interfaces != null ? node.interfaces : Collections.emptyList();
        this.access = node.access;
        this.isAnon = (node.outerClass != null);
        this.fields = node.fields.stream().map(f -> new FieldInfo(this, f)).collect(Collectors.toList());

        this.methods = node.methods.stream()
                .map(n -> new MethodInfo(this, n))
                .collect(Collectors.toMap(MethodInfo::getKey, Function.identity()));

        this.methods.values().forEach(MethodInfo::initBouncer);
    }

    public String getName()
    {
        return name;
    }

    public String getSuperCls()
    {
        return superCls;
    }

    public List<String> getInterfaces()
    {
        return interfaces;
    }

    @Override
    public int getAccess()
    {
        return access;
    }

    public boolean isAnon()
    {
        return isAnon;
    }

    public boolean isEnum()
    {
        return superCls.equals(Constants.CLS_ENUM);
    }

    public Collection<FieldInfo> getFields()
    {
        return fields;
    }

    public Collection<MethodInfo> getMethods()
    {
        return methods.values();
    }

    public MethodInfo getMethod(String name, String desc)
    {
        return getMethod(name + " " + desc);
    }

    public MethodInfo getMethod(String key)
    {
        return methods.get(key);
    }

    public void initOverrides(Map<String, ClassInfo> classes, List<Set<MethodInfo>> multiOverrides)
    {
        this.methods.values().forEach(m -> m.initOverride(classes, multiOverrides));
        if (!hasModifier(ACC_INTERFACE))
            processInterfaceOverrides(classes, multiOverrides);
    }

    /*
    This checks for
     */
    private void processInterfaceOverrides(Map<String, ClassInfo> classes, List<Set<MethodInfo>> multiOverrides)
    {
        Map<String, MethodInfo> interfaceMethods = getAllInterfaceMethods(classes);
        methods.keySet().forEach(interfaceMethods::remove);

        String scls = superCls;

        while (!interfaceMethods.isEmpty() && scls != null)
        {
            ClassInfo cls = classes.get(scls);
            cls.methods.values().forEach(m ->
            {
                MethodInfo intfMethod = interfaceMethods.remove(m.getKey());
                if (intfMethod != null)
                {
                    if (m.getOverride() == null)
                    {
                        m.setOverride(intfMethod);
                    }
                    else
                    {
                        List<Set<MethodInfo>> toUpdate = multiOverrides.stream()
                                .filter(s -> s.contains(intfMethod))
                                .collect(Collectors.toList());
                        if (toUpdate.isEmpty())
                            multiOverrides.add(new HashSet<>(Arrays.asList(intfMethod, m)));
                        else
                            toUpdate.forEach(s -> s.add(m)); //TODO check for multiple sets and merger?
                    }
                }
            });
            scls = cls.superCls;
        }
    }

    private Map<String, MethodInfo> getAllInterfaceMethods(Map<String, ClassInfo> classes)
    {
        LinkedList<ClassInfo> queue = new LinkedList<>();
        this.interfaces.stream().map(classes::get).forEach(queue::add);
        Map<String, MethodInfo> ret = new HashMap<>();

        while (!queue.isEmpty())
        {
            ClassInfo cls = queue.removeFirst();
            ret.putAll(cls.methods);

            cls.interfaces.stream().map(classes::get).forEach(queue::addLast);
        }

        return ret;
    }
}
