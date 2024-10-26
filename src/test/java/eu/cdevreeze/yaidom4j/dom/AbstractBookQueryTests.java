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

package eu.cdevreeze.yaidom4j.dom;

import com.google.common.base.Preconditions;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;
import eu.cdevreeze.yaidom4j.queryapi.ElementPredicateFactoryApi;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Immutable DOM query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
public abstract class AbstractBookQueryTests<E extends ElementApi<E>> {

    private static final String NS = "http://bookstore";

    protected abstract E rootElement();

    protected abstract ElementPredicateFactoryApi<E> epf();

    @Test
    void testQueryNamespacesOfAllElements() {
        var namespaces = rootElement().elementStream()
                .map(ElementApi::elementName)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(Set.of(NS), namespaces);

        var namespaces2 = rootElement().descendantElementOrSelfStream()
                .map(ElementApi::elementName)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(namespaces, namespaces2);
    }

    @Test
    void testQueryNamesOfAllElements() {
        var names = rootElement().elementStream()
                .map(ElementApi::elementName)
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

        var names2 = rootElement().descendantElementOrSelfStream()
                .map(ElementApi::elementName)
                .distinct()
                .toList();

        assertEquals(names, names2);
    }

    @Test
    void testQueryPrefixOfRootElement() {
        assertEquals("books", rootElement().elementName().getPrefix());
    }

    @Test
    void testQueryPrefixOfDescendantElements() {
        var prefixes = rootElement().descendantElementStream()
                .map(ElementApi::elementName)
                .map(QName::getPrefix)
                .collect(Collectors.toSet());

        assertEquals(Set.of(""), prefixes);
    }

    @Test
    void testQueryChildElementNames() {
        Map<QName, Long> childElemCounts =
                rootElement().childElementStream()
                        .collect(Collectors.groupingBy(ElementApi::elementName, Collectors.counting()));

        assertEquals(
                Map.of(new QName(NS, "Book"), 4L, new QName(NS, "Magazine"), 4L),
                childElemCounts
        );

        Map<QName, Long> childElemCounts2 =
                rootElement().topmostDescendantElementOrSelfStream(e ->
                                epf().hasName(NS, "Book").test(e) || epf().hasName(NS, "Magazine").test(e)
                        )
                        .collect(Collectors.groupingBy(ElementApi::elementName, Collectors.counting()));

        assertEquals(childElemCounts, childElemCounts2);
    }

    @Test
    void testQueryMagazineMonths() {
        List<String> magazineMonths = rootElement()
                .childElementStream(epf().hasName(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(
                List.of("January", "February", "February", "March"),
                magazineMonths
        );

        List<String> magazineMonths2 = rootElement()
                .descendantElementOrSelfStream(epf().hasName(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(magazineMonths, magazineMonths2);
    }

    @Test
    void testQueryMagazineTitles() {
        List<String> magazineTitles = rootElement().childElementStream(epf().hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.childElementStream(epf().hasName(NS, "Title")))
                .map(ElementApi::text)
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

        List<String> magazineTitles2 = rootElement().elementStream(epf().hasName(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.elementStream(epf().hasName(NS, "Title")))
                .map(ElementApi::text)
                .distinct()
                .toList();

        assertEquals(magazineTitles, magazineTitles2);
    }

    @Test
    void testQueryBooksCoauthoredByJenniferWidom() {
        Predicate<E> authorIsJenniferWidom = authorElem ->
                epf().hasName(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(epf().hasName(NS, "First_Name"))
                                .anyMatch(epf().hasOnlyText("Jennifer")) &&
                        authorElem.childElementStream(epf().hasName(NS, "Last_Name"))
                                .anyMatch(epf().hasOnlyText("Widom"));

        Predicate<E> bookCowrittenByJenniferWidom = bookElem ->
                epf().hasName(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(epf().hasName(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookTitlesCoauthoredByJenniferWidom =
                rootElement().childElementStream(epf().hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.childElementStream(epf().hasName(NS, "Title")))
                        .map(ElementApi::text)
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
                rootElement().descendantElementOrSelfStream(epf().hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.descendantElementOrSelfStream(epf().hasName(NS, "Title")))
                        .map(ElementApi::text)
                        .collect(Collectors.toSet());

        assertEquals(bookTitlesCoauthoredByJenniferWidom, bookTitlesCoauthoredByJenniferWidom2);
    }

    @Test
    void testQueryAuthorNames() {
        Function<E, String> getAuthorName = authorElem -> {
            Preconditions.checkArgument(epf().hasName(NS, "Author").test(authorElem));

            String firstName = authorElem.childElementStream(epf().hasName(NS, "First_Name"))
                    .findFirst()
                    .map(ElementApi::text)
                    .orElse("");

            String lastName = authorElem.childElementStream(epf().hasName(NS, "Last_Name"))
                    .findFirst()
                    .map(ElementApi::text)
                    .orElse("");

            return String.format("%s %s", firstName, lastName).strip();
        };

        Set<String> authorNames = rootElement()
                .descendantElementStream(epf().hasName(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina"),
                authorNames
        );

        Set<String> authorNames2 = rootElement()
                .topmostElementStream(epf().hasName(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(authorNames, authorNames2);
    }

    @Test
    void testQueryBookIsbnsCoauthoredByJenniferWidom() {
        Predicate<E> authorIsJenniferWidom = authorElem ->
                epf().hasName(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(epf().hasName(NS, "First_Name"))
                                .anyMatch(epf().hasOnlyStrippedText("Jennifer")) &&
                        authorElem.childElementStream(epf().hasName(NS, "Last_Name"))
                                .anyMatch(epf().hasOnlyStrippedText("Widom"));

        Predicate<E> bookCowrittenByJenniferWidom = bookElem ->
                epf().hasName(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(epf().hasName(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookIsbnsCoauthoredByJenniferWidom =
                rootElement().childElementStream(epf().hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(
                Set.of("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-9-88-777777-6"),
                bookIsbnsCoauthoredByJenniferWidom
        );

        Set<String> bookIsbnsCoauthoredByJenniferWidom2 =
                rootElement().topmostElementStream(epf().hasName(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(bookIsbnsCoauthoredByJenniferWidom, bookIsbnsCoauthoredByJenniferWidom2);
    }

    @Test
    void testQueryFebruaryMagazines() {
        List<String> februaryMagazineTitles = rootElement()
                .childElementStream(epf().hasName(NS, "Magazine"))
                .filter(epf().hasAttributeValue("Month", "February"))
                .flatMap(e -> e.childElementStream(epf().hasName(NS, "Title")))
                .map(ElementApi::text)
                .toList();

        assertEquals(
                List.of("National Geographic", "Newsweek"),
                februaryMagazineTitles
        );

        List<String> februaryMagazineTitles2 = rootElement()
                .topmostDescendantElementOrSelfStream(epf().hasName(NS, "Magazine"))
                .filter(epf().hasAttributeValue("Month", "February"))
                .flatMap(e -> e.elementStream(epf().hasName(NS, "Title")))
                .map(ElementApi::text)
                .toList();

        assertEquals(februaryMagazineTitles, februaryMagazineTitles2);
    }
}
