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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * XML element node.
 *
 * @author Chris de Vreeze
 */
public record Element(
        QName name,
        ImmutableMap<QName, String> attributes,
        NamespaceScope namespaceScope,
        ImmutableList<Node> children
) implements CanBeDocumentChild {

    public Element {
        Objects.requireNonNull(name);
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(namespaceScope);
        Objects.requireNonNull(children);

        Preconditions.checkArgument(
                name.getPrefix().isEmpty() ||
                        name.getPrefix().equals(XMLConstants.XML_NS_PREFIX) ||
                        namespaceScope.inScopeNamespaces().containsKey(name.getPrefix())
        );

        Preconditions.checkArgument(
                attributes.keySet().stream()
                        .allMatch(attrName ->
                                attrName.getPrefix().isEmpty() ||
                                        attrName.getPrefix().equals(XMLConstants.XML_NS_PREFIX) ||
                                        namespaceScope.inScopeNamespaces().containsKey(attrName.getPrefix())));

        Preconditions.checkArgument(
                attributes.keySet().stream()
                        .allMatch(attrName -> attrName.getNamespaceURI().isEmpty() || !attrName.getPrefix().isEmpty())
        );
    }

    @Override
    public boolean isElement() {
        return true;
    }
}
