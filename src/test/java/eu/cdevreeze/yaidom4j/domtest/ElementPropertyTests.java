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

package eu.cdevreeze.yaidom4j.domtest;

import module java.base;
import eu.cdevreeze.yaidom4j.dom.Document;
import eu.cdevreeze.yaidom4j.dom.Element;
import eu.cdevreeze.yaidom4j.dom.GreenClarkNodes;
import eu.cdevreeze.yaidom4j.dom.GreenNodes;
import eu.cdevreeze.yaidom4j.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.parse.DocumentParsers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Element} properties.
 * <p>
 * This is a parameterized test. See
 * <a href="https://www.baeldung.com/parameterized-tests-junit-5">parameterized test</a>
 * for more on how to create parameterized tests.
 *
 * @author Chris de Vreeze
 */
class ElementPropertyTests {

    private static Document parseDocument(URI url) {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(url);
    }

    @ParameterizedTest
    @MethodSource("provideValidXmlDocUris")
    void testNavigationFromRootNodeReturningSameElement(URI xmlUri) {
        Document doc = parseDocument(xmlUri);
        Element docElem = doc.documentElement();

        List<Element> allElems1 = docElem.descendantOrSelfElementStream().toList();

        List<Element> allElems2 = allElems1.stream()
                .map(e -> {
                    List<Integer> nodeNavigationPath = e.nodeNavigationPath();
                    Document rootNode = (Document) e.ancestorOrSelfStream().toList().getLast();
                    return rootNode.navigate(nodeNavigationPath).orElseThrow().asElement();
                })
                .toList();

        assertEquals(
                allElems1.stream().map(Element::greenTreeNode).toList(),
                allElems2.stream().map(Element::greenTreeNode).toList()
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidXmlDocUris")
    void testSameGreenNodesFoundFromDocAndGreenTreeDoc(URI xmlUri) {
        Document doc = parseDocument(xmlUri);
        GreenNodes.Document greenTreeDoc = doc.greenTreeNode();

        List<GreenNodes.Element> allElems1 = doc.documentElement()
                .descendantOrSelfElementStream()
                .map(Element::greenTreeNode)
                .toList();
        List<GreenNodes.Element> allElems2 = greenTreeDoc.documentElement()
                .descendantOrSelfElementStream()
                .toList();

        assertEquals(allElems1, allElems2);
    }

    @ParameterizedTest
    @MethodSource("provideValidXmlDocUris")
    void testSameClarkNodesFoundFromGreenTreeDocAndClarkTreeDoc(URI xmlUri) {
        GreenNodes.Document greenTreeDoc = parseDocument(xmlUri).greenTreeNode();
        GreenClarkNodes.Document clarkDoc = greenTreeDoc.toGreenClarkNode();

        List<GreenClarkNodes.Element> allElems1 = greenTreeDoc.documentElement()
                .descendantOrSelfElementStream()
                .map(GreenNodes.Element::toGreenClarkNode)
                .toList();
        List<GreenClarkNodes.Element> allElems2 = clarkDoc.documentElement()
                .descendantOrSelfElementStream()
                .toList();

        assertEquals(allElems1, allElems2);
    }

    private static Stream<Arguments> provideValidXmlDocUris() throws URISyntaxException {
        URI xmlDirUri =
                Objects.requireNonNull(ElementPropertyTests.class.getResource("/xmlconformancesuite/xmlconf/eduni/namespaces/1.0"))
                        .toURI();
        File dir = new File(xmlDirUri);
        List<File> xmlFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".xml"))
                .filter(ElementPropertyTests::isIncludedXml)
                .sorted()
                .toList();
        return xmlFiles.stream().map(f -> Arguments.of(f.toURI()));
    }

    private static boolean isIncludedXml(File xmlFile) {
        String fileName = xmlFile.getName();

        return Set.of(
                "001.xml",
                "002.xml",
                "003.xml",
                "004.xml",
                "005.xml",
                "007.xml",
                "008.xml",
                "017.xml",
                "018.xml",
                "019.xml",
                "020.xml",
                "021.xml",
                "022.xml",
                "024.xml",
                "027.xml",
                "028.xml",
                "034.xml",
                "037.xml",
                "038.xml",
                "039.xml",
                "040.xml",
                "041.xml",
                "045.xml",
                "046.xml",
                "047.xml",
                "048.xml"
        ).contains(fileName);
    }
}
