/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for logging event and status messages. It maintains a
 * collection of streams for different types of messages, but any messages with
 * unknown or unspecified stream go to the default stream.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Logger.java,v 1.8 2006/11/09 06:01:43 rickknowles Exp $
 */
public class Logger {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator"); 
    
    public final static String DEFAULT_STREAM = "Winstone";
    public static int MIN = 1;
    public static int ERROR = 2;
    public static int WARNING = 3;
    public static int INFO = 5;
    public static int SPEED = 6;
    public static int DEBUG = 7;
    public static int FULL_DEBUG = 8;
    public static int MAX = 9;

    protected static Boolean semaphore = new Boolean(true);
    protected static boolean initialised = false;
    protected static Writer defaultStream;
    protected static Map namedStreams;
//    protected static Collection nullStreams;
    protected static int currentDebugLevel;
    protected final static DateFormat sdfLog = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    protected static boolean showThrowingThread;

    /**
     * Initialises default streams
     */
    public static void init(int level) {
        init(level, System.out, false);
    }

    /**
     * Initialises default streams
     */
    public static void init(int level, OutputStream defaultStream, 
            boolean showThrowingThreadArg) {
        synchronized (semaphore) {
            if (!initialised) { // recheck in case we were blocking on another init
                initialised = false;
                currentDebugLevel = level;
                namedStreams = new HashMap();
//                nullStreams = new ArrayList();
                initialised = true;
                setStream(DEFAULT_STREAM, defaultStream);
                showThrowingThread = showThrowingThreadArg;
            }
        }
    }

    /**
     * Allocates a stream for redirection to a file etc
     */
    public static void setStream(String name, OutputStream stream) {
        setStream(name, stream != null ? new OutputStreamWriter(stream) : null);
    }

    /**
     * Allocates a stream for redirection to a file etc
     */
    public static void setStream(String name, Writer stream) {
        if (name == null) {
            name = DEFAULT_STREAM;
        }
        if (!initialised) {
            init(INFO);
        }
        synchronized (semaphore) {
            if (name.equals(DEFAULT_STREAM)) {
                defaultStream = stream;
            } else if (stream == null) {
                namedStreams.remove(name);
            } else {
                namedStreams.put(name, stream);
            }
        }
    }

    /**
     * Forces a flush of the contents to file, display, etc
     */
    public static void flush(String name) {
        if (!initialised) {
            init(INFO);
        }

        Writer stream = getStreamByName(name);
        if (stream != null) {
            try {stream.flush();} catch (IOException err) {}
        }
    }
    
    private static Writer getStreamByName(String streamName) {
        if ((streamName != null) && streamName.equals(DEFAULT_STREAM)) {
            // As long as the stream has not been nulled, assign the default if not found
            synchronized (semaphore) {
                Writer stream = (Writer) namedStreams.get(streamName);
                if ((stream == null) && !namedStreams.containsKey(streamName)) {
                    stream = defaultStream;
                }
                return stream;
            }
        } else {
            return defaultStream;
        }
        
    }

    public static void setCurrentDebugLevel(int level) {
        if (!initialised) {
            init(level);
        } else synchronized (semaphore) {
            currentDebugLevel = level;
        }
    }

    /**
     * Writes a log message to the requested stream, and immediately flushes
     * the contents of the stream.
     */
    private static void logInternal(String streamName, String message, Throwable error) {
        
        if (!initialised) {
            init(INFO);
        }
        
        Writer stream = getStreamByName(streamName);
        if (stream != null) {
            Writer fullMessage = new StringWriter();
            String date = null;
            synchronized (sdfLog) {
                date = sdfLog.format(new Date());
            }
            try {
                fullMessage.write("[");
                fullMessage.write(streamName);
                fullMessage.write(" ");
                fullMessage.write(date);
                fullMessage.write("] - ");
                if (showThrowingThread) {
                    fullMessage.write("[");
                    fullMessage.write(Thread.currentThread().getName());
                    fullMessage.write("] - ");
                }
                fullMessage.write(message);
                if (error != null) {
                    fullMessage.write(LINE_SEPARATOR);
                    PrintWriter pw = new PrintWriter(fullMessage);
                    error.printStackTrace(pw);
                    pw.flush();
                }
                fullMessage.write(LINE_SEPARATOR);
                
                stream.write(fullMessage.toString());
                stream.flush();
            } catch (IOException err) {
                System.err.println(Launcher.RESOURCES.getString("Logger.StreamWriteError", message));
                err.printStackTrace(System.err);
            }
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey), null);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, Throwable error) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey), error);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String param) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, param), null);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String params[]) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, params), null);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String param, Throwable error) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, param), error);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String messageKey, String params[], Throwable error) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(DEFAULT_STREAM, resources.getString(messageKey, params), error);
        }
    }

    public static void log(int level, WinstoneResourceBundle resources,
            String streamName, String messageKey, String params[], Throwable error) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(streamName, resources.getString(messageKey, params), error);
        }
    }

    public static void logDirectMessage(int level, String streamName, String message, 
            Throwable error) {
        if (currentDebugLevel < level) {
            return;
        } else {
            logInternal(streamName, message, error);
        }
    }
}
