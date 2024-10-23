/*
 * Copyright 2024-2024 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom4j.misc;

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Large file base64 decoding/encoding test, that happens to be an XML file.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class Base64LargeXmlTests {

    @Test
    void testProcessingOfBase64EncodedXml() throws URISyntaxException, IOException {
        URI xmlUri = Objects.requireNonNull(Base64LargeXmlTests.class.getResource("/orders.xml")).toURI();

        String xmlString = Files.readString(Path.of(xmlUri), UTF_8);

        // It's a big file
        assertTrue(xmlString.length() > 5_000_000);

        Base64.Encoder encoder = Base64.getMimeEncoder();
        byte[] base64EncodedXml = encoder.encode(xmlString.getBytes(UTF_8));

        System.out.println("First 1000 base64 encoded bytes:");
        // Extremely inefficient
        String firstEncodedChars = new String(base64EncodedXml, UTF_8).substring(0, 1000);
        System.out.println(firstEncodedChars);

        // MIME encoded, with lines less than 100 characters
        assertTrue(firstEncodedChars.lines().count() >= 10L);

        Base64.Decoder decoder = Base64.getMimeDecoder();
        String xmlString2 = new String(decoder.decode(base64EncodedXml), UTF_8);

        // Round tripping of base64 encoding and decoding returns the same (XML) string
        assertEquals(xmlString, xmlString2);

        Document doc = parseDocument(new ByteArrayInputStream(xmlString.getBytes(UTF_8)));

        // The parsed base64 decoded XML indeed has quite a lot of element nodes
        long numberOfElements = doc.documentElement().elementStream().count();
        assertTrue(numberOfElements >= 150_000L);
        assertFalse(numberOfElements >= 1_000_000L);
    }

    private static Document parseDocument(InputStream inputStream) {
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();
        InputSource inputSource = new InputSource(inputStream);
        SaxParsers.parse(inputSource, saxHandler);
        return saxHandler.resultingDocument().removeInterElementWhitespace();
    }
}
