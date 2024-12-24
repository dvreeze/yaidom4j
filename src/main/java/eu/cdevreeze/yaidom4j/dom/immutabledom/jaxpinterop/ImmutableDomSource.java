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
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.util.Objects;

/**
 * Immutable DOM document as {@link javax.xml.transform.Source}. Inspired by class JAXBSource.
 * <p>
 * This class extends class {@link SAXSource}, which is an implementation detail, but needed in order to plug into
 * the Transformer API. {@link SAXSource} methods such as {@link SAXSource#setXMLReader} and {@link SAXSource#setInputSource}
 * should never be called. Methods of the contained {@link XMLReader} should not be called either.
 * <p>
 * Code using this "SAXSource" and expecting it to return proper {@link XMLReader} objects will fail, so in those
 * cases this class is not useful. For example, this class may be of little use when offering a {@link javax.xml.transform.Source}
 * to factory method {@link javax.xml.validation.SchemaFactory#newSchema()} (using the JAXP stack in the JDK).
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

            @Override
            public boolean getFeature(String name) throws SAXNotRecognizedException {
                return switch (name) {
                    case "http://xml.org/sax/features/namespaces" -> true;
                    case "http://xml.org/sax/features/namespace-prefixes" -> false;
                    default -> throw new SAXNotRecognizedException(name);
                };
            }

            @Override
            public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
                switch (name) {
                    case "http://xml.org/sax/features/namespaces" -> {
                        if (!value) {
                            throw new SAXNotRecognizedException(name);
                        }
                    }
                    case "http://xml.org/sax/features/namespace-prefixes" -> {
                        if (value) {
                            throw new SAXNotRecognizedException(name);
                        }
                    }
                    default -> throw new SAXNotRecognizedException(name);
                }
            }

            @Override
            public Object getProperty(String name) throws SAXNotRecognizedException {
                if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
                    return lexicalHandler;
                } else {
                    throw new SAXNotRecognizedException(name);
                }
            }

            @Override
            public void setProperty(String name, Object value) throws SAXNotRecognizedException {
                if (name.equals("http://xml.org/sax/properties/lexical-handler")) {
                    this.lexicalHandler = (LexicalHandler) value;
                } else {
                    throw new SAXNotRecognizedException(name);
                }
            }

            private LexicalHandler lexicalHandler;

            private EntityResolver entityResolver;

            @Override
            public void setEntityResolver(EntityResolver resolver) {
                this.entityResolver = resolver;
            }

            @Override
            public EntityResolver getEntityResolver() {
                return entityResolver;
            }

            private DTDHandler dtdHandler;

            @Override
            public void setDTDHandler(DTDHandler handler) {
                this.dtdHandler = handler;
            }

            @Override
            public DTDHandler getDTDHandler() {
                return dtdHandler;
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

            private ErrorHandler errorHandler;

            @Override
            public void setErrorHandler(ErrorHandler handler) {
                this.errorHandler = handler;
            }

            @Override
            public ErrorHandler getErrorHandler() {
                return errorHandler;
            }

            @Override
            public void parse(InputSource input) throws SAXException {
                // Ignoring InputSource
                parse();
            }

            @Override
            public void parse(String systemId) throws SAXException {
                // Ignoring systemId
                parse();
            }

            public void parse() throws SAXException {
                try {
                    // Not parsing, but "processing" the Document
                    TransformerHandler th = TransformerHandlers.newTransformerHandler(saxTransformerFactory);
                    SAXResult result = new SAXResult(Objects.requireNonNull(xmlFilter.getContentHandler()));
                    // No system ID set!
                    th.setResult(result);
                    ImmutableDomConsumingSaxEventGenerator eventGenerator = new ImmutableDomConsumingSaxEventGenerator(th);
                    eventGenerator.processDocument(document);
                } catch (RuntimeException e) {
                    var se = new SAXParseException(e.getMessage(), null, null, -1, -1, e);
                    if (errorHandler != null) {
                        errorHandler.fatalError(se);
                    }
                    throw se;
                }
            }
        };
    }
}
