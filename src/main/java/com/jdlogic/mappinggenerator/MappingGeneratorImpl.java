package com.jdlogic.mappinggenerator;

import com.jdlogic.mappinggenerator.naming.ConfiguredNameGenerator;
import com.jdlogic.mappinggenerator.naming.INameGenerator;
import com.jdlogic.mappinggenerator.naming.INameProvider;
import com.jdlogic.mappinggenerator.naming.LayeredNameProvider;
import com.jdlogic.mappinggenerator.naming.MappingFileNameProvider;
import com.jdlogic.mappinggenerator.typeinfo.ClassInfo;
import com.jdlogic.mappinggenerator.typeinfo.FieldInfo;
import com.jdlogic.mappinggenerator.typeinfo.MethodInfo;
import net.minecraftforge.srgutils.IMappingFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MappingGeneratorImpl
{
    static final GeneratorConfig CONFIG = new GeneratorConfig();
    static void process(Path in, Path out, Path cfg, Path mappingFile, List<Path> libs, IMappingFile.Format format) throws IOException
    {
        CONFIG.load(cfg);

        INameGenerator generator = new ConfiguredNameGenerator(CONFIG);

        LayeredNameProvider nameProvider = new LayeredNameProvider();
        IMappingFile mappingOut = IMappingFile.create();

        nameProvider.addProvider(new MappingFileNameProvider(mappingOut));

        if (mappingFile != null)
        {
            IMappingFile oldMappingFile = IMappingFile.load(mappingFile.toFile());
            oldMappingFile.getClasses().forEach(cls ->
            {
                generator.processExistingClass(cls.getMapped());
                cls.getFields().stream().map(IMappingFile.INode::getMapped).forEach(generator::processExistingField);
                // TODO do this later when the 'type' of method is actually known?
                cls.getMethods().stream().map(IMappingFile.INode::getMapped).forEach(generator::processExistingMethod);
            });

            nameProvider.addProvider(new MappingFileNameProvider(oldMappingFile));
        }

        Map<String, ClassInfo> classes = new HashMap<>();

        MappingGeneratorImpl gen = new MappingGeneratorImpl();
        ClasspathLibs.getClasspathLibs(classes);
        gen.loadLibs(libs, classes);

        gen.processJar(in, classes, nameProvider, generator, mappingOut);

        mappingOut.write(out, format, false);
    }

    private void loadLibs(List<Path> libs, Map<String, ClassInfo> classes) throws IOException
    {
        for (Path p : libs)
        {
            MappingGenerator.LOG.info("Loading lib: " + p.toString());
            readJar(p, classes);
        }
    }

    public static void readClass(InputStream stream, Map<String, ClassInfo> classes) throws IOException
    {
        ClassReader reader = new ClassReader(Utils.toByteArray(stream));
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        ClassInfo info = new ClassInfo(classNode);
        classes.put(info.getName(), info);
    }

    public static void readClass(File lib, Map<String, ClassInfo> classes) throws IOException
    {
        try(InputStream stream = Files.newInputStream(lib.toPath()))
        {
            readClass(stream, classes);
        }
    }

    public static void readJar(Path lib, Map<String, ClassInfo> classes) throws IOException
    {
        try (ZipFile zip = new ZipFile(lib.toFile()))
        {
            List<? extends ZipEntry> entries = zip.stream()
                    .filter(e -> e.getName().endsWith(".class") && !e.getName().startsWith("."))
                    .collect(Collectors.toList());

            for (ZipEntry entry : entries)
            {
                try(InputStream stream = zip.getInputStream(entry))
                {
                    readClass(stream, classes);
                }
            }
        }
    }

    private void processJar(Path jar, Map<String, ClassInfo> classes, INameProvider nameProvider, INameGenerator nameGenerator, IMappingFile mappingOut) throws IOException
    {
        Map<String, ClassInfo> jarClasses = new HashMap<>();
        readJar(jar, jarClasses);

        List<ClassInfo> toProcess = jarClasses.values().stream().sorted(Comparator.comparing(ClassInfo::getName)).collect(Collectors.toList());
        classes.putAll(jarClasses);
        jarClasses.clear();

       List<Set<MethodInfo>> multiOverrides = new ArrayList<>();

       MappingGenerator.LOG.info("Processing method overrides and interface implementations");
        toProcess.forEach(cls ->
        {
            String indent = Utils.getLogIndent(cls.getName(), 1);
            MappingGenerator.LOG.info( indent + "Processing " + cls.getName() + "...");
            cls.initOverrides(classes, multiOverrides);
        });

        MappingGenerator.LOG.info("Renaming classes");
        toProcess.forEach(cls ->
        {
            renameClass(cls, nameProvider, nameGenerator, mappingOut);
        });

        MappingGenerator.LOG.info("Renaming class members");
        toProcess.forEach(cls ->
        {
            String indent = Utils.getLogIndent(cls.getName(), 1);
            MappingGenerator.LOG.info(indent + "Processing members for class " + cls.getName());
            cls.getFields().forEach(f -> renameField(cls, f, nameProvider, nameGenerator, mappingOut));
            cls.getMethods().forEach(m -> renameMethod(cls, m, nameProvider, nameGenerator, mappingOut, multiOverrides, toProcess));
        });
    }

    private static void renameClass(ClassInfo cls, INameProvider nameProvider, INameGenerator nameGenerator, IMappingFile mappingOut)
    {
        String clsName;
        String oldName = cls.getName();
        int index = oldName.lastIndexOf("$");
        String logIndent = Utils.getLogIndent(cls.getName(), 1);

        if (index >= 0)
        {
            // Inner/Anon class
            String newOuter = mappingOut.remapClass(oldName.substring(0, index));
            String inner = oldName.substring(index + 1);

            if (cls.isAnon())
            {
                clsName = newOuter + "$" + inner;
                MappingGenerator.LOG.info(logIndent + String.format("Renaming anon class %s to %s", cls.getName(), clsName));
            }
            else
            {
                clsName = nameProvider.getClassName(cls.getName()).orElseGet(() -> nameGenerator.generateInnerClass(cls.getName(), newOuter, inner));
                MappingGenerator.LOG.info(logIndent + String.format("Renaming inner class %s to %s", oldName, clsName));
            }
        }
        else
        {
            clsName = nameProvider.getClassName(cls.getName()).orElseGet(() -> nameGenerator.generateClass(cls.getName()));
            MappingGenerator.LOG.info(logIndent + String.format("Renaming class %s to %s", oldName, clsName));
        }
        mappingOut.addClass(cls.getName(), clsName);
    }

    private static void renameField(ClassInfo cls, FieldInfo f, INameProvider nameProvider, INameGenerator nameGenerator, IMappingFile mappingOut)
    {
        String logIndent = Utils.getLogIndent(cls.getName(), 2);

        String fieldName = nameProvider.getFieldName(cls.getName(), f.getName()).orElseGet(() ->
        {
            if (cls.isEnum() && f.hasModifier(Opcodes.ACC_ENUM))
            {
                if (f.hasModifier(Opcodes.ACC_ENUM))
                {
                    String ret = nameGenerator.generateEnum(cls.getName(), f.getName());
                    MappingGenerator.LOG.info(logIndent + String.format("Renaming enum %s to %s", f.toString(), ret));
                    return ret;
                }
                else if (f.hasModifier(Opcodes.ACC_SYNTHETIC) && f.getDesc().equals(String.format(Constants.ENUM_FVALS_DESC, cls.getName())))
                {
                    return Constants.ENUM_FVALS_NAME;
                }
            }
            String ret = nameGenerator.generateField(cls.getName(), f.getName());
            MappingGenerator.LOG.info(logIndent + String.format("Renaming field %s to %s", f.toString(), ret));
            return ret;
        });
        mappingOut.getClass(cls.getName()).addField(f.getName(), fieldName);
    }

    private static String renameMethod(ClassInfo currentCls, MethodInfo m, INameProvider nameProvider, INameGenerator nameGenerator, IMappingFile mappingOut, List<Set<MethodInfo>> multiOverrides, List<ClassInfo> jarClasses)
    {
        if (Utils.isConstructor(m.getName()))
            return "";

        if (!jarClasses.contains(m.getParent()))
            return m.getName(); // override from lib, just return the name

        String logIndent = Utils.getLogIndent(currentCls.getName(), 2);

        String methodName;
        if (m.getBouncer() != null)
        {
            methodName = renameMethod(currentCls, m.getBouncer(), nameProvider, nameGenerator, mappingOut, multiOverrides, jarClasses);
            MappingGenerator.LOG.info(logIndent + String.format("Renaming method %s after bouncer %s %s", m.toString(), methodName, m.getBouncer().getDesc()));
        }
        else if (m.getOverride() != null)
        {
            methodName = renameMethod(currentCls, m.getOverride(), nameProvider, nameGenerator, mappingOut, multiOverrides, jarClasses);
            MappingGenerator.LOG.info(logIndent + String.format("Renaming method %s after override %s.%s %s", m.toString(), m.getOverride().getParent().getName(), methodName, m.getOverride().getDesc()));
        }
        else
        {
            methodName = nameProvider.getMethodName(m.getParent().getName(), m.getName(), m.getDesc()).orElseGet(() ->
            {
                if (m.isLambda())
                {
                    String ret = nameGenerator.generateLambda(m.getParent().getName(), m.getName(), m.getDesc());
                    MappingGenerator.LOG.info(logIndent + String.format("Renaming lambda %s to %s", m.toString(), ret));
                    return ret;
                }
                else if (m.isAccessCheck())
                {
                    String ret = nameGenerator.generateAccessCheck(m.getParent().getName(), m.getName(), m.getDesc());
                    MappingGenerator.LOG.info(logIndent + String.format("Renaming access check %s to %s", m.toString(), ret));
                    return ret;
                }
                else if (m.getParent().isEnum())
                {
                    if (m.getDesc().equals(String.format(Constants.ENUM_VAL_OF_DESC, m.getParent().getName())))
                    {
                        return Constants.ENUM_VAL_OF_NAME;
                    }
                    else if (m.getDesc().equals(String.format(Constants.ENUM_VALS_DESC, m.getParent().getName())))
                    {
                        return Constants.ENUM_VALS_NAME;
                    }
                }

                String ret = multiOverrides.stream()
                        .filter(s -> s.contains(m))
                        .map(s -> s.stream()
                                .map(md -> nameProvider.getMethodName(md.getParent().getName(), md.getName(), md.getDesc()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .findFirst()
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                        .orElseGet(() -> nameGenerator.generateMethod(m.getParent().getName(), m.getName(), m.getDesc()));
                MappingGenerator.LOG.info(logIndent + String.format("Renaming method %s to %s", m.toString(), ret));
                return ret;
            });
        }

        mappingOut.getClass(m.getParent().getName()).addMethod(m.getName(), m.getDesc(), methodName);

        multiOverrides.stream()
            .filter(s -> s.contains(m))
            .forEach(s -> s.forEach(o -> mappingOut.getClass(o.getParent().getName()).addMethod(o.getName(), o.getDesc(), methodName)));

        return methodName;
    }
}
