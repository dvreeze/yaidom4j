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

import module java.base;

/**
 * Object-oriented node query API. This API is implemented by yaidom4j
 * node implementations, but it can also be implemented by other node implementations
 * (such as Saxon XDM item wrappers).
 *
 * @param <N> The specific node type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface GreenTreeNodeApi<N extends GreenTreeNodeApi<N>> {

    Stream<N> childStream();

    Stream<N> childStream(Predicate<? super N> predicate);

    Stream<N> descendantOrSelfStream();

    Stream<N> descendantOrSelfStream(Predicate<? super N> predicate);

    Stream<N> descendantStream();

    Stream<N> descendantStream(Predicate<? super N> predicate);

    Stream<N> topmostDescendantOrSelfStream(Predicate<? super N> predicate);

    Stream<N> topmostDescendantStream(Predicate<? super N> predicate);

    /**
     * Navigates to the node referred to by the zero-based node navigation path, returning the result
     * wrapped in an {@link Optional} (so returning an empty Optional if no such node is found).
     * <p>
     * If the node navigation path is an empty collection, this node is returned (wrapped in an Optional).
     * <p>
     * If the node navigation path is a singleton collection, the single zero-based index or step in this
     * singleton collection is a 0-based index of the next child node to be found, if any, wrapped in an Optional.
     * <p>
     * Otherwise, this process is repeated, until the final node is found, if any. That is, the following is returned:
     * {@snippet :
     * // Recursion
     * nextNodeOption.flatMap(n -> n.navigate(nodeNavigationPath.stream().skip(1).toList()))
     *}
     */
    Optional<N> navigate(List<Integer> nodeNavigationPath);
}
