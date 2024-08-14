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
import com.google.common.collect.Sets;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mapping from namespace prefixes to namespace strings. Instances of this class are used to represent
 * namespaces that are in-scope (at a certain element in an XML DOM tree).
 * <p>
 * The empty namespace prefix is used to represent the default namespace. The namespace strings themselves
 * must not be empty strings.
 * <p>
 * This class does not implement interface NamespaceContext, because class NamespaceScope does not consider
 * "namespace declaration attributes" to be attributes.
 *
 * @author Chris de Vreeze
 */
public record NamespaceScope(ImmutableMap<String, String> inScopeNamespaces) {

    public NamespaceScope {
        Preconditions.checkArgument(inScopeNamespaces.values().stream().noneMatch(String::isEmpty));
        Preconditions.checkArgument(!inScopeNamespaces.containsKey(XMLConstants.XML_NS_PREFIX));
        Preconditions.checkArgument(!inScopeNamespaces.containsKey(XMLConstants.XMLNS_ATTRIBUTE));
    }

    public Optional<String> defaultNamespaceOption() {
        return Optional.ofNullable(inScopeNamespaces.get(XMLConstants.DEFAULT_NS_PREFIX));
    }

    public NamespaceScope withoutDefaultNamespace() {
        if (inScopeNamespaces.containsKey(XMLConstants.DEFAULT_NS_PREFIX)) {
            return withoutPrefix(XMLConstants.DEFAULT_NS_PREFIX);
        } else {
            return this;
        }
    }

    /**
     * Functionally updates the namespace scope with the parameter prefix and corresponding namespace.
     * <p>
     * If the namespace is an empty string, this is a namespace un-declaration. In XML 1.0 this is not
     * allowed if the prefix is non-empty, but this method does not prevent that.
     */
    public NamespaceScope resolve(String prefix, String namespace) {
        if (namespace.isEmpty()) {
            return new NamespaceScope(
                    inScopeNamespaces.entrySet().stream()
                            .filter(kv -> !kv.getKey().equals(prefix))
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }

        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            Preconditions.checkArgument(namespace.equals(XMLConstants.XML_NS_URI));
            return this;
        } else {
            if (inScopeNamespaces.containsKey(prefix) && namespace.equals(inScopeNamespaces.get(prefix))) {
                return this; // No unnecessary object creation in most cases
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
     * in the parameter prefix-namespace map. This may include namespace un-declarations, but mind the
     * possibility that the result is not valid in XML 1.0 (in which case first calling method
     * "withoutPrefixedNamespaceUndeclarations" on the parameter mapping helps to make the result valid in
     * XML 1.0).
     */
    public NamespaceScope resolve(ImmutableMap<String, String> prefixNamespaceMap) {
        NamespaceScope accScope = this;
        for (var prefixNamespace : prefixNamespaceMap.entrySet()) {
            accScope = accScope.resolve(prefixNamespace.getKey(), prefixNamespace.getValue());
        }
        return accScope;
    }

    /**
     * Returns the "namespace (un-)declarations" such that resolving them against this scope returns
     * the given parameter scope. The result may be invalid in XML 1.0, in which case removing namespace
     * un-declarations for prefixed namespaces would rectify that.
     */
    public ImmutableMap<String, String> relativize(NamespaceScope other) {
        if (this.equals(other)) {
            return ImmutableMap.of();
        }

        Set<String> undeclaredPrefixes =
                Sets.difference(inScopeNamespaces.keySet(), other.inScopeNamespaces.keySet());
        Map<String, String> namespaceUndeclarations = undeclaredPrefixes.stream()
                .map(pref -> Map.entry(pref, ""))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> updatedPrefixes =
                Sets.intersection(inScopeNamespaces.keySet(), other.inScopeNamespaces.keySet());
        Map<String, String> updatedNamespaceDeclarations = updatedPrefixes.stream()
                .map(pref -> Map.entry(pref, Objects.requireNonNull(other.inScopeNamespaces.get(pref))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> newPrefixes =
                Sets.difference(other.inScopeNamespaces.keySet(), inScopeNamespaces.keySet());
        Map<String, String> newNamespaceDeclarations = newPrefixes.stream()
                .map(pref -> Map.entry(pref, Objects.requireNonNull(other.inScopeNamespaces.get(pref))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return ImmutableMap.<String, String>builder()
                .putAll(namespaceUndeclarations)
                .putAll(updatedNamespaceDeclarations)
                .putAll(newNamespaceDeclarations)
                .build();
    }

    /**
     * Resolves the given syntactic element QName, given this namespace scope.
     */
    public QName resolveSyntacticElementQName(String syntacticQName) {
        String[] parts = syntacticQName.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);

        if (parts.length == 1) {
            return defaultNamespaceOption().map(ns -> new QName(ns, parts[0])).orElse(new QName(parts[0]));
        } else if (parts[0].equals(XMLConstants.XML_NS_PREFIX)) {
            return new QName(XMLConstants.XML_NS_URI, parts[1], XMLConstants.XML_NS_PREFIX);
        } else {
            Preconditions.checkArgument(inScopeNamespaces.containsKey(parts[0]));
            String ns = inScopeNamespaces.get(parts[0]);
            return new QName(ns, parts[1], parts[0]);
        }
    }

    /**
     * Resolves the given syntactic attribute QName, given this namespace scope.
     */
    public QName resolveSyntacticAttributeQName(String syntacticQName) {
        String[] parts = syntacticQName.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);

        if (parts.length == 1) {
            return new QName(parts[0]);
        } else if (parts[0].equals(XMLConstants.XML_NS_PREFIX)) {
            return new QName(XMLConstants.XML_NS_URI, parts[1], XMLConstants.XML_NS_PREFIX);
        } else {
            Preconditions.checkArgument(withoutDefaultNamespace().inScopeNamespaces.containsKey(parts[0]));
            String ns = withoutDefaultNamespace().inScopeNamespaces.get(parts[0]);
            return new QName(ns, parts[1], parts[0]);
        }
    }

    /**
     * Resolves the given syntactic QName when used in "text content", given this namespace scope.
     * <p>
     * With "text content" attribute values and element content text is meant. Usually an XML Schema
     * is used to recognize and make sense of such content.
     * <p>
     * This method delegates to "resolveSyntacticElementQName".
     */
    public QName resolveSyntacticQNameInContent(String syntacticQName) {
        return resolveSyntacticElementQName(syntacticQName);
    }

    public boolean subScopeOf(NamespaceScope other) {
        return other.inScopeNamespaces.keySet().containsAll(inScopeNamespaces.keySet()) &&
                inScopeNamespaces.keySet().stream().allMatch(pref ->
                        Objects.equals(inScopeNamespaces.get(pref), other.inScopeNamespaces.get(pref)));
    }

    // TODO Other functional update methods and query methods

    /**
     * Removes namespace un-declarations that are not allowed in XML 1.0. In XML 1.0 only the default
     * namespace can be un-declared. Hence, this method makes the namespace (un-)declarations valid in XML 1.0.
     */
    public static ImmutableMap<String, String> withoutPrefixedNamespaceUndeclarations(
            ImmutableMap<String, String> prefixNamespaceMap
    ) {
        if (prefixNamespaceMap.entrySet().stream().anyMatch(kv -> kv.getValue().isEmpty())) {
            return prefixNamespaceMap.entrySet().stream()
                    .filter(kv -> !kv.getValue().isEmpty() || kv.getKey().equals(XMLConstants.DEFAULT_NS_PREFIX))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return prefixNamespaceMap;
        }
    }

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
