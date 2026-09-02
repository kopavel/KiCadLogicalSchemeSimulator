/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.diode.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class DiodeTest extends NetTester {
    private static final String[] anodeIn =
            {"hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1"};
    private static final String[] cathodeIn =
            {"hi", "hi", "hi", "hi", "hi", "w0", "w0", "w0", "w0", "w0", "w1", "w1", "w1", "w1", "w1", "s0", "s0", "s0", "s0", "s0", "s1", "s1", "s1", "s1", "s1"};
    private static final String[] anodeState =
            {"hi", "w0", "w1", "s0", "s1", "w0", "w0", "er", "s0", "s1", "hi", "er", "w1", "s0", "s1", "s0", "s0", "s0", "er", "er", "hi", "w0", "w1", "s0", "er"};
    private static final String[] cathodeState =
            {"hi", "hi", "w1", "hi", "s1", "w0", "w0", "er", "w0", "s1", "w1", "er", "w1", "w1", "s1", "s0", "s0", "s0", "er", "er", "s1", "s1", "s1", "s1", "er"};

    @Override
    protected String getNetFilePath() {
        return "test/resources/diode.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void diodeTest() {
        Pin anode = outPin("A");
        Pin cathode = outPin("K");
        for (int i = 0; i < anodeState.length; i++) {
            if ("er".equals(anodeState[i])) {
                continue;
            }
            switch (anodeIn[i]) {
                case "hi" -> {
                    if (!anode.hiImpedance) {
                        anode.setHiImpedance();
                    }
                }
                case "w0" -> anode.setLo(false);
                case "w1" -> anode.setHi(false);
                case "s0" -> anode.setLo(true);
                case "s1" -> anode.setHi(true);
            }
            switch (cathodeIn[i]) {
                case "hi" -> {
                    if (!cathode.hiImpedance) {
                        cathode.setHiImpedance();
                    }
                }
                case "w0" -> {
                    if (cathode.strong || cathode.state) {
                        cathode.setLo(false);
                    }
                }
                case "w1" -> {
                    if (cathode.strong || !cathode.state) {
                        cathode.setHi(false);
                    }
                }
                case "s0" -> {
                    if (!cathode.strong || cathode.state) {
                        cathode.setLo(true);
                    }
                }
                case "s1" -> {
                    if (!cathode.strong || !cathode.state) {
                        cathode.setHi(true);
                    }
                }
            }
            switch (anodeState[i]) {
                case "hi" -> checkPinImpedance("inA", "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    checkPin("inA", false, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    checkPin("inA", true, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    checkPin("inA", false, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    checkPin("inA", true, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be Strong Hi");
                }
            }
            switch (cathodeState[i]) {
                case "hi" -> checkPinImpedance("inK", "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    checkPin("inK", false, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    checkPin("inK", true, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    checkPin("inK", false, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    checkPin("inK", true, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be Strong Hi");
                }
            }
        }
    }
}
