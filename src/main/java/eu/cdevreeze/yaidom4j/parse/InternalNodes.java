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

package eu.cdevreeze.yaidom4j.parse;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.GreenNodes;
import org.jspecify.annotations.Nullable;

/**
 * Package private builders for immutable DOM nodes. This enables immutable DOM creation via SAX, for example.
 *
 * @author Chris de Vreeze
 */
class InternalNodes {

    private InternalNodes() {
    }

    sealed interface InternalNode permits InternalCanBeChild, InternalText {

        GreenNodes.Node toGreenTreeNode();
    }

    sealed interface InternalCanBeChild extends InternalNode {

        @Override
        GreenNodes.CanBeChild toGreenTreeNode();
    }

    static final class InternalElement implements InternalCanBeChild {

        private final @Nullable InternalElement parentElementOrNull;

        private final QName name;

        private final Map<QName, String> attributes;

        private final NamespaceScope namespaceScope;

        private final List<InternalNode> children = new ArrayList<>();

        InternalElement(
                @Nullable InternalElement parentElementOrNull,
                QName name,
                Map<QName, String> attributes,
                NamespaceScope namespaceScope
        ) {
            this.parentElementOrNull = parentElementOrNull;
            this.name = name;
            this.attributes = Map.copyOf(attributes);
            this.namespaceScope = namespaceScope;
        }

        Optional<InternalElement> getParentElementOption() {
            return Optional.ofNullable(parentElementOrNull);
        }

        NamespaceScope namespaceScope() {
            return namespaceScope;
        }

        void addChild(InternalNode childNode) {
            children.add(childNode);
        }

        @Override
        public GreenNodes.Element toGreenTreeNode() {
            // Recursive
            return new GreenNodes.Element(
                    name,
                    attributes,
                    namespaceScope,
                    children.stream().map(InternalNode::toGreenTreeNode).map(GreenNodes.Node::asCanBeChild).toList()
            );
        }
    }

    record InternalText(String value) implements InternalNode {

        @Override
        public GreenNodes.Text toGreenTreeNode() {
            return new GreenNodes.Text(value);
        }

    }

    record InternalComment(String value) implements InternalCanBeChild {

        @Override
        public GreenNodes.Comment toGreenTreeNode() {
            return new GreenNodes.Comment(value);
        }
    }

    record InternalProcessingInstruction(String target, String data) implements InternalCanBeChild {

        @Override
        public GreenNodes.ProcessingInstruction toGreenTreeNode() {
            return new GreenNodes.ProcessingInstruction(target, data);
        }
    }
}
