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

package eu.cdevreeze.yaidom4j.dom.ancestryaware;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * "Ancestry-aware" immutable XML nodes.
 *
 * @author Chris de Vreeze
 */
public class AncestryAwareNodes {

    private AncestryAwareNodes() {
    }

    /**
     * Any XML node, whether an element, text node, comment or processing instruction.
     *
     * @author Chris de Vreeze
     */
    public sealed interface Node permits CanBeDocumentChild, Text {

        boolean isElement();

        eu.cdevreeze.yaidom4j.dom.immutabledom.Node underlyingNode();
    }

    /**
     * Any XML node that can be an immediate document child, whether an element, processing instruction or comment.
     *
     * @author Chris de Vreeze
     */
    public sealed interface CanBeDocumentChild extends Node permits ElementTree.Element, Comment, ProcessingInstruction {

        @Override
        eu.cdevreeze.yaidom4j.dom.immutabledom.CanBeDocumentChild underlyingNode();
    }

    /**
     * Element tree.
     *
     * @author Chris de Vreeze
     */
    public static final class ElementTree {

        private static final QName XML_BASE_QNAME = new QName(XMLConstants.XML_NS_URI, "base");

        private final Optional<URI> docUriOption;
        private final ImmutableMap<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap;

        private ElementTree(
                Optional<URI> docUriOption,
                ImmutableMap<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap
        ) {
            this.docUriOption = docUriOption;
            this.elementMap = Objects.requireNonNull(elementMap);
        }

        public ElementTree.Element rootElement() {
            return new ElementTree.Element(docUriOption, ImmutableList.of());
        }

        public final class Element implements CanBeDocumentChild, AncestryAwareElementApi<ElementTree.Element> {

            private final Optional<URI> docUriOption;
            private final ImmutableList<Integer> navigationPath;

            private Element(Optional<URI> docUriOption, ImmutableList<Integer> navigationPath) {
                this.docUriOption = docUriOption;
                this.navigationPath = Objects.requireNonNull(navigationPath);
            }

            public ImmutableList<Integer> navigationPath() {
                return navigationPath;
            }

            public ElementTree containingElementTree() {
                return ElementTree.this;
            }

            @Override
            public boolean isElement() {
                return true;
            }

            @Override
            public eu.cdevreeze.yaidom4j.dom.immutabledom.Element underlyingNode() {
                return underlyingElement();
            }

            public eu.cdevreeze.yaidom4j.dom.immutabledom.Element underlyingElement() {
                return Objects.requireNonNull(elementMap.get(navigationPath));
            }

            public QName name() {
                return underlyingElement().name();
            }

            @Override
            public Optional<String> attributeOption(QName attrName) {
                Objects.requireNonNull(attrName);

                return underlyingElement().attributeOption(attrName);
            }

            @Override
            public String attribute(QName attrName) {
                Objects.requireNonNull(attrName);

                return underlyingElement().attribute(attrName);
            }

            @Override
            public String text() {
                return underlyingElement().text();
            }

            @Override
            public Optional<NamespaceScope> namespaceScopeOption() {
                return underlyingElement().namespaceScopeOption();
            }

            @Override
            public Optional<URI> docUriOption() {
                return docUriOption;
            }

            @Override
            public Optional<URI> baseUriOption() {
                Optional<ElementTree.Element> currentElementOption = Optional.of(this);
                List<URI> xmlBaseUris = new ArrayList<>();

                while (currentElementOption.isPresent()) {
                    ElementTree.Element currentElement = currentElementOption.get();
                    currentElement.attributeOption(XML_BASE_QNAME).ifPresent(u -> xmlBaseUris.add(URI.create(u)));
                    currentElementOption = currentElement.parentElementOption();
                }

                Collections.reverse(xmlBaseUris); // Java 21 offers the more functional "reversed" method

                Optional<URI> uriOption = docUriOption;

                for (URI u : xmlBaseUris) {
                    // See http://stackoverflow.com/questions/22203111/is-javas-uri-resolve-incompatible-with-rfc-3986-when-the-relative-uri-contains
                    uriOption = Optional.of(uriOption.map(b -> b.resolve(u)).orElse(u));
                }

                return uriOption;
            }

            @Override
            public NamespaceScope namespaceScope() {
                return namespaceScopeOption().orElseThrow();
            }

            @Override
            public boolean equals(Object other) {
                if (other instanceof ElementTree.Element otherElement) {
                    return this.navigationPath.equals(otherElement.navigationPath) &&
                            this.containingElementTree().equals(otherElement.containingElementTree());
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return List.of(navigationPath, containingElementTree()).hashCode();
            }

            @Override
            public QName elementName() {
                return name();
            }

            @Override
            public ImmutableMap<QName, String> attributes() {
                return underlyingElement().attributes();
            }

            @Override
            public Stream<ElementTree.Element> elementStream() {
                return descendantElementOrSelfStream();
            }

            @Override
            public Stream<ElementTree.Element> elementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return descendantElementOrSelfStream(predicate);
            }

            @Override
            public Stream<ElementTree.Element> topmostElementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return topmostDescendantElementOrSelfStream(predicate);
            }

            @Override
            public Optional<ElementTree.Element> parentElementOption() {
                return parentPathOption(navigationPath()).map(e -> new ElementTree.Element(docUriOption, e));
            }

            @Override
            public Stream<ElementTree.Element> ancestorElementOrSelfStream() {
                // Recursive
                return Stream.concat(
                        Stream.of(this),
                        parentElementOption().stream().flatMap(ElementTree.Element::ancestorElementOrSelfStream)
                );
            }

            @Override
            public Stream<ElementTree.Element> ancestorElementOrSelfStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return ancestorElementOrSelfStream().filter(predicate);
            }

            @Override
            public Stream<ElementTree.Element> ancestorElementStream() {
                return parentElementOption().stream().flatMap(ElementTree.Element::ancestorElementOrSelfStream);
            }

            @Override
            public Stream<ElementTree.Element> ancestorElementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return ancestorElementStream().filter(predicate);
            }

            public Stream<Node> childNodeStream() {
                int elementIdx = 0;
                List<Node> children = new ArrayList<>();

                for (var underlyingChildNode : underlyingElement().children()) {
                    if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Element) {
                        children.add(new ElementTree.Element(docUriOption, addToPath(elementIdx, navigationPath)));
                        elementIdx += 1;
                    } else if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Text t) {
                        children.add(new Text(t.value(), t.isCData()));
                    } else if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Comment c) {
                        children.add(new Comment(c.value()));
                    } else if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.ProcessingInstruction pi) {
                        children.add(new ProcessingInstruction(pi.target(), pi.data()));
                    }
                }

                return children.stream();
            }

            @Override
            public Stream<ElementTree.Element> childElementStream() {
                var navigationPath = navigationPath();

                return IntStream.range(0, (int) underlyingElement().childElementStream().count())
                        .mapToObj(i -> addToPath(i, navigationPath))
                        .map(e -> new ElementTree.Element(docUriOption, e));
            }

            @Override
            public Stream<ElementTree.Element> childElementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return childElementStream().filter(predicate);
            }

            @Override
            public Stream<ElementTree.Element> descendantElementOrSelfStream() {
                // Recursive
                return Stream.concat(
                        Stream.of(this),
                        childElementStream().flatMap(ElementTree.Element::descendantElementOrSelfStream)
                );
            }

            @Override
            public Stream<ElementTree.Element> descendantElementOrSelfStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return descendantElementOrSelfStream().filter(predicate);
            }

            @Override
            public Stream<ElementTree.Element> descendantElementStream() {
                return childElementStream().flatMap(ElementTree.Element::descendantElementOrSelfStream);
            }

            @Override
            public Stream<ElementTree.Element> descendantElementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return descendantElementStream().filter(predicate);
            }

            @Override
            public Stream<ElementTree.Element> topmostDescendantElementOrSelfStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                // Recursive
                if (predicate.test(this)) {
                    return Stream.of(this);
                } else {
                    return childElementStream().flatMap(e -> e.topmostDescendantElementOrSelfStream(predicate));
                }
            }

            @Override
            public Stream<ElementTree.Element> topmostDescendantElementStream(Predicate<? super ElementTree.Element> predicate) {
                Objects.requireNonNull(predicate);

                return childElementStream().flatMap(e -> e.topmostDescendantElementOrSelfStream(predicate));
            }
        }

        public static ElementTree create(
                Optional<URI> docUriOption,
                eu.cdevreeze.yaidom4j.dom.immutabledom.Element underlyingRootElement
        ) {
            ImmutableList<Integer> navigationPath = ImmutableList.of();
            Map<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap = new HashMap<>();
            buildElementCache(docUriOption, navigationPath, underlyingRootElement, elementMap);
            return new ElementTree(docUriOption, ImmutableMap.copyOf(elementMap));
        }

        private static void buildElementCache(
                Optional<URI> docUriOption,
                ImmutableList<Integer> elementNavigationPath,
                eu.cdevreeze.yaidom4j.dom.immutabledom.Element element,
                Map<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap
        ) {
            elementMap.put(elementNavigationPath, element);

            int idx = 0;
            for (var childElement : element.childElementStream().toList()) {
                // Recursion
                buildElementCache(
                        docUriOption,
                        addToPath(idx, elementNavigationPath),
                        childElement,
                        elementMap
                );
                idx += 1;
            }
        }

        private static ImmutableList<Integer> addToPath(int nextIndex, ImmutableList<Integer> path) {
            return ImmutableList.<Integer>builder().addAll(path).add(nextIndex).build();
        }

        private static Optional<ImmutableList<Integer>> parentPathOption(ImmutableList<Integer> path) {
            return Optional.of(path).flatMap(p ->
                    (p.isEmpty()) ? Optional.empty() : Optional.of(p.subList(0, p.size() - 1)));
        }
    }

    /**
     * XML text node.
     *
     * @author Chris de Vreeze
     */
    public record Text(String value, boolean isCData) implements Node {

        public Text {
            Objects.requireNonNull(value);
        }

        @Override
        public eu.cdevreeze.yaidom4j.dom.immutabledom.Text underlyingNode() {
            return new eu.cdevreeze.yaidom4j.dom.immutabledom.Text(value, isCData);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }

    /**
     * XML comment node.
     *
     * @author Chris de Vreeze
     */
    public record Comment(String value) implements CanBeDocumentChild {

        public Comment {
            Objects.requireNonNull(value);
        }

        @Override
        public eu.cdevreeze.yaidom4j.dom.immutabledom.Comment underlyingNode() {
            return new eu.cdevreeze.yaidom4j.dom.immutabledom.Comment(value);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }

    /**
     * XML processing instruction node.
     *
     * @author Chris de Vreeze
     */
    public record ProcessingInstruction(String target, String data) implements CanBeDocumentChild {

        public ProcessingInstruction {
            Objects.requireNonNull(target);
            Objects.requireNonNull(data);
        }

        @Override
        public eu.cdevreeze.yaidom4j.dom.immutabledom.ProcessingInstruction underlyingNode() {
            return new eu.cdevreeze.yaidom4j.dom.immutabledom.ProcessingInstruction(target, data);
        }

        @Override
        public boolean isElement() {
            return false;
        }
    }
}