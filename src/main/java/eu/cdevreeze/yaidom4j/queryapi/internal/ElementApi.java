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

package eu.cdevreeze.yaidom4j.queryapi.internal;

import com.google.common.collect.ImmutableMap;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Object-oriented variant of the element query API.
 *
 * @param <E>
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

    Stream<? super E> childNodeStream();

    Stream<E> elementStream();

    Stream<E> elementStream(Predicate<E> predicate);

    Stream<E> topmostElementStream(Predicate<E> predicate);

    Stream<E> childElementStream();

    Stream<E> childElementStream(Predicate<E> predicate);

    Stream<E> descendantElementOrSelfStream();

    Stream<E> descendantElementOrSelfStream(Predicate<E> predicate);

    Stream<E> descendantElementStream();

    Stream<E> descendantElementStream(Predicate<E> predicate);

    Stream<E> topmostDescendantElementOrSelfStream(Predicate<E> predicate);

    Stream<E> topmostDescendantElementStream(Predicate<E> predicate);
}
