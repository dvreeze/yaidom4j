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

package eu.cdevreeze.yaidom4j.dom.clark;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static eu.cdevreeze.yaidom4j.dom.clark.ClarkElementPredicates.hasAttributeValue;
import static eu.cdevreeze.yaidom4j.dom.clark.ClarkElementPredicates.hasName;
import static eu.cdevreeze.yaidom4j.dom.clark.ClarkElementSteps.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * XBRL instance query tests using plain XML querying with element steps.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XbrlInstanceXmlQueryTests {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String ISO4217_NS = "http://www.xbrl.org/2003/iso4217";

    private static final String GAAP_NS = "http://xasb.org/gaap";

    private ClarkNodes.Element xbrlDocElem;

    private final NamespaceScope scope = NamespaceScope.of("gaap", GAAP_NS)
            .resolve("iso4217", ISO4217_NS)
            .resolve("", XBRLI_NS);

    @BeforeAll
    void parseDocument() {
        InputStream inputStream = XbrlInstanceXmlQueryTests.class.getResourceAsStream("/sample-xbrl-instance.xml");
        this.xbrlDocElem = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream))
                .documentElement()
                .toClarkNode();
    }

    @Test
    void testQueryDimensionMembers() {
        ClarkNodes.Element context = xbrlDocElem.select(
                selfElement(hasName(XBRLI_NS, "xbrl"))
                        .then(childElements(hasName(XBRLI_NS, "context"))
                                .where(hasAttributeValue("id", "I-2005"))
                        )
        ).findFirst().orElseThrow();

        ImmutableMap<QName, QName> dimensionMembers =
                context.select(
                                childElements(hasName(XBRLI_NS, "entity"))
                                        .then(descendantElements(hasName(XBRLDI_NS, "explicitMember")))
                        ).map(this::explicitDimensionMember)
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
        ImmutableList<ClarkNodes.Element> landFacts = xbrlDocElem.select(
                childElements(hasName(GAAP_NS, "Land"))
        ).collect(ImmutableList.toImmutableList());

        assertEquals(3, landFacts.size());

        ImmutableList<ClarkNodes.Element> landFacts2005 = landFacts
                .stream()
                .filter(hasAttributeValue("contextRef", "I-2005"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, landFacts2005.size());

        ClarkNodes.Element landFact2005 = landFacts2005.get(0);

        ClarkNodes.Element context = xbrlDocElem.select(
                childElements(hasName(XBRLI_NS, "context"))
                        .where(hasAttributeValue("id", landFact2005.attribute(new QName("contextRef"))))
        ).findFirst().orElseThrow();

        assertNotNull(context);

        ImmutableMap<QName, QName> dimensionMembers =
                context.select(
                                childElements(hasName(XBRLI_NS, "entity"))
                                        .then(descendantElements(hasName(XBRLDI_NS, "explicitMember")))
                        ).map(this::explicitDimensionMember)
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
        ImmutableList<ClarkNodes.Element> landFacts = xbrlDocElem.select(
                        childElements(hasName(GAAP_NS, "Land"))
                )
                .collect(ImmutableList.toImmutableList());

        assertEquals(3, landFacts.size());

        ImmutableList<ClarkNodes.Element> landFacts2007 = landFacts
                .stream()
                .filter(hasAttributeValue("contextRef", "I-2007"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, landFacts2007.size());

        ClarkNodes.Element landFact2007 = landFacts2007.get(0);

        ClarkNodes.Element unit = xbrlDocElem.select(
                        childElements(hasName(XBRLI_NS, "unit"))
                                .where(hasAttributeValue("id", landFact2007.attribute(new QName("unitRef"))))
                )
                .findFirst()
                .orElseThrow();

        assertNotNull(unit);

        String unitRef = landFact2007.attributeOption(new QName("unitRef")).orElse("");

        List<QName> measures = xbrlDocElem.select(
                        childElements(hasName(XBRLI_NS, "unit"))
                                .where(hasAttributeValue("id", unitRef))
                )
                .findFirst()
                .orElseThrow()
                .select(
                        childElements(hasName(XBRLI_NS, "measure"))
                )
                .map(e -> scope.resolveSyntacticQNameInContent(e.text()))
                .toList();

        ImmutableList<QName> expectedMeasures = ImmutableList.of(new QName(ISO4217_NS, "USD"));

        assertEquals(expectedMeasures, measures);
    }

    @Test
    void testQueryItemFactValue() {
        ImmutableList<ClarkNodes.Element> preferredStockAmountFacts = xbrlDocElem.select(
                childElements(hasName(GAAP_NS, "PreferredStockAmount"))
        ).collect(ImmutableList.toImmutableList());

        assertEquals(6, preferredStockAmountFacts.size());

        ImmutableList<ClarkNodes.Element> preferredStockAmountFacts2007PsAll = preferredStockAmountFacts
                .stream()
                .filter(f -> f.attribute(new QName("contextRef")).equals("I-2007-PS-All"))
                .collect(ImmutableList.toImmutableList());

        assertEquals(1, preferredStockAmountFacts2007PsAll.size());

        ClarkNodes.Element preferredStockAmountFact2007PsAll = preferredStockAmountFacts2007PsAll.get(0);

        assertEquals(String.valueOf(2000), preferredStockAmountFact2007PsAll.text().strip());
    }

    private Map.Entry<QName, QName> explicitDimensionMember(ClarkNodes.Element explicitDimension) {
        Preconditions.checkArgument(hasName(XBRLDI_NS, "explicitMember").test(explicitDimension));

        return Map.entry(
                scope.resolveSyntacticQNameInContent(explicitDimension.attribute(new QName("dimension"))),
                scope.resolveSyntacticQNameInContent(explicitDimension.text().strip())
        );
    }
}
