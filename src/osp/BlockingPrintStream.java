package osp;

import arc.func.Cons;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class BlockingPrintStream extends PrintStream {
    private final Cons<String> cons;

    private int last = -1;

    public BlockingPrintStream(Cons<String> cons) {
        super(new ByteArrayOutputStream());
        this.cons = cons;
    }

    public ByteArrayOutputStream out() {
        return (ByteArrayOutputStream) out;
    }

    @Override
    public void write(int b) {
        if (last == 13 && b == 10) {
            last = -1;
            return;
        }

        last = b;
        if (b == 13 || b == 10) {
            flush();
        } else {
            super.write(b);
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        for (int i = 0; i < len; i++) {
            write(buf[off + i]);
        }
    }

    @Override
    public void flush() {
        String str = out().toString();
        out().reset();
        cons.get(str);
    }
}