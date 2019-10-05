package com.jdlogic.mappinggenerator.typeinfo;

public interface IAccessInfo
{
    int getAccess();

    default boolean hasModifier(int mask)
    {
        return (getAccess() & mask) != 0;
    }
}
