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

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.dom.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.ElementPredicates;
import eu.cdevreeze.yaidom4j.experimental.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentProducingSaxHandler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Immutable green tree DOM query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class BookQueryTests extends AbstractBookQueryTests<Element> {

    private static @Nullable Element rootElement = null;

    @BeforeAll
    protected static void parseRootElement() {
        DocumentProducingSaxHandler saxHandler = new DocumentProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        rootElement = saxHandler.resultingDocument()
                .removeInterElementWhitespace()
                .documentElement();
    }

    @Override
    protected Element rootElement() {
        return Objects.requireNonNull(rootElement);
    }

    @Override
    protected BiFunction<String, String, Predicate<Element>> hasName() {
        return ElementPredicates::hasName;
    }

    @Override
    protected Function<String, Predicate<Element>> hasOnlyText() {
        return ElementPredicates::hasOnlyText;
    }

    @Override
    protected Function<String, Predicate<Element>> hasOnlyTextAfterStripping() {
        return ElementPredicates::hasOnlyTextAfterStripping;
    }

    @Override
    protected BiFunction<String, String, Predicate<Element>> hasAttributeValue() {
        return ElementPredicates::hasAttributeValue;
    }

    @Test
    void testParentOfChildElements() {
        var allElements = rootElement().descendantOrSelfElementStream().toList();

        assertTrue(allElements.size() > 10);

        assertTrue(
                allElements.stream().allMatch(
                        e -> e.childElementStream().allMatch(che -> che.parentElementOption().orElseThrow().equals(e))
                )
        );
    }
}
