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
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.*;
import eu.cdevreeze.yaidom4j.queryapi.AncestryAwareElementApi;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.Set;

/**
 * Web app data. Some data in the original XML is lost, in particular most "description groups".
 *
 * @author Chris de Vreeze
 */
public record WebApp(
        Optional<DescriptionGroup> firstDescriptionGroupOption,
        ImmutableList<Filter> filters,
        ImmutableList<FilterMapping> filterMappings,
        ImmutableList<Listener> listeners,
        ImmutableList<Servlet> servlets,
        ImmutableList<ServletMapping> servletMappings
) implements ConvertibleToXml {

    public static WebApp parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("web-app"));

        String ns = element.elementName().getNamespaceURI();

        ImmutableList<AncestryAwareElementApi<?>> descriptionGroupElements = element
                .childElementStream()
                .takeWhile(e ->
                        Set.of(new QName(ns, "description"), new QName(ns, "display-name"), new QName(ns, "icon"))
                                .contains(e.elementName())
                )
                .collect(ImmutableList.toImmutableList());

        Optional<DescriptionGroup> descriptionGroupOption =
                (descriptionGroupElements.isEmpty()) ?
                        Optional.empty() :
                        Optional.of(new DescriptionGroup(
                                descriptionGroupElements
                                        .stream()
                                        .filter(e -> e.elementName().equals(new QName(ns, "description")))
                                        .map(Description::parse)
                                        .collect(ImmutableList.toImmutableList()),
                                descriptionGroupElements
                                        .stream()
                                        .filter(e -> e.elementName().equals(new QName(ns, "display-name")))
                                        .map(ElementApi::text)
                                        .collect(ImmutableList.toImmutableList()),
                                descriptionGroupElements
                                        .stream()
                                        .filter(e -> e.elementName().equals(new QName(ns, "icon")))
                                        .map(Icon::parse)
                                        .collect(ImmutableList.toImmutableList())
                        ));

        // TODO
        return new WebApp(
                descriptionGroupOption,
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "filter")))
                        .map(Filter::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "filter-mapping")))
                        .map(FilterMapping::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "listener")))
                        .map(Listener::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "servlet")))
                        .map(Servlet::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "servlet-mapping")))
                        .map(ServletMapping::parse)
                        .collect(ImmutableList.toImmutableList())
        );
    }

    @Override
    public Element toXml(QName elementName) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(elementName.getNamespaceURI()));

        String ns = elementName.getNamespaceURI();
        String prefix = elementName.getPrefix();
        var nb = NodeBuilder.ConciseApi.empty().resolve(prefix, ns);

        // TODO
        return nb.element(
                nb.name(prefix, elementName.getLocalPart()),
                ImmutableMap.of(),
                ImmutableList.<Node>builder()
                        .addAll(
                                firstDescriptionGroupOption()
                                        .stream()
                                        .flatMap(dg ->
                                                dg.toXml(new QName(ns, "descriptionGroup", prefix)).childElementStream()
                                        )
                                        .toList()
                        )
                        .addAll(
                                filters()
                                        .stream()
                                        .map(v -> v.toXml(new QName(ns, "filter", prefix)))
                                        .toList()
                        )
                        .addAll(
                                filterMappings()
                                        .stream()
                                        .map(v -> v.toXml(new QName(ns, "filter-mapping", prefix)))
                                        .toList()
                        )
                        .addAll(
                                listeners()
                                        .stream()
                                        .map(v -> v.toXml(new QName(ns, "listener", prefix)))
                                        .toList()
                        )
                        .addAll(
                                servlets()
                                        .stream()
                                        .map(v -> v.toXml(new QName(ns, "servlet", prefix)))
                                        .toList()
                        )
                        .addAll(
                                servletMappings()
                                        .stream()
                                        .map(v -> v.toXml(new QName(ns, "servlet-mapping", prefix)))
                                        .toList()
                        )
                        .build()
        );
    }
}
