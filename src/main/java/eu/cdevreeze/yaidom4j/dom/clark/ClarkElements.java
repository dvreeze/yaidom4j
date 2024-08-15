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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes.*;

/**
 * Several Clark element utilities, mainly for functionally transforming elements.
 * <p>
 * Note that it is very easy to create Clark elements that cannot correspond to any valid XML (1.0).
 *
 * @author Chris de Vreeze
 */
public class ClarkElements {

    private ClarkElements() {
    }

    // General element/node transformations

    /**
     * Functionally updates the list of children of the given element.
     */
    public static Element withChildren(Element element, ImmutableList<Node> newChildren) {
        return new Element(
                element.name(),
                element.attributes(),
                newChildren
        );
    }

    /**
     * Returns an adapted copy of the parameter element where the children are replaced by the results of applying
     * the given function.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    public static Element transformChildrenToNodeLists(Element element, Function<Node, List<Node>> f) {
        return withChildren(
                element,
                element.children().stream()
                        .flatMap(ch -> f.apply(ch).stream())
                        .collect(ImmutableList.toImmutableList())
        );
    }

    /**
     * Returns an adapted copy of the parameter element where the child elements are replaced by the results of applying
     * the given function, and the non-element children are left as-is.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    public static Element transformChildElementsToNodeLists(Element element, Function<Element, List<Node>> f) {
        return transformChildrenToNodeLists(
                element,
                ch -> {
                    if (ch instanceof Element e) {
                        return f.apply(e);
                    } else {
                        return List.of(ch);
                    }
                }
        );
    }

    /**
     * Like "transformChildElementsToNodeLists", but mapping each element to just one element instead
     * of a node list. Hence, this method leaves the number of child nodes the same, and child nodes other
     * than element nodes also stay the same.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    public static Element transformChildElements(Element element, UnaryOperator<Element> f) {
        return transformChildElementsToNodeLists(element, e -> List.of(f.apply(e)));
    }

    /**
     * Bottom-up transformation of all descendant-or-self elements.
     */
    public static Element transformDescendantElementsOrSelf(Element element, UnaryOperator<Element> f) {
        // Recursion
        Element descendantResult = transformChildElements(
                element,
                che -> transformDescendantElementsOrSelf(che, f));
        return f.apply(descendantResult);
    }

    /**
     * Bottom-up transformation of all descendant elements.
     */
    public static Element transformDescendantElements(Element element, UnaryOperator<Element> f) {
        return transformChildElements(
                element,
                che -> transformDescendantElementsOrSelf(che, f));
    }

    // Custom element transformations

    /**
     * Removes all inter-element whitespace, through a naive algorithm. That is, recursively throughout
     * the element tree the following transformation is done: if an element has at least one element
     * node as child and if all text children are only whitespace, the text children are removed.
     * <p>
     * The "xml:space" attribute is not respected, if any.
     * <p>
     * This method is useful to remove inter-element whitespace if parsing of the document was done
     * without any validation against DTD or XSD, thus preventing the parser from recognizing "ignorable
     * whitespace".
     */
    public static Element removeInterElementWhitespace(Element element) {
        ImmutableList<Node> children = element.children();

        // Naive implementation
        boolean hasElementChild = children.stream().anyMatch(n -> n instanceof Element);
        boolean hasOnlyInterElementWhitespaceTextChildren = hasElementChild &&
                children.stream().allMatch(ch -> !(ch instanceof Text t) || t.value().isBlank());

        List<Node> filteredChildren =
                hasOnlyInterElementWhitespaceTextChildren ?
                        children.stream().filter(ch -> !(ch instanceof Text)).toList() :
                        children;

        return withChildren(
                element,
                filteredChildren.stream()
                        .map(ch -> {
                            if (ch instanceof Element che) {
                                // Recursion
                                return removeInterElementWhitespace(che);
                            } else {
                                return ch;
                            }
                        })
                        .collect(ImmutableList.toImmutableList())
        );
    }
}
