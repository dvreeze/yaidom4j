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
import eu.cdevreeze.yaidom4j.jaxp.TransformerHandlers;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.util.Objects;

/**
 * Immutable DOM document as Source. Inspired by class JAXBSource.
 * <p>
 * This class extends class SAXSource, which is an implementation detail, but needed in order to plug into
 * the Transformer API. SAXSource methods such as setXMLReader and setInputSource should never be called.
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomSource extends SAXSource {

    private final Document document;
    private final SAXTransformerFactory saxTransformerFactory;

    public ImmutableDomSource(Document document, SAXTransformerFactory saxTransformerFactory) {
        super(pseudoXmlReader(document, saxTransformerFactory), new InputSource()); // dummy unused InputSource
        this.document = document;
        this.saxTransformerFactory = saxTransformerFactory;
    }

    public ImmutableDomSource(Document document) {
        this(document, TransformerHandlers.newSaxTransformerFactory());
    }

    public Document getDocument() {
        return document;
    }

    public SAXTransformerFactory getSaxTransformerFactory() {
        return saxTransformerFactory;
    }

    private static XMLReader pseudoXmlReader(Document document, SAXTransformerFactory saxTransformerFactory) {
        return new XMLReader() {

            // The feature/property getters and setters should not be called

            @Override
            public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
                return false;
            }

            @Override
            public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
            }

            @Override
            public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
                return null;
            }

            @Override
            public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            }

            @Override
            public void setEntityResolver(EntityResolver resolver) {
            }

            @Override
            public EntityResolver getEntityResolver() {
                return null;
            }

            @Override
            public void setDTDHandler(DTDHandler handler) {
            }

            @Override
            public DTDHandler getDTDHandler() {
                return null;
            }

            private final XMLFilter xmlFilter = new XMLFilterImpl();

            @Override
            public void setContentHandler(ContentHandler handler) {
                xmlFilter.setContentHandler(handler);
            }

            @Override
            public ContentHandler getContentHandler() {
                return xmlFilter.getContentHandler();
            }

            @Override
            public void setErrorHandler(ErrorHandler handler) {
                // TODO
            }

            @Override
            public ErrorHandler getErrorHandler() {
                // TODO
                return null;
            }

            @Override
            public void parse(InputSource input) {
                // Ignoring InputSource
                parse();
            }

            @Override
            public void parse(String systemId) {
                // Ignoring systemId
                parse();
            }

            public void parse() {
                // Not parsing, but "processing" the Document
                TransformerHandler th = TransformerHandlers.newTransformerHandler(saxTransformerFactory);
                SAXResult result = new SAXResult(Objects.requireNonNull(xmlFilter.getContentHandler()));
                // No system ID set!
                th.setResult(result);
                ImmutableDomConsumingSaxEventGenerator eventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);
                eventGenerator.processDocument(document);
            }
        };
    }
}
