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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.function.Predicate;

/**
 * Element predicates.
 *
 * @author Chris de Vreeze
 */
public final class ElementPredicates {

    private ElementPredicates() {
        // Non-instantiable
    }

    public static Predicate<Element> hasName(QName name) {
        return e -> e.elementName().equals(name);
    }

    public static Predicate<Element> hasName(String namespace, String localName) {
        return e -> e.elementName().getNamespaceURI().equals(namespace) && e.elementName().getLocalPart().equals(localName);
    }

    public static Predicate<Element> hasName(String noNamespaceName) {
        return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
    }

    public static Predicate<Element> hasLocalName(String localName) {
        return e -> e.elementName().getLocalPart().equals(localName);
    }

    public static Predicate<Element> hasAttributeWithName(QName attrName) {
        return e -> e.attributeOption(attrName).isPresent();
    }

    public static Predicate<Element> hasAttributeWithName(String attrNamespace, String attrLocalName) {
        return hasAttributeWithName(new QName(attrNamespace, attrLocalName));
    }

    public static Predicate<Element> hasAttributeWithName(String attrNoNamespaceName) {
        return hasAttributeWithName(new QName(attrNoNamespaceName));
    }

    public static Predicate<Element> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate) {
        return e -> e.attributes().entrySet().stream()
                .anyMatch(kv -> kv.getKey().equals(attrName) && attrValuePredicate.test(kv.getValue()));
    }

    public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
        return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValuePredicate);
    }

    public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
        return hasAttributeValue(new QName(attrNoNamespaceName), attrValuePredicate);
    }

    public static Predicate<Element> hasAttributeValue(QName attrName, String attrValue) {
        return hasAttributeValue(attrName, s -> s.equals(attrValue));
    }

    public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue) {
        return hasAttributeValue(new QName(attrNamespace, attrLocalName), attrValue);
    }

    public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, String attrValue) {
        return hasAttributeValue(new QName(attrNoNamespaceName), attrValue);
    }

    public static Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
        return e -> e.children().stream().allMatch(ch -> ch instanceof Text) &&
                textPredicate.test(e.text());
    }

    public static Predicate<Element> hasOnlyText(String text) {
        return hasOnlyText(s -> s.equals(text));
    }

    public static Predicate<Element> hasOnlyTextAfterStripping(String text) {
        return hasOnlyText(s -> s.strip().equals(text));
    }
}
