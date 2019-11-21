package org.graalvm.vm.x86.trcview.arch;

import java.io.InputStream;

import org.graalvm.vm.posix.elf.Elf;
import org.graalvm.vm.x86.trcview.decode.CallDecoder;
import org.graalvm.vm.x86.trcview.decode.SyscallDecoder;
import org.graalvm.vm.x86.trcview.io.data.ArchTraceReader;
import org.graalvm.vm.x86.trcview.io.data.DefaultEventParser;
import org.graalvm.vm.x86.trcview.io.data.EventParser;
import org.graalvm.vm.x86.trcview.io.data.StepFormat;

public class None extends Architecture {
    public static final StepFormat FORMAT = new StepFormat(StepFormat.NUMBERFMT_HEX, 16, 16, 1, false);

    private static final EventParser DEFAULT_PARSER = new DefaultEventParser();

    @Override
    public short getId() {
        return Elf.EM_NONE;
    }

    @Override
    public String getName() {
        return "none";
    }

    @Override
    public ArchTraceReader getTraceReader(InputStream in) {
        return null;
    }

    @Override
    public EventParser getEventParser() {
        return DEFAULT_PARSER;
    }

    @Override
    public SyscallDecoder getSyscallDecoder() {
        return null;
    }

    @Override
    public CallDecoder getCallDecoder() {
        return null;
    }

    @Override
    public int getTabSize() {
        return 16;
    }

    @Override
    public StepFormat getFormat() {
        return FORMAT;
    }
}