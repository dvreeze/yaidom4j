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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Immutable DOM query tests against books.xml file.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookQueryTests {

    private static String NS = "http://bookstore";

    private Document doc;

    @BeforeAll
    void parseDocument() throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
        saxParserFactory.setNamespaceAware(true); // Important!
        saxParserFactory.setValidating(false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = saxParserFactory.newSAXParser();
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        parser.parse(new InputSource(inputStream), saxHandler);
        doc = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
    }

    @Test
    void testQueryNamespacesOfAllElements() {
        var namespaces = doc.documentElement().elementStream()
                .map(Element::name)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(Set.of(NS), namespaces);

        var namespaces2 = doc.documentElement().descendantElementOrSelfStream()
                .map(Element::name)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(namespaces, namespaces2);
    }

    @Test
    void testQueryNamesOfAllElements() {
        var names = doc.documentElement().elementStream()
                .map(Element::name)
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

        var names2 = doc.documentElement().descendantElementOrSelfStream()
                .map(Element::name)
                .distinct()
                .toList();

        assertEquals(names, names2);
    }

    @Test
    void testQueryPrefixOfRootElement() {
        assertEquals("books", doc.documentElement().name().getPrefix());
    }

    @Test
    void testQueryPrefixOfDescendantElements() {
        var prefixes = doc.documentElement().descendantElementStream()
                .map(Element::name)
                .map(QName::getPrefix)
                .collect(Collectors.toSet());

        assertEquals(Set.of(""), prefixes);
    }

    @Test
    void testQueryChildElementNames() {
        Map<QName, Long> childElemCounts =
                doc.documentElement().childElementStream()
                        .collect(
                                Collectors.groupingBy(
                                        Element::name,
                                        Collectors.counting()
                                )
                        );

        assertEquals(
                Map.of(new QName(NS, "Book"), 4L, new QName(NS, "Magazine"), 4L),
                childElemCounts
        );
    }

    @Test
    void testQueryMagazineMonths() {
        List<String> magazineMonths = doc.documentElement()
                .childElementStream(hasName(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(
                List.of("January", "February", "February", "March"),
                magazineMonths
        );
    }

    @Test
    void testQueryMagazineTitles() {
        List<String> magazineTitles = doc.documentElement().childElementStream(hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.childElementStream(hasName(NS, "Title")))
                .map(Element::text)
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

        List<String> magazineTitles2 = doc.documentElement().elementStream(hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.elementStream(hasName(NS, "Title")))
                .map(Element::text)
                .distinct()
                .toList();

        assertEquals(magazineTitles, magazineTitles2);
    }

    @Test
    void testQueryBooksCoauthoredByJenniferWidom() {
        Predicate<Element> authorIsJenniferWidom = authorElem ->
                hasName(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(hasName(NS, "First_Name"))
                                .anyMatch(e -> e.text().equals("Jennifer")) &&
                        authorElem.childElementStream(hasName(NS, "Last_Name"))
                                .anyMatch(e -> e.text().equals("Widom"));

        Predicate<Element> bookCowrittenByJenniferWidom = bookElem ->
                hasName(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(hasName(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookTitlesCoauthoredByJenniferWidom =
                doc.documentElement().childElementStream(hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.childElementStream(hasName(NS, "Title")))
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
    }
}
