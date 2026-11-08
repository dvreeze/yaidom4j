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

package eu.cdevreeze.yaidom4j.experimental.testsupport.saxon;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.queryapi.ElementApi;
import eu.cdevreeze.yaidom4j.experimental.queryapi.NodeApi;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.streams.Steps;
import net.sf.saxon.tree.NamespaceNode;
import org.jspecify.annotations.NullMarked;

/**
 * Saxon node wrappers.
 *
 * @author Chris de Vreeze
 */
@NullMarked
public class SaxonNodes {

    private SaxonNodes() {
    }

    public static Optional<Node> optionalNodeFrom(XdmNode xdmNode) {
        return switch (xdmNode.getNodeKind()) {
            case ELEMENT -> Optional.of(new Element(xdmNode));
            case TEXT -> Optional.of(new Text(xdmNode));
            case COMMENT -> Optional.of(new Comment(xdmNode));
            case PROCESSING_INSTRUCTION -> Optional.of(new ProcessingInstruction(xdmNode));
            case DOCUMENT -> Optional.of(new Document(xdmNode));
            case ATTRIBUTE -> Optional.of(new Attribute(xdmNode));
            case NAMESPACE -> Optional.of(new Namespace(xdmNode));
        };
    }

    public sealed interface Node extends NodeApi<Node> permits CanBeChild, Document, Attribute, Namespace {

        XdmNode xdmNode();

        boolean isElement();

        GreenXmlTrees.Node toGreenTreeNode();

        GreenClarkTrees.Node toClarkNode();

        @Override
        default Optional<Node> parentOption() {
            return xdmNode().select(Steps.parent())
                    .flatMap(n -> optionalNodeFrom(n).stream())
                    .findFirst()
                    .map(n -> n);
        }

        @Override
        default Stream<Node> ancestorOrSelfStream() {
            return xdmNode().select(Steps.ancestorOrSelf())
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> ancestorOrSelfStream(Predicate<? super Node> predicate) {
            return xdmNode().select(Steps.ancestorOrSelf(n -> optionalNodeFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> ancestorStream() {
            return xdmNode().select(Steps.ancestor())
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> ancestorStream(Predicate<? super Node> predicate) {
            return xdmNode().select(Steps.ancestor(n -> optionalNodeFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> childStream() {
            return xdmNode().select(Steps.child())
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> childStream(Predicate<? super Node> predicate) {
            return xdmNode().select(Steps.child(n -> optionalNodeFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> descendantOrSelfStream() {
            return xdmNode().select(Steps.descendantOrSelf())
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> descendantOrSelfStream(Predicate<? super Node> predicate) {
            return xdmNode().select(Steps.descendantOrSelf(n -> optionalNodeFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> descendantStream() {
            return xdmNode().select(Steps.descendant())
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> descendantStream(Predicate<? super Node> predicate) {
            return xdmNode().select(Steps.descendant(n -> optionalNodeFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> optionalNodeFrom(n).stream());
        }

        @Override
        default Stream<Node> topmostDescendantOrSelfStream(Predicate<? super Node> predicate) {
            // Inefficient
            return descendantOrSelfStream(predicate)
                    .filter(n -> n.ancestorStream().noneMatch(predicate));
        }

        @Override
        default Stream<Node> topmostDescendantStream(Predicate<? super Node> predicate) {
            // Inefficient
            return descendantStream(predicate)
                    .filter(n -> n.ancestorStream().noneMatch(predicate));
        }
    }

    public static final class Document implements Node {

        private final XdmNode xdmNode;

        public Document(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.DOCUMENT));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.Document toGreenTreeNode() {
            var uriOption = Optional.ofNullable(xdmNode.getUnderlyingNode().getSystemId()).map(URI::create);
            return new GreenXmlTrees.Document(
                    uriOption,
                    xdmNode().select(Steps.child())
                            .flatMap(n -> optionalNodeFrom(n).stream())
                            .map(Node::toGreenTreeNode)
                            .map(n -> (GreenXmlTrees.CanBeChild) n)
                            .toList()
            );
        }

        @Override
        public GreenClarkTrees.Document toClarkNode() {
            var uriOption = Optional.ofNullable(xdmNode.getUnderlyingNode().getSystemId()).map(URI::create);
            return new GreenClarkTrees.Document(
                    uriOption,
                    xdmNode().select(Steps.child())
                            .flatMap(n -> optionalNodeFrom(n).stream())
                            .map(Node::toClarkNode)
                            .map(n -> (GreenClarkTrees.CanBeChild) n)
                            .toList()
            );
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Document otherDoc) {
                return otherDoc.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }
    }

    public static final class Attribute implements Node {

        private final XdmNode xdmNode;

        public Attribute(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.ATTRIBUTE));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.Attribute toGreenTreeNode() {
            return new GreenXmlTrees.Attribute(
                    xdmNode.getNodeName().getStructuredQName().toJaxpQName(),
                    xdmNode.getStringValue()
            );
        }

        @Override
        public GreenClarkTrees.Attribute toClarkNode() {
            return new GreenClarkTrees.Attribute(
                    xdmNode.getNodeName().getStructuredQName().toJaxpQName(),
                    xdmNode.getStringValue()
            );
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Attribute otherAttr) {
                return otherAttr.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }
    }

    public static final class Namespace implements Node {

        private final XdmNode xdmNode;

        public Namespace(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.NAMESPACE));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.Namespace toGreenTreeNode() {
            return new GreenXmlTrees.Namespace(
                    xdmNode.getUnderlyingNode().getDisplayName(),
                    xdmNode.getStringValue()
            );
        }

        @Override
        public GreenClarkTrees.Node toClarkNode() {
            throw new RuntimeException("No Clark equivalent of a namespace node");
        }
    }

    public sealed interface CanBeChild extends Node permits Element, Text, Comment, ProcessingInstruction {
    }

    public static final class Element implements CanBeChild, ElementApi<Element> {

        private final XdmNode xdmNode;

        public Element(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.ELEMENT));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        @Override
        public boolean isElement() {
            return true;
        }

        @Override
        public GreenXmlTrees.Element toGreenTreeNode() {
            // Recursive
            return new GreenXmlTrees.Element(
                    elementName(),
                    attributes(),
                    namespaceScope(),
                    childNodeStream().map(Node::toGreenTreeNode).map(n -> (GreenXmlTrees.CanBeChild) n).toList()
            );
        }

        @Override
        public GreenClarkTrees.Element toClarkNode() {
            // Recursive
            return new GreenClarkTrees.Element(
                    elementName(),
                    attributes(),
                    childNodeStream().map(Node::toClarkNode).map(n -> (GreenClarkTrees.CanBeChild) n).toList()
            );
        }

        @Override
        public QName elementName() {
            return xdmNode.getNodeName().getStructuredQName().toJaxpQName();
        }

        @Override
        public Map<QName, String> attributes() {
            return xdmNode.select(Steps.attribute())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    xdmAttrNode -> xdmAttrNode.getNodeName().getStructuredQName().toJaxpQName(),
                                    XdmItem::getStringValue
                            )
                    );
        }

        @Override
        public Optional<String> attributeOption(QName attrName) {
            return xdmNode.select(Steps.attribute(attrName.getNamespaceURI(), attrName.getLocalPart()))
                    .map(XdmItem::getStringValue)
                    .findAny();
        }

        @Override
        public String attribute(QName attrName) {
            return attributeOption(attrName).orElseThrow();
        }

        @Override
        public String text() {
            return xdmNode.select(Steps.child())
                    .filter(n -> n.getNodeKind().equals(XdmNodeKind.TEXT))
                    .map(XdmNode::getStringValue)
                    .collect(Collectors.joining());
        }

        @Override
        public NamespaceScope namespaceScope() {
            Map<String, String> nsMappings = xdmNode.select(Steps.namespace())
                    .map(n -> (NamespaceNode) n.getUnderlyingNode())
                    .map(n -> Map.entry(n.getDisplayName(), n.getStringValue()))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

            return NamespaceScope.from(nsMappings);
        }

        @Override
        public Optional<URI> docUriOption() {
            return Optional.ofNullable(xdmNode.getUnderlyingNode().getSystemId())
                    .filter(u -> !u.isBlank())
                    .map(URI::create);
        }

        @Override
        public Optional<URI> baseUriOption() {
            return Optional.ofNullable(xdmNode.getBaseURI());
        }

        public Stream<Node> childNodeStream() {
            return xdmNode.select(Steps.child())
                    .flatMap(n -> SaxonNodes.optionalNodeFrom(n).stream());
        }

        @Override
        public Stream<Element> childElementStream() {
            return xdmNode.select(SaxonElementSteps.childElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> childElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.childElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantOrSelfElementStream() {
            return xdmNode.select(SaxonElementSteps.descendantOrSelfElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantOrSelfElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.descendantOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementStream() {
            return xdmNode.select(SaxonElementSteps.descendantElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.descendantElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> topmostDescendantOrSelfElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.topmostDescendantOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.topmostDescendantElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Optional<Element> parentElementOption() {
            return xdmNode.select(SaxonElementSteps.parentElements()).flatMap(n -> Element.optionallyFrom(n).stream()).findFirst();
        }

        @Override
        public Stream<Element> ancestorOrSelfElementStream() {
            return xdmNode.select(SaxonElementSteps.ancestorOrSelfElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorOrSelfElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.ancestorOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorElementStream() {
            return xdmNode.select(SaxonElementSteps.ancestorElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorElementStream(Predicate<? super Element> predicate) {
            return xdmNode.select(SaxonElementSteps.ancestorElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Element otherElement) {
                return otherElement.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }

        public static Optional<Element> optionallyFrom(XdmNode xdmNode) {
            if (xdmNode.getNodeKind().equals(XdmNodeKind.ELEMENT)) {
                return Optional.of(new Element(xdmNode));
            } else {
                return Optional.empty();
            }
        }
    }

    public static final class Text implements CanBeChild {

        private final XdmNode xdmNode;

        public Text(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.TEXT));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        public String value() {
            return xdmNode().getStringValue();
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.Text toGreenTreeNode() {
            return new GreenXmlTrees.Text(value());
        }

        @Override
        public GreenClarkTrees.Text toClarkNode() {
            return new GreenClarkTrees.Text(value());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Text otherText) {
                return otherText.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }
    }

    public static final class Comment implements CanBeChild {

        private final XdmNode xdmNode;

        public Comment(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.COMMENT));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        public String value() {
            return xdmNode().getStringValue();
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.Comment toGreenTreeNode() {
            return new GreenXmlTrees.Comment(value());
        }

        @Override
        public GreenClarkTrees.Comment toClarkNode() {
            return new GreenClarkTrees.Comment(value());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Comment otherComment) {
                return otherComment.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }
    }

    public static final class ProcessingInstruction implements CanBeChild {

        private final XdmNode xdmNode;

        public ProcessingInstruction(XdmNode xdmNode) {
            PreconditionUtil.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.PROCESSING_INSTRUCTION));
            this.xdmNode = xdmNode;
        }

        @Override
        public XdmNode xdmNode() {
            return xdmNode;
        }

        public String target() {
            return xdmNode.getUnderlyingNode().getDisplayName();
        }

        public String data() {
            return xdmNode.getStringValue();
        }

        @Override
        public boolean isElement() {
            return false;
        }

        @Override
        public GreenXmlTrees.ProcessingInstruction toGreenTreeNode() {
            return new GreenXmlTrees.ProcessingInstruction(target(), data());
        }

        @Override
        public GreenClarkTrees.ProcessingInstruction toClarkNode() {
            return new GreenClarkTrees.ProcessingInstruction(target(), data());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ProcessingInstruction otherPi) {
                return otherPi.xdmNode().equals(xdmNode());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return xdmNode.hashCode();
        }
    }
}
