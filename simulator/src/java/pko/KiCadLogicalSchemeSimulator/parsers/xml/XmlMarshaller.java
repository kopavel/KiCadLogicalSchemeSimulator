/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.parsers.xml;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import tools.jackson.module.jaxb.JaxbAnnotationModule;

@SuppressWarnings("unused")
public enum XmlMarshaller {
    ;
    private static volatile XmlMapper xmlMapper;

    public static String toXml(Object o) throws JacksonException {
        return getMapper().writeValueAsString(o);
    }

    public static <T> T fromXml(String xml, Class<T> clazz) throws JacksonException {
        return getMapper().readValue(xml, clazz);
    }

    private static XmlMapper getMapper() {
        if (xmlMapper == null) {
            synchronized (XmlMarshaller.class) {
                if (xmlMapper == null) {
                    xmlMapper = XmlMapper.xmlBuilder()
                                         .defaultUseWrapper(false)
                                         .addModule(new JaxbAnnotationModule())
                                         .addModule(new JakartaXmlBindAnnotationModule())
                                         .changeDefaultPropertyInclusion(old -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL,
                                                 JsonInclude.Include.NON_NULL))
                                         .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                         .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                                         .build();
                }
            }
        }
        return xmlMapper;
    }
}
