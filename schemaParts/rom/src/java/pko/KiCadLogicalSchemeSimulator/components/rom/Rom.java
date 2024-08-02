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
package pko.KiCadLogicalSchemeSimulator.components.rom;
import pko.KiCadLogicalSchemeSimulator.api.FloatingInException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.EdgeInPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Rom extends SchemaPart {
    private final long[] words;
    private Bus outPin;
    private int addr;
    private boolean csActive;
    private int size;
    private int aSize;

    protected Rom(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("file")) {
            throw new RuntimeException("Rom component need \"file\" parameter");
        }
        if (!sParam.contains("size")) {
            throw new RuntimeException("Rom component need \"size\" parameter");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Rom component need \"size\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException ignore) {
        }
        long mask = Utils.getMaskForSize(size);
        if (size < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (size > 64) {
            throw new RuntimeException("Component " + id + " max size is 64");
        }
        try {
            aSize = Integer.parseInt(params.get("aSize"));
        } catch (NumberFormatException ignore) {
        }
        if (aSize < 1) {
            throw new RuntimeException("Component " + id + " aSize must be positive number");
        }
        if (aSize > 31) {
            throw new RuntimeException("Component " + id + " max aSize is 31");
        }
        String file = params.get("file");
        int romSize = (int) Math.pow(2, aSize);
        words = new long[romSize];
        for (int i = 0; i < romSize; i++) {
            words[i] = 0;
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] fileBytes = is.readAllBytes();
            if (size < 9) {
                for (int i = 0; i < fileBytes.length; i++) {
                    words[i] = fileBytes[i] & mask;
                }
            } else {
                int wordSize = size / 8;
                for (int pos = 0; pos < romSize; pos++) {
                    long word = 0;
                    for (int j = 0; j < wordSize; j++) {
                        word = word << 8 | fileBytes[pos * wordSize + j];
                    }
                    words[pos] = word & mask;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't load file " + file, e);
        }
        addInBus(new InBus("A", this, aSize) {
            @Override
            public void setHiImpedance() {
                hiImpedance = true;
                if (csActive) {
                    throw new FloatingInException(this);
                }
            }

            @Override
            public void setState(long newState) {
                addr = (int) newState;
                state = newState;
                if (csActive) {
                    if (outPin.state != words[addr]) {
                        outPin.state = words[addr];
                        outPin.setState(outPin.state);
                        outPin.hiImpedance = false;
                    }
                }
                hiImpedance = false;
            }
        });
        addOutBus("D", size);
        if (reverse) {
            addInPin(new EdgeInPin("~{CS}", this) {
                @Override
                public void onFallingEdge() {
                    csActive = true;
                    if (outPin.hiImpedance || outPin.state != words[addr]) {
                        outPin.state = words[addr];
                        outPin.setState(outPin.state);
                        outPin.hiImpedance = false;
                    }
                }

                @Override
                public void onRisingEdge() {
                    csActive = false;
                    if (!outPin.hiImpedance) {
                        outPin.setHiImpedance();
                        outPin.hiImpedance = true;
                    }
                }
            });
        } else {
            addInPin(new EdgeInPin("CS", this) {
                @Override
                public void onFallingEdge() {
                    csActive = false;
                    if (!outPin.hiImpedance) {
                        outPin.setHiImpedance();
                        outPin.hiImpedance = true;
                    }
                }

                @Override
                public void onRisingEdge() {
                    csActive = true;
                    if (outPin.hiImpedance || outPin.state != words[addr]) {
                        outPin.state = words[addr];
                        outPin.setState(outPin.state);
                        outPin.hiImpedance = false;
                    }
                }
            });
        }
    }

    @Override
    public void initOuts() {
        outPin = getOutBus("D");
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(aSize / 4d) + "X", addr) +
                (csActive ? ("\nD:" + (addr >= words.length ? "OutOfRange" : String.format("%0" + (int) Math.ceil(size / 4d) + "X", words[addr]))) : "");
    }
}
