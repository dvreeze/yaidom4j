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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Static factory methods of {@link ElementStep} instances. The factory methods trivially delegate
 * to the corresponding {@link Element} methods in their implementations.
 * <p>
 * These factory methods offer nothing other than syntactic sugar, making the use of class {@link Element}
 * for querying slightly more pleasant.
 *
 * @author Chris de Vreeze
 */
public class ElementSteps {

    private ElementSteps() {
    }

    public static ElementStep selfElements() {
        return Element::selfElementStream;
    }

    public static ElementStep selfElements(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.selfElementStream(predicate);
    }

    public static ElementStep childElements() {
        return Element::childElementStream;
    }

    public static ElementStep childElements(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.childElementStream(predicate);
    }

    public static ElementStep descendantElementsOrSelf() {
        return Element::descendantElementOrSelfStream;
    }

    public static ElementStep descendantElementsOrSelf(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementOrSelfStream(predicate);
    }

    public static ElementStep descendantElements() {
        return Element::descendantElementStream;
    }

    public static ElementStep descendantElements(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementStream(predicate);
    }

    public static ElementStep topmostDescendantElementsOrSelf(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementOrSelfStream(predicate);
    }

    public static ElementStep topmostDescendantElements(Predicate<? super Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementStream(predicate);
    }
}
