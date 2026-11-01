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

package eu.cdevreeze.yaidom4j.queryapi;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Object-oriented node query API. This API is implemented by yaidom4j
 * node implementations, but it can also be implemented by other node implementations
 * (such as Saxon XDM item wrappers) or even domain-specific node hierarchies.
 *
 * @param <N> The specific node type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface NodeApi<N extends NodeApi<N>> {

    Stream<N> selfStream();

    Stream<N> selfStream(Predicate<? super N> predicate);

    Stream<N> childStream();

    Stream<N> childStream(Predicate<? super N> predicate);

    Stream<N> descendantOrSelfStream();

    Stream<N> descendantOrSelfStream(Predicate<? super N> predicate);

    Stream<N> descendantStream();

    Stream<N> descendantStream(Predicate<? super N> predicate);

    Stream<N> topmostDescendantOrSelfStream(Predicate<? super N> predicate);

    Stream<N> topmostDescendantStream(Predicate<? super N> predicate);
}
