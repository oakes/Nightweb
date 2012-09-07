/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class ServletOutputStream extends OutputStream {
    final String CR_LF = "\r\n";

    protected ServletOutputStream() {
        super();
    }

    public void print(boolean b) throws IOException {
        print("" + b);
    }

    public void print(char c) throws IOException {
        print("" + c);
    }

    public void print(double d) throws IOException {
        print("" + d);
    }

    public void print(float f) throws IOException {
        print("" + f);
    }

    public void print(int i) throws IOException {
        print("" + i);
    }

    public void print(long l) throws IOException {
        print("" + l);
    }

    public void print(String s) throws IOException {
        write(s.getBytes());
    }

    public void println() throws IOException {
        println("");
    }

    public void println(boolean b) throws IOException {
        println("" + b);
    }

    public void println(char c) throws IOException {
        println("" + c);
    }

    public void println(double d) throws IOException {
        println("" + d);
    }

    public void println(float f) throws IOException {
        println("" + f);
    }

    public void println(int i) throws IOException {
        println("" + i);
    }

    public void println(long l) throws IOException {
        println("" + l);
    }

    public void println(String s) throws IOException {
        print(s + CR_LF);
    }
}
