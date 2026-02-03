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

import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import net.sf.saxon.s9api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Using the XML conformance suite, in particular the XML files in path "xmlconf/eduni/namespaces/1.0",
 * this class tests whether the XML files with namespaces meet the test expectations.
 * <p>
 * The performance suite is about testing conformance of XML processors. Of course, yaidom4j is not
 * an XML processor. The purpose of this test is to check that yaidom4j can handle whatever is parsed
 * by an XML processor as valid (standalone) XML with namespaces.
 * <p>
 * This is a parameterized test. See
 * <a href="https://www.baeldung.com/parameterized-tests-junit-5">parameterized test</a>
 * for more on how to create parameterized tests.
 *
 * @author Chris de Vreeze
 */
class XmlWithNamespacesTest {

    private final Processor processor = new Processor(false);

    @ParameterizedTest
    @MethodSource("provideValidXmlDocUris")
    void xmlParsedIntoYaidom4jTree(URI xmlUri, Predicate<Element> expectation) throws SaxonApiException {
        // First parse with Saxon
        DocumentBuilder saxonDocBuilder = processor.newDocumentBuilder();
        XdmNode xdmDocNode = saxonDocBuilder.build(new File(xmlUri));
        assertSame(XdmNodeKind.DOCUMENT, xdmDocNode.getNodeKind());

        // Default (non-validating) NS instance of SAXParserFactory, in order to allow for entity processing
        SAXParserFactory spf = SAXParserFactory.newDefaultNSInstance();
        DocumentParser documentParser = DocumentParsers.builder(spf).build();

        Document doc = documentParser.parse(xmlUri);

        assertTrue(expectation.test(doc.documentElement()));
    }

    private static Stream<Arguments> provideValidXmlDocUris() throws URISyntaxException {
        Map<Path, Predicate<Element>> expectations = getAllExpectations();

        URI xmlDirUri =
                Objects.requireNonNull(ValidStandaloneXmlTest.class.getResource("/xmlconformancesuite/xmlconf/eduni/namespaces/1.0"))
                        .toURI();
        File dir = new File(xmlDirUri);
        List<File> xmlFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".xml"))
                .filter(f -> expectations.containsKey(Path.of(f.getName())))
                .filter(f -> !isExcludedXml(f))
                .sorted()
                .toList();
        return xmlFiles.stream().map(f -> Arguments.of(f.toURI(), expectations.get(Path.of(f.getName()))));
    }

    private static Map<Path, Predicate<Element>> getAllExpectations() {
        Map<Path, Predicate<Element>> expectations = new HashMap<>();

        expectations.put(
                Path.of("001.xml"),
                element -> element.name().getNamespaceURI().equals("http://example.org/namespace")
        );
        expectations.put(
                Path.of("002.xml"),
                element -> element.name().getNamespaceURI().equals("zarquon://example.org/namespace")
        );
        expectations.put(
                Path.of("003.xml"),
                element -> element.name().getNamespaceURI().equals("http://example.org/namespace#apples")
        );
        expectations.put(
                Path.of("004.xml"), // deprecated
                element -> element.name().getNamespaceURI().equals("namespaces/zaphod")
        );
        expectations.put(
                Path.of("005.xml"), // deprecated
                element -> element.name().getNamespaceURI().equals("#beeblebrox")
        );
        // 006.xml contains an error
        expectations.put(
                Path.of("007.xml"),
                element -> element.childElementStream(hasName("bar"))
                        .anyMatch(e -> e.attributes().keySet()
                                .stream()
                                .map(QName::getNamespaceURI)
                                .collect(Collectors.toSet())
                                .equals(
                                        Set.of(
                                                "http://example.org/wine",
                                                "http://Example.org/wine",
                                                "http://example.org/Wine"
                                        )
                                ))
        );
        expectations.put(
                Path.of("008.xml"),
                element -> element.childElementStream(hasName("bar"))
                        .anyMatch(e -> e.attributes().keySet()
                                .stream()
                                .map(QName::getNamespaceURI)
                                .collect(Collectors.toSet())
                                .equals(
                                        Set.of(
                                                "http://example.org/~wilbur",
                                                "http://example.org/%7ewilbur",
                                                "http://example.org/%7Ewilbur"
                                        )
                                ))
        );
        // 009.xml is not namespace-well-formed and therefore rejected by the parser
        // 010.xml is not namespace-well-formed and therefore rejected by the parser
        // 011.xml is not namespace-well-formed and therefore rejected by the parser
        // 012.xml is not namespace-well-formed and therefore rejected by the parser
        // 013.xml is not namespace-well-formed and therefore rejected by the parser
        // 014.xml is not namespace-well-formed and therefore rejected by the parser
        // 015.xml is not namespace-well-formed yet accepted by Saxon (or the underlying XML parser)
        // 016.xml is not namespace-well-formed and therefore rejected by the parser
        // 017.xml till 022.xml are considered invalid ("simple legal case"), but are technically fine
        expectations.put(
                Path.of("017.xml"),
                element -> element.name().getNamespaceURI().equals(XMLConstants.NULL_NS_URI)
        );
        expectations.put(
                Path.of("018.xml"),
                element -> element.namespaceScope().equals(NamespaceScope.of("", "http://example.org/namespace"))
        );
        expectations.put(
                Path.of("019.xml"),
                element -> element.namespaceScope().equals(NamespaceScope.of("a", "http://example.org/namespace"))
        );
        expectations.put(
                Path.of("020.xml"),
                element -> element.attributeOption(new QName("http://example.org/namespace", "attr"))
                        .isPresent()
        );
        expectations.put(
                Path.of("021.xml"),
                element -> element.descendantElementOrSelfStream()
                        .map(Element::namespaceScope)
                        .collect(Collectors.toSet())
                        .equals(Set.of(
                                NamespaceScope.empty(),
                                NamespaceScope.of("", "http://example.org/namespace")
                        ))
        );
        expectations.put(
                Path.of("022.xml"),
                element -> element.descendantElementOrSelfStream()
                        .map(Element::namespaceScope)
                        .collect(Collectors.toSet())
                        .equals(Set.of(
                                NamespaceScope.of("", "http://example.org/namespace"),
                                NamespaceScope.of("", "http://example.org/other-namespace")
                        ))
        );
        // 023.xml is not namespace-well-formed (in XML 1.0) and therefore rejected by the parser
        // 024.xml is considered invalid ("simple legal case"), but is technically fine
        expectations.put(
                Path.of("024.xml"),
                element -> element.descendantElementOrSelfStream()
                        .map(Element::namespaceScope)
                        .collect(Collectors.toSet())
                        .equals(Set.of(
                                NamespaceScope.of("a", "http://example.org/namespace"),
                                NamespaceScope.of("a", "http://example.org/other-namespace")
                        ))
        );
        // 025.xml is not namespace-well-formed and therefore rejected by the parser
        // 026.xml is not namespace-well-formed and therefore rejected by the parser
        // 027.xml and 028.xml are considered invalid ("simple legal case"), but are technically fine
        expectations.put(
                Path.of("027.xml"),
                element -> element.attributeOption(new QName(XMLConstants.XML_NS_URI, "lang"))
                        .stream().anyMatch(v -> v.equals("en"))
        );
        expectations.put(
                Path.of("028.xml"),
                element -> element.namespaceScope().equals(NamespaceScope.empty())
        );
        // 029.xml is not namespace-well-formed and therefore rejected by the parser
        // 030.xml is not namespace-well-formed and therefore rejected by the parser
        // 031.xml is not namespace-well-formed and therefore rejected by the parser
        // 032.xml is not namespace-well-formed and therefore rejected by the parser
        // 033.xml is not namespace-well-formed and therefore rejected by the parser
        // 034.xml is considered invalid ("simple legal case"), but is technically fine
        expectations.put(
                Path.of("034.xml"),
                element -> element.namespaceScope()
                        .equals(NamespaceScope.of("xml2", "http://example.org/namespace"))
        );
        // 035.xml is not namespace-well-formed and therefore rejected by the parser
        // 036.xml is not namespace-well-formed and therefore rejected by the parser
        // 037.xml till 041.xml are considered invalid ("simple legal case"), but are technically fine
        expectations.put(
                Path.of("037.xml"),
                element -> element.descendantElementOrSelfStream(hasName("bar"))
                        .anyMatch(e -> e.attributes().keySet().equals(
                                Set.of(
                                        new QName("http://example.org/~wilbur", "attr"),
                                        new QName("http://example.org/~kipper", "attr")
                                )))
        );
        expectations.put(
                Path.of("038.xml"),
                element -> element.descendantElementOrSelfStream(hasName("bar"))
                        .anyMatch(e -> e.attributes().keySet().equals(
                                Set.of(
                                        new QName("http://example.org/~wilbur", "attr"),
                                        new QName("attr")
                                )))
        );
        expectations.put(
                Path.of("039.xml"),
                element -> element.descendantElementOrSelfStream(hasName("http://example.org/~kipper", "bar"))
                        .anyMatch(e -> e.attributes().keySet().equals(
                                Set.of(
                                        new QName("http://example.org/~wilbur", "attr"),
                                        new QName("attr")
                                )))
        );
        expectations.put(
                Path.of("040.xml"),
                element -> element.descendantElementOrSelfStream(hasName("http://example.org/~wilbur", "bar"))
                        .anyMatch(e -> e.attributes().keySet().equals(
                                Set.of(
                                        new QName("http://example.org/~wilbur", "attr"),
                                        new QName("attr")
                                )))
        );
        expectations.put(
                Path.of("041.xml"),
                element -> element.descendantElementOrSelfStream(hasName("http://example.org/~wilbur", "bar"))
                        .anyMatch(e -> e.attributes().keySet().equals(
                                Set.of(
                                        new QName("http://example.org/~wilbur", "attr"),
                                        new QName("attr")
                                )))
        );
        // 042.xml is not namespace-well-formed and therefore rejected by the parser
        // 043.xml is not namespace-well-formed yet accepted by Saxon (or the underlying XML parser)
        // 044.xml is not namespace-well-formed yet accepted by Saxon (or the underlying XML parser)
        // 045.xml and 046.xml are considered invalid ("simple legal case"), but are at least "well-formed"
        expectations.put(
                Path.of("045.xml"),
                element -> element.attribute(new QName("id")).equals("a:b")
        );
        expectations.put(
                Path.of("046.xml"),
                element -> element.childElementStream()
                        .findFirst()
                        .orElseThrow()
                        .attribute(new QName("id")).equals("a:b")
        );
        expectations.put(
                Path.of("047.xml"),
                element -> element.name().equals(new QName(XMLConstants.XML_NS_URI, "foo"))
        );
        expectations.put(
                Path.of("048.xml"),
                element -> element.attributeOption(new QName(XMLConstants.XML_NS_URI, "foo"))
                        .isPresent()
        );

        return Map.copyOf(expectations);
    }

    private static boolean isExcludedXml(File xmlFile) {
        String fileName = xmlFile.getName();

        return Set.of(
                // Dummy file name
                "notAnExistingFile.xml",
                // Dummy file name
                "neitherAnExistingFile.xml"
        ).contains(fileName);
    }
}
