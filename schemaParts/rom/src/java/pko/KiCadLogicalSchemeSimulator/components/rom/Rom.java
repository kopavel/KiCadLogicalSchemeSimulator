/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    public final RomCsPin[] csPins;
    private final int size;
    private final int aSize;
    public RomABus aBus;
    public int csCount = 1;

    protected Rom(String id, String sParam) {
        super(id, sParam);
        if (!sParam.contains("file")) {
            throw new RuntimeException("Rom component " + id + " need \"file\" parameter");
        }
        if (!sParam.contains("size")) {
            throw new RuntimeException("Rom component " + id + " need \"size\" parameter");
        }
        if (sParam.contains("csCount")) {
            try {
                csCount = Integer.parseInt(params.get("csCount"));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Rom component " + id + " \"csCount\" parameter must be numeric");
            }
        }
        if (!sParam.contains("aSize")) {
            throw new RuntimeException("Rom component " + id + " need \"aSize\" parameter");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Rom component " + id + " \"size\" parameter must be numeric");
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
        } catch (NumberFormatException e) {
            throw new RuntimeException("Rom component " + id + " \"aSize\" parameter must be numeric");
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
        csPins = new RomCsPin[csCount];
        for (int i = 0; i < csCount; i++) {
            String name = csCount == 1 ? "CS" : "CS" + (i + 1);
            csPins[i] = addInPin(new RomCsPin(name, this));
        }
    }

    @Override
    public void initOuts() {
        Bus dBus = getOutBus("D");
        dBus.hiImpedance = nReverse;
        aBus.iCsActive = reverse ? 0 : csPins.length;
        aBus.bCsActive = reverse;
        aBus.dBus = dBus;
        for (RomCsPin csPin : csPins) {
            csPin.dBus = dBus;
        }
    }

    @Override
    public String extraState() {
        return "A:" + String.format("%0" + (int) Math.ceil(aSize / 4.0d) + "X", aBus.state) + (aBus.iCsActive > 0 ? ("\nD:" + (aBus.state >= words.length
                                                                                                                               ? "OutOfRange"
                                                                                                                               : String.format("%0" +
                                                                                                                                               (int) Math.ceil(
                                                                                                                                                       size / 4.0d) +
                                                                                                                                               "X",
                                                                                                                                       words[aBus.state]))) : "");
    }

    @Override
    public Supplier<JPanel> extraPanel() {
        return () -> new MemoryDumpPanel(words);
    }
}
