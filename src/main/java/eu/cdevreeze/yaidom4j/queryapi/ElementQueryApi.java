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

import javax.xml.namespace.QName;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Simple API contract for querying XML element nodes, where the element node type is type parameter "E".
 * <p>
 * Most methods return a Java Stream. These methods are inspired by XPath axes such as descendant-or-self
 * and children, except that only element nodes are returned. The only exception to the latter is method
 * "childNodeStream", which returns a Java Stream of all child nodes, including text nodes, comments etc.
 * <p>
 * Each method below that returns a Java Stream should return a fresh new Stream on each call of that method.
 *
 * @param <E>
 * @author Chris de Vreeze
 */
public interface ElementQueryApi<E> {

    QName elementName(E element);

    ImmutableMap<QName, String> attributes(E element);

    Stream<? super E> childNodeStream(E element);

    // Aliases of other stream-returning methods

    /**
     * Alias of descendantElementOrSelfStream
     */
    default Stream<E> elementStream(E element) {
        return descendantElementOrSelfStream(element);
    }

    /**
     * Alias of descendantElementOrSelfStream
     */
    default Stream<E> elementStream(E element, Predicate<E> predicate) {
        return descendantElementOrSelfStream(element, predicate);
    }

    /**
     * Alias of topmostDescendantElementOrSelfStream
     */
    default Stream<E> topmostElementStream(E element, Predicate<E> predicate) {
        return topmostDescendantElementOrSelfStream(element, predicate);
    }

    // Specific stream-returning methods

    Stream<E> childElementStream(E element);

    Stream<E> childElementStream(E element, Predicate<E> predicate);

    Stream<E> descendantElementOrSelfStream(E element);

    Stream<E> descendantElementOrSelfStream(E element, Predicate<E> predicate);

    Stream<E> descendantElementStream(E element);

    Stream<E> descendantElementStream(E element, Predicate<E> predicate);

    Stream<E> topmostDescendantElementOrSelfStream(E element, Predicate<E> predicate);

    Stream<E> topmostDescendantElementStream(E element, Predicate<E> predicate);
}
