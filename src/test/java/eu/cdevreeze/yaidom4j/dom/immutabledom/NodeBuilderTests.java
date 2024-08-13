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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.comparison.NodeComparisons;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Node builder tests against sample XML files from
 * <a href="https://www.lenzconsulting.com/namespaces/">Understanding XML Namespaces</a>.
 * These files are equal, except that they use different namespace prefixes and have the namespace
 * declarations in different elements.
 * <p>
 * This is not a regular unit test, in that it assumes parsing, node equality comparisons etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeBuilderTests {

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

        InputStream inputStream = NodeBuilderTests.class.getResourceAsStream(xmlClasspathResource);
        parser.parse(new InputSource(inputStream), saxHandler);
        return Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
    }

    @Test
    void testEquivalence() {
        String innerText = doc1.documentElement()
                .descendantElementStream(hasName(XHTML_NS, "div"))
                .findFirst()
                .orElseThrow()
                .text();

        Element rootElement1 = rootElement1(innerText);
        Element rootElement2 = rootElement2(innerText);

        var defaultEquality = NodeComparisons.defaultEquality();

        assertTrue(defaultEquality.areEqual(doc1.documentElement(), doc2.documentElement()));
        assertTrue(defaultEquality.areEqual(doc1.documentElement(), doc3.documentElement()));
        assertTrue(defaultEquality.areEqual(doc3.documentElement(), doc2.documentElement()));

        assertFalse(defaultEquality.areEqual(doc1.documentElement(), rootElement1));

        Element docRootElement1 = prepareForComparison(doc1.documentElement());
        Element docRootElement2 = prepareForComparison(doc2.documentElement());

        assertNotEquals(
                rootElement1.elementStream().map(e -> e.name().getPrefix()).collect(Collectors.toSet()),
                rootElement2.elementStream().map(e -> e.name().getPrefix()).collect(Collectors.toSet())
        );
        assertTrue(defaultEquality.areEqual(rootElement1, rootElement2));

        assertTrue(defaultEquality.areEqual(docRootElement1, rootElement1));
        assertTrue(defaultEquality.areEqual(docRootElement2, rootElement2));
    }

    private static final NodeBuilder.ConciseApi nb1 = new NodeBuilder.ConciseApi(
            NamespaceScope.from(ImmutableMap.of(
                    "", "http://www.w3.org/2005/Atom",
                    "xhtml", "http://www.w3.org/1999/xhtml",
                    "my", "http://xmlportfolio.com/xmlguild-examples"
            ))
    );

    private static Element rootElement1(String innerText) {
        return nb1.element("feed")
                .plusChild(nb1.textElement("title", "Example Feed"))
                .plusChild(
                        nb1.element(
                                        "rights",
                                        ImmutableMap.of("type", "xhtml", "my:type", "silly")
                                )
                                .plusChild(
                                        nb1.textElement("xhtml:div", innerText)
                                )
                );
    }

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    private static final NodeBuilder.ConciseApi nb2 =
            NodeBuilder.ConciseApi.empty().resolve("", ATOM_NS);

    private static final String EXAMPLE_NS = "http://xmlportfolio.com/xmlguild-examples";

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    private static Element rootElement2(String innerText) {
        return nb2.element("feed")
                .plusChild(nb2.textElement("title", "Example Feed"))
                .plusChild(
                        nb2.resolve("example", EXAMPLE_NS)
                                .element(
                                        "rights",
                                        ImmutableMap.of("type", "xhtml", "example:type", "silly")
                                )
                                .plusChild(
                                        nb2.resolve("example", EXAMPLE_NS)
                                                .resolve("", XHTML_NS)
                                                .textElement("div", innerText)
                                )
                );
    }

    private static Element prepareForComparison(Element element) {
        Element result = Elements.transformDescendantElementsOrSelf(
                element,
                e -> {
                    if (hasName(XHTML_NS, "div").test(e)) {
                        return e.withChildren(ImmutableList.of(new Text(e.text(), false)));
                    } else {
                        return e.withChildren(
                                e.children().stream().filter(n -> !(n instanceof Comment)).collect(ImmutableList.toImmutableList()));
                    }
                }
        );
        return Elements.removeInterElementWhitespace(result);
    }
}
