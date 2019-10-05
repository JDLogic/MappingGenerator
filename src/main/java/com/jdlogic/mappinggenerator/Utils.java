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
}
