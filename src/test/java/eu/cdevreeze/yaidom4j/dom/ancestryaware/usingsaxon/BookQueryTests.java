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
import eu.cdevreeze.yaidom4j.testsupport.saxon.SaxonElementPredicates;
import eu.cdevreeze.yaidom4j.testsupport.saxon.SaxonNodes;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Immutable DOM query tests against books.xml file. This test class uses Saxon bootstrapping.
 * Using TrAX (Transformation API for XML), a Saxon XdmNode (as TrAX Source) is transformed to a SAX
 * Result that creates an ancestry-aware immutable DOM tree. The latter is achieved using
 * ImmutableDomProducingSaxHandler as SAX ContentHandler. Once the yaidom4j native document has been
 * created, this test class uses it as input document for the tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class BookQueryTests extends AbstractBookQueryTests<SaxonNodes.Element> {

    private static final String NS = "http://bookstore";

    private static SaxonNodes.Element rootElement;

    private static final SaxonElementPredicates.Factory epf = new SaxonElementPredicates.Factory();

    @BeforeAll
    protected static void parseRootElement() throws SAXException, ParserConfigurationException, IOException, SaxonApiException, TransformerException {
        Processor processor = new Processor(false);
        DocumentBuilder docBuilder = processor.newDocumentBuilder();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/books.xml");
        XdmNode xdmDoc = docBuilder.build(new StreamSource(inputStream));

        rootElement = new SaxonNodes.Element(xdmDoc.getOutermostElement());
    }

    @Override
    protected SaxonNodes.Element rootElement() {
        return rootElement;
    }

    @Override
    protected SaxonElementPredicates.Factory epf() {
        return epf;
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
