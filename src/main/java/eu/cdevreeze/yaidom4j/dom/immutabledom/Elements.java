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

import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.queryapi.ElementQueryApi;

import javax.xml.namespace.QName;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Element query API.
 *
 * @author Chris de Vreeze
 */
public class Elements implements ElementQueryApi<Element> {

    private Elements() {
    }

    public static Elements INSTANCE = new Elements();

    @Override
    public QName elementName(Element element) {
        Objects.requireNonNull(element);

        return element.name();
    }

    @Override
    public ImmutableMap<QName, String> attributes(Element element) {
        Objects.requireNonNull(element);

        return element.attributes();
    }

    @Override
    public Stream<Node> childNodeStream(Element element) {
        Objects.requireNonNull(element);

        return element.children().stream();
    }

    @Override
    public Stream<Element> childElementStream(Element element) {
        Objects.requireNonNull(element);

        return childNodeStream(element).filter(Node::isElement).map(n -> (Element) n);
    }

    @Override
    public Stream<Element> childElementStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return childElementStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream(Element element) {
        Objects.requireNonNull(element);

        Stream<Element> selfStream = Stream.of(element);
        // Recursion
        Stream<Element> descendantElemStream =
                childElementStream(element).flatMap(this::descendantElementOrSelfStream);
        return Stream.concat(selfStream, descendantElemStream);
    }

    @Override
    public Stream<Element> descendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return descendantElementOrSelfStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> descendantElementStream(Element element) {
        Objects.requireNonNull(element);

        return childElementStream(element).flatMap(this::descendantElementOrSelfStream);
    }

    @Override
    public Stream<Element> descendantElementStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return descendantElementStream(element).filter(predicate);
    }

    @Override
    public Stream<Element> topmostDescendantElementOrSelfStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        if (predicate.test(element)) {
            return Stream.of(element);
        } else {
            // Recursion
            return childElementStream(element).flatMap(che -> topmostDescendantElementOrSelfStream(che, predicate));
        }
    }

    @Override
    public Stream<Element> topmostDescendantElementStream(Element element, Predicate<Element> predicate) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(predicate);

        return childElementStream(element).flatMap(che -> topmostDescendantElementOrSelfStream(che, predicate));
    }
}
