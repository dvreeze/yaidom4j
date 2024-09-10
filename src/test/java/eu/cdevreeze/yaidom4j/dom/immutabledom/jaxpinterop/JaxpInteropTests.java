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
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JAXP interop tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing, node equality comparisons etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JaxpInteropTests {

    private Document doc;

    @BeforeAll
    void parseDocuments() {
        doc = parseDocument("/feed1.xml");
    }

    private Document parseDocument(String xmlClasspathResource) {
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = JaxpInteropTests.class.getResourceAsStream(xmlClasspathResource);
        SaxParsers.parse(new InputSource(inputStream), saxHandler);
        return saxHandler.resultingDocument().removeInterElementWhitespace()
                .withUri(URI.create("http://example.com/feed1.xml"));
    }

    @Test
    void testEquivalenceAfterTransforming() throws TransformerException {
        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        ImmutableDomResult result = new ImmutableDomResult();
        tf.newTransformer().transform(new ImmutableDomSource(doc), result);
        Document doc2 = result.getDocument();

        assertEquals(doc.documentElement().toClarkNode(), doc2.documentElement().toClarkNode());
    }

    @Test
    void testEquivalenceAfterTransformingMultipleTimes() throws TransformerException {
        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        StringWriter sw = new StringWriter();
        Result streamResult = new StreamResult(sw);
        tf.newTransformer().transform(new ImmutableDomSource(doc), streamResult);
        String xmlString = sw.toString();

        StreamSource streamSource = new StreamSource(new StringReader(xmlString));
        ImmutableDomResult result = new ImmutableDomResult();
        tf.newTransformer().transform(streamSource, result);
        Document doc2 = result.getDocument();

        assertEquals(doc.documentElement().toClarkNode(), doc2.documentElement().toClarkNode());
    }
}
