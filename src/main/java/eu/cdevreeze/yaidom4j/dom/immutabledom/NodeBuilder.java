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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * XML node builder, retaining a NamespaceScope.
 *
 * @author Chris de Vreeze
 */
public class NodeBuilder {

    private final NamespaceScope namespaceScope;

    public NodeBuilder(NamespaceScope namespaceScope) {
        this.namespaceScope = namespaceScope;
    }

    public NodeBuilder resolve(ImmutableMap<String, String> prefixNamespaceMap) {
        return new NodeBuilder(namespaceScope.resolve(prefixNamespaceMap));
    }

    public NodeBuilder resolve(String prefix, String namespace) {
        return resolve(ImmutableMap.of(prefix, namespace));
    }

    public Element element(QName name, ImmutableMap<QName, String> attributes, ImmutableList<Node> children) {
        return new Element(name, attributes, namespaceScope, children);
    }

    public Element element(QName name, ImmutableMap<QName, String> attributes) {
        return element(name, attributes, ImmutableList.of());
    }

    public Element element(QName name) {
        return element(name, ImmutableMap.of());
    }

    public Element textElement(QName name, ImmutableMap<QName, String> attributes, String textValue) {
        return element(name, attributes, ImmutableList.of(new Text(textValue, false)));
    }

    public Element textElement(QName name, String textValue) {
        return textElement(name, ImmutableMap.of(), textValue);
    }

    public Text text(String textValue, boolean isCData) {
        return new Text(textValue, isCData);
    }

    public Text text(String textValue) {
        return text(textValue, false);
    }

    public Comment comment(String commentValue) {
        return new Comment(commentValue);
    }

    public ProcessingInstruction processingInstruction(String target, String data) {
        return new ProcessingInstruction(target, data);
    }

    public static NodeBuilder empty() {
        return new NodeBuilder(NamespaceScope.empty());
    }

    /**
     * Concise API that uses syntactic names for elements and attributes.
     */
    public static final class ConciseApi {

        private final NamespaceScope namespaceScope;

        public ConciseApi(NamespaceScope namespaceScope) {
            this.namespaceScope = namespaceScope;
        }

        public ConciseApi resolve(ImmutableMap<String, String> prefixNamespaceMap) {
            return new ConciseApi(namespaceScope.resolve(prefixNamespaceMap));
        }

        public ConciseApi resolve(String prefix, String namespace) {
            return resolve(ImmutableMap.of(prefix, namespace));
        }

        public Element element(String syntacticName, ImmutableMap<String, String> attrs, ImmutableList<Node> children) {
            QName name = resolveSyntacticElementName(syntacticName);
            ImmutableMap<QName, String> attributes = attrs.entrySet().stream()
                    .map(kv -> Map.entry(resolveSyntacticAttributeName(kv.getKey()), kv.getValue()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            return new Element(name, attributes, namespaceScope, children);
        }

        public Element element(String syntacticName, ImmutableMap<String, String> attrs) {
            return element(syntacticName, attrs, ImmutableList.of());
        }

        public Element element(String syntacticName) {
            return element(syntacticName, ImmutableMap.of());
        }

        public Element textElement(String syntacticName, ImmutableMap<String, String> attrs, String textValue) {
            return element(syntacticName, attrs, ImmutableList.of(new Text(textValue, false)));
        }

        public Element textElement(String syntacticName, String textValue) {
            return textElement(syntacticName, ImmutableMap.of(), textValue);
        }

        public Text text(String textValue, boolean isCData) {
            return new Text(textValue, isCData);
        }

        public Text text(String textValue) {
            return text(textValue, false);
        }

        public Comment comment(String commentValue) {
            return new Comment(commentValue);
        }

        public ProcessingInstruction processingInstruction(String target, String data) {
            return new ProcessingInstruction(target, data);
        }

        public static ConciseApi empty() {
            return new ConciseApi(NamespaceScope.empty());
        }

        private QName resolveSyntacticElementName(String syntacticName) {
            return namespaceScope.resolveSyntacticElementQName(syntacticName);
        }

        private QName resolveSyntacticAttributeName(String syntacticName) {
            return namespaceScope.resolveSyntacticAttributeQName(syntacticName);
        }
    }
}
