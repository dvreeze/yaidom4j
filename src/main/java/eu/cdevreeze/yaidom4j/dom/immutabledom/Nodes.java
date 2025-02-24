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
import java.net.URI;
import java.util.Optional;

/**
 * Friendly Node creation API. Shamelessly copied, adapted and enhanced from
 * <a href="https://www.saxonica.com/html/documentation12/javadoc/net/sf/saxon/sapling/Saplings.html">Saxon Sapling</a>.
 * So credits go to <a href="https://www.saxonica.com">Saxonica</a> for this API. On the other hand,
 * yaidom4j already fits this API perfectly, because in essence it is roughly based on the XPath Data Model
 * information items (slightly simplified), which deeply underlies the Saxon XML processor library.
 *
 * @author Chris de Vreeze
 */
public class Nodes {

    private Nodes() {
    }

    public static Document doc() {
        return new Document(Optional.empty(), ImmutableList.of());
    }

    public static Document doc(URI docUri) {
        return new Document(Optional.of(docUri), ImmutableList.of());
    }

    /**
     * Creates an empty Element node, taking the passed parent scope into consideration.
     *
     * @deprecated Use {@link Nodes#elem(QName)} instead
     */
    @Deprecated
    public static Element elem(QName name, NamespaceScope parentScope) {
        return new Element(
                name,
                ImmutableMap.of(),
                name.getNamespaceURI().isEmpty() ?
                        parentScope :
                        parentScope.resolve(name.getPrefix(), name.getNamespaceURI()),
                ImmutableList.of()
        );
    }

    /**
     * Creates an empty Element node. If the element name has a namespace, the prefix and namespace
     * make up the one-element namespace scope of the created element. Otherwise, the namespace scope
     * of the element is empty.
     * <p>
     * Consider calling {@link Element#deeplyUsingParentAttributeScope(NamespaceScope)} on the result
     * to safely add prefix-namespace bindings.
     */
    public static Element elem(QName name) {
        return new Element(
                name,
                ImmutableMap.of(),
                name.getNamespaceURI().isEmpty() ?
                        NamespaceScope.empty() :
                        NamespaceScope.of(name.getPrefix(), name.getNamespaceURI()),
                ImmutableList.of()
        );
    }

    public static Element elem(String noNamespaceName) {
        return elem(new QName(noNamespaceName));
    }

    public static Text text(String value, boolean isCData) {
        return new Text(value, isCData);
    }

    public static Text text(String value) {
        return text(value, false);
    }

    public static Comment comment(String value) {
        return new Comment(value);
    }

    public static ProcessingInstruction pi(String target, String data) {
        return new ProcessingInstruction(target, data);
    }
}
