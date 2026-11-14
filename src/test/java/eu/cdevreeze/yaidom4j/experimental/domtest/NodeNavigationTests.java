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

import eu.cdevreeze.yaidom4j.experimental.dom.Document;
import eu.cdevreeze.yaidom4j.experimental.dom.Element;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.dom.Node;
import eu.cdevreeze.yaidom4j.experimental.parse.DocumentParsers;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Node navigation tests.
 * <p>
 * This is not a regular unit test, in that it assumes parsing etc. to work correctly.
 *
 * @author Chris de Vreeze
 */
class NodeNavigationTests {

    private static Document parseDocument() {
        InputStream inputStream = NodeNavigationTests.class.getResourceAsStream("/sample-xbrl-instance.xml");
        return DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(new InputSource(inputStream));
    }

    @Test
    void testRetrieveNodeNavigationPaths() {
        Document doc = parseDocument();
        List<List<Integer>> nodeNavigationPaths = doc.descendantOrSelfStream()
                .map(Node::nodeNavigationPath)
                .toList();

        assertTrue(nodeNavigationPaths.size() > 1000);
    }

    @Test
    void testNavigationFromDocument() {
        Document doc = parseDocument();
        List<Node> nodes = doc.descendantOrSelfStream().toList();
        List<List<Integer>> nodeNavigationPaths = nodes.stream()
                .map(Node::nodeNavigationPath)
                .toList();

        List<Node> foundNodes = nodeNavigationPaths.stream()
                .flatMap(p -> doc.navigate(p).stream())
                .toList();

        List<GreenXmlTrees.Node> greenTreeNodes = nodes.stream().map(Node::greenTreeNode).toList();
        List<GreenXmlTrees.Node> greenTreeFoundNodes = foundNodes.stream().map(Node::greenTreeNode).toList();

        assertEquals(greenTreeNodes.size(), greenTreeFoundNodes.size());
        assertEquals(greenTreeNodes, greenTreeFoundNodes);
        assertEquals(
                greenTreeNodes.stream().map(GreenXmlTrees.Node::toClarkNode).toList(),
                greenTreeFoundNodes.stream().map(GreenXmlTrees.Node::toClarkNode).toList()
        );
        // For regular ("ancestry-aware") elements we have reference equality
        // So we don't compare those nodes, but instead compare the underlying green tree nodes
    }

    @Test
    void testNavigationFromElement() {
        Element docElem = new Element(0, null, parseDocument().documentElement().greenTreeNode());
        List<Node> nodes = docElem.descendantOrSelfStream().toList();
        List<List<Integer>> nodeNavigationPaths = nodes.stream()
                .map(Node::nodeNavigationPath)
                .toList();

        List<Node> foundNodes = nodeNavigationPaths.stream()
                .flatMap(p -> docElem.navigate(p).stream())
                .toList();

        List<GreenXmlTrees.Node> greenTreeNodes = nodes.stream().map(Node::greenTreeNode).toList();
        List<GreenXmlTrees.Node> greenTreeFoundNodes = foundNodes.stream().map(Node::greenTreeNode).toList();

        assertEquals(greenTreeNodes.size(), greenTreeFoundNodes.size());
        assertEquals(greenTreeNodes, greenTreeFoundNodes);
        assertEquals(
                greenTreeNodes.stream().map(GreenXmlTrees.Node::toClarkNode).toList(),
                greenTreeFoundNodes.stream().map(GreenXmlTrees.Node::toClarkNode).toList()
        );
        // For regular ("ancestry-aware") elements we have reference equality
        // So we don't compare those nodes, but instead compare the underlying green tree nodes
    }
}
