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

package eu.cdevreeze.yaidom4j.experimental.queryapi;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Object-oriented element query API. This API is implemented by yaidom4j
 * element node implementations, but it can also be implemented by other element implementations
 * (such as Saxon XDM item wrappers) or even domain-specific element hierarchies (such as
 * the elements in an XML Schema, whose common super-type extends/implements this query API interface).
 * <p>
 * Note that this query API is a convenience API, as an alternative to {@link GreenTreeNodeApi} where it is
 * desired to query for element nodes.
 *
 * @param <E> The specific element type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface GreenTreeElementApi<E extends GreenTreeElementApi<E>> {

    QName elementName();

    /**
     * Returns the attributes as an "unmodifiable map".
     */
    Map<QName, String> attributes();

    Optional<String> attributeOption(QName attrName);

    String attribute(QName attrName);

    /**
     * Returns the concatenated text of the text children (ignoring other child nodes than text nodes)
     */
    String text();

    Stream<E> childElementStream();

    Stream<E> childElementStream(Predicate<? super E> predicate);

    Stream<E> descendantOrSelfElementStream();

    Stream<E> descendantOrSelfElementStream(Predicate<? super E> predicate);

    Stream<E> descendantElementStream();

    Stream<E> descendantElementStream(Predicate<? super E> predicate);

    Stream<E> topmostDescendantOrSelfElementStream(Predicate<? super E> predicate);

    Stream<E> topmostDescendantElementStream(Predicate<? super E> predicate);
}
