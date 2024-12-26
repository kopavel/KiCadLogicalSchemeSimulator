/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.blocking;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncZigConsumer implements AutoCloseable {
    private static final MethodHandle INIT_QUEUE;
    private static final MethodHandle WRITE_QUEUE;
    private static final MethodHandle READ_QUEUE;
    private static final MethodHandle DEINIT_QUEUE;
    static {
        Arena arena = Arena.global();
        Linker linker = Linker.nativeLinker();
        SymbolLookup lookup =
                SymbolLookup.libraryLookup(Path.of("D:\\soft_a\\verilog\\KiCadLogicalSchemeSimulator\\zigQueue\\src\\zig\\jni.dll"), arena);// This loads the library
        INIT_QUEUE = linker.downcallHandle(lookup.findOrThrow("AsyncZigConsumer_initQueue"), FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        WRITE_QUEUE =
                linker.downcallHandle(lookup.findOrThrow("AsyncZigConsumer_writeNative"), FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
        READ_QUEUE = linker.downcallHandle(lookup.findOrThrow("AsyncZigConsumer_readNative"), FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
        DEINIT_QUEUE = linker.downcallHandle(lookup.findOrThrow("AsyncZigConsumer_deinitQueue"), FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG));
    }
    Thread consumerthread;
    AtomicBoolean run = new AtomicBoolean(true);
    private long nativeHandle; // Pointer to the native BusyWaitQueue instance

    // Initialize the queue with a specified capacity
    public AsyncZigConsumer(int size) throws Throwable {
        nativeHandle = initQueue(size);
        if (nativeHandle == 0) {
            throw new RuntimeException("Failed to initialize BusyWaitQueue");
        }
        consumerthread = Thread.ofPlatform().start(() -> {
            try {
                while (run.getOpaque()) {
                    consume(readNative(nativeHandle));
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Method to initialize the queue
    public static long initQueue(int size) throws Throwable {
        return (long) INIT_QUEUE.invokeExact(size);
    }

    // Method to write to the queue
    public static void writeNative(long cQueue, int value) throws Throwable {
        WRITE_QUEUE.invokeExact(cQueue, value);
    }

    // Method to read from the queue
    public static int readNative(long cQueue) throws Throwable {
        return (int) READ_QUEUE.invokeExact(cQueue);
    }

    // Method to deinitialize the queue
    public static void deinitQueue(long cQueue) throws Throwable {
        DEINIT_QUEUE.invokeExact(cQueue);
    }

    public abstract void consume(int payload);

    // Write a value to the queue
    public void accept(int value) throws Throwable {
        writeNative(nativeHandle, value);
    }

    // Free resources
    public void close() {
        run.setRelease(false);
        try {
            accept(0);
            consumerthread.join();
            if (nativeHandle != 0) {
                deinitQueue(nativeHandle);
                nativeHandle = 0;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
