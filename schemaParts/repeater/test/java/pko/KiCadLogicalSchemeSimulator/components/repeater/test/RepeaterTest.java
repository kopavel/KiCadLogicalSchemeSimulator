/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.repeater.test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pko.KiCadLogicalSchemeSimulator.test.schemaPartTester.NetTester;

public class RepeaterTest extends NetTester {
    @BeforeEach
    protected void reset() {
        setLo("ampOut");
        setLo("notOut");
    }

    @Override
    protected String getNetFilePath() {
        return "test/resources/repeater.net";
    }

    @Override
    protected String getRootPath() {
        return "../..";
    }

    @Test
    @DisplayName("repeater")
    void repeater() {
        checkPin("notIn", true, "with Lo input NOT out must be Hi");
        checkPin("ampIn", false, "with Lo input repeater out must be Lo");
        setHi("notOut");
        setHi("ampOut");
        checkPin("notIn", false, "with Hi input NOT out must be Lo");
        checkPin("ampIn", true, "with Hi input repeater out must be Hi");
    }
}
