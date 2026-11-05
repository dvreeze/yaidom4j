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

import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeElementApi;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeNodeApi;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Green XML trees, unaware of parent nodes. These node trees are immutable, using "non-view unmodifiable collections".
 * <p>
 * The different node types are consistent with the XPath Data Model. Per node type the properties are
 * a small subset of those found in the XPath Data Model. For example, green trees do not know anything
 * about ancestor nodes, and typing information is absent.
 *
 * @author Chris de Vreeze
 */
public class GreenXmlTrees {

    private GreenXmlTrees() {
        // Non-instantiable
    }

    public sealed interface Node extends GreenTreeNodeApi<Node> permits CanBeChild, Document, Attribute, Namespace {

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

        // TODO

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
    }

    public record Attribute(QName name, String value) implements Node {
        // TODO

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record Namespace(String prefix, String namespaceUri) implements Node {
        // TODO

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public sealed interface CanBeChild extends Node permits Element, Text, Comment, ProcessingInstruction {
    }

    public record Text(String value) implements CanBeChild {
        // TODO

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record Comment(String value) implements CanBeChild {
        // TODO

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record ProcessingInstruction(String target, String data) implements CanBeChild {
        // TODO

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }
    }

    public record Element(
            QName name,
            Map<QName, String> attributes,
            NamespaceScope namespaceScope,
            List<CanBeChild> children
    ) implements CanBeChild, GreenTreeElementApi<Element> {

        public Element {
            // We want immutable collections of attributes and child nodes
            attributes = Map.copyOf(attributes);
            children = List.copyOf(children);

            Objects.requireNonNull(name);
            Objects.requireNonNull(namespaceScope);

            PreconditionUtil.checkArgument(
                    namespaceScope.allowsElementName(name),
                    String.format("Name '%s' not allowed by NamespaceScope %s", name, namespaceScope)
            );

            PreconditionUtil.checkArgument(attributes.keySet().stream().allMatch(namespaceScope::allowsAttributeName));
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

        public List<Namespace> namespaceNodes() {
            // TODO Add built-in namespaces?
            return List.copyOf(
                    namespaceScope().inScopeNamespaces()
                            .entrySet()
                            .stream()
                            .map(kv -> new Namespace(kv.getKey(), kv.getValue()))
                            .toList()
            );
        }

        public List<Attribute> attributeNodes() {
            return List.copyOf(
                    attributes.entrySet()
                            .stream()
                            .map(kv -> new Attribute(kv.getKey(), kv.getValue()))
                            .toList()
            );
        }
    }
}
