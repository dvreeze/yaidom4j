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

package eu.cdevreeze.yaidom4j.dom.ancestryaware;

import eu.cdevreeze.yaidom4j.dom.AbstractBookQueryTests;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementTree.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Documents;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import eu.cdevreeze.yaidom4j.internal.SaxParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Immutable DOM query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class BookQueryTests extends AbstractBookQueryTests<Element> {

    private static final String NS = "http://bookstore";

    private static Element rootElement;

    @BeforeAll
    protected static void parseRootElement() {
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        var underlyingDoc = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
        var doc = Document.from(underlyingDoc);
        rootElement = doc.documentElement();
    }

    @Override
    protected Element rootElement() {
        return rootElement;
    }

    @Override
    protected Predicate<Element> hasName(String namespace, String localName) {
        return ElementPredicates.hasName(namespace, localName);
    }

    @Override
    protected Predicate<Element> hasAttribute(String noNamespaceAttrName, String attrValue) {
        return ElementPredicates.hasAttribute(noNamespaceAttrName, attrValue);
    }

    @Override
    protected Predicate<Element> hasOnlyText(String text) {
        return ElementPredicates.hasOnlyText(text);
    }

    @Override
    protected Predicate<Element> hasOnlyStrippedText(String text) {
        return ElementPredicates.hasOnlyStrippedText(text);
    }

    @Test
    void testParentOfChildElements() {
        var allElements = rootElement().descendantElementOrSelfStream().toList();

        assertTrue(allElements.size() > 10);

        assertTrue(
                allElements.stream().allMatch(
                        e -> e.childElementStream().allMatch(che -> che.parentElementOption().orElseThrow().equals(e))
                )
        );
    }
}
