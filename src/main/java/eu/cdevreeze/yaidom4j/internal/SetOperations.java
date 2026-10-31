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

package eu.cdevreeze.yaidom4j.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * Operations on {@link java.util.Set} (in the absence of a Guava dependency).
 * Good enough as internal API, but unfit as general set operation utility.
 *
 * @author Chris de Vreeze
 */
public class SetOperations {

    private SetOperations() {
        // Non-instantiable
    }

    /**
     * Returns the set difference of the two parameter sets as an "unmodifiable set".
     */
    public static <E> Set<E> difference(Set<E> set1, Set<E> set2) {
        Set<E> result = new HashSet<>(set1);
        result.removeAll(set2);
        return Set.copyOf(result);
    }

    /**
     * Returns the set intersection of the two parameter sets as an "unmodifiable set".
     */
    public static <E> Set<E> intersection(Set<E> set1, Set<E> set2) {
        Set<E> result = new HashSet<>(set1);
        result.retainAll(set2);
        return Set.copyOf(result);
    }
}
