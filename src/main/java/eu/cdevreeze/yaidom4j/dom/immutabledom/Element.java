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
        return Optional.ofNullable(attributes().get(attrName));
    }

    @Override
    public String attribute(QName attrName) {
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

    private static final QueryApi queryApi = new QueryApi();

    public static QueryApi queryApi() {
        return queryApi;
    }

    // Convenience methods delegating to the element query API

    @Override
    public Stream<Node> childNodeStream() {
        return queryApi().childNodeStream(this);
    }

    @Override
    public Stream<Element> elementStream() {
        return queryApi().elementStream(this);
    }

    @Override
    public Stream<Element> elementStream(Predicate<Element> predicate) {
        return queryApi().elementStream(this, predicate);
    }

    @Override
    public Stream<Element> topmostElementStream(Predicate<Element> predicate) {
        return queryApi().topmostElementStream(this, predicate);
    }

    @Override
    public Stream<Element> childElementStream() {
        return queryApi().childElementStream(this);
    }

    @Override
    public Stream<Element> childElementStream(Predicate<Element> predicate) {
        return queryApi().childElementStream(this, predicate);
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream() {
        return queryApi().descendantElementOrSelfStream(this);
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream(Predicate<Element> predicate) {
        return queryApi().descendantElementOrSelfStream(this, predicate);
    }

    @Override
    public Stream<Element> descendantElementStream() {
        return queryApi().descendantElementStream(this);
    }

    @Override
    public Stream<Element> descendantElementStream(Predicate<Element> predicate) {
        return queryApi().descendantElementStream(this, predicate);
    }

    @Override
    public Stream<Element> topmostDescendantElementOrSelfStream(Predicate<Element> predicate) {
        return queryApi().topmostDescendantElementOrSelfStream(this, predicate);
    }

    @Override
    public Stream<Element> topmostDescendantElementStream(Predicate<Element> predicate) {
        return queryApi().topmostDescendantElementStream(this, predicate);
    }

    /**
     * Element query API implementation for immutable DOM elements.
     * <p>
     * Instead of using this API directly, consider using their friendly counterparts as Element
     * instance methods (which delegate to this element query API class).
     */
    public static final class QueryApi implements ElementQueryApi<Element> {

        @Override
        public QName elementName(Element element) {
            Objects.requireNonNull(element);

            return element.name();
        }

        @Override
        public ImmutableMap<QName, String> attributes(Element element) {
            Objects.requireNonNull(element);

            return element.attributes();
        }

        @Override
        public Stream<Node> childNodeStream(Element element) {
            Objects.requireNonNull(element);

            return element.children().stream();
        }

        @Override
        public Stream<Element> childElementStream(Element element) {
            Objects.requireNonNull(element);

            return childNodeStream(element).filter(Node::isElement).map(n -> (Element) n);
        }

        @Override
        public Stream<Element> childElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return childElementStream(element).filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Element element) {
            Objects.requireNonNull(element);

            Stream<Element> selfStream = Stream.of(element);
            // Recursion
            Stream<Element> descendantElemStream =
                    childElementStream(element).flatMap(this::descendantElementOrSelfStream);
            return Stream.concat(selfStream, descendantElemStream);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return descendantElementOrSelfStream(element).filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementStream(Element element) {
            Objects.requireNonNull(element);

            return childElementStream(element).flatMap(this::descendantElementOrSelfStream);
        }

        @Override
        public Stream<Element> descendantElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return descendantElementStream(element).filter(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            if (predicate.test(element)) {
                return Stream.of(element);
            } else {
                // Recursion
                return childElementStream(element).flatMap(che -> topmostDescendantElementOrSelfStream(che, predicate));
            }
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Element element, Predicate<Element> predicate) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(predicate);

            return childElementStream(element).flatMap(che -> topmostDescendantElementOrSelfStream(che, predicate));
        }
    }
}
