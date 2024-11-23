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

package eu.cdevreeze.yaidom4j.examples.dialects.jakartaee;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.NodeBuilder;
import eu.cdevreeze.yaidom4j.examples.dialects.ConvertibleToXml;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Icon data.
 *
 * @author Chris de Vreeze
 */
public record Icon(
        Optional<String> idOption,
        Optional<String> languageOption,
        Optional<String> smallIconOption,
        Optional<String> largeIconOption
) implements ConvertibleToXml {

    public static Icon parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("icon"));

        String ns = element.elementName().getNamespaceURI();

        return new Icon(
                element.attributeOption(new QName("id")),
                element.attributeOption(new QName(XMLConstants.XML_NS_URI, "lang")),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "small-icon")))
                        .map(ElementApi::text)
                        .findFirst(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "large-icon")))
                        .map(ElementApi::text)
                        .findFirst()
        );
    }

    @Override
    public Element toXml(QName elementName) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(elementName.getNamespaceURI()));

        String ns = elementName.getNamespaceURI();
        String prefix = elementName.getPrefix();
        var nb = NodeBuilder.ConciseApi.empty().resolve(prefix, ns);

        return nb.element(
                nb.name(prefix, elementName.getLocalPart()),
                ImmutableMap.<String, String>builderWithExpectedSize(2)
                        .putAll(
                                idOption().stream()
                                        .map(id -> Map.entry("id", id))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                        )
                        .putAll(
                                languageOption().stream()
                                        .map(v -> Map.entry("xml:lang", v))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                        )
                        .build(),
                ImmutableList.<Node>builder()
                        .addAll(
                                smallIconOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "small-icon"), v))
                                        .toList()
                        )
                        .addAll(
                                largeIconOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "large-icon"), v))
                                        .toList()
                        )
                        .build()
        );
    }
}
