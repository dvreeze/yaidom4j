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

package eu.cdevreeze.yaidom4j.core;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * Utility checking QNames.
 *
 * @author Chris de Vreeze
 */
public class QNames {

    private QNames() {
    }

    /**
     * {@link QName} correctness check, returning true if either the prefix is empty, or the namespace
     * URI is non-empty.
     */
    public static boolean hasNamespaceIfPrefixNonEmpty(QName name) {
        return name.getPrefix().equals(XMLConstants.DEFAULT_NS_PREFIX) || !name.getNamespaceURI().equals(XMLConstants.NULL_NS_URI);
    }
}
