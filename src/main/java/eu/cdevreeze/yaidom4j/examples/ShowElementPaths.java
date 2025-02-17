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

import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareDocument;
import eu.cdevreeze.yaidom4j.dom.ancestryaware.AncestryAwareNodes;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Program showing distinct element paths in a parsed document. It can be used to get a
 * quick overview of the structure of a possibly large document. This program is inspired
 * by Saxon's <a href="https://www.saxonica.com/documentation12/index.html#!gizmo/paths">Gizmo paths command</a>.
 *
 * @author Chris de Vreeze
 */
public class ShowElementPaths {

    public static void main(String[] args) throws URISyntaxException {
        Objects.checkIndex(0, args.length);
        URI inputFile = new URI(args[0]);

        AncestryAwareDocument doc = AncestryAwareDocument.from(
                DocumentParsers.instance().parse(inputFile)
        );

        List<List<QName>> distinctPaths = doc.documentElement()
                .descendantElementOrSelfStream()
                .map(ShowElementPaths::extractPath)
                .distinct()
                .toList();

        System.out.println();
        distinctPaths.forEach(p -> System.out.println(showPath(p)));
    }

    public static List<QName> extractPath(AncestryAwareNodes.Element element) {
        List<QName> ancestorOrSelfNames = element.ancestorElementOrSelfStream()
                .map(AncestryAwareNodes.Element::name)
                .toList();
        List<QName> mutableAncestorOrSelfNames = new ArrayList<>(ancestorOrSelfNames);
        Collections.reverse(mutableAncestorOrSelfNames);
        return List.copyOf(mutableAncestorOrSelfNames);
    }

    private static String showPath(List<QName> path) {
        return path.stream()
                .map(QName::toString)
                .map(n -> "/" + n)
                .collect(Collectors.joining());
    }
}
