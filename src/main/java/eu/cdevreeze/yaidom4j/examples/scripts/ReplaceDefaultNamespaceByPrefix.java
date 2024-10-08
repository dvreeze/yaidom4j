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
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
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

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(1, args.length);
        URI inputFile = new URI(args[0]);
        String prefix = args[1];

        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();
        SaxParsers.parse(inputFile, saxHandler);
        Document doc = saxHandler.resultingDocument().withUri(inputFile).removeInterElementWhitespace();

        checkElement(doc.documentElement());

        String ns = doc.documentElement().namespaceScope().defaultNamespaceOption().orElseThrow();
        Element transformedElement = transformElement(doc.documentElement(), prefix, ns);

        Preconditions.checkArgument(
                doc.documentElement().toClarkNode().equals(transformedElement.toClarkNode())
        );

        String xmlString = printElement(transformedElement);
        System.out.println(xmlString);
    }

    private static void checkElement(Element element) {
        String ns = element.namespaceScope().defaultNamespaceOption().orElseThrow();
        Preconditions.checkArgument(
                element.elementStream()
                        .allMatch(e -> e.namespaceScope().defaultNamespaceOption().equals(Optional.of(ns)))
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
