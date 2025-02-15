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
import eu.cdevreeze.yaidom4j.dom.immutabledom.Text;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinter;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static final int NUMBER_OF_UPDATES = 20;

    @Test
    void testThreadSafety() throws ExecutionException, InterruptedException {
        Element startElement = createElement();
        final AtomicReference<Element> elementHolder = new AtomicReference<>(startElement);

        ImmutableList<ImmutableList<Integer>> elemNavigationPaths =
                AncestryAwareNodes.Element.create(Optional.empty(), startElement)
                        .descendantElementOrSelfStream()
                        .map(AncestryAwareNodes.Element::navigationPath)
                        .collect(ImmutableList.toImmutableList());

        List<CompletableFuture<Void>> updateFutures =
                IntStream.range(0, NUMBER_OF_UPDATES)
                        .mapToObj(e -> CompletableFuture.runAsync(() -> {
                            logger.info("Current thread (start): " + Thread.currentThread());

                            updateAllElements(elementHolder, elemNavigationPaths);
                        }))
                        .toList();

        CompletableFuture<Void> resultFuture = CompletableFuture.allOf(
                updateFutures.toArray(CompletableFuture[]::new)
        );

        resultFuture.get();

        Element endElement = elementHolder.get();

        DocumentPrinter docPrinter = DocumentPrinters.instance();

        // Thread-safe, without any memory visibility problems; all numbers are now NUMBER_OF_UPDATES + 1 after the update
        assertEquals(
                Set.of(NUMBER_OF_UPDATES + 1),
                endElement.descendantElementOrSelfStream()
                        .filter(e -> !e.text().isBlank())
                        .map(e -> Integer.parseInt(e.text()))
                        .collect(Collectors.toSet())
        );

        System.out.println(docPrinter.print(endElement));
    }

    private Element createElement() {
        InputStream is = ThreadSafetyTest.class.getResourceAsStream("/ones.xml");
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(new InputSource(is)).documentElement();
    }

    private void updateAllElements(
            AtomicReference<Element> elementHolder,
            ImmutableList<ImmutableList<Integer>> elementNavigationPaths
    ) {
        updateAllElements(
                elementHolder,
                elementNavigationPaths,
                elem -> {
                    if (elem.text().isBlank()) {
                        return elem;
                    } else {
                        logger.fine("Current thread (updating element): " + Thread.currentThread());
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
        );
    }

    private void updateAllElements(
            AtomicReference<Element> elementHolder,
            ImmutableList<ImmutableList<Integer>> elementNavigationPaths,
            UnaryOperator<Element> f
    ) {
        if (!elementNavigationPaths.isEmpty()) {
            ImmutableList<Integer> lastPath = elementNavigationPaths.get(elementNavigationPaths.size() - 1);
            ImmutableList<ImmutableList<Integer>> pathsButLast =
                    elementNavigationPaths.subList(0, elementNavigationPaths.size() - 1);

            elementHolder.updateAndGet(e ->
                    updateElement(e, lastPath, f)
            );
            // Recursive call
            updateAllElements(elementHolder, pathsButLast, f);
        }
    }

    private Element updateElement(
            Element element,
            ImmutableList<Integer> elementNavigationPath,
            UnaryOperator<Element> f) {
        // Very expensive recursive function

        if (elementNavigationPath.isEmpty()) {
            return f.apply(element);
        } else {
            int firstNavigationPathEntry = elementNavigationPath.get(0);

            AtomicInteger elemIndex = new AtomicInteger(0);
            return element.withChildren(
                    element.children()
                            .stream()
                            .map(ch -> {
                                if (ch instanceof Element che) {
                                    int currElemIndex = elemIndex.getAndIncrement();

                                    if (currElemIndex == firstNavigationPathEntry) {
                                        // Recursive call
                                        return updateElement(
                                                che,
                                                removeFirstEntry(elementNavigationPath),
                                                f
                                        );
                                    } else {
                                        return che;
                                    }
                                } else {
                                    return ch;
                                }
                            })
                            .collect(ImmutableList.toImmutableList())
            );
        }
    }

    private static ImmutableList<Integer> removeFirstEntry(ImmutableList<Integer> elementNavigationPath) {
        Preconditions.checkArgument(!elementNavigationPath.isEmpty());
        return elementNavigationPath.subList(1, elementNavigationPath.size());
    }
}
