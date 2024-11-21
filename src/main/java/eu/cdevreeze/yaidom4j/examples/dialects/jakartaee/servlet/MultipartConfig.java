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

package eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.servlet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Node;
import eu.cdevreeze.yaidom4j.dom.immutabledom.NodeBuilder;
import eu.cdevreeze.yaidom4j.examples.dialects.ConvertibleToXml;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Multipart-config data.
 *
 * @author Chris de Vreeze
 */
public record MultipartConfig(
        Optional<String> locationOption,
        OptionalLong maxFileSizeOption,
        OptionalLong maxRequestSizeOption,
        OptionalInt fileSizeThresholdOption
) implements ConvertibleToXml {

    public static MultipartConfig parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(element.elementName().equals(new QName(NS, "multipart-config")));

        return new MultipartConfig(
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "location")))
                        .findFirst()
                        .map(ElementApi::text),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "max-file-size")))
                        .map(ElementApi::text)
                        .mapToLong(Long::valueOf)
                        .findFirst(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "max-request-size")))
                        .map(ElementApi::text)
                        .mapToLong(Long::valueOf)
                        .findFirst(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "file-size-threshold")))
                        .map(ElementApi::text)
                        .mapToInt(Integer::valueOf)
                        .findFirst()
        );
    }

    @Override
    public Optional<Element> toXmlOption() {
        return Optional.of(toXml(new QName(NS, "multipart-config")));
    }

    @Override
    public Element toXml(QName elementName) {
        Preconditions.checkArgument(elementName.getNamespaceURI().equals(NS));

        String prefix = elementName.getPrefix();
        var nb = NodeBuilder.ConciseApi.empty().resolve(prefix, NS);

        return nb.element(
                nb.name(prefix, elementName.getLocalPart()),
                ImmutableMap.of(),
                ImmutableList.<Node>builder()
                        .addAll(
                                locationOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "location"), v))
                                        .toList()
                        )
                        .addAll(
                                maxFileSizeOption()
                                        .stream()
                                        .mapToObj(v -> nb.textElement(nb.name(prefix, "max-file-size"), String.valueOf(v)))
                                        .toList()
                        )
                        .build()
        );
    }

    private static final String NS = "https://jakarta.ee/xml/ns/jakartaee";
}
