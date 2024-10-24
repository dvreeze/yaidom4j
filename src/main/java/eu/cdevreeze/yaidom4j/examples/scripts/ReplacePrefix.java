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
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * Replaces the given namespace prefix (2nd program argument), if bound to the given namespace
 * (3rd program argument), by the second namespace prefix (4th program argument). It is not necessary
 * that all namespace declarations are in the root element. The default namespace (prefix "") can not
 * be used in this program, so both prefixes must be non-empty. It is assumed that the first prefix
 * is no longer needed afterward for the given namespace. Note that this script must be tweaked if
 * XML edits are needed for attribute values or element text.
 *
 * @author Chris de Vreeze
 */
public class ReplacePrefix {

    // Note that the xsi:schemaLocation attribute, if any, can stay the same

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(3, args.length);
        URI inputFile = new URI(args[0]);
        String oldPrefix = args[1];
        Preconditions.checkArgument(!oldPrefix.isBlank());
        String namespace = args[2];
        Preconditions.checkArgument(!namespace.isBlank());
        String newPrefix = args[3];
        Preconditions.checkArgument(!newPrefix.isBlank());
        Preconditions.checkArgument(!newPrefix.equals(oldPrefix));

        Document doc = DocumentParsers.removingInterElementWhitespace().parse(inputFile);

        Element transformedElement = replacePrefix(doc.documentElement(), oldPrefix, namespace, newPrefix);

        String xmlString = DocumentPrinters.print(transformedElement);
        System.out.println(xmlString);
    }

    // Programmatic access to the functionality

    public static Element replacePrefix(Element element, String oldPrefix, String namespace, String newPrefix) {
        Preconditions.checkArgument(!oldPrefix.isBlank());
        Preconditions.checkArgument(!namespace.isBlank());
        Preconditions.checkArgument(!newPrefix.isBlank());
        Preconditions.checkArgument(!newPrefix.equals(oldPrefix));

        checkElement(element, oldPrefix, namespace, newPrefix);

        Element transformedElement = transformElement(element, oldPrefix, namespace, newPrefix);

        Preconditions.checkArgument(
                element.toClarkNode().equals(transformedElement.toClarkNode())
        );

        return transformedElement;
    }

    private static void checkElement(Element element, String oldPrefix, String namespace, String newPrefix) {
        Preconditions.checkArgument(
                element.elementStream()
                        .allMatch(e -> e.namespaceScope().findNamespaceOfPrefix(oldPrefix).stream().allMatch(namespace::equals))
        );
        Preconditions.checkArgument(
                element.elementStream()
                        .allMatch(e -> e.namespaceScope().findNamespaceOfPrefix(newPrefix).stream().allMatch(namespace::equals))
        );
    }

    private static Element transformElement(Element element, String oldPrefix, String namespace, String newPrefix) {
        return element.transformDescendantElementsOrSelf(
                elem ->
                        new Element(
                                replacePrefix(elem.name(), oldPrefix, namespace, newPrefix),
                                elem.attributes()
                                        .entrySet()
                                        .stream()
                                        .collect(
                                                ImmutableMap.toImmutableMap(
                                                        kv -> replacePrefix(kv.getKey(), oldPrefix, namespace, newPrefix),
                                                        Map.Entry::getValue
                                                )),
                                elem.namespaceScope()
                                        .resolve(oldPrefix, "") // oldPrefix not in scope anywhere anymore, so no (disallowed) prefixed namespace undeclarations
                                        .resolve(newPrefix, namespace), // newPrefix in scope, whether used or not
                                elem.children()
                        )
        );
    }

    private static QName replacePrefix(QName name, String oldPrefix, String namespace, String newPrefix) {
        if (oldPrefix.equals(name.getPrefix())) {
            Preconditions.checkArgument(namespace.equals(name.getNamespaceURI()));
            return new QName(name.getNamespaceURI(), name.getLocalPart(), newPrefix);
        } else {
            return name;
        }
    }
}
