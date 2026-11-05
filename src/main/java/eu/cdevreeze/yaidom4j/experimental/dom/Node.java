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

package eu.cdevreeze.yaidom4j.experimental.dom;

import eu.cdevreeze.yaidom4j.experimental.queryapi.NodeApi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Immutable ("red-green") XML node.
 *
 * @author Chris de Vreeze
 */
public sealed interface Node extends NodeApi<Node> permits CanBeChild, Document, Attribute, Namespace {

    /**
     * Returns the 0-based offset of this node as child node of the parent, if any.
     * Must be 0 if there is no parent node.
     */
    int offset();

    @Override
    Optional<Node> parentOption();

    GreenXmlTrees.Node greenTreeNode();

    @Override
    default Stream<Node> childStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return childStream().filter(predicate);
    }

    @Override
    default Stream<Node> descendantOrSelfStream() {
        Stream<Node> selfStream = Stream.of(this);
        // Recursion
        Stream<Node> descendantStream =
                childStream().flatMap(Node::descendantOrSelfStream);
        return Stream.concat(selfStream, descendantStream);
    }

    @Override
    default Stream<Node> descendantOrSelfStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return descendantOrSelfStream().filter(predicate);
    }

    @Override
    default Stream<Node> descendantStream() {
        return childStream().flatMap(Node::descendantOrSelfStream);
    }

    @Override
    default Stream<Node> descendantStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return descendantStream().filter(predicate);
    }

    @Override
    default Stream<Node> topmostDescendantOrSelfStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        if (predicate.test(this)) {
            return Stream.of(this);
        } else {
            // Recursion
            return childStream().flatMap(che -> che.topmostDescendantOrSelfStream(predicate));
        }
    }

    @Override
    default Stream<Node> topmostDescendantStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return childStream().flatMap(che -> che.topmostDescendantOrSelfStream(predicate));
    }

    @Override
    default Stream<Node> ancestorOrSelfStream() {
        // Recursive
        return Stream.concat(
                Stream.of(this),
                parentOption().stream().flatMap(Node::ancestorOrSelfStream)
        );
    }

    @Override
    default Stream<Node> ancestorOrSelfStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return ancestorOrSelfStream().filter(predicate);
    }

    @Override
    default Stream<Node> ancestorStream() {
        return parentOption().stream().flatMap(Node::ancestorOrSelfStream);
    }

    @Override
    default Stream<Node> ancestorStream(Predicate<? super Node> predicate) {
        Objects.requireNonNull(predicate);

        return ancestorStream().filter(predicate);
    }

    default Node rootNode() {
        return ancestorOrSelfStream().toList().reversed().getFirst();
    }

    /**
     * Returns the zero-based node navigation path of this node relative to the root node.
     * That is, returns <code>ancestorOrSelfStream().map(Node::offset).toList().reversed()</code>.
     */
    default List<Integer> nodeNavigationPath() {
        // Immutable List
        return List.copyOf(
                ancestorOrSelfStream().map(Node::offset).toList().reversed()
        );
    }
}
