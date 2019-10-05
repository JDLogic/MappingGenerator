package com.jdlogic.mappinggenerator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

// Basically a copy of the log formatter here:
// https://github.com/ModCoderPack/MCInjector/blob/master/src/main/java/de/oceanlabs/mcp/mcinjector/LogFormatter.java
public class LogFormatter extends Formatter
{
    @Override
    public synchronized String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getLevel().getName());
        sb.append(": ");
        sb.append(this.formatMessage(record));
        sb.append("\n");
        if (record.getThrown() != null)
        {
            try(
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)
            )
            {
                record.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            }
            catch (Exception ex) { /*NOOP*/ }
        }
        return sb.toString();
    }
}
