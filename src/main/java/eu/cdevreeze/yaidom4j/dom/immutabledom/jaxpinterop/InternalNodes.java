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

package eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.*;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Package private builders for immutable DOM nodes. This enables immutable DOM creation via SAX, for example.
 *
 * @author Chris de Vreeze
 */
class InternalNodes {

    private InternalNodes() {
    }

    sealed interface InternalNode permits InternalCanBeDocumentChild, InternalText {

        Node toNode();
    }

    sealed interface InternalCanBeDocumentChild extends InternalNode {

        @Override
        CanBeDocumentChild toNode();
    }

    static final class InternalElement implements InternalCanBeDocumentChild {

        private final Optional<InternalElement> parentElementOption;

        private final QName name;

        private final ImmutableMap<QName, String> attributes;

        private final NamespaceScope namespaceScope;

        private final List<InternalNode> children = new ArrayList<>();

        InternalElement(
                Optional<InternalElement> parentElementOption,
                QName name,
                ImmutableMap<QName, String> attributes,
                NamespaceScope namespaceScope
        ) {
            this.parentElementOption = parentElementOption;
            this.name = name;
            this.attributes = attributes;
            this.namespaceScope = namespaceScope;
        }

        Optional<InternalElement> getParentElementOption() {
            return parentElementOption;
        }

        NamespaceScope namespaceScope() {
            return namespaceScope;
        }

        void addChild(InternalNode childNode) {
            children.add(childNode);
        }

        @Override
        public Element toNode() {
            // Recursive
            return new Element(
                    name,
                    attributes,
                    namespaceScope,
                    children.stream().map(InternalNode::toNode).collect(ImmutableList.toImmutableList())
            );
        }
    }

    record InternalText(String value, boolean isCData) implements InternalNode {

        @Override
        public Text toNode() {
            return new Text(value, isCData);
        }

    }

    record InternalComment(String value) implements InternalCanBeDocumentChild {

        @Override
        public Comment toNode() {
            return new Comment(value);
        }
    }

    record InternalProcessingInstruction(String target, String data) implements InternalCanBeDocumentChild {

        @Override
        public ProcessingInstruction toNode() {
            return new ProcessingInstruction(target, data);
        }
    }
}
