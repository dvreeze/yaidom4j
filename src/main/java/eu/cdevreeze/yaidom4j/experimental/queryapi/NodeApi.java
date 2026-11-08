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

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Node query API, extending {@link GreenTreeNodeApi}.
 * <p>
 * Note that this query API is a convenience API, as an alternative to {@link NodeApi} where it is
 * desired to query for element nodes.
 *
 * @param <N> The specific node type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface NodeApi<N extends NodeApi<N>> extends GreenTreeNodeApi<N> {

    Optional<? extends N> parentOption();

    Stream<? extends N> ancestorOrSelfStream();

    Stream<? extends N> ancestorOrSelfStream(Predicate<? super N> predicate);

    Stream<? extends N> ancestorStream();

    Stream<? extends N> ancestorStream(Predicate<? super N> predicate);
}
