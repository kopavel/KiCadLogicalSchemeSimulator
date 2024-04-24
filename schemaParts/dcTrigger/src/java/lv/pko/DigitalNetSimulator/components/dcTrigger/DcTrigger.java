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
package lv.pko.DigitalNetSimulator.components.dcTrigger;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.pins.in.FallingEdgeInPin;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.in.RisingEdgeInPin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;

public class DcTrigger extends Chip {
    private final InPin dPin;
    private OutPin qOut;
    private OutPin iqOut;

    protected DcTrigger(String id, String sParam) {
        super(id, sParam);
        dPin = addInPin("D", 1);
        if (reverse) {
            addInPin(new FallingEdgeInPin("C", this) {
                @Override
                public void onFallingEdge() {
                    store();
                }
            });
        } else {
            addInPin(new RisingEdgeInPin("C", this) {
                @Override
                public void onRisingEdge() {
                    store();
                }
            });
        }
        addOutPin("Q", 1);
        addOutPin("~{Q}", 1);
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        qOut.state = 0;
        iqOut = getOutPin("~{Q}");
        iqOut.state = 1;
    }

    private void store() {
        long dState = dPin.rawState;
        if (dState > 0) {
            qOut.setState(1);
            iqOut.setState(0);
        } else {
            qOut.setState(0);
            iqOut.setState(1);
        }
    }
}