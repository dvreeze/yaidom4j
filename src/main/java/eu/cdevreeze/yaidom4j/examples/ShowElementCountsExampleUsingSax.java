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

package eu.cdevreeze.yaidom4j.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Text;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomConsumingSaxEventGenerator;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.jaxp.TransformerHandlers;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Example showing element counts of a parsed document.
 *
 * @author Chris de Vreeze
 */
public class ShowElementCountsExampleUsingSax {

    record ElementNameCount(QName name, long count) {
    }

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);

        logTime("Going to parse document ...");

        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        SaxParsers.parse(inputFile, saxHandler);

        Document doc = saxHandler.resultingDocument().withUri(inputFile).removeInterElementWhitespace();

        logTime("Parsed \"immutable DOM\" document " + doc.uriOption().map(Object::toString).orElse(""));

        System.out.println();
        System.out.printf(
                "Number of elements: %d%n",
                doc.documentElement().elementStream().count());

        System.out.println();
        logTime("Retrieving element counts ...");

        List<ElementNameCount> elementCounts =
                doc.documentElement().elementStream()
                        .map(Element::name)
                        .collect(Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()))
                        .entrySet()
                        .stream()
                        .map(kv -> new ElementNameCount(kv.getKey(), kv.getValue()))
                        .sorted(Comparator.comparingLong(ElementNameCount::count).reversed())
                        .toList();

        System.out.println();
        elementCounts.forEach(elemCount ->
                System.out.printf("Element count for %s: %d%n", elemCount.name(), elemCount.count())
        );

        System.out.println();
        logTime("Retrieving distinct namespace scopes ...");

        System.out.println();
        System.out.printf(
                "Distinct namespace scopes: %s%n",
                doc.documentElement().elementStream()
                        .map(Element::namespaceScope)
                        .distinct()
                        .toList()
        );

        System.out.println();
        String xmlOutput = printElement(convertElementCountsToXml(elementCounts));
        System.out.println(xmlOutput);

        System.out.println();
        logTime("Ready");
    }

    private static void logTime(String message) {
        System.out.printf("[%s] %s%n", Instant.now(), message);
    }

    private static Element convertElementCountsToXml(List<ElementNameCount> elementNameCounts) {
        var ns = "http://yaidom4j/showElementCounts";
        var scope = NamespaceScope.from(ImmutableMap.of("", ns));

        return new Element(
                new QName(ns, "elementNameCounts"),
                ImmutableMap.of(),
                scope,
                elementNameCounts.stream()
                        .map(nameCount -> new Element(
                                new QName(ns, "elementNameCount"),
                                ImmutableMap.of(),
                                scope,
                                ImmutableList.of(
                                        new Element(
                                                new QName(ns, "elementName"),
                                                ImmutableMap.of(),
                                                scope,
                                                ImmutableList.of(new Text(nameCount.name().toString(), false))
                                        ),
                                        new Element(
                                                new QName(ns, "count"),
                                                ImmutableMap.of(),
                                                scope,
                                                ImmutableList.of(new Text(String.valueOf(nameCount.count()), false))
                                        )
                                )
                        ))
                        .collect(ImmutableList.toImmutableList())
        );
    }

    private static String printElement(Element element) {
        var sw = new StringWriter();
        var streamResult = new StreamResult(sw);

        TransformerHandler th = TransformerHandlers.newTransformerHandler();
        th.setResult(streamResult);

        var saxEventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);

        saxEventGenerator.processElement(element, NamespaceScope.empty());

        return sw.toString();
    }
}
