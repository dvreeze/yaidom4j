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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Several element predicates.
 *
 * @author Chris de Vreeze
 */
public class ElementPredicates {

    private ElementPredicates() {
    }

    public static Predicate<Element> hasName(Predicate<QName> namePredicate) {
        return e -> namePredicate.test(e.name());
    }

    public static Predicate<Element> hasName(QName name) {
        return hasName(nm -> nm.equals(name));
    }

    public static Predicate<Element> hasName(String namespace, String localName) {
        return hasName(nm -> nm.getNamespaceURI().equals(namespace) && nm.getLocalPart().equals(localName));
    }

    public static Predicate<Element> hasName(String noNamespaceName) {
        return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
    }

    public static Predicate<Element> hasAttribute(Predicate<Map.Entry<QName, String>> attrPredicate) {
        return e -> e.attributes().entrySet().stream().anyMatch(attrPredicate);
    }

    public static Predicate<Element> hasAttribute(QName attrName, Predicate<String> attrValuePredicate) {
        return hasAttribute(kv -> kv.getKey().equals(attrName) && attrValuePredicate.test(kv.getValue()));
    }

    public static Predicate<Element> hasAttribute(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
        return hasAttribute(new QName(attrNamespace, attrLocalName), attrValuePredicate);
    }

    public static Predicate<Element> hasAttribute(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
        return hasAttribute(new QName(attrNoNamespaceName), attrValuePredicate);
    }

    public static Predicate<Element> hasAttribute(QName attrName, String attrValue) {
        return hasAttribute(attrName, s -> s.equals(attrValue));
    }

    public static Predicate<Element> hasAttribute(String attrNamespace, String attrLocalName, String attrValue) {
        return hasAttribute(new QName(attrNamespace, attrLocalName), attrValue);
    }

    public static Predicate<Element> hasAttribute(String attrNoNamespaceName, String attrValue) {
        return hasAttribute(new QName(attrNoNamespaceName), attrValue);
    }

    public static Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
        return e -> e.children().stream().allMatch(ch -> ch instanceof Text) &&
                textPredicate.test(e.text());
    }

    public static Predicate<Element> hasOnlyText(String text) {
        return hasOnlyText(s -> s.equals(text));
    }

    public static Predicate<Element> hasOnlyStrippedText(String text) {
        return hasOnlyText(s -> s.strip().equals(text));
    }
}
