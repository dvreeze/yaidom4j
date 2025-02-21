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

package eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * SAX handler that produces an immutable DOM document. An instance should be used only once!
 * <p>
 * It is expected that W3C documents have been parsed/created in a namespace-aware manner, or else
 * an exception is likely to be thrown.
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomProducingSaxHandler extends DefaultHandler implements LexicalHandler {

    private Optional<URI> docUriOption = Optional.empty();

    private final List<InternalNodes.InternalCanBeDocumentChild> docChildren = new ArrayList<>();

    private InternalNodes.InternalElement currentRootElement;

    private InternalNodes.InternalElement currentElement;

    private boolean currentlyInCData = false;

    private final NamespaceSupport namespaceSupport = newNamespaceSupport();

    /**
     * When startPrefixMapping is called the first time for a subsequent startElement call,
     * "NamespaceSupport.pushContext()" should be called, before calling "NamespaceSupport.declarePrefix".
     * It is possible, though, that the subsequent startElement call has not been preceded by any
     * startPrefixMapping call. In the latter case, it is method startElement that should call
     * "NamespaceSupport.pushContext()". To distinguish between these 2 scenarios, this variable is used.
     * Note that only the first startPrefixMapping call for an element should push a new context, which
     * is also supported by this variable.
     * <p>
     * The invariant is that per startElement call, pushContext is called precisely once (whether
     * by this method or just before that), and per endElement call, the corresponding popContext
     * call is made.
     */
    private boolean namespaceContextHasAlreadyBeenPushed = false;

    @Override
    public void setDocumentLocator(Locator locator) {
        Optional.ofNullable(locator.getSystemId()).ifPresent(u -> {
            docUriOption = Optional.of(URI.create(u));
        });
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qname, Attributes attrs) {
        if (!namespaceContextHasAlreadyBeenPushed) {
            // Pushing an empty context, that will be popped by method endElement
            namespaceSupport.pushContext();
        }
        // Clear the namespaceContextHasAlreadyBeenPushed status in time, before a next startElement call
        namespaceContextHasAlreadyBeenPushed = false;

        NamespaceScope namespaceScope = convertToNamespaceScope(namespaceSupport);

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
                attributes,
                namespaceScope
        );

        if (currentRootElement == null) {
            Preconditions.checkArgument(currentElement == null);

            currentRootElement = elem;
            currentElement = elem;

            docChildren.add(elem);
        } else {
            Preconditions.checkArgument(currentElement != null);

            currentElement.addChild(elem);
            currentElement = elem;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qname) {
        Preconditions.checkArgument(currentRootElement != null);
        Preconditions.checkArgument(currentElement != null);

        // Per startElement call, pushContext() has been called exactly once, so call popContext() now
        namespaceSupport.popContext();
        Preconditions.checkArgument(!namespaceContextHasAlreadyBeenPushed);

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

            docChildren.add(pi);
        } else {
            if (currentElement == null) {
                docChildren.add(pi);
            } else {
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
    public void startPrefixMapping(String prefix, String uri) {
        if (!namespaceContextHasAlreadyBeenPushed) {
            namespaceSupport.pushContext();
            namespaceContextHasAlreadyBeenPushed = true;
        }
        namespaceSupport.declarePrefix(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) {
        Preconditions.checkArgument(!namespaceContextHasAlreadyBeenPushed);
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

            docChildren.add(comment);
        } else {
            if (currentElement == null) {
                docChildren.add(comment);
            } else {
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

    public Document resultingDocument() {
        return new Document(
                docUriOption,
                docChildren.stream()
                        .map(InternalNodes.InternalCanBeDocumentChild::toNode)
                        .collect(ImmutableList.toImmutableList())
        );
    }

    private NamespaceSupport newNamespaceSupport() {
        NamespaceSupport namespaceSupport = new NamespaceSupport();
        namespaceSupport.setNamespaceDeclUris(false);
        return namespaceSupport;
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

    private static NamespaceScope convertToNamespaceScope(NamespaceSupport namespaceSupport) {
        Preconditions.checkArgument(!namespaceSupport.isNamespaceDeclUris());

        List<String> prefixes = new ArrayList<>(Collections.list(namespaceSupport.getPrefixes()));
        Optional.ofNullable(namespaceSupport.getURI("")).ifPresent(prefixes::add);

        Map<String, String> nsBindingsWithoutDefaultNs =
                prefixes.stream()
                        .filter(pref -> namespaceSupport.getURI(pref) != null) // Yep, needed
                        .distinct()
                        .collect(Collectors.toMap(
                                pref -> pref,
                                namespaceSupport::getURI
                        ));
        Map<String, String> nsBindings = new HashMap<>(nsBindingsWithoutDefaultNs);
        Optional.ofNullable(namespaceSupport.getURI("")).ifPresent(ns -> nsBindings.put("", ns));

        return NamespaceScope.from(ImmutableMap.copyOf(nsBindings));
    }
}
