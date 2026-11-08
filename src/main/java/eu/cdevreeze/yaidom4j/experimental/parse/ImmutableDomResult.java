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

package eu.cdevreeze.yaidom4j.experimental.parse;

import eu.cdevreeze.yaidom4j.experimental.dom.Document;

import javax.xml.transform.sax.SAXResult;

/**
 * Immutable DOM document producing {@link javax.xml.transform.Result}.
 * <p>
 * This class extends class {@link SAXResult}, which is an implementation detail, but needed in order to plug into
 * the Transformer API. {@link SAXResult} methods such as {@link SAXResult#setHandler} and {@link SAXResult#setLexicalHandler}
 * should never be called.
 * <p>
 * Example code:
 * {@snippet :
 * TransformerHandler th = TransformerHandlers.newTransformerHandler();
 * ImmutableDomResult domResult = new ImmutableDomResult();
 * th.setResult(domResult);
 * XMLReader saxReader = SaxParsers.newNonValidatingSaxParserFactory().newSAXParser().getXMLReader();
 * saxReader.setContentHandler(th);
 * saxReader.parse(new InputSource(url.openStream()));
 * Document doc = domResult.getDocument();
 *}
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomResult extends SAXResult {

    public ImmutableDomResult() {
        super(new DocumentProducingSaxHandler());
    }

    public Document getDocument() {
        return ((DocumentProducingSaxHandler) getHandler()).resultingDocument();
    }
}
