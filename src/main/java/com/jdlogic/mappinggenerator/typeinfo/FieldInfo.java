package com.jdlogic.mappinggenerator.typeinfo;

import org.objectweb.asm.tree.FieldNode;

public class FieldInfo implements IAccessInfo
{
    private final ClassInfo parent;
    private final String name;
    private final String desc;
    private final int access;

    public FieldInfo(ClassInfo parent, FieldNode node)
    {
        this.parent = parent;
        this.name = node.name;
        this.desc = node.desc;
        this.access = node.access;
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

    @Override
    public String toString()
    {
        return parent.getName() + "." + name;
    }
}
