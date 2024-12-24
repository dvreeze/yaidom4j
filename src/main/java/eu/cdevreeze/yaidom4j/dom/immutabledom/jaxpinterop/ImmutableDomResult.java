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

package eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;

import javax.xml.transform.sax.SAXResult;

/**
 * Immutable DOM document producing {@link javax.xml.transform.Result}.
 * <p>
 * This class extends class {@link SAXResult}, which is an implementation detail, but needed in order to plug into
 * the Transformer API. {@link SAXResult} methods such as {@link SAXResult#setHandler} and {@link SAXResult#setLexicalHandler}
 * should never be called.
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomResult extends SAXResult {

    public ImmutableDomResult() {
        super(new ImmutableDomProducingSaxHandler());
    }

    public Document getDocument() {
        return ((ImmutableDomProducingSaxHandler) getHandler()).resultingDocument();
    }
}
