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
        StringBuilder message = new StringBuilder(
                "Setting:\n" + (source instanceof MergerInput<?> input ? Utils.LPad(16, '0', Integer.toBinaryString(input.getMask())) + ":" : "") +
                        source.getName() + ":" +
                        (source instanceof Pin ? (((Pin) source).strong ? "" : "W") + (state > 0 ? "1" : "0") : Utils.LPad(16, '0', Integer.toBinaryString(state))) +
                        " Shortcut with: \n");
        int[] states = {0};
        pins.stream().filter(pin -> pin!=source).sorted(Comparator.comparingInt((MergerInput<? extends T> pin) -> pin.getMask())).forEach(pin -> {
            if (!pin.isHiImpedance() && (states[0] & pin.getMask()) == 0) {
                states[0] |= pin.getMask();
                message.append(Utils.LPad(16, '0', Integer.toBinaryString(pin.getMask()))).append(":");
                message.append(pin.getName()).append(":");
                if (pin.isHiImpedance()) {
                    message.append("H");
                } else {
                    if (!pin.isStrong()) {
                        message.append("W");
                    }
                    message.append(Utils.LPad(16, '0', Integer.toBinaryString(pin.getState())));
                }
                message.append(";\n");
            }
        });
        this.message = message.toString();
    }
}
