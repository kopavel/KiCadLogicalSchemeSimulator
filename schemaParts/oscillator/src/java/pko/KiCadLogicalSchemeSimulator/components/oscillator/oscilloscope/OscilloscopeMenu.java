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
package pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.ui.main.MainMenu.mainI81n;

public class OscilloscopeMenu extends JMenuBar {
    private final Oscilloscope oscilloscope;

    public OscilloscopeMenu(Oscilloscope parent) {
        oscilloscope = parent;
        Map<Character, JMenu> letterMenus = new HashMap<>();
        JMenu schemaParts = new JMenu(mainI81n.getString("schemaParts"));
        add(schemaParts);
        for (SchemaPart schemaPart : Simulator.net.schemaParts.values()) {
            char firstLetter = Character.toUpperCase(schemaPart.id.charAt(0));
            letterMenus.putIfAbsent(firstLetter, new JMenu(String.valueOf(firstLetter)));
            JMenu schemaPartItem = new JMenu(schemaPart.id);
            letterMenus.get(firstLetter).add(schemaPartItem);
            schemaPartItem.add(new JLabel(Oscilloscope.localization.getString("inPins")));
            schemaPart.inPins.values()
                    .stream().distinct().sorted().forEach(inItem -> {
                          JMenuItem inPinItem = new JMenuItem(schemaPart.ids.get(inItem));
                          inPinItem.addActionListener(e -> oscilloscope.addPin(inItem, schemaPart.ids.get(inItem)));
                          schemaPartItem.add(inPinItem);
                      });
            schemaPartItem.addSeparator();
            schemaPartItem.add(new JLabel(Oscilloscope.localization.getString("outPins")));
            schemaPart.outPins.values()
                    .stream().distinct().sorted().forEach(inItem -> {
                          JMenuItem inPinItem = new JMenuItem(schemaPart.ids.get(inItem));
                          inPinItem.addActionListener(e -> oscilloscope.addPin(inItem, schemaPart.ids.get(inItem)));
                          schemaPartItem.add(inPinItem);
                      });
        }
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            if (letterMenus.containsKey(letter)) {
                schemaParts.add(letterMenus.get(letter));
            }
        }
    }
}
