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
import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareNodes.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.InputStream;

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

    private static final AncestryAwareElementPredicates.Factory epf = new AncestryAwareElementPredicates.Factory();

    @BeforeAll
    protected static void parseRootElement() {
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        var underlyingDoc = saxHandler.resultingDocument().removeInterElementWhitespace();
        var doc = AncestryAwareDocument.from(underlyingDoc);
        rootElement = doc.documentElement();
    }

    @Override
    protected Element rootElement() {
        return rootElement;
    }

    @Override
    protected AncestryAwareElementPredicates.Factory epf() {
        return epf;
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
