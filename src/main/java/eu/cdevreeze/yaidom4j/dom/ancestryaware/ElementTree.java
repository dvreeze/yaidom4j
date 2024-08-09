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
        this.elementMap = elementMap;
    }

    public Element rootElement() {
        return new Element(ImmutableList.of());
    }

    @Override
    public Optional<Element> parentElementOption(Element element) {
        return parentPathOption(element.navigationPath()).map(Element::new);
    }

    @Override
    public Stream<Element> ancestorElementOrSelfStream(Element element) {
        // Recursive
        return Stream.concat(
                Stream.of(element),
                parentElementOption(element).stream().flatMap(this::ancestorElementOrSelfStream)
        );
    }

    @Override
    public Stream<Element> ancestorElementOrSelfStream(Element element, Predicate<Element> predicate) {
        return ancestorElementOrSelfStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> ancestorElementStream(Element element) {
        return parentElementOption(element).stream().flatMap(this::ancestorElementOrSelfStream);
    }

    @Override
    public Stream<Element> ancestorElementStream(Element element, Predicate<Element> predicate) {
        return ancestorElementStream(element).filter(predicate);
    }

    @Override
    public QName elementName(Element element) {
        return element.underlyingElement().name();
    }

    @Override
    public ImmutableMap<QName, String> attributes(Element element) {
        return element.underlyingElement().attributes();
    }

    @Override
    public Stream<? super Element> childNodeStream(Element element) {
        int elementIdx = 0;
        List<Node> children = new ArrayList<>();

        for (var underlyingChildNode : element.underlyingElement().children()) {
            if (underlyingChildNode instanceof eu.cdevreeze.yaidom4j.dom.immutabledom.Element e) {
                children.add(new Element(addToPath(elementIdx, element.navigationPath)));
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
    public Stream<Element> childElementStream(Element element) {
        var navigationPath = element.navigationPath();

        return IntStream.range(0, (int) element.underlyingElement().childElementStream().count())
                .mapToObj(i -> addToPath(i, navigationPath))
                .map(Element::new);
    }

    @Override
    public Stream<Element> childElementStream(Element element, Predicate<Element> predicate) {
        return childElementStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream(Element element) {
        // Recursive
        return Stream.concat(
                Stream.of(element),
                childElementStream(element).flatMap(this::descendantElementOrSelfStream)
        );
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
        return descendantElementOrSelfStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> descendantElementStream(Element element) {
        return childElementStream(element).flatMap(this::descendantElementOrSelfStream);
    }

    @Override
    public Stream<Element> descendantElementStream(Element element, Predicate<Element> predicate) {
        return descendantElementStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> topmostDescendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
        // Recursive
        if (predicate.test(element)) {
            return Stream.of(element);
        } else {
            return childElementStream(element).flatMap(e -> topmostDescendantElementOrSelfStream(e, predicate));
        }
    }

    @Override
    public Stream<Element> topmostDescendantElementStream(Element element, Predicate<Element> predicate) {
        return childElementStream(element).flatMap(e -> topmostDescendantElementOrSelfStream(e, predicate));
    }

    public final class Element implements CanBeDocumentChild {

        private final ImmutableList<Integer> navigationPath;

        private Element(ImmutableList<Integer> navigationPath) {
            this.navigationPath = navigationPath;
        }

        public ImmutableList<Integer> navigationPath() {
            return navigationPath;
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

        public Optional<String> attributeOption(QName attrName) {
            return underlyingElement().attributeOption(attrName);
        }

        public String attribute(QName attrName) {
            return underlyingElement().attribute(attrName);
        }

        /**
         * Returns the concatenated text of the text children (ignoring other child nodes than text nodes)
         */
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

        // Convenience methods delegating to the element query API

        public Stream<Element> elementStream() {
            return ElementTree.this.elementStream(this);
        }

        public Stream<Element> elementStream(Predicate<Element> predicate) {
            return ElementTree.this.elementStream(this, predicate);
        }

        public Stream<Element> topmostElementStream(Predicate<Element> predicate) {
            return ElementTree.this.topmostElementStream(this, predicate);
        }

        public Optional<Element> parentElementOption() {
            return ElementTree.this.parentElementOption(this);
        }

        public Stream<Element> ancestorElementOrSelfStream() {
            return ElementTree.this.ancestorElementOrSelfStream(this);
        }

        public Stream<Element> ancestorElementOrSelfStream(Predicate<Element> predicate) {
            return ElementTree.this.ancestorElementOrSelfStream(this, predicate);
        }

        public Stream<Element> ancestorElementStream() {
            return ElementTree.this.ancestorElementStream(this);
        }

        public Stream<Element> ancestorElementStream(Predicate<Element> predicate) {
            return ElementTree.this.ancestorElementStream(this, predicate);
        }

        public QName elementName() {
            return ElementTree.this.elementName(this);
        }

        public ImmutableMap<QName, String> attributes() {
            return ElementTree.this.attributes(this);
        }

        public Stream<? super Element> childNodeStream() {
            return ElementTree.this.childNodeStream(this);
        }

        public Stream<Element> childElementStream() {
            return ElementTree.this.childElementStream(this);
        }

        public Stream<Element> childElementStream(Predicate<Element> predicate) {
            return ElementTree.this.childElementStream(this, predicate);
        }

        public Stream<Element> descendantElementOrSelfStream() {
            return ElementTree.this.descendantElementOrSelfStream(this);
        }

        public Stream<Element> descendantElementOrSelfStream(Predicate<Element> predicate) {
            return ElementTree.this.descendantElementOrSelfStream(this, predicate);
        }

        public Stream<Element> descendantElementStream() {
            return ElementTree.this.descendantElementStream(this);
        }

        public Stream<Element> descendantElementStream(Predicate<Element> predicate) {
            return ElementTree.this.descendantElementStream(this, predicate);
        }

        public Stream<Element> topmostDescendantElementOrSelfStream(Predicate<Element> predicate) {
            return ElementTree.this.topmostDescendantElementOrSelfStream(this, predicate);
        }

        public Stream<Element> topmostDescendantElementStream(Predicate<Element> predicate) {
            return ElementTree.this.topmostDescendantElementStream(this, predicate);
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
