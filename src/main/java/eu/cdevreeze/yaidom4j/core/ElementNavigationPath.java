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

import com.google.common.collect.ImmutableList;

import java.util.Optional;

/**
 * Element navigation path, to be interpreted against some "root element". An empty path
 * resolves to that "root element" itself. A path with first entry 2 and second entry 5 resolves
 * to the 6th child element of the 3rd child element of the "root element". Note that the entries
 * are zero-based indices of child element nodes.
 * <p>
 * Note that element navigation paths are stable under (functional) updates where single elements are
 * replaced by single elements, regardless of element name changes.
 *
 * @author Chris de Vreeze
 */
public record ElementNavigationPath(ImmutableList<Integer> entries) {

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
                ImmutableList.<Integer>builder().addAll(entries()).add(childElementIndex).build()
        );
    }

    public ElementNavigationPath prependEntry(int childElementIndex) {
        return new ElementNavigationPath(
                ImmutableList.<Integer>builder().add(childElementIndex).addAll(entries()).build()
        );
    }

    public static ElementNavigationPath empty() {
        return new ElementNavigationPath(ImmutableList.of());
    }
}
