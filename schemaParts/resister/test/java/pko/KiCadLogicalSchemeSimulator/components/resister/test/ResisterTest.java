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
package pko.KiCadLogicalSchemeSimulator.components.resister.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ResisterTest extends NetTester {
    private final String[] in1 =
            {"hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1", "hi", "w0", "w1", "s0", "s1"};
    private final String[] in2 =
            {"hi", "hi", "hi", "hi", "hi", "w0", "w0", "w0", "w0", "w0", "w1", "w1", "w1", "w1", "w1", "s0", "s0", "s0", "s0", "s0", "s1", "s1", "s1", "s1", "s1"};
    private final String[] state1 =
            {"hi", "w0", "w1", "s0", "s1", "w0", "w0", "er", "s0", "er", "w1", "er", "w1", "er", "s1", "w0", "w0", "er", "s0", "er", "w1", "er", "w1", "s0", "s1"};
    private final String[] state2 =
            {"hi", "W0", "w1", "w0", "w1", "w0", "w0", "er", "w0", "er", "w1", "er", "w1", "er", "w1", "s0", "s0", "er", "s0", "er", "s1", "er", "s1", "s1", "s1"};

    @Override
    protected String getNetFilePath() {
        return "test/resources/Resister.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void resisterTest() {
        Pin out1 = outPin("OUT1");
        Pin out2 = outPin("OUT2");
        for (int i = 0; i < state1.length; i++) {
            if ("er".equals(state1[i])) {
                continue;
            }
            Log.debug(ResisterTest.class, "{},{},{},{}", in1[i], in2[i], state1[i], state2[i]);
            switch (in1[i]) {
                case "hi" -> {
                    if (!out1.hiImpedance) {
                        out1.setHiImpedance();
                    }
                }
                case "w0" -> {
                    out1.strong = false;
                    out1.setLo();
                }
                case "w1" -> {
                    out1.strong = false;
                    out1.setHi();
                }
                case "s0" -> {
                    out1.strong = true;
                    out1.setLo();
                }
                case "s1" -> {
                    out1.strong = true;
                    out1.setHi();
                }
            }
            switch (in2[i]) {
                case "hi" -> {
                    if (!out2.hiImpedance) {
                        out2.setHiImpedance();
                    }
                }
                case "w0" -> {
                    if (out2.strong || out2.state) {
                        out2.strong = false;
                        out2.setLo();
                    }
                }
                case "w1" -> {
                    if (out2.strong || !out2.state) {
                        out2.strong = false;
                        out2.setHi();
                    }
                }
                case "s0" -> {
                    if (!out2.strong || out2.state) {
                        out2.strong = true;
                        out2.setLo();
                    }
                }
                case "s1" -> {
                    if (!out2.strong || !out2.state) {
                        out2.strong = true;
                        out2.setHi();
                    }
                }
            }
            switch (state1[i]) {
                case "hi" -> checkPinImpedance("IN1", "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("IN1").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 not to be in hiImpedance");
                    checkPin("IN1", false, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("IN1").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 not to be in hiImpedance");
                    checkPin("IN1", true, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("IN1").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 not to be in hiImpedance");
                    checkPin("IN1", false, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("IN1").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 not to be in hiImpedance");
                    checkPin("IN1", true, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN1 must be Strong Hi");
                }
            }
            switch (state2[i]) {
                case "hi" -> checkPinImpedance("IN2", "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 must be in hiImpedance");
                case "w0" -> {
                    assertFalse(inPin("IN2").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 not to be in hiImpedance");
                    checkPin("IN2", false, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 must be weak Lo");
                }
                case "w1" -> {
                    assertFalse(inPin("IN2").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 not to be in hiImpedance");
                    checkPin("IN2", true, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 must be weak Hi");
                }
                case "s0" -> {
                    assertFalse(inPin("IN2").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 not to be in hiImpedance");
                    checkPin("IN2", false, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 must be strong Lo");
                }
                case "s1" -> {
                    assertFalse(inPin("IN2").hiImpedance, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 not to be in hiImpedance");
                    checkPin("IN2", true, "With OUT1=" + in1[i] + " and OUT2=" + in2[i] + " IN2 must be Strong Hi");
                }
            }
        }
    }
}
