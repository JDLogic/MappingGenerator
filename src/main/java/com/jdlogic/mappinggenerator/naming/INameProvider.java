package com.jdlogic.mappinggenerator.naming;

import java.util.Optional;

public interface INameProvider
{
    Optional<String> getClassName(String clsName);

    Optional<String> getFieldName(String clsName, String fieldName);

    Optional<String> getMethodName(String clsName, String obfName, String desc);
}
