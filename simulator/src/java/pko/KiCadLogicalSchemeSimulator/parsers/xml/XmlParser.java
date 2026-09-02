/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.parsers.xml;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.io.File;
import java.io.IOException;

public enum XmlParser {
    ;

    public static <T> T parse(String filePath, Class<T> clazz) throws IOException {
        String xml = Utils.readFileToString(new File(filePath));
        return XmlMarshaller.fromXml(xml, clazz);
    }
}
