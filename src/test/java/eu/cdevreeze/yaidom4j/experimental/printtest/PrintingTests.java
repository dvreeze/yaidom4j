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

package eu.cdevreeze.yaidom4j.experimental.printtest;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenClarkTrees;
import eu.cdevreeze.yaidom4j.experimental.jaxp.TransformerHandlers;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import eu.cdevreeze.yaidom4j.experimental.parse.ImmutableDomResult;
import eu.cdevreeze.yaidom4j.experimental.parse.JaxpDomToImmutableDomConverter;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinter;
import eu.cdevreeze.yaidom4j.experimental.print.DocumentPrinters;
import eu.cdevreeze.yaidom4j.experimental.print.ImmutableDomSource;
import eu.cdevreeze.yaidom4j.experimental.print.ImmutableDomToJaxpDomConverter;
import eu.cdevreeze.yaidom4j.experimental.testsupport.saxon.SaxonNodes;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Document printing test.
 * <p>
 * This is not a regular unit test, in that it assumes node querying etc. to work correctly.
 * <p>
 * This test depends on the parsing support as well. Hence, the separate package.
 *
 * @author Chris de Vreeze
 */
class PrintingTests {

    private static final String relativeXmlPath = "/sample-xbrl-instance.xml";

    private static final Processor saxonProcessor = new Processor();

    private static @Nullable Document document;

    @BeforeAll
    public static void parseDocument() throws SaxonApiException, IOException {
        URL url = Objects.requireNonNull(PrintingTests.class.getResource(relativeXmlPath));

        XdmNode docNode = saxonProcessor.newDocumentBuilder().build(
                new StreamSource(url.openStream())
        );
        document = Objects.requireNonNull(
                new Document(new SaxonNodes.Document(docNode).toGreenTreeNode())
                        .removeInterElementWhitespace()
        );
    }

    @Test
    void testSimplePrinting() {
        Objects.requireNonNull(document);

        DocumentPrinter docPrinter = DocumentPrinters.instance();
        String docAsString = docPrinter.print(document);

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(new ByteArrayInputStream(docAsString.getBytes())));

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    void testComplexPrinting() {
        Objects.requireNonNull(document);

        DocumentPrinter docPrinter = DocumentPrinters
                .builder(TransformerHandlers.newSaxTransformerFactory())
                .withTransformerHandlerInitializer(th ->
                        TransformerHandlers.Config.indenting(4).accept(th)
                )
                .build();
        String docAsString = docPrinter.print(document);

        Document doc = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(new ByteArrayInputStream(docAsString.getBytes())));

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    @Disabled("Fix: org.w3c.dom.DOMException: NAMESPACE_ERR: An attempt is made to create or change an object in a way which is incorrect with regard to namespaces")
    void testDomBasedPrinting() {
        Objects.requireNonNull(document);

        // The DOMException was thrown when processing the next line of code
        org.w3c.dom.Document w3cDoc = ImmutableDomToJaxpDomConverter.convertDocument(document, DocumentBuilderFactory.newDefaultNSInstance());

        Document doc = new Document(JaxpDomToImmutableDomConverter.convertDocument(w3cDoc));

        assertEquals(
                combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace(),
                combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
        );
    }

    @Test
    void testTransformerBasedPrinting() throws TransformerException {
        Objects.requireNonNull(document);

        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        ImmutableDomResult result = new ImmutableDomResult();
        tf.newTransformer().transform(new ImmutableDomSource(document), result);
        Document doc = result.getDocument();

        assertEquals(
                removeComments(
                        combineAdjacentTextNodes(Objects.requireNonNull(document).documentElement().greenTreeNode().toClarkNode()).removeInterElementWhitespace()
                ),
                removeComments(
                        combineAdjacentTextNodes(doc.documentElement().greenTreeNode().toClarkNode().removeInterElementWhitespace())
                )
        );
    }

    // Comparing with Saxon

    private static GreenClarkTrees.Element removeComments(GreenClarkTrees.Element element) {
        return element.transformDescendantOrSelfElements(e ->
                e.withChildren(e.children().stream().filter(n -> !(n instanceof GreenClarkTrees.Comment)).toList())
        );
    }

    private static GreenClarkTrees.Element combineAdjacentTextNodes(GreenClarkTrees.Element element) {
        return element.transformDescendantOrSelfElements(elem ->
                elem.withChildren(List.copyOf(combineAdjacentTextNodes(elem.children())))
        );
    }

    private static List<GreenClarkTrees.CanBeChild> combineAdjacentTextNodes(List<GreenClarkTrees.CanBeChild> nodes) {
        if (nodes.isEmpty()) {
            return nodes;
        }
        if (!(nodes.getFirst() instanceof GreenClarkTrees.Text)) {
            GreenClarkTrees.CanBeChild firstNode = nodes.getFirst();
            List<GreenClarkTrees.CanBeChild> remainder = nodes.subList(1, nodes.size());

            // Recursion
            List<GreenClarkTrees.CanBeChild> remainderWithCombinedTextNodes = combineAdjacentTextNodes(remainder);

            List<GreenClarkTrees.CanBeChild> result = new ArrayList<>();
            result.add(firstNode);
            result.addAll(remainderWithCombinedTextNodes);
            return List.copyOf(result);
        }

        List<GreenClarkTrees.CanBeChild> adjacentTextNodes = nodes.stream().takeWhile(n -> (n instanceof GreenClarkTrees.Text)).toList();
        List<GreenClarkTrees.CanBeChild> remainder = nodes.stream().dropWhile(n -> (n instanceof GreenClarkTrees.Text)).toList();
        List<GreenClarkTrees.CanBeChild> combinedTextNodes = adjacentTextNodes.isEmpty() ?
                List.of() :
                List.of(new GreenClarkTrees.Text(
                        adjacentTextNodes.stream()
                                .map(n -> ((GreenClarkTrees.Text) n).value())
                                .collect(Collectors.joining())
                ));

        // Recursion
        List<GreenClarkTrees.CanBeChild> remainderWithCombinedTextNodes = combineAdjacentTextNodes(remainder);

        List<GreenClarkTrees.CanBeChild> result = new ArrayList<>(combinedTextNodes);
        result.addAll(remainderWithCombinedTextNodes);
        return List.copyOf(result);
    }
}
