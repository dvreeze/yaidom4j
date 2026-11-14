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

package eu.cdevreeze.yaidom4j.experimental.parse;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;

/**
 * W3C DOM to "immutable DOM" converter. It is expected that W3C documents have been parsed/created
 * in a namespace-aware manner, or else an exception is likely to be thrown.
 * <p>
 * Example code:
 * {@snippet :
 * org.w3c.dom.Document w3cDoc = DocumentBuilders.parse(url.toURI());
 * Document doc = new Document(JaxpDomToImmutableDomConverter.convertDocument(w3cDoc));
 *}
 *
 * @author Chris de Vreeze
 */
public class JaxpDomToImmutableDomConverter {

    // TODO Review and improve implementation (are corner cases handled appropriately?)

    private JaxpDomToImmutableDomConverter() {
        // Non-instantiable
    }

    public static GreenXmlTrees.Document convertDocument(org.w3c.dom.Document doc) {
        PreconditionUtil.checkArgument(
                Optional.ofNullable(doc.getDocumentElement().getLocalName()).isPresent(),
                "Could not get the local name of the document element. Was the document created in a namespace-aware manner?"
        );

        // It seems that the DOM Document does not keep the URI from which it was loaded. Related (but not the same) is bug
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4916415.

        Optional<URI> uriOption = Optional.ofNullable(doc.getDocumentURI())
                .or(() -> Optional.ofNullable(doc.getBaseURI()))
                .map(JaxpDomToImmutableDomConverter::createUri);

        List<GreenXmlTrees.CanBeChild> children =
                convertNodeListToList(doc.getChildNodes())
                        .stream()
                        .flatMap(n -> switch (n) {
                            case org.w3c.dom.Element e -> Stream.of(convertElement(e, NamespaceScope.empty()));
                            case org.w3c.dom.Comment c -> Stream.of(convertComment(c, NamespaceScope.empty()));
                            case org.w3c.dom.ProcessingInstruction pi ->
                                    Stream.of(convertProcessingInstruction(pi, NamespaceScope.empty()));
                            default -> Stream.empty();
                        })
                        .collect(Collectors.toUnmodifiableList());

        return new GreenXmlTrees.Document(uriOption, children);
    }

    public static GreenXmlTrees.Element convertElement(org.w3c.dom.Element elem, NamespaceScope parentNamespaceScope) {
        PreconditionUtil.checkArgument(
                Optional.ofNullable(elem.getLocalName()).isPresent(),
                "Could not get the local name of the element. Was the element created in a namespace-aware manner?"
        );

        QName name = extractName(elem);

        Map<QName, String> attrs = extractAttributes(elem.getAttributes());

        NamespaceScope newScope =
                parentNamespaceScope.resolve(
                        NamespaceScope.withoutPrefixedNamespaceUndeclarations(extractNamespaceDeclarations(elem.getAttributes())));
        PreconditionUtil.checkArgument(parentNamespaceScope.withoutDefaultNamespace().subScopeOf(newScope.withoutDefaultNamespace()));

        List<GreenXmlTrees.CanBeChild> childNodes = convertNodeListToList(elem.getChildNodes())
                .stream()
                .flatMap(n -> convertNode(n, newScope).stream())
                .map(GreenXmlTrees.Node::asCanBeChild)
                .toList();

        return new GreenXmlTrees.Element(name, attrs, newScope, childNodes);
    }

    public static Optional<GreenXmlTrees.Node> convertNode(org.w3c.dom.Node node, NamespaceScope parentNamespaceScope) {
        return switch (node) {
            case org.w3c.dom.Element e -> Optional.of(convertElement(e, parentNamespaceScope));
            case org.w3c.dom.Text t -> Optional.of(convertText(t, parentNamespaceScope));
            case org.w3c.dom.Comment c -> Optional.of(convertComment(c, parentNamespaceScope));
            case org.w3c.dom.ProcessingInstruction pi ->
                    Optional.of(convertProcessingInstruction(pi, parentNamespaceScope));
            default -> Optional.empty();
        };
    }

    public static GreenXmlTrees.Text convertText(org.w3c.dom.Text text, NamespaceScope parentNamespaceScope) {
        return new GreenXmlTrees.Text(text.getData());
    }

    public static GreenXmlTrees.Comment convertComment(org.w3c.dom.Comment comment, NamespaceScope parentNamespaceScope) {
        return new GreenXmlTrees.Comment(comment.getData());
    }

    public static GreenXmlTrees.ProcessingInstruction convertProcessingInstruction(org.w3c.dom.ProcessingInstruction pi, NamespaceScope parentNamespaceScope) {
        return new GreenXmlTrees.ProcessingInstruction(pi.getTarget(), pi.getData());
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

    private static QName extractName(org.w3c.dom.Node node) {
        return new QName(
                Optional.ofNullable(node.getNamespaceURI()).orElse(XMLConstants.NULL_NS_URI),
                node.getLocalName(),
                Optional.ofNullable(node.getPrefix()).orElse(XMLConstants.DEFAULT_NS_PREFIX));
    }

    private static boolean isNamespaceDeclaration(Attr attr) {
        String name = attr.getName();
        if (name.equals(":")) {
            // Corner-case, where the local name is a colon, and the prefix is empty
            return false;
        }
        String[] parts = name.split(Pattern.quote(":"));
        PreconditionUtil.checkArgument(parts.length >= 1 && parts.length <= 2);
        return parts[0].equals(XMLConstants.XMLNS_ATTRIBUTE);
    }

    private static Map.Entry<String, String> extractNamespaceDeclaration(Attr attr) {
        String name = attr.getName();
        PreconditionUtil.checkArgument(!name.equals(":"));
        String[] parts = name.split(Pattern.quote(":"));
        PreconditionUtil.checkArgument(parts.length >= 1 && parts.length <= 2);
        PreconditionUtil.checkArgument(parts[0].equals(XMLConstants.XMLNS_ATTRIBUTE));
        String prefix = (parts.length == 2) ? parts[1] : XMLConstants.DEFAULT_NS_PREFIX;
        String ns = attr.getValue();
        return Map.entry(prefix, ns);
    }

    private static Map<QName, String> extractAttributes(NamedNodeMap nodeMap) {
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
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, String> extractNamespaceDeclarations(NamedNodeMap nodeMap) {
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
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
