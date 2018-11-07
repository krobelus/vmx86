package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.memory.vector.Vector128;
import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.ImmediateOperand;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Psll extends AMD64Instruction {
    private final String name;
    private final Operand operand1;
    private final Operand operand2;

    @Child protected ReadNode readSrc;
    @Child protected ReadNode readShift;
    @Child protected WriteNode writeDst;

    protected Psll(long pc, byte[] instruction, String name, Operand operand1, Operand operand2) {
        super(pc, instruction);
        this.name = name;
        this.operand1 = operand1;
        this.operand2 = operand2;

        setGPRReadOperands(operand1, operand2);
        setGPRWriteOperands(operand1);
    }

    @Override
    protected void createChildNodes() {
        ArchitecturalState state = getState();
        readSrc = operand1.createRead(state, next());
        readShift = operand2.createRead(state, next());
        writeDst = operand1.createWrite(state, next());
    }

    public static class Psllw extends Psll {
        public Psllw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, "psllw", operands.getAVXOperand2(128), operands.getAVXOperand1(128));
        }

        public Psllw(long pc, byte[] instruction, OperandDecoder operands, int imm) {
            super(pc, instruction, "psllw", operands.getAVXOperand1(128), new ImmediateOperand(imm));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 val = readSrc.executeI128(frame);
            int n = readShift.executeI32(frame);
            Vector128 result = val.shlPackedI16(n);
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    public static class Pslld extends Psll {
        public Pslld(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, "pslld", operands.getAVXOperand2(128), operands.getAVXOperand1(128));
        }

        public Pslld(long pc, byte[] instruction, OperandDecoder operands, int imm) {
            super(pc, instruction, "pslld", operands.getAVXOperand1(128), new ImmediateOperand(imm));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 val = readSrc.executeI128(frame);
            int n = readShift.executeI32(frame);
            Vector128 result = val.shlPackedI32(n);
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    public static class Psllq extends Psll {
        public Psllq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, "psllq", operands.getAVXOperand2(128), operands.getAVXOperand1(128));
        }

        public Psllq(long pc, byte[] instruction, OperandDecoder operands, int imm) {
            super(pc, instruction, "psllq", operands.getAVXOperand1(128), new ImmediateOperand(imm));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            Vector128 val = readSrc.executeI128(frame);
            int n = readShift.executeI32(frame);
            Vector128 result = val.shlPackedI64(n);
            writeDst.executeI128(frame, result);
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{name, operand1.toString(), operand2.toString()};
    }
}
