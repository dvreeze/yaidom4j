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

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * Web app data.
 *
 * @author Chris de Vreeze
 */
public record WebApp(
        ImmutableList<Filter> filters,
        ImmutableList<FilterMapping> filterMappings,
        ImmutableList<Servlet> servlets,
        ImmutableList<ServletMapping> servletMappings
) implements ConvertibleToXml {

    public static WebApp parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("web-app"));

        String ns = element.elementName().getNamespaceURI();

        // TODO
        return new WebApp(
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "filter")))
                        .map(Filter::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(ns, "filter-mapping")))
                        .map(FilterMapping::parse)
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
