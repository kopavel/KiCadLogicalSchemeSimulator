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
package lv.pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import lv.pko.KiCadLogicalSchemeSimulator.Simulator;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.IModelItem;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.ModelInItem;

import javax.swing.*;
import java.util.List;

public class OscilloscopeMenu extends JMenuBar {
    private final Oscilloscope oscilloscope;

    public OscilloscopeMenu(Oscilloscope parent) {
        oscilloscope = parent;
        JMenu outPinMenu = new JMenu(Oscilloscope.localization.getString("outPins"));
        add(outPinMenu);
        JMenu inPinMenu = new JMenu(Oscilloscope.localization.getString("InPins"));
        add(inPinMenu);
        List<IModelItem> outPins = Simulator.model.schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .toList();
        for (IModelItem pin : outPins) {
            JMenuItem outPinItem = new JMenuItem(pin.getName());
            outPinItem.addActionListener(e -> oscilloscope.addPin(pin, pin.getName()));
            outPinMenu.add(outPinItem);
        }
        List<ModelInItem> inPins = Simulator.model.schemaParts.values()
                .stream()
                .flatMap(p -> p.inMap.values()
                        .stream())
                .toList();
        for (ModelInItem pin : inPins) {
            JMenuItem inPinItem = new JMenuItem(pin.getName());
            inPinItem.addActionListener(e -> oscilloscope.addPin(pin, pin.getName()));
            inPinMenu.add(inPinItem);
        }
    }
}
