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
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPartSpi;

module KiCadLogicalSchemeSimulator.simulator {
    exports pko.KiCadLogicalSchemeSimulator.api.pins.in;
    exports pko.KiCadLogicalSchemeSimulator.api.pins.out;
    exports pko.KiCadLogicalSchemeSimulator.api.pins;
    exports pko.KiCadLogicalSchemeSimulator.api.schemaPart;
    exports pko.KiCadLogicalSchemeSimulator.api;
    exports pko.KiCadLogicalSchemeSimulator.api_v2;
    exports pko.KiCadLogicalSchemeSimulator.api_v2.bus.in;
    exports pko.KiCadLogicalSchemeSimulator.api_v2.bus;
    exports pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart;
    exports pko.KiCadLogicalSchemeSimulator.api_v2.wire.in;
    exports pko.KiCadLogicalSchemeSimulator.api_v2.wire;
    exports pko.KiCadLogicalSchemeSimulator.model;
    exports pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap;
    exports pko.KiCadLogicalSchemeSimulator.parsers.pojo;
    exports pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
    exports pko.KiCadLogicalSchemeSimulator.tools;
    exports pko.KiCadLogicalSchemeSimulator;
    opens pko.KiCadLogicalSchemeSimulator.model;
    opens pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap;
    opens pko.KiCadLogicalSchemeSimulator.parsers.pojo;
    opens pko.KiCadLogicalSchemeSimulator;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.module.jakarta.xmlbind;
    requires com.fasterxml.jackson.module.jaxb;
    requires com.formdev.flatlaf;
    requires com.lmax.disruptor;
    requires info.picocli;
    requires jakarta.xml.bind;
    requires java.desktop;
    requires java.gui.forms.rt;
    requires java.xml.bind;
    requires org.apache.commons.compress;
    requires org.apache.commons.lang3;
    requires org.apache.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.jcl;
    requires org.apache.logging.log4j.jpl;
    requires org.apache.logging.log4j.jul;
    requires org.apache.logging.log4j.slf4j.impl;
    requires org.apache.logging.log4j;
    requires org.tukaani.xz;
    requires static lombok;
    uses SchemaPartSpi;
}
