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
import com.google.common.collect.ImmutableSet;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.*;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * W3C DOM to "immutable DOM" converter. It is expected that W3C documents have been parsed/created
 * in a namespace-aware manner, or else an exception is likely to be thrown.
 *
 * @author Chris de Vreeze
 */
public class JaxpDomToImmutableDomConverter {

    // TODO Review and improve implementation (are corner cases handled appropriately?)

    public static Document convertDocument(org.w3c.dom.Document doc) {
        // It seems that the DOM Document does not keep the URI from which it was loaded. Related (but not the same) is bug
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4916415.

        Optional<URI> uriOption = Optional.ofNullable(doc.getDocumentURI())
                .or(() -> Optional.ofNullable(doc.getBaseURI()))
                .map(JaxpDomToImmutableDomConverter::createUri);

        ImmutableList<CanBeDocumentChild> children =
                convertNodeListToList(doc.getChildNodes())
                        .stream()
                        .flatMap(n -> {
                            if (n instanceof org.w3c.dom.Element e) {
                                return Stream.of(convertElement(e, NamespaceScope.empty()));
                            } else if (n instanceof org.w3c.dom.Comment c) {
                                return Stream.of(convertComment(c, NamespaceScope.empty()));
                            } else if (n instanceof org.w3c.dom.ProcessingInstruction pi) {
                                return Stream.of(convertProcessingInstruction(pi, NamespaceScope.empty()));
                            } else {
                                return Stream.empty();
                            }
                        })
                        .collect(ImmutableList.toImmutableList());

        return new Document(uriOption, children);
    }

    public static Element convertElement(org.w3c.dom.Element elem, NamespaceScope namespaceScope) {
        QName name = extractName(elem);

        ImmutableMap<QName, String> attrs = extractAttributes(elem.getAttributes());

        NamespaceScope scopeTakingNsDeclsIntoAccount = resolve(namespaceScope, extractNamespaceDeclarations(elem.getAttributes()));
        Set<QName> qnames = ImmutableSet.<QName>builder().addAll(attrs.keySet()).add(name).build();
        NamespaceScope newScope = resolve(scopeTakingNsDeclsIntoAccount, extractNamespaceDeclarations(qnames));

        ImmutableList<Node> childNodes = convertNodeListToList(elem.getChildNodes())
                .stream()
                .flatMap(n -> convertNode(n, newScope).stream())
                .collect(ImmutableList.toImmutableList());

        return new Element(name, attrs, newScope, childNodes);
    }

    public static Optional<Node> convertNode(org.w3c.dom.Node node, NamespaceScope namespaceScope) {
        if (node instanceof org.w3c.dom.Element e) {
            return Optional.of(convertElement(e, namespaceScope));
        } else if (node instanceof org.w3c.dom.Text t) {
            return Optional.of(convertText(t, namespaceScope));
        } else if (node instanceof org.w3c.dom.Comment c) {
            return Optional.of(convertComment(c, namespaceScope));
        } else if (node instanceof org.w3c.dom.ProcessingInstruction pi) {
            return Optional.of(convertProcessingInstruction(pi, namespaceScope));
        } else {
            return Optional.empty();
        }
    }

    public static Text convertText(org.w3c.dom.Text text, NamespaceScope namespaceScope) {
        if (text instanceof org.w3c.dom.CDATASection cdata && text.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
            return new Text(text.getData(), true);
        } else {
            return new Text(text.getData(), false);
        }
    }

    public static Comment convertComment(org.w3c.dom.Comment comment, NamespaceScope namespaceScope) {
        return new Comment(comment.getData());
    }

    public static ProcessingInstruction convertProcessingInstruction(org.w3c.dom.ProcessingInstruction pi, NamespaceScope namespaceScope) {
        return new ProcessingInstruction(pi.getTarget(), pi.getData());
    }

    private static URI createUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<org.w3c.dom.Node> convertNodeListToList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).toList();
    }

    private static QName extractName(org.w3c.dom.Element elem) {
        return new QName(
                Optional.ofNullable(elem.getNamespaceURI()).orElse(XMLConstants.NULL_NS_URI),
                elem.getLocalName(),
                Optional.ofNullable(elem.getPrefix()).orElse(XMLConstants.DEFAULT_NS_PREFIX));
    }

    private static QName extractName(org.w3c.dom.Attr attr) {
        return new QName(
                Optional.ofNullable(attr.getNamespaceURI()).orElse(XMLConstants.NULL_NS_URI),
                attr.getLocalName(),
                Optional.ofNullable(attr.getPrefix()).orElse(XMLConstants.DEFAULT_NS_PREFIX));
    }

    private static boolean isNamespaceDeclaration(org.w3c.dom.Attr attr) {
        String name = attr.getName();
        String[] parts = name.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);
        return parts[0].equals(XMLConstants.XMLNS_ATTRIBUTE);
    }

    private static Map.Entry<String, String> extractNamespaceDeclaration(org.w3c.dom.Attr attr) {
        String name = attr.getName();
        String[] parts = name.split(Pattern.quote(":"));
        Preconditions.checkArgument(parts.length >= 1 && parts.length <= 2);
        Preconditions.checkArgument(parts[0].equals(XMLConstants.XMLNS_ATTRIBUTE));
        String prefix = (parts.length == 2) ? parts[1] : parts[0];
        String ns = attr.getValue();
        return Map.entry(prefix, ns);
    }

    private static ImmutableMap<QName, String> extractAttributes(NamedNodeMap nodeMap) {
        return IntStream.range(0, nodeMap.getLength())
                .boxed()
                .flatMap(i -> {
                    Attr attr = (Attr) nodeMap.item(i);

                    if (isNamespaceDeclaration(attr)) {
                        return Stream.empty();
                    } else {
                        return Stream.of(Map.entry(extractName(attr), attr.getValue()));
                    }
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static ImmutableMap<String, String> extractNamespaceDeclarations(NamedNodeMap nodeMap) {
        return IntStream.range(0, nodeMap.getLength())
                .boxed()
                .flatMap(i -> {
                    Attr attr = (Attr) nodeMap.item(i);

                    if (isNamespaceDeclaration(attr)) {
                        return Stream.of(extractNamespaceDeclaration(attr));
                    } else {
                        return Stream.empty();
                    }
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static ImmutableMap<String, String> extractNamespaceDeclarations(Set<QName> qnames) {
        return qnames.stream()
                .map(qn -> Map.entry(qn.getPrefix(), qn.getNamespaceURI()))
                .distinct()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static NamespaceScope resolve(NamespaceScope scope, ImmutableMap<String, String> decls) {
        NamespaceScope currScope = scope;

        if (decls.entrySet().stream().anyMatch(kv -> kv.getValue().isBlank() && kv.getKey().isBlank())) {
            // XML 1.0 only allows namespace un-declarations for the default namespace (we ignore the other ones, if any)
            currScope = currScope.withoutDefaultNamespace();
        }

        ImmutableMap<String, String> realDecls = decls.entrySet().stream()
                .filter(kv -> !kv.getValue().isBlank())
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        return currScope.resolve(realDecls);
    }
}
