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

package eu.cdevreeze.yaidom4j.conformance.xmltest;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.usingsaxon.SaxonNodes;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.JaxpDomToImmutableDomConverter;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Steps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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
        assertSame(xdmDocNode.getNodeKind(), XdmNodeKind.DOCUMENT);

        int docChildCount = (int) xdmDocNode.select(Steps.child()).count();

        // Default (non-validating) NS instance of SAXParserFactory, in order to allow for entity processing
        SAXParserFactory spf = SAXParserFactory.newDefaultNSInstance();
        DocumentParser documentParser = DocumentParsers.builder(spf).build();

        Document doc = documentParser.parse(xmlUri);
        Document outputDoc = documentParser.parse(outputXmlUri);

        // It is the SAX parser that does not fire the "comment" call; not much I can do about that
        Set<Path> excludedFromDocChildCountCheck = Set.of(Path.of("037.xml"), Path.of("038.xml"));

        if (!excludedFromDocChildCountCheck.contains(Path.of(xmlUri).getFileName())) {
            // Counting the document children (root element plus comments plus processing instructions)
            assertEquals(docChildCount, doc.children().size());
        }

        // Besides successful "yaidom4j parsing", the most important check
        assertEquals(
                makeComparable(outputDoc.documentElement().toClarkNode()),
                makeComparable(doc.documentElement().toClarkNode())
        );

        SaxonNodes.Element saxonDocElement = new SaxonNodes.Element(
                xdmDocNode.select(Steps.child(c -> c.getNodeKind() == XdmNodeKind.ELEMENT))
                        .findFirst()
                        .orElseThrow()
        );

        // Comparing at least the structure. This should probably be improved.
        assertEquals(
                removeProcessingInstructions(removeText(makeComparable(saxonDocElement.toClarkNode()))),
                removeProcessingInstructions(removeText(makeComparable(doc.documentElement().toClarkNode())))
        );

        // W3C DOM
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultNSInstance();
        org.w3c.dom.Document domDoc = dbf.newDocumentBuilder().parse(new File(xmlUri));
        Document docFromDomDoc = JaxpDomToImmutableDomConverter.convertDocument(domDoc);

        // It is the DOM parser that does not accept a colon as attribute name; not much I can do about that
        // For 097.xml, the DOM parser recognizes 2 attributes where it should parse only one attribute
        Set<Path> excludedFromDomEquivCheck = Set.of(Path.of("012.xml"), Path.of("097.xml"));

        if (!excludedFromDomEquivCheck.contains(Path.of(xmlUri).getFileName())) {
            assertEquals(
                    makeComparable(doc.documentElement().toClarkNode()),
                    makeComparable(docFromDomDoc.documentElement().toClarkNode())
            );
        }
    }

    private static Stream<Arguments> provideXmlDocUris() throws URISyntaxException {
        URI xmlDirUri =
                Objects.requireNonNull(ValidStandaloneXmlTest.class.getResource("/xmlconformancesuite/xmlconf/xmltest/valid/sa"))
                        .toURI();
        File dir = new File(xmlDirUri);
        List<File> xmlFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".xml"))
                .sorted()
                .toList();
        return xmlFiles.stream().map(f -> Arguments.of(f.toURI(), convertToUriInOutputDir(f.toURI())));
    }

    private static URI convertToUriInOutputDir(URI xmlDocUri) {
        Preconditions.checkArgument(xmlDocUri.getScheme().equals("file"));
        Path path = Path.of(xmlDocUri);
        Path simpleName = path.getFileName();
        return path.getParent().resolve("out").resolve(simpleName).toUri();
    }

    // Lenient comparison with Saxon

    private static ClarkNodes.Element removeProcessingInstructions(ClarkNodes.Element element) {
        return element.transformDescendantElementsOrSelf(elem ->
                elem.withChildren(
                        elem.children().stream()
                                .filter(c -> !(c instanceof ClarkNodes.ProcessingInstruction))
                                .collect(ImmutableList.toImmutableList())
                )
        );
    }

    private static ClarkNodes.Element removeText(ClarkNodes.Element element) {
        return element.transformDescendantElementsOrSelf(elem ->
                elem.withChildren(
                        elem.children().stream()
                                .filter(c -> !(c instanceof ClarkNodes.Text))
                                .collect(ImmutableList.toImmutableList())
                )
        );
    }

    // Comparing XML (with canonical XML)

    private static ClarkNodes.Element makeComparable(ClarkNodes.Element element) {
        return element.transformDescendantElementsOrSelf(elem -> {
            List<ClarkNodes.Node> childrenWithoutComments = elem.children()
                    .stream()
                    .filter(n -> !(n instanceof ClarkNodes.Comment))
                    .toList();
            List<ClarkNodes.Node> childrenWithCombinedTextNodes =
                    childrenWithoutComments.stream().allMatch(n -> n instanceof ClarkNodes.Text) ?
                            List.of(new ClarkNodes.Text(elem.text())) :
                            childrenWithoutComments;
            // Normalize whitespace in element text child nodes
            List<ClarkNodes.Node> children = childrenWithCombinedTextNodes.stream()
                    .map(n -> (n instanceof ClarkNodes.Text t) ? normalizeWhitespace(t) : n)
                    .toList();
            // Normalize whitespace in attribute values as well
            return elem.withChildren(ImmutableList.copyOf(children))
                    .withAttributes(
                            elem.attributes()
                                    .entrySet()
                                    .stream()
                                    .map(kv -> Map.entry(kv.getKey(), normalizeWhitespace(kv.getValue())))
                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
        }).removeInterElementWhitespace();
    }

    private static ClarkNodes.Text normalizeWhitespace(ClarkNodes.Text text) {
        return new ClarkNodes.Text(normalizeWhitespace(normalizeWhitespace(text.value())));
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
