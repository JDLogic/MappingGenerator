package com.jdlogic.mappinggenerator;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import net.minecraftforge.srgutils.IMappingFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MappingGenerator
{
    public static final String VERSION = "MappingGenerator v" + Optional.ofNullable(MappingGenerator.class.getPackage().getImplementationVersion()).orElse("Unknown");
    public static final Logger LOG = Logger.getLogger("com.jdlogic.mappinggenerator.MappingGenerator");

    private Path in;
    private Path out;
    private Path cfg;
    private Path mappingFile;
    private List<Path> libs = new ArrayList<>();
    private IMappingFile.Format format;

    public MappingGenerator(Path in, Path out, Path cfg)
    {
        this.in = in;
        this.out = out;
        this.cfg = cfg;
    }

    public MappingGenerator format(IMappingFile.Format format)
    {
        this.format = format;
        return this;
    }

    public MappingGenerator mapping(Path mappingFile)
    {
        this.mappingFile = mappingFile;
        return this;
    }

    public MappingGenerator addLib(Path lib)
    {
        this.libs.add(lib);
        return this;
    }

    public MappingGenerator addLibs(Collection<Path> libs)
    {
        this.libs.addAll(libs);
        return this;
    }

    public MappingGenerator log(Path log)
    {
        if (log == null)
            return this;

        try
        {
            FileHandler filehandler = new FileHandler(log.toString());
            filehandler.setFormatter(new LogFormatter());
            MappingGenerator.LOG.addHandler(filehandler);
        }
        catch (IOException e)
        {
            System.out.println("Failed to setup logger: " + e.toString());
            e.printStackTrace();
        }
        return this;
    }

    public MappingGenerator log()
    {
        return log(System.out);
    }

    public MappingGenerator log(PrintStream stream)
    {
        LOG.addHandler(new Handler()
        {
            @Override
            public void publish(LogRecord record)
            {
                stream.println(String.format(record.getMessage(), record.getParameters()));
            }
            @Override public void flush() {}
            @Override public void close() throws SecurityException {}
        });
        return this;
    }

    public void run() throws IOException
    {
        MappingGeneratorImpl.process(this.in, this.out, this.cfg, this.mappingFile, this.libs, this.format);
    }

    private static ValueConverter<Path> PATH_ARG = new ValueConverter<Path>()
    {
        public Path convert( String value )
        {
            return Paths.get(value);
        }

        public Class<Path> valueType()
        {
            return Path.class;
        }

        public String valuePattern()
        {
            return null;
        }
    };

    private static ValueConverter<Level> LEVEL_ARG = new ValueConverter<Level>()
    {
        public Level convert( String value )
        {
            return Level.parse(value.toUpperCase(Locale.ENGLISH));
        }

        public Class<Level> valueType()
        {
            return Level.class;
        }

        public String valuePattern()
        {
            return null;
        }
    };

    private static ValueConverter<IMappingFile.Format> FORMAT_ARG = new ValueConverter<IMappingFile.Format>()
    {
        public IMappingFile.Format convert( String value )
        {
            return IMappingFile.Format.get(value.toUpperCase(Locale.ENGLISH));
        }

        public Class<IMappingFile.Format> valueType()
        {
            return IMappingFile.Format.class;
        }

        public String valuePattern()
        {
            return null;
        }
    };

    public static void main(String[] args) throws Exception
    {
        OptionParser parser = new OptionParser();
        OptionSpec<Void>  help    = parser.accepts("help")   .forHelp();
        OptionSpec<Void>  ver     = parser.accepts("version").forHelp();
        OptionSpec<Path>  in      = parser.accepts("in")      .withRequiredArg().withValuesConvertedBy(PATH_ARG).required();
        OptionSpec<Path>  out     = parser.accepts("out")     .withRequiredArg().withValuesConvertedBy(PATH_ARG).required();
        OptionSpec<Path>  config  = parser.accepts("cfg")     .withRequiredArg().withValuesConvertedBy(PATH_ARG).required();
        OptionSpec<Path>  mapping = parser.accepts("mapping") .withRequiredArg().withValuesConvertedBy(PATH_ARG);
        OptionSpec<Path>  libs    = parser.accepts("libs")    .withRequiredArg().withValuesConvertedBy(PATH_ARG);
        OptionSpec<Path>  lib     = parser.accepts("lib")     .withRequiredArg().withValuesConvertedBy(PATH_ARG);
        OptionSpec<Path>  log     = parser.accepts("log")     .withRequiredArg().withValuesConvertedBy(PATH_ARG);
        OptionSpec<Level> logLvl  = parser.accepts("level")   .withRequiredArg().withValuesConvertedBy(LEVEL_ARG).defaultsTo(Level.INFO);
        OptionSpec<IMappingFile.Format> format = parser.accepts("format").withRequiredArg().withValuesConvertedBy(FORMAT_ARG).defaultsTo(IMappingFile.Format.TSRG);

        try
        {
            OptionSet options = parser.parse(args);
            if (options.has(help))
            {
                System.out.println(VERSION);
                parser.printHelpOn(System.out);
                return;
            }
            else if (options.has(ver))
            {
                System.out.println(VERSION);
                return;
            }

            LOG.setUseParentHandlers(false);
            LOG.setLevel(options.valueOf(logLvl));

            List<Path> libsIn = new ArrayList<>(options.valuesOf(lib));

            if (options.has(libs))
            {
                try
                {
                    Files.readAllLines(options.valueOf(libs)).forEach(l ->
                    {
                        String p = l.contains("=") ? l.split("=")[1] : l;
                        libsIn.add(Paths.get(p));
                    });
                }
                catch (Exception e)
                {
                    System.err.println("ERROR: " + e.getMessage());
                    LOG.log(Level.SEVERE, "ERROR", e);
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            try
            {
                MappingGenerator generator = new MappingGenerator(options.valueOf(in), options.valueOf(out), options.valueOf(config))
                        .log()
                        .log(options.valueOf(log));

                LOG.info(VERSION);
                LOG.info("Input:        " + options.valueOf(in));
                LOG.info("Output:       " + options.valueOf(out));
                LOG.info("Mapping:      " + options.valueOf(mapping));
                LOG.info("Config:       " + options.valueOf(config));
                LOG.info("Log:          " + options.valueOf(log));

                libsIn.forEach(p -> LOG.info("Lib:          " + p.toString()));

                generator.mapping(options.valueOf(mapping))
                        .addLibs(libsIn)
                        .format(options.valueOf(format))
                        .run();
            }
            catch (Exception e)
            {
                System.err.println("ERROR: " + e.getMessage());
                LOG.log(Level.SEVERE, "ERROR", e);
                e.printStackTrace();
                System.exit(1);
            }
        }
        catch (OptionException e)
        {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }
}
