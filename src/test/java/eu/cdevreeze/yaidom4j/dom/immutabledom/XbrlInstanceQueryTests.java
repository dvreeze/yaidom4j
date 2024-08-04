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

import com.google.common.collect.ImmutableList;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * XBRL instance query tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XbrlInstanceQueryTests {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String ISO4217_NS = "http://www.xbrl.org/2003/iso4217";

    private static final String GAAP_NS = "http://xasb.org/gaap";

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
        Document doc = Documents.removeInterElementWhitespace(saxHandler.resultingDocument());
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

    @Test
    void testQueryItemFactContext() {
        ImmutableList<TestXbrlInstances.ItemFact> landFacts = instance.topLevelItemFacts(new QName(GAAP_NS, "Land"));

        assertEquals(3, landFacts.size());

        ImmutableList<TestXbrlInstances.ItemFact> landFacts2005 = landFacts
                .stream()
                .filter(f -> f.contextRef().equals("I-2005"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, landFacts2005.size());

        TestXbrlInstances.ItemFact landFact2005 = landFacts2005.get(0);

        TestXbrlInstances.Context context = instance.contexts().get(landFact2005.contextRef());

        assertNotNull(context);

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

    @Test
    void testQueryItemFactUnit() {
        ImmutableList<TestXbrlInstances.ItemFact> landFacts = instance.topLevelItemFacts(new QName(GAAP_NS, "Land"));

        assertEquals(3, landFacts.size());

        ImmutableList<TestXbrlInstances.ItemFact> landFacts2007 = landFacts
                .stream()
                .filter(f -> f.contextRef().equals("I-2007"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, landFacts2007.size());

        TestXbrlInstances.ItemFact landFact2007 = landFacts2007.get(0);

        TestXbrlInstances.Unit unit = instance.units().get(landFact2007.unitRefOption().orElse(""));

        assertNotNull(unit);

        String unitRef = landFact2007.unitRefOption().orElse("");

        List<QName> measures = Objects.requireNonNull(instance.units().get(unitRef))
                .measures()
                .stream()
                .map(TestXbrlInstances.Measure::measure)
                .toList();

        ImmutableList<QName> expectedMeasures = ImmutableList.of(new QName(ISO4217_NS, "USD"));

        assertEquals(expectedMeasures, measures);
    }

    @Test
    void testQueryItemFactValue() {
        ImmutableList<TestXbrlInstances.ItemFact> preferredStockAmountFacts = instance.topLevelItemFacts(new QName(GAAP_NS, "PreferredStockAmount"));

        assertEquals(6, preferredStockAmountFacts.size());

        ImmutableList<TestXbrlInstances.ItemFact> preferredStockAmountFacts2007PsAll = preferredStockAmountFacts
                .stream()
                .filter(f -> f.contextRef().equals("I-2007-PS-All"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, preferredStockAmountFacts2007PsAll.size());

        TestXbrlInstances.ItemFact preferredStockAmountFact2007PsAll = preferredStockAmountFacts2007PsAll.get(0);

        assertEquals(String.valueOf(2000), preferredStockAmountFact2007PsAll.rawFactValue().strip());
    }
}
