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

package eu.cdevreeze.yaidom4j.dom.ancestryaware.usingsaxon;

import eu.cdevreeze.yaidom4j.dom.AbstractBookQueryTests;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.Document;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementPredicates;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementTree.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Documents;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import net.sf.saxon.BasicTransformerFactory;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Immutable DOM query tests against books.xml file. This test class uses Saxon bootstrapping.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class BookQueryTests extends AbstractBookQueryTests<Element> {

    private static final String NS = "http://bookstore";

    private static Element rootElement;

    @BeforeAll
    protected static void parseRootElement() throws SAXException, ParserConfigurationException, IOException, SaxonApiException, TransformerException {
        Processor processor = new Processor(false);
        DocumentBuilder docBuilder = processor.newDocumentBuilder();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        XdmNode xdmDoc = docBuilder.build(new StreamSource(inputStream));

        TransformerFactory tff = new BasicTransformerFactory(processor.getUnderlyingConfiguration());
        Transformer tf = tff.newTransformer();

        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        tf.transform(xdmDoc.asSource(), new SAXResult(saxHandler));

        var underlyingDoc = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
        var doc = Document.from(underlyingDoc);
        rootElement = doc.documentElement();
    }

    @Override
    protected Element rootElement() {
        return rootElement;
    }

    @Override
    protected Predicate<Element> hasName(String namespace, String localName) {
        return ElementPredicates.hasName(namespace, localName);
    }

    @Override
    protected Predicate<Element> hasAttribute(String noNamespaceAttrName, String attrValue) {
        return ElementPredicates.hasAttribute(noNamespaceAttrName, attrValue);
    }

    @Override
    protected Predicate<Element> hasOnlyText(String text) {
        return ElementPredicates.hasOnlyText(text);
    }

    @Override
    protected Predicate<Element> hasOnlyStrippedText(String text) {
        return ElementPredicates.hasOnlyStrippedText(text);
    }

    @Test
    void testParentOfChildElements() {
        var allElements = rootElement().descendantElementOrSelfStream().toList();

        assertTrue(allElements.size() > 10);

        assertTrue(
                allElements.stream().allMatch(
                        e -> e.childElementStream().allMatch(che -> che.parentElementOption().orElseThrow().equals(e))
                )
        );
    }
}
