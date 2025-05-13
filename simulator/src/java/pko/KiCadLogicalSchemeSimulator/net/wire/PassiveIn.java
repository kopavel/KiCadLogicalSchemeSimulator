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
package pko.KiCadLogicalSchemeSimulator.net.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class PassiveIn extends InPin {
    public final PassivePin destination;

    public PassiveIn(PassivePin destination) {
        super(destination, null);//always after merger - so no parent.
        this.destination = destination;
        CheckShortCut checker = new CheckShortCut(destination);
        destination.addDestination(checker);
        triStateIn = true;
    }

    @Override
    public void setHi() {
        destination.otherState = true;
        destination.otherImpedance = false;
        if (source != null) {
            destination.otherStrong = ((Pin) source).strong;
        } else {
            destination.otherStrong = strong;
        }
        destination.onChange();
    }

    @Override
    public void setLo() {
        destination.otherState = false;
        destination.otherImpedance = false;
        if (source != null) {
            destination.otherStrong = ((Pin) source).strong;
        } else {
            destination.otherStrong = strong;
        }
        destination.onChange();
    }

    @Override
    public void setHiImpedance() {
        destination.otherImpedance = true;
    }

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        return this;
    }

    private static final class CheckShortCut extends InPin {
        private final PassivePin source;

        private CheckShortCut(PassivePin source) {
            super(source, "ShortcutChecker");
            this.source = source;
        }

        @Override
        public void setHi() {
            if (source != null && !source.hiImpedance) {
                throw new ShortcutException(source.destinations);
            }
        }

        @Override
        public void setLo() {
            if (source != null && !source.hiImpedance) {
                throw new ShortcutException(source.destinations);
            }
        }
    }
}
