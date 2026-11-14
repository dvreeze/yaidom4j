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

import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional update test.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class UpdateTest {

    @Test
    void testUpdateChildElements() {
        GreenXmlTrees.Element element = createElement("/ones.xml");

        GreenXmlTrees.Element updatedElement = element.transformChildElements(e ->
                new GreenXmlTrees.Element(
                        new QName("onesChild"),
                        e.attributes(),
                        e.namespaceScope(),
                        e.children()
                )
        );

        assertEquals(
                Set.of(new QName("onesChild")),
                updatedElement.childElementStream().map(GreenXmlTrees.Element::name).collect(Collectors.toSet())
        );
        assertEquals(
                Set.of(new QName("grandchild"), new QName("number")),
                updatedElement.childElementStream()
                        .flatMap(GreenXmlTrees.Element::descendantElementStream)
                        .map(GreenXmlTrees.Element::name)
                        .collect(Collectors.toSet())
        );
    }

    @Test
    void testEquivalentTransformationAndUpdate() {
        GreenXmlTrees.Element element = createElement("/ones.xml");

        GreenXmlTrees.Element updatedElement = element.transformDescendantOrSelfElements(this::addOneToElement);

        assertTrue(
                updatedElement.descendantOrSelfElementStream().allMatch(e ->
                        e.text().isBlank() || Integer.parseInt(e.text()) == 2
                )
        );
    }

    private GreenXmlTrees.Element addOneToElement(GreenXmlTrees.Element elem) {
        if (elem.text().isBlank()) {
            return elem;
        } else {
            return elem.withChildren(
                    List.of(
                            new GreenXmlTrees.Text(
                                    String.valueOf(Integer.parseInt(elem.text()) + 1)
                            )
                    )
            );
        }
    }

    private GreenXmlTrees.Element createElement(String classPathResource) {
        InputStream is = UpdateTest.class.getResourceAsStream(classPathResource);
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(new InputSource(is)).documentElement().greenTreeNode();
    }
}
