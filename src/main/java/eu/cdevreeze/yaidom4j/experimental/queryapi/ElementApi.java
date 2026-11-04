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

import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;

import java.net.URI;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Element query API, extending {@link GreenTreeElementApi}.
 *
 * @param <E> The specific element type itself (note the use of F-bounded polymorphism)
 * @author Chris de Vreeze
 */
public interface ElementApi<E extends ElementApi<E>> extends GreenTreeElementApi<E> {

    Optional<URI> docUriOption();

    Optional<URI> baseUriOption();

    NamespaceScope namespaceScope();

    Optional<E> parentElementOption();

    Stream<E> ancestorOrSelfElementStream();

    Stream<E> ancestorOrSelfElementStream(Predicate<? super E> predicate);

    Stream<E> ancestorElementStream();

    Stream<E> ancestorElementStream(Predicate<? super E> predicate);
}
