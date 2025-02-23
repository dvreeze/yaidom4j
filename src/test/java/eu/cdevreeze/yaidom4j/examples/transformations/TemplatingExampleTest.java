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

package eu.cdevreeze.yaidom4j.examples.transformations;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Nodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinter;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Poor man's templating test. Borrowed from <a href="http://www.w3schools.com/Xml/xsl_templates.asp">templates</a>,
 * but using yaidom4j instead of XSLT.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class TemplatingExampleTest {

    @Test
    void testTemplatingExample() throws URISyntaxException {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser
                .parse(
                        Objects.requireNonNull(TemplatingExampleTest.class
                                .getResource("/transformationexamples/catalog.xml")).toURI()
                );

        Element htmlElement = transform(doc.documentElement());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(htmlElement));
        }

        Element tableExtractedFromCatalog =
                doc.documentElement().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                Nodes.elem("tr")
                                        .plusChildOption(
                                                cdElem.childElementStream(hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")))
                                        )
                                        .plusChildOption(
                                                cdElem.childElementStream(hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")))
                                        )
                        );

        Element tableExtractedFromHtml =
                htmlElement.descendantElementStream(hasName("table"))
                        .findFirst()
                        .orElseThrow()
                        .withAttributes(ImmutableMap.of())
                        .transformChildElementsToNodeLists(che ->
                                che.childElementStream(hasName("th")).findAny().isEmpty() ?
                                        List.of(che) :
                                        List.of()
                        );

        assertEquals(tableExtractedFromCatalog.toClarkNode(), tableExtractedFromHtml.toClarkNode());
    }

    @Test
    void testTemplatingExampleWithSorting() throws URISyntaxException {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser
                .parse(
                        Objects.requireNonNull(TemplatingExampleTest.class
                                .getResource("/transformationexamples/catalog.xml")).toURI()
                );

        Element htmlElement = transformSortingOnArtist(doc.documentElement());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(htmlElement));
        }

        Element tableExtractedFromCatalog =
                doc.documentElement().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                Nodes.elem("tr")
                                        .plusChildOption(
                                                cdElem.childElementStream(hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")))
                                        )
                                        .plusChildOption(
                                                cdElem.childElementStream(hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")))
                                        )
                        );

        Element tableExtractedFromHtml =
                htmlElement.descendantElementStream(hasName("table"))
                        .findFirst()
                        .orElseThrow()
                        .withAttributes(ImmutableMap.of())
                        .transformChildElementsToNodeLists(che ->
                                che.childElementStream(hasName("th")).findAny().isEmpty() ?
                                        List.of(che) :
                                        List.of()
                        );

        assertEquals(
                tableExtractedFromCatalog.toClarkNode().childElementStream().collect(Collectors.toSet()),
                tableExtractedFromHtml.toClarkNode().childElementStream().collect(Collectors.toSet())
        );
    }

    @Test
    void testTemplatingExampleMappingToParagraphs() throws URISyntaxException {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser
                .parse(
                        Objects.requireNonNull(TemplatingExampleTest.class
                                .getResource("/transformationexamples/catalog.xml")).toURI()
                );

        Element htmlElement = transformToParagraphs(doc.documentElement());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(htmlElement));
        }

        Set<String> titlesFromCatalog =
                doc.documentElement().descendantElementStream(hasName("title"))
                        .map(Element::text)
                        .collect(Collectors.toSet());

        Set<String> titlesFromHtml =
                htmlElement.descendantElementStream()
                        .filter(e -> e.text().contains("Title:"))
                        .map(e -> e.childElementStream(hasName("span")).findFirst()
                                .orElseThrow().text())
                        .collect(Collectors.toSet());

        assertEquals(titlesFromCatalog, titlesFromHtml);
    }

    Element transform(Element catalogElement) {
        Preconditions.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return Nodes.elem("html")
                .plusChild(Nodes.elem("body")
                        .plusChild(Nodes.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                Nodes.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                Nodes.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(Nodes.elem("th").withText("Title"))
                                                        .plusChild(Nodes.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(hasName("cd"))
                                                        .map(this::transformCdElement)
                                                        .collect(ImmutableList.toImmutableList())
                                        )
                        )
                );
    }

    Element transformSortingOnArtist(Element catalogElement) {
        Preconditions.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return Nodes.elem("html")
                .plusChild(Nodes.elem("body")
                        .plusChild(Nodes.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                Nodes.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                Nodes.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(Nodes.elem("th").withText("Title"))
                                                        .plusChild(Nodes.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(hasName("cd"))
                                                        .sorted(Comparator.comparing(
                                                                cdElem -> cdElem.childElementStream(hasName("artist"))
                                                                        .findAny().orElseThrow().text()
                                                        ))
                                                        .map(this::transformCdElement)
                                                        .collect(ImmutableList.toImmutableList())
                                        )
                        )
                );
    }

    Element transformToParagraphs(Element catalogElement) {
        Preconditions.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return Nodes.elem("html")
                .plusChild(
                        Nodes.elem("body")
                                .plusChild(Nodes.elem("h2").withText("My CD Collection"))
                                .plusChildren(
                                        catalogElement.childElementStream(hasName("cd"))
                                                .map(this::mapCdElemToParagraph)
                                                .collect(ImmutableList.toImmutableList())
                                )
                );
    }

    private Element transformCdElement(Element cdElement) {
        Preconditions.checkArgument(cdElement.name().equals(new QName("cd")));

        String title = cdElement.childElementStream(hasName("title"))
                .findFirst().orElseThrow().text();
        String artist = cdElement.childElementStream(hasName("artist"))
                .findFirst().orElseThrow().text();
        return Nodes.elem("tr")
                .plusChild(
                        Nodes.elem("td").withText(title)
                )
                .plusChild(
                        Nodes.elem("td").withText(artist)
                );
    }

    private Element mapCdElemToParagraph(Element cdElement) {
        Preconditions.checkArgument(cdElement.name().equals(new QName("cd")));

        return Nodes.elem("p")
                .plusChildren(
                        cdElement.transformChildElementsToNodeLists(e -> {
                            if (hasName("title").test(e)) {
                                return mapTitleElemToNodes(e);
                            } else if (hasName("artist").test(e)) {
                                return mapArtistElemToNodes(e);
                            } else {
                                return List.of();
                            }
                        }).children()
                );
    }

    private List<Node> mapTitleElemToNodes(Element titleElement) {
        Preconditions.checkArgument(titleElement.name().equals(new QName("title")));

        return List.of(
                Nodes.text("Title: "),
                Nodes.elem("span")
                        .withText(titleElement.text())
                        .plusAttribute(new QName("style"), "color:#ff0000"),
                Nodes.elem("br")
        );
    }

    private List<Node> mapArtistElemToNodes(Element artistElement) {
        Preconditions.checkArgument(artistElement.name().equals(new QName("artist")));

        return List.of(
                Nodes.text("Artist: "),
                Nodes.elem("span")
                        .withText(artistElement.text())
                        .plusAttribute(new QName("style"), "color:#00ff00"),
                Nodes.elem("br")
        );
    }
}
