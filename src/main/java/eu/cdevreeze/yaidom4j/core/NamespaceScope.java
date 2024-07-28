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

package eu.cdevreeze.yaidom4j.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import javax.xml.XMLConstants;
import java.util.Map;

/**
 * Mapping from namespace prefixes to namespace strings. Instances of this class are used to represent
 * namespaces that are in-scope (at a certain element in an XML DOM tree).
 * <p>
 * The empty namespace prefix is used to represent the default namespace. The namespace strings themselves
 * must not be empty strings.
 *
 * @author Chris de Vreeze
 */
public record NamespaceScope(ImmutableMap<String, String> inScopeNamespaces) {

    // TODO Consider implementing interface NamespaceContext

    public NamespaceScope {
        Preconditions.checkArgument(inScopeNamespaces.values().stream().noneMatch(String::isBlank));
        Preconditions.checkArgument(!inScopeNamespaces.containsKey(XMLConstants.XML_NS_PREFIX));
    }

    public NamespaceScope withoutDefaultNamespace() {
        if (inScopeNamespaces.containsKey(XMLConstants.DEFAULT_NS_PREFIX)) {
            return new NamespaceScope(
                    inScopeNamespaces.entrySet()
                            .stream()
                            .filter(kv -> !kv.getKey().isBlank())
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
        } else {
            return this;
        }
    }

    // TODO Other functional update methods and query methods

    // TODO Factory method that checks and removes "xml" prefix, if any
}
