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

package eu.cdevreeze.yaidom4j.dom.clark.jaxpinterop;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.clark.ClarkNodes;
import org.xml.sax.Attributes;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * SAX handler that produces an immutable Clark DOM document element. An instance should be used only once!
 * <p>
 * It is expected that W3C documents have been parsed/created in a namespace-aware manner, or else
 * an exception is likely to be thrown.
 * <p>
 * This class is an adapted (and simplified) copy of {@link eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.ImmutableDomProducingSaxHandler}.
 *
 * @author Chris de Vreeze
 */
public class ClarkDomProducingSaxHandler extends DefaultHandler implements LexicalHandler {

    private InternalNodes.InternalElement currentRootElement;

    private InternalNodes.InternalElement currentElement;

    private boolean currentlyInCData = false;

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes attrs) {
        QName name =
                new QName(
                        Objects.requireNonNull(uri),
                        Objects.requireNonNull(localName),
                        extractPrefix(Objects.requireNonNull(qname))
                );

        ImmutableMap<QName, String> attributes = extractAttributes(attrs);

        InternalNodes.InternalElement elem = new InternalNodes.InternalElement(
                Optional.ofNullable(currentElement),
                name,
                attributes
        );

        if (currentRootElement == null) {
            Preconditions.checkArgument(currentElement == null);

            currentRootElement = elem;
        } else {
            Preconditions.checkArgument(currentElement != null);

            currentElement.addChild(elem);
        }
        currentElement = elem;
    }

    @Override
    public void endElement(String uri, String localName, String qname) {
        Preconditions.checkArgument(currentRootElement != null);
        Preconditions.checkArgument(currentElement != null);

        currentElement = currentElement.getParentElementOption().orElse(null);
    }

    @Override
    public void characters(char[] chars, int start, int length) {
        boolean isCData = this.currentlyInCData;

        InternalNodes.InternalText text = new InternalNodes.InternalText(new String(chars, start, length), isCData);

        if (currentRootElement == null) {
            // Do nothing. This should not happen anyway.
            Preconditions.checkArgument(currentElement == null);
        } else {
            Preconditions.checkArgument(currentElement != null);

            currentElement.addChild(text);
        }
    }

    @Override
    public void processingInstruction(String target, String data) {
        InternalNodes.InternalProcessingInstruction pi = new InternalNodes.InternalProcessingInstruction(target, data);

        if (currentRootElement == null) {
            Preconditions.checkArgument(currentElement == null);
        } else {
            if (currentElement != null) {
                currentElement.addChild(pi);
            }
        }
    }

    @Override
    public void endDocument() {
    }

    @Override
    public void ignorableWhitespace(char[] chars, int start, int length) {
        // No-op
    }

    @Override
    public void startCDATA() {
        currentlyInCData = true;
    }

    @Override
    public void endCDATA() {
        currentlyInCData = false;
    }

    @Override
    public void comment(char[] chars, int start, int length) {
        var comment = new InternalNodes.InternalComment(new String(chars, start, length));

        if (currentRootElement == null) {
            Preconditions.checkArgument(currentElement == null);
        } else {
            if (currentElement != null) {
                currentElement.addChild(comment);
            }
        }
    }

    @Override
    public void startEntity(String name) {
    }

    @Override
    public void endEntity(String name) {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) {
    }

    @Override
    public void endDTD() {
    }

    public ClarkNodes.Element resultingOutermostElement() {
        return currentRootElement.toNode();
    }

    private String extractPrefix(String qname) {
        if (qname.equals(":")) {
            // Corner-case, where the local name is a colon, and the prefix is empty
            return "";
        }
        String[] parts = qname.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);
        return parts.length == 2 ? parts[0] : "";
    }

    private ImmutableMap<QName, String> extractAttributes(Attributes attrs) {
        return IntStream.range(0, attrs.getLength())
                .boxed()
                .flatMap(i -> {
                    String ns = Objects.requireNonNull(attrs.getURI(i));
                    String localName = Objects.requireNonNull(attrs.getLocalName(i));
                    String qname = Objects.requireNonNull(attrs.getQName(i));

                    if (isNamespaceDeclaration(qname)) {
                        return Stream.empty();
                    } else {
                        return Stream.of(Map.entry(new QName(ns, localName, extractPrefix(qname)), attrs.getValue(i)));
                    }
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isNamespaceDeclaration(String attrName) {
        if (attrName.equals(":")) {
            // Corner-case, where the local name is a colon, and the prefix is empty
            return false;
        }
        String[] parts = attrName.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);
        return parts[0].equals(XMLConstants.XMLNS_ATTRIBUTE);
    }
}
