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

package eu.cdevreeze.yaidom4j.experimental.domtest;

import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees.Element;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees.ElementPredicates.hasAttributeValue;
import static eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees.ElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * XBRL instance query tests using plain XML querying.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClarkXbrlInstanceXmlQueryTests {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String ISO4217_NS = "http://www.xbrl.org/2003/iso4217";

    private static final String GAAP_NS = "http://xasb.org/gaap";

    private Document xbrlDoc;

    private final NamespaceScope scope = NamespaceScope.of("gaap", GAAP_NS)
            .resolve("iso4217", ISO4217_NS)
            .resolve("", XBRLI_NS);

    @BeforeAll
    void parseDocument() {
        InputStream inputStream = ClarkXbrlInstanceXmlQueryTests.class.getResourceAsStream("/sample-xbrl-instance.xml");
        this.xbrlDoc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream))
                .greenTreeNode()
                .toClarkNode();
    }

    @Test
    void testQueryDimensionMembers() {
        Element context = Stream.of(xbrlDoc.documentElement())
                .filter(hasName(XBRLI_NS, "xbrl"))
                .flatMap(e -> e.childElementStream(hasName(XBRLI_NS, "context")))
                .filter(hasAttributeValue("id", "I-2005"))
                .findFirst().orElseThrow();

        Map<QName, QName> dimensionMembers =
                context.childElementStream(hasName(XBRLI_NS, "entity"))
                        .flatMap(e -> e.descendantElementStream(hasName(XBRLDI_NS, "explicitMember")))
                        .map(this::explicitDimensionMember)
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<QName, QName> expectedDimensionMembers = Map.of(
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
        List<Element> landFacts = xbrlDoc.documentElement()
                .childElementStream(hasName(GAAP_NS, "Land"))
                .toList();

        assertEquals(3, landFacts.size());

        List<Element> landFacts2005 = landFacts
                .stream()
                .filter(hasAttributeValue("contextRef", "I-2005"))
                .toList();

        assertEquals(1, landFacts2005.size());

        Element landFact2005 = landFacts2005.getFirst();

        Element context = xbrlDoc.documentElement()
                .childElementStream(hasName(XBRLI_NS, "context"))
                .filter(hasAttributeValue("id", landFact2005.attribute(new QName("contextRef"))))
                .findFirst().orElseThrow();

        assertNotNull(context);

        Map<QName, QName> dimensionMembers =
                context.childElementStream(hasName(XBRLI_NS, "entity"))
                        .flatMap(e -> e.descendantElementStream(hasName(XBRLDI_NS, "explicitMember")))
                        .map(this::explicitDimensionMember)
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<QName, QName> expectedDimensionMembers = Map.of(
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
        List<Element> landFacts = xbrlDoc.documentElement()
                .childElementStream(hasName(GAAP_NS, "Land"))
                .toList();

        assertEquals(3, landFacts.size());

        List<Element> landFacts2007 = landFacts
                .stream()
                .filter(hasAttributeValue("contextRef", "I-2007"))
                .toList();

        assertEquals(1, landFacts2007.size());

        Element landFact2007 = landFacts2007.getFirst();

        Element unit = xbrlDoc.documentElement()
                .childElementStream(hasName(XBRLI_NS, "unit"))
                .filter(hasAttributeValue("id", landFact2007.attribute(new QName("unitRef"))))
                .findFirst()
                .orElseThrow();

        assertNotNull(unit);

        String unitRef = landFact2007.attributeOption(new QName("unitRef")).orElse("");

        List<QName> measures = xbrlDoc.documentElement()
                .childElementStream(hasName(XBRLI_NS, "unit"))
                .filter(hasAttributeValue("id", unitRef))
                .findFirst()
                .orElseThrow()
                .childElementStream(hasName(XBRLI_NS, "measure"))
                .map(e -> scope.resolveSyntacticQNameInContent(e.text()))
                .toList();

        List<QName> expectedMeasures = List.of(new QName(ISO4217_NS, "USD"));

        assertEquals(expectedMeasures, measures);
    }

    @Test
    void testQueryItemFactValue() {
        List<Element> preferredStockAmountFacts = xbrlDoc.documentElement()
                .childElementStream(hasName(GAAP_NS, "PreferredStockAmount"))
                .toList();

        assertEquals(6, preferredStockAmountFacts.size());

        List<Element> preferredStockAmountFacts2007PsAll = preferredStockAmountFacts
                .stream()
                .filter(f -> f.attribute(new QName("contextRef")).equals("I-2007-PS-All"))
                .toList();

        assertEquals(1, preferredStockAmountFacts2007PsAll.size());

        Element preferredStockAmountFact2007PsAll = preferredStockAmountFacts2007PsAll.getFirst();

        assertEquals(String.valueOf(2000), preferredStockAmountFact2007PsAll.text().strip());
    }

    private Map.Entry<QName, QName> explicitDimensionMember(Element explicitDimension) {
        PreconditionUtil.checkArgument(hasName(XBRLDI_NS, "explicitMember").test(explicitDimension));

        return Map.entry(
                scope.resolveSyntacticQNameInContent(explicitDimension.attribute(new QName("dimension"))),
                scope.resolveSyntacticQNameInContent(explicitDimension.text().strip())
        );
    }
}
