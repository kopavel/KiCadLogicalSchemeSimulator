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
package pko.KiCadLogicalSchemeSimulator.components.led16Segment;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class Led16SegmentDisplay extends SchemaPart implements InteractiveSchemaPart {
    private final Led16SegmentDisplayUiComponent led16SegmentDisplayUiComponent;
    public int segments;

    protected Led16SegmentDisplay(String id, String sParams) {
        super(id, sParams);
        addInPin(new Led16SegmentDisplayInPin("A1", this, 0));
        addInPin(new Led16SegmentDisplayInPin("A2", this, 1));
        addInPin(new Led16SegmentDisplayInPin("B", this, 8));
        addInPin(new Led16SegmentDisplayInPin("C", this, 11));
        addInPin(new Led16SegmentDisplayInPin("D1", this, 2));
        addInPin(new Led16SegmentDisplayInPin("D2", this, 3));
        addInPin(new Led16SegmentDisplayInPin("E", this, 9));
        addInPin(new Led16SegmentDisplayInPin("F", this, 6));
        addInPin(new Led16SegmentDisplayInPin("G1", this, 4));
        addInPin(new Led16SegmentDisplayInPin("G2", this, 4));
        addInPin(new Led16SegmentDisplayInPin("H", this, 12));
        addInPin(new Led16SegmentDisplayInPin("I", this, 7));
        addInPin(new Led16SegmentDisplayInPin("J", this, 13));
        addInPin(new Led16SegmentDisplayInPin("K", this, 14));
        addInPin(new Led16SegmentDisplayInPin("L", this, 10));
        addInPin(new Led16SegmentDisplayInPin("M", this, 15));
        addInPin(new Led16SegmentDisplayInPin("DP", this, 16));
        int size = Integer.parseInt(params.getOrDefault("size", "60"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#ff0000"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#808080"));
        led16SegmentDisplayUiComponent = new Led16SegmentDisplayUiComponent(size, on, off, id);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return led16SegmentDisplayUiComponent;
    }
}
