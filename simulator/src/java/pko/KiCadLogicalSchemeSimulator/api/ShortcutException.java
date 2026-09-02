/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.merger.MergerInput;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Collection;
import java.util.Comparator;

@Getter
public class ShortcutException extends RuntimeException {
    private final String message;

    public <T> ShortcutException(IModelItem<?> source, Integer state, Collection<? extends MergerInput<? extends T>> pins) {
        StringBuilder message = new StringBuilder(source.getClass().getName() + "\nSetting: <mask>:<state>:<pin>\n" +
                (source instanceof MergerInput<?> input ? Utils.LPad(16, '0', Integer.toBinaryString(input.getMask())) + ":" : "") +
                (source instanceof Pin ? (((Pin) source).strong ? "" : "W") + (state != 0 ? "1" : "0") : Utils.LPad(16, '0', Integer.toBinaryString(state))) + ":" +
                source.getName() + " Shortcut with: \n");
        pins.stream()
                .filter(pin -> pin != source).sorted(Comparator.comparingInt((MergerInput<? extends T> pin) -> pin.getMask())).forEach(pin -> {
                if (!((ModelItem<?>) pin).hiImpedance) {
                    message.append(Utils.LPad(16, '0', Integer.toBinaryString(pin.getMask()))).append(":");
                    if (((ModelItem<?>) pin).hiImpedance) {
                        message.append("H");
                    } else {
                        message.append(Utils.LPad(16, '0', Integer.toBinaryString(pin.getState())));
                        if (!pin.isStrong()) {
                            message.append("W");
                        }
                    }
                    message.append(":").append(pin.getName());
                    message.append(";\n");
                }
            });
        this.message = message.toString();
    }
}
