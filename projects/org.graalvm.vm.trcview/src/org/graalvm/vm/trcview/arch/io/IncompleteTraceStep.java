package org.graalvm.vm.trcview.arch.io;

import java.io.IOException;

import org.graalvm.vm.posix.elf.Elf;
import org.graalvm.vm.trcview.arch.None;
import org.graalvm.vm.util.io.WordOutputStream;

public class IncompleteTraceStep extends StepEvent {
    public IncompleteTraceStep(int tid) {
        super(Elf.EM_NONE, tid);
    }

    @Override
    public byte[] getMachinecode() {
        return new byte[0];
    }

    @Override
    public String getDisassembly() {
        return null;
    }

    @Override
    public String[] getDisassemblyComponents() {
        return null;
    }

    @Override
    public String getMnemonic() {
        return null;
    }

    @Override
    public long getPC() {
        return 0;
    }

    @Override
    public boolean isCall() {
        return false;
    }

    @Override
    public boolean isReturn() {
        return false;
    }

    @Override
    public boolean isSyscall() {
        return false;
    }

    @Override
    public boolean isReturnFromSyscall() {
        return false;
    }

    @Override
    public InstructionType getType() {
        return InstructionType.OTHER;
    }

    @Override
    public long getStep() {
        return 0;
    }

    @Override
    public CpuState getState() {
        return new CpuState(Elf.EM_NONE, getTid()) {
            @Override
            public long getStep() {
                return IncompleteTraceStep.this.getPC();
            }

            @Override
            public long getPC() {
                return IncompleteTraceStep.this.getPC();
            }

            @Override
            public long get(String name) {
                return 0;
            }

            @Override
            protected void writeRecord(WordOutputStream out) throws IOException {
                // nothing
            }

            @Override
            public String toString() {
                return "<unavailable>\n";
            }
        };
    }

    @Override
    public StepFormat getFormat() {
        return None.FORMAT;
    }

    @Override
    protected void writeRecord(WordOutputStream out) throws IOException {
        // nothing
    }
}