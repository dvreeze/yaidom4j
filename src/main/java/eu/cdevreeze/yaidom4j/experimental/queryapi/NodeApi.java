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
 * Node query API, extending {@link GreenTreeNodeApi}.
 *
 * @param <N> The specific node type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface NodeApi<N extends NodeApi<N>> extends GreenTreeNodeApi<N> {

    /**
     * Returns the optional parent node of this node.
     * <p>
     * Note that a parent node can be either a document node or an element node.
     * If the "root element" does not have a parent document, then the parent node (if any) is always an element node.
     */
    Optional<N> parentOption();

    /**
     * Returns the stream of ancestor-or-self nodes, which is tne transitive closure of the parent axis,
     * starting with this node itself.
     * <p>
     * See {@link NodeApi#parentOption()}.
     */
    Stream<N> ancestorOrSelfStream();

    /**
     * Returns the equivalent of {@code ancestorOrSelfStream().filter(predicate)}.
     */
    Stream<N> ancestorOrSelfStream(Predicate<? super N> predicate);

    /**
     * Returns the stream of ancestor nodes, which is tne transitive closure of the parent axis,
     * but excluding this node itself.
     * <p>
     * See {@link NodeApi#parentOption()}.
     */
    Stream<N> ancestorStream();

    /**
     * Returns the equivalent of {@code ancestorStream().filter(predicate)}.
     */
    Stream<N> ancestorStream(Predicate<? super N> predicate);
}
