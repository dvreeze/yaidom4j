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

package eu.cdevreeze.yaidom4j.queryapi;

import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Object-oriented element query API. This API is implemented by yaidom4j
 * element node implementations, but it can also be implemented by other element implementations
 * (such as Saxon XDM item wrappers) or even domain-specific element hierarchies (such as
 * the elements in an XML Schema, whose common super-type extends/implements this query API interface).
 *
 * @param <E> The specific element type itself
 * @author Chris de Vreeze
 */
public interface ElementApi<E extends ElementApi<E>> {

    QName elementName();

    ImmutableMap<QName, String> attributes();

    Optional<String> attributeOption(QName attrName);

    String attribute(QName attrName);

    /**
     * Returns the concatenated text of the text children (ignoring other child nodes than text nodes)
     */
    String text();

    /**
     * Returns the NamespaceScope, if any. Most element implementations should have one, so content
     * (text content or attribute values) that represents QNames can be resolved as QName. Note
     * that "Clark elements" do not have any NamespaceScope. Hence, this method returns an optional result.
     */
    Optional<NamespaceScope> namespaceScopeOption();

    /**
     * Alias of {@link ElementApi#descendantElementOrSelfStream()}
     */
    Stream<E> elementStream();

    /**
     * Alias of {@link ElementApi#descendantElementOrSelfStream(Predicate)}
     */
    Stream<E> elementStream(Predicate<? super E> predicate);

    /**
     * Alias of {@link ElementApi#topmostDescendantElementOrSelfStream(Predicate)}
     */
    Stream<E> topmostElementStream(Predicate<? super E> predicate);

    Stream<E> childElementStream();

    Stream<E> childElementStream(Predicate<? super E> predicate);

    Stream<E> descendantElementOrSelfStream();

    Stream<E> descendantElementOrSelfStream(Predicate<? super E> predicate);

    Stream<E> descendantElementStream();

    Stream<E> descendantElementStream(Predicate<? super E> predicate);

    Stream<E> topmostDescendantElementOrSelfStream(Predicate<? super E> predicate);

    Stream<E> topmostDescendantElementStream(Predicate<? super E> predicate);
}
