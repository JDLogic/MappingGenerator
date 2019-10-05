package com.jdlogic.mappinggenerator;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GeneratorConfig
{
    private boolean generateClassNames = true;
    private boolean generateFieldNames = true;
    private boolean generateMethodNames = true;
    private boolean generateEnumNames = true;
    private boolean generateLambdaNames = true;
    private boolean generateAccessCheckNames = true;

    private String defaultPackage = "net/newclasses/";
    private Map<String, String> nameFormatMap = new HashMap<>();
    private Map<String, Integer> idMap = new HashMap<>();

    public void load(Path cfg) throws IOException
    {
        try (FileConfig config = FileConfig.of(cfg))
        {
            config.load();

            generateClassNames = config.getOrElse("generateClassNames", generateClassNames);
            generateFieldNames = config.getOrElse("generateFieldNames", generateFieldNames);
            generateMethodNames = config.getOrElse("generateMethodNames", generateMethodNames);
            generateEnumNames = config.getOrElse("generateEnumNames", generateEnumNames);
            generateLambdaNames = config.getOrElse("generateLambdaNames", generateLambdaNames);
            generateAccessCheckNames = config.getOrElse("generateAccessCheckNames", generateAccessCheckNames);

            defaultPackage = config.getOrElse("nameformat.defaultpackage", defaultPackage);
            if (!defaultPackage.isEmpty() && !defaultPackage.endsWith("/"))
                defaultPackage += "/";
            String classFormat = config.getOrElse("nameformat.class", "C_{classid}_{obfname}");
            nameFormatMap.put("class", classFormat);
            nameFormatMap.put("innerclass", config.getOrElse("nameformat.innerclass", classFormat));
            nameFormatMap.put("field", config.getOrElse("nameformat.field", "field_{fmid}_{obfname}"));
            nameFormatMap.put("enum", config.getOrElse("nameformat.enum", "ENUM_{enumid}_{obfname}"));
            String methodFormat = config.getOrElse("nameformat.method", "func_{fmid}_{obfname}");
            nameFormatMap.put("method", methodFormat);
            nameFormatMap.put("lambda", config.getOrElse("nameformat.lambda", methodFormat));
            nameFormatMap.put("access", config.getOrElse("nameformat.access", methodFormat));

            config.<UnmodifiableConfig>getOptional("ids").ifPresent(c ->
            {
                c.valueMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() instanceof Integer)
                        .forEach(e -> idMap.put(e.getKey(), (Integer)e.getValue()));
            });
        }
    }

    public boolean generateClassNames()
    {
        return generateClassNames;
    }

    public boolean generateFieldNames()
    {
        return generateFieldNames;
    }

    public boolean generateMethodNames()
    {
        return generateMethodNames;
    }

    public boolean generateEnumNames()
    {
        return generateEnumNames;
    }

    public boolean generateLambdaNames()
    {
        return generateLambdaNames;
    }

    public boolean generateAccessCheckNames()
    {
        return generateAccessCheckNames;
    }

    public String getDefaultPackage()
    {
        return defaultPackage;
    }

    public Map<String, String> getNameFormatMap()
    {
        return nameFormatMap;
    }

    public Map<String, Integer> getIdMap()
    {
        return idMap;
    }
}
