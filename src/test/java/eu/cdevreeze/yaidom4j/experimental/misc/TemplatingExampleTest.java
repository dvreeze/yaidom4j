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

package eu.cdevreeze.yaidom4j.experimental.misc;

import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinter;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinters;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cdevreeze.yaidom4j.experimental.dom.ElementPredicates.hasName;
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

        GreenXmlTrees.Element htmlElement = transform(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(new Element(0, null, htmlElement)));
        }

        GreenXmlTrees.Element tableExtractedFromCatalog =
                doc.documentElement().greenTreeNode().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                GreenXmlTrees.elem("tr")
                                        .plusChildren(
                                                cdElem.childElementStream(GreenXmlTrees.ElementPredicates.hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                                        .plusChildren(
                                                cdElem.childElementStream(GreenXmlTrees.ElementPredicates.hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                        );

        GreenXmlTrees.Element tableExtractedFromHtml =
                transformChildElementsToNodeLists(
                        htmlElement.descendantElementStream(GreenXmlTrees.ElementPredicates.hasName("table"))
                                .findFirst()
                                .orElseThrow()
                                .withAttributes(Map.of()),
                        che ->
                                che.childElementStream(GreenXmlTrees.ElementPredicates.hasName("th")).findAny().isEmpty() ?
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

        GreenXmlTrees.Element htmlElement = transformSortingOnArtist(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(new Element(0, null, htmlElement)));
        }

        GreenXmlTrees.Element tableExtractedFromCatalog =
                doc.documentElement().greenTreeNode().withName(new QName("table"))
                        .transformChildElements(cdElem ->
                                GreenXmlTrees.elem("tr")
                                        .plusChildren(
                                                cdElem.childElementStream(GreenXmlTrees.ElementPredicates.hasName("title"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                                        .plusChildren(
                                                cdElem.childElementStream(GreenXmlTrees.ElementPredicates.hasName("artist"))
                                                        .findFirst().map(e -> e.withName(new QName("td")).asCanBeChild()).stream().toList()
                                        )
                        );

        GreenXmlTrees.Element tableExtractedFromHtml =
                transformChildElementsToNodeLists(
                        htmlElement.descendantElementStream(GreenXmlTrees.ElementPredicates.hasName("table"))
                                .findFirst()
                                .orElseThrow()
                                .withAttributes(Map.of()),
                        che ->
                                che.childElementStream(GreenXmlTrees.ElementPredicates.hasName("th")).findAny().isEmpty() ?
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

        GreenXmlTrees.Element htmlElement = transformToParagraphs(doc.documentElement().greenTreeNode());

        if (Boolean.parseBoolean(System.getProperty("debugTemplatingExampleTest"))) {
            DocumentPrinter docPrinter = DocumentPrinters.instance();
            System.out.println();
            System.out.println(docPrinter.print(new Element(0, null, htmlElement)));
        }

        Set<String> titlesFromCatalog =
                doc.documentElement().descendantElementStream(hasName("title"))
                        .map(Element::text)
                        .collect(Collectors.toSet());

        Set<String> titlesFromHtml =
                htmlElement.descendantElementStream()
                        .filter(e -> e.text().contains("Title:"))
                        .map(e -> e.childElementStream(GreenXmlTrees.ElementPredicates.hasName("span")).findFirst()
                                .orElseThrow().text())
                        .collect(Collectors.toSet());

        assertEquals(titlesFromCatalog, titlesFromHtml);
    }

    GreenXmlTrees.Element transform(GreenXmlTrees.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenXmlTrees.elem("html")
                .plusChild(GreenXmlTrees.elem("body")
                        .plusChild(GreenXmlTrees.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                GreenXmlTrees.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                GreenXmlTrees.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(GreenXmlTrees.elem("th").withText("Title"))
                                                        .plusChild(GreenXmlTrees.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(GreenXmlTrees.ElementPredicates.hasName("cd"))
                                                        .map(this::transformCdElement)
                                                        .collect(Collectors.toUnmodifiableList())
                                        )
                        )
                );
    }

    GreenXmlTrees.Element transformSortingOnArtist(GreenXmlTrees.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenXmlTrees.elem("html")
                .plusChild(GreenXmlTrees.elem("body")
                        .plusChild(GreenXmlTrees.elem("h2").withText("My CD Collection"))
                        .plusChild(
                                GreenXmlTrees.elem("table")
                                        .plusAttribute(new QName("border"), "1")
                                        .plusChild(
                                                GreenXmlTrees.elem("tr")
                                                        .plusAttribute(new QName("bgcolor"), "#9acd32")
                                                        .plusChild(GreenXmlTrees.elem("th").withText("Title"))
                                                        .plusChild(GreenXmlTrees.elem("th").withText("Artist"))
                                        )
                                        .plusChildren(
                                                catalogElement.childElementStream(GreenXmlTrees.ElementPredicates.hasName("cd"))
                                                        .sorted(Comparator.comparing(
                                                                cdElem -> cdElem.childElementStream(GreenXmlTrees.ElementPredicates.hasName("artist"))
                                                                        .findAny().orElseThrow().text()
                                                        ))
                                                        .map(this::transformCdElement)
                                                        .collect(Collectors.toUnmodifiableList())
                                        )
                        )
                );
    }

    GreenXmlTrees.Element transformToParagraphs(GreenXmlTrees.Element catalogElement) {
        PreconditionUtil.checkArgument(catalogElement.name().equals(new QName("catalog")));

        return GreenXmlTrees.elem("html")
                .plusChild(
                        GreenXmlTrees.elem("body")
                                .plusChild(GreenXmlTrees.elem("h2").withText("My CD Collection"))
                                .plusChildren(
                                        catalogElement.childElementStream(GreenXmlTrees.ElementPredicates.hasName("cd"))
                                                .map(this::mapCdElemToParagraph)
                                                .collect(Collectors.toUnmodifiableList())
                                )
                );
    }

    private GreenXmlTrees.Element transformCdElement(GreenXmlTrees.Element cdElement) {
        PreconditionUtil.checkArgument(cdElement.name().equals(new QName("cd")));

        String title = cdElement.childElementStream(GreenXmlTrees.ElementPredicates.hasName("title"))
                .findFirst().orElseThrow().text();
        String artist = cdElement.childElementStream(GreenXmlTrees.ElementPredicates.hasName("artist"))
                .findFirst().orElseThrow().text();
        return GreenXmlTrees.elem("tr")
                .plusChild(
                        GreenXmlTrees.elem("td").withText(title)
                )
                .plusChild(
                        GreenXmlTrees.elem("td").withText(artist)
                );
    }

    private GreenXmlTrees.Element mapCdElemToParagraph(GreenXmlTrees.Element cdElement) {
        PreconditionUtil.checkArgument(cdElement.name().equals(new QName("cd")));

        return GreenXmlTrees.elem("p")
                .plusChildren(
                        transformChildElementsToNodeLists(cdElement, e -> {
                            if (GreenXmlTrees.ElementPredicates.hasName("title").test(e)) {
                                return mapTitleElemToNodes(e);
                            } else if (GreenXmlTrees.ElementPredicates.hasName("artist").test(e)) {
                                return mapArtistElemToNodes(e);
                            } else {
                                return List.of();
                            }
                        }).children()
                );
    }

    private List<GreenXmlTrees.Node> mapTitleElemToNodes(GreenXmlTrees.Element titleElement) {
        PreconditionUtil.checkArgument(titleElement.name().equals(new QName("title")));

        return List.of(
                GreenXmlTrees.text("Title: "),
                GreenXmlTrees.elem("span")
                        .withText(titleElement.text())
                        .plusAttribute(new QName("style"), "color:#ff0000"),
                GreenXmlTrees.elem("br")
        );
    }

    private List<GreenXmlTrees.Node> mapArtistElemToNodes(GreenXmlTrees.Element artistElement) {
        PreconditionUtil.checkArgument(artistElement.name().equals(new QName("artist")));

        return List.of(
                GreenXmlTrees.text("Artist: "),
                GreenXmlTrees.elem("span")
                        .withText(artistElement.text())
                        .plusAttribute(new QName("style"), "color:#00ff00"),
                GreenXmlTrees.elem("br")
        );
    }

    private GreenXmlTrees.Element transformChildElementsToNodeLists(
            GreenXmlTrees.Element element,
            Function<GreenXmlTrees.Element, List<GreenXmlTrees.Node>> f
    ) {
        return element.withChildren(
                element.children()
                        .stream()
                        .flatMap(ch -> switch (ch) {
                            case GreenXmlTrees.Element che ->
                                    f.apply(che).stream().map(GreenXmlTrees.Node::asCanBeChild);
                            default -> Stream.of(ch);
                        })
                        .toList()
        );
    }
}
