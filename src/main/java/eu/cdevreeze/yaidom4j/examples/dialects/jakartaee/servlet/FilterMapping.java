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
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.Names;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Filter-mapping data.
 *
 * @author Chris de Vreeze
 */
public record FilterMapping(
        Optional<String> idOption,
        String filterName,
        ImmutableList<String> urlPatterns,
        ImmutableList<String> servletNames,
        ImmutableList<Dispatcher> dispatchers
) implements ConvertibleToXml {

    enum Dispatcher {
        FORWARD, INCLUDE, REQUEST, ASYNC, ERROR
    }

    public static FilterMapping parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("filter-mapping"));

        String ns = element.elementName().getNamespaceURI();

        return new FilterMapping(
                element.attributeOption(new QName("id")),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "filter-name")))
                        .findFirst()
                        .orElseThrow()
                        .text(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "url-pattern")))
                        .map(ElementApi::text)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "servlet-name")))
                        .map(ElementApi::text)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "dispatcher")))
                        .map(ElementApi::text)
                        .map(Dispatcher::valueOf)
                        .collect(ImmutableList.toImmutableList())
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
                ImmutableMap.copyOf(idOption().stream().map(id -> Map.entry("id", id)).toList()),
                ImmutableList.<Node>builder()
                        .add(nb.textElement(nb.name(prefix, "filter-name"), filterName()))
                        .addAll(
                                urlPatterns()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "url-pattern"), v))
                                        .toList()
                        )
                        .addAll(
                                servletNames()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "servlet-name"), v))
                                        .toList()
                        )
                        .addAll(
                                dispatchers()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "dispatcher"), v.toString()))
                                        .toList()
                        )
                        .build()
        );
    }
}
