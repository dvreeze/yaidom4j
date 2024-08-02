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
import java.util.function.Predicate;

/**
 * Several element predicates.
 *
 * @author Chris de Vreeze
 */
public class ElementPredicates {

    private ElementPredicates() {
    }

    public static Predicate<Element> hasName(QName name) {
        return e -> e.name().equals(name);
    }

    public static Predicate<Element> hasName(String namespace, String localName) {
        return e -> e.name().getNamespaceURI().equals(namespace) &&
                e.name().getLocalPart().equals(localName);
    }

    public static Predicate<Element> hasName(String noNamespaceName) {
        return hasName(XMLConstants.NULL_NS_URI, noNamespaceName);
    }
}
