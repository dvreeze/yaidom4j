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

package eu.cdevreeze.yaidom4j.core;

import eu.cdevreeze.yaidom4j.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.internal.SetOperations;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mapping from namespace prefixes to namespace strings. Instances of this class are used to represent
 * namespaces that are in-scope (at a certain element in an XML DOM tree).
 * <p>
 * The empty namespace prefix is used to represent the default namespace. The namespace strings themselves
 * must not be empty strings.
 * <p>
 * This class does not implement interface {@link javax.xml.namespace.NamespaceContext}, because class {@link NamespaceScope}
 * does not consider "namespace declaration attributes" to be attributes.
 * <p>
 * This class is immutable, due to the fact that record component "inScopeNamespaces" is an "unmodifiable map".
 *
 * @author Chris de Vreeze
 */
public record NamespaceScope(Map<String, String> inScopeNamespaces) {

    public NamespaceScope {
        inScopeNamespaces = Map.copyOf(inScopeNamespaces);
        PreconditionUtil.checkArgument(inScopeNamespaces.values().stream().noneMatch(String::isEmpty));
        PreconditionUtil.checkArgument(!inScopeNamespaces.containsKey(XMLConstants.XML_NS_PREFIX));
        PreconditionUtil.checkArgument(!inScopeNamespaces.containsKey(XMLConstants.XMLNS_ATTRIBUTE));
    }

    public Optional<String> defaultNamespaceOption() {
        return findNamespaceOfPrefix(XMLConstants.DEFAULT_NS_PREFIX);
    }

    public NamespaceScope withoutDefaultNamespace() {
        if (inScopeNamespaces.containsKey(XMLConstants.DEFAULT_NS_PREFIX)) {
            return withoutPrefix(XMLConstants.DEFAULT_NS_PREFIX);
        } else {
            return this;
        }
    }

    public Optional<String> findNamespaceOfPrefix(String prefix) {
        PreconditionUtil.checkArgument(!XMLConstants.XMLNS_ATTRIBUTE.equals(prefix));

        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return Optional.of(XMLConstants.XML_NS_URI);
        } else {
            return Optional.ofNullable(inScopeNamespaces.get(prefix));
        }
    }

    /**
     * Returns true if this namespace scope and the parameter QName agree on the prefix-namespace
     * binding, given the parameter QName is an element name.
     * <p>
     * If the QName has a non-empty prefix but no namespace, an exception is thrown.
     */
    public boolean allowsElementName(QName name) {
        PreconditionUtil.checkArgument(QNames.hasNamespaceIfPrefixNonEmpty(name));

        return findNamespaceOfPrefix(name.getPrefix())
                .orElse(XMLConstants.NULL_NS_URI)
                .equals(name.getNamespaceURI());
    }

    /**
     * Returns true if this namespace scope without default namespace and the parameter QName agree
     * on the prefix-namespace binding, given the parameter QName is an attribute name.
     * <p>
     * If the QName has a non-empty prefix but no namespace, an exception is thrown.
     */
    public boolean allowsAttributeName(QName name) {
        PreconditionUtil.checkArgument(QNames.hasNamespaceIfPrefixNonEmpty(name));

        if (name.getPrefix().equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI());
        }
        return findNamespaceOfPrefix(name.getPrefix())
                .orElse(XMLConstants.NULL_NS_URI)
                .equals(name.getNamespaceURI());
    }

    /**
     * Functionally updates the namespace scope with the parameter prefix and corresponding namespace.
     * <p>
     * If the namespace is an empty string, this is a namespace un-declaration. In XML 1.0 this is not
     * allowed if the prefix is non-empty, but this method does not prevent that.
     */
    public NamespaceScope resolve(String prefix, String namespace) {
        PreconditionUtil.checkArgument(!XMLConstants.XMLNS_ATTRIBUTE.equals(prefix));

        if (namespace.isEmpty()) {
            return new NamespaceScope(
                    inScopeNamespaces.entrySet().stream()
                            .filter(kv -> !kv.getKey().equals(prefix))
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }

        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            PreconditionUtil.checkArgument(namespace.equals(XMLConstants.XML_NS_URI));
            return this;
        } else {
            if (inScopeNamespaces.containsKey(prefix) && namespace.equals(inScopeNamespaces.get(prefix))) {
                return this; // No unnecessary object creation needed in most cases
            } else {
                Map<String, String> newInScopeNamespaces = new HashMap<>(inScopeNamespaces);
                newInScopeNamespaces.put(prefix, namespace);
                return new NamespaceScope(Map.copyOf(newInScopeNamespaces));
            }
        }
    }

    public NamespaceScope withoutPrefix(String prefix) {
        return new NamespaceScope(
                this.inScopeNamespaces.entrySet().stream()
                        .filter(kv -> !prefix.equals(kv.getKey()))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    /**
     * Returns the result of applying the overloaded {@link NamespaceScope#resolve} function for all prefix-namespace pairs
     * in the parameter prefix-namespace map. This may include namespace un-declarations, but mind the
     * possibility that the result is not valid in XML 1.0 (in which case first calling method
     * {@link NamespaceScope#withoutPrefixedNamespaceUndeclarations} on the parameter mapping helps to make the result valid in
     * XML 1.0).
     */
    public NamespaceScope resolve(Map<String, String> prefixNamespaceMap) {
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
    public Map<String, String> relativize(NamespaceScope other) {
        if (this.equals(other)) {
            return Map.of();
        }

        Set<String> undeclaredPrefixes =
                SetOperations.difference(inScopeNamespaces.keySet(), other.inScopeNamespaces.keySet());
        Map<String, String> namespaceUndeclarations = undeclaredPrefixes.stream()
                .map(pref -> Map.entry(pref, ""))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> updatedPrefixes =
                SetOperations.intersection(inScopeNamespaces.keySet(), other.inScopeNamespaces.keySet());
        Map<String, String> updatedNamespaceDeclarations = updatedPrefixes.stream()
                .map(pref -> Map.entry(pref, Objects.requireNonNull(other.inScopeNamespaces.get(pref))))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> newPrefixes =
                SetOperations.difference(other.inScopeNamespaces.keySet(), inScopeNamespaces.keySet());
        Map<String, String> newNamespaceDeclarations = newPrefixes.stream()
                .map(pref -> Map.entry(pref, Objects.requireNonNull(other.inScopeNamespaces.get(pref))))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> mappings = new HashMap<>();
        mappings.putAll(namespaceUndeclarations);
        mappings.putAll(updatedNamespaceDeclarations);
        mappings.putAll(newNamespaceDeclarations);
        return Map.copyOf(mappings);
    }

    /**
     * Resolves the given syntactic element QName, given this namespace scope.
     */
    public QName resolveSyntacticElementQName(String syntacticQName) {
        String[] parts = syntacticQName.split(Pattern.quote(":"));
        PreconditionUtil.checkArgument(parts.length >= 1 && parts.length <= 2);

        if (parts.length == 1) {
            return defaultNamespaceOption().map(ns -> new QName(ns, parts[0])).orElse(new QName(parts[0]));
        } else if (parts[0].equals(XMLConstants.XML_NS_PREFIX)) {
            return new QName(XMLConstants.XML_NS_URI, parts[1], XMLConstants.XML_NS_PREFIX);
        } else {
            PreconditionUtil.checkArgument(inScopeNamespaces.containsKey(parts[0]));
            String ns = inScopeNamespaces.get(parts[0]);
            return new QName(ns, parts[1], parts[0]);
        }
    }

    /**
     * Resolves the given syntactic attribute QName, given this namespace scope.
     */
    public QName resolveSyntacticAttributeQName(String syntacticQName) {
        String[] parts = syntacticQName.split(Pattern.quote(":"));
        PreconditionUtil.checkArgument(parts.length >= 1 && parts.length <= 2);

        if (parts.length == 1) {
            return new QName(parts[0]);
        } else if (parts[0].equals(XMLConstants.XML_NS_PREFIX)) {
            return new QName(XMLConstants.XML_NS_URI, parts[1], XMLConstants.XML_NS_PREFIX);
        } else {
            PreconditionUtil.checkArgument(withoutDefaultNamespace().inScopeNamespaces.containsKey(parts[0]));
            String ns = withoutDefaultNamespace().inScopeNamespaces.get(parts[0]);
            return new QName(ns, parts[1], parts[0]);
        }
    }

    /**
     * Resolves the given syntactic QName when used in "text content", given this namespace scope.
     * <p>
     * In this context "text content" refers to attribute values or element content text. Usually an XML Schema
     * is used to recognize and make sense of such content.
     * <p>
     * This method delegates to {@link NamespaceScope#resolveSyntacticElementQName}.
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
     * Removes namespace un-declarations for non-empty prefixes. They are not allowed in XML 1.0. In XML 1.0 only the
     * default namespace can be un-declared. Hence, this method makes the namespace (un-)declarations valid in XML 1.0.
     * <p>
     * The result is returned as a new "unmodifiable map".
     */
    public static Map<String, String> withoutPrefixedNamespaceUndeclarations(
            Map<String, String> prefixNamespaceMap
    ) {
        if (prefixNamespaceMap.entrySet().stream().anyMatch(kv -> kv.getValue().isEmpty())) {
            return prefixNamespaceMap.entrySet().stream()
                    .filter(kv -> !kv.getValue().isEmpty() || kv.getKey().equals(XMLConstants.DEFAULT_NS_PREFIX))
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return Map.copyOf(prefixNamespaceMap);
        }
    }

    /**
     * Factory method that removes the "xml" prefix from the parameter prefix-namespace map, if any
     * (but nonetheless checking that prefix "xml" refers to the correct namespace). Hence, this factory
     * method is more lenient than the constructor, which does not allow the "xml" prefix to be passed.
     */
    public static NamespaceScope from(Map<String, String> inScopeNamespaces) {
        PreconditionUtil.checkArgument(
                Optional.ofNullable(inScopeNamespaces.get(XMLConstants.XML_NS_PREFIX))
                        .stream()
                        .allMatch(ns -> ns.equals(XMLConstants.XML_NS_URI))
        );

        return new NamespaceScope(
                inScopeNamespaces.entrySet().stream()
                        .filter(kv -> !kv.getKey().equals(XMLConstants.XML_NS_PREFIX))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public static NamespaceScope of(String prefix, String namespace) {
        return NamespaceScope.EMPTY.resolve(prefix, namespace);
    }

    private static final NamespaceScope EMPTY = new NamespaceScope(Map.of());

    public static NamespaceScope empty() {
        return EMPTY;
    }
}
