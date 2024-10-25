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

package eu.cdevreeze.yaidom4j.dom.clark;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;
import eu.cdevreeze.yaidom4j.transformationapi.TransformableElementApi;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
public class ClarkNodes {

    private ClarkNodes() {
    }

    public sealed interface Node permits Element, Text, Comment, ProcessingInstruction {

        boolean isElement();
    }

    public record Element(
            QName name,
            ImmutableMap<QName, String> attributes,
            ImmutableList<Node> children
    ) implements Node, ElementApi<Element>, TransformableElementApi<Element, Node> {

        public Element {
            Objects.requireNonNull(name);
            Objects.requireNonNull(attributes);
            Objects.requireNonNull(children);

            Preconditions.checkArgument(
                    attributes.keySet().stream()
                            .allMatch(attrName -> attrName.getNamespaceURI().isEmpty() || !attrName.getPrefix().isEmpty())
            );
        }

        @Override
        public boolean isElement() {
            return true;
        }

        @Override
        public QName elementName() {
            return name();
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
            return children().stream()
                    .filter(n -> n instanceof Text)
                    .map(n -> (Text) n)
                    .map(Text::value)
                    .collect(Collectors.joining());
        }

        @Override
        public Stream<Node> childNodeStream() {
            return children().stream();
        }

        @Override
        public Stream<Element> elementStream() {
            return descendantElementOrSelfStream();
        }

        @Override
        public Stream<Element> elementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> topmostElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return topmostDescendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> childElementStream() {
            return childNodeStream().filter(Node::isElement).map(n -> (Element) n);
        }

        @Override
        public Stream<Element> childElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream() {
            Stream<Element> selfStream = Stream.of(this);
            // Recursion
            Stream<Element> descendantElemStream =
                    childElementStream().flatMap(Element::descendantElementOrSelfStream);
            return Stream.concat(selfStream, descendantElemStream);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementOrSelfStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementStream() {
            return childElementStream().flatMap(Element::descendantElementOrSelfStream);
        }

        @Override
        public Stream<Element> descendantElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantElementOrSelfStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            if (predicate.test(this)) {
                return Stream.of(this);
            } else {
                // Recursion
                return childElementStream().flatMap(che -> che.topmostDescendantElementOrSelfStream(predicate));
            }
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().flatMap(che -> che.topmostDescendantElementOrSelfStream(predicate));
        }

        // Functional updates

        @Override
        public Element withChildren(ImmutableList<Node> newChildren) {
            return new Element(
                    name(),
                    attributes(),
                    newChildren
            );
        }

        @Override
        public Element plusChild(Node newChild) {
            return withChildren(
                    ImmutableList.<Node>builder().addAll(children).add(newChild).build()
            );
        }

        @Override
        public Element withAttributes(ImmutableMap<QName, String> newAttributes) {
            return new Element(
                    name(),
                    newAttributes,
                    children()
            );
        }

        @Override
        public Element plusAttribute(QName attrName, String attrValue) {
            return withAttributes(
                    ImmutableMap.<QName, String>builder()
                            .putAll(attributes)
                            .put(attrName, attrValue)
                            .buildKeepingLast()
            );
        }

        @Override
        public Element transformChildrenToNodeLists(Function<Node, List<Node>> f) {
            return withChildren(
                    children().stream()
                            .flatMap(ch -> f.apply(ch).stream())
                            .collect(ImmutableList.toImmutableList())
            );
        }

        @Override
        public Element transformChildElementsToNodeLists(Function<Element, List<Node>> f) {
            return transformChildrenToNodeLists(
                    ch -> {
                        if (ch instanceof Element e) {
                            return f.apply(e);
                        } else {
                            return List.of(ch);
                        }
                    }
            );
        }

        @Override
        public Element transformChildElements(UnaryOperator<Element> f) {
            return transformChildElementsToNodeLists(e -> List.of(f.apply(e)));
        }

        @Override
        public Element transformDescendantElementsOrSelf(UnaryOperator<Element> f) {
            // Recursion
            Element descendantResult =
                    transformChildElements(che -> che.transformDescendantElementsOrSelf(f));
            return f.apply(descendantResult);
        }

        @Override
        public Element transformDescendantElements(UnaryOperator<Element> f) {
            return transformChildElements(che -> che.transformDescendantElementsOrSelf(f));
        }

        @Override
        public Element removeInterElementWhitespace() {
            ImmutableList<Node> children = children();

            // Naive implementation
            boolean hasElementChild = children.stream().anyMatch(n -> n instanceof Element);
            boolean hasOnlyInterElementWhitespaceTextChildren = hasElementChild &&
                    children.stream().allMatch(ch -> !(ch instanceof Text t) || t.value().isBlank());

            List<Node> filteredChildren =
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
                            .collect(ImmutableList.toImmutableList())
            );
        }
    }

    public record Text(String value) implements Node {

        public Text {
            Objects.requireNonNull(value);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }

    public record Comment(String value) implements Node {

        public Comment {
            Objects.requireNonNull(value);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }

    public record ProcessingInstruction(String target, String data) implements Node {

        public ProcessingInstruction {
            Objects.requireNonNull(target);
            Objects.requireNonNull(data);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }
}
