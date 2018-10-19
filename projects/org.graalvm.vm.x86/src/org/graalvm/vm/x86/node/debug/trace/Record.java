package org.graalvm.vm.x86.node.debug.trace;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Logger;

import org.graalvm.vm.memory.util.HexFormatter;

import com.everyware.util.io.WordInputStream;
import com.everyware.util.io.WordOutputStream;
import com.everyware.util.log.Trace;

public abstract class Record {
    private static final Logger log = Trace.create(Record.class);

    private final int magic;

    protected Record(int magic) {
        this.magic = magic;
    }

    @SuppressWarnings("unchecked")
    public static final <T extends Record> T read(WordInputStream in) throws IOException {
        int type;
        try {
            type = in.read32bit();
        } catch (EOFException e) {
            return null;
        }
        int size = in.read32bit();
        long start = in.tell();

        Record record = null;
        switch (type) {
            case CpuStateRecord.MAGIC:
                record = new CpuStateRecord();
                break;
            case LocationRecord.MAGIC:
                record = new LocationRecord();
                break;
            case MemoryEventRecord.MAGIC:
                record = new MemoryEventRecord();
                break;
            case StepRecord.MAGIC:
                record = new StepRecord();
                break;
            case CallArgsRecord.MAGIC:
                record = new CallArgsRecord();
                break;
            case SystemLogRecord.MAGIC:
                record = new SystemLogRecord();
                break;
            case EofRecord.MAGIC:
                record = new EofRecord();
                break;
        }
        if (record != null) {
            record.readRecord(in);
        } else {
            log.warning("Unknown record: 0x" + HexFormatter.tohex(type, 8));
            in.skip(size);
        }

        long end = in.tell();
        long sz = end - start;
        if (sz != size) {
            throw new IOException("Error: invalid size (" + size + " vs " + sz + "; " + (record != null ? record.getClass().getSimpleName() : "unknown class") + ")");
        }

        return (T) record;
    }

    public final void write(WordOutputStream out) throws IOException {
        int size = size();
        out.write32bit(magic);
        out.write32bit(size);
        long start = out.tell();
        writeRecord(out);
        long end = out.tell();
        long sz = end - start;
        if (sz != size) {
            throw new IOException("Error: invalid size (" + size + " vs " + sz + ")");
        }
    }

    protected abstract int size();

    protected abstract void readRecord(WordInputStream in) throws IOException;

    protected abstract void writeRecord(WordOutputStream out) throws IOException;

    protected static final int sizeString(String s) {
        if (s == null) {
            return 2;
        } else {
            return 2 + s.getBytes().length;
        }
    }

    protected static final String readString(WordInputStream in) throws IOException {
        int length = in.read16bit();
        if (length == 0) {
            return null;
        } else {
            byte[] bytes = new byte[length];
            in.read(bytes);
            return new String(bytes);
        }
    }

    protected static final void writeString(WordOutputStream out, String s) throws IOException {
        if (s == null) {
            out.write16bit((short) 0);
        } else {
            byte[] bytes = s.getBytes();
            out.write16bit((short) bytes.length);
            out.write(bytes);
        }
    }

    protected static final int sizeStringArray(String[] s) {
        if (s == null) {
            return 2;
        } else {
            int size = 2;
            for (String part : s) {
                size += sizeString(part);
            }
            return size;
        }
    }

    protected static final String[] readStringArray(WordInputStream in) throws IOException {
        int length = in.read16bit();
        if (length == 0) {
            return null;
        } else {
            String[] result = new String[length];
            for (int i = 0; i < result.length; i++) {
                int slen = in.read16bit();
                if (slen == 0) {
                    result[i] = null;
                } else {
                    byte[] bytes = new byte[slen];
                    in.read(bytes);
                    result[i] = new String(bytes);
                }
            }
            return result;
        }
    }

    protected static final void writeStringArray(WordOutputStream out, String[] data) throws IOException {
        if (data == null) {
            out.write16bit((short) 0);
        } else {
            out.write16bit((short) data.length);
            for (int i = 0; i < data.length; i++) {
                writeString(out, data[i]);
            }
        }
    }

    protected static final int sizeArray(byte[] data) {
        if (data == null) {
            return 4;
        } else {
            return data.length + 4;
        }
    }

    protected static final byte[] readArray(WordInputStream in) throws IOException {
        int length = in.read32bit();
        if (length == 0) {
            return null;
        } else {
            byte[] data = new byte[length];
            in.read(data);
            return data;
        }
    }

    protected static final void writeArray(WordOutputStream out, byte[] data) throws IOException {
        if (data == null) {
            out.write32bit(0);
        } else {
            out.write32bit(data.length);
            out.write(data);
        }
    }
}
