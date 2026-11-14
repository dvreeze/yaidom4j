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
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees;
import eu.cdevreeze.yaidom4j.experimental.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentProducingSaxHandler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;

/**
 * Immutable Clark element query tests against books.xml file.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class ClarkBookQueryTests extends AbstractBookQueryTests<GreenClarkTrees.Element> {

    private static GreenClarkTrees.@Nullable Element rootElement = null;

    @BeforeAll
    protected static void parseRootElement() {
        DocumentProducingSaxHandler saxHandler = new DocumentProducingSaxHandler();

        InputStream inputStream = ClarkBookQueryTests.class.getResourceAsStream("/books.xml");
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        rootElement = saxHandler.resultingDocument()
                .documentElement()
                .greenTreeNode()
                .removeInterElementWhitespace()
                .toClarkNode();
    }

    @Override
    protected GreenClarkTrees.Element rootElement() {
        return Objects.requireNonNull(rootElement);
    }

    @Override
    protected BiFunction<String, String, Predicate<GreenClarkTrees.Element>> hasName() {
        return GreenClarkTrees.Element::hasName;
    }

    @Override
    protected Function<String, Predicate<GreenClarkTrees.Element>> hasOnlyText() {
        return GreenClarkTrees.Element::hasOnlyText;
    }

    @Override
    protected Function<String, Predicate<GreenClarkTrees.Element>> hasOnlyTextAfterStripping() {
        return GreenClarkTrees.Element::hasOnlyTextAfterStripping;
    }

    @Override
    protected BiFunction<String, String, Predicate<GreenClarkTrees.Element>> hasAttributeValue() {
        return GreenClarkTrees.Element::hasAttributeValue;
    }
}
