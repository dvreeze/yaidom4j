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
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.ParamValue;
import eu.cdevreeze.yaidom4j.queryapi.ElementApi;

import javax.xml.namespace.QName;
import java.util.Optional;
import java.util.Set;

/**
 * Servlet XML element wrapper.
 *
 * @author Chris de Vreeze
 */
public final class Servlet {

    private final ElementTree.Element element;

    public Servlet(ElementTree.Element element) {
        Preconditions.checkArgument(
                Set.of(Names.JAKARTAEE_NS, Names.JAVAEE_NS).contains(element.elementName().getNamespaceURI()));
        Preconditions.checkArgument(element.elementName().getLocalPart().equals("servlet"));

        this.element = element;
    }

    public ElementTree.Element getElement() {
        return element;
    }

    public Optional<String> idOption() {
        return element.attributeOption(new QName("id"));
    }

    public String servletName() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(e -> e.elementName().equals(new QName(ns, "servlet-name")))
                .findFirst()
                .orElseThrow()
                .text();
    }

    public Optional<String> servletClassOption() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(e -> e.elementName().equals(new QName(ns, "servlet-class")))
                .findFirst()
                .map(ElementApi::text);
    }

    public ImmutableList<ParamValue> initParams() {
        String ns = element.elementName().getNamespaceURI();
        return element
                .childElementStream(e -> e.elementName().equals(new QName(ns, "init-param")))
                .map(ParamValue::new)
                .collect(ImmutableList.toImmutableList());
    }
}
