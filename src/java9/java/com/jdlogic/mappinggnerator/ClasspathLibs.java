package com.jdlogic.mappinggnerator;

import com.jdlogic.mappinggenerator.MappingGenerator;
import com.jdlogic.mappinggenerator.MappingGeneratorImpl;
import com.jdlogic.mappinggenerator.typeinfo.ClassInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ClasspathLibs
{
    public static void getClasspathLibs(Map<String, ClassInfo> classes) throws IOException
    {
        Set<String> found = new HashSet<>();
        String[] props = { System.getProperty("java.class.path"), System.getProperty("sun.boot.class.path") };
        for (String prop : props) {
            if (prop == null)
                continue;

            for (final String path : prop.split(File.pathSeparator)) {
                File file = new File(path);
                if (!found.add(file.getAbsolutePath()) || !file.exists())
                    continue;

                if (file.getName().endsWith(".class"))
                {
                    MappingGenerator.LOG.info("Loading classpath lib: " + file.getAbsolutePath());
                    MappingGeneratorImpl.readClass(file, classes);
                }
                else if (file.getName().endsWith(".jar"))
                {
                    MappingGenerator.LOG.info("Loading classpath lib: " + file.getAbsolutePath());
                    MappingGeneratorImpl.readJar(file.toPath(), classes);
                }
            }
        }

        addClasspathModules(classes);
    }

    private static void addClasspathModules(Map<String, ClassInfo> classes) throws IOException
    {
        for (ModuleReference module : ModuleFinder.ofSystem().findAll())
        {
            MappingGenerator.LOG.info("Loading classpath module: " + module.descriptor().name());
            try (ModuleReader reader = module.open())
            {
                reader.list()
                        .filter(s -> s.endsWith(".class") && !s.endsWith("module-info.class"))
                        .map(s ->
                        {
                            try
                            {
                                return reader.open(s);
                            }
                            catch (IOException e) { /* NOOP */ }
                            return Optional.<InputStream>empty();
                        })
                        .filter(Optional::isPresent)
                        .forEach(optional ->
                        {
                            try (InputStream stream = optional.get())
                            {
                                MappingGeneratorImpl.readClass(stream, classes);
                            }
                            catch (IOException e) { /* NOOP */ }
                        });
            }
        }
    }
}
