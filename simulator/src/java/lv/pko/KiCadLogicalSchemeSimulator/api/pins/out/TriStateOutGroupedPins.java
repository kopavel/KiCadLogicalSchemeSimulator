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
package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class TriStateOutGroupedPins extends TriStateOutPin {
    public MaskGroup[] groups = new MaskGroup[0];

    public TriStateOutGroupedPins(TriStateOutPin oldPin) {
        super(oldPin.id, oldPin.parent, oldPin.size);
        aliases = oldPin.aliases;
    }

    @Override
    public void addDest(InPin pin) {
        if (pin instanceof EdgeInPin) {
            throw new RuntimeException("Edge pin on tri-state out");
        }
        MaskGroup destGroup = null;
        for (MaskGroup group : groups) {
            if (group.mask == pin.mask) {
                destGroup = group;
                break;
            }
        }
        if (destGroup == null) {
            groups = Utils.addToArray(groups, new MaskGroup(pin));
        } else {
            destGroup.addDest(pin);
        }
    }

    @Override
    public void setState(long newState) {
        if (hiImpedance) {
            hiImpedance = false;
            this.state = newState;
            for (MaskGroup group : groups) {
                group.oldVal = newState & group.mask;
                for (InPin InPin : group.dest) {
                    InPin.transit(group.oldVal, false);
                }
            }
        } else {
            if (newState != this.state) {
                this.state = newState;
                for (MaskGroup group : groups) {
                    long maskState = newState & group.mask;
                    if (group.oldVal != maskState) {
                        group.oldVal = maskState;
                        for (InPin InPin : group.dest) {
                            InPin.transit(maskState, false);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void reSendState() {
        for (MaskGroup group : groups) {
            group.oldVal = state & group.mask;
            for (InPin pin : group.dest) {
                pin.transit(group.oldVal, hiImpedance);
            }
        }
    }

    @Override
    public void setHiImpedance() {
        if (!hiImpedance) {
            hiImpedance = true;
            for (MaskGroup group : groups) {
                for (InPin inPin : group.dest) {
                    inPin.transit(0, true);
                }
            }
        }
    }

    @Override
    public boolean noDest() {
        return groups.length == 0;
    }
}
