package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.RegisterAccessFactory;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.Flags;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteFlagNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Inc extends AMD64Instruction {
    private final Operand operand;

    @Child protected ReadNode read;
    @Child protected WriteNode write;

    @Child protected WriteFlagNode writeOF;
    @Child protected WriteFlagNode writeSF;
    @Child protected WriteFlagNode writeZF;
    @Child protected WriteFlagNode writeAF;
    @Child protected WriteFlagNode writePF;

    protected Inc(long pc, byte[] instruction, Operand operand) {
        super(pc, instruction);
        this.operand = operand;
    }

    protected void createChildren() {
        assert read == null;
        assert write == null;

        CompilerDirectives.transferToInterpreterAndInvalidate();
        ArchitecturalState state = getContextReference().get().getState();
        RegisterAccessFactory regs = state.getRegisters();
        read = operand.createRead(state, next());
        write = operand.createWrite(state, next());
        writeOF = regs.getOF().createWrite();
        writeSF = regs.getSF().createWrite();
        writeZF = regs.getZF().createWrite();
        writeAF = regs.getAF().createWrite();
        writePF = regs.getPF().createWrite();
    }

    public static class Incb extends Inc {
        public Incb(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R8));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            if (read == null) {
                createChildren();
            }
            byte val = read.executeI8(frame);
            byte result = (byte) (val + 1);
            write.executeI8(frame, result);

            boolean overflow = result < 0 && val > 0;
            boolean adjust = ((val ^ result) & 0x10) != 0;

            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity(result));
            writeAF.execute(frame, adjust);
            return next();
        }
    }

    public static class Incw extends Inc {
        public Incw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R16));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            if (read == null) {
                createChildren();
            }
            short val = read.executeI16(frame);
            short result = (short) (val + 1);
            write.executeI16(frame, result);

            boolean overflow = result < 0 && val > 0;
            boolean adjust = ((val ^ result) & 0x10) != 0;

            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            writeAF.execute(frame, adjust);
            return next();
        }
    }

    public static class Incl extends Inc {
        public Incl(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R32));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            if (read == null) {
                createChildren();
            }
            int val = read.executeI32(frame);
            int result = val + 1;
            write.executeI32(frame, result);

            boolean overflow = result < 0 && val > 0;
            boolean adjust = ((val ^ result) & 0x10) != 0;

            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            writeAF.execute(frame, adjust);
            return next();
        }
    }

    public static class Incq extends Inc {
        public Incq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R64));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            if (read == null) {
                createChildren();
            }
            long val = read.executeI64(frame);
            long result = val + 1;
            write.executeI64(frame, result);

            boolean overflow = result < 0 && val > 0;
            boolean adjust = ((val ^ result) & 0x10) != 0;

            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            writeAF.execute(frame, adjust);
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{"inc", operand.toString()};
    }
}
