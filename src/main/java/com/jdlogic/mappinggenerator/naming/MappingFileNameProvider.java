package com.jdlogic.mappinggenerator.naming;

import com.jdlogic.mappinggenerator.Utils;
import net.minecraftforge.srgutils.IMappingFile;
import java.util.Optional;
import java.util.function.Function;

public class MappingFileNameProvider implements INameProvider
{
    private final IMappingFile oldMappingFile;

    public MappingFileNameProvider(IMappingFile oldMappingFile)
    {
        this.oldMappingFile = oldMappingFile;
    }

    public Optional<String> getClassName(String clsName)
    {
        return Optional.ofNullable(oldMappingFile.getClass(clsName))
                                .map(IMappingFile.IClass::getMapped);
    }

    public Optional<String> getFieldName(String clsName, String fieldName)
    {
        return Utils.streamOfNullable(oldMappingFile.getClass(clsName))
                .map(cls -> cls.getFields().stream())
                .flatMap(Function.identity())
                .filter(f -> fieldName.equals(f.getOriginal()))
                .map(IMappingFile.IField::getMapped)
                .findFirst();
    }

    public Optional<String> getMethodName(String clsName, String obfName, String desc)
    {
        return Utils.streamOfNullable(oldMappingFile.getClass(clsName))
                            .map(cls -> cls.getMethods().stream())
                            .flatMap(Function.identity())
                            .filter(m -> obfName.equals(m.getOriginal()))
                            .filter(m -> desc.equals(m.getDescriptor()))
                            .map(IMappingFile.IMethod::getMapped)
                            .findFirst();
    }
}
