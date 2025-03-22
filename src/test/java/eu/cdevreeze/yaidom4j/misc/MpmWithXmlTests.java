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

package eu.cdevreeze.yaidom4j.misc;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParser;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test checks parsing of non-nested and nested multipart MIME data containing XML content.
 * <p>
 * This is not a unit test.
 *
 * @author Chris de Vreeze
 */
class MpmWithXmlTests {

    @Test
    void testParsingOfNonNestedMultipartMimeWithXml() throws URISyntaxException, IOException, MimeException {
        URI mpmUri = Objects.requireNonNull(MpmWithXmlTests.class.getResource("/mpm/sample-mpm.txt")).toURI();

        ContentHandlerCollectingXml handler = new ContentHandlerCollectingXml();
        MimeStreamParser mimeParser = new MimeStreamParser();
        mimeParser.setContentHandler(handler);
        mimeParser.parse(mpmUri.toURL().openStream());
        ImmutableMap<ImmutableList<Integer>, Element> rootElements = handler.xmlCollectionByBodyPathInMpm();

        assertEquals(3, rootElements.size());

        Element firstElement = parseElementOnClassPath("/sample-soap-message.xml");
        Element secondElement = parseElementOnClassPath("/rose.xml");
        Element thirdElement = parseElementOnClassPath("/valid-books.xml");

        assertEquals(
                firstElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(1))).toClarkNode()
        );

        assertEquals(
                secondElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(3))).toClarkNode()
        );

        assertEquals(
                thirdElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(4))).toClarkNode()
        );

        assertEquals(
                ImmutableMap.of(
                        ImmutableList.of(1), firstElement.toClarkNode(),
                        ImmutableList.of(3), secondElement.toClarkNode(),
                        ImmutableList.of(4), thirdElement.toClarkNode()
                ),
                rootElements.entrySet()
                        .stream()
                        .map(kv -> Map.entry(kv.getKey(), kv.getValue().toClarkNode()))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    @Test
    void testParsingOfNestedMultipartMimeWithXml() throws URISyntaxException, IOException, MimeException {
        URI mpmUri = Objects.requireNonNull(MpmWithXmlTests.class.getResource("/mpm/sample-nested-mpm.txt")).toURI();

        ContentHandlerCollectingXml handler = new ContentHandlerCollectingXml();
        MimeStreamParser mimeParser = new MimeStreamParser();
        mimeParser.setContentHandler(handler);
        mimeParser.parse(mpmUri.toURL().openStream());
        ImmutableMap<ImmutableList<Integer>, Element> rootElements = handler.xmlCollectionByBodyPathInMpm();

        assertEquals(3, rootElements.size());

        Element firstElement = parseElementOnClassPath("/sample-soap-message.xml");
        Element secondElement = parseElementOnClassPath("/rose.xml");
        Element thirdElement = parseElementOnClassPath("/valid-books.xml");

        assertEquals(
                firstElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(1))).toClarkNode()
        );

        assertEquals(
                secondElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(2, 1))).toClarkNode()
        );

        assertEquals(
                thirdElement.toClarkNode(),
                Objects.requireNonNull(rootElements.get(ImmutableList.of(3))).toClarkNode()
        );

        assertEquals(
                ImmutableMap.of(
                        ImmutableList.of(1), firstElement.toClarkNode(),
                        ImmutableList.of(2, 1), secondElement.toClarkNode(),
                        ImmutableList.of(3), thirdElement.toClarkNode()
                ),
                rootElements.entrySet()
                        .stream()
                        .map(kv -> Map.entry(kv.getKey(), kv.getValue().toClarkNode()))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private static Element parseElementOnClassPath(String classPathResource) {
        try {
            DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();
            return docParser.parse(
                            Objects.requireNonNull(MpmWithXmlTests.class.getResource(classPathResource)).toURI()
                    )
                    .documentElement();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class ContentHandlerCollectingXml extends AbstractContentHandler {

        private final DocumentParser docParser = DocumentParsers.builder().removingInterElementWhitespace().build();

        private final Map<ImmutableList<Integer>, Element> xmlCollectionByBodyPathInMpm = new HashMap<>();

        private final List<Integer> bodyPathInMpm = new ArrayList<>();

        @Override
        public void startMultipart(BodyDescriptor bd) {
            bodyPathInMpm.add(-1);
        }

        @Override
        public void endMultipart() {
            Preconditions.checkArgument(!bodyPathInMpm.isEmpty());
            bodyPathInMpm.remove(bodyPathInMpm.size() - 1);
        }

        @Override
        public void startBodyPart() {
            Preconditions.checkArgument(!bodyPathInMpm.isEmpty());
            int previousPartIdx = bodyPathInMpm.get(bodyPathInMpm.size() - 1);
            bodyPathInMpm.set(bodyPathInMpm.size() - 1, 1 + previousPartIdx);
        }

        @Override
        public void body(BodyDescriptor bd, InputStream is) {
            if (bd.getMimeType().equals("text/xml")) {
                Element element = docParser.parse(new InputSource(is)).documentElement();
                xmlCollectionByBodyPathInMpm.put(ImmutableList.copyOf(bodyPathInMpm), element);
            }
        }

        public ImmutableMap<ImmutableList<Integer>, Element> xmlCollectionByBodyPathInMpm() {
            return xmlCollectionByBodyPathInMpm.entrySet()
                    .stream()
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
