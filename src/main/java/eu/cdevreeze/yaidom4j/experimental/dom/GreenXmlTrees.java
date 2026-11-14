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

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeElementApi;
import eu.cdevreeze.yaidom4j.experimental.queryapi.GreenTreeNodeApi;

/**
 * Green XML trees, unaware of parent nodes. These node trees are immutable, using "non-view unmodifiable collections".
 * <p>
 * The different node types are consistent with the XPath Data Model. Per node type the properties are
 * a small subset of those found in the XPath Data Model. For example, green trees do not know anything
 * about ancestor nodes, and typing information is absent.
 * <p>
 * This class also contains a friendly Node creation API. It is shamelessly copied, adapted and enhanced from
 * <a href="https://www.saxonica.com/html/documentation12/javadoc/net/sf/saxon/sapling/Saplings.html">Saxon Sapling</a>.
 * So credits go to <a href="https://www.saxonica.com">Saxonica</a> for that API. On the other hand,
 * yaidom4j already fits this API perfectly, because in essence it is roughly based on the XPath Data Model
 * information items (yet simplified), which deeply underlies the Saxon XML processor library.
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

        GreenClarkTrees.Node toClarkNode();

        @Override
        default Optional<Node> navigate(List<Integer> nodeNavigationPath) {
            if (nodeNavigationPath.isEmpty()) {
                return Optional.of(this);
            } else {
                int step = nodeNavigationPath.getFirst();
                List<Integer> remainingNodePath = nodeNavigationPath.stream().skip(1).toList();

                PreconditionUtil.checkArgument(step >= 0);
                Optional<Node> nextNodeOption = this.childStream().skip(step).findFirst();
                // Recursion
                return nextNodeOption.flatMap(node -> node.navigate(remainingNodePath));
            }
        }
    }

    public record Document(Optional<URI> uriOption, List<CanBeChild> children) implements Node {

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

        public Document withUri(URI uri) {
            return new Document(Optional.of(uri), children);
        }

        public Element documentElement() {
            return childStream().filter(n -> n instanceof Element)
                    .map(n -> (Element) n)
                    .findFirst()
                    .orElseThrow();
        }

        /**
         * Removes all inter-element whitespace, through the equally named method on the document element.
         */
        public Document removeInterElementWhitespace() {
            return new Document(
                    uriOption(),
                    children().stream()
                            .map(ch -> {
                                if (ch instanceof Element e) {
                                    return e.removeInterElementWhitespace();
                                } else {
                                    return ch;
                                }
                            })
                            .toList()
            );
        }

        @Override
        public GreenClarkTrees.Document toClarkNode() {
            return new GreenClarkTrees.Document(
                    uriOption,
                    children.stream()
                            .map(ch -> (GreenClarkTrees.CanBeChild) ch.toClarkNode())
                            .toList()
            );
        }
    }

    public record Attribute(QName name, String value) implements Node {

        public Attribute {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }

        @Override
        public GreenClarkTrees.Attribute toClarkNode() {
            return new GreenClarkTrees.Attribute(name, value);
        }
    }

    public record Namespace(String prefix, String namespaceUri) implements Node {

        public Namespace {
            Objects.requireNonNull(prefix);
            Objects.requireNonNull(namespaceUri);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }

        @Override
        public GreenClarkTrees.Node toClarkNode() {
            throw new RuntimeException("No Clark equivalent of a namespace node");
        }
    }

    public sealed interface CanBeChild extends Node permits Element, Text, Comment, ProcessingInstruction {
    }

    public record Text(String value) implements CanBeChild {

        public Text {
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }

        @Override
        public GreenClarkTrees.Text toClarkNode() {
            return new GreenClarkTrees.Text(value);
        }
    }

    public record Comment(String value) implements CanBeChild {

        public Comment {
            Objects.requireNonNull(value);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }

        @Override
        public GreenClarkTrees.Comment toClarkNode() {
            return new GreenClarkTrees.Comment(value);
        }
    }

    public record ProcessingInstruction(String target, String data) implements CanBeChild {

        public ProcessingInstruction {
            Objects.requireNonNull(target);
            Objects.requireNonNull(data);
        }

        @Override
        public Stream<Node> childStream() {
            return Stream.empty();
        }

        @Override
        public GreenClarkTrees.ProcessingInstruction toClarkNode() {
            return new GreenClarkTrees.ProcessingInstruction(target, data);
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
            return List.copyOf(
                    namespaceScope().completeInScopeNamespaces()
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

        /**
         * Functionally updates this element by replacing the collection of child nodes.
         * <p>
         * When calling this function, take care to align the in-scope namespaces in order to
         * prevent invalid XML 1.0 from being created. After all, prefixed namespace undeclarations
         * are not allowed in XML 1.0.
         */
        public Element withChildren(List<CanBeChild> newChildren) {
            return new Element(
                    name(),
                    attributes(),
                    namespaceScope(),
                    List.copyOf(newChildren)
            );
        }

        public Element plusChild(CanBeChild newChild) {
            return withChildren(
                    Stream.concat(children.stream(), Stream.of(newChild)).toList()
            );
        }

        /**
         * Convenience method to set a Text child as the only child node
         */
        public Element withText(String textValue) {
            return withChildren(List.of(new Text(textValue)));
        }

        /**
         * Convenience method to add a Text child
         */
        public Element plusText(String textValue) {
            return plusChild(new Text(textValue));
        }

        /**
         * Functionally updates this element by replacing the collection of attributes.
         * <p>
         * For prefixed attributes, take care to use only prefixes that are in scope.
         */
        public Element withAttributes(Map<QName, String> newAttributes) {
            return new Element(
                    name(),
                    Map.copyOf(newAttributes),
                    namespaceScope(),
                    children()
            );
        }

        public Element plusAttribute(QName attrName, String attrValue) {
            Map<QName, String> newAttributes = new HashMap<>(attributes());
            newAttributes.put(attrName, attrValue);
            return withAttributes(Map.copyOf(newAttributes));
        }

        public Element transformChildElements(UnaryOperator<Element> f) {
            return withChildren(
                    children.stream().map(ch ->
                            switch (ch) {
                                case Element e -> f.apply(e);
                                default -> ch;
                            }
                    ).toList()
            );
        }

        public Element transformDescendantOrSelfElements(UnaryOperator<Element> f) {
            // Recursion
            Element descendantResult =
                    transformChildElements(che -> che.transformDescendantOrSelfElements(f));
            return f.apply(descendantResult);
        }

        public Element transformSelfElement(UnaryOperator<Element> f) {
            return f.apply(this);
        }

        @Override
        public GreenClarkTrees.Element toClarkNode() {
            return new GreenClarkTrees.Element(
                    name,
                    attributes,
                    children().stream()
                            .map(ch -> (GreenClarkTrees.CanBeChild) ch.toClarkNode())
                            .toList()
            );
        }

        // Custom element transformations

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
            PreconditionUtil.checkArgument(namespaceScope().subScopeOf(newScope));
            PreconditionUtil.checkArgument(namespaceScope().defaultNamespaceOption().equals(newScope.defaultNamespaceOption()));

            // Recursion
            return new Element(name(), attributes(), newScope, children())
                    .transformChildElements(che -> che.withParentAttributeScope(newScope));
        }

        public Element removeInterElementWhitespace() {
            // Naive implementation
            boolean hasElementChild = children.stream().anyMatch(n -> n instanceof Element);
            boolean hasOnlyInterElementWhitespaceTextChildren = hasElementChild &&
                    children.stream().allMatch(ch -> !(ch instanceof Text(String value)) || value.isBlank());

            List<CanBeChild> filteredChildren =
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
                            .toList()
            );
        }
    }

    public static final class ElementPredicates {

        private ElementPredicates() {
            // Non-instantiable
        }

        public static Predicate<Element> hasName(QName name) {
            return e -> e.name().equals(name);
        }

        public static Predicate<Element> hasName(String namespace, String localName) {
            return e -> e.name().getNamespaceURI().equals(namespace) && e.name().getLocalPart().equals(localName);
        }

        public static Predicate<Element> hasName(String noNamespaceName) {
            return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
        }

        public static Predicate<Element> hasLocalName(String localName) {
            return e -> e.name().getLocalPart().equals(localName);
        }

        public static Predicate<Element> hasAttributeWithName(QName attrName) {
            return e -> e.attributeOption(attrName).isPresent();
        }

        public static Predicate<Element> hasAttributeWithName(String attrNamespace, String attrLocalName) {
            return hasAttributeWithName(new QName(attrNamespace, attrLocalName));
        }

        public static Predicate<Element> hasAttributeWithName(String attrNoNamespaceName) {
            return hasAttributeWithName(new QName(attrNoNamespaceName));
        }

        public static Predicate<Element> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate) {
            return e -> e.attributes().entrySet().stream()
                    .anyMatch(kv -> kv.getKey().equals(attrName) && attrValuePredicate.test(kv.getValue()));
        }

        public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
            return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValuePredicate);
        }

        public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
            return hasAttributeValue(new QName(attrNoNamespaceName), attrValuePredicate);
        }

        public static Predicate<Element> hasAttributeValue(QName attrName, String attrValue) {
            return hasAttributeValue(attrName, s -> s.equals(attrValue));
        }

        public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue) {
            return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValue);
        }

        public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, String attrValue) {
            return hasAttributeValue(new QName(attrNoNamespaceName), attrValue);
        }

        public static Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
            return e -> e.children().stream().allMatch(ch -> ch instanceof Text) &&
                    textPredicate.test(e.text());
        }

        public static Predicate<Element> hasOnlyText(String text) {
            return hasOnlyText(s -> s.equals(text));
        }

        public static Predicate<Element> hasOnlyTextAfterStripping(String text) {
            return hasOnlyText(s -> s.strip().equals(text));
        }
    }

    // Node creation API

    public static Document doc() {
        return new Document(Optional.empty(), List.of());
    }

    public static Document doc(URI docUri) {
        return new Document(Optional.of(docUri), List.of());
    }

    /**
     * Creates an empty Element node. If the element name has a namespace, the prefix and namespace
     * make up the one-element namespace scope of the created element. Otherwise, the namespace scope
     * of the element is empty.
     * <p>
     * Consider calling {@link Element#withParentAttributeScope(NamespaceScope)} on the result
     * to safely add prefix-namespace bindings.
     */
    public static Element elem(QName name) {
        return new Element(
                name,
                Map.of(),
                name.getNamespaceURI().isEmpty() ?
                        NamespaceScope.empty() :
                        NamespaceScope.of(name.getPrefix(), name.getNamespaceURI()),
                List.of()
        );
    }

    public static Element elem(String noNamespaceName) {
        return elem(new QName(noNamespaceName));
    }

    public static Text text(String value) {
        return new Text(value);
    }

    public static Comment comment(String value) {
        return new Comment(value);
    }

    public static ProcessingInstruction pi(String target, String data) {
        return new ProcessingInstruction(target, data);
    }
}
