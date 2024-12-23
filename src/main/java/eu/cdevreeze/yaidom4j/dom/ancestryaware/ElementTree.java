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
 * Element tree.
 *
 * @author Chris de Vreeze
 * @deprecated Use {@link AncestryAwareNodes.ElementTree} instead.
 */
@Deprecated(forRemoval = true, since = "0.10.0")
public final class ElementTree {

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

    public Element rootElement() {
        return new Element(docUriOption, ImmutableList.of());
    }

    public final class Element implements CanBeDocumentChild, AncestryAwareElementApi<Element> {

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
            Optional<Element> currentElementOption = Optional.of(this);
            List<URI> xmlBaseUris = new ArrayList<>();

            while (currentElementOption.isPresent()) {
                Element currentElement = currentElementOption.get();
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
            if (other instanceof Element otherElement) {
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
        public Stream<Element> elementStream() {
            return descendantElementOrSelfStream();
        }

        @Override
        public Stream<Element> elementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> topmostElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return topmostDescendantElementOrSelfStream(predicate);
        }

        @Override
        public Optional<Element> parentElementOption() {
            return parentPathOption(navigationPath()).map(e -> new Element(docUriOption, e));
        }

        @Override
        public Stream<Element> ancestorElementOrSelfStream() {
            // Recursive
            return Stream.concat(
                    Stream.of(this),
                    parentElementOption().stream().flatMap(Element::ancestorElementOrSelfStream)
            );
        }

        @Override
        public Stream<Element> ancestorElementOrSelfStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return ancestorElementOrSelfStream().filter(predicate);
        }

        @Override
        public Stream<Element> ancestorElementStream() {
            return parentElementOption().stream().flatMap(Element::ancestorElementOrSelfStream);
        }

        @Override
        public Stream<Element> ancestorElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return ancestorElementStream().filter(predicate);
        }

        public Stream<Node> childNodeStream() {
            int elementIdx = 0;
            List<Node> children = new ArrayList<>();

            for (var underlyingChildNode : underlyingElement().children()) {
                if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Element) {
                    children.add(new Element(docUriOption, addToPath(elementIdx, navigationPath)));
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
        public Stream<Element> childElementStream() {
            var navigationPath = navigationPath();

            return IntStream.range(0, (int) underlyingElement().childElementStream().count())
                    .mapToObj(i -> addToPath(i, navigationPath))
                    .map(e -> new Element(docUriOption, e));
        }

        @Override
        public Stream<Element> childElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream() {
            // Recursive
            return Stream.concat(
                    Stream.of(this),
                    childElementStream().flatMap(Element::descendantElementOrSelfStream)
            );
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementOrSelfStream().filter(predicate);
        }

        @Override
        public Stream<Element> descendantElementStream() {
            return childElementStream().flatMap(Element::descendantElementOrSelfStream);
        }

        @Override
        public Stream<Element> descendantElementStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            return descendantElementStream().filter(predicate);
        }

        @Override
        public Stream<Element> topmostDescendantElementOrSelfStream(Predicate<? super Element> predicate) {
            Objects.requireNonNull(predicate);

            // Recursive
            if (predicate.test(this)) {
                return Stream.of(this);
            } else {
                return childElementStream().flatMap(e -> e.topmostDescendantElementOrSelfStream(predicate));
            }
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<? super Element> predicate) {
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
