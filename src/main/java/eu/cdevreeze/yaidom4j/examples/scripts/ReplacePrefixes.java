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

package eu.cdevreeze.yaidom4j.examples.scripts;

import com.google.common.base.Preconditions;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Program that repeatedly invokes programs ReplacePrefix and ReplaceDefaultNamespaceByPrefix
 * (the latter one hopefully at most once). The first program argument is the URI of the input
 * XML file to edit. The second program argument is the URI of an XML configuration file holding
 * the prefix mappings. Its format is as follows: the root element is called "namespaces", and the
 * child elements are called "namespace". Those "namespace" elements have a namespace URI as text
 * content and attributes "oldPrefix" and "newPrefix" with obvious semantics. Multiple "namespace"
 * elements may refer to the same namespace. After all, a namespace can be referred to by more than
 * one namespace prefix.
 * <p>
 * Requirements/limitations of programs {@link ReplacePrefix} and {@link ReplaceDefaultNamespaceByPrefix} also apply
 * to this program.
 *
 * @author Chris de Vreeze
 */
public class ReplacePrefixes {

    public record PrefixMapping(String namespace, String oldPrefix, String newPrefix) {

        public PrefixMapping {
            Preconditions.checkArgument(!namespace.isBlank());
            Preconditions.checkArgument(!newPrefix.isBlank());
        }
    }

    // Note that the xsi:schemaLocation attribute, if any, can stay the same

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(1, args.length);
        URI inputFile = new URI(args[0]);
        URI configFile = new URI(args[1]);

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(inputFile);
        Document configDoc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(configFile);

        List<PrefixMapping> prefixMappings = extractPrefixMappings(configDoc.documentElement());

        Element transformedElement = replacePrefixes(doc.documentElement(), prefixMappings);

        String xmlString = DocumentPrinters.instance().print(transformedElement);
        System.out.println(xmlString);
    }

    // Programmatic access to the functionality

    public static Element replacePrefixes(Element element, List<PrefixMapping> prefixMappings) {
        // To implement this method with a Java Stream pipeline instead of recursion, we would need a Java 22+ folding Gatherer
        if (prefixMappings.isEmpty()) {
            return element;
        } else {
            // Recursive
            return replacePrefixes(
                    replacePrefix(element, prefixMappings.get(0)),
                    prefixMappings.subList(1, prefixMappings.size())
            );
        }
    }

    public static List<PrefixMapping> extractPrefixMappings(Element configElement) {
        Preconditions.checkArgument(configElement.name().equals(new QName("namespaces")));

        return configElement
                .childElementStream(e -> e.name().equals(new QName("namespace")))
                .map(e ->
                        new PrefixMapping(
                                e.text().trim(),
                                e.attribute(new QName("oldPrefix")),
                                e.attribute(new QName("newPrefix"))
                        )
                )
                .toList();
    }

    private static Element replacePrefix(Element element, PrefixMapping prefixMapping) {
        if (prefixMapping.oldPrefix().isBlank()) {
            Preconditions.checkArgument(element.namespaceScope().defaultNamespaceOption().equals(Optional.of(prefixMapping.namespace())));
            return ReplaceDefaultNamespaceByPrefix.replaceDefaultNamespaceByPrefix(element, prefixMapping.newPrefix);
        } else {
            return ReplacePrefix.replacePrefix(element, prefixMapping.oldPrefix(), prefixMapping.namespace(), prefixMapping.newPrefix());
        }
    }
}
