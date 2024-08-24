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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DcTriggerTest extends NetTester {
    @Override
    protected String getNetFilePath() {
        return "test/resources/dcTrigger.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @BeforeEach
    void reset() {
        setPin("S", false);
        setPin("R", true);
        setPin("R", false);
        setPin("D", false);
        setPin("C", false);
    }

    @Test
    @DisplayName("default")
    void defaultState() {
        assertFalse(inPin("Q").state, "Default Q state must be 0");
        assertTrue(inPin("~{Q}").state, "Default ~{Q} state must be 1;");
    }

    @Test
    @DisplayName("main states")
    void mainStates() {
        setPin("C", true);
        assertFalse(inPin("Q").state, "after C pin raise  with Lo 'D' Q state must remain 0");
        assertTrue(inPin("~{Q}").state, "after C pin raise with Lo 'D' ~{Q} state must remain 1");
        setPin("D", true);
        setPin("C", true);
        assertTrue(inPin("Q").state, "after C pin raise with Hi 'D' Q state must be 1");
        assertFalse(inPin("~{Q}").state, "after C pin raise with Hi 'D' ~{Q} state must be 0");
        setPin("D", false);
        setPin("C", false);
        assertTrue(inPin("Q").state, "after C pin fall Q state must be preserved");
        assertFalse(inPin("~{Q}").state, "after C pin fall with Hi 'D' ~{Q} state must be preserved");
        setPin("C", true);
        assertFalse(inPin("Q").state, "after C pin raise with Lo 'D' Q state must be 0");
        assertTrue(inPin("~{Q}").state, "after C pin raise with Lo 'D' ~{Q} state must be 1");
    }

    @Test
    @DisplayName("RS states")
    void rsStates() {
        setPin("S", true);
        assertTrue(inPin("Q").state, "after S pin set to Hi Q state must be 1");
        assertFalse(inPin("~{Q}").state, "after S pin set to Hi 'D' ~{Q} state must be 0");
        setPin("D", false);
        setPin("C", true);
        assertTrue(inPin("Q").state, "with Hi S pin C pin state change must be ignored");
        assertFalse(inPin("~{Q}").state, "with Hi S pin C pin state change must be ignored");
        setPin("R", true);
        assertTrue(inPin("Q").state, "after R pin set to Hi with S pin Hi too Q state must be 1");
        assertTrue(inPin("~{Q}").state, "after R pin set to Hi with S pin Hi too ~{Q] state must be 1");
        setPin("S", false);
        assertFalse(inPin("Q").state, "after S pin set to Lo with Hi R  Q state must be 0");
        assertTrue(inPin("~{Q}").state, "after S pin set to Lo with Hi R  ~{Q} state must be 0");
        setPin("D", false);
        setPin("C", true);
        assertFalse(inPin("Q").state, "with Hi R pin C pin state change must be ignored");
        assertTrue(inPin("~{Q}").state, "with Hi R pin C pin state change must be ignored");
    }
}
