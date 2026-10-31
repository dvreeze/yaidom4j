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

import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example test showing equality of different XML documents that use different namespace prefixes and namespace
 * declarations in different elements but that are otherwise equal.
 * <p>
 * The test example is taken from <a href="https://www.lenzconsulting.com/namespaces/">Understanding XML Namespaces</a>
 * (and slightly adapted).
 *
 * @author Chris de Vreeze
 */
class XmlEqualityTests {

    @Test
    void testXmlEquality() {
        Document doc1 = Objects.requireNonNull(getDocument1());
        Document doc2 = Objects.requireNonNull(getDocument2());

        assertEquals(doc1.documentElement().toClarkNode(), doc2.documentElement().toClarkNode());

        List<Element> allElems1 = doc1.documentElement().elementStream().toList();
        List<Element> allElems2 = doc2.documentElement().elementStream().toList();

        boolean areEqual = allElems1.size() == allElems2.size() &&
                IntStream.range(0, allElems1.size())
                        .allMatch(i -> allElems1.get(i).toClarkNode().equals(allElems2.get(i).toClarkNode()));

        assertTrue(areEqual);

        Set<QName> elementNames1 = doc1.documentElement().elementStream()
                .map(Element::name)
                .collect(Collectors.toSet());
        Set<QName> elementNames2 = doc2.documentElement().elementStream()
                .map(Element::name)
                .collect(Collectors.toSet());

        assertEquals(elementNames1, elementNames2);

        assertEquals(
                Set.of(
                        new QName("http://www.w3.org/2005/Atom", "rights"),
                        new QName("http://www.w3.org/1999/xhtml", "div"),
                        new QName("http://www.w3.org/2005/Atom", "feed"),
                        new QName("http://www.w3.org/2005/Atom", "title")
                ),
                elementNames1
        );
    }

    private static String divText() {
        return """
                You may not read, utter, interpret, or otherwise
                verbally process the words
                contained in this feed without express written
                permission from the authors.""";
    }

    private static Document getDocument1() {
        String atomNs = "http://www.w3.org/2005/Atom";
        String xhtmlNs = "http://www.w3.org/1999/xhtml";
        String myNs = "http://xmlportfolio.com/xmlguild-examples";

        NamespaceScope nsScope = new NamespaceScope(
                Map.of("", atomNs, "xhtml", xhtmlNs, "my", myNs)
        );

        return new Document(
                Optional.empty(),
                List.of(
                        new Element(
                                new QName(atomNs, "feed", ""),
                                Map.of(),
                                nsScope,
                                List.of(
                                        new Element(
                                                new QName(atomNs, "title", ""),
                                                Map.of(),
                                                nsScope,
                                                List.of(new Text("Example Feed", false))
                                        ),
                                        new Element(
                                                new QName(atomNs, "rights", ""),
                                                Map.of(
                                                        new QName("type"), "xhtml",
                                                        new QName(myNs, "type", "my"), "silly"
                                                ),
                                                nsScope,
                                                List.of(
                                                        new Element(
                                                                new QName(xhtmlNs, "div", "xhtml"),
                                                                Map.of(),
                                                                nsScope,
                                                                List.of(
                                                                        new Text(divText(), false)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static Document getDocument2() {
        String atomNs = "http://www.w3.org/2005/Atom";
        String xhtmlNs = "http://www.w3.org/1999/xhtml";
        String exampleNs = "http://xmlportfolio.com/xmlguild-examples";

        NamespaceScope nsScope1 = new NamespaceScope(
                Map.of("", atomNs)
        );

        NamespaceScope nsScope2 = new NamespaceScope(
                Map.of("", atomNs, "example", exampleNs)
        );

        NamespaceScope nsScope3 = new NamespaceScope(
                Map.of("", xhtmlNs, "example", exampleNs)
        );

        return new Document(
                Optional.empty(),
                List.of(
                        new Element(
                                new QName(atomNs, "feed", ""),
                                Map.of(),
                                nsScope1,
                                List.of(
                                        new Element(
                                                new QName(atomNs, "title", ""),
                                                Map.of(),
                                                nsScope1,
                                                List.of(new Text("Example Feed", false))
                                        ),
                                        new Element(
                                                new QName(atomNs, "rights", ""),
                                                Map.of(
                                                        new QName("type"), "xhtml",
                                                        new QName(exampleNs, "type", "example"), "silly"
                                                ),
                                                nsScope2,
                                                List.of(
                                                        new Element(
                                                                new QName(xhtmlNs, "div", ""),
                                                                Map.of(),
                                                                nsScope3,
                                                                List.of(
                                                                        new Text(divText(), false)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
