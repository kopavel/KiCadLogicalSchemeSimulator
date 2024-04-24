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
package lv.pko.DigitalNetSimulator.api.pins.in;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.pins.Manipulable;
import lv.pko.DigitalNetSimulator.api.pins.Pin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;

public abstract class InPin extends Pin implements Manipulable {
    public long mask;
    //Fixme volatile??
    public long rawState;
    public byte offset;
    public OutPin source;
    public byte nOffset;

    public InPin(String id, Chip parent, int size, String... names) {
        super(id, parent, size, names);
    }

    public InPin(String id, Chip parent) {
        super(id, parent, 1);
    }

    public void addSource(OutPin source) {
        this.source = source;
        source.addDest(this);
    }

    public long getState() {
        return correctState(rawState);
    }

    public long correctState(long state) {
        if (offset == 0) {
            return state;
        } else if (offset > 0) {
            return state >> offset;
        } else {
            return state << nOffset;
        }
    }

    public void setOffset(byte offset) {
        this.offset = offset;
        this.nOffset = (byte) -offset;
    }

    public void transit(long oldState, long newState, boolean hiImpedance) {
        //todo make it on "OUT" side - it can be skipped, if OUT and IN are only one (in OutPin specifically).
        newState = newState & mask;
        rawState = newState;
        onChange(newState, hiImpedance);
    }

    abstract public void onChange(long newState, boolean hiImpedance);

    @Override
    public Object[] getConstructorParameters(Class<?>[] paramTypes) {
        if (paramTypes[0].isAssignableFrom(String.class)) {
            if (paramTypes.length == 4) {
                return new Object[]{id, parent, size, aliases.keySet().toArray(new String[0])};
            } else {
                return new Object[]{id, parent};
            }
        } else {
            //anonymous class
            if (paramTypes.length == 5) {
                return new Object[]{parent, id, parent, size, aliases.keySet().toArray(new String[0])};
            } else {
                return new Object[]{parent, id, parent};
            }
        }
    }

    //methods for using by Byte manipulation
    @SuppressWarnings("unused")
    protected long correctStateStateNoOffset(long state) {
        return (state & mask);
    }

    //methods for using by Byte manipulation
    @SuppressWarnings("unused")
    protected long correctStateStatePositiveOffset(long state) {
        return (state & mask) >> offset;
    }

    //methods for using by Byte manipulation
    @SuppressWarnings("unused")
    protected long correctStateStateNegativeOffset(long state) {
        return (state & mask) << nOffset;
    }
}