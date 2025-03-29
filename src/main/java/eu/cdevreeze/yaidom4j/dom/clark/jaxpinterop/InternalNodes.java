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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
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

        private final Optional<InternalElement> parentElementOption;

        private final QName name;

        private final ImmutableMap<QName, String> attributes;

        private final List<InternalNode> children = new ArrayList<>();

        InternalElement(
                Optional<InternalElement> parentElementOption,
                QName name,
                ImmutableMap<QName, String> attributes
        ) {
            this.parentElementOption = parentElementOption;
            this.name = name;
            this.attributes = attributes;
        }

        Optional<InternalElement> getParentElementOption() {
            return parentElementOption;
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
                    children.stream().map(InternalNode::toNode).collect(ImmutableList.toImmutableList())
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
