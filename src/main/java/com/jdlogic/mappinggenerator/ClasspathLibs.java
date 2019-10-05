package com.jdlogic.mappinggenerator;

import com.jdlogic.mappinggenerator.typeinfo.ClassInfo;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClasspathLibs
{
    public static void getClasspathLibs(Map<String, ClassInfo> classes) throws IOException
    {
        Set<String> found = new HashSet<String>();
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
    }
}
