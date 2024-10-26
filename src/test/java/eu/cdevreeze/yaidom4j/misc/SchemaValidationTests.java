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
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomSource;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import eu.cdevreeze.yaidom4j.jaxp.SchemaFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for different ways of doing schema validation. The schema set uses xs:import and xs:include,
 * and contains a chameleon schema as well.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class SchemaValidationTests {

    private static Path xmlFilePath;
    private static Path invalidXmlFilePath;
    private static Path xsdFilePath;

    @BeforeAll
    static void init() {
        try {
            xmlFilePath = Path.of(Objects.requireNonNull(SchemaValidationTests.class.getResource("/schema/chapter04.xml")).toURI());
            invalidXmlFilePath = Path.of(Objects.requireNonNull(SchemaValidationTests.class.getResource("/schema/chapter04-invalid.xml")).toURI());
            xsdFilePath = Path.of(Objects.requireNonNull(SchemaValidationTests.class.getResource("/schema/chapter04ord1.xsd")).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testNoSchemaValidation() {
        DocumentParser parser = DocumentParsers.instance();
        Document document = parser.parse(xmlFilePath.toUri());

        assertTrue(document.documentElement().elementStream().count() >= 10);

        Document invalidDocument = parser.parse(invalidXmlFilePath.toUri());

        assertTrue(invalidDocument.documentElement().elementStream().count() >= 10);
    }

    @Test
    void testSchemaValidation() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactories.newSchemaFactory();
        schemaFactory.setResourceResolver(getCatalogResolver());
        // Using schema hints in document for imports and includes (using the catalog as well)
        // I also tried to call method newSchema without parameters, but did not get that to work
        Schema schema = schemaFactory.newSchema(xsdFilePath.toFile());

        DocumentParser parser = DocumentParsers.builder(SaxParsers.newSaxParserFactory(schema)).build();

        // Parse and validate valid instance

        Document document = parser.parse(xmlFilePath.toUri());

        assertTrue(document.documentElement().elementStream().count() >= 10);

        Validator validator = schema.newValidator();
        validator.validate(new ImmutableDomSource(document));

        // Parse and validate invalid instance (parsing itself succeeds, since we set no error resolution)

        Document invalidDocument = parser.parse(invalidXmlFilePath.toUri());

        assertTrue(invalidDocument.documentElement().elementStream().count() >= 10);

        validator.reset();
        assertThrows(SAXParseException.class, () -> validator.validate(new ImmutableDomSource(invalidDocument)));
    }

    private static CatalogResolver getCatalogResolver() {
        try {
            URI catalogUri = Objects.requireNonNull(SchemaValidationTests.class
                    .getResource("/schema/catalog.xml")).toURI();
            CatalogFeatures catalogFeatures = CatalogFeatures.builder()
                    .with(CatalogFeatures.Feature.PREFER, "system")
                    .build();
            return CatalogManager.catalogResolver(catalogFeatures, catalogUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
