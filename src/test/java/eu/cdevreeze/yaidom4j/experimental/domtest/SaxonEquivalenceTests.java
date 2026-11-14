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

import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentProducingSaxHandler;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentConsumingSaxEventGenerator;
import net.sf.saxon.BasicTransformerFactory;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Steps;
import net.sf.saxon.tree.tiny.TinyBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.experimental.testsupport.saxon.SaxonElementSteps.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Immutable DOM query tests against books.xml file. This test uses bootstrapping from Saxon,
 * conversions back to Saxon, and tests equivalence of yaidom4j queries with corresponding Saxon queries.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class SaxonEquivalenceTests {

    private static final String NS = "http://bookstore";

    private static Processor processor;
    private static Element rootElement;

    private static Document parseUsingSaxon(String classpathUri, Processor processor) throws SaxonApiException, TransformerException {
        DocumentBuilder docBuilder = processor.newDocumentBuilder();

        InputStream inputStream = SaxonEquivalenceTests.class.getResourceAsStream(classpathUri);
        XdmNode xdmDoc = docBuilder.build(new StreamSource(inputStream));

        TransformerFactory tff = new BasicTransformerFactory(processor.getUnderlyingConfiguration());
        Transformer tf = tff.newTransformer();

        DocumentProducingSaxHandler saxHandler = new DocumentProducingSaxHandler();

        tf.transform(xdmDoc.asSource(), new SAXResult(saxHandler));

        return saxHandler.resultingDocument().removeInterElementWhitespace();
    }

    private static XdmNode convertToSaxon(Document doc, Processor processor) {
        // Copied and adapted from similar code in yaidom. I could not possibly have come up with this myself.

        PipelineConfiguration pipe = processor.getUnderlyingConfiguration().makePipelineConfiguration();

        TinyBuilder tinyBuilder = new TinyBuilder(pipe);

        ReceivingContentHandler receivingContentHandler = new ReceivingContentHandler();
        receivingContentHandler.setPipelineConfiguration(pipe);
        receivingContentHandler.setReceiver(tinyBuilder);

        var saxEventGenerator = new DocumentConsumingSaxEventGenerator(receivingContentHandler);

        saxEventGenerator.processDocument(doc);

        return new XdmNode(tinyBuilder.getTree().getRootNode());
    }

    private Element rootElement() {
        return rootElement;
    }

    @BeforeAll
    protected static void parseRootElement() throws SaxonApiException, TransformerException {
        processor = new Processor(false);
        rootElement = parseUsingSaxon("/books.xml", processor).documentElement();
    }

    @Test
    void testQueryNamespacesOfAllElements() {
        var namespaces = rootElement().descendantOrSelfElementStream()
                .map(Element::elementName)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(Set.of(NS), namespaces);

        var namespaces2 = rootElement().descendantOrSelfElementStream()
                .map(Element::elementName)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(namespaces, namespaces2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        var namespaces3 = xdmDoc
                .select(descendantOrSelfElements())
                .map(n -> n.getNodeName().getNamespace())
                .collect(Collectors.toSet());

        assertEquals(namespaces, namespaces3);
    }

    @Test
    void testQueryNamesOfAllElements() {
        var names = rootElement().descendantOrSelfElementStream()
                .map(Element::elementName)
                .distinct()
                .toList();

        assertEquals(List.of(
                new QName(NS, "Bookstore"),
                new QName(NS, "Book"),
                new QName(NS, "Title"),
                new QName(NS, "Authors"),
                new QName(NS, "Author"),
                new QName(NS, "First_Name"),
                new QName(NS, "Last_Name"),
                new QName(NS, "Remark"),
                new QName(NS, "Magazine")
        ), names);

        var names2 = rootElement().descendantOrSelfElementStream()
                .map(Element::elementName)
                .distinct()
                .toList();

        assertEquals(names, names2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        var names3 = xdmDoc
                .select(descendantOrSelfElements())
                .map(n -> n.getNodeName().getStructuredQName().toJaxpQName())
                .distinct()
                .toList();

        assertEquals(names, names3);
    }

    @Test
    void testQueryPrefixOfRootElement() {
        assertEquals("books", rootElement().elementName().getPrefix());

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        assertEquals(
                "books",
                xdmDoc
                        .select(descendantOrSelfElements())
                        .findFirst()
                        .orElseThrow()
                        .getNodeName()
                        .getPrefix()
        );
    }

    @Test
    void testQueryPrefixOfDescendantElements() {
        var prefixes = rootElement().descendantElementStream()
                .map(Element::elementName)
                .map(QName::getPrefix)
                .collect(Collectors.toSet());

        assertEquals(Set.of(""), prefixes);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        var prefixes2 = xdmDoc
                .select(descendantOrSelfElements())
                .findFirst()
                .orElseThrow()
                .select(descendantElements())
                .map(n -> n.getNodeName().getStructuredQName().getPrefix())
                .collect(Collectors.toSet());

        assertEquals(Set.of(""), prefixes2);
    }

    @Test
    void testQueryChildElementNames() {
        Map<QName, Long> childElemCounts =
                rootElement().childElementStream()
                        .collect(Collectors.groupingBy(Element::elementName, Collectors.counting()));

        assertEquals(
                Map.of(new QName(NS, "Book"), 4L, new QName(NS, "Magazine"), 4L),
                childElemCounts
        );

        Map<QName, Long> childElemCounts2 =
                rootElement().topmostDescendantOrSelfElementStream(e ->
                                GreenXmlTrees.Element.hasName(NS, "Book").test(e.greenTreeNode()) ||
                                        GreenXmlTrees.Element.hasName(NS, "Magazine").test(e.greenTreeNode())
                        )
                        .collect(Collectors.groupingBy(Element::elementName, Collectors.counting()));

        assertEquals(childElemCounts, childElemCounts2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        Map<QName, Long> childElemCounts3 = xdmDoc
                .select(
                        topmostDescendantOrSelfElements(n ->
                                n.getNodeKind().equals(XdmNodeKind.ELEMENT) &&
                                        (n.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Book")) ||
                                                n.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Magazine")))
                        )
                )
                .collect(Collectors.groupingBy(
                        n -> n.getNodeName().getStructuredQName().toJaxpQName(),
                        Collectors.counting()));

        assertEquals(childElemCounts, childElemCounts3);
    }

    @Test
    void testQueryMagazineMonths() {
        List<String> magazineMonths = rootElement()
                .greenTreeNode()
                .childElementStream(GreenXmlTrees.Element.hasName(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(
                List.of("January", "February", "February", "March"),
                magazineMonths
        );

        List<String> magazineMonths2 = rootElement()
                .greenTreeNode()
                .descendantOrSelfElementStream(GreenXmlTrees.Element.hasName(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(magazineMonths, magazineMonths2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        List<String> magazineMonths3 = xdmDoc
                .select(
                        descendantOrSelfElements(NS, "Magazine")
                                .then(Steps.attribute("Month"))
                )
                .map(XdmItem::getStringValue)
                .toList();

        assertEquals(magazineMonths, magazineMonths3);
    }

    @Test
    void testQueryMagazineTitles() {
        List<String> magazineTitles = rootElement().greenTreeNode().childElementStream(GreenXmlTrees.Element.hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.childElementStream(GreenXmlTrees.Element.hasName(NS, "Title")))
                .map(GreenXmlTrees.Element::text)
                .distinct()
                .toList();

        assertEquals(
                List.of(
                        "National Geographic",
                        "Newsweek",
                        "Hector and Jeff's Database Hints"
                ),
                magazineTitles
        );

        List<String> magazineTitles2 = rootElement()
                .greenTreeNode()
                .descendantOrSelfElementStream(GreenXmlTrees.Element.hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.descendantOrSelfElementStream(GreenXmlTrees.Element.hasName(NS, "Title")))
                .map(GreenXmlTrees.Element::text)
                .distinct()
                .toList();

        assertEquals(magazineTitles, magazineTitles2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        List<String> magazineTitles3 = xdmDoc
                .select(
                        descendantOrSelfElements(NS, "Magazine")
                                .then(descendantOrSelfElements(NS, "Title"))
                )
                .map(e -> e.select(
                                Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                        ).map(XdmItem::getStringValue).collect(Collectors.joining())
                )
                .distinct()
                .toList();

        assertEquals(magazineTitles, magazineTitles3);
    }

    @Test
    void testQueryBooksCoauthoredByJenniferWidom() {
        Predicate<Element> authorIsJenniferWidom = authorElem ->
                Element.hasName(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(Element.hasName(NS, "First_Name"))
                                .anyMatch(Element.hasOnlyText("Jennifer")) &&
                        authorElem.childElementStream(Element.hasName(NS, "Last_Name"))
                                .anyMatch(Element.hasOnlyText("Widom"));

        Predicate<Element> bookCowrittenByJenniferWidom = bookElem ->
                Element.hasName(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(Element.hasName(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookTitlesCoauthoredByJenniferWidom =
                rootElement().childElementStream(Element.hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.childElementStream(Element.hasName(NS, "Title")))
                        .map(Element::text)
                        .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "A First Course in Database Systems",
                        "Database Systems: The Complete Book",
                        "Jennifer's Economical Database Hints"
                ),
                bookTitlesCoauthoredByJenniferWidom
        );

        Set<String> bookTitlesCoauthoredByJenniferWidom2 =
                rootElement().descendantOrSelfElementStream(Element.hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.descendantOrSelfElementStream(Element.hasName(NS, "Title")))
                        .map(Element::text)
                        .collect(Collectors.toSet());

        assertEquals(bookTitlesCoauthoredByJenniferWidom, bookTitlesCoauthoredByJenniferWidom2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        Predicate<XdmNode> authorIsJenniferWidomInSaxon = authorElem ->
                authorElem.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Author")) &&
                        authorElem.select(childElements(NS, "First_Name"))
                                .anyMatch(e -> {
                                            var textValue = e.select(
                                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                                            ).map(XdmItem::getStringValue).collect(Collectors.joining());
                                            return textValue.equals("Jennifer");
                                        }
                                ) &&
                        authorElem.select(childElements(NS, "Last_Name"))
                                .anyMatch(e -> {
                                            var textValue = e.select(
                                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                                            ).map(XdmItem::getStringValue).collect(Collectors.joining());
                                            return textValue.equals("Widom");
                                        }
                                );

        Predicate<XdmNode> bookCowrittenByJenniferWidomInSaxon = bookElem ->
                bookElem.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Book")) &&
                        bookElem.select(descendantElements(NS, "Author"))
                                .anyMatch(authorIsJenniferWidomInSaxon);

        Set<String> bookTitlesCoauthoredByJenniferWidom3 = xdmDoc
                .select(descendantOrSelfElements(NS, "Book"))
                .filter(bookCowrittenByJenniferWidomInSaxon)
                .flatMap(e -> e.select(descendantOrSelfElements(NS, "Title")))
                .map(e -> e.select(
                                Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                        ).map(XdmItem::getStringValue).collect(Collectors.joining())
                )
                .collect(Collectors.toSet());

        assertEquals(bookTitlesCoauthoredByJenniferWidom, bookTitlesCoauthoredByJenniferWidom3);
    }

    @Test
    void testQueryAuthorNames() {
        Function<Element, String> getAuthorName = authorElem -> {
            PreconditionUtil.checkArgument(Element.hasName(NS, "Author").test(authorElem));

            String firstName = authorElem.childElementStream(Element.hasName(NS, "First_Name"))
                    .findFirst()
                    .map(Element::text)
                    .orElse("");

            String lastName = authorElem.childElementStream(Element.hasName(NS, "Last_Name"))
                    .findFirst()
                    .map(Element::text)
                    .orElse("");

            return String.format("%s %s", firstName, lastName).strip();
        };

        Set<String> authorNames = rootElement()
                .descendantElementStream(Element.hasName(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina"),
                authorNames
        );

        Set<String> authorNames2 = rootElement()
                .topmostDescendantOrSelfElementStream(Element.hasName(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(authorNames, authorNames2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        Function<XdmNode, String> getAuthorNameInSaxon = authorElem -> {
            PreconditionUtil.checkArgument(
                    authorElem.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Author"))
            );

            String firstName = authorElem.select(childElements(NS, "First_Name"))
                    .findFirst()
                    .map(e -> e.select(
                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                            ).map(XdmItem::getStringValue).collect(Collectors.joining())
                    )
                    .orElse("");

            String lastName = authorElem.select(childElements(NS, "Last_Name"))
                    .findFirst()
                    .map(e -> e.select(
                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                            ).map(XdmItem::getStringValue).collect(Collectors.joining())
                    )
                    .orElse("");

            return String.format("%s %s", firstName, lastName).strip();
        };

        Set<String> authorNames3 = xdmDoc
                .select(topmostDescendantElements(NS, "Author"))
                .map(getAuthorNameInSaxon)
                .collect(Collectors.toSet());

        assertEquals(authorNames, authorNames3);
    }

    @Test
    void testQueryBookIsbnsCoauthoredByJenniferWidom() {
        Predicate<Element> authorIsJenniferWidom = authorElem ->
                Element.hasName(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(Element.hasName(NS, "First_Name"))
                                .anyMatch(Element.hasOnlyTextAfterStripping("Jennifer")) &&
                        authorElem.childElementStream(Element.hasName(NS, "Last_Name"))
                                .anyMatch(Element.hasOnlyTextAfterStripping("Widom"));

        Predicate<Element> bookCowrittenByJenniferWidom = bookElem ->
                Element.hasName(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(Element.hasName(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookIsbnsCoauthoredByJenniferWidom =
                rootElement().childElementStream(Element.hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(
                Set.of("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-9-88-777777-6"),
                bookIsbnsCoauthoredByJenniferWidom
        );

        Set<String> bookIsbnsCoauthoredByJenniferWidom2 =
                rootElement().topmostDescendantOrSelfElementStream(Element.hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(bookIsbnsCoauthoredByJenniferWidom, bookIsbnsCoauthoredByJenniferWidom2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        Predicate<XdmNode> authorIsJenniferWidomInSaxon = authorElem ->
                authorElem.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Author")) &&
                        authorElem.select(childElements(NS, "First_Name"))
                                .anyMatch(e -> {
                                            var textValue = e.select(
                                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                                            ).map(XdmItem::getStringValue).collect(Collectors.joining());
                                            return textValue.equals("Jennifer");
                                        }
                                ) &&
                        authorElem.select(childElements(NS, "Last_Name"))
                                .anyMatch(e -> {
                                            var textValue = e.select(
                                                    Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                                            ).map(XdmItem::getStringValue).collect(Collectors.joining());
                                            return textValue.equals("Widom");
                                        }
                                );

        Predicate<XdmNode> bookCowrittenByJenniferWidomInSaxon = bookElem ->
                bookElem.getNodeName().getStructuredQName().toJaxpQName().equals(new QName(NS, "Book")) &&
                        bookElem.select(descendantElements(NS, "Author"))
                                .anyMatch(authorIsJenniferWidomInSaxon);

        Set<String> bookIsbnsCoauthoredByJenniferWidom3 = xdmDoc
                .select(topmostDescendantElements(NS, "Book"))
                .filter(bookCowrittenByJenniferWidomInSaxon)
                .flatMap(e -> e.select(Steps.attribute("ISBN")).map(XdmNode::getStringValue))
                .collect(Collectors.toSet());

        assertEquals(bookIsbnsCoauthoredByJenniferWidom, bookIsbnsCoauthoredByJenniferWidom3);
    }

    @Test
    void testQueryFebruaryMagazines() {
        List<String> februaryMagazineTitles = rootElement()
                .childElementStream(Element.hasName(NS, "Magazine"))
                .filter(Element.hasAttributeValue("Month", "February"))
                .flatMap(e -> e.childElementStream(Element.hasName(NS, "Title")))
                .map(Element::text)
                .toList();

        assertEquals(
                List.of("National Geographic", "Newsweek"),
                februaryMagazineTitles
        );

        List<String> februaryMagazineTitles2 = rootElement()
                .topmostDescendantOrSelfElementStream(Element.hasName(NS, "Magazine"))
                .filter(Element.hasAttributeValue("Month", "February"))
                .flatMap(e -> e.descendantOrSelfElementStream(Element.hasName(NS, "Title")))
                .map(Element::text)
                .toList();

        assertEquals(februaryMagazineTitles, februaryMagazineTitles2);

        Document doc = new Document(new GreenXmlTrees.Document(Optional.empty(), List.of(rootElement().greenTreeNode())));
        XdmNode xdmDoc = convertToSaxon(doc, processor);

        List<String> februaryMagazineTitles3 = xdmDoc
                .select(topmostDescendantOrSelfElements(NS, "Magazine"))
                .filter(e -> e.select(Steps.attribute("Month")).anyMatch(a -> "February".equals(a.getStringValue())))
                .flatMap(e -> e.select(descendantOrSelfElements(NS, "Title")))
                .map(e -> e.select(
                                Steps.child(t -> t.getNodeKind().equals(XdmNodeKind.TEXT))
                        ).map(XdmItem::getStringValue).collect(Collectors.joining())
                )
                .toList();

        assertEquals(februaryMagazineTitles, februaryMagazineTitles3);
    }
}
