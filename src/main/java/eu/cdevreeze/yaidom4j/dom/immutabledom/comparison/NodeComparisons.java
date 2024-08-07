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

package eu.cdevreeze.yaidom4j.dom.immutabledom.comparison;

import eu.cdevreeze.yaidom4j.dom.immutabledom.*;

import java.util.stream.IntStream;

/**
 * Node equality comparison support.
 *
 * @author Chris de Vreeze
 */
public class NodeComparisons {

    private NodeComparisons() {
    }

    /**
     * API for node equality comparisons.
     * <p>
     * Equality comparisons provided out of the box can be used as basis for specific custom node
     * equality comparisons.
     */
    @FunctionalInterface
    public interface NodeEqualityComparison {

        boolean areEqual(Node node1, Node node2);
    }

    public static DefaultEqualityComparison defaultEquality() {
        return new DefaultEqualityComparison();
    }

    /**
     * Node equality comparison ignoring namespace prefixes (and namespace scopes) of element nodes.
     */
    public static class DefaultEqualityComparison implements NodeEqualityComparison {

        @Override
        public boolean areEqual(Node node1, Node node2) {
            if (node1 instanceof Element elem1) {
                return (node2 instanceof Element elem2) && areEqual(elem1, elem2);
            } else if (node1 instanceof Text text1) {
                return (node2 instanceof Text text2) && text1.equals(text2);
            } else if (node1 instanceof Comment comment1) {
                return (node2 instanceof Comment comment2) && comment1.equals(comment2);
            } else if (node1 instanceof ProcessingInstruction pi1) {
                return (node2 instanceof ProcessingInstruction pi2) && pi1.equals(pi2);
            } else {
                return false;
            }
        }

        public boolean areEqual(Element element1, Element element2) {
            return element1.name().equals(element2.name()) &&
                    element1.attributes().equals(element2.attributes()) &&
                    element1.children().size() == element2.children().size() &&
                    IntStream.range(0, element1.children().size())
                            .allMatch(i -> areEqual(element1.children().get(i), element2.children().get(i)));
        }
    }
}
