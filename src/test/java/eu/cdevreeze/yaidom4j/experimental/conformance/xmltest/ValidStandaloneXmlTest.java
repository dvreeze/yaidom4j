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

package eu.cdevreeze.yaidom4j.experimental.conformance.xmltest;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Document;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import eu.cdevreeze.yaidom4j.experimental.parse.JaxpDomToImmutableDomConverter;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Steps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Using the XML conformance suite, in particular the XML files in path "xmlconf/xmltest/valid/sa",
 * this class tests whether these XML files are parsed successfully into yaidom4j immutable trees,
 * and that they are equal to the corresponding XML files in the "out" subdirectory.
 * <p>
 * The performance suite is about testing conformance of XML processors. Of course, yaidom4j is not
 * an XML processor. The purpose of this test is to check that yaidom4j can handle whatever is parsed
 * by an XML processor as valid (standalone) XML.
 * <p>
 * This is a parameterized test. See
 * <a href="https://www.baeldung.com/parameterized-tests-junit-5">parameterized test</a>
 * for more on how to create parameterized tests.
 *
 * @author Chris de Vreeze
 */
class ValidStandaloneXmlTest {

    private final Processor processor = new Processor(false);

    @ParameterizedTest
    @MethodSource("provideXmlDocUris")
    void xmlParsedIntoYaidom4jTree(URI xmlUri, URI outputXmlUri) throws SaxonApiException, ParserConfigurationException, IOException, SAXException {
        // First parse with Saxon
        DocumentBuilder saxonDocBuilder = processor.newDocumentBuilder();
        XdmNode xdmDocNode = saxonDocBuilder.build(new File(xmlUri));
        assertSame(XdmNodeKind.DOCUMENT, xdmDocNode.getNodeKind());

        int docChildCount = (int) xdmDocNode.select(Steps.child()).count();

        // Default (non-validating) NS instance of SAXParserFactory, in order to allow for entity processing
        SAXParserFactory spf = SAXParserFactory.newDefaultNSInstance();
        DocumentParser documentParser = DocumentParsers.builder(spf).build();

        Document doc = documentParser.parse(xmlUri).greenTreeNode();
        Document outputDoc = documentParser.parse(outputXmlUri).greenTreeNode();

        // Counting the document children (root element plus comments plus processing instructions)
        assertEquals(docChildCount, doc.children().size());

        // Besides successful "yaidom4j parsing", the most important check
        assertEquals(
                makeComparableWithCanonicalXml(outputDoc.documentElement().toClarkNode()),
                makeComparableWithCanonicalXml(doc.documentElement().toClarkNode())
        );

        // W3C DOM
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultNSInstance();
        org.w3c.dom.Document domDoc = dbf.newDocumentBuilder().parse(new File(xmlUri));
        Document docFromDomDoc = JaxpDomToImmutableDomConverter.convertDocument(domDoc);

        assertEquals(
                combineAdjacentTextNodes(doc.documentElement().toClarkNode().removeInterElementWhitespace()),
                combineAdjacentTextNodes(docFromDomDoc.documentElement().toClarkNode().removeInterElementWhitespace())
        );
    }

    private static Stream<Arguments> provideXmlDocUris() throws URISyntaxException {
        URI xmlDirUri =
                Objects.requireNonNull(ValidStandaloneXmlTest.class.getResource("/xmlconformancesuite/xmlconf/xmltest/valid/sa"))
                        .toURI();
        File dir = new File(xmlDirUri);
        List<File> xmlFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".xml"))
                .filter(f -> !isExcludedXml(f))
                .sorted()
                .toList();
        return xmlFiles.stream().map(f -> Arguments.of(f.toURI(), convertToUriInOutputDir(f.toURI())));
    }

    private static boolean isExcludedXml(File xmlFile) {
        String fileName = xmlFile.getName();

        return Set.of(
                // It is the DOM parser that does not accept a colon as attribute name; not much I can do about that
                "012.xml",
                // Document child (comment) found which really isn't a document child. Saxon gets it right, though.
                "066.xml",
                // For 097.xml, the DOM parser recognizes 2 attributes where it should parse only one attribute
                "097.xml"
        ).contains(fileName);
    }

    private static URI convertToUriInOutputDir(URI xmlDocUri) {
        PreconditionUtil.checkArgument(xmlDocUri.getScheme().equals("file"));
        Path path = Path.of(xmlDocUri);
        Path simpleName = path.getFileName();
        return path.getParent().resolve("out").resolve(simpleName).toUri();
    }

    // Comparing with Saxon

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
