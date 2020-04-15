package cn.neural.common.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * ExceptionUtils
 *
 * @author lry
 */
public class ExceptionUtils {

    public static String toStack(Throwable e) {
        return toStack(null, e);
    }

    public static String toStack(String msg, Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        if (msg != null) {
            w.write(msg + "\n");
        }

        try (PrintWriter p = new PrintWriter(w)) {
            p.print(e.getClass().getName());
            e.printStackTrace(p);
            return w.toString();
        }
    }

    /**
     * Thread-unsafe StringWriter.
     */
    public static class UnsafeStringWriter extends Writer {
        private StringBuilder mBuffer;

        public UnsafeStringWriter() {
            lock = mBuffer = new StringBuilder();
        }

        @Override
        public void write(int c) {
            mBuffer.append((char) c);
        }

        @Override
        public void write(char[] cs) throws IOException {
            mBuffer.append(cs, 0, cs.length);
        }

        @Override
        public void write(char[] cs, int off, int len) throws IOException {
            if ((off < 0) || (off > cs.length) || (len < 0) ||
                    ((off + len) > cs.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            if (len > 0) {
                mBuffer.append(cs, off, len);
            }
        }

        @Override
        public void write(String str) {
            mBuffer.append(str);
        }

        @Override
        public void write(String str, int off, int len) {
            mBuffer.append(str, off, off + len);
        }

        @Override
        public Writer append(CharSequence csq) {
            if (csq == null) {
                write("null");
            } else {
                write(csq.toString());
            }
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) {
            CharSequence cs = (csq == null ? "null" : csq);
            write(cs.subSequence(start, end).toString());
            return this;
        }

        @Override
        public Writer append(char c) {
            mBuffer.append(c);
            return this;
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public String toString() {
            return mBuffer.toString();
        }
    }

}
