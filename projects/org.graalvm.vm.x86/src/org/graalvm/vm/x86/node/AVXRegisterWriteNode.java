package org.graalvm.vm.x86.node;

import org.graalvm.vm.memory.vector.Vector128;
import org.graalvm.vm.memory.vector.Vector256;
import org.graalvm.vm.memory.vector.Vector512;
import org.graalvm.vm.x86.isa.AVXRegister;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

public class AVXRegisterWriteNode extends WriteNode {
    private static final boolean USE_XMM = true;
    private static final boolean USE_TYPE = true;

    private final FrameSlot zmm;
    private final FrameSlot xmm;
    private final FrameSlot xmmF32;
    private final FrameSlot xmmF64;
    private final FrameSlot xmmType;

    @CompilationFinal private int cachedType;

    public AVXRegisterWriteNode(FrameSlot zmm, FrameSlot xmm, FrameSlot xmmF32, FrameSlot xmmF64, FrameSlot xmmType) {
        this.zmm = zmm;
        this.xmm = xmm;
        this.xmmF32 = xmmF32;
        this.xmmF64 = xmmF64;
        this.xmmType = xmmType;
    }

    // TODO: add fallback if type changes during runtime
    private int cached(int type) {
        if (type != cachedType) {
            if (CompilerDirectives.inCompiledCode()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                System.out.println("PERF WARNING: AVX register type changed! (" + type + " -> " + type + ")");
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedType = type;
            return type;
        } else {
            return cachedType;
        }
    }

    public void executeClear(VirtualFrame frame) {
        if (cachedType != AVXRegister.TYPE_XMM) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedType = AVXRegister.TYPE_XMM;
        }

        frame.setInt(xmmType, AVXRegister.TYPE_XMM);
        frame.setObject(zmm, new Vector512());
        frame.setObject(xmm, new Vector128());
    }

    private void materialize(VirtualFrame frame, int type) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (type == AVXRegister.TYPE_F32) {
            Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
            float val = FrameUtil.getFloatSafe(frame, xmmF32);
            reg.setF32(3, val);
            frame.setInt(xmmType, AVXRegister.TYPE_XMM);
        } else if (type == AVXRegister.TYPE_F64) {
            Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
            double val = FrameUtil.getDoubleSafe(frame, xmmF64);
            reg.setF64(1, val);
            frame.setInt(xmmType, AVXRegister.TYPE_XMM);
        } else {
            throw new AssertionError("unknown type: " + type);
        }
    }

    public void executeI32(VirtualFrame frame, int i, int value) {
        CompilerAsserts.partialEvaluationConstant(zmm);
        if (USE_XMM) {
            if (USE_TYPE) {
                int type = cached(FrameUtil.getIntSafe(frame, xmmType));
                if (type != AVXRegister.TYPE_XMM) {
                    materialize(frame, type);
                }
            }
            Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
            reg.setI32(i - 12, value);
        } else {
            Vector512 reg = (Vector512) FrameUtil.getObjectSafe(frame, zmm);
            reg.setI32(i, value);
        }
    }

    public void executeI64(VirtualFrame frame, int i, long value) {
        CompilerAsserts.partialEvaluationConstant(zmm);
        if (USE_XMM) {
            if (USE_TYPE) {
                int type = cached(FrameUtil.getIntSafe(frame, xmmType));
                if (type != AVXRegister.TYPE_XMM) {
                    materialize(frame, type);
                }
            }
            Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
            reg.setI64(i - 6, value);
        } else {
            Vector512 reg = (Vector512) FrameUtil.getObjectSafe(frame, zmm);
            reg.setI64(i, value);
        }
    }

    public void executeI128(VirtualFrame frame, int i, Vector128 value) {
        CompilerAsserts.partialEvaluationConstant(zmm);
        if (USE_XMM) {
            if (USE_TYPE) {
                cached(AVXRegister.TYPE_XMM);
                frame.setInt(xmmType, AVXRegister.TYPE_XMM);
            }
            assert i == 3;
            frame.setObject(xmm, value.clone());
        } else {
            Vector512 reg = (Vector512) FrameUtil.getObjectSafe(frame, zmm);
            reg.setI128(i, value);
        }
    }

    public void executeI256(VirtualFrame frame, int i, Vector256 value) {
        CompilerAsserts.partialEvaluationConstant(zmm);
        if (USE_XMM) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError("AVX is unsupported");
        } else {
            Vector512 reg = (Vector512) FrameUtil.getObjectSafe(frame, zmm);
            reg.setI256(i, value);
        }
    }

    @Override
    public void executeI512(VirtualFrame frame, Vector512 value) {
        CompilerAsserts.partialEvaluationConstant(zmm);
        if (USE_XMM) {
            if (USE_TYPE) {
                cached(AVXRegister.TYPE_XMM);
                frame.setInt(xmmType, AVXRegister.TYPE_XMM);
            }
            Vector128 r0 = value.getI128(0);
            Vector128 r1 = value.getI128(1);
            Vector128 r2 = value.getI128(2);
            Vector128 r3 = value.getI128(3);
            if (!r0.equals(Vector128.ZERO) || !r1.equals(Vector128.ZERO) || !r2.equals(Vector128.ZERO)) {
                CompilerDirectives.transferToInterpreter();
                throw new AssertionError("AVX is unsupported");
            }
            frame.setObject(xmm, r3);
        } else {
            frame.setObject(zmm, value.clone());
        }
    }

    @Override
    public void executeI8(VirtualFrame frame, byte value) {
        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException();
    }

    @Override
    public void executeI16(VirtualFrame frame, short value) {
        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException();
    }

    @Override
    public void executeI32(VirtualFrame frame, int value) {
        executeI32(frame, 15, value);
    }

    @Override
    public void executeF32(VirtualFrame frame, float value) {
        if (USE_XMM) {
            if (USE_TYPE) {
                int type = cached(FrameUtil.getIntSafe(frame, xmmType));
                if (type == AVXRegister.TYPE_F32) {
                    frame.setFloat(xmmF32, value);
                } else {
                    if (type != AVXRegister.TYPE_XMM) {
                        materialize(frame, type);
                    }
                    // frame.setFloat(xmmF32, value);
                    // frame.setInt(xmmType, AVXRegister.TYPE_F32);
                    Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
                    reg.setF32(3, value);
                }
            } else {
                Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
                reg.setF32(3, value);
            }
        } else {
            super.executeF32(frame, value);
        }
    }

    @Override
    public void executeI64(VirtualFrame frame, long value) {
        executeI64(frame, 7, value);
    }

    @Override
    public void executeF64(VirtualFrame frame, double value) {
        if (USE_XMM) {
            if (USE_TYPE) {
                int type = cached(FrameUtil.getIntSafe(frame, xmmType));
                if (type == AVXRegister.TYPE_F64) {
                    frame.setDouble(xmmF64, value);
                } else {
                    if (type != AVXRegister.TYPE_XMM) {
                        materialize(frame, type);
                    }
                    // frame.setDouble(xmmF64, value);
                    // frame.setInt(xmmType, AVXRegister.TYPE_F64);
                    Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
                    reg.setF64(1, value);
                }
            } else {
                Vector128 reg = (Vector128) FrameUtil.getObjectSafe(frame, xmm);
                reg.setF64(1, value);
            }
        } else {
            super.executeF64(frame, value);
        }
    }

    @Override
    public void executeI128(VirtualFrame frame, Vector128 value) {
        executeI128(frame, 3, value);
    }

    @Override
    public void executeI256(VirtualFrame frame, Vector256 value) {
        executeI256(frame, 1, value);
    }
}
