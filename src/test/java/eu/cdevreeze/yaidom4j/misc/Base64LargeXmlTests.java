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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Text;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomConsumingSaxEventGenerator;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.jaxp.TransformerHandlers;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
 * The test also tests processing of base64 encoded XML as element text content.
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

        String base64EncodedXmlAsString = new String(base64EncodedXml, UTF_8);

        System.out.println("First 1000 base64 encoded bytes:");
        String firstEncodedChars = base64EncodedXmlAsString.substring(0, 1000);
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

        // Let's ramp things up a bit:
        // Create an XML document containing the base64 encoded XML string as element content

        Element rootElem =
                new Element(
                        new QName("dummyRoot"),
                        ImmutableMap.of(),
                        doc.documentElement().namespaceScope(),
                        ImmutableList.of(
                                new Element(
                                        new QName("dummyChild"),
                                        ImmutableMap.of(),
                                        doc.documentElement().namespaceScope(),
                                        ImmutableList.of(new Text(base64EncodedXmlAsString, false))
                                )
                        )
                );

        String wrapperXmlString = printElement(rootElem);

        Document wrapperDoc = parseDocument(new ByteArrayInputStream(wrapperXmlString.getBytes(UTF_8)));

        System.out.println();
        System.out.println("The wrapper document, without the base64 encoded string holding XML itself:");
        System.out.println(
                printElement(
                        wrapperDoc.documentElement().transformDescendantElements(
                                e -> e.withChildren(
                                        e.children()
                                                .stream()
                                                .filter(n -> !(n instanceof Text))
                                                .collect(ImmutableList.toImmutableList())
                                )
                        )
                )
        );

        // Extracting the same XML as the original, step by step

        Element childElem = wrapperDoc.documentElement().childElementStream().findFirst().orElseThrow();

        String childElemTextContent = childElem.text();
        byte[] base64DecodedChildElemTextContent = decoder.decode(childElemTextContent.getBytes(UTF_8));

        Document nestedDoc = parseDocument(new ByteArrayInputStream(base64DecodedChildElemTextContent));

        // We should get the original XML again, so at the very least the root element names must match
        assertEquals(doc.documentElement().name(), nestedDoc.documentElement().name());

        // Hopefully we have equivalent XML again
        assertEquals(doc.documentElement().toClarkNode(), nestedDoc.documentElement().toClarkNode());
    }

    private static Document parseDocument(InputStream inputStream) {
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();
        InputSource inputSource = new InputSource(inputStream);
        SaxParsers.parse(inputSource, saxHandler);
        return saxHandler.resultingDocument().removeInterElementWhitespace();
    }

    private static String printElement(Element element) {
        var sw = new StringWriter();
        var streamResult = new StreamResult(sw);

        TransformerHandler th = TransformerHandlers.newTransformerHandler();
        th.setResult(streamResult);

        var saxEventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);

        saxEventGenerator.processElement(element, NamespaceScope.empty());

        return sw.toString();
    }
}