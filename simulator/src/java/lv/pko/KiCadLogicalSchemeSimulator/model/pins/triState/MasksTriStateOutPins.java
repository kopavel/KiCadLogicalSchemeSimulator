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
package lv.pko.KiCadLogicalSchemeSimulator.model.pins.triState;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.EdgeInPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups.MaskGroupPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups.MaskGroupPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups.SameMaskGroupPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups.SameMaskGroupPins;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MasksTriStateOutPins extends TriStateOutPin {
    public MaskGroupPin[] groups = new MaskGroupPin[0];

    public MasksTriStateOutPins(TriStateOutPin oldPin) {
        super(oldPin.id, oldPin.parent, oldPin.size);
        aliases = oldPin.aliases;
        hiImpedance = oldPin.hiImpedance;
        state = oldPin.state;
        mask = oldPin.mask;
    }

    @Override
    public void addDest(InPin pin) {
        if (pin instanceof EdgeInPin) {
            throw new RuntimeException("Edge pin on tri-state out");
        }
        int destGroupId = -1;
        for (int i = 0; i < groups.length; i++) {
            if (groups[i].mask == pin.mask) {
                destGroupId = i;
                break;
            }
        }
        if (destGroupId == -1) {
            groups = Utils.addToArray(groups, (mask == pin.mask) ? new SameMaskGroupPin(pin) : new MaskGroupPin(pin));
        } else {
            MaskGroupPins targetGroup;
            if (groups[destGroupId] instanceof MaskGroupPins pins) {
                targetGroup = pins;
            } else {
                targetGroup = (mask == groups[destGroupId].mask) ? new SameMaskGroupPins(groups[destGroupId]) : new MaskGroupPins(groups[destGroupId]);
                groups[destGroupId] = targetGroup;
            }
            targetGroup.addDest(pin);
        }
    }

    @Override
    public void setState(long newState) {
        if (hiImpedance) {
            hiImpedance = false;
            state = newState;
            for (MaskGroupPin group : groups) {
                group.onChangeForce(newState);
            }
        } else if (newState != state) {
            state = newState;
            for (MaskGroupPin group : groups) {
                group.onChange(newState);
            }
        }
    }

    @Override
    public void setStateForce(long newState) {
        hiImpedance = false;
        state = newState;
        for (MaskGroupPin group : groups) {
            group.onChange(newState);
        }
    }

    @Override
    public void reSendState() {
        RuntimeException result = null;
        for (MaskGroupPin group : groups) {
            try {
                group.resend(state, hiImpedance);
            } catch (FloatingPinException | ShortcutException e) {
                if (result == null) {
                    result = e;
                }
            }
        }
        if (result != null) {
            throw result;
        }
    }

    @Override
    public void setHiImpedance() {
        if (!hiImpedance) {
            hiImpedance = true;
            for (MaskGroupPin group : groups) {
                group.setHiImpedance();
            }
        }
    }

    @Override
    public boolean noDest() {
        return groups.length == 0;
    }
}
