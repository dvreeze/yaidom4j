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

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Several element utilities, such as an element query API.
 *
 * @author Chris de Vreeze
 */
public class Elements {

    private Elements() {
    }

    private static final QueryApi queryApi = new QueryApi();

    public static QueryApi queryApi() {
        return queryApi;
    }

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

    /**
     * Returns an adapted copy of the parameter element where the children are replaced by the results of applying
     * the given function.
     */
    public static Element transformChildren(Element element, Function<Node, List<Node>> f) {
        return new Element(
                element.name(),
                element.attributes(),
                element.namespaceScope(),
                element.children().stream()
                        .flatMap(ch -> f.apply(ch).stream())
                        .collect(ImmutableList.toImmutableList())
        );
    }

    /**
     * Returns an adapted copy of the parameter element where the child elements are replaced by the results of applying
     * the given function, and the non-element children are left as-is.
     */
    public static Element transformChildElements(Element element, Function<Element, List<Node>> f) {
        return transformChildren(
                element,
                ch -> {
                    if (ch instanceof Element e) {
                        return f.apply(e);
                    } else {
                        return List.of(ch);
                    }
                }
        );
    }

    /**
     * Returns the "same element", except that there are no prefixed namespace un-declarations
     * (which are not allowed in XML 1.0), and the given parent scope (without its default
     * namespace, if any) is taken as the "starting point".
     */
    public static Element notUndeclaringPrefixes(Element element, NamespaceScope parentScope) {
        NamespaceScope newScope =
                parentScope.withoutDefaultNamespace().resolve(element.namespaceScope().inScopeNamespaces());

        Preconditions.checkArgument(element.namespaceScope().subScopeOf(newScope));
        Preconditions.checkArgument(element.namespaceScope().defaultNamespaceOption().equals(newScope.defaultNamespaceOption()));

        // Recursion
        return transformChildElements(
                new Element(
                        element.name(),
                        element.attributes(),
                        newScope,
                        element.children()
                ),
                e -> List.of(notUndeclaringPrefixes(e, newScope))
        );
    }

    /**
     * Removes all inter-element whitespace, through a naive algorithm. That is, recursively throughout
     * the element tree the following transformation is done: if an element has at least one element
     * node as child and if all text children are only whitespace, the text children are removed.
     * <p>
     * The "xml:space" attribute is not respected, if any.
     * <p>
     * This method is useful to remove inter-element whitespace if parsing of the document was done
     * without any validation against DTD or XSD, thus preventing the parser from recognizing "ignorable
     * whitespace".
     */
    public static Element removeInterElementWhitespace(Element element) {
        ImmutableList<Node> children = element.children();

        // Naive implementation
        boolean hasElementChild = children.stream().anyMatch(n -> n instanceof Element);
        boolean hasOnlyInterElementWhitespaceTextChildren = hasElementChild &&
                children.stream().allMatch(ch -> !(ch instanceof Text t) || t.value().isBlank());

        List<Node> filteredChildren =
                hasOnlyInterElementWhitespaceTextChildren ?
                        children.stream().filter(ch -> !(ch instanceof Text)).toList() :
                        children;

        return new Element(
                element.name(),
                element.attributes(),
                element.namespaceScope(),
                filteredChildren.stream()
                        .map(ch -> {
                            if (ch instanceof Element che) {
                                // Recursion
                                return removeInterElementWhitespace(che);
                            } else {
                                return ch;
                            }
                        })
                        .collect(ImmutableList.toImmutableList())
        );
    }
}
