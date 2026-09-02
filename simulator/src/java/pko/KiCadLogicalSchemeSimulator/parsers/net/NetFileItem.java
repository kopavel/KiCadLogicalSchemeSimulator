/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.parsers.net;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetFileItem {
    public final Map<String, List<NetFileItem>> items = new HashMap<>();
    public String name;
    public String value;
}
