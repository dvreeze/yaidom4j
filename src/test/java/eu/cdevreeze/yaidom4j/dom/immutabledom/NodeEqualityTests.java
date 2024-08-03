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
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Node equality tests against sample XML files from
 * <a href="https://www.lenzconsulting.com/namespaces/">Understanding XML Namespaces</a>.
 * These files are equal, except that they use different namespace prefixes and have the namespace
 * declarations in different elements.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeEqualityTests {

    private Document doc1;
    private Document doc2;
    private Document doc3;

    @BeforeAll
    void parseDocuments() throws SAXException, ParserConfigurationException, IOException {
        doc1 = parseDocument("/feed1.xml");
        doc2 = parseDocument("/feed2.xml");
        doc3 = parseDocument("/feed3.xml");
    }

    private Document parseDocument(String xmlClasspathResource) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
        saxParserFactory.setNamespaceAware(true); // Important!
        saxParserFactory.setValidating(false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = saxParserFactory.newSAXParser();
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = NodeEqualityTests.class.getResourceAsStream(xmlClasspathResource);
        parser.parse(new InputSource(inputStream), saxHandler);
        return Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
    }

    @Test
    void testNoWhitespaceOnlyText() {
        assertEquals(
                0,
                doc1.documentElement().elementStream()
                        .flatMap(e -> e.children().stream().filter(ch -> ch instanceof Text))
                        .map(n -> (Text) n)
                        .filter(t -> t.value().isBlank())
                        .count()
        );
        assertEquals(
                0,
                doc2.documentElement().elementStream()
                        .flatMap(e -> e.children().stream().filter(ch -> ch instanceof Text))
                        .map(n -> (Text) n)
                        .filter(t -> t.value().isBlank())
                        .count()
        );
        assertEquals(
                0,
                doc3.documentElement().elementStream()
                        .flatMap(e -> e.children().stream().filter(ch -> ch instanceof Text))
                        .map(n -> (Text) n)
                        .filter(t -> t.value().isBlank())
                        .count()
        );
    }

    @Test
    void testSameElementNames() {
        List<QName> elementNames1 = doc1.documentElement()
                .elementStream()
                .map(Element::name)
                .toList();
        List<QName> elementNames2 = doc2.documentElement()
                .elementStream()
                .map(Element::name)
                .toList();
        List<QName> elementNames3 = doc3.documentElement()
                .elementStream()
                .map(Element::name)
                .toList();

        var ns = doc1.documentElement().name().getNamespaceURI();
        assertTrue(elementNames1.containsAll(Set.of(
                new QName(ns, "feed"), new QName(ns, "title"), new QName(ns, "rights"))));
        assertEquals(elementNames1, elementNames2);
        assertEquals(elementNames1, elementNames3);
        assertEquals(elementNames2, elementNames3);
    }

    @Test
    void testDifferentPrefixes() {
        List<String> elementPrefixes1 = doc1.documentElement()
                .elementStream()
                .map(Element::name)
                .map(QName::getPrefix)
                .toList();
        List<String> elementPrefixes2 = doc2.documentElement()
                .elementStream()
                .map(Element::name)
                .map(QName::getPrefix)
                .toList();
        List<String> elementPrefixes3 = doc3.documentElement()
                .elementStream()
                .map(Element::name)
                .map(QName::getPrefix)
                .toList();

        assertTrue(elementPrefixes1.containsAll(Set.of("", "xhtml")));
        assertNotEquals(elementPrefixes1, elementPrefixes2);
        assertNotEquals(elementPrefixes2, elementPrefixes3);
    }

    @Test
    void testDifferentNamespaceScopes() {
        assertNotEquals(
                doc1.documentElement().elementStream().map(Element::namespaceScope).toList(),
                doc2.documentElement().elementStream().map(Element::namespaceScope).toList()
        );
        assertNotEquals(
                doc2.documentElement().elementStream().map(Element::namespaceScope).toList(),
                doc3.documentElement().elementStream().map(Element::namespaceScope).toList()
        );
    }

    @Test
    void testEqualDocumentElements() {
        var nodeComparison = new NodeComparisons.DefaultEqualityComparison();
        assertTrue(nodeComparison.areEqual(doc1.documentElement(), doc2.documentElement()));
        assertTrue(nodeComparison.areEqual(doc1.documentElement(), doc3.documentElement()));
        assertTrue(nodeComparison.areEqual(doc2.documentElement(), doc3.documentElement()));
    }

    @Test
    void testEqualDescendantOrSelfElements() {
        List<Element> elements1 = doc1.documentElement()
                .elementStream()
                .toList();
        List<Element> elements2 = doc2.documentElement()
                .elementStream()
                .toList();
        List<Element> elements3 = doc3.documentElement()
                .elementStream()
                .toList();

        assertTrue(doc1.documentElement().elementStream().count() >= 5);

        assertEquals(elements1.size(), elements2.size());
        assertEquals(elements1.size(), elements3.size());

        var nodeComparison = new NodeComparisons.DefaultEqualityComparison();
        assertTrue(
                IntStream.range(0, elements1.size())
                        .allMatch(i -> nodeComparison.areEqual(elements1.get(i), elements2.get(i)))
        );
        assertTrue(
                IntStream.range(0, elements1.size())
                        .allMatch(i -> nodeComparison.areEqual(elements1.get(i), elements3.get(i)))
        );
        assertTrue(
                IntStream.range(0, elements1.size())
                        .allMatch(i -> nodeComparison.areEqual(elements2.get(i), elements3.get(i)))
        );
    }
}
