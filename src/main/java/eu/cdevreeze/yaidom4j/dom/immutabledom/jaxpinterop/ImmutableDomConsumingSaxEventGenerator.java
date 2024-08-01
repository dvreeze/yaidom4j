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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.namespace.QName;

/**
 * Producer of SAX events, taking an immutable DOM as input.
 * <p>
 * This class can be used to serialize an immutable DOM document.
 *
 * @author Chris de Vreeze
 */
public class ImmutableDomConsumingSaxEventGenerator {

    private final ContentHandler contentHandler;

    public ImmutableDomConsumingSaxEventGenerator(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public void processDocument(Document document) {
        try {
            contentHandler.startDocument();

            document.children().forEach(docChild -> processNode(docChild, NamespaceScope.empty()));

            contentHandler.endDocument();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void processElement(Element element, NamespaceScope parentScope) {
        try {
            ImmutableMap<String, String> nsDecls =
                    NamespaceScope.withoutPrefixedNamespaceUndeclarations(parentScope.relativize(element.namespaceScope()));

            for (var kv : nsDecls.entrySet()) {
                contentHandler.startPrefixMapping(kv.getKey(), kv.getValue());
            }

            contentHandler.startElement(
                    element.name().getNamespaceURI(),
                    element.name().getLocalPart(),
                    getSyntacticQName(element.name()),
                    getAttributes(element)
            );

            for (Node child : element.children()) {
                // Recursion
                processNode(child, element.namespaceScope());
            }

            contentHandler.endElement(
                    element.name().getNamespaceURI(),
                    element.name().getLocalPart(),
                    getSyntacticQName(element.name())
            );

            for (var kv : nsDecls.entrySet()) {
                contentHandler.endPrefixMapping(kv.getKey());
            }
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void processNode(Node node, NamespaceScope parentScope) {
        if (node instanceof Element e) {
            processElement(e, parentScope);
        } else if (node instanceof Text t) {
            processText(t, parentScope);
        } else if (node instanceof Comment c) {
            processComment(c, parentScope);
        } else if (node instanceof ProcessingInstruction pi) {
            processProcessingInstruction(pi, parentScope);
        }
    }

    public void processText(Text text, NamespaceScope parentScope) {
        try {
            if (contentHandler instanceof LexicalHandler lh) {
                if (text.isCData()) {
                    lh.startCDATA();
                }
                contentHandler.characters(text.value().toCharArray(), 0, text.value().length());
                if (text.isCData()) {
                    lh.endCDATA();
                }
            } else {
                contentHandler.characters(text.value().toCharArray(), 0, text.value().length());
            }
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void processComment(Comment comment, NamespaceScope parentScope) {
        try {
            if (contentHandler instanceof LexicalHandler lh) {
                lh.comment(comment.value().toCharArray(), 0, comment.value().length());
            }
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void processProcessingInstruction(ProcessingInstruction pi, NamespaceScope parentScope) {
        try {
            contentHandler.processingInstruction(pi.target(), pi.data());
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSyntacticQName(QName name) {
        if (name.getPrefix().isEmpty()) {
            return name.getLocalPart();
        } else {
            return String.format("%s:%s", name.getPrefix(), name.getLocalPart());
        }
    }

    private Attributes getAttributes(Element element) {
        AttributesImpl attrs = new AttributesImpl();

        for (var kv : element.attributes().entrySet()) {
            QName name = kv.getKey();
            String value = kv.getValue();

            attrs.addAttribute(
                    name.getNamespaceURI(),
                    name.getLocalPart(),
                    getSyntacticQName(name),
                    "CDATA",
                    value
            );
        }

        return attrs;
    }
}
