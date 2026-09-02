/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder.test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class MultiOutDecoderTest extends NetTester {
    @Test
    @DisplayName("value decode")
    public void valueDecode() {
        setBus("A", 0);
        setBus("Eb", 0b11);
        checkPinImpedance("Qa0", "without CS pins Qa0 pin state must be hiImpedance");
        checkPinImpedance("Qa1", "without CS pins Qa1 pin state must be hiImpedance");
        checkPinImpedance("Qa2", "without CS pins Qa2 pin state must be hiImpedance");
        checkPinImpedance("Qa3", "without CS pins Qa3 pin state must be hiImpedance");
        checkPinImpedance("Qb0", "without CS pins Qb0 pin state must be hiImpedance");
        checkPinImpedance("Qb1", "without CS pins Qb1 pin state must be hiImpedance");
        checkPinImpedance("Qb2", "without CS pins Qb2 pin state must be hiImpedance");
        checkPinImpedance("Qb3", "without CS pins Qb3 pin state must be hiImpedance");
        setBus("Ea", 0b11);
        checkPinImpedance("Qa0", "with partial CS pins state Qa0 pin state must be hiImpedance");
        checkPinImpedance("Qa1", "with partial CS pins state Qa1 pin state must be hiImpedance");
        checkPinImpedance("Qa2", "with partial CS pins state Qa2 pin state must be hiImpedance");
        checkPinImpedance("Qa3", "with partial CS pins state Qa3 pin state must be hiImpedance");
        checkPinImpedance("Qb0", "without CS pins Qb0 pin state must be hiImpedance");
        checkPinImpedance("Qb1", "without CS pins Qb1 pin state must be hiImpedance");
        checkPinImpedance("Qb2", "without CS pins Qb2 pin state must be hiImpedance");
        checkPinImpedance("Qb3", "without CS pins Qb3 pin state must be hiImpedance");
        setBus("Ea", 0b01);
        assertFalse(inPin("Qa0").isHiImpedance(), "with 0 at A and CS pins set Qa0 pin state must not be hiImpedance");
        checkPinImpedance("Qa1", "with 0 at A and CS pins set Qa1 pin state must be hiImpedance");
        checkPinImpedance("Qa2", "with 0 at A and CS pins set Qa2 pin state must be hiImpedance ");
        checkPinImpedance("Qa3", "with 0 at A and CS pins set Qa3 pin state must be hiImpedance ");
        checkPin("Qa0",false, "with 0 at A and CS pins set Qa0 pins state must be Lo");
        checkPinImpedance("Qb0", "without CS pins Qb0 pin state must be hiImpedance");
        checkPinImpedance("Qb1", "without CS pins Qb1 pin state must be hiImpedance");
        checkPinImpedance("Qb2", "without CS pins Qb2 pin state must be hiImpedance");
        checkPinImpedance("Qb3", "without CS pins Qb3 pin state must be hiImpedance");
        setBus("Ea", 0b11);
        setBus("Eb", 0b10);
        checkPinImpedance("Qa0", "with partial CS pins state Qa0 pin state must be hiImpedance ");
        checkPinImpedance("Qa1", "with partial CS pins state Qa1 pin state must be hiImpedance ");
        checkPinImpedance("Qa2", "with partial CS pins state Qa2 pin state must be hiImpedance ");
        checkPinImpedance("Qa3", "with partial CS pins state Qa3 pin state must be hiImpedance ");
        checkPinImpedance("Qb0", "with partial CS pins state Qb0 pin state must be hiImpedance ");
        checkPinImpedance("Qb1", "with partial CS pins state Qb1 pin state must be hiImpedance ");
        checkPinImpedance("Qb2", "with partial CS pins state Qb2 pin state must be hiImpedance ");
        checkPinImpedance("Qb3", "with partial CS pins state Qb3 pin state must be hiImpedance ");
        setBus("Eb", 0b00);
        checkPinImpedance("Qa0", "with partial CS pins state Qa0 pin state must be hiImpedance ");
        checkPinImpedance("Qa1", "with partial CS pins state Qa1 pin state must be hiImpedance ");
        checkPinImpedance("Qa2", "with partial CS pins state Qa2 pin state must be hiImpedance ");
        checkPinImpedance("Qa3", "with partial CS pins state Qa3 pin state must be hiImpedance ");
        assertFalse(inPin("Qb0").isHiImpedance(), "with 0 at A and CS pins set Qb0 pin state must not be hiImpedance ");
        checkPinImpedance("Qb1", "with 0 at A and CS pins set Qb1 pin state must be hiImpedance ");
        checkPinImpedance("Qb2", "with 0 at A and CS pins set Qb2 pin state must be hiImpedance ");
        checkPinImpedance("Qb3", "with 0 at A and CS pins set Qb3 pin state must be hiImpedance ");
        checkPin("Qb0",false, "with 0 at A and CS pins set Qb0 pins state must be Lo");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/MultiOutDecoder.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
