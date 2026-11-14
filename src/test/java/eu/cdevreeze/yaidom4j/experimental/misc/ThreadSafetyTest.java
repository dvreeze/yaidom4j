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

import eu.cdevreeze.yaidom4j.experimental.dom.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.dom.Node;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinter;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinters;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
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

    private static final Logger logger = LoggerFactory.getLogger(ThreadSafetyTest.class);

    private static final int NUMBER_OF_UPDATES = 20;

    @Test
    void testThreadSafety() throws ExecutionException, InterruptedException {
        final AtomicReference<GreenXmlTrees.Element> elementHolder = new AtomicReference<>(createElement());

        // Node navigation paths starting with an element, not a document
        List<List<Integer>> nodeNavigationPaths =
                new Element(0, null, elementHolder.get())
                        .descendantOrSelfElementStream()
                        .map(Node::nodeNavigationPath)
                        .toList();

        List<CompletableFuture<Void>> updateFutures =
                IntStream.range(0, NUMBER_OF_UPDATES)
                        .mapToObj(_ -> CompletableFuture.runAsync(() -> {
                            logger.info("Current thread (start): {}", Thread.currentThread());

                            updateAllElements(elementHolder, nodeNavigationPaths);
                        }))
                        .toList();

        CompletableFuture<Void> resultFuture = CompletableFuture.allOf(
                updateFutures.toArray(CompletableFuture[]::new)
        );

        resultFuture.get();

        GreenXmlTrees.Element endElement = elementHolder.get();

        DocumentPrinter docPrinter = DocumentPrinters.instance();

        // Thread-safe, without any memory visibility problems; all numbers are now NUMBER_OF_UPDATES + 1 after the update
        assertEquals(
                Set.of(NUMBER_OF_UPDATES + 1),
                endElement.descendantOrSelfElementStream()
                        .filter(e -> !e.text().isBlank())
                        .map(e -> Integer.parseInt(e.text()))
                        .collect(Collectors.toSet())
        );

        if (Boolean.parseBoolean(System.getProperty("debugThreadSafetyTest"))) {
            System.out.println(docPrinter.print(new Element(0, null, endElement)));
        }
    }

    private GreenXmlTrees.Element createElement() {
        InputStream is = ThreadSafetyTest.class.getResourceAsStream("/ones.xml");
        DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
        return docParser.parse(new InputSource(is)).documentElement().greenTreeNode();
    }

    private void updateAllElements(
            AtomicReference<GreenXmlTrees.Element> elementHolder,
            List<List<Integer>> nodeNavigationPaths
    ) {
        updateAllElements(
                elementHolder,
                nodeNavigationPaths,
                elem -> {
                    if (elem.text().isBlank()) {
                        return elem;
                    } else {
                        int oldValue = Integer.parseInt(elem.text());
                        int newValue = oldValue + 1;
                        logger.trace("Current thread (updating element from {} to {}): {}", oldValue, newValue, Thread.currentThread());
                        return elem.withChildren(
                                List.of(
                                        new GreenXmlTrees.Text(String.valueOf(newValue))
                                )
                        );
                    }
                }
        );
    }

    private void updateAllElements(
            AtomicReference<GreenXmlTrees.Element> elementHolder,
            List<List<Integer>> nodeNavigationPaths,
            UnaryOperator<GreenXmlTrees.Element> f
    ) {
        if (!nodeNavigationPaths.isEmpty()) {
            List<Integer> lastPath = nodeNavigationPaths.getLast();
            List<List<Integer>> pathsButLast =
                    nodeNavigationPaths.subList(0, nodeNavigationPaths.size() - 1);

            elementHolder.updateAndGet(e ->
                    updateElement(e, lastPath, f)
            );
            // Recursive call
            updateAllElements(elementHolder, pathsButLast, f);
        }
    }

    private GreenXmlTrees.Element updateElement(
            GreenXmlTrees.Element element,
            List<Integer> nodeNavigationPath,
            UnaryOperator<GreenXmlTrees.Element> f) {
        // Very expensive recursive function

        if (nodeNavigationPath.isEmpty()) {
            return f.apply(element);
        } else {
            int firstNavigationPathEntry = nodeNavigationPath.getFirst();

            AtomicInteger nodeIndex = new AtomicInteger(0);
            return element.withChildren(
                    element.children()
                            .stream()
                            .map(ch -> {
                                int currNodeIndex = nodeIndex.getAndIncrement();

                                if (ch instanceof GreenXmlTrees.Element che) {
                                    if (currNodeIndex == firstNavigationPathEntry) {
                                        // Recursive call
                                        return updateElement(che, nodeNavigationPath.stream().skip(1).toList(), f);
                                    } else {
                                        return che;
                                    }
                                } else {
                                    return ch;
                                }
                            })
                            .toList()
            );
        }
    }
}
