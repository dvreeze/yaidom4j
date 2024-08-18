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

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

/**
 * Opinionated utility to create W3C DOM parsers and use them.
 * <p>
 * The parsed DOM tree can be converted to an immutable DOM tree native to yaidom4j, using class
 * "JaxpDomToImmutableDomConverter".
 *
 * @author Chris de Vreeze
 */
public class DocumentBuilders {

    private DocumentBuilders() {
    }

    /**
     * Creates a namespace-aware non-validating DocumentBuilderFactory
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            docBuilderFactory.setNamespaceAware(true); // Important!
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return docBuilderFactory;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parse(InputSource inputSource, DocumentBuilderFactory documentBuilderFactory) {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            return documentBuilder.parse(inputSource);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Document parse(InputSource inputSource) {
        return parse(inputSource, newDocumentBuilderFactory());
    }

    public static Document parse(URI inputFile, DocumentBuilderFactory documentBuilderFactory) {
        try {
            return parse(new InputSource(inputFile.toURL().openStream()), documentBuilderFactory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Document parse(URI inputFile) {
        return parse(inputFile, newDocumentBuilderFactory());
    }
}
