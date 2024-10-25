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

package eu.cdevreeze.yaidom4j.misc;

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.jaxp.SchemaFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for different scenarios around ignorable whitespace, with and without schema validation.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class IgnorableWhitespaceTests {

    private static Path xmlFilePath;
    private static Path xsdFilePath;

    @BeforeAll
    static void init() {
        try {
            xmlFilePath = Path.of(IgnorableWhitespaceTests.class.getResource("/bestellung.xml").toURI());
            xsdFilePath = Path.of(IgnorableWhitespaceTests.class.getResource("/bestellung.xsd").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testParsingWithoutSchemaButRemovingWhitespace() {
        DocumentParser parser = DocumentParsers.builder().removingInterElementWhitespace().build();
        Document document = parser.parse(xmlFilePath.toUri());

        assertTrue(
                document.documentElement()
                        .elementStream(e -> e.text().isBlank() && !e.text().isEmpty())
                        .findAny()
                        .isEmpty()
        );
    }

    @Test
    void testParsingWithoutSchema() {
        DocumentParser parser = DocumentParsers.builder().build();
        Document document = parser.parse(xmlFilePath.toUri());

        assertTrue(
                document.documentElement()
                        .elementStream(e -> e.text().isBlank() && !e.text().isEmpty())
                        .count() >= 5
        );

        Document otherDoc =
                DocumentParsers.builder().removingInterElementWhitespace().build().parse(xmlFilePath.toUri());

        assertNotEquals(
                otherDoc.documentElement().toClarkNode(),
                document.documentElement().toClarkNode()
        );
        assertEquals(
                otherDoc.documentElement().toClarkNode(),
                document.documentElement().removeInterElementWhitespace().toClarkNode()
        );
    }

    @Test
    void testParsingUsingSchema() throws SAXException {
        DocumentParser parser = DocumentParsers
                .builder(
                        SaxParsers.newSaxParserFactory(
                                SchemaFactories.newSchemaFactory().newSchema(xsdFilePath.toFile())
                        )
                )
                .build();
        Document document = parser.parse(xmlFilePath.toUri());

        assertTrue(
                document.documentElement()
                        .elementStream(e -> e.text().isBlank() && !e.text().isEmpty())
                        .findAny()
                        .isEmpty()
        );

        Document otherDoc =
                DocumentParsers.builder().removingInterElementWhitespace().build().parse(xmlFilePath.toUri());

        assertEquals(
                otherDoc.documentElement().toClarkNode(),
                document.documentElement().toClarkNode()
        );
    }
}
