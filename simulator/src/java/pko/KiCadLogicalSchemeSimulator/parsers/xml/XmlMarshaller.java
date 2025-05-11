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
package pko.KiCadLogicalSchemeSimulator.parsers.xml;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

@SuppressWarnings("unused")
public enum XmlMarshaller {
    ;
    private static volatile XmlMapper xmlMapper;

    public static String toXml(Object o) throws JsonProcessingException {
        return getMapper().writeValueAsString(o);
    }

    public static <T> T fromXml(String xml, Class<T> clazz) throws JsonProcessingException {
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
                                         .serializationInclusion(JsonInclude.Include.NON_NULL)
                                         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                         .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                                         .build();
                }
            }
        }
        return xmlMapper;
    }
}
