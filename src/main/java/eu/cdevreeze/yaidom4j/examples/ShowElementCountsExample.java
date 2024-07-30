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
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.JaxpDomToImmutableDomConverter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
public class ShowElementCountsExample {

    public record ElementNameCount(QName name, long count) {
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
        docBuilderFactory.setNamespaceAware(true); // Important!
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = JaxpDomToImmutableDomConverter.convertDocument(docBuilder.parse(new InputSource(inputFile.toURL().openStream())));

        System.out.printf(
                "Number of elements: %d%n",
                Elements.queryApi().elementStream(doc.documentElement()).count());

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
    }
}
