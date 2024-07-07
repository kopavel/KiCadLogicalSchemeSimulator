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
package lv.pko.KiCadLogicalSchemeSimulator.api.pins;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Pin {
    public final String id;
    public final SchemaPart parent;
    public final int size;
    //Fixme volatile??
    public long state;
    public long mask;
    public Map<String, Byte> aliasOffsets = new HashMap<>();
    public boolean useBitPresentation;

    public Pin(String id, SchemaPart parent, int size, String... names) {
        this.id = id;
        this.parent = parent;
        this.size = size;
        if (names == null || names.length == 0) {
            if (size == 1) {
                aliasOffsets.put(id, (byte) 0);
            } else {
                for (byte i = 0; i < size; i++) {
                    aliasOffsets.put(id + i, i);
                }
            }
        } else if (names.length != size) {
            throw new RuntimeException("Pin definition Error, Names amount not equal size, pin" + getName());
        } else if (size == 1) {
            aliasOffsets = Collections.singletonMap(id, (byte) 0);
        } else {
            for (byte i = 0; i < names.length; i++) {
                aliasOffsets.put(names[i], i);
            }
        }
    }

    public String getName() {
        return parent.id + "_" + id;
    }
}
