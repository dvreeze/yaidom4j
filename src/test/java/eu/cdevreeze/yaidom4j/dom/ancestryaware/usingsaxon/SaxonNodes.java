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

package eu.cdevreeze.yaidom4j.dom.ancestryaware.usingsaxon;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Saxon node wrappers.
 *
 * @author Chris de Vreeze
 */
public class SaxonNodes {

    private SaxonNodes() {
    }

    public static Optional<? extends Node> optionallyFrom(XdmNode xdmNode) {
        return switch (xdmNode.getNodeKind()) {
            case ELEMENT -> Optional.of(new Element(xdmNode));
            case TEXT -> Optional.of(new Text(xdmNode));
            case COMMENT -> Optional.of(new Comment(xdmNode));
            case PROCESSING_INSTRUCTION -> Optional.of(new ProcessingInstruction(xdmNode));
            default -> Optional.empty();
        };
    }

    public sealed interface Node permits CanBeDocumentChild, Text {

        XdmNode xdmNode();

        boolean isElement();

        ClarkNodes.Node toClarkNode();
    }

    public sealed interface CanBeDocumentChild extends Node permits Element, Comment, ProcessingInstruction {
    }

    public static final class Element implements CanBeDocumentChild, AncestryAwareElementApi<Element> {

        private final XdmNode xdmNode;

        public Element(XdmNode xdmNode) {
            Preconditions.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.ELEMENT));
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
        public ClarkNodes.Element toClarkNode() {
            // Recursive
            return new ClarkNodes.Element(
                    elementName(),
                    attributes(),
                    childNodeStream().map(Node::toClarkNode).collect(ImmutableList.toImmutableList())
            );
        }

        @Override
        public QName elementName() {
            return xdmNode.getNodeName().getStructuredQName().toJaxpQName();
        }

        @Override
        public ImmutableMap<QName, String> attributes() {
            return xdmNode.axisIterator(Axis.ATTRIBUTE)
                    .stream()
                    .collect(
                            ImmutableMap.toImmutableMap(
                                    xdmAttrNode -> xdmAttrNode.getNodeName().getStructuredQName().toJaxpQName(),
                                    XdmItem::getStringValue
                            )
                    );
        }

        @Override
        public Optional<String> attributeOption(QName attrName) {
            return xdmNode.axisIterator(Axis.ATTRIBUTE, new net.sf.saxon.s9api.QName(attrName))
                    .stream()
                    .map(XdmItem::getStringValue)
                    .findAny();
        }

        @Override
        public String attribute(QName attrName) {
            return attributeOption(attrName).orElseThrow();
        }

        @Override
        public String text() {
            return xdmNode.axisIterator(Axis.CHILD)
                    .stream()
                    .filter(n -> n.getNodeKind().equals(XdmNodeKind.TEXT))
                    .map(XdmNode::getStringValue)
                    .collect(Collectors.joining());
        }

        @Override
        public Stream<Node> childNodeStream() {
            return xdmNode.axisIterator(Axis.CHILD)
                    .stream()
                    .flatMap(n -> optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> elementStream() {
            return descendantElementOrSelfStream();
        }

        @Override
        public Stream<Element> elementStream(Predicate<Element> predicate) {
            return descendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> topmostElementStream(Predicate<Element> predicate) {
            return topmostDescendantElementOrSelfStream(predicate);
        }

        @Override
        public Stream<Element> childElementStream() {
            return xdmNode.select(SaxonElementSteps.childElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> childElementStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.childElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream() {
            return xdmNode.select(SaxonElementSteps.descendantOrSelfElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementOrSelfStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.descendantOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementStream() {
            return xdmNode.select(SaxonElementSteps.descendantElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> descendantElementStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.descendantElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> topmostDescendantElementOrSelfStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.topmostDescendantOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> topmostDescendantElementStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.topmostDescendantElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Optional<Element> parentElementOption() {
            return xdmNode.select(SaxonElementSteps.parentElements()).flatMap(n -> Element.optionallyFrom(n).stream()).findFirst();
        }

        @Override
        public Stream<Element> ancestorElementOrSelfStream() {
            return xdmNode.select(SaxonElementSteps.ancestorOrSelfElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorElementOrSelfStream(Predicate<Element> predicate) {
            return xdmNode.select(SaxonElementSteps.ancestorOrSelfElements(n -> Element.optionallyFrom(n).map(predicate::test).orElse(false)))
                    .flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorElementStream() {
            return xdmNode.select(SaxonElementSteps.ancestorElements()).flatMap(n -> Element.optionallyFrom(n).stream());
        }

        @Override
        public Stream<Element> ancestorElementStream(Predicate<Element> predicate) {
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

    public static final class Text implements Node {

        private final XdmNode xdmNode;

        public Text(XdmNode xdmNode) {
            Preconditions.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.TEXT));
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
        public ClarkNodes.Text toClarkNode() {
            return new ClarkNodes.Text(value());
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

    public static final class Comment implements CanBeDocumentChild {

        private final XdmNode xdmNode;

        public Comment(XdmNode xdmNode) {
            Preconditions.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.COMMENT));
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
        public ClarkNodes.Comment toClarkNode() {
            return new ClarkNodes.Comment(value());
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

    public static final class ProcessingInstruction implements CanBeDocumentChild {

        private final XdmNode xdmNode;

        public ProcessingInstruction(XdmNode xdmNode) {
            Preconditions.checkArgument(xdmNode.getNodeKind().equals(XdmNodeKind.PROCESSING_INSTRUCTION));
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
        public ClarkNodes.ProcessingInstruction toClarkNode() {
            return new ClarkNodes.ProcessingInstruction(target(), data());
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
