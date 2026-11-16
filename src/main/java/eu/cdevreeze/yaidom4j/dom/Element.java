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

package eu.cdevreeze.yaidom4j.dom;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;
import org.jspecify.annotations.Nullable;

/**
 * Immutable element node.
 * <p>
 * Not a record class, and using reference equality rather than (in this case) very expensive value equality.
 *
 * @author Chris de Vreeze
 */
public final class Element implements CanBeChild, ElementApi<Element> {

    private static final QName XML_BASE_QNAME = new QName(XMLConstants.XML_NS_URI, "base");

    private final int offset;
    private final @Nullable Node parentOrNull;
    private final GreenNodes.Element greenTreeNode;

    // Package-private constructor
    Element(int offset, @Nullable Node parentOrNull, GreenNodes.Element greenTreeNode) {
        this.offset = offset;
        this.parentOrNull = parentOrNull;
        this.greenTreeNode = greenTreeNode;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public Optional<Node> parentOption() {
        return Optional.ofNullable(parentOrNull);
    }

    @Override
    public GreenNodes.Element greenTreeNode() {
        return greenTreeNode;
    }

    @Override
    public Optional<URI> docUriOption() {
        return ancestorOrSelfStream()
                .filter(n -> n instanceof Document)
                .map(n -> (Document) n)
                .findFirst()
                .flatMap(d -> d.greenTreeNode().uriOption());
    }

    @Override
    public Optional<URI> baseUriOption() {
        List<URI> xmlBaseUrisReversed = ancestorOrSelfElementStream()
                .flatMap(e -> e.attributeOption(XML_BASE_QNAME).stream())
                .map(URI::create)
                .toList();

        List<URI> xmlBaseUris = xmlBaseUrisReversed.reversed(); // Since Java 21

        // See http://stackoverflow.com/questions/22203111/is-javas-uri-resolve-incompatible-with-rfc-3986-when-the-relative-uri-contains

        // Stream gatherers have been available since Java 24
        return xmlBaseUris.stream().gather(Gatherers.fold(
                this::docUriOption,
                (Optional<URI> currUriOption, URI base) ->
                        Optional.of(currUriOption.map(b -> b.resolve(base)).orElse(base))
        )).findFirst().orElse(Optional.empty());
    }

    @Override
    public NamespaceScope namespaceScope() {
        return greenTreeNode().namespaceScope();
    }

    @Override
    public Optional<Element> parentElementOption() {
        return parentOption().filter(Node::isElement).map(Node::asElement);
    }

    @Override
    public Stream<Element> ancestorOrSelfElementStream() {
        return ancestorOrSelfStream().filter(Node::isElement).map(Node::asElement);
    }

    @Override
    public Stream<Element> ancestorOrSelfElementStream(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return ancestorOrSelfElementStream().filter(predicate);
    }

    @Override
    public Stream<Element> ancestorElementStream() {
        return ancestorStream().filter(Node::isElement).map(Node::asElement);
    }

    @Override
    public Stream<Element> ancestorElementStream(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return ancestorElementStream().filter(predicate);
    }

    @Override
    public QName elementName() {
        return greenTreeNode().elementName();
    }

    @Override
    public Map<QName, String> attributes() {
        return greenTreeNode().attributes();
    }

    @Override
    public Optional<String> attributeOption(QName attrName) {
        return greenTreeNode().attributeOption(attrName);
    }

    @Override
    public String attribute(QName attrName) {
        return greenTreeNode().attribute(attrName);
    }

    @Override
    public String text() {
        return greenTreeNode().text();
    }

    @Override
    public Stream<Element> childElementStream() {
        return childStream().filter(Node::isElement).map(Node::asElement);
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
        return Stream.concat(
                selfStream,
                childElementStream().flatMap(Element::descendantOrSelfElementStream)
        );
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
        List<GreenNodes.CanBeChild> children = greenTreeNode.children();
        int numberOfChildren = children.size();
        return IntStream.range(0, numberOfChildren)
                .mapToObj(idx -> switch (children.get(idx)) {
                    case GreenNodes.Element e -> new Element(idx, this, e);
                    case GreenNodes.Text t -> new Text(idx, this, t);
                    case GreenNodes.Comment c -> new Comment(idx, this, c);
                    case GreenNodes.ProcessingInstruction p -> new ProcessingInstruction(idx, this, p);
                });
    }

    public List<CanBeChild> children() {
        // Immutable List
        return List.copyOf(
                childStream().map(Node::asCanBeChild).toList()
        );
    }

    // Factories

    /**
     * Creates a top-level {@link Element} from the given {@link GreenNodes.Element}.
     * Hence, the returned element has no parent node, not even a document.
     */
    public static Element of(GreenNodes.Element greenTreeElement) {
        return new Element(0, null, greenTreeElement);
    }

    // Element predicates

    public static Predicate<Element> hasName(QName name) {
        return e -> e.elementName().equals(name);
    }

    public static Predicate<Element> hasName(String namespace, String localName) {
        return e -> e.elementName().getNamespaceURI().equals(namespace) && e.elementName().getLocalPart().equals(localName);
    }

    public static Predicate<Element> hasName(String noNamespaceName) {
        return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
    }

    public static Predicate<Element> hasLocalName(String localName) {
        return e -> e.elementName().getLocalPart().equals(localName);
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
