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

package eu.cdevreeze.yaidom4j.transformationapi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.ElementNavigationPath;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Element transformation API, implemented by some element implementations.
 *
 * @param <E> The specific element type itself
 * @param <N> The node super-type of the element type and of all other node types (text, comment etc.)
 * @author Chris de Vreeze
 */
public interface TransformableElementApi<E extends TransformableElementApi<E, N>, N> {

    /**
     * Functionally updates the name of this element.
     */
    E withName(QName newName);

    /**
     * Functionally updates the list of children of this element.
     */
    E withChildren(ImmutableList<N> newChildren);

    /**
     * Calls method {@link TransformableElementApi#withChildren} to functionally add one child node at the end
     */
    E plusChild(N newChild);

    /**
     * Calls method {@link TransformableElementApi#withChildren} to functionally add at most one child node at the end
     */
    E plusChildOption(Optional<N> newChildOption);

    /**
     * Calls method {@link TransformableElementApi#withChildren} to functionally add zero or more child nodes at the end
     */
    E plusChildren(ImmutableList<N> newChildren);

    /**
     * Functionally updates this element by replacing the collection of attributes.
     */
    E withAttributes(ImmutableMap<QName, String> newAttributes);

    /**
     * Calls method {@link TransformableElementApi#withAttributes} to functionally add or update one attribute
     */
    E plusAttribute(QName attrName, String attrValue);

    /**
     * Returns an adapted copy of this element where the children are replaced by the results of applying
     * the given function.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    E transformChildrenToNodeLists(Function<N, List<N>> f);

    /**
     * Returns an adapted copy of this element where the child elements are replaced by the results of applying
     * the given function, and the non-element children are left as-is.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    E transformChildElementsToNodeLists(Function<E, List<N>> f);

    /**
     * Like {@link TransformableElementApi#transformChildElementsToNodeLists}, but mapping each element to just one element
     * instead of a node list. Hence, this method leaves the number of child nodes the same, and child nodes other
     * than element nodes also stay the same.
     * <p>
     * Often this method is useful in combination with a recursive top-down transformation.
     */
    E transformChildElements(UnaryOperator<E> f);

    /**
     * Bottom-up transformation of all descendant-or-self elements.
     */
    E transformDescendantElementsOrSelf(UnaryOperator<E> f);

    /**
     * Bottom-up transformation of all descendant elements.
     */
    E transformDescendantElements(UnaryOperator<E> f);

    /**
     * Functionally updates this element, by applying the given function to the elements found
     * at the given element navigation paths relative to this element.
     * <p>
     * Note that an empty element navigation path refers to this element itself.
     */
    E updateElement(
            Set<ElementNavigationPath> elementNavigationPaths,
            BiFunction<ElementNavigationPath, E, E> f
    );

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
    E removeInterElementWhitespace();
}
