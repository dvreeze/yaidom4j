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
import java.util.Objects;
import java.util.Optional;

/**
 * Replaces the default namespace by a prefix for that namespace, but only if the same default
 * namespace is used throughout the XML. It is assumed that default namespace is no longer needed
 * afterward, once we use a prefix for it. Note that this script must be tweaked if XML edits are
 * needed for attribute values or element text.
 *
 * @author Chris de Vreeze
 */
public class ReplaceDefaultNamespaceByPrefix {

    // Note that the xsi:schemaLocation attribute, if any, can stay the same

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(1, args.length);
        URI inputFile = new URI(args[0]);
        String prefix = args[1];

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build().parse(inputFile);

        Element transformedElement = replaceDefaultNamespaceByPrefix(doc.documentElement(), prefix);

        String xmlString = DocumentPrinters.print(transformedElement);
        System.out.println(xmlString);
    }

    // Programmatic access to the functionality

    public static Element replaceDefaultNamespaceByPrefix(Element element, String prefix) {
        Preconditions.checkArgument(!prefix.isBlank());
        String ns = element.namespaceScope().defaultNamespaceOption().orElseThrow();

        checkElement(element, prefix);

        Element transformedElement = transformElement(element, prefix, ns);

        Preconditions.checkArgument(
                element.toClarkNode().equals(transformedElement.toClarkNode())
        );

        return transformedElement;
    }

    private static void checkElement(Element element, String prefix) {
        String ns = element.namespaceScope().defaultNamespaceOption().orElseThrow();
        Preconditions.checkArgument(
                element.elementStream()
                        .allMatch(e -> e.namespaceScope().defaultNamespaceOption().equals(Optional.of(ns)))
        );
        Preconditions.checkArgument(
                element.elementStream()
                        .allMatch(e -> e.namespaceScope().findNamespaceOfPrefix(prefix).stream().allMatch(ns::equals))
        );
    }

    private static Element transformElement(Element element, String prefix, String namespace) {
        return element.transformDescendantElementsOrSelf(
                elem -> {
                    Preconditions.checkArgument(elem.namespaceScope().defaultNamespaceOption().isPresent());
                    Preconditions.checkArgument(elem.namespaceScope().defaultNamespaceOption().equals(element.namespaceScope().defaultNamespaceOption()));

                    return new Element(
                            elem.name().getPrefix().isEmpty() ?
                                    new QName(namespace, elem.name().getLocalPart(), prefix) :
                                    elem.name(),
                            elem.attributes(), // default namespace not applicable to attributes
                            elem.namespaceScope().withoutDefaultNamespace().resolve(prefix, namespace),
                            elem.children()
                    );
                }
        );
    }
}
