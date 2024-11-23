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
import eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementTree;
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.Names;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.Set;

import static eu.cdevreeze.yaidom4j.dom.ancestryaware.ElementPredicates.hasName;

/**
 * Filter mapping XML element wrapper.
 *
 * @author Chris de Vreeze
 */
public final class FilterMapping {

    public enum Dispatcher {
        FORWARD, INCLUDE, REQUEST, ASYNC, ERROR
    }

    private final ElementTree.Element element;

    public FilterMapping(ElementTree.Element element) {
        Preconditions.checkArgument(
                Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("filter-mapping"));

        this.element = element;
    }

    public ElementTree.Element getElement() {
        return element;
    }

    public Optional<String> idOption() {
        return element.attributeOption(new QName("id"));
    }

    public String filterName() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(hasName(ns, "filter-name"))
                .findFirst()
                .orElseThrow()
                .text();
    }

    public ImmutableList<String> urlPatterns() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(hasName(ns, "url-pattern"))
                .map(ElementApi::text)
                .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<String> servletNames() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(hasName(ns, "servlet-name"))
                .map(ElementApi::text)
                .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<Dispatcher> dispatchers() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(hasName(ns, "dispatcher"))
                .map(ElementApi::text)
                .map(Dispatcher::valueOf)
                .collect(ImmutableList.toImmutableList());
    }
}
