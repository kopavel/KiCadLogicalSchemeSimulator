/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.jkTrigger.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class JkTriggerTest extends NetTester {
    @BeforeEach
    public void reset() {
        net.schemaParts.get("U1#A").reset();
        setLo("R");
        setLo("S");
        setLo("J");
        setLo("K");
    }

    @Test
    @DisplayName("main states")
    public void mainStates() {
        //Lo j and k - store
        checkPin("Q", false, "Default Q state must be 0");
        checkPin("~{Q}", true, "Default ~{Q} state must be 1;");
        setHi("C");
        checkPin("Q", false, "after C pin fall with Lo on 'J' and 'K' Q state must remain 0");
        checkPin("~{Q}", true, "after C pin fall with Lo on 'J' and 'K' ~{Q} state must remain 1");
        setHi("C");
        checkPin("Q", false, "after C pin fall with Lo on 'J' and 'K' Q state must remain 0");
        checkPin("~{Q}", true, "after C pin fall with Lo on 'J' and 'K' ~{Q} state must remain 1");
        //Hi j Lo k - set
        setHi("J");
        setHi("C");
        checkPin("Q", true, "after C pin fall with Hi on 'J' and Lo on 'K' Q state must be 1");
        checkPin("~{Q}", false, "after C pin fall with Hi on 'J' and Lo on 'K' ~{Q} state must be 0");
        setHi("C");
        checkPin("Q", true, "after C pin fall with Hi on 'J' and Lo on 'K' Q state must be 1");
        checkPin("~{Q}", false, "after C pin fall with Hi on 'J' and Lo on 'K' ~{Q} state must be 0");
        //Hi j and k - toggle
        setHi("K");
        setHi("C");
        checkPin("Q", false, "after C pin fall with Hi on 'J' and Lo on 'K' Q state must be toggled");
        checkPin("~{Q}", true, "after C pin fall with Hi on 'J' and Lo on 'K' ~{Q} state must be toggled");
        setHi("C");
        checkPin("Q", true, "after C pin fall with Hi on 'J' and Lo on 'K' Q state must be toggled");
        checkPin("~{Q}", false, "after C pin fall with Hi on 'J' and Lo on 'K' ~{Q} state must be toggled");
        //Lo j Hi k - reset
        setLo("J");
        setHi("C");
        checkPin("Q", false, "after C pin fall with Lo on 'J' and Hi on 'K' Q state must be reset");
        checkPin("~{Q}", true, "after C pin fall with Lo on 'J' and Hi on 'K' ~{Q} state must be toggled");
        setHi("C");
        checkPin("Q", false, "after C pin fall with Lo on 'J' and Hi on 'K' Q state must be reset");
        checkPin("~{Q}", true, "after C pin fall with Lo on 'J' and Hi on 'K' ~{Q} state must be toggled");
    }

    @Test
    @DisplayName("RS states")
    public void rsStates() {
        setHi("S");
        checkPin("Q", true, "after S pin set to Hi Q state must be 1");
        checkPin("~{Q}", false, "after S pin set to Hi 'D' ~{Q} state must be 0");
        setHi("J");
        setHi("K");
        setHi("C");
        checkPin("Q", true, "with Hi S pin C pin state change must be ignored");
        checkPin("~{Q}", false, "with Hi S pin C pin state change must be ignored");
        setHi("R");
        checkPin("Q", true, "after R pin set to Hi with S pin Hi too Q state must be 1");
        checkPin("~{Q}", true, "after R pin set to Hi with S pin Hi too ~{Q] state must be 1");
        setLo("S");
        checkPin("Q", false, "after S pin set to Lo with Hi R  Q state must be 0");
        checkPin("~{Q}", true, "after S pin set to Lo with Hi R  ~{Q} state must be 0");
        setHi("C");
        checkPin("Q", false, "with Hi R pin C pin state change must be ignored");
        checkPin("~{Q}", true, "with Hi R pin C pin state change must be ignored");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/jkTrigger.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }
}
