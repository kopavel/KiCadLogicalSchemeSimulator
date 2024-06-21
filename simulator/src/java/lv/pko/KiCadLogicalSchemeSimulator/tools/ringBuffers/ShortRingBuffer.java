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
package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class ShortRingBuffer extends RingBuffer {
    private final short[] data;

    public ShortRingBuffer() {
        this(0);
    }

    public ShortRingBuffer(int capacity) {
        super((capacity < 1) ? DEFAULT_CAPACITY : capacity);
        this.data = new short[this.capacity];
    }

    @Override
    public void put(long element) {
        super.put(element);
        data[writePos] = (short) element;
    }

    @Override
    public IRingBufferSlice take(int offset, int amount) {
        int snapWritePos = writePos;
        int available = isFull ? capacity : snapWritePos + 1;
        if (offset > available) {
            offset = available;
        }
        if (amount > offset) {
            amount = offset;
        }
        short[] retVal = new short[amount];
        offset--;
        if (offset <= snapWritePos) {
            System.arraycopy(data, snapWritePos - offset, retVal, 0, amount);
        } else {
            int tailLength = offset - snapWritePos;
            if (amount <= tailLength) {
                System.arraycopy(data, capacity - tailLength - 1, retVal, 0, amount);
            } else {
                System.arraycopy(data, capacity - tailLength - 1, retVal, 0, tailLength);
                System.arraycopy(data, 0, retVal, tailLength, amount - tailLength);
            }
        }
        return new ShortBufferSlice(retVal);
    }
}
