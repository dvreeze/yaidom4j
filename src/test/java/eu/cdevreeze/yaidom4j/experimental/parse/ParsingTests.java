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

package eu.cdevreeze.yaidom4j.experimental.parse;

import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees;
import eu.cdevreeze.yaidom4j.experimental.jaxp.DocumentBuilders;
import eu.cdevreeze.yaidom4j.experimental.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.experimental.jaxp.TransformerHandlers;
import eu.cdevreeze.yaidom4j.experimental.testsupport.saxon.SaxonNodes;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Document parsing test.
 * <p>
 * This is not a regular unit test, in that it assumes node querying etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class ParsingTests {

    private static final String relativeXmlPath = "/sample-xbrl-instance.xml";

    private static final Processor saxonProcessor = new Processor();

    private static @Nullable Document document;

    @BeforeAll
    public static void parseDocument() throws SaxonApiException, IOException {
        URL url = Objects.requireNonNull(ParsingTests.class.getResource(relativeXmlPath));

        XdmNode docNode = saxonProcessor.newDocumentBuilder().build(
                new StreamSource(url.openStream())
        );
        document = new Document(new SaxonNodes.Document(docNode).toGreenTreeNode())
                .removeInterElementWhitespace();
    }

    @Test
    void testSimpleParsing() throws IOException {
        URL url = Objects.requireNonNull(ParsingTests.class.getResource(relativeXmlPath));

        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser.parse(new InputSource(url.openStream()));

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    void testSaxBasedParsing() throws URISyntaxException {
        URL url = Objects.requireNonNull(ParsingTests.class.getResource(relativeXmlPath));

        DocumentProducingSaxHandler handler = new DocumentProducingSaxHandler();
        SaxParsers.parse(url.toURI(), handler);
        Document doc = handler.resultingDocument();

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    void testDomBasedParsing() throws URISyntaxException {
        URL url = Objects.requireNonNull(ParsingTests.class.getResource(relativeXmlPath));

        org.w3c.dom.Document w3cDoc = DocumentBuilders.parse(url.toURI());
        Document doc = new Document(JaxpDomToImmutableDomConverter.convertDocument(w3cDoc));

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    void testTransformerBasedParsing() throws ParserConfigurationException, SAXException, IOException {
        URL url = Objects.requireNonNull(ParsingTests.class.getResource(relativeXmlPath));

        TransformerHandler th = TransformerHandlers.newTransformerHandler();
        ImmutableDomResult domResult = new ImmutableDomResult();
        th.setResult(domResult);
        XMLReader saxReader = SaxParsers.newNonValidatingSaxParserFactory().newSAXParser().getXMLReader();
        saxReader.setContentHandler(th);
        saxReader.parse(new InputSource(url.openStream()));
        Document doc = domResult.getDocument();

        assertEquals(
                removeComments(
                        combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace()
                ),
                removeComments(
                        combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
                )
        );
    }

    // Comparing with Saxon

    private static GreenClarkTrees.Element removeComments(GreenClarkTrees.Element element) {
        return element.transformDescendantOrSelfElements(e ->
                e.withChildren(e.children().stream().filter(n -> !(n instanceof GreenClarkTrees.Comment)).toList())
        );
    }

    private static GreenClarkTrees.Element combineAdjacentTextNodes(GreenClarkTrees.Element element) {
        return element.transformDescendantOrSelfElements(elem ->
                elem.withChildren(List.copyOf(combineAdjacentTextNodes(elem.children())))
        );
    }

    private static List<GreenClarkTrees.CanBeChild> combineAdjacentTextNodes(List<GreenClarkTrees.CanBeChild> nodes) {
        if (nodes.isEmpty()) {
            return nodes;
        }
        if (!(nodes.getFirst() instanceof GreenClarkTrees.Text)) {
            GreenClarkTrees.CanBeChild firstNode = nodes.getFirst();
            List<GreenClarkTrees.CanBeChild> remainder = nodes.subList(1, nodes.size());

            // Recursion
            List<GreenClarkTrees.CanBeChild> remainderWithCombinedTextNodes = combineAdjacentTextNodes(remainder);

            List<GreenClarkTrees.CanBeChild> result = new ArrayList<>();
            result.add(firstNode);
            result.addAll(remainderWithCombinedTextNodes);
            return List.copyOf(result);
        }

        List<GreenClarkTrees.CanBeChild> adjacentTextNodes = nodes.stream().takeWhile(n -> (n instanceof GreenClarkTrees.Text)).toList();
        List<GreenClarkTrees.CanBeChild> remainder = nodes.stream().dropWhile(n -> (n instanceof GreenClarkTrees.Text)).toList();
        List<GreenClarkTrees.CanBeChild> combinedTextNodes = adjacentTextNodes.isEmpty() ?
                List.of() :
                List.of(new GreenClarkTrees.Text(
                        adjacentTextNodes.stream()
                                .map(n -> ((GreenClarkTrees.Text) n).value())
                                .collect(Collectors.joining())
                ));

        // Recursion
        List<GreenClarkTrees.CanBeChild> remainderWithCombinedTextNodes = combineAdjacentTextNodes(remainder);

        List<GreenClarkTrees.CanBeChild> result = new ArrayList<>(combinedTextNodes);
        result.addAll(remainderWithCombinedTextNodes);
        return List.copyOf(result);
    }

    // Comparing XML (with canonical XML)

    private static GreenClarkTrees.Element makeComparableWithCanonicalXml(GreenClarkTrees.Element element) {
        return element.transformDescendantOrSelfElements(elem -> {
            List<GreenClarkTrees.CanBeChild> childrenWithoutComments = elem.children()
                    .stream()
                    .filter(n -> !(n instanceof GreenClarkTrees.Comment))
                    .toList();
            List<GreenClarkTrees.CanBeChild> childrenWithCombinedTextNodes =
                    combineAdjacentTextNodes(childrenWithoutComments);
            // Normalize whitespace in element text child nodes
            List<GreenClarkTrees.CanBeChild> children = childrenWithCombinedTextNodes.stream()
                    .map(n -> (n instanceof GreenClarkTrees.Text t) ? normalizeWhitespace(t) : n)
                    .toList();
            // Normalize whitespace in attribute values as well
            return elem.withChildren(List.copyOf(children))
                    .withAttributes(
                            elem.attributes()
                                    .entrySet()
                                    .stream()
                                    .map(kv -> Map.entry(kv.getKey(), normalizeWhitespace(kv.getValue())))
                                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }).removeInterElementWhitespace();
    }

    private static GreenClarkTrees.Text normalizeWhitespace(GreenClarkTrees.Text text) {
        return new GreenClarkTrees.Text(normalizeWhitespace(text.value()));
    }

    private static String normalizeWhitespace(String str) {
        int[] almostNormalizedWhitespaceChars = str.chars()
                .map(c -> Character.isWhitespace(c) ? ' ' : c)
                .toArray();
        int[] normalizedWhitespaceChars = combineSpaces(almostNormalizedWhitespaceChars);
        return new String(normalizedWhitespaceChars, 0, normalizedWhitespaceChars.length);
    }

    private static int[] combineSpaces(int[] chars) {
        return combineSpaces(Arrays.stream(chars).boxed().toList())
                .stream()
                .mapToInt(i -> i)
                .toArray();
    }

    private static List<Integer> combineSpaces(List<Integer> chars) {
        if (chars.isEmpty()) {
            return List.of();
        }
        List<Integer> nonSpaces = chars.stream().takeWhile(c -> !Character.isWhitespace(c)).toList();
        List<Integer> remainder = chars.stream().dropWhile(c -> !Character.isWhitespace(c)).toList();
        boolean nextCharIsSpace = !remainder.isEmpty();

        // Recursion
        List<Integer> remainderWithCombinedSpaces =
                combineSpaces(remainder.stream().dropWhile(Character::isWhitespace).toList());

        List<Integer> result = new ArrayList<>(nonSpaces);
        if (nextCharIsSpace) {
            result.add((int) ' ');
        }
        result.addAll(remainderWithCombinedSpaces);
        return List.copyOf(result);
    }
}
