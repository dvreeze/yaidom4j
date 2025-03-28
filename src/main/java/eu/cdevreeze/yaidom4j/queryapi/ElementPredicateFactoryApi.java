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

package eu.cdevreeze.yaidom4j.queryapi;

import javax.xml.namespace.QName;
import java.util.function.Predicate;

/**
 * Generic factory API for several element predicates, applicable to multiple element implementations.
 *
 * @author Chris de Vreeze
 */
public interface ElementPredicateFactoryApi<E> {

    Predicate<E> hasName(QName name);

    Predicate<E> hasName(String namespace, String localName);

    Predicate<E> hasName(String noNamespaceName);

    Predicate<E> hasLocalName(String localName);

    Predicate<E> hasAttributeWithName(QName attrName);

    Predicate<E> hasAttributeWithName(String attrNamespace, String attrLocalName);

    Predicate<E> hasAttributeWithName(String attrNoNamespaceName);

    Predicate<E> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttributeValue(QName attrName, String attrValue);

    Predicate<E> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue);

    Predicate<E> hasAttributeValue(String attrNoNamespaceName, String attrValue);

    /**
     * Returns an element predicate which returns true if and only if the element has only text
     * nodes as children and the concatenated text of those child text nodes matches the given
     * text predicate.
     */
    Predicate<E> hasOnlyText(Predicate<String> textPredicate);

    /**
     * Returns an element predicate which returns true if and only if the element has only text
     * nodes as children and the concatenated text of those child text nodes equals the given
     * text parameter.
     */
    Predicate<E> hasOnlyText(String text);

    /**
     * Returns the same as method {@link ElementPredicateFactoryApi#hasOnlyText(String)}, except that the
     * concatenated text of the child text nodes is stripped before comparing it with the
     * parameter text.
     */
    Predicate<E> hasOnlyStrippedText(String text);
}
