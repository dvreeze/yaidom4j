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
import eu.cdevreeze.yaidom4j.core.ElementNavigationPath;
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
 * <p>
 * I tried out a similar test for org.w3c.dom, to show that those mutable node trees are not thread-safe.
 * It was even worse than that, multithreaded access to the same node tree led to data corruption.
 * See for example
 * <a href="https://issues.apache.org/jira/browse/FOP-2970?page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel&focusedCommentId=17623155">this issue</a>.
 * I got a very similar exception, namely an NPE with the following message:
 * <quote>Cannot assign field "fChildIndex" because "this.fNodeListCache" is null</quote>.
 * <p>
 * In all fairness, functional updates for immutable yaidom4j element trees are quite expensive at runtime.
 *
 * @author Chris de Vreeze
 */
class ThreadSafetyTest {

    private static final Logger logger = Logger.getGlobal();

    private static final int NUMBER_OF_UPDATES = 20;

    @Test
    void testThreadSafety() throws ExecutionException, InterruptedException {
        final AtomicReference<Element> elementHolder = new AtomicReference<>(createElement());

        ImmutableList<ElementNavigationPath> elemNavigationPaths =
                AncestryAwareNodes.Element.create(Optional.empty(), elementHolder.get())
                        .descendantElementOrSelfStream()
                        .map(AncestryAwareNodes.Element::elementNavigationPath)
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
            ImmutableList<ElementNavigationPath> elementNavigationPaths
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
            ImmutableList<ElementNavigationPath> elementNavigationPaths,
            UnaryOperator<Element> f
    ) {
        if (!elementNavigationPaths.isEmpty()) {
            ElementNavigationPath lastPath = elementNavigationPaths.get(elementNavigationPaths.size() - 1);
            ImmutableList<ElementNavigationPath> pathsButLast =
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
            ElementNavigationPath elementNavigationPath,
            UnaryOperator<Element> f) {
        // Very expensive recursive function

        if (elementNavigationPath.isEmpty()) {
            return f.apply(element);
        } else {
            int firstNavigationPathEntry = elementNavigationPath.getEntry(0);

            AtomicInteger elemIndex = new AtomicInteger(0);
            return element.withChildren(
                    element.children()
                            .stream()
                            .map(ch -> {
                                if (ch instanceof Element che) {
                                    int currElemIndex = elemIndex.getAndIncrement();

                                    if (currElemIndex == firstNavigationPathEntry) {
                                        // Recursive call
                                        return updateElement(che, elementNavigationPath.withoutFirstEntry(), f);
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
