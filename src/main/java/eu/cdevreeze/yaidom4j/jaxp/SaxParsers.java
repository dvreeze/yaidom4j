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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

/**
 * Opinionated utility to create SAX parsers and use them.
 * <p>
 * If the SAX handler is an "ImmutableDomProducingSaxHandler", the SAX parser creates an immutable DOM tree.
 *
 * @author Chris de Vreeze
 */
public class SaxParsers {

    private SaxParsers() {
    }

    /**
     * Creates a namespace-aware non-validating SAXParserFactory
     */
    public static SAXParserFactory newSaxParserFactory() {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
            saxParserFactory.setNamespaceAware(true); // Important!
            saxParserFactory.setValidating(false);
            saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return saxParserFactory;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
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
        parse(inputSource, saxHandler, newSaxParserFactory());
    }

    public static void parse(URI inputFile, DefaultHandler saxHandler, SAXParserFactory saxParserFactory) {
        try {
            parse(new InputSource(inputFile.toURL().openStream()), saxHandler, saxParserFactory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void parse(URI inputFile, DefaultHandler saxHandler) {
        parse(inputFile, saxHandler, newSaxParserFactory());
    }
}
