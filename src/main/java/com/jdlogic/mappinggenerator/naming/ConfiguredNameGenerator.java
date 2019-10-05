package com.jdlogic.mappinggenerator.naming;

import com.jdlogic.mappinggenerator.GeneratorConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfiguredNameGenerator implements INameGenerator
{
    private final Pattern replacementPattern = Pattern.compile("\\{(\\w+)}");

    private final GeneratorConfig config;
    private final String defaultPackage;
    private final Map<String, String> formatMap;
    private final Map<String, List<String>> keylistMap = new HashMap<>();
    private final Map<String, Pattern> regexMap;
    private final Map<String, Integer> idMap;

    public ConfiguredNameGenerator(GeneratorConfig config)
    {
        this.config = config;
        this.defaultPackage = config.getDefaultPackage();
        this.formatMap = config.getNameFormatMap();
        this.regexMap = createRegexMap();
        this.idMap = config.getIdMap();
    }

    @Override
    public String generateClass(String oldName)
    {
        return config.generateClassNames() ? defaultPackage + formatName("class", oldName) : oldName;
    }

    @Override
    public String generateInnerClass(String oldName, String newOuter, String oldInner)
    {
        return config.generateClassNames() ? newOuter + "$" + formatName("innerclass", oldInner) : oldName;
    }

    @Override
    public String generateField(String cls, String oldName)
    {
        return config.generateFieldNames() ? formatName("field", oldName) : oldName;
    }

    @Override
    public String generateEnum(String cls, String oldName)
    {
        return config.generateEnumNames() ? formatName("enum", oldName) : oldName;
    }

    @Override
    public String generateMethod(String cls, String oldName, String desc)
    {
        return config.generateMethodNames() ? formatName("method", oldName) : oldName;
    }

    @Override
    public String generateLambda(String cls, String oldName, String desc)
    {
        return config.generateLambdaNames() ? formatName("lambda", oldName) : oldName;
    }

    @Override
    public String generateAccessCheck(String cls, String oldName, String desc)
    {
        return config.generateAccessCheckNames() ? formatName("access", oldName) : oldName;
    }

    @Override
    public void processExistingClass(String clsName)
    {
        int index = clsName.lastIndexOf("$");
        if (index >= 0)
        {
            processExistingIDs("innerclass", clsName.substring(index + 1));
        }
        else
        {
            index = clsName.lastIndexOf("/");
            clsName = index < 0 ? clsName : clsName.substring(index + 1);
            processExistingIDs("class", clsName);
        }
    }

    @Override
    public void processExistingField(String fdName)
    {
        String[] formatTypes = new String[] {"field", "enum"};
        for (String formatType : formatTypes)
        {
            if (processExistingIDs(formatType, fdName))
                break;
        }
    }

    @Override
    public void processExistingMethod(String mdName)
    {
        String[] formatTypes = new String[] {"method", "lambda", "access"};
        for (String formatType : formatTypes)
        {
            if (processExistingIDs(formatType, mdName))
                break;
        }
    }

    private boolean processExistingIDs(String formatType, String obfName)
    {
        Matcher matcher = regexMap.get(formatType).matcher(obfName);
        if (matcher.matches())
        {
            for (String key : keylistMap.get(formatType))
            {
                if (!key.toLowerCase(Locale.ROOT).equals("obfname"))
                {
                    int newVal = Integer.parseInt(matcher.group(key));
                    if (newVal >= idMap.getOrDefault(key, 0))
                    {
                        idMap.put(key, newVal + 1);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private String formatName(String formatType, String obfName)
    {
        String format = formatMap.get(formatType);
        for (String key : keylistMap.get(formatType))
        {
            if (key.toLowerCase(Locale.ROOT).equals("obfname"))
            {
                format = format.replace("{" + key + "}", obfName);
            }
            else
            {
                int id = idMap.getOrDefault(key, 0);
                format = format.replace("{" + key + "}", Integer.toString(id));
                idMap.put(key, id + 1);
            }
        }

        return format;
    }

    private Map<String, Pattern> createRegexMap()
    {
        Map<String, Pattern> ret = new HashMap<>();
        formatMap.forEach((formatType, format) ->
        {
            List<String> keys = new ArrayList<>();

            Matcher matcher = replacementPattern.matcher(format);
            while (matcher.find())
            {
                String key = matcher.group(1);
                keys.add(key);
                if (key.toLowerCase(Locale.ROOT).equals("obfname"))
                {
                    format = format.replace("{" + key + "}", "(?<" + key + ">[a-zA-Z]+)");
                }
                else
                {
                    format = format.replace("{" + key + "}", "(?<" + key + ">\\d+)");
                }
            }
            keylistMap.put(formatType, keys);
            ret.put(formatType, Pattern.compile(format));
        });
        return ret;
    }
}
