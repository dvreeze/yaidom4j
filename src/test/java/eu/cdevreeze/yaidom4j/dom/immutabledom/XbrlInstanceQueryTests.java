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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * XBRL instance query tests.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XbrlInstanceQueryTests {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String GAAP_NS = "http://xasb.org/gaap";

    private Document doc;
    private TestXbrlInstances.XbrlInstance instance;

    @BeforeAll
    void parseDocument() throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxParserFactory = SAXParserFactory.newDefaultInstance();
        saxParserFactory.setNamespaceAware(true); // Important!
        saxParserFactory.setValidating(false);
        saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SAXParser parser = saxParserFactory.newSAXParser();
        ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();

        InputStream inputStream = BookQueryTests.class.getResourceAsStream("/sample-xbrl-instance.xml");
        parser.parse(new InputSource(inputStream), saxHandler);
        doc = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
        instance = TestXbrlInstances.XbrlInstance.from(doc.documentElement());
    }

    @Test
    void testQueryDimensionMembers() {
        TestXbrlInstances.Context context = Objects.requireNonNull(instance.contexts().get("I-2005"));
        ImmutableMap<QName, QName> dimensionMembers =
                context.entity().segmentOption().orElseThrow()
                        .explicitMembers()
                        .stream()
                        .map(e -> Map.entry(e.dimension(), e.member()))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        ImmutableMap<QName, QName> expectedDimensionMembers = ImmutableMap.of(
                new QName(GAAP_NS, "EntityAxis"), new QName(GAAP_NS, "ABCCompanyDomain"),
                new QName(GAAP_NS, "BusinessSegmentAxis"), new QName(GAAP_NS, "ConsolidatedGroupDomain"),
                new QName(GAAP_NS, "VerificationAxis"), new QName(GAAP_NS, "UnqualifiedOpinionMember"),
                new QName(GAAP_NS, "PremiseAxis"), new QName(GAAP_NS, "ActualMember"),
                new QName(GAAP_NS, "ReportDateAxis"), new QName(GAAP_NS, "ReportedAsOfMarch182008Member")
        );

        assertEquals(expectedDimensionMembers, dimensionMembers);
    }
}
