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

package eu.cdevreeze.yaidom4j.dom.clark.jaxpinterop;

import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import org.jspecify.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Package private builders for immutable Clark nodes. This enables immutable Clark DOM creation via SAX, for example.
 *
 * @author Chris de Vreeze
 */
class InternalNodes {

    private InternalNodes() {
    }

    sealed interface InternalNode permits InternalElement, InternalComment, InternalProcessingInstruction, InternalText {

        ClarkNodes.Node toNode();
    }

    static final class InternalElement implements InternalNode {

        private final @Nullable InternalElement parentElementOrNull;

        private final QName name;

        private final Map<QName, String> attributes;

        private final List<InternalNode> children = new ArrayList<>();

        InternalElement(
                @Nullable InternalElement parentElementOrNull,
                QName name,
                Map<QName, String> attributes
        ) {
            this.parentElementOrNull = parentElementOrNull;
            this.name = name;
            this.attributes = Map.copyOf(attributes);
        }

        Optional<InternalElement> getParentElementOption() {
            return Optional.ofNullable(parentElementOrNull);
        }

        void addChild(InternalNode childNode) {
            children.add(childNode);
        }

        @Override
        public ClarkNodes.Element toNode() {
            // Recursive
            return new ClarkNodes.Element(
                    name,
                    attributes,
                    children.stream().map(InternalNode::toNode).toList()
            );
        }
    }

    record InternalText(String value, boolean isCData) implements InternalNode {

        @Override
        public ClarkNodes.Text toNode() {
            return new ClarkNodes.Text(value);
        }

    }

    record InternalComment(String value) implements InternalNode {

        @Override
        public ClarkNodes.Comment toNode() {
            return new ClarkNodes.Comment(value);
        }
    }

    record InternalProcessingInstruction(String target, String data) implements InternalNode {

        @Override
        public ClarkNodes.ProcessingInstruction toNode() {
            return new ClarkNodes.ProcessingInstruction(target, data);
        }
    }
}
