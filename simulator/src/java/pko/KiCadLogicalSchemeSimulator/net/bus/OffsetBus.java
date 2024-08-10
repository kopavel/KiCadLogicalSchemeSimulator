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
package pko.KiCadLogicalSchemeSimulator.net.bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.net.javaCompiller.ClassOptimiser;

public class OffsetBus extends Bus {
    protected byte offset;
    protected Bus destination;

    public OffsetBus(Bus destination, byte offset) {
        super(destination, "offset" + offset);
        if (offset == 0) {
            throw new RuntimeException("Offset must not be 0");
        }
        this.destination = destination;
        this.offset = offset;
        id += ":offset" + offset;
    }

    /*Optimiser constructor*/
    public OffsetBus(OffsetBus oldBus, String variantId) {
        super(oldBus, variantId);
        offset = oldBus.offset;
        destination = oldBus.destination;
    }

    @Override
    public void setState(long newState) {
        /*Optimiser bind offset*/
        destination.setState(newState << offset);
    }

    @Override
    public void setHiImpedance() {
        destination.setHiImpedance();
    }

    @Override
    public Bus getOptimised() {
        destination = destination.getOptimised();
        return new ClassOptimiser<>(this).bind("offset", String.valueOf(offset)).build();
    }
}
