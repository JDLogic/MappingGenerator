package com.jdlogic.mappinggenerator.naming;

public interface INameGenerator
{
    String generateClass(String oldName);

    String generateInnerClass(String oldName, String newOuter, String oldInner);

    String generateField(String cls, String oldName);

    String generateEnum(String cls, String oldName);

    String generateMethod(String cls, String oldName, String desc);

    String generateLambda(String cls, String oldName, String desc);

    String generateAccessCheck(String cls, String oldName, String desc);

    void processExistingClass(String clsName);

    void processExistingField(String fdName);

    void processExistingMethod(String mdName);
}
