/*
 * Copyright 2024-2026 Chris de Vreeze
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

package eu.cdevreeze.yaidom4j.experimental.misc;

import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.experimental.jaxp.SchemaFactories;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

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
            xmlFilePath = Path.of(Objects.requireNonNull(IgnorableWhitespaceTests.class.getResource("/bestellung.xml")).toURI());
            xsdFilePath = Path.of(Objects.requireNonNull(IgnorableWhitespaceTests.class.getResource("/bestellung.xsd")).toURI());
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
                        .descendantOrSelfElementStream(e -> e.text().isBlank() && !e.text().isEmpty())
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
                        .descendantOrSelfElementStream(e -> e.text().isBlank() && !e.text().isEmpty())
                        .count() >= 5
        );

        Document otherDoc =
                DocumentParsers.builder().removingInterElementWhitespace().build().parse(xmlFilePath.toUri());

        assertNotEquals(
                otherDoc.documentElement().greenTreeNode().toClarkNode(),
                document.documentElement().greenTreeNode().toClarkNode()
        );
        assertEquals(
                otherDoc.documentElement().greenTreeNode().toClarkNode(),
                document.documentElement().greenTreeNode().removeInterElementWhitespace().toClarkNode()
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
                        .descendantOrSelfElementStream(e -> e.text().isBlank() && !e.text().isEmpty())
                        .findAny()
                        .isEmpty()
        );

        Document otherDoc =
                DocumentParsers.builder().removingInterElementWhitespace().build().parse(xmlFilePath.toUri());

        assertEquals(
                otherDoc.documentElement().greenTreeNode().toClarkNode(),
                document.documentElement().greenTreeNode().toClarkNode()
        );
    }
}
