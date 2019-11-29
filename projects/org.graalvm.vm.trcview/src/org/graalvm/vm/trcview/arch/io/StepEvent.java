package org.graalvm.vm.trcview.arch.io;

public abstract class StepEvent extends Event {
    protected StepEvent(short arch, int tid) {
        super(arch, STEP, tid);
    }

    public abstract byte[] getMachinecode();

    public abstract String getDisassembly();

    public abstract String[] getDisassemblyComponents();

    public abstract String getMnemonic();

    public abstract long getPC();

    public abstract boolean isCall();

    public abstract boolean isReturn();

    public abstract boolean isSyscall();

    public abstract boolean isReturnFromSyscall();

    public abstract InstructionType getType();

    public abstract long getStep();

    public abstract CpuState getState();

    public abstract StepFormat getFormat();
}