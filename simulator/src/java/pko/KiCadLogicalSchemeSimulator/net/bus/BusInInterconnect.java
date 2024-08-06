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
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;

public class BusInInterconnect extends InBus {
    public final long interconnectMask;
    public final long senseMask;
    public final long inverseInterconnectMask;
    public InBus destination;

    public BusInInterconnect(InBus destination, long interconnectMask, Byte offset) {
        super(destination, "interconnect" + interconnectMask);
        this.destination = destination;
        this.interconnectMask = interconnectMask;
        this.inverseInterconnectMask = ~interconnectMask;
        this.senseMask = 1L << offset;
    }

    @Override
    public void setState(long newState) {
        if ((newState & interconnectMask) != 0) {
            destination.state = newState | interconnectMask;
            destination.setState(destination.state);
        } else {
            destination.state = newState & inverseInterconnectMask;
            destination.setState(destination.state);
        }
        destination.hiImpedance = false;
    }

    @Override
    public void setHiImpedance() {
        destination.setHiImpedance();
        destination.hiImpedance = true;
    }

    @Override
    public InBus getOptimised() {
        destination = destination.getOptimised();
        return this;
    }
}
