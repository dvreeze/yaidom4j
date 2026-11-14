/*
 * Copyright 2024-2026 Chris de Vreeze
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

package eu.cdevreeze.yaidom4j.experimental.domtest;

import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Comment;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Text;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Element.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Node creation tests against sample XML files from
 * <a href="https://www.lenzconsulting.com/namespaces/">Understanding XML Namespaces</a>.
 * These files are equal, except that they use different namespace prefixes and have the namespace
 * declarations in different elements.
 * <p>
 * This is not a regular unit test, in that it assumes parsing, node equality comparisons etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GreenTreeNodesTests {

    private Document doc1;
    private Document doc2;
    private Document doc3;

    @BeforeAll
    void parseDocuments() {
        doc1 = parseDocument("/feed1.xml");
        doc2 = parseDocument("/feed2.xml");
        doc3 = parseDocument("/feed3.xml");
    }

    private Document parseDocument(String xmlClasspathResource) {
        InputStream inputStream = GreenTreeNodesTests.class.getResourceAsStream(xmlClasspathResource);
        return DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream))
                .greenTreeNode();
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

        assertEquals(doc1.documentElement().toClarkNode(), doc2.documentElement().toClarkNode());
        assertEquals(doc1.documentElement().toClarkNode(), doc3.documentElement().toClarkNode());
        assertEquals(doc3.documentElement().toClarkNode(), doc2.documentElement().toClarkNode());

        assertNotEquals(doc1.documentElement().toClarkNode(), rootElement1.toClarkNode());

        Element docRootElement1 = prepareForComparison(doc1.documentElement());
        Element docRootElement2 = prepareForComparison(doc2.documentElement());

        assertNotEquals(
                rootElement1.descendantOrSelfElementStream().map(e -> e.name().getPrefix()).collect(Collectors.toSet()),
                rootElement2.descendantOrSelfElementStream().map(e -> e.name().getPrefix()).collect(Collectors.toSet())
        );
        assertEquals(rootElement1.toClarkNode(), rootElement2.toClarkNode());

        assertEquals(docRootElement1.toClarkNode(), rootElement1.toClarkNode());
        assertEquals(docRootElement2.toClarkNode(), rootElement2.toClarkNode());
    }

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    private static final String EXAMPLE_NS = "http://xmlportfolio.com/xmlguild-examples";

    private static final NamespaceScope parentScope = NamespaceScope.from(Map.of(
                    "", ATOM_NS,
                    "xhtml", XHTML_NS,
                    "my", EXAMPLE_NS
            )
    );

    private static Element rootElement1(String innerText) {
        return GreenXmlTrees.elem(new QName(ATOM_NS, "feed", "")).withParentAttributeScope(parentScope)
                .transformSelfElement(e ->
                        e.plusChild(
                                GreenXmlTrees.elem(new QName(ATOM_NS, "title", ""))
                                        .withParentAttributeScope(e.namespaceScope())
                                        .plusText("Example Feed"))
                )
                .transformSelfElement(e ->
                        e.plusChild(
                                GreenXmlTrees.elem(new QName(ATOM_NS, "rights", ""))
                                        .withParentAttributeScope(e.namespaceScope())
                                        .plusAttribute(new QName("type"), "xhtml")
                                        .plusAttribute(new QName(EXAMPLE_NS, "type", "my"), "silly")
                                        .transformSelfElement(e2 ->
                                                e2.plusChild(
                                                        GreenXmlTrees.elem(new QName(XHTML_NS, "div", "xhtml"))
                                                                .withParentAttributeScope(e2.namespaceScope())
                                                                .plusText(innerText)
                                                )
                                        )
                        )
                );
    }

    private static Element rootElement2(String innerText) {
        return GreenXmlTrees.elem(new QName(ATOM_NS, "feed", ""))
                .transformSelfElement(e ->
                        e.plusChild(GreenXmlTrees.elem(new QName(ATOM_NS, "title", ""))
                                .withParentAttributeScope(e.namespaceScope())
                                .plusText("Example Feed"))
                )
                .transformSelfElement(e ->
                        e.plusChild(
                                GreenXmlTrees.elem(new QName(ATOM_NS, "rights", ""))
                                        .withParentAttributeScope(
                                                e.namespaceScope().resolve("example", EXAMPLE_NS)
                                        )
                                        .plusAttribute(new QName("type"), "xhtml")
                                        .plusAttribute(new QName(EXAMPLE_NS, "type", "example"), "silly")
                                        .transformSelfElement(e2 ->
                                                e2.plusChild(
                                                        GreenXmlTrees.elem(new QName(XHTML_NS, "div", ""))
                                                                .withParentAttributeScope(e2.namespaceScope())
                                                                .plusText(innerText)
                                                )
                                        )
                        )
                );
    }

    private static Element prepareForComparison(Element element) {
        Element result = element.transformDescendantOrSelfElements(
                e -> {
                    if (hasName(XHTML_NS, "div").test(e)) {
                        return e.withChildren(List.of(new Text(e.text())));
                    } else {
                        return e.withChildren(
                                e.children().stream().filter(n -> !(n instanceof Comment)).toList());
                    }
                }
        );
        return result.removeInterElementWhitespace();
    }
}
