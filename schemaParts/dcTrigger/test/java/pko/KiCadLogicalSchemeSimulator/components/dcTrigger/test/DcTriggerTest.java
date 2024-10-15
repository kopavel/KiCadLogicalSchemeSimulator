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
        setLo("D");
        setLo("C");
        setLo("S");
        setHi("R");
        setLo("R");
    }

    @Test
    @DisplayName("default")
    void defaultState() {
        checkPin("Q", false, "Default Q state must be 0");
        checkPin("~{Q}", true, "Default ~{Q} state must be 1;");
    }

    @Test
    @DisplayName("main states")
    void mainStates() {
        setHi("C");
        checkPin("Q", false, "after C pin raise  with Lo 'D' Q state must remain 0");
        checkPin("~{Q}", true, "after C pin raise with Lo 'D' ~{Q} state must remain 1");
        setHi("D");
        setHi("C");
        checkPin("Q", true, "after C pin raise with Hi 'D' Q state must be 1");
        checkPin("~{Q}", false, "after C pin raise with Hi 'D' ~{Q} state must be 0");
        setLo("D");
        setLo("C");
        checkPin("Q", true, "after C pin fall Q state must be preserved");
        checkPin("~{Q}", false, "after C pin fall with Hi 'D' ~{Q} state must be preserved");
        setHi("C");
        checkPin("Q", false, "after C pin raise with Lo 'D' Q state must be 0");
        checkPin("~{Q}", true, "after C pin raise with Lo 'D' ~{Q} state must be 1");
    }

    @Test
    @DisplayName("RS states")
    void rsStates() {
        setHi("S");
        checkPin("Q", true, "after S pin set to Hi Q state must be 1");
        checkPin("~{Q}", false, "after S pin set to Hi 'D' ~{Q} state must be 0");
        setLo("D");
        setHi("C");
        checkPin("Q", true, "with Hi S pin C pin state change must be ignored");
        checkPin("~{Q}", false, "with Hi S pin C pin state change must be ignored");
        setHi("R");
        checkPin("Q", true, "after R pin set to Hi with S pin Hi too Q state must be 1");
        checkPin("~{Q}", true, "after R pin set to Hi with S pin Hi too ~{Q] state must be 1");
        setLo("S");
        checkPin("Q", false, "after S pin set to Lo with Hi R  Q state must be 0");
        checkPin("~{Q}", true, "after S pin set to Lo with Hi R  ~{Q} state must be 0");
        setLo("D");
        setHi("C");
        checkPin("Q", false, "with Hi R pin C pin state change must be ignored");
        checkPin("~{Q}", true, "with Hi R pin C pin state change must be ignored");
    }
}
