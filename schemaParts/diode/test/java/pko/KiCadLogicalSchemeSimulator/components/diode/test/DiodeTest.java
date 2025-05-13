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
        return "test/resources/Diode.net";
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
                case "w0" -> {
                    anode.strong = false;
                    anode.setLo();
                }
                case "w1" -> {
                    anode.strong = false;
                    anode.setHi();
                }
                case "s0" -> {
                    anode.strong = true;
                    anode.setLo();
                }
                case "s1" -> {
                    anode.strong = true;
                    anode.setHi();
                }
            }
            switch (cathodeIn[i]) {
                case "hi" -> {
                    if (!cathode.hiImpedance) {
                        cathode.setHiImpedance();
                    }
                }
                case "w0" -> {
                    if (cathode.strong || cathode.state) {
                        cathode.strong = false;
                        cathode.setLo();
                    }
                }
                case "w1" -> {
                    if (cathode.strong || !cathode.state) {
                        cathode.strong = false;
                        cathode.setHi();
                    }
                }
                case "s0" -> {
                    if (!cathode.strong || cathode.state) {
                        cathode.strong = true;
                        cathode.setLo();
                    }
                }
                case "s1" -> {
                    if (!cathode.strong || !cathode.state) {
                        cathode.strong = true;
                        cathode.setHi();
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
