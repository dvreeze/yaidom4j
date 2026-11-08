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
import eu.cdevreeze.yaidom4j.experimental.queryapi.ElementApi;
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
    private final GreenXmlTrees.Element greenTreeNode;

    public Element(int offset, @Nullable Node parentOrNull, GreenXmlTrees.Element greenTreeNode) {
        this.offset = offset;
        this.parentOrNull = parentOrNull;
        this.greenTreeNode = greenTreeNode;
    }

    @Override
    public int offset() {
        return offset;
    }

    public Optional<Node> parentOption() {
        return Optional.ofNullable(parentOrNull);
    }

    @Override
    public GreenXmlTrees.Element greenTreeNode() {
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
        return parentOption().filter(n -> n instanceof Element).map(n -> (Element) n);
    }

    @Override
    public Stream<Element> ancestorOrSelfElementStream() {
        return ancestorOrSelfStream().filter(n -> n instanceof Element).map(n -> (Element) n);
    }

    @Override
    public Stream<Element> ancestorOrSelfElementStream(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return ancestorOrSelfElementStream().filter(predicate);
    }

    @Override
    public Stream<Element> ancestorElementStream() {
        return ancestorStream().filter(n -> n instanceof Element).map(n -> (Element) n);
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
        List<GreenXmlTrees.CanBeChild> children = greenTreeNode.children();
        int numberOfChildren = children.size();
        return IntStream.range(0, numberOfChildren)
                .mapToObj(idx -> switch (children.get(idx)) {
                    case GreenXmlTrees.Element e -> new Element(idx, this, e);
                    case GreenXmlTrees.Text t -> new Text(idx, this, t);
                    case GreenXmlTrees.Comment c -> new Comment(idx, this, c);
                    case GreenXmlTrees.ProcessingInstruction p -> new ProcessingInstruction(idx, this, p);
                });
    }

    public List<CanBeChild> children() {
        // Immutable List
        return List.copyOf(
                childStream().map(n -> (CanBeChild) n).toList()
        );
    }
}
