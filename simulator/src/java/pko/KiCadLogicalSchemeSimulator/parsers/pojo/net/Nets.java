/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.parsers.pojo.net;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nets {
    public Export parent;
    public List<Net> net;

    public void fillParents(Export export) {
        parent=export;
        net.forEach(n->n.fillParent(this));
    }
}
