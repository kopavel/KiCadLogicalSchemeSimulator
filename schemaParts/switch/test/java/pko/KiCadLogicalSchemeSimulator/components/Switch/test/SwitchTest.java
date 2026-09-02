/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
