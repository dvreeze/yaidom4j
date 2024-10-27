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

import com.google.common.base.Preconditions;
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

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for different ways of doing schema validation. The (original) schema set uses xs:import and xs:include,
 * and contains a chameleon schema as well.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class SchemaValidationTests {

    private static URL xmlFileUrl;
    private static URL invalidXmlFileUrl;
    private static URL xsdFileUrl;

    @BeforeAll
    static void init() {
        xmlFileUrl = getClassPathResource("/schema/chapter04.xml");
        invalidXmlFileUrl = getClassPathResource("/schema/chapter04-invalid.xml");
        xsdFileUrl = getClassPathResource("/schema/chapter04ord1.xsd");
    }

    @Test
    void testNoSchemaValidation() {
        DocumentParser parser = DocumentParsers.instance();
        Document document = parser.parse(convertToUri(xmlFileUrl));

        assertTrue(document.documentElement().elementStream().count() >= 10);

        Document invalidDocument = parser.parse(convertToUri(invalidXmlFileUrl));

        assertTrue(invalidDocument.documentElement().elementStream().count() >= 10);
    }

    // There is no test method trying out calling method "SchemaFactory.newSchema()" without arguments
    // Not only would it feel wrong (the schema should be there before using it), but it also seems hard to get working

    @Test
    void testSchemaValidationUsingSchemaLocationHints() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactories.newSchemaFactory();
        schemaFactory.setResourceResolver(getCatalogResolver());
        // Using schema hints in document for imports and includes (using the catalog as well)
        // Note the possibility of denial-of-service attacks
        Schema schema = schemaFactory.newSchema(xsdFileUrl); // passing just the "entry point schema document URL"

        DocumentParser parser = DocumentParsers.builder(SaxParsers.newSaxParserFactory(schema)).build();

        // Parse and validate valid instance

        Document document = parser.parse(convertToUri(xmlFileUrl));

        assertTrue(document.documentElement().elementStream().count() >= 10);

        Validator validator = schema.newValidator();
        validator.validate(new ImmutableDomSource(document));

        // Parse and validate invalid instance (parsing itself succeeds, since we set no error resolution)

        Document invalidDocument = parser.parse(convertToUri(invalidXmlFileUrl));

        assertTrue(invalidDocument.documentElement().elementStream().count() >= 10);

        validator.reset();
        assertThrows(
                SAXParseException.class,
                () -> validator.validate(new ImmutableDomSource(invalidDocument))
        );
    }

    @Test
    void testSchemaValidationPassingFullSchemaSet() throws SAXException, IOException {
        DocumentParser docParserForSchema = DocumentParsers.instance();

        // This time we avoid xs:include, and parse an adapted schema that only uses xs:import
        // Obviously, we do not have to first parse and check the schema documents, but we can if we want to

        List<URL> schemaDocUrls =
                Stream.of("chapter04prod.xsd", "chapter04ord1-adapted.xsd") // order matters!
                        .map(name -> "/schema/" + name)
                        .map(SchemaValidationTests::getClassPathResource)
                        .toList();

        List<Document> schemaDocs =
                schemaDocUrls.stream()
                        .map(u -> docParserForSchema.parse(convertToUri(u)))
                        .toList();

        Preconditions.checkArgument(
                schemaDocs.stream()
                        .allMatch(d -> hasName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema").test(d.documentElement()))
        );

        SchemaFactory schemaFactory = SchemaFactories.newSchemaFactory();
        // This time we need no XML catalog to resolve schema document URLs, because we pass all schema document URLs below

        // We possibly need to pass (stable) URLs rather than input streams (which can only be processed once)
        // Passing ImmutableDomSource objects does not work, because the SchemaFactory implementation expects this "SAXSource" to be a proper SAXSource
        Schema schema =
                schemaFactory.newSchema(
                        schemaDocUrls.stream().map(u -> new StreamSource(u.toExternalForm())).toArray(Source[]::new)
                );

        DocumentParser parser = DocumentParsers.builder(SaxParsers.newSaxParserFactory(schema)).build();

        // Parse and validate valid instance

        Document document = parser.parse(convertToUri(xmlFileUrl));

        assertTrue(document.documentElement().elementStream().count() >= 10);

        Validator validator = schema.newValidator();
        validator.validate(new ImmutableDomSource(document));

        // Parse and validate invalid instance (parsing itself succeeds, since we set no error resolution)

        Document invalidDocument = parser.parse(convertToUri(invalidXmlFileUrl));

        assertTrue(invalidDocument.documentElement().elementStream().count() >= 10);

        validator.reset();
        assertThrows(
                SAXParseException.class,
                () -> validator.validate(new ImmutableDomSource(invalidDocument))
        );

        // Do the above once again, but this time create the Schema from InputStream objects (which does work in this case)

        Schema schema2 =
                schemaFactory.newSchema(
                        schemaDocUrls.stream().map(u -> {
                            try {
                                return new StreamSource(u.openStream());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).toArray(Source[]::new)
                );

        DocumentParser parser2 = DocumentParsers.builder(SaxParsers.newSaxParserFactory(schema2)).build();

        // Parse and validate valid instance

        Document document2 = parser2.parse(convertToUri(xmlFileUrl));

        assertTrue(document2.documentElement().elementStream().count() >= 10);

        Validator validator2 = schema2.newValidator();
        validator2.validate(new ImmutableDomSource(document2));

        // Parse and validate invalid instance (parsing itself succeeds, since we set no error resolution)

        Document invalidDocument2 = parser2.parse(convertToUri(invalidXmlFileUrl));

        assertTrue(invalidDocument2.documentElement().elementStream().count() >= 10);

        validator2.reset();
        assertThrows(
                SAXParseException.class,
                () -> validator2.validate(new ImmutableDomSource(invalidDocument2))
        );
    }

    private static CatalogResolver getCatalogResolver() {
        URI catalogUri = convertToUri(getClassPathResource("/schema/catalog.xml"));
        CatalogFeatures catalogFeatures = CatalogFeatures.builder()
                .with(CatalogFeatures.Feature.PREFER, "system")
                .build();
        return CatalogManager.catalogResolver(catalogFeatures, catalogUri);
    }

    private static URI convertToUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL getClassPathResource(String classPathResource) {
        return Objects.requireNonNull(SchemaValidationTests.class.getResource(classPathResource));
    }
}
