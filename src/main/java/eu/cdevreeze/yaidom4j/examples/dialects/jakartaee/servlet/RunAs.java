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

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;

/**
 * Run-as data.
 *
 * @author Chris de Vreeze
 */
public record RunAs(
        Optional<String> idOption,
        ImmutableList<Description> descriptions,
        String roleName
) implements ConvertibleToXml {

    public static RunAs parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(element.elementName().equals(new QName(NS, "run-as")));

        return new RunAs(
                element.attributeOption(new QName("id")),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "description")))
                        .map(Description::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "role-name")))
                        .findFirst()
                        .orElseThrow()
                        .text()
        );
    }

    @Override
    public Optional<Element> toXmlOption() {
        return Optional.of(toXml(new QName(NS, "run-as")));
    }

    @Override
    public Element toXml(QName elementName) {
        Preconditions.checkArgument(elementName.getNamespaceURI().equals(NS));

        String prefix = elementName.getPrefix();
        var nb = NodeBuilder.ConciseApi.empty().resolve(prefix, NS);

        return nb.element(
                nb.name(prefix, elementName.getLocalPart()),
                ImmutableMap.copyOf(idOption().stream().map(id -> Map.entry("id", id)).toList()),
                ImmutableList.<Node>builder()
                        .addAll(
                                descriptions()
                                        .stream()
                                        .map(v -> v.toXml(new QName(NS, "description", prefix)))
                                        .toList()
                        )
                        .add(nb.textElement(nb.name(prefix, "role-name"), roleName()))
                        .build()
        );
    }

    private static final String NS = "https://jakarta.ee/xml/ns/jakartaee";
}
