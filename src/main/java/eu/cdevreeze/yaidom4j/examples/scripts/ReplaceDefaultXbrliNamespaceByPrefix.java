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
import com.google.common.collect.ImmutableList;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Text;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

/**
 * Replaces the default "http://www.xbrl.org/2003/instance" namespace by a prefix for that namespace
 * ("xbrli"), but only if the same default namespace is used throughout the XML. It is assumed that
 * default namespace is no longer needed afterward, once we use a prefix for it. Note that this
 * script does take element content in mind, for xbrli:pure and xbrli:shares values of element
 * xbrli:measure.
 *
 * @author Chris de Vreeze
 */
public class ReplaceDefaultXbrliNamespaceByPrefix {

    // Note that the xsi:schemaLocation attribute, if any, can stay the same

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);
        String prefix = args.length == 2 ? args[1] : "xbrli";

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(inputFile);

        Element transformedElement = replaceDefaultNamespaceByPrefix(doc.documentElement(), prefix);

        String xmlString = DocumentPrinters.instance().print(transformedElement);
        System.out.println(xmlString);
    }

    // Programmatic access to the functionality

    public static Element replaceDefaultNamespaceByPrefix(Element element, String prefix) {
        Preconditions.checkArgument(!prefix.isBlank());
        String ns = element.namespaceScope().defaultNamespaceOption().orElseThrow();

        checkElement(element, prefix);

        Element transformedElement = transformElement(element, prefix, ns);

        ClarkNodes.Element clarkDocElem =
                transformElementForComparison(element.toClarkNode(), prefix);
        ClarkNodes.Element transformedClarkElem =
                transformElementForComparison(transformedElement.toClarkNode(), prefix);

        Preconditions.checkArgument(clarkDocElem.equals(transformedClarkElem));

        return transformedElement;
    }

    private static void checkElement(Element element, String prefix) {
        String ns = element.namespaceScope().defaultNamespaceOption().orElseThrow();
        Preconditions.checkArgument(ns.equals(XBRLI_NS));
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

                    ImmutableList<Node> children = adaptMeasure(elem, prefix).children();

                    return new Element(
                            elem.name().getPrefix().isEmpty() ?
                                    new QName(namespace, elem.name().getLocalPart(), prefix) :
                                    elem.name(),
                            elem.attributes(), // default namespace not applicable to attributes
                            elem.namespaceScope().withoutDefaultNamespace().resolve(prefix, namespace),
                            children
                    );
                }
        );
    }

    private static Element adaptMeasure(Element element, String xbrliPrefix) {
        if (element.name().equals(new QName(XBRLI_NS, "measure"))) {
            if (element.text().equals("pure")) {
                String syntacticQName = xbrliPrefix + ":pure";
                return element.withChildren(ImmutableList.of(new Text(syntacticQName, false)));
            } else if (element.text().equals("shares")) {
                String syntacticQName = xbrliPrefix + ":shares";
                return element.withChildren(ImmutableList.of(new Text(syntacticQName, false)));
            } else {
                return element;
            }
        } else {
            return element;
        }
    }

    private static ClarkNodes.Element transformElementForComparison(ClarkNodes.Element element, String prefix) {
        return element.transformDescendantElementsOrSelf(
                elem -> {
                    ImmutableList<ClarkNodes.Node> children = adaptMeasureForComparison(elem, prefix).children();

                    return new ClarkNodes.Element(elem.name(), elem.attributes(), children);
                }
        );
    }

    private static ClarkNodes.Element adaptMeasureForComparison(ClarkNodes.Element element, String xbrliPrefix) {
        if (element.name().equals(new QName(XBRLI_NS, "measure"))) {
            if (element.text().equals("pure") || element.text().equals(xbrliPrefix + ":pure")) {
                String qname = new QName(XBRLI_NS, "pure").toString();
                return element.withChildren(ImmutableList.of(new ClarkNodes.Text(qname)));
            } else if (element.text().equals("shares") || element.text().equals(xbrliPrefix + ":shares")) {
                String qname = new QName(XBRLI_NS, "shares").toString();
                return element.withChildren(ImmutableList.of(new ClarkNodes.Text(qname)));
            } else {
                return element;
            }
        } else {
            return element;
        }
    }
}
