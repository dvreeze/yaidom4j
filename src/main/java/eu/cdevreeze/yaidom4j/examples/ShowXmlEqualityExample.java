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

package eu.cdevreeze.yaidom4j.examples;

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * This program tests equality of 2 XML documents (in terms of "Clark node" equality).
 * The program takes 2 program arguments, namely the 2 XML document URIs.
 *
 * @author Chris de Vreeze
 */
public class ShowXmlEqualityExample {

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(1, args.length);
        URI inputFile1 = new URI(args[0]);
        URI inputFile2 = new URI(args[1]);

        Document doc1 = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(inputFile1);
        Document doc2 = DocumentParsers.builder().removingInterElementWhitespace().build()
                .parse(inputFile2);

        System.out.printf("Parsed documents %s and %s%n", doc1.uriOption().orElseThrow(), doc2.uriOption().orElseThrow());

        boolean areEqual = doc1.documentElement().toClarkNode().equals(doc2.documentElement().toClarkNode());

        System.out.printf("Both documents are equal (as \"Clark nodes\"): %b%n", areEqual);
    }
}
