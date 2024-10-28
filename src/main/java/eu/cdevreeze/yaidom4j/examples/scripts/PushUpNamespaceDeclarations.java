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
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;

import javax.xml.XMLConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This program tries to push up namespace declarations to the root element. Default namespace declarations
 * are not pushed up, because that would likely change the semantics of the XML document.
 * The program takes only one program argument, namely the XML document URI.
 *
 * @author Chris de Vreeze
 */
public class PushUpNamespaceDeclarations {

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(inputFile);

        // A parsed XML 1.0 document element has no prefixed namespace undeclarations
        Preconditions.checkArgument(
                doc.documentElement().equals(doc.documentElement().notUndeclaringPrefixes(NamespaceScope.empty())));

        Element transformedElement = pushUpNamespaceDeclarations(doc.documentElement());

        String xmlString = DocumentPrinters.instance().print(transformedElement);
        System.out.println(xmlString);
    }

    // Programmatic access to the functionality

    public static Element pushUpNamespaceDeclarations(Element element) {
        List<NamespaceScope> namespaceScopes = element
                .elementStream()
                .map(Element::namespaceScope)
                .distinct()
                .toList();

        Map<String, List<String>> prefixNamespaces = namespaceScopes
                .stream()
                .flatMap(nsScope -> nsScope.inScopeNamespaces().entrySet().stream())
                .distinct()
                .filter(kv -> !kv.getKey().equals(XMLConstants.DEFAULT_NS_PREFIX))
                .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(
                                        Map.Entry::getValue,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                values -> values.stream().distinct().toList()
                                        )
                                )
                        )
                );

        ImmutableMap<String, String> stablePrefixNamespaces = prefixNamespaces.entrySet()
                .stream()
                .filter(kv -> kv.getValue().size() == 1)
                .map(kv -> Map.entry(kv.getKey(), kv.getValue().get(0)))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        NamespaceScope startScope = NamespaceScope.from(stablePrefixNamespaces);

        Element resultElement = element.notUndeclaringPrefixes(startScope);

        Preconditions.checkArgument(element.toClarkNode().equals(resultElement.toClarkNode()));

        return resultElement;
    }
}
