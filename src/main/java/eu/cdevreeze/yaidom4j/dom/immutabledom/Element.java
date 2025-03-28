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
import eu.cdevreeze.yaidom4j.core.ElementNavigationPath;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;
import eu.cdevreeze.yaidom4j.transformationapi.TransformableElementApi;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * XML element node. The element is namespace-well-formed in the sense that (non-empty) prefixes in element name
 * and attribute names are guaranteed to be bound to a namespace, or else an exception is thrown.
 * It is also checked that the element name and attribute names are allowed by the passed namespace scope.
 *
 * @author Chris de Vreeze
 */
public record Element(
        QName name,
        ImmutableMap<QName, String> attributes,
        NamespaceScope namespaceScope,
        ImmutableList<Node> children
) implements CanBeDocumentChild, ElementApi<Element>, TransformableElementApi<Element, Node> {

    public Element {
        Objects.requireNonNull(name);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(namespaceScope);
        Objects.requireNonNull(children);

        Preconditions.checkArgument(
                namespaceScope.allowsElementName(name),
                String.format("Name '%s' not allowed by NamespaceScope %s", name, namespaceScope)
        );

        Preconditions.checkArgument(attributes.keySet().stream().allMatch(namespaceScope::allowsAttributeName));
    }

    @Override
    public boolean isElement() {
        return true;
    }

    @Override
    public ClarkNodes.Element toClarkNode() {
        return new ClarkNodes.Element(
                name(),
                attributes(),
                children.stream().map(Node::toClarkNode).collect(ImmutableList.toImmutableList())
        );
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
    public Optional<NamespaceScope> namespaceScopeOption() {
        return Optional.of(namespaceScope());
    }

    public Stream<Node> childNodeStream() {
        return children().stream();
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
    public Stream<Element> selfElementStream() {
        return Stream.of(Element.this);
    }

    @Override
    public Stream<Element> selfElementStream(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return selfElementStream().filter(predicate);
    }

    @Override
    public Stream<Element> childElementStream() {
        return childNodeStream().filter(Node::isElement).map(n -> (Element) n);
    }

    @Override
    public Stream<Element> childElementStream(Predicate<? super Element> predicate) {
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

        if (predicate.test(this)) {
            return Stream.of(this);
        } else {
            // Recursion
            return childElementStream().flatMap(che -> che.topmostDescendantElementOrSelfStream(predicate));
        }
    }

    @Override
    public Stream<Element> topmostDescendantElementStream(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return childElementStream().flatMap(che -> che.topmostDescendantElementOrSelfStream(predicate));
    }

    public Stream<Element> select(ElementStep elementStep) {
        return elementStep.apply(Element.this);
    }

    // Functional updates

    /**
     * Functionally adds or updates a namespace binding. This is sometimes needed, for example when
     * (functionally) updating the name where a new namespace binding is needed for the prefix in the
     * name.
     * <p>
     * On the other hand, be careful when calling this method, because this may cause a prefix to
     * be bound to another namespace than before. In particular, be careful with default namespaces.
     * <p>
     * Also, adding a (non-empty) namespace binding requires all descendant elements to get this extra
     * namespace binding as well, or else the result will not be valid XML 1.0.
     *
     * @deprecated If feasible, prefer {@link Element#withParentAttributeScope(NamespaceScope)} instead
     */
    @Deprecated
    public Element plusNamespaceBinding(String prefix, String namespace) {
        return new Element(name(), attributes(), namespaceScope().resolve(prefix, namespace), children());
    }

    /**
     * Returns the same element, except that the namespace scope is enhanced by resolving it against the
     * given parent scope, without the default namespace, if any. This implies that this element's
     * namespace scope "is in the lead", and that the default namespace is not introduced by the
     * parent scope, even if the parameter parent scope has a default namespace. Hence, this is a safe
     * operation that only adds still unused prefix-namespace bindings (without default namespace).
     *
     * @deprecated Use {@link Element#withParentAttributeScope(NamespaceScope)} instead
     */
    @Deprecated
    public Element usingParentAttributeScope(NamespaceScope parentScope) {
        return new Element(
                name(),
                attributes(),
                parentScope.withoutDefaultNamespace().resolve(namespaceScope().inScopeNamespaces()),
                children()
        );
    }

    /**
     * Returns the "same element", except that there are no prefixed namespace un-declarations
     * (which are not allowed in XML 1.0), and the given parent scope (without its default
     * namespace, if any) is taken as the "starting point". If this element is valid in XML 1.0,
     * then this element and its descendant elements will (functionally) get the extra (non-empty)
     * prefixes in the parent scope that do not conflict with the element's own namespace scope.
     */
    public Element withParentAttributeScope(NamespaceScope parentScope) {
        NamespaceScope newScope =
                parentScope.withoutDefaultNamespace().resolve(namespaceScope().inScopeNamespaces());

        // The namespace scope additions are completely safe:
        Preconditions.checkArgument(namespaceScope().subScopeOf(newScope));
        Preconditions.checkArgument(namespaceScope().defaultNamespaceOption().equals(newScope.defaultNamespaceOption()));

        // Recursion
        return new Element(name(), attributes(), newScope, children())
                .transformChildElements(che -> che.withParentAttributeScope(newScope));
    }

    /**
     * Functionally updates the name of this element.
     * <p>
     * When calling this function, make sure to first add a namespace binding if needed.
     * The safest way to do that is calling method {@link Element#withParentAttributeScope(NamespaceScope)},
     * if the parameter name's prefix has not already been bound to another namespace.
     */
    @Override
    public Element withName(QName newName) {
        return new Element(newName, attributes(), namespaceScope(), children());
    }

    /**
     * Functionally updates this element by replacing the collection of child nodes.
     * <p>
     * When calling this function, take care to align the in-scope namespaces in order to
     * prevent invalid XML 1.0 from being created. After all, prefixed namespace undeclarations
     * are not allowed in XML 1.0.
     */
    @Override
    public Element withChildren(ImmutableList<Node> newChildren) {
        return new Element(
                name(),
                attributes(),
                namespaceScope(),
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
    public Element plusChildOption(Optional<Node> newChildOption) {
        return newChildOption.map(this::plusChild).orElse(this);
    }

    @Override
    public Element plusChildren(ImmutableList<Node> newChildren) {
        return withChildren(
                ImmutableList.<Node>builder().addAll(children).addAll(newChildren).build()
        );
    }

    /**
     * Convenience method to set a Text child as the only child node
     */
    public Element withText(String textValue) {
        return withChildren(ImmutableList.of(new Text(textValue, false)));
    }

    /**
     * Convenience method to add a Text child
     */
    public Element plusText(String textValue) {
        return plusChild(new Text(textValue, false));
    }

    /**
     * Functionally updates this element by replacing the collection of attributes.
     * <p>
     * For prefixed attributes, take care to use only prefixes that are in scope.
     */
    @Override
    public Element withAttributes(ImmutableMap<QName, String> newAttributes) {
        return new Element(
                name(),
                newAttributes,
                namespaceScope(),
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
    public Element plusAttributeOption(QName attrName, Optional<String> newAttrValueOption) {
        return withAttributes(
                ImmutableMap.<QName, String>builder()
                        .putAll(attributes)
                        .putAll(
                                newAttrValueOption
                                        .stream()
                                        .map(v -> Map.entry(attrName, v))
                                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                        )
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
    public Element transformSelf(UnaryOperator<Element> f) {
        return f.apply(this);
    }

    @Override
    public Element updateElement(
            Set<ElementNavigationPath> elementNavigationPaths,
            BiFunction<ElementNavigationPath, Element, Element> f
    ) {
        Map<Integer, Set<ElementNavigationPath>> pathsByFirstChildElemIndex =
                elementNavigationPaths
                        .stream()
                        .filter(p -> !p.isEmpty())
                        .collect(Collectors.groupingBy(
                                p -> p.getEntry(0),
                                Collectors.toSet()
                        ));

        // Recursive
        Element descendantUpdateResult =
                updateChildElements(
                        pathsByFirstChildElemIndex.keySet(),
                        (childElemIdx, che) ->
                                che.updateElement(
                                        pathsByFirstChildElemIndex.get(childElemIdx)
                                                .stream()
                                                .map(ElementNavigationPath::withoutFirstEntry)
                                                .collect(Collectors.toSet()),
                                        (path, elem) -> f.apply(path.prependEntry(childElemIdx), elem)
                                )
                );

        return elementNavigationPaths.contains(ElementNavigationPath.empty()) ?
                f.apply(ElementNavigationPath.empty(), descendantUpdateResult) :
                descendantUpdateResult;
    }

    // Custom element transformations

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

    /**
     * Alias for {@link Element#withParentAttributeScope(NamespaceScope)}.
     */
    public Element notUndeclaringPrefixes(NamespaceScope parentScope) {
        return withParentAttributeScope(parentScope);
    }

    // Private methods

    private Element updateChildElements(Set<Integer> childElementIndices, BiFunction<Integer, Element, Element> f) {
        if (childElementIndices.isEmpty()) {
            return this;
        } else {
            int childNodeIndex = 0;
            int childElementIndex = 0;
            List<Node> updatedChildren = new ArrayList<>();

            while (childNodeIndex < this.children().size()) {
                if (this.children().get(childNodeIndex) instanceof Element elem) {
                    if (childElementIndices.contains(childElementIndex)) {
                        updatedChildren.add(f.apply(childElementIndex, elem));
                    } else {
                        updatedChildren.add(elem);
                    }
                    childElementIndex += 1;
                } else {
                    updatedChildren.add(this.children().get(childNodeIndex));
                }
                childNodeIndex += 1;
            }
            Preconditions.checkArgument(updatedChildren.size() == this.children().size());

            return this.withChildren(ImmutableList.copyOf(updatedChildren));
        }
    }
}
