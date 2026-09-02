/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.HashMap;
import java.util.Map;

public class Keyboard extends SchemaPart implements InteractiveSchemaPart {
    private final KbdIn[] ins = new KbdIn[8];
    final InPin enable;
    private final Map<String, KeyDescriptor> keyDescriptors = new HashMap<>();
    private final KeyboardUiComponent keyboardUiComponent;
    private final OutState[] outs = new OutState[8];
    private Pin event;

    protected Keyboard(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("map")) {
            throw new RuntimeException("Component " + id + " must have \"map\" parameter");
        }
        String[] keys = params.get("map").split("\\|");
        for (String key : keys) {
            String[] keyDesc = key.split("_");
            int rMask = 1 << (keyDesc[1].charAt(0) - '0');
            int cMask = 1 << (keyDesc[1].charAt(1) - '0');
            keyDescriptors.put(keyDesc[0], new KeyDescriptor(keyDesc[1]));
        }
        enable = addInPin(new InPin("En", this) {
            @Override
            public void setHi() {
                state = true;
                for (OutState state : outs) {
                    state.disable();
                }
            }

            @Override
            public void setLo() {
                state = false;
                for (OutState state : outs) {
                    state.setOut();
                }
            }
        });
        for (int i = 0; i < 8; i++) {
            ins[i] = addInPin(new KbdIn(i, this));
            addTriStateOutPin("Out" + i);
        }
        addOutPin("Ev");
        keyboardUiComponent = new KeyboardUiComponent(id, 125, this);
    }

    public void keyEvent(String text, boolean pressed) {
        if (keyDescriptors.containsKey(text)) {
            event.setHi();
            KeyDescriptor descriptor = keyDescriptors.get(text);
                if (pressed) {
                    ins[descriptor.rNo].addState(outs[descriptor.cNo]);
                } else {
                    ins[descriptor.rNo].removeState(outs[descriptor.cNo]);
                }
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < 8; i++) {
            outs[i] = new OutState(getOutPin("Out" + i),enable);
        }
        event = getOutPin("Ev");
    }

    @Override
    public AbstractUiComponent getComponent() {
        return keyboardUiComponent;
    }

    @AllArgsConstructor
    private static class KeyDescriptor {
        public int rNo;
        public int cNo;

        public KeyDescriptor(String desc) {
            rNo = desc.charAt(0) - '0';
            cNo = desc.charAt(1) - '0';
        }
    }
}
