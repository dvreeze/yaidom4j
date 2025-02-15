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

package eu.cdevreeze.yaidom4j.misc;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Text;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinter;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Thread safety test. In several threads a yaidom4j XML tree is updated simultaneously, without any data
 * corruption, illustrating the thread-safety of yaidom4j immutable DOM trees.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class ThreadSafetyTest {

    private static final Logger logger = Logger.getGlobal();

    private record IndexedElement(Element element, int index) {

        IndexedElement withElement(Element newElement) {
            return new IndexedElement(newElement, index());
        }
    }

    @Test
    void testThreadSafety() {
        Element startElement = createElement();
        Element updatedElement = updateElement(startElement);

        DocumentPrinter docPrinter = DocumentPrinters.instance();

        // Thread-safe, without any memory visibility problems; all numbers are now 2 after the update
        assertEquals(
                Set.of(2),
                updatedElement.descendantElementOrSelfStream()
                        .filter(e -> !e.text().isBlank())
                        .map(e -> Integer.parseInt(e.text()))
                        .collect(Collectors.toSet())
        );

        System.out.println(docPrinter.print(updatedElement));
    }

    private Element createElement() {
        InputStream is = ThreadSafetyTest.class.getResourceAsStream("/ones.xml");
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(new InputSource(is)).documentElement();
    }

    private Element updateElement(Element element) {
        AtomicInteger index = new AtomicInteger(0);

        List<CompletableFuture<IndexedElement>> indexedChildElementFutures =
                element.childElementStream()
                        .map(e -> createUpdateElementFuture(new IndexedElement(e, index.getAndIncrement())))
                        .toList();

        List<IndexedElement> indexedChildElements = indexedChildElementFutures
                .stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        ImmutableList<Node> childElementNodes = indexedChildElements.stream()
                .sorted(Comparator.comparingInt(IndexedElement::index))
                .map(IndexedElement::element)
                .collect(ImmutableList.toImmutableList());

        return element.withChildren(childElementNodes);
    }

    private CompletableFuture<IndexedElement> createUpdateElementFuture(IndexedElement indexedElement) {
        return CompletableFuture.supplyAsync(() -> indexedElement.withElement(
                doUpdateElement(indexedElement.element())
        ));
    }

    private Element doUpdateElement(Element element) {
        logger.info("Current thread (start): " + Thread.currentThread());

        Element resultElement = element.transformDescendantElementsOrSelf(elem -> {
            ImmutableList<Node> newChildren = elem.children()
                    .stream()
                    .map(ch -> {
                        // Somewhat expensive computation, to trigger parallelism

                        boolean propertyHolds = AncestryAwareNodes.Element.create(Optional.empty(), element)
                                .descendantElementOrSelfStream()
                                .anyMatch(e -> !(ch instanceof Element che) || e.underlyingElement().name().equals(che.name()));
                        Preconditions.checkArgument(propertyHolds);

                        if ((ch instanceof Text text) && !text.value().isBlank()) {
                            return new Text(String.valueOf(Integer.parseInt(text.value()) + 1), false);
                        } else {
                            return ch;
                        }
                    })
                    .collect(ImmutableList.toImmutableList());

            return elem.withChildren(newChildren);
        });

        logger.info("Current thread (finish): " + Thread.currentThread());

        return resultElement;
    }
}
