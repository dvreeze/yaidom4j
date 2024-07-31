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

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Elements;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
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

    public record ElementNameCount(QName name, long count) {
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);

        logTime("Going to parse document ...");

        SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
        saxParserFactory.setNamespaceAware(true); // Important!
        saxParserFactory.setValidating(false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = saxParserFactory.newSAXParser();
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        parser.parse(new InputSource(inputFile.toURL().openStream()), saxHandler);
        Document doc = saxHandler.resultingDocument().withUri(inputFile);

        logTime("Parsed \"immutable DOM\" document " + doc.uriOption().map(Object::toString).orElse(""));

        System.out.println();
        System.out.printf(
                "Number of elements: %d%n",
                Elements.queryApi().elementStream(doc.documentElement()).count());

        System.out.println();
        logTime("Retrieving element counts ...");

        List<ElementNameCount> elementCounts =
                Elements.queryApi().elementStream(doc.documentElement())
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
                Elements.queryApi().elementStream(doc.documentElement())
                        .map(Element::namespaceScope)
                        .distinct()
                        .toList()
        );

        System.out.println();
        logTime("Ready");
    }

    private static void logTime(String message) {
        System.out.printf("[%s] %s%n", Instant.now(), message);
    }
}
