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
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Thread unsafety test for W3C DOM. In several threads an XML tree is updated simultaneously, causing data
 * corruption, illustrating the thread-unsafety of W3C DOM mutable DOM trees.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
public class W3cDomThreadUnsafetyTest {

    private static final Logger logger = Logger.getGlobal();

    private record IndexedElement(Element element, int index) {

        IndexedElement withElement(Element newElement) {
            return new IndexedElement(newElement, index());
        }
    }

    @Test
    void testThreadUnsafety() {
        Element startElement = createElement();
        Element updatedElement = updateElement(startElement);

        DocumentPrinter docPrinter = DocumentPrinters.instance();

        // Not thread-safe, inviting memory visibility problems; maybe not all numbers are 2 after the update
        Set<Integer> numbers = updatedElement.descendantElementOrSelfStream()
                .filter(e -> !e.text().isBlank())
                .map(e -> Integer.parseInt(e.text()))
                .collect(Collectors.toSet());

        logger.info("Number of distinct numbers after the XML update: " + numbers.size());

        // Very weak assertion, of course
        assertFalse(numbers.isEmpty());

        System.out.println(docPrinter.print(updatedElement));
    }

    private Element createElement() {
        InputStream is = W3cDomThreadUnsafetyTest.class.getResourceAsStream("/ones.xml");
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

        org.w3c.dom.Document domDoc = ImmutableDomToJaxpDomConverter.convertDocument(
                new Document(Optional.empty(), ImmutableList.of(element)),
                DocumentBuilderFactory.newDefaultNSInstance()
        );
        org.w3c.dom.Element domElement = domDoc.getDocumentElement();

        doUpdateElement(domElement, domDoc, NamespaceScope.empty());

        logger.info("Current thread (finish): " + Thread.currentThread());

        return JaxpDomToImmutableDomConverter.convertElement(domElement, NamespaceScope.empty());
    }

    private void doUpdateElement(org.w3c.dom.Element domElement, org.w3c.dom.Document domDoc, NamespaceScope parentScope) {
        // Recursive
        NodeList childNodes = domElement.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node childNode = childNodes.item(i);

            if (childNode instanceof org.w3c.dom.Text text) {
                if (!text.getData().isBlank()) {
                    text.setData(String.valueOf(Integer.parseInt(text.getData()) + 1));
                }
            } else if (childNode instanceof org.w3c.dom.Element che) {
                // Somewhat expensive computation, to trigger parallelism

                Element element = JaxpDomToImmutableDomConverter.convertDocument(domDoc).documentElement();
                Element childElement = JaxpDomToImmutableDomConverter.convertElement(che, parentScope);

                boolean propertyHolds = AncestryAwareNodes.Element.create(Optional.empty(), element)
                        .descendantElementOrSelfStream()
                        .anyMatch(e -> e.underlyingElement().name().equals(childElement.name()));
                Preconditions.checkArgument(propertyHolds);

                NamespaceScope newScope = getNamespaceScope(che, parentScope);
                // Recursion
                doUpdateElement(che, domDoc, newScope);
            }
        }
    }

    private NamespaceScope getNamespaceScope(org.w3c.dom.Element domElement, NamespaceScope parentScope) {
        ImmutableMap<String, String> decls = extractNamespaceDeclarations(domElement.getAttributes());
        return parentScope.resolve(decls);
    }

    private ImmutableMap<String, String> extractNamespaceDeclarations(NamedNodeMap domAttrs) {
        return IntStream.range(0, domAttrs.getLength())
                .mapToObj(i -> (org.w3c.dom.Attr) domAttrs.item(i))
                .filter(this::isNamespaceDeclaration)
                .map(this::extractNamespaceDeclaration)
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isNamespaceDeclaration(org.w3c.dom.Attr attr) {
        String name = attr.getName();
        String[] nameParts = name.split(":");
        Preconditions.checkArgument(nameParts.length >= 1 && nameParts.length <= 2);
        return nameParts[0].equals("xmlns");
    }

    private Map.Entry<String, String> extractNamespaceDeclaration(org.w3c.dom.Attr attr) {
        String name = attr.getName();
        String[] nameParts = name.split(":");
        Preconditions.checkArgument(nameParts.length >= 1 && nameParts.length <= 2);
        Preconditions.checkArgument(nameParts[0].equals("xmlns"));
        String prefix = (nameParts.length == 1) ? "" : nameParts[1];
        String attrValue = attr.getValue();
        return Map.entry(prefix, attrValue);
    }
}
