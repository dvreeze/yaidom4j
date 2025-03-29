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

package eu.cdevreeze.yaidom4j.dom.ancestryaware;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Static factory methods of {@link AncestryAwareElementStep} instances. The factory methods trivially delegate
 * to the corresponding {@link AncestryAwareNodes.Element} methods in their implementations.
 * <p>
 * These factory methods offer nothing other than syntactic sugar, making the use of class {@link AncestryAwareNodes.Element}
 * for querying slightly more pleasant.
 *
 * @author Chris de Vreeze
 */
public class AncestryAwareElementSteps {

    private AncestryAwareElementSteps() {
    }

    public static AncestryAwareElementStep selfElement() {
        return AncestryAwareNodes.Element::selfElementStream;
    }

    public static AncestryAwareElementStep selfElement(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.selfElementStream(predicate);
    }

    public static AncestryAwareElementStep childElements() {
        return AncestryAwareNodes.Element::childElementStream;
    }

    public static AncestryAwareElementStep childElements(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.childElementStream(predicate);
    }

    public static AncestryAwareElementStep descendantElementsOrSelf() {
        return AncestryAwareNodes.Element::descendantElementOrSelfStream;
    }

    public static AncestryAwareElementStep descendantElementsOrSelf(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementOrSelfStream(predicate);
    }

    public static AncestryAwareElementStep descendantElements() {
        return AncestryAwareNodes.Element::descendantElementStream;
    }

    public static AncestryAwareElementStep descendantElements(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.descendantElementStream(predicate);
    }

    public static AncestryAwareElementStep topmostDescendantElementsOrSelf(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementOrSelfStream(predicate);
    }

    public static AncestryAwareElementStep topmostDescendantElements(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.topmostDescendantElementStream(predicate);
    }

    public static AncestryAwareElementStep parentElement() {
        return e -> e.parentElementOption().stream();
    }

    public static AncestryAwareElementStep ancestorElementsOrSelf() {
        return AncestryAwareNodes.Element::ancestorElementOrSelfStream;
    }

    public static AncestryAwareElementStep ancestorElementsOrSelf(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.ancestorElementOrSelfStream(predicate);
    }

    public static AncestryAwareElementStep ancestorElements() {
        return AncestryAwareNodes.Element::ancestorElementStream;
    }

    public static AncestryAwareElementStep ancestorElements(Predicate<? super AncestryAwareNodes.Element> predicate) {
        Objects.requireNonNull(predicate);

        return e -> e.ancestorElementStream(predicate);
    }
}
