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

import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.core.NamespaceScope;
import eu.cdevreeze.yaidom4j.dom.immutabledom.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;

/**
 * "Immutable DOM" to W3C DOM converter.
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomToJaxpDomConverter {

    // TODO Review and improve implementation (are corner cases handled appropriately?)

    private ImmutableDomToJaxpDomConverter() {
    }

    public static org.w3c.dom.Document convertDocument(Document document, DocumentBuilderFactory dbf) {
        try {
            return convertDocument(document, dbf.newDocumentBuilder().newDocument());
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static org.w3c.dom.Element convertElement(Element element, org.w3c.dom.Document w3cDomDocument, NamespaceScope parentScope) {
        org.w3c.dom.Element w3cDomElement = w3cDomDocument.createElementNS(
                convertEmptyStringToNull(element.name().getNamespaceURI()),
                getSyntacticQName(element.name())
        );

        ImmutableMap<String, String> namespaceDeclarations = parentScope.relativize(element.namespaceScope());

        for (var prefixNamespace : namespaceDeclarations.entrySet()) {
            String prefix = prefixNamespace.getKey();
            String namespace = prefixNamespace.getValue();

            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                w3cDomElement.setAttributeNS(null, XMLConstants.XMLNS_ATTRIBUTE, namespace);
            } else {
                w3cDomElement.setAttributeNS(
                        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                        String.format("%s:%s", XMLConstants.XMLNS_ATTRIBUTE, prefix),
                        namespace
                );
            }
        }

        for (var attributeNameValue : element.attributes().entrySet()) {
            QName attributeName = attributeNameValue.getKey();
            String attributeValue = attributeNameValue.getValue();

            w3cDomElement.setAttributeNS(
                    convertEmptyStringToNull(attributeName.getNamespaceURI()),
                    getSyntacticQName(attributeName),
                    attributeValue
            );
        }

        List<org.w3c.dom.Node> w3cDomChildNodes = element
                .childNodeStream()
                .map(ch -> convertNode(ch, w3cDomDocument, element.namespaceScope()))
                .toList();

        for (var childNode : w3cDomChildNodes) {
            w3cDomElement.appendChild(childNode);
        }

        return w3cDomElement;
    }

    public static org.w3c.dom.Node convertNode(Node node, org.w3c.dom.Document w3cDomDocument, NamespaceScope parentScope) {
        if (node instanceof Element elem) {
            return convertElement(elem, w3cDomDocument, parentScope);
        } else if (node instanceof Text text) {
            return convertText(text, w3cDomDocument);
        } else if (node instanceof Comment comment) {
            return convertComment(comment, w3cDomDocument);
        } else if (node instanceof ProcessingInstruction pi) {
            return convertProcessingInstruction(pi, w3cDomDocument);
        } else {
            throw new RuntimeException("Unknown node type (not possible)");
        }
    }

    public static org.w3c.dom.Text convertText(Text text, org.w3c.dom.Document w3cDomDocument) {
        if (text.isCData()) {
            return w3cDomDocument.createCDATASection(text.value());
        } else {
            return w3cDomDocument.createTextNode(text.value());
        }
    }

    public static org.w3c.dom.Comment convertComment(Comment comment, org.w3c.dom.Document w3cDomDocument) {
        return w3cDomDocument.createComment(comment.value());
    }

    public static org.w3c.dom.ProcessingInstruction convertProcessingInstruction(ProcessingInstruction pi, org.w3c.dom.Document w3cDomDocument) {
        return w3cDomDocument.createProcessingInstruction(pi.target(), pi.data());
    }

    private static org.w3c.dom.Document convertDocument(Document document, org.w3c.dom.Document w3cDomDocument) {
        List<org.w3c.dom.Node> w3cDomDocumentChildren = document.children()
                .stream()
                .map(n -> convertNode(n, w3cDomDocument, NamespaceScope.empty()))
                .toList();

        for (var w3cDomNode : w3cDomDocumentChildren) {
            w3cDomDocument.appendChild(w3cDomNode);
        }

        document.uriOption().ifPresent(uri ->
                w3cDomDocument.setDocumentURI(uri.toString())
        );

        return w3cDomDocument;
    }

    private static String getSyntacticQName(QName name) {
        return name.getPrefix().isEmpty() ?
                name.getLocalPart() :
                String.format("%s:%s", name.getPrefix(), name.getLocalPart());
    }

    private static String convertEmptyStringToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
