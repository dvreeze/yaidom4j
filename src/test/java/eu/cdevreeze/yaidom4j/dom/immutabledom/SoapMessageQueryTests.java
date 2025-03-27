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
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomToJaxpDomConverter;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.JaxpDomToImmutableDomConverter;
import eu.cdevreeze.yaidom4j.jaxp.DocumentBuilders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasLocalName;
import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementSteps.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void parseDocument() {
        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/sample-soap-message.xml");
        soapMessage = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream));
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

        Element editedSoapMessage = soapMessage.documentElement().notUndeclaringPrefixes(
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

        assertEquals(soapMessage.documentElement().toClarkNode(), editedSoapMessage.toClarkNode());
    }

    @Test
    void testW3cDomRoundtripping() {
        org.w3c.dom.Document w3cDomDocument =
                ImmutableDomToJaxpDomConverter.convertDocument(
                        soapMessage,
                        DocumentBuilders.newNonValidatingDocumentBuilderFactory()
                );

        Document soapMessage2 = JaxpDomToImmutableDomConverter.convertDocument(w3cDomDocument);

        assertEquals(soapMessage.documentElement().toClarkNode(), soapMessage2.documentElement().toClarkNode());

        // Even stronger guarantee
        assertEquals(soapMessage.documentElement(), soapMessage2.documentElement());
    }

    @Test
    void testQueryingUsingElementSteps() {
        // Using just the Element query API
        String departing1 = soapMessage.documentElement()
                .selfElementStream(hasLocalName("Envelope"))
                .flatMap(e -> e.childElementStream(hasLocalName("Body")))
                .flatMap(e -> e.childElementStream(hasLocalName("itinerary")))
                .flatMap(e -> e.childElementStream(hasLocalName("departure")))
                .flatMap(e -> e.childElementStream(hasLocalName("departing")))
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        assertEquals("New York", departing1);

        // Using ElementStep instances (which is slightly more friendly syntactically)
        String departing2 = soapMessage.documentElement()
                .selfElementStream(hasLocalName("Envelope"))
                .flatMap(childElements(hasLocalName("Body")))
                .flatMap(childElements(hasLocalName("itinerary")))
                .flatMap(childElements(hasLocalName("departure")))
                .flatMap(childElements(hasLocalName("departing")))
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        assertEquals(departing1, departing2);

        // Using ElementStep chains
        String departing3 = soapMessage.documentElement()
                .selfElementStream(hasLocalName("Envelope"))
                .flatMap(
                        childElements(hasLocalName("Body"))
                                .andThen(childElements(hasLocalName("itinerary")))
                                .andThen(childElements(hasLocalName("departure")))
                                .andThen(childElements(hasLocalName("departing")))
                )
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        assertEquals(departing1, departing3);

        // Much like the preceding example
        String departing4 = soapMessage.documentElement()
                .selfElementStream()
                .flatMap(
                        selfElements(hasLocalName("Envelope"))
                                .andThen(childElements(hasLocalName("Body")))
                                .andThen(childElements(hasLocalName("itinerary")))
                                .andThen(childElements(hasLocalName("departure")))
                                .andThen(childElements(hasLocalName("departing")))
                )
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        assertEquals(departing1, departing4);

        // Using different axes
        String departing5 = soapMessage.documentElement()
                .selfElementStream()
                .flatMap(
                        descendantElementsOrSelf(hasLocalName("Envelope"))
                                .andThen(topmostDescendantElements(hasLocalName("Body")))
                                .andThen(topmostDescendantElementsOrSelf(hasLocalName("itinerary")))
                                .andThen(descendantElements(hasLocalName("departure")))
                                .andThen(descendantElementsOrSelf(hasLocalName("departing")))
                )
                .findFirst()
                .map(Element::text)
                .orElseThrow();

        assertEquals(departing1, departing5);
    }
}
