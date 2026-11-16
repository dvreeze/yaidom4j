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

package eu.cdevreeze.yaidom4j.misc;

import eu.cdevreeze.yaidom4j.dom.Document;
import eu.cdevreeze.yaidom4j.dom.Element;
import eu.cdevreeze.yaidom4j.dom.GreenNodes;
import eu.cdevreeze.yaidom4j.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.parse.DocumentParsers;
import eu.cdevreeze.yaidom4j.print.DocumentPrinter;
import eu.cdevreeze.yaidom4j.print.DocumentPrinters;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cdevreeze.yaidom4j.dom.Element.hasName;
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

    // TODO Make this code far more ergonomic. This implies tweaking the yaidom4j public API itself!

    @Test
    void testTemplatingExample() throws URISyntaxException {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser
                .parse(
                        Objects.requireNonNull(TemplatingExampleTest.class
                                .getResource("/transformationexamples/catalog.xml")).toURI()
                );

        GreenNodes.Element htmlElement = transform(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(Element.of(htmlElement)));
        }

        GreenNodes.Element tableExtractedFromCatalog =
                doc.documentElement().greenTreeNode().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                GreenNodes.elem("tr")
                                        .plusChildren(
                                                cdElem.childElementStream(GreenNodes.Element.hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                                        .plusChildren(
                                                cdElem.childElementStream(GreenNodes.Element.hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                        );

        GreenNodes.Element tableExtractedFromHtml =
                transformChildElementsToNodeLists(
                        htmlElement.descendantElementStream(GreenNodes.Element.hasName("table"))
                                .findFirst()
                                .orElseThrow()
                                .withAttributes(Map.of()),
                        che ->
                                che.childElementStream(GreenNodes.Element.hasName("th")).findAny().isEmpty() ?
                                        List.of(che) :
                                        List.of()
                );

        assertEquals(tableExtractedFromCatalog.toGreenClarkNode(), tableExtractedFromHtml.toGreenClarkNode());
    }

    @Test
    void testTemplatingExampleWithSorting() throws URISyntaxException {
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document doc = docParser
                .parse(
                        Objects.requireNonNull(TemplatingExampleTest.class
                                .getResource("/transformationexamples/catalog.xml")).toURI()
                );

        GreenNodes.Element htmlElement = transformSortingOnArtist(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(Element.of(htmlElement)));
        }

        GreenNodes.Element tableExtractedFromCatalog =
                doc.documentElement().greenTreeNode().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                GreenNodes.elem("tr")
                                        .plusChildren(
                                                cdElem.childElementStream(GreenNodes.Element.hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                                        .plusChildren(
                                                cdElem.childElementStream(GreenNodes.Element.hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                        );

        GreenNodes.Element tableExtractedFromHtml =
                transformChildElementsToNodeLists(
                        htmlElement.descendantElementStream(GreenNodes.Element.hasName("table"))
                                .findFirst()
                                .orElseThrow()
                                .withAttributes(Map.of()),
                        che ->
                                che.childElementStream(GreenNodes.Element.hasName("th")).findAny().isEmpty() ?
                                        List.of(che) :
                                        List.of()
                );

        assertEquals(
                tableExtractedFromCatalog.toGreenClarkNode().childElementStream().collect(Collectors.toSet()),
                tableExtractedFromHtml.toGreenClarkNode().childElementStream().collect(Collectors.toSet())
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

        GreenNodes.Element htmlElement = transformToParagraphs(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(Element.of(htmlElement)));
        }

        Set<String> titlesFromCatalog =
                doc.documentElement().descendantElementStream(hasName("title"))
                        .map(Element::text)
                        .collect(Collectors.toSet());

        Set<String> titlesFromHtml =
                htmlElement.descendantElementStream()
                        .filter(e -> e.text().contains("Title:"))
                        .map(e -> e.childElementStream(GreenNodes.Element.hasName("span")).findFirst()
                                .orElseThrow().text())
                        .collect(Collectors.toSet());

        assertEquals(titlesFromCatalog, titlesFromHtml);
    }

    GreenNodes.Element transform(GreenNodes.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenNodes.elem("html")
                .plusChild(GreenNodes.elem("body")
                        .plusChild(GreenNodes.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                GreenNodes.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                GreenNodes.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(GreenNodes.elem("th").withText("Title"))
                                                        .plusChild(GreenNodes.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(GreenNodes.Element.hasName("cd"))
                                                        .map(this::transformCdElement)
                                                        .collect(Collectors.toUnmodifiableList())
                                        )
                        )
                );
    }

    GreenNodes.Element transformSortingOnArtist(GreenNodes.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenNodes.elem("html")
                .plusChild(GreenNodes.elem("body")
                        .plusChild(GreenNodes.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                GreenNodes.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                GreenNodes.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(GreenNodes.elem("th").withText("Title"))
                                                        .plusChild(GreenNodes.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(GreenNodes.Element.hasName("cd"))
                                                        .sorted(Comparator.comparing(
                                                                cdElem -> cdElem.childElementStream(GreenNodes.Element.hasName("artist"))
                                                                        .findAny().orElseThrow().text()
                                                        ))
                                                        .map(this::transformCdElement)
                                                        .collect(Collectors.toUnmodifiableList())
                                        )
                        )
                );
    }

    GreenNodes.Element transformToParagraphs(GreenNodes.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenNodes.elem("html")
                .plusChild(
                        GreenNodes.elem("body")
                                .plusChild(GreenNodes.elem("h2").withText("My CD Collection"))
                                .plusChildren(
                                        catalogElement.childElementStream(GreenNodes.Element.hasName("cd"))
                                                .map(this::mapCdElemToParagraph)
                                                .collect(Collectors.toUnmodifiableList())
                                )
                );
    }

    private GreenNodes.Element transformCdElement(GreenNodes.Element cdElement) {
        PreconditionUtil.checkArgument(cdElement.name().equals(new QName("cd")));

        String title = cdElement.childElementStream(GreenNodes.Element.hasName("title"))
                .findFirst().orElseThrow().text();
        String artist = cdElement.childElementStream(GreenNodes.Element.hasName("artist"))
                .findFirst().orElseThrow().text();
        return GreenNodes.elem("tr")
                .plusChild(
                        GreenNodes.elem("td").withText(title)
                )
                .plusChild(
                        GreenNodes.elem("td").withText(artist)
                );
    }

    private GreenNodes.Element mapCdElemToParagraph(GreenNodes.Element cdElement) {
        PreconditionUtil.checkArgument(cdElement.name().equals(new QName("cd")));

        return GreenNodes.elem("p")
                .plusChildren(
                        transformChildElementsToNodeLists(cdElement, e -> {
                            if (GreenNodes.Element.hasName("title").test(e)) {
                                return mapTitleElemToNodes(e);
                            } else if (GreenNodes.Element.hasName("artist").test(e)) {
                                return mapArtistElemToNodes(e);
                            } else {
                                return List.of();
                            }
                        }).children()
                );
    }

    private List<GreenNodes.Node> mapTitleElemToNodes(GreenNodes.Element titleElement) {
        PreconditionUtil.checkArgument(titleElement.name().equals(new QName("title")));

        return List.of(
                GreenNodes.text("Title: "),
                GreenNodes.elem("span")
                        .withText(titleElement.text())
                        .plusAttribute(new QName("style"), "color:#ff0000"),
                GreenNodes.elem("br")
        );
    }

    private List<GreenNodes.Node> mapArtistElemToNodes(GreenNodes.Element artistElement) {
        PreconditionUtil.checkArgument(artistElement.name().equals(new QName("artist")));

        return List.of(
                GreenNodes.text("Artist: "),
                GreenNodes.elem("span")
                        .withText(artistElement.text())
                        .plusAttribute(new QName("style"), "color:#00ff00"),
                GreenNodes.elem("br")
        );
    }

    private GreenNodes.Element transformChildElementsToNodeLists(
            GreenNodes.Element element,
            Function<GreenNodes.Element, List<GreenNodes.Node>> f
    ) {
        return element.withChildren(
                element.children()
                        .stream()
                        .flatMap(ch -> switch (ch) {
                            case GreenNodes.Element che ->
                                    f.apply(che).stream().map(GreenNodes.Node::asCanBeChild);
                            default -> Stream.of(ch);
                        })
                        .toList()
        );
    }
}
