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

import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.jaxp.TransformerHandlers;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Simple utility to print immutable Documents to a String.
 * <p>
 * Do not use this utility when the printed documents take too much memory.
 *
 * @author Chris de Vreeze
 */
public class DocumentPrinters {

    private DocumentPrinters() {
    }

    public static Builder builder(SAXTransformerFactory saxTransformerFactory) {
        return new Builder(saxTransformerFactory);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final SAXTransformerFactory saxTransformerFactory;

        public Builder(SAXTransformerFactory saxTransformerFactory) {
            this.saxTransformerFactory = saxTransformerFactory;
        }

        public Builder() {
            this(TransformerHandlers.newSaxTransformerFactory());
        }

        // TODO Configure TransformerHandler

        public DocumentPrinter build() {
            return new DefaultDocumentPrinter(saxTransformerFactory);
        }
    }

    public static DocumentPrinter instance() {
        return builder().build();
    }

    public static final class DefaultDocumentPrinter implements DocumentPrinter {

        private final SAXTransformerFactory saxTransformerFactory;

        public DefaultDocumentPrinter(SAXTransformerFactory saxTransformerFactory) {
            this.saxTransformerFactory = saxTransformerFactory;
        }

        public String print(Document document) {
            var sw = new StringWriter();
            var streamResult = new StreamResult(sw);

            TransformerHandler th = TransformerHandlers.newTransformerHandler(saxTransformerFactory);
            th.setResult(streamResult);

            var saxEventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);

            saxEventGenerator.processDocument(document);

            return sw.toString();
        }

        public String print(Element element) {
            var sw = new StringWriter();
            var streamResult = new StreamResult(sw);

            TransformerHandler th = TransformerHandlers.newTransformerHandler(saxTransformerFactory);
            th.setResult(streamResult);

            var saxEventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);

            saxEventGenerator.processElement(element, NamespaceScope.empty());

            return sw.toString();
        }
    }
}
