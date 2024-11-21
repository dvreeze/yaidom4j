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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Servlet data.
 *
 * @author Chris de Vreeze
 */
public record Servlet(
        Optional<String> idOption,
        String name,
        Optional<String> servletClassOption,
        Optional<String> jspFileTypeOption,
        ImmutableList<ParamValue> initParams,
        Optional<String> loadOnStartupOption,
        Optional<Boolean> enabledOption,
        Optional<Boolean> asyncSupportedOption,
        Optional<RunAs> runAsOption,
        ImmutableList<SecurityRoleRef> securityRoleRefs,
        Optional<MultipartConfig> multipartConfigOption
) implements ConvertibleToXml {
    public Servlet {
        Preconditions.checkArgument(servletClassOption.isEmpty() || jspFileTypeOption.isEmpty());
    }

    public static Servlet parse(AncestryAwareElementApi<?> element) {
        Preconditions.checkArgument(element.elementName().equals(new QName(NS, "servlet")));

        return new Servlet(
                element.attributeOption(new QName("id")),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "servlet-name")))
                        .findFirst()
                        .orElseThrow()
                        .text(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "servlet-class")))
                        .findFirst()
                        .map(ElementApi::text),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "jsp-file")))
                        .findFirst()
                        .map(ElementApi::text),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "init-param")))
                        .map(ParamValue::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "load-on-startup")))
                        .findFirst()
                        .map(ElementApi::text),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "enabled")))
                        .findFirst()
                        .map(e -> Boolean.parseBoolean(e.text())),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "async-supported")))
                        .findFirst()
                        .map(e -> Boolean.parseBoolean(e.text())),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "run-as")))
                        .map(e -> Objects.requireNonNull(RunAs.parse(e)))
                        .findFirst(),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "security-role-ref")))
                        .map(SecurityRoleRef::parse)
                        .collect(ImmutableList.toImmutableList()),
                element
                        .childElementStream(e -> e.elementName().equals(new QName(NS, "multipart-config")))
                        .map(e -> Objects.requireNonNull(MultipartConfig.parse(e)))
                        .findFirst()
        );
    }

    @Override
    public Optional<Element> toXmlOption() {
        return Optional.of(toXml(new QName(NS, "servlet")));
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
                        .add(nb.textElement(nb.name(prefix, "servlet-name"), name()))
                        .addAll(
                                servletClassOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "servlet-class"), v))
                                        .toList()
                        )
                        .addAll(
                                jspFileTypeOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "jsp-file"), v))
                                        .toList()
                        )
                        .addAll(
                                initParams()
                                        .stream()
                                        .map(v -> v.toXml(new QName(NS, "init-param", prefix)))
                                        .toList()
                        )
                        .addAll(
                                loadOnStartupOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "load-on-startup"), v))
                                        .toList()
                        )
                        .addAll(
                                enabledOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "enabled"), v.toString()))
                                        .toList()
                        )
                        .addAll(
                                asyncSupportedOption()
                                        .stream()
                                        .map(v -> nb.textElement(nb.name(prefix, "async-supported"), v.toString()))
                                        .toList()
                        )
                        .addAll(
                                runAsOption()
                                        .stream()
                                        .map(v -> v.toXml(new QName(NS, "run-as", prefix)))
                                        .toList()
                        )
                        .addAll(
                                securityRoleRefs()
                                        .stream()
                                        .map(v -> v.toXml(new QName(NS, "security-role-ref", prefix)))
                                        .toList()
                        )
                        .addAll(
                                multipartConfigOption()
                                        .stream()
                                        .map(v -> v.toXml(new QName(NS, "multipart-config", prefix)))
                                        .toList()
                        )
                        .build()
        );
    }

    private static final String NS = "https://jakarta.ee/xml/ns/jakartaee";
}
