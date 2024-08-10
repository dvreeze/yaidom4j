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
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementQueryApi;
import eu.cdevreeze.yaidom4j.queryapi.internal.AncestryAwareElementApi;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Element tree.
 *
 * @author Chris de Vreeze
 */
public final class ElementTree implements AncestryAwareElementQueryApi<ElementTree.Element> {

    private final ImmutableMap<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap;

    private ElementTree(ImmutableMap<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap) {
        this.elementMap = Objects.requireNonNull(elementMap);
    }

    public Element rootElement() {
        return new Element(ImmutableList.of());
    }

    @Override
    public Optional<Element> parentElementOption(Element element) {
        Objects.requireNonNull(element);

        return element.parentElementOption();
    }

    @Override
    public Stream<Element> ancestorElementOrSelfStream(Element element) {
        Objects.requireNonNull(element);

        return element.ancestorElementOrSelfStream();
    }

    @Override
    public Stream<Element> ancestorElementOrSelfStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return element.ancestorElementOrSelfStream(predicate);
    }

    @Override
    public Stream<Element> ancestorElementStream(Element element) {
        Objects.requireNonNull(element);

        return element.ancestorElementStream();
    }

    @Override
    public Stream<Element> ancestorElementStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return element.ancestorElementStream(predicate);
    }

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
    public Stream<? super Element> childNodeStream(Element element) {
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

    public final class Element implements CanBeDocumentChild, AncestryAwareElementApi<Element> {

        private final ImmutableList<Integer> navigationPath;

        private Element(ImmutableList<Integer> navigationPath) {
            this.navigationPath = navigationPath;
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
        public boolean equals(Object other) {
            if (other instanceof Element otherElement) {
                return this.navigationPath.equals(otherElement.navigationPath);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return navigationPath.hashCode();
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
        public Optional<Element> parentElementOption() {
            return parentPathOption(navigationPath()).map(Element::new);
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
        public Stream<Element> ancestorElementOrSelfStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return ancestorElementOrSelfStream().filter(predicate);
        }

        @Override
        public Stream<Element> ancestorElementStream() {
            return parentElementOption().stream().flatMap(Element::ancestorElementOrSelfStream);
        }

        @Override
        public Stream<Element> ancestorElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return ancestorElementStream().filter(predicate);
        }

        @Override
        public Stream<? super Element> childNodeStream() {
            int elementIdx = 0;
            List<Node> children = new ArrayList<>();

            for (var underlyingChildNode : underlyingElement().children()) {
                if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Element e) {
                    children.add(new Element(addToPath(elementIdx, navigationPath)));
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
                    .map(Element::new);
        }

        @Override
        public Stream<Element> childElementStream(Predicate<Element> predicate) {
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

            // Recursive
            if (predicate.test(this)) {
                return Stream.of(this);
            } else {
                return childElementStream().flatMap(e -> e.topmostDescendantElementOrSelfStream(predicate));
            }
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<Element> predicate) {
            Objects.requireNonNull(predicate);

            return childElementStream().flatMap(e -> e.topmostDescendantElementOrSelfStream(predicate));
        }
    }

    public static ElementTree create(eu.cdevreeze.yaidom4j.dom.immutabledom.Element underlyingRootElement) {
        ImmutableList<Integer> navigationPath = ImmutableList.of();
        Map<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap = new HashMap<>();
        buildElementCache(navigationPath, underlyingRootElement, elementMap);
        return new ElementTree(ImmutableMap.copyOf(elementMap));
    }

    private static void buildElementCache(
            ImmutableList<Integer> elementNavigationPath,
            eu.cdevreeze.yaidom4j.dom.immutabledom.Element element,
            Map<ImmutableList<Integer>, eu.cdevreeze.yaidom4j.dom.immutabledom.Element> elementMap
    ) {
        elementMap.put(elementNavigationPath, element);

        int idx = 0;
        for (var childElement : element.childElementStream().toList()) {
            // Recursion
            buildElementCache(
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
