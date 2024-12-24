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

package eu.cdevreeze.yaidom4j.jaxp;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

/**
 * Utility to create SAX parsers and use them.
 * <p>
 * If the SAX handler is an {@link eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler},
 * the SAX parser creates an immutable DOM tree.
 *
 * @author Chris de Vreeze
 */
public class SaxParsers {

    private SaxParsers() {
    }

    /**
     * Creates a namespace-aware non-(DTD-)validating {@link SAXParserFactory}.
     * The factory is aware of XXE attacks and tries to protect against them.
     */
    public static SAXParserFactory newNonValidatingSaxParserFactory() {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultNSInstance();
            saxParserFactory.setValidating(false);
            // See https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
            // Ideally, feature "http://apache.org/xml/features/disallow-doctype-decl" would be set to true
            saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxParserFactory.setXIncludeAware(false);
            saxParserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return saxParserFactory;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link SAXParserFactory} that uses the passed {@link Schema} for schema validation.
     * The factory is aware of XXE attacks and tries to protect against them.
     */
    public static SAXParserFactory newSaxParserFactory(Schema schema) {
        SAXParserFactory spf = newNonValidatingSaxParserFactory();
        spf.setSchema(schema);
        return spf;
    }

    public static void parse(InputSource inputSource, DefaultHandler saxHandler, SAXParserFactory saxParserFactory) {
        try {
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(inputSource, saxHandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parse(InputSource inputSource, DefaultHandler saxHandler) {
        parse(inputSource, saxHandler, newNonValidatingSaxParserFactory());
    }

    public static void parse(URI inputFile, DefaultHandler saxHandler, SAXParserFactory saxParserFactory) {
        try {
            parse(new InputSource(inputFile.toURL().openStream()), saxHandler, saxParserFactory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void parse(URI inputFile, DefaultHandler saxHandler) {
        parse(inputFile, saxHandler, newNonValidatingSaxParserFactory());
    }
}
