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
import module java.xml;
import eu.cdevreeze.yaidom4j.dom.GreenClarkNodes;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.parse.DocumentProducingSaxHandler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;

/**
 * Immutable Clark element query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class ClarkBookQueryTests extends AbstractBookQueryTests<GreenClarkNodes.Element> {

    private static GreenClarkNodes.@Nullable Element rootElement = null;

    @BeforeAll
    protected static void parseRootElement() {
        DocumentProducingSaxHandler saxHandler = new DocumentProducingSaxHandler();

        InputStream inputStream = ClarkBookQueryTests.class.getResourceAsStream("/books.xml");
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        rootElement = saxHandler.resultingDocument()
                .documentElement()
                .greenTreeNode()
                .removeInterElementWhitespace()
                .toGreenClarkNode();
    }

    @Override
    protected GreenClarkNodes.Element rootElement() {
        return Objects.requireNonNull(rootElement);
    }

    @Override
    protected BiFunction<String, String, Predicate<GreenClarkNodes.Element>> hasName() {
        return GreenClarkNodes.Element::hasName;
    }

    @Override
    protected Function<String, Predicate<GreenClarkNodes.Element>> hasOnlyText() {
        return GreenClarkNodes.Element::hasOnlyText;
    }

    @Override
    protected Function<String, Predicate<GreenClarkNodes.Element>> hasOnlyTextAfterStripping() {
        return GreenClarkNodes.Element::hasOnlyTextAfterStripping;
    }

    @Override
    protected BiFunction<String, String, Predicate<GreenClarkNodes.Element>> hasAttributeValue() {
        return GreenClarkNodes.Element::hasAttributeValue;
    }
}
