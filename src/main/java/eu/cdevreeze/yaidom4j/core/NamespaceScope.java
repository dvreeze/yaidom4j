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
import java.util.Optional;

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
        Preconditions.checkArgument(
                inScopeNamespaces.keySet().stream().noneMatch(pref -> pref.startsWith(XMLConstants.XMLNS_ATTRIBUTE))
        );
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

    /**
     * Functionally updates the namespace scope with the parameter prefix and corresponding namespace.
     * The namespace must not be an empty string, so this method cannot be used for un-declaring namespaces.
     */
    public NamespaceScope resolve(String prefix, String namespace) {
        Preconditions.checkArgument(!namespace.isBlank());

        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            Preconditions.checkArgument(namespace.equals(XMLConstants.XML_NS_URI));
            return this;
        } else {
            if (inScopeNamespaces.containsKey(prefix) && namespace.equals(inScopeNamespaces.get(prefix))) {
                return this; // No unnecessary object creation
            } else {
                return new NamespaceScope(
                        ImmutableMap.<String, String>builder()
                                .putAll(this.inScopeNamespaces)
                                .put(prefix, namespace)
                                .buildKeepingLast()
                );
            }
        }
    }

    public NamespaceScope withoutPrefix(String prefix) {
        return new NamespaceScope(
                this.inScopeNamespaces.entrySet().stream()
                        .filter(kv -> !prefix.equals(kv.getKey()))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    /**
     * Returns the result of applying the overloaded "resolve" function for all prefix-namespace pairs
     * in the parameter prefix-namespace map.
     */
    public NamespaceScope resolve(ImmutableMap<String, String> prefixNamespaceMap) {
        NamespaceScope accScope = this;
        for (var prefixNamespace : prefixNamespaceMap.entrySet()) {
            accScope = accScope.resolve(prefixNamespace.getKey(), prefixNamespace.getValue());
        }
        return accScope;
    }

    // TODO Other functional update methods and query methods

    /**
     * Factory method that removes the "xml" prefix from the parameter prefix-namespace map, if any
     * (but nonetheless checking that prefix "xml" refers to the correct namespace).
     */
    public static NamespaceScope from(ImmutableMap<String, String> inScopeNamespaces) {
        Preconditions.checkArgument(
                Optional.ofNullable(inScopeNamespaces.get(XMLConstants.XML_NS_PREFIX))
                        .stream()
                        .allMatch(ns -> ns.equals(XMLConstants.XML_NS_URI))
        );

        return new NamespaceScope(
                inScopeNamespaces.entrySet().stream()
                        .filter(kv -> !kv.getKey().equals(XMLConstants.XML_NS_PREFIX))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private static final NamespaceScope EMPTY = new NamespaceScope(ImmutableMap.of());

    public static NamespaceScope empty() {
        return EMPTY;
    }
}
