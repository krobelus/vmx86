package org.graalvm.vm.x86.nfi;

import org.graalvm.vm.memory.MemoryPage;
import org.graalvm.vm.memory.VirtualMemory;
import org.graalvm.vm.x86.AMD64;
import org.graalvm.vm.x86.AMD64Context;
import org.graalvm.vm.x86.InteropFunctionPointers;
import org.graalvm.vm.x86.node.AMD64Node;

import com.everyware.posix.api.PosixException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;

public class AMD64ContextInitializerNode extends AMD64Node {
    @Child private InterpreterStartNode interpreter = new InterpreterStartNode();

    public void execute(VirtualFrame frame) {
        ContextReference<AMD64Context> ctxref = getContextReference();
        AMD64Context ctx = ctxref.get();
        VirtualMemory mem = ctx.getMemory();

        InteropFunctionPointers ptrs = ctx.getInteropFunctionPointers();
        if (ptrs == null) {
            ptrs = interpreter.execute(frame);
            ctx.setInteropFunctionPointers(ptrs);

            long len = mem.roundToPageSize(AMD64.SCRATCH_SIZE);
            MemoryPage scratch = mem.allocate(len, "[scratch]");
            try {
                mem.mprotect(scratch.base, scratch.size, true, true, false);
            } catch (PosixException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(e);
            }
            ctx.setScratchMemory(scratch.base);

            MemoryPage callbacks = mem.allocate(8192, "[callbacks]");
            for (int i = 0; i < 128; i++) {
                CallbackCode.writeCallback(mem, callbacks.base + Addresses.OFFSET_CALLBACKS, i);
            }
            TruffleNativeAPI.writeCode(mem, callbacks.base + Addresses.OFFSET_TRUFFLENATIVEAPI_CODE);
            TruffleNativeAPI.writeStruct(mem, callbacks.base + Addresses.OFFSET_TRUFFLENATIVEAPI_STRUCT, callbacks.base + Addresses.OFFSET_TRUFFLENATIVEAPI_CODE);
            mem.setI64(callbacks.base + Addresses.OFFSET_TRUFFLENATIVEAPI_PTR, callbacks.base + Addresses.OFFSET_TRUFFLENATIVEAPI_STRUCT);
            try {
                mem.mprotect(callbacks.base, callbacks.size, true, true, true);
            } catch (PosixException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(e);
            }
            ctx.setCallbackMemory(callbacks.base);
        }
    }
}
