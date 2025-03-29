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

package eu.cdevreeze.yaidom4j.dom.clark;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Static factory methods of {@link ClarkElementStep} instances. The factory methods trivially delegate
 * to the corresponding {@link ClarkNodes.Element} methods in their implementations.
 * <p>
 * These factory methods offer nothing other than syntactic sugar, making the use of class {@link ClarkNodes.Element}
 * for querying slightly more pleasant.
 *
 * @author Chris de Vreeze
 */
public class ClarkElementSteps {

    private ClarkElementSteps() {
    }

    public static ClarkElementStep selfElement() {
        return ClarkNodes.Element::selfElementStream;
    }

    public static ClarkElementStep selfElement(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.selfElementStream(predicate);
    }

    public static ClarkElementStep childElements() {
        return ClarkNodes.Element::childElementStream;
    }

    public static ClarkElementStep childElements(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.childElementStream(predicate);
    }

    public static ClarkElementStep descendantElementsOrSelf() {
        return ClarkNodes.Element::descendantElementOrSelfStream;
    }

    public static ClarkElementStep descendantElementsOrSelf(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementOrSelfStream(predicate);
    }

    public static ClarkElementStep descendantElements() {
        return ClarkNodes.Element::descendantElementStream;
    }

    public static ClarkElementStep descendantElements(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementStream(predicate);
    }

    public static ClarkElementStep topmostDescendantElementsOrSelf(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementOrSelfStream(predicate);
    }

    public static ClarkElementStep topmostDescendantElements(Predicate<? super ClarkNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementStream(predicate);
    }
}
