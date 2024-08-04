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
package pko.KiCadLogicalSchemeSimulator.components.LED;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import java.awt.*;

public class Led extends SchemaPart implements InteractiveSchemaPart {
    private final LedUiComponent ledUiComponent;

    protected Led(String id, String sParams) {
        super(id, sParams);
        addInPin(new NoFloatingInPin("IN", this) {
            @Override
            public void setState(boolean newState) {
                state = newState;
                ledUiComponent.setState(state);
            }
        });
        int size = Integer.parseInt(params.getOrDefault("size", "20"));
        Color on = UiTools.getColor(params.getOrDefault("onColor", "#808080"));
        Color off = UiTools.getColor(params.getOrDefault("offColor", "#ff0000"));
        ledUiComponent = new LedUiComponent(size, on, off, id);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return ledUiComponent;
    }
}
