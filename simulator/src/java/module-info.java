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
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;

module KiCadLogicalSchemeSimulator.simulator {
    uses SchemaPartSpi;
    uses lv.pko.KiCadLogicalSchemeSimulator.v2.api.schemaPart.SchemaPartSpi;
    requires static lombok;
    requires java.desktop;
    requires com.formdev.flatlaf;
    requires java.gui.forms.rt;
    requires org.tukaani.xz;
    requires org.apache.commons.compress;
    requires org.apache.log4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.jcl;
    requires org.apache.logging.log4j.jpl;
    requires org.apache.logging.log4j.jul;
    requires org.apache.logging.log4j.slf4j.impl;
    requires com.lmax.disruptor;
    requires jakarta.xml.bind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.jakarta.xmlbind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.module.jaxb;
    requires org.apache.commons.lang3;
    requires java.xml.bind;
    requires info.picocli;
    exports lv.pko.KiCadLogicalSchemeSimulator;
    opens lv.pko.KiCadLogicalSchemeSimulator;
    exports lv.pko.KiCadLogicalSchemeSimulator.tools;
    exports lv.pko.KiCadLogicalSchemeSimulator.api;
    exports lv.pko.KiCadLogicalSchemeSimulator.api.pins;
    exports lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart;
    exports lv.pko.KiCadLogicalSchemeSimulator.api.pins.in;
    exports lv.pko.KiCadLogicalSchemeSimulator.api.pins.out;
    exports lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api.bus.in;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api.pin.in;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2.api.schemaPart;
    exports lv.pko.KiCadLogicalSchemeSimulator.v2;
    opens lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo;
    exports lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap;
    opens lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap;
    exports lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
}