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

package eu.cdevreeze.yaidom4j.core;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Element navigation path, to be interpreted against some "root element". An empty path
 * resolves to that "root element" itself. A path with first entry 2 and second entry 5 resolves
 * to the 6th child element of the 3rd child element of the "root element". Note that the entries
 * are zero-based indices of child element nodes.
 * <p>
 * Note that element navigation paths are stable under (functional) updates where single elements are
 * replaced by single elements, regardless of element name changes.
 * <p>
 * There are 2 important differences with
 * <a href="https://www.w3.org/TR/xptr-element/">XPointer element() scheme</a>, limited to a child
 * sequence element() scheme XPointer alone. First of all, the child indices in these XPointers are
 * 1-based instead of 0-based. Secondly, the first index in the XPointer points to some root element,
 * whereas the first entry in yaidom4j element navigation paths is a child element index relative to
 * some "root element", which is unknown to the element navigation path. The element() scheme XPointers
 * and element navigation paths have in common that indices refer to element nodes, not arbitrary nodes.
 * <p>
 * This class is immutable, due to the fact that record component "entries" is an "unmodifiable list".
 *
 * @author Chris de Vreeze
 */
public record ElementNavigationPath(List<Integer> entries) {

    public ElementNavigationPath {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries().isEmpty();
    }

    public int getEntry(int entryIndex) {
        return entries().get(entryIndex);
    }

    public Optional<ElementNavigationPath> withoutFirstEntryOption() {
        return Optional.of(entries)
                .filter(es -> !es.isEmpty())
                .map(es -> es.subList(1, es.size()))
                .map(ElementNavigationPath::new);
    }

    public ElementNavigationPath withoutFirstEntry() {
        return withoutFirstEntryOption().orElseThrow();
    }

    public Optional<ElementNavigationPath> withoutLastEntryOption() {
        return Optional.of(entries)
                .filter(es -> !es.isEmpty())
                .map(es -> es.subList(0, es.size() - 1))
                .map(ElementNavigationPath::new);
    }

    public ElementNavigationPath withoutLastEntry() {
        return withoutLastEntryOption().orElseThrow();
    }

    public ElementNavigationPath appendEntry(int childElementIndex) {
        return new ElementNavigationPath(
                Stream.concat(entries().stream(), Stream.of(childElementIndex)).toList()
        );
    }

    public ElementNavigationPath prependEntry(int childElementIndex) {
        return new ElementNavigationPath(
                Stream.concat(Stream.of(childElementIndex), entries().stream()).toList()
        );
    }

    public static ElementNavigationPath empty() {
        return new ElementNavigationPath(List.of());
    }
}
