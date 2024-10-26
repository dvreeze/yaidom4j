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

import eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementTree.Element;
import eu.cdevreeze.yaidom4j.queryapi.ElementPredicateFactoryApi;

import javax.xml.namespace.QName;
import java.util.function.Predicate;

/**
 * Several element predicates.
 *
 * @author Chris de Vreeze
 */
public class ElementPredicates {

    private ElementPredicates() {
    }

    private static final Factory factory = new Factory();

    public static Predicate<Element> hasName(QName name) {
        return factory.hasName(name);
    }

    public static Predicate<Element> hasName(String namespace, String localName) {
        return factory.hasName(namespace, localName);
    }

    public static Predicate<Element> hasName(String noNamespaceName) {
        return factory.hasName(noNamespaceName);
    }

    public static Predicate<Element> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate) {
        return factory.hasAttributeValue(attrName, attrValuePredicate);
    }

    public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
        return factory.hasAttributeValue(attrNamespace, attrLocalName, attrValuePredicate);
    }

    public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
        return factory.hasAttributeValue(attrNoNamespaceName, attrValuePredicate);
    }

    public static Predicate<Element> hasAttributeValue(QName attrName, String attrValue) {
        return factory.hasAttributeValue(attrName, attrValue);
    }

    public static Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue) {
        return factory.hasAttributeValue(attrNamespace, attrLocalName, attrValue);
    }

    public static Predicate<Element> hasAttributeValue(String attrNoNamespaceName, String attrValue) {
        return factory.hasAttributeValue(attrNoNamespaceName, attrValue);
    }

    public static Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
        return factory.hasOnlyText(textPredicate);
    }

    public static Predicate<Element> hasOnlyText(String text) {
        return factory.hasOnlyText(text);
    }

    public static Predicate<Element> hasOnlyStrippedText(String text) {
        return factory.hasOnlyStrippedText(text);
    }

    public static final class Factory implements ElementPredicateFactoryApi<Element> {

        private final eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.Factory underlyingFactory =
                new eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.Factory();

        @Override
        public Predicate<Element> hasName(QName name) {
            return e -> underlyingFactory.hasName(name).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasName(String namespace, String localName) {
            return e -> underlyingFactory.hasName(namespace, localName).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasName(String noNamespaceName) {
            return e -> underlyingFactory.hasName(noNamespaceName).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(QName attrName, Predicate<String> attrValuePredicate) {
            return e -> underlyingFactory.hasAttributeValue(attrName, attrValuePredicate).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, Predicate<String> attrValuePredicate) {
            return e -> underlyingFactory.hasAttributeValue(attrNamespace, attrLocalName, attrValuePredicate).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(String attrNoNamespaceName, Predicate<String> attrValuePredicate) {
            return e -> underlyingFactory.hasAttributeValue(attrNoNamespaceName, attrValuePredicate).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(QName attrName, String attrValue) {
            return e -> underlyingFactory.hasAttributeValue(attrName, attrValue).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(String attrNamespace, String attrLocalName, String attrValue) {
            return e -> underlyingFactory.hasAttributeValue(attrNamespace, attrLocalName, attrValue).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasAttributeValue(String attrNoNamespaceName, String attrValue) {
            return e -> underlyingFactory.hasAttributeValue(attrNoNamespaceName, attrValue).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasOnlyText(Predicate<String> textPredicate) {
            return e -> underlyingFactory.hasOnlyText(textPredicate).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasOnlyText(String text) {
            return e -> underlyingFactory.hasOnlyText(text).test(e.underlyingElement());
        }

        @Override
        public Predicate<Element> hasOnlyStrippedText(String text) {
            return e -> underlyingFactory.hasOnlyStrippedText(text).test(e.underlyingElement());
        }
    }
}
