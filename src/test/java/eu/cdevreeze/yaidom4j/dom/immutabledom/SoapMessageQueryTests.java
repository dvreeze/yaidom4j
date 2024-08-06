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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.comparison.NodeComparisons;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SOAP message query tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoapMessageQueryTests {

    private static final String ENV_NS = "http://www.w3.org/2003/05/soap-envelope";

    private static final String M_NS = "http://travelcompany.example.org/reservation";
    private static final String N_NS = "http://mycompany.example.com/employees";
    private static final String P_NS = "http://travelcompany.example.org/reservation/travel";

    private Document soapMessage;

    @BeforeAll
    void parseDocument() throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
        saxParserFactory.setNamespaceAware(true); // Important!
        saxParserFactory.setValidating(false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = saxParserFactory.newSAXParser();
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/sample-soap-message.xml");
        parser.parse(new InputSource(inputStream), saxHandler);
        soapMessage = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
    }

    @Test
    void testQuerySoapEnvelope() {
        Element envelope = soapMessage.documentElement();

        assertEquals(new QName(ENV_NS, "Envelope"), envelope.name());

        List<Element> envelopeChildElements = envelope.childElementStream().toList();

        assertEquals(
                List.of(new QName(ENV_NS, "Header"), new QName(ENV_NS, "Body")),
                envelopeChildElements.stream().map(Element::name).toList()
        );
    }

    @Test
    void testQuerySoapHeader() {
        Element header = soapMessage.documentElement().childElementStream(hasName(ENV_NS, "Header"))
                .findFirst()
                .orElseThrow();

        assertEquals(new QName(ENV_NS, "Header"), header.name());

        List<Element> headerChildElements = header.childElementStream().toList();

        assertEquals(
                List.of(new QName(M_NS, "reservation"), new QName(N_NS, "passenger")),
                headerChildElements.stream().map(Element::name).toList()
        );
    }

    @Test
    void testQuerySoapBody() {
        Element body = soapMessage.documentElement().childElementStream(hasName(ENV_NS, "Body"))
                .findFirst()
                .orElseThrow();

        assertEquals(new QName(ENV_NS, "Body"), body.name());

        List<Element> bodyChildElements = body.childElementStream().toList();

        assertEquals(
                List.of(new QName(P_NS, "itinerary")),
                bodyChildElements.stream().map(Element::name).toList()
        );

        List<Element> nonLeafElementBodyDescendantElements = body
                .descendantElementStream(e -> e.childElementStream().findAny().isPresent())
                .toList();

        assertEquals(
                List.of(
                        new QName(P_NS, "itinerary"),
                        new QName(P_NS, "departure"),
                        new QName(P_NS, "return")
                ),
                nonLeafElementBodyDescendantElements.stream().map(Element::name).toList()
        );
    }

    @Test
    void testPushingUpPrefixedNamespaces() {
        assertEquals(4,
                soapMessage.documentElement().elementStream()
                        .map(Element::namespaceScope)
                        .distinct()
                        .count());

        Element editedSoapMessage = Elements.notUndeclaringPrefixes(
                soapMessage.documentElement(),
                NamespaceScope.from(ImmutableMap.of(
                        "env", ENV_NS,
                        "m", M_NS,
                        "n", N_NS,
                        "p", P_NS
                ))
        );

        assertEquals(1,
                editedSoapMessage.elementStream()
                        .map(Element::namespaceScope)
                        .distinct()
                        .count());

        var nodeComparison = new NodeComparisons.DefaultEqualityComparison();
        assertTrue(nodeComparison.areEqual(soapMessage.documentElement(), editedSoapMessage));
    }
}
