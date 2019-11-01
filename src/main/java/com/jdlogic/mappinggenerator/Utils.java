package com.jdlogic.mappinggenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public class Utils
{
    public static <T> Stream<T> streamOfNullable(T t)
    {
        return t != null ? Stream.of(t) : Stream.empty();
    }

    public static byte[] toByteArray(InputStream stream) throws IOException
    {
        int len;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        while ((len = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, len);
        }

        return buffer.toByteArray();
    }

    public static boolean isConstructor(String name)
    {
        return Constants.INIT.equals(name) || Constants.CLINIT.equals(name);
    }

    public static int countChar(String s, char c)
    {
        int count = 0;
        for (int i = 0; i < s.length(); ++i)
        {
            if (s.charAt(i) == c)
                ++count;
        }
        return count;
    }

    public static String getLogIndent(String clsName)
    {
        return getLogIndent(clsName, 0);
    }

    public static String getLogIndent(String clsName, int offset)
    {
        return getLogIndent(countChar(clsName, '$') + offset);
    }

    public static String getLogIndent(int count)
    {
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (i++ < count)
        {
            ret.append("    ");
        }
        return ret.toString();
    }
}
