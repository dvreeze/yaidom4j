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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.queryapi.ElementQueryApi;
import eu.cdevreeze.yaidom4j.queryapi.internal.ElementApi;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * XML element node.
 *
 * @author Chris de Vreeze
 */
public record Element(
        QName name,
        ImmutableMap<QName, String> attributes,
        NamespaceScope namespaceScope,
        ImmutableList<Node> children
) implements CanBeDocumentChild, ElementApi<Element> {

    public Element {
        Objects.requireNonNull(name);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(namespaceScope);
        Objects.requireNonNull(children);

        Preconditions.checkArgument(
                name.getPrefix().isEmpty() ||
                        name.getPrefix().equals(XMLConstants.XML_NS_PREFIX) ||
                        namespaceScope.inScopeNamespaces().containsKey(name.getPrefix())
        );

        Preconditions.checkArgument(
                attributes.keySet().stream()
                        .allMatch(attrName ->
                                attrName.getPrefix().isEmpty() ||
                                        attrName.getPrefix().equals(XMLConstants.XML_NS_PREFIX) ||
                                        namespaceScope.inScopeNamespaces().containsKey(attrName.getPrefix())));

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

    /**
     * Functionally updates this element by replacing the collection of child nodes.
     * <p>
     * When calling this function, take care to align the in-scope namespaces in order to
     * prevent invalid XML 1.0 from being created. After all, prefixed namespace undeclarations
     * are not allowed in XML 1.0.
     */
    public Element withChildren(ImmutableList<Node> newChildren) {
        return new Element(
                name(),
                attributes(),
                namespaceScope(),
                newChildren
        );
    }

    /**
     * Calls method "withChildren" to functionally add one child node at the end
     */
    public Element plusChild(Node newChild) {
        return withChildren(
                ImmutableList.<Node>builder().addAll(children).add(newChild).build()
        );
    }

    /**
     * Functionally updates this element by replacing the collection of attributes.
     * <p>
     * For prefixed attributes, take care to use only prefixes that are in scope.
     */
    public Element withAttributes(ImmutableMap<QName, String> newAttributes) {
        return new Element(
                name(),
                newAttributes,
                namespaceScope(),
                children()
        );
    }

    /**
     * Calls method "withAttributes" to functionally add or update one attribute
     */
    public Element plusAttribute(QName attrName, String attrValue) {
        return withAttributes(
                ImmutableMap.<QName, String>builder()
                        .putAll(attributes)
                        .put(attrName, attrValue)
                        .buildKeepingLast()
        );
    }

    /**
     * Element query API implementation for immutable DOM elements.
     */
    public static final class QueryApi implements ElementQueryApi<Element> {

        @Override
        public QName elementName(Element element) {
            Objects.requireNonNull(element);

            return element.elementName();
        }

        @Override
        public ImmutableMap<QName, String> attributes(Element element) {
            Objects.requireNonNull(element);

            return element.attributes();
        }

        @Override
        public Optional<String> attributeOption(Element element, QName attrName) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(attrName);

            return element.attributeOption(attrName);
        }

        @Override
        public String attribute(Element element, QName attrName) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(attrName);

            return element.attribute(attrName);
        }

        @Override
        public String text(Element element) {
            Objects.requireNonNull(element);

            return element.text();
        }

        @Override
        public Stream<Node> childNodeStream(Element element) {
            Objects.requireNonNull(element);

            return element.childNodeStream();
        }

        @Override
        public Stream<Element> childElementStream(Element element) {
            Objects.requireNonNull(element);

            return element.childElementStream();
        }

        @Override
        public Stream<Element> childElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return element.childElementStream(predicate);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Element element) {
            Objects.requireNonNull(element);

            return element.descendantElementOrSelfStream();
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return element.descendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> descendantElementStream(Element element) {
            Objects.requireNonNull(element);

            return element.descendantElementStream();
        }

        @Override
        public Stream<Element> descendantElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return element.descendantElementStream(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return element.topmostDescendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return element.topmostDescendantElementStream(predicate);
        }
    }
}
