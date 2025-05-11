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
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.MemoryDumpPanel;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public class Rom extends SchemaPart {
    public final int[] words;
    public final RomABus aBus;
    public final RomCsPin csPin;
    private int size;
    private int aSize;

    protected Rom(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("file")) {
            throw new RuntimeException("Rom component " + id + " need \"file\" parameter");
        }
        if (!sParam.contains("size")) {
            throw new RuntimeException("Rom component " + id + " need \"size\" parameter");
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Rom component " + id + " need \"aSize\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException ignore) {
        }
        int mask = Utils.getMaskForSize(size);
        if (size < 1) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (size > 32) {
            throw new RuntimeException("Component " + id + " max size is 32");
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
        words = new int[romSize];
        for (int i = 0; i < romSize; i++) {
            words[i] = 0;
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] fileBytes = is.readAllBytes();
            if (fileBytes.length > romSize) {
                throw new RuntimeException("Rom component " + id + " file size (" + fileBytes.length + ") is bigger, then Rom size(" + romSize + ")");
            } else if (fileBytes.length < romSize) {
                Log.warn(Rom.class, "Rom component {} file size ({}) is smaller, then Rom size ({})", id, fileBytes.length, romSize);
            }
            if (size < 9) {
                for (int i = 0; i < fileBytes.length; i++) {
                    words[i] = fileBytes[i] & mask;
                }
            } else {
                int wordSize = size / 8;
                for (int pos = 0; pos < romSize; pos++) {
                    int word = 0;
                    for (int j = 0; j < wordSize; j++) {
                        word = word << 8 | fileBytes[pos * wordSize + j];
                    }
                    words[pos] = word & mask;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't load file " + file, e);
        }
        addTriStateOutBus("D", size);
        aBus = addInBus(new RomABus("A", this, aSize));
        if (reverse) {
            csPin = addInPin(new RomNCsPin("~{CS}", this));
        } else {
            csPin = addInPin(new RomCsPin("CS", this));
        }
    }

    @Override
    public void initOuts() {
        Bus dBus = getOutBus("D");
        dBus.hiImpedance=nReverse;
        aBus.csActive=reverse;
        aBus.dBus = dBus;
        csPin.dBus = dBus;
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(aSize / 4.0d) + "X", aBus.state) + (aBus.csActive
                                                                                               ? ("\nD:" +
                (aBus.state >= words.length ? "OutOfRange" : String.format("%0" + (int) Math.ceil(size / 4.0d) + "X", words[aBus.state])))
                                                                                               : "");
    }

    @Override
    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(words);
    }
}
