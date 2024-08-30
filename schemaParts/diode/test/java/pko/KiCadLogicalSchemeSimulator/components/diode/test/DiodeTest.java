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
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiodeTest extends NetTester {
    String[] anodeIn =
            {"h", "w0", "w1", "s0", "s1", "h", "w0", "w1", "s0", "s1", "h", "w0", "w1", "s0", "s1", "h", "w0", "w1", "s0", "s1", "h", "w0", "w1", "s0", "s1"};
    String[] cathodeIn =
            {"h", "h", "h", "h", "h", "w0", "w0", "w0", "w0", "w0", "w1", "w1", "w1", "w1", "w1", "s0", "s0", "s0", "s0", "s0", "s1", "s1", "s1", "s1", "s1"};
    String[] anodeState =
            {"h", "w0", "w1", "s0", "s1", "w0", "w0", "er", "s0", "s1", "h", "er", "w1", "s0", "s1", "s0", "s0", "s0", "er", "er", "h", "w0", "w1", "s0", "er"};
    String[] cathodeState =
            {"h", "h", "w1", "h", "s1", "w0", "w0", "er", "w0", "s1", "w1", "er", "w1", "w1", "s1", "s0", "s0", "s0", "er", "er", "s1", "s1", "s1", "s1", "er"};

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
        for (int i = 0; i < anodeState.length; i++) {
            if (anodeState[i].equals("er")) {
                continue;
            }
            if (!outPin("A").hiImpedance) {
                outPin("A").setHiImpedance();
            }
            switch (anodeIn[i]) {
                case "h" -> {
                }
                case "w0" -> {
                    outPin("A").strong = false;
                    setPin("A", false);
                }
                case "w1" -> {
                    outPin("A").strong = false;
                    setPin("A", true);
                }
                case "s0" -> {
                    outPin("A").strong = true;
                    setPin("A", false);
                }
                case "s1" -> {
                    outPin("A").strong = true;
                    setPin("A", true);
                }
            }
            if (!outPin("K").hiImpedance) {
                outPin("K").setHiImpedance();
            }
            switch (cathodeIn[i]) {
                case "h" -> {
                }
                case "w0" -> {
                    outPin("K").strong = false;
                    setPin("K", false);
                }
                case "w1" -> {
                    outPin("K").strong = false;
                    setPin("K", true);
                }
                case "s0" -> {
                    outPin("K").strong = true;
                    setPin("K", false);
                }
                case "s1" -> {
                    outPin("K").strong = true;
                    setPin("K", true);
                }
            }
            switch (anodeState[i]) {
                case "h" -> assertTrue(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    assertFalse(inPin("inA").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    assertTrue(inPin("inA").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    assertFalse(inPin("inA").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("inA").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode not to be in hiImpedance");
                    assertTrue(inPin("inA").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Anode must be Strong Hi");
                }
            }
            switch (cathodeState[i]) {
                case "h" -> assertTrue(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    assertFalse(inPin("inK").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    assertTrue(inPin("inK").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    assertFalse(inPin("inK").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("inK").hiImpedance, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode not to be in hiImpedance");
                    assertTrue(inPin("inK").state, "With A=" + anodeIn[i] + " and k=" + cathodeIn[i] + " Cathode must be Strong Hi");
                }
            }
        }
    }
}
