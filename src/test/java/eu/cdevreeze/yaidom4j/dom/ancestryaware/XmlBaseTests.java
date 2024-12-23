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

package eu.cdevreeze.yaidom4j.dom.ancestryaware;

import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareElementPredicates.hasName;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * XML Base tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class XmlBaseTests {

    private AncestryAwareDocument parseDocument(String nameOnClasspath) {
        InputStream inputStream = BookQueryTests.class.getResourceAsStream(nameOnClasspath);
        var underlyingDoc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream));
        return AncestryAwareDocument.from(underlyingDoc);
    }

    @Test
    void testXmlBase() {
        AncestryAwareDocument doc = parseDocument("/xml-base-example.xml");

        assertEquals(Optional.empty(), doc.uriOption());

        assertEquals(Optional.of(URI.create("http://example.org/today/")), doc.documentElement().baseUriOption());

        AncestryAwareNodes.ElementTree.Element firstParagraph =
                doc.documentElement()
                        .elementStream(hasName("paragraph"))
                        .findFirst()
                        .orElseThrow();

        AncestryAwareNodes.ElementTree.Element firstSimpleLink =
                firstParagraph
                        .childElementStream(hasName("link"))
                        .findFirst()
                        .orElseThrow();

        String xlinkNamespace = "http://www.w3.org/1999/xlink";

        assertEquals(doc.documentElement().baseUriOption(), firstSimpleLink.baseUriOption());

        assertEquals(
                Optional.of(URI.create("http://example.org/today/new.xml")),
                firstSimpleLink
                        .baseUriOption()
                        .map(b -> b.resolve(firstSimpleLink.attribute(new QName(xlinkNamespace, "href"))))
        );

        AncestryAwareNodes.ElementTree.Element olist =
                doc.documentElement()
                        .elementStream(hasName("olist"))
                        .findFirst()
                        .orElseThrow();

        assertEquals(Optional.of(URI.create("http://example.org/hotpicks/")), olist.baseUriOption());

        List<AncestryAwareNodes.ElementTree.Element> remainingSimpleLinks =
                doc.documentElement()
                        .elementStream(hasName("link"))
                        .toList()
                        .subList(1, 4);

        assertEquals(
                List.of(
                        URI.create("http://example.org/hotpicks/pick1.xml"),
                        URI.create("http://example.org/hotpicks/pick2.xml"),
                        URI.create("http://example.org/hotpicks/pick3.xml")
                ),
                remainingSimpleLinks.stream()
                        .map(e -> e.baseUriOption().orElseThrow().resolve(e.attribute(new QName(xlinkNamespace, "href"))))
                        .toList()
        );
    }

    @Test
    void testXmlBaseWithoutEscaping() {
        AncestryAwareDocument doc = parseDocument("/rose.xml");

        assertEquals(Optional.empty(), doc.uriOption());

        assertEquals(URI.create("http://example.org/wine/"), doc.documentElement().baseUriOption().orElseThrow());

        AncestryAwareNodes.ElementTree.Element e2 =
                doc.documentElement()
                        .childElementStream(hasName("e2"))
                        .findFirst()
                        .orElseThrow();

        assertEquals(URI.create("http://example.org/wine/rosé"), e2.baseUriOption().orElseThrow());
    }
}
