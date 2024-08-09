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
import java.util.Map;
import java.util.function.Predicate;

/**
 * Factory API for several element predicates.
 *
 * @author Chris de Vreeze
 */
public interface ElementPredicateFactoryApi<E> {

    Predicate<E> hasName(Predicate<QName> namePredicate);

    Predicate<E> hasName(QName name);

    Predicate<E> hasName(String namespace, String localName);

    Predicate<E> hasName(String noNamespaceName);

    Predicate<E> hasAttribute(Predicate<Map.Entry<QName, String>> attrPredicate);

    Predicate<E> hasAttribute(QName attrName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttribute(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttribute(String attrNoNamespaceName, Predicate<String> attrValuePredicate);

    Predicate<E> hasAttribute(QName attrName, String attrValue);

    Predicate<E> hasAttribute(String attrNamespace, String attrLocalName, String attrValue);

    Predicate<E> hasAttribute(String attrNoNamespaceName, String attrValue);

    Predicate<E> hasOnlyText(Predicate<String> textPredicate);

    Predicate<E> hasOnlyText(String text);

    Predicate<E> hasOnlyStrippedText(String text);
}
