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

import com.google.common.collect.ImmutableList;
import eu.cdevreeze.yaidom4j.core.ElementNavigationPath;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareNodes;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static final Logger logger = Logger.getGlobal();

    @Test
    void testUpdateRootElement() {
        Element element = createElement("/ones.xml");

        Element updatedElement = element.updateElement(
                Set.of(ElementNavigationPath.empty()),
                (path, elem) -> {
                    if (path.isEmpty()) {
                        return new Element(
                                new QName("onesRoot"),
                                elem.attributes(),
                                elem.namespaceScope(),
                                elem.children()
                        );
                    } else {
                        return elem;
                    }
                }
        );

        assertEquals(new QName("onesRoot"), updatedElement.name());
        assertEquals(
                element.descendantElementStream().map(Element::name).collect(Collectors.toSet()),
                updatedElement.descendantElementStream().map(Element::name).collect(Collectors.toSet())
        );
    }

    @Test
    void testUpdateChildElements() {
        Element element = createElement("/ones.xml");

        Element updatedElement1 = element.transformChildElements(e ->
                new Element(
                        new QName("onesChild"),
                        e.attributes(),
                        e.namespaceScope(),
                        e.children()
                )
        );

        Element updatedElement2 = element.updateElement(
                IntStream.range(0, (int) element.childElementStream().count())
                        .mapToObj(i -> ElementNavigationPath.empty().appendEntry(i))
                        .collect(Collectors.toSet()),
                (path, elem) -> {
                    if (path.entries().size() == 1) {
                        return new Element(
                                new QName("onesChild"),
                                elem.attributes(),
                                elem.namespaceScope(),
                                elem.children()
                        );
                    } else {
                        return elem;
                    }
                }
        );

        assertEquals(
                Set.of(new QName("onesChild")),
                updatedElement1.childElementStream().map(Element::name).collect(Collectors.toSet())
        );
        assertEquals(
                Set.of(new QName("onesChild")),
                updatedElement2.childElementStream().map(Element::name).collect(Collectors.toSet())
        );

        assertEquals(updatedElement1.toClarkNode(), updatedElement2.toClarkNode());

        ClarkNodes.Element updatedClarkElement2 = element.toClarkNode().updateElement(
                IntStream.range(0, (int) element.childElementStream().count())
                        .mapToObj(i -> ElementNavigationPath.empty().appendEntry(i))
                        .collect(Collectors.toSet()),
                (path, elem) -> {
                    if (path.entries().size() == 1) {
                        return new ClarkNodes.Element(
                                new QName("onesChild"),
                                elem.attributes(),
                                elem.children()
                        );
                    } else {
                        return elem;
                    }
                }
        );

        assertEquals(updatedElement2.toClarkNode(), updatedClarkElement2);
    }

    @Test
    void testEquivalentTransformationAndUpdate() {
        Element element = createElement("/ones.xml");

        Element updatedElement1 = element.transformDescendantElementsOrSelf(this::addOneToElement);

        Set<ElementNavigationPath> allElementNavigationPaths =
                AncestryAwareNodes.Element.create(Optional.empty(), element)
                        .descendantElementOrSelfStream()
                        .map(AncestryAwareNodes.Element::elementNavigationPath)
                        .collect(Collectors.toSet());

        Element updatedElement2 =
                element.updateElement(allElementNavigationPaths, (ignoredPath, elem) -> addOneToElement(elem));

        assertTrue(
                updatedElement1.descendantElementOrSelfStream().allMatch(e ->
                        e.text().isBlank() || Integer.parseInt(e.text()) == 2
                )
        );

        assertTrue(
                updatedElement2.descendantElementOrSelfStream().allMatch(e ->
                        e.text().isBlank() || Integer.parseInt(e.text()) == 2
                )
        );

        assertEquals(updatedElement1.toClarkNode(), updatedElement2.toClarkNode());
    }

    private Element addOneToElement(Element elem) {
        if (elem.text().isBlank()) {
            return elem;
        } else {
            return elem.withChildren(
                    ImmutableList.of(
                            new Text(
                                    String.valueOf(Integer.parseInt(elem.text()) + 1),
                                    false
                            )
                    )
            );
        }
    }

    private Element createElement(String classPathResource) {
        InputStream is = UpdateTest.class.getResourceAsStream(classPathResource);
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(new InputSource(is)).documentElement();
    }
}
