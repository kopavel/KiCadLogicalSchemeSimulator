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
package pko.KiCadLogicalSchemeSimulator.components.Switch.test;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.components.Switch.Switch;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class SwitchTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/switch.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    protected void switchTest() {
        Switch sw1 = (Switch) net.schemaParts.get("SW1");
        Switch sw2 = (Switch) net.schemaParts.get("SW2");
        checkPin("Pull", true, "Initial 'Pull' state must be Hi");
        checkPinImpedance("Mid", "Initial 'Mid' impedance must be True");
        Log.debug(SwitchTest.class, "SW1 on");
        sw1.toggle(true);
        checkPin("Pull", true, "With Sw1 on and Sw2 off 'Pull' state must remain Hi");
        checkPin("Mid", true, "With Sw1 on and Sw2 off 'Mid' state must be Hi");
        Log.debug(SwitchTest.class, "SW1 off");
        sw1.toggle(false);
        checkPin("Pull", true, "After Sw1 off 'Pull' state must be Hi");
        checkPinImpedance("Mid", "After Sw1 off 'Mid' impedance must be True");
        Log.debug(SwitchTest.class, "SW2 on");
        sw2.toggle(true);
        checkPin("Pull", true, "With Sw1 off and Sw2 on 'Pull' state must remain Hi");
        checkPin("Mid", false, "With Sw1 off and Sw2 on 'Mid' state must go Lo");
        Log.debug(SwitchTest.class, "SW2 off");
        sw2.toggle(false);
        checkPin("Pull", true, "After Sw2 off 'Pull' state must be Hi");
        checkPinImpedance("Mid", "After Sw2 off 'Mid' impedance must be True");
        Log.debug(SwitchTest.class, "SW1/2 on");
        sw1.toggle(true);
        sw2.toggle(true);
        checkPin("Pull", false, "After Sw1 go on and then Sw2 on 'Pull' state must be Lo");
        checkPin("Mid", false, "After Sw1 go on and then Sw2 on 'Mid' state must be Lo");
        Log.debug(SwitchTest.class, "SW1 ff");
        sw1.toggle(false);
        checkPin("Pull", true, "After Sw1 go off with Sw2 on 'Pull' state must go Hi");
        checkPin("Mid", false, "After Sw1 go off with Sw2 on 'Mid' state must remain Lo");
        Log.debug(SwitchTest.class, "SW1 on, SW2 off");
        sw1.toggle(true);
        sw2.toggle(false);
        checkPin("Pull", true, "After Sw1 go on but Sw2 go off 'Pull' state must remain Hi");
        checkPin("Mid", true, "After Sw1 go on but Sw2 go off 'Mid' state must go Hi");
        Log.debug(SwitchTest.class, "SW2 on");
        sw2.toggle(true);
        checkPin("Pull", false, "After Sw2 go on with SW1 on 'Pull' state must be Lo");
        checkPin("Mid", false, "After Sw2 go on with SW1 on 'Mid' state must be Lo");
    }
}
