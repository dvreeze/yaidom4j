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
import eu.cdevreeze.yaidom4j.dom.immutabledom.*;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Example showing equality of different XML documents that use different namespace prefixes and namespace
 * declarations in different elements but that are otherwise equal.
 * <p>
 * The example is taken from <a href="https://www.lenzconsulting.com/namespaces/">Understanding XML Namespaces</a>
 * (and slightly adapted).
 *
 * @author Chris de Vreeze
 */
public class XmlEqualityExample {

    // Example equality comparison, looking at actual namespace declarations rather than prefixes

    public static boolean areEqual(Element element1, Element element2) {
        return element1.name().equals(element2.name()) &&
                element1.attributes().equals(element2.attributes()) &&
                element1.children().size() == element2.children().size() &&
                IntStream.range(0, element1.children().size()).allMatch(i -> areEqual(element1.children().get(i), element2.children().get(i)));
    }

    public static boolean areEqual(Node node1, Node node2) {
        if (node1 instanceof Element elem1) {
            return (node2 instanceof Element elem2) && areEqual(elem1, elem2);
        } else if (node1 instanceof Text text1) {
            return (node2 instanceof Text text2) && text1.equals(text2);
        } else if (node1 instanceof Comment comment1) {
            return (node2 instanceof Comment comment2) && comment1.equals(comment2);
        } else if (node1 instanceof ProcessingInstruction pi1) {
            return (node2 instanceof ProcessingInstruction pi2) && pi1.equals(pi2);
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        Document doc1 = Objects.requireNonNull(getDocument1());
        Document doc2 = Objects.requireNonNull(getDocument2());

        boolean areEqual = areEqual(doc1.documentElement(), doc2.documentElement());

        System.out.printf("Both documents are equal: %b%n", areEqual);

        List<Element> allElems1 = Elements.queryApi().elementStream(doc1.documentElement()).toList();
        List<Element> allElems2 = Elements.queryApi().elementStream(doc2.documentElement()).toList();

        areEqual = allElems1.size() == allElems2.size() &&
                IntStream.range(0, allElems1.size()).allMatch(i -> areEqual(allElems1.get(i), allElems2.get(i)));

        System.out.printf("Both documents are equal (by comparing element streams): %b%n", areEqual);

        Set<QName> elementNames1 = Elements.queryApi().elementStream(doc1.documentElement())
                .map(Element::name)
                .collect(Collectors.toSet());
        Set<QName> elementNames2 = Elements.queryApi().elementStream(doc2.documentElement())
                .map(Element::name)
                .collect(Collectors.toSet());

        System.out.printf("Both documents have the same element names: %b%n", elementNames1.equals(elementNames2));
    }

    private static String divText() {
        return """
                You may not read, utter, interpret, or otherwise
                verbally process the words
                contained in this feed without express written
                permission from the authors.""";
    }

    private static Document getDocument1() {
        String atomNs = "http://www.w3.org/2005/Atom";
        String xhtmlNs = "http://www.w3.org/1999/xhtml";
        String myNs = "http://xmlportfolio.com/xmlguild-examples";

        NamespaceScope nsScope = new NamespaceScope(
                ImmutableMap.of(
                        "", atomNs,
                        "xhtml", xhtmlNs,
                        "my", myNs
                )
        );

        return new Document(
                Optional.empty(),
                ImmutableList.of(
                        new Element(
                                new QName(atomNs, "feed", ""),
                                ImmutableMap.of(),
                                nsScope,
                                ImmutableList.of(
                                        new Element(
                                                new QName(atomNs, "title", ""),
                                                ImmutableMap.of(),
                                                nsScope,
                                                ImmutableList.of(new Text("Example Feed", false))
                                        ),
                                        new Element(
                                                new QName(atomNs, "rights", ""),
                                                ImmutableMap.of(
                                                        new QName("type"), "xhtml",
                                                        new QName(myNs, "type", "my"), "silly"
                                                ),
                                                nsScope,
                                                ImmutableList.of(
                                                        new Element(
                                                                new QName(xhtmlNs, "div", "xhtml"),
                                                                ImmutableMap.of(),
                                                                nsScope,
                                                                ImmutableList.of(
                                                                        new Text(divText(), false)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static Document getDocument2() {
        String atomNs = "http://www.w3.org/2005/Atom";
        String xhtmlNs = "http://www.w3.org/1999/xhtml";
        String exampleNs = "http://xmlportfolio.com/xmlguild-examples";

        NamespaceScope nsScope1 = new NamespaceScope(
                ImmutableMap.of(
                        "", atomNs
                )
        );

        NamespaceScope nsScope2 = new NamespaceScope(
                ImmutableMap.of(
                        "", atomNs,
                        "example", exampleNs
                )
        );

        NamespaceScope nsScope3 = new NamespaceScope(
                ImmutableMap.of(
                        "", xhtmlNs,
                        "example", exampleNs
                )
        );

        return new Document(
                Optional.empty(),
                ImmutableList.of(
                        new Element(
                                new QName(atomNs, "feed", ""),
                                ImmutableMap.of(),
                                nsScope1,
                                ImmutableList.of(
                                        new Element(
                                                new QName(atomNs, "title", ""),
                                                ImmutableMap.of(),
                                                nsScope1,
                                                ImmutableList.of(new Text("Example Feed", false))
                                        ),
                                        new Element(
                                                new QName(atomNs, "rights", ""),
                                                ImmutableMap.of(
                                                        new QName("type"), "xhtml",
                                                        new QName(exampleNs, "type", "example"), "silly"
                                                ),
                                                nsScope2,
                                                ImmutableList.of(
                                                        new Element(
                                                                new QName(xhtmlNs, "div", ""),
                                                                ImmutableMap.of(),
                                                                nsScope3,
                                                                ImmutableList.of(
                                                                        new Text(divText(), false)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
