/*
 * Copyright 2024-2026 Chris de Vreeze
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

package eu.cdevreeze.yaidom4j.experimental.dom;

import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeElementApi;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeNodeApi;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * "Minimal" XML nodes. That is, elements only have a name, attributes and children, and no
 * in-scope namespaces. These nodes are good candidates for XML equality comparisons, where
 * namespaces prefixes are irrelevant but the actual namespaces of elements and attributes
 * are quite relevant. These nodes are inspired by James Clark and his article on
 * <a href="http://www.jclark.com/xml/xmlns.htm">XML Namespaces</a>.
 * <p>
 * Note that it is possible to create a "Clark element" that could not possibly correspond to
 * any "immutable DOM" element that represents valid XML 1.0, due to the latter having prefixed namespace
 * un-declarations, which is not allowed in XML 1.0.
 * <p>
 * If an "immutable DOM" element has namespaces used in element text content or attribute values,
 * and that element is converted to a "Clark element" for easy comparison, consider turning that
 * content with namespace prefixes into "expanded QNames" before converting to a "Clark element".
 *
 * @author Chris de Vreeze
 */
public class GreenClarkTrees {

    private GreenClarkTrees() {
        // Non-instantiable
    }

    public sealed interface Node extends GreenTreeNodeApi<Node> permits CanBeChild, Document, Attribute {

        @Override
        default Stream<Node> childStream(Predicate<? super Node> predicate) {
            Objects.requireNonNull(predicate);

            return childStream().filter(predicate);
        }

        @Override
        default Stream<Node> descendantOrSelfStream() {
            Stream<Node> selfStream = Stream.of(this);
            // Recursion
            Stream<Node> descendantStream =
                    childStream().flatMap(Node::descendantOrSelfStream);
            return Stream.concat(selfStream, descendantStream);
        }

        @Override
        default Stream<Node> descendantOrSelfStream(Predicate<? super Node> predicate) {
            Objects.requireNonNull(predicate);

            return descendantOrSelfStream().filter(predicate);
        }

        @Override
        default Stream<Node> descendantStream() {
            return childStream().flatMap(Node::descendantOrSelfStream);
        }

        @Override
        default Stream<Node> descendantStream(Predicate<? super Node> predicate) {
            Objects.requireNonNull(predicate);

            return descendantStream().filter(predicate);
        }

        @Override
        default Stream<Node> topmostDescendantOrSelfStream(Predicate<? super Node> predicate) {
            Objects.requireNonNull(predicate);

            if (predicate.test(this)) {
                return Stream.of(this);
            } else {
                // Recursion
                return childStream().flatMap(che -> che.topmostDescendantOrSelfStream(predicate));
            }
        }

        @Override
        default Stream<Node> topmostDescendantStream(Predicate<? super Node> predicate) {
            Objects.requireNonNull(predicate);

            return childStream().flatMap(che -> che.topmostDescendantOrSelfStream(predicate));
        }
    }

    public record Document(Optional<URI> uriOption, List<CanBeChild> children) implements Node {

        public Document {
            children = List.copyOf(children);

            Objects.requireNonNull(uriOption);

            PreconditionUtil.checkArgument(
                    children.stream().filter(n -> n instanceof Element).count() == 1
            );
        }

        @Override
        public Stream<Node> childStream() {
            return children.stream().map(n -> n);
        }

        public Document withUri(URI uri) {
            return new Document(Optional.of(uri), children);
        }

        public Element documentElement() {
            return childStream().filter(n -> n instanceof Element)
                    .map(n -> (Element) n)
                    .findFirst()
                    .orElseThrow();
        }

        /**
         * Removes all inter-element whitespace, through the equally named method on the document element.
         */
        public Document removeInterElementWhitespace() {
            return new Document(
                    uriOption(),
                    children().stream()
                            .map(ch -> {
                                if (ch instanceof Element e) {
                                    return e.removeInterElementWhitespace();
                                } else {
                                    return ch;
                                }
                            })
                            .toList()
            );
        }
    }

    public record Attribute(QName name, String value) implements Node {

        public Attribute {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public sealed interface CanBeChild extends Node permits Element, Text, Comment, ProcessingInstruction {
    }

    public record Text(String value) implements CanBeChild {

        public Text {
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record Comment(String value) implements CanBeChild {

        public Comment {
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record ProcessingInstruction(String target, String data) implements CanBeChild {

        public ProcessingInstruction {
            Objects.requireNonNull(target);
            Objects.requireNonNull(data);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record Element(
            QName name,
            Map<QName, String> attributes,
            List<CanBeChild> children
    ) implements CanBeChild, GreenTreeElementApi<Element> {

        public Element {
            // We want immutable collections of attributes and child nodes
            attributes = Map.copyOf(attributes);
            children = List.copyOf(children);

            Objects.requireNonNull(name);

            PreconditionUtil.checkArgument(
                    attributes.keySet().stream()
                            .allMatch(attrName -> attrName.getNamespaceURI().isEmpty() || !attrName.getPrefix().isEmpty())
            );
        }

        @Override
        public QName elementName() {
            return name;
        }

        @Override
        public Optional<String> attributeOption(QName attrName) {
            Objects.requireNonNull(attrName);

            return Optional.ofNullable(attributes().get(attrName));
        }

        @Override
        public String attribute(QName attrName) {
            Objects.requireNonNull(attrName);

            return attributeOption(attrName).orElseThrow();
        }

        @Override
        public String text() {
            return childStream(n -> n instanceof Text)
                    .map(n -> (Text) n)
                    .map(Text::value)
                    .collect(Collectors.joining());
        }

        @Override
        public Stream<Element> childElementStream() {
            return childStream().filter(n -> n instanceof Element).map(n -> (Element) n);
        }

        @Override
        public Stream<Element> childElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantOrSelfElementStream() {
            Stream<Element> selfStream = Stream.of(this);
            // Recursion
            Stream<Element> descendantElemStream =
                    childElementStream().flatMap(Element::descendantOrSelfElementStream);
            return Stream.concat(selfStream, descendantElemStream);
        }

        @Override
        public Stream<Element> descendantOrSelfElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantOrSelfElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementStream() {
            return childElementStream().flatMap(Element::descendantOrSelfElementStream);
        }

        @Override
        public Stream<Element> descendantElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantOrSelfElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            if (predicate.test(this)) {
                return Stream.of(this);
            } else {
                // Recursion
                return childElementStream().flatMap(che -> che.topmostDescendantOrSelfElementStream(predicate));
            }
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().flatMap(che -> che.topmostDescendantOrSelfElementStream(predicate));
        }

        @Override
        public Stream<Node> childStream() {
            return children.stream().map(n -> n);
        }

        public List<Attribute> attributeNodes() {
            return List.copyOf(
                    attributes.entrySet()
                            .stream()
                            .map(kv -> new Attribute(kv.getKey(), kv.getValue()))
                            .toList()
            );
        }

        /**
         * Functionally updates this element by replacing the collection of child nodes.
         */
        public Element withChildren(List<CanBeChild> newChildren) {
            return new Element(
                    name(),
                    attributes(),
                    List.copyOf(newChildren)
            );
        }

        public Element withAttributes(Map<QName, String> newAttributes) {
            return new Element(
                    name(),
                    Map.copyOf(newAttributes),
                    children()
            );
        }

        public Element transformChildElements(UnaryOperator<Element> f) {
            return withChildren(
                    children.stream().map(ch ->
                            switch (ch) {
                                case Element e -> f.apply(e);
                                default -> ch;
                            }
                    ).toList()
            );
        }

        public Element transformDescendantOrSelfElements(UnaryOperator<Element> f) {
            // Recursion
            Element descendantResult =
                    transformChildElements(che -> che.transformDescendantOrSelfElements(f));
            return f.apply(descendantResult);
        }

        // Custom element transformations

        public Element removeInterElementWhitespace() {
            // Naive implementation
            boolean hasElementChild = children.stream().anyMatch(n -> n instanceof Element);
            boolean hasOnlyInterElementWhitespaceTextChildren = hasElementChild &&
                    children.stream().allMatch(ch -> !(ch instanceof Text(String value)) || value.isBlank());

            List<CanBeChild> filteredChildren =
                    hasOnlyInterElementWhitespaceTextChildren ?
                            children.stream().filter(ch -> !(ch instanceof Text)).toList() :
                            children;

            return withChildren(
                    filteredChildren.stream()
                            .map(ch -> {
                                if (ch instanceof Element che) {
                                    // Recursion
                                    return che.removeInterElementWhitespace();
                                } else {
                                    return ch;
                                }
                            })
                            .toList()
            );
        }
    }

    public static final class ElementPredicates {

        private ElementPredicates() {
            // Non-instantiable
        }

        public static Predicate<Element> hasName(QName name) {
            return e -> e.name().equals(name);
        }

        public static Predicate<Element> hasName(String namespace, String localName) {
            return e -> e.name().getNamespaceURI().equals(namespace) && e.name().getLocalPart().equals(localName);
        }

        public static Predicate<Element> hasName(String noNamespaceName) {
            return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
        }

        public static Predicate<Element> hasLocalName(String localName) {
            return e -> e.name().getLocalPart().equals(localName);
        }

        public static Predicate<Element> hasAttributeWithName(QName attrName) {
            return e -> e.attributeOption(attrName).isPresent();
        }

        public static Predicate<Element> hasAttributeWithName(String attrNamespace, String attrLocalName) {
            return hasAttributeWithName(new QName(attrNamespace, attrLocalName));
        }

        public static Predicate<Element> hasAttributeWithName(String attrNoNamespaceName) {
            return hasAttributeWithName(new QName(attrNoNamespaceName));
        }

        public static Predicate<Element> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate) {
            return e -> e.attributes().entrySet().stream()
                    .anyMatch(kv -> kv.getKey().equals(attrName) && attrValuePredicate.test(kv.getValue()));
        }

        public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
            return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValuePredicate);
        }

        public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
            return hasAttributeValue(new QName(attrNoNamespaceName), attrValuePredicate);
        }

        public static Predicate<Element> hasAttributeValue(QName attrName, String attrValue) {
            return hasAttributeValue(attrName, s -> s.equals(attrValue));
        }

        public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue) {
            return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValue);
        }

        public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, String attrValue) {
            return hasAttributeValue(new QName(attrNoNamespaceName), attrValue);
        }

        public static Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
            return e -> e.children().stream().allMatch(ch -> ch instanceof Text) &&
                    textPredicate.test(e.text());
        }

        public static Predicate<Element> hasOnlyText(String text) {
            return hasOnlyText(s -> s.equals(text));
        }

        public static Predicate<Element> hasOnlyTextAfterStripping(String text) {
            return hasOnlyText(s -> s.strip().equals(text));
        }
    }

    // Node creation API

    public static Document doc() {
        return new Document(Optional.empty(), List.of());
    }

    public static Document doc(URI docUri) {
        return new Document(Optional.of(docUri), List.of());
    }

    public static Element elem(QName name) {
        return new Element(
                name,
                Map.of(),
                List.of()
        );
    }

    public static Element elem(String noNamespaceName) {
        return elem(new QName(noNamespaceName));
    }

    public static Text text(String value) {
        return new Text(value);
    }

    public static Comment comment(String value) {
        return new Comment(value);
    }

    public static ProcessingInstruction pi(String target, String data) {
        return new ProcessingInstruction(target, data);
    }
}
