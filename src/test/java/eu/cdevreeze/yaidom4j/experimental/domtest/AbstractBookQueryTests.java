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

package eu.cdevreeze.yaidom4j.experimental.domtest;

import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeElementApi;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Immutable DOM query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 * <p>
 * This test depends on the parsing support as well. Hence, the separate package.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractBookQueryTests<E extends GreenTreeElementApi<E>> {

    private static final String NS = "http://bookstore";

    protected abstract E rootElement();

    protected abstract BiFunction<String, String, Predicate<E>> hasName();

    protected abstract Function<String, Predicate<E>> hasOnlyText();

    protected abstract Function<String, Predicate<E>> hasOnlyTextAfterStripping();

    protected abstract BiFunction<String, String, Predicate<E>> hasAttributeValue();

    @Test
    void testQueryNamespacesOfAllElements() {
        var namespaces = rootElement().descendantOrSelfElementStream()
                .map(GreenTreeElementApi::elementName)
                .map(QName::getNamespaceURI)
                .collect(Collectors.toSet());

        assertEquals(Set.of(NS), namespaces);
    }

    @Test
    void testQueryNamesOfAllElements() {
        var names = rootElement().descendantOrSelfElementStream()
                .map(GreenTreeElementApi::elementName)
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

        var names2 = rootElement().descendantOrSelfElementStream()
                .map(GreenTreeElementApi::elementName)
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
                .map(GreenTreeElementApi::elementName)
                .map(QName::getPrefix)
                .collect(Collectors.toSet());

        assertEquals(Set.of(""), prefixes);
    }

    @Test
    void testQueryChildElementNames() {
        Map<QName, Long> childElemCounts =
                rootElement().childElementStream()
                        .collect(Collectors.groupingBy(GreenTreeElementApi::elementName, Collectors.counting()));

        assertEquals(
                Map.of(new QName(NS, "Book"), 4L, new QName(NS, "Magazine"), 4L),
                childElemCounts
        );

        Map<QName, Long> childElemCounts2 =
                rootElement().topmostDescendantOrSelfElementStream(e ->
                                hasName().apply(NS, "Book").test(e) || hasName().apply(NS, "Magazine").test(e)
                        )
                        .collect(Collectors.groupingBy(GreenTreeElementApi::elementName, Collectors.counting()));

        assertEquals(childElemCounts, childElemCounts2);
    }

    @Test
    void testQueryMagazineMonths() {
        List<String> magazineMonths = rootElement()
                .childElementStream(hasName().apply(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(
                List.of("January", "February", "February", "March"),
                magazineMonths
        );

        List<String> magazineMonths2 = rootElement()
                .descendantOrSelfElementStream(hasName().apply(NS, "Magazine"))
                .flatMap(e -> e.attributeOption(new QName("Month")).stream())
                .toList();

        assertEquals(magazineMonths, magazineMonths2);
    }

    @Test
    void testQueryMagazineTitles() {
        List<String> magazineTitles = rootElement().childElementStream(hasName().apply(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.childElementStream(hasName().apply(NS, "Title")))
                .map(GreenTreeElementApi::text)
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

        List<String> magazineTitles2 = rootElement().descendantOrSelfElementStream(hasName().apply(NS, "Magazine"))
                .flatMap(magazineElem -> magazineElem.descendantOrSelfElementStream(hasName().apply(NS, "Title")))
                .map(GreenTreeElementApi::text)
                .distinct()
                .toList();

        assertEquals(magazineTitles, magazineTitles2);
    }

    @Test
    void testQueryBooksCoauthoredByJenniferWidom() {
        Predicate<E> authorIsJenniferWidom = authorElem ->
                hasName().apply(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(hasName().apply(NS, "First_Name"))
                                .anyMatch(hasOnlyText().apply("Jennifer")) &&
                        authorElem.childElementStream(hasName().apply(NS, "Last_Name"))
                                .anyMatch(hasOnlyText().apply("Widom"));

        Predicate<E> bookCowrittenByJenniferWidom = bookElem ->
                hasName().apply(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(hasName().apply(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookTitlesCoauthoredByJenniferWidom =
                rootElement().childElementStream(hasName().apply(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.childElementStream(hasName().apply(NS, "Title")))
                        .map(GreenTreeElementApi::text)
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
                rootElement().descendantOrSelfElementStream(hasName().apply(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .flatMap(e -> e.descendantOrSelfElementStream(hasName().apply(NS, "Title")))
                        .map(GreenTreeElementApi::text)
                        .collect(Collectors.toSet());

        assertEquals(bookTitlesCoauthoredByJenniferWidom, bookTitlesCoauthoredByJenniferWidom2);
    }

    @Test
    void testQueryAuthorNames() {
        Function<E, String> getAuthorName = authorElem -> {
            PreconditionUtil.checkArgument(hasName().apply(NS, "Author").test(authorElem));

            String firstName = authorElem.childElementStream(hasName().apply(NS, "First_Name"))
                    .findFirst()
                    .map(GreenTreeElementApi::text)
                    .orElse("");

            String lastName = authorElem.childElementStream(hasName().apply(NS, "Last_Name"))
                    .findFirst()
                    .map(GreenTreeElementApi::text)
                    .orElse("");

            return String.format("%s %s", firstName, lastName).strip();
        };

        Set<String> authorNames = rootElement()
                .descendantOrSelfElementStream(hasName().apply(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina"),
                authorNames
        );

        Set<String> authorNames2 = rootElement()
                .topmostDescendantOrSelfElementStream(hasName().apply(NS, "Author"))
                .map(getAuthorName)
                .collect(Collectors.toSet());

        assertEquals(authorNames, authorNames2);
    }

    @Test
    void testQueryBookIsbnsCoauthoredByJenniferWidom() {
        Predicate<E> authorIsJenniferWidom = authorElem ->
                hasName().apply(NS, "Author").test(authorElem) &&
                        authorElem.childElementStream(hasName().apply(NS, "First_Name"))
                                .anyMatch(hasOnlyTextAfterStripping().apply("Jennifer")) &&
                        authorElem.childElementStream(hasName().apply(NS, "Last_Name"))
                                .anyMatch(hasOnlyTextAfterStripping().apply("Widom"));

        Predicate<E> bookCowrittenByJenniferWidom = bookElem ->
                hasName().apply(NS, "Book").test(bookElem) &&
                        bookElem.descendantElementStream(hasName().apply(NS, "Author"))
                                .anyMatch(authorIsJenniferWidom);

        Set<String> bookIsbnsCoauthoredByJenniferWidom =
                rootElement().childElementStream(hasName().apply(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(
                Set.of("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-9-88-777777-6"),
                bookIsbnsCoauthoredByJenniferWidom
        );

        Set<String> bookIsbnsCoauthoredByJenniferWidom2 =
                rootElement().topmostDescendantOrSelfElementStream(hasName().apply(NS, "Book"))
                        .filter(bookCowrittenByJenniferWidom)
                        .map(e -> e.attribute(new QName("ISBN")))
                        .collect(Collectors.toSet());

        assertEquals(bookIsbnsCoauthoredByJenniferWidom, bookIsbnsCoauthoredByJenniferWidom2);
    }

    @Test
    void testQueryFebruaryMagazines() {
        List<String> februaryMagazineTitles = rootElement()
                .childElementStream(hasName().apply(NS, "Magazine"))
                .filter(hasAttributeValue().apply("Month", "February"))
                .flatMap(e -> e.childElementStream(hasName().apply(NS, "Title")))
                .map(GreenTreeElementApi::text)
                .toList();

        assertEquals(
                List.of("National Geographic", "Newsweek"),
                februaryMagazineTitles
        );

        List<String> februaryMagazineTitles2 = rootElement()
                .topmostDescendantOrSelfElementStream(hasName().apply(NS, "Magazine"))
                .filter(hasAttributeValue().apply("Month", "February"))
                .flatMap(e -> e.descendantOrSelfElementStream(hasName().apply(NS, "Title")))
                .map(GreenTreeElementApi::text)
                .toList();

        assertEquals(februaryMagazineTitles, februaryMagazineTitles2);
    }
}
