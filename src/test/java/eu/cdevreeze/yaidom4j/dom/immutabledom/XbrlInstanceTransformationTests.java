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
import eu.cdevreeze.yaidom4j.dom.immutabledom.comparison.NodeComparisons;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XBRL instance transformation tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XbrlInstanceTransformationTests {

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
    void testAddFact() {
        String ns = "http://dummyns";

        Element newFactElem = new Element(
                new QName(ns, "Loans", "d"),
                ImmutableMap.of(
                        new QName("contextRef"), "I-2007",
                        new QName("unitRef"), "U-Monetary",
                        new QName("decimals"), "INF"
                ),
                instance.element().namespaceScope().resolve("d", ns),
                ImmutableList.of(new Text(String.valueOf(2500), false))
        );

        TestXbrlInstances.XbrlInstance newInstance = TestXbrlInstances.XbrlInstance.from(
                Elements.transformChildElementsToNodeLists(
                        instance.element(),
                        e -> {
                            if (e.name().equals(new QName(GAAP_NS, "Loans")) &&
                                    new TestXbrlInstances.ItemFact(e).contextRef().equals("I-2007")) {
                                return List.of(newFactElem, e);
                            } else {
                                return List.of(e);
                            }
                        }
                )
        );

        ImmutableList<TestXbrlInstances.ItemFact> newFacts =
                newInstance.topLevelItemFacts(new QName(ns, "Loans"));

        assertEquals(1, newFacts.size());

        TestXbrlInstances.ItemFact newFact = newFacts.get(0);

        assertEquals("I-2007", newFact.contextRef());
        assertEquals("U-Monetary", newFact.unitRefOption().orElse(""));
        assertEquals("2500", newFact.rawFactValue());

        assertEquals(1, instance.element().elementStream().map(Element::namespaceScope).distinct().count());
        assertEquals(2, newInstance.element().elementStream().map(Element::namespaceScope).distinct().count());

        assertEquals(instance.element().elementStream().count() + 1, newInstance.element().elementStream().count());

        TestXbrlInstances.XbrlInstance newInstance2 = TestXbrlInstances.XbrlInstance.from(
                Elements.notUndeclaringPrefixes(
                        newInstance.element(),
                        instance.element().namespaceScope().resolve("d", ns)
                )
        );

        assertEquals(1, newInstance2.element().elementStream().map(Element::namespaceScope).distinct().count());

        assertEquals(instance.element().elementStream().count() + 1, newInstance.element().elementStream().count());

        var nodeComparison = new NodeComparisons.DefaultEqualityComparison();

        assertTrue(nodeComparison.areEqual(newInstance.element(), newInstance2.element()));

        TestXbrlInstances.XbrlInstance newInstance3 = TestXbrlInstances.XbrlInstance.from(
                Elements.transformChildElementsToNodeLists(
                        newInstance.element(),
                        e -> {
                            if (e.name().getNamespaceURI().equals(ns)) {
                                return List.of();
                            } else {
                                return List.of(e);
                            }
                        }
                )
        );

        assertTrue(nodeComparison.areEqual(instance.element(), newInstance3.element()));
    }
}
