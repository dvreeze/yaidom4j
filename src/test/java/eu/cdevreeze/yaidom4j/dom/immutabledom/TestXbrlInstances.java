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

package eu.cdevreeze.yaidom4j.dom.immutabledom;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.xml.namespace.QName;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static eu.cdevreeze.yaidom4j.dom.immutabledom.ElementPredicates.hasName;

/**
 * XBRL instance data model. Incorrect and incomplete, but good enough for the tests it is used in.
 *
 * @author Chris de Vreeze
 */
public class TestXbrlInstances {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private TestXbrlInstances() {
    }

    public interface XbrlInstancePart {

        Element element();
    }

    public static final class Identifier implements XbrlInstancePart {

        private final Element element;

        public Identifier(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "identifier")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public String scheme() {
            return element.attribute(new QName("scheme"));
        }

        public String identifierValue() {
            return element.text().strip();
        }
    }

    public static final class ExplicitMember implements XbrlInstancePart {

        private final Element element;

        public ExplicitMember(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLDI_NS, "explicitMember")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public QName dimension() {
            String dimensionAsString = element.attribute(new QName("dimension"));
            return element.namespaceScope().resolveSyntacticQNameInContent(dimensionAsString);
        }

        public QName member() {
            String memberAsString = element.text().strip();
            return element.namespaceScope().resolveSyntacticQNameInContent(memberAsString);
        }
    }

    public static final class Segment implements XbrlInstancePart {

        private final Element element;

        public Segment(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "segment")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public ImmutableList<ExplicitMember> explicitMembers() {
            return element.childElementStream(hasName(XBRLDI_NS, "explicitMember"))
                    .map(ExplicitMember::new)
                    .collect(ImmutableList.toImmutableList());
        }
    }

    public static final class Entity implements XbrlInstancePart {

        private final Element element;

        public Entity(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "entity")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public Identifier identifier() {
            return element.childElementStream(hasName(XBRLI_NS, "identifier"))
                    .map(Identifier::new)
                    .findFirst()
                    .orElseThrow();
        }

        public Optional<Segment> segmentOption() {
            return element.childElementStream(hasName(XBRLI_NS, "segment"))
                    .map(Segment::new)
                    .findFirst();
        }
    }

    public interface Period extends XbrlInstancePart {

        static Period fromElement(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));

            if (element.childElementStream(hasName(XBRLI_NS, "instant")).findAny().isPresent()) {
                return new InstantPeriod(element);
            } else {
                return new StartEndDatePeriod(element);
            }
        }
    }

    public static final class InstantPeriod implements Period {

        private final Element element;

        public InstantPeriod(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));
            Preconditions.checkArgument(element.childElementStream(hasName(XBRLI_NS, "instant")).findAny().isPresent());
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public LocalDate instant() {
            return LocalDate.parse(
                    element.childElementStream(hasName(XBRLI_NS, "instant"))
                            .findFirst()
                            .orElseThrow()
                            .text()
            );
        }
    }

    public static final class StartEndDatePeriod implements Period {

        private final Element element;

        public StartEndDatePeriod(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));
            Preconditions.checkArgument(element.childElementStream(hasName(XBRLI_NS, "startDate")).findAny().isPresent());
            Preconditions.checkArgument(element.childElementStream(hasName(XBRLI_NS, "endDate")).findAny().isPresent());
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public LocalDate startDate() {
            return LocalDate.parse(
                    element.childElementStream(hasName(XBRLI_NS, "startDate"))
                            .findFirst()
                            .orElseThrow()
                            .text()
            );
        }

        public LocalDate endDate() {
            return LocalDate.parse(
                    element.childElementStream(hasName(XBRLI_NS, "endDate"))
                            .findFirst()
                            .orElseThrow()
                            .text()
            );
        }
    }

    public static final class Context implements XbrlInstancePart {

        private final Element element;

        public Context(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "context")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public String id() {
            return element.attribute(new QName("id"));
        }

        public Entity entity() {
            return element.childElementStream(hasName(XBRLI_NS, "entity"))
                    .map(Entity::new)
                    .findAny()
                    .orElseThrow();
        }

        public Period period() {
            return element.childElementStream(hasName(XBRLI_NS, "period"))
                    .map(Period::fromElement)
                    .findAny()
                    .orElseThrow();
        }
    }

    public static final class Measure implements XbrlInstancePart {

        private final Element element;

        public Measure(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "measure")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public QName measure() {
            String measureAsString = element.text();
            return element.namespaceScope().resolveSyntacticQNameInContent(measureAsString);
        }
    }

    public static final class Unit implements XbrlInstancePart {

        private final Element element;

        public Unit(Element element) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "unit")));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public String id() {
            return element.attribute(new QName("id"));
        }

        public ImmutableList<Measure> measures() {
            return element.childElementStream(hasName(XBRLI_NS, "measure"))
                    .map(Measure::new)
                    .collect(ImmutableList.toImmutableList());
        }
    }

    public interface Fact extends XbrlInstancePart {
    }

    public static final class ItemFact implements Fact {

        private final Element element;

        public ItemFact(Element element) {
            Preconditions.checkArgument(!Set.of(XBRLI_NS, LINK_NS).contains(element.name().getNamespaceURI()));
            this.element = element;
        }

        @Override
        public Element element() {
            return element;
        }

        public QName factName() {
            return element.name();
        }

        public String contextRef() {
            return element.attribute(new QName("contextRef"));
        }

        public Optional<String> unitRefOption() {
            return element.attributeOption(new QName("unitRef"));
        }

        public Optional<String> decimalsOption() {
            return element.attributeOption(new QName("decimals"));
        }

        public String rawFactValue() {
            return element.text();
        }
    }

    public static final class XbrlInstance implements XbrlInstancePart {

        private final Element element;
        private final ImmutableMap<String, Context> contexts;
        private final ImmutableMap<String, Unit> units;

        private XbrlInstance(
                Element element,
                ImmutableMap<String, Context> contexts,
                ImmutableMap<String, Unit> units
        ) {
            Preconditions.checkArgument(element.name().equals(new QName(XBRLI_NS, "xbrl")));
            this.element = element;
            this.contexts = contexts;
            this.units = units;
        }

        @Override
        public Element element() {
            return element;
        }

        public ImmutableMap<String, Context> contexts() {
            return contexts;
        }

        public ImmutableMap<String, Unit> units() {
            return units;
        }

        public ImmutableList<ItemFact> topLevelItemFacts() {
            return element
                    .childElementStream(e -> !Set.of(XBRLI_NS, LINK_NS).contains(e.name().getNamespaceURI()))
                    .map(ItemFact::new)
                    .collect(ImmutableList.toImmutableList());
        }

        public ImmutableList<ItemFact> topLevelItemFacts(QName factName) {
            return topLevelItemFacts()
                    .stream()
                    .filter(f -> f.factName().equals(factName))
                    .collect(ImmutableList.toImmutableList());
        }

        public static XbrlInstance from(Element element) {
            ImmutableMap<String, Context> contexts =
                    element.childElementStream(hasName(XBRLI_NS, "context"))
                            .map(Context::new)
                            .collect(ImmutableMap.toImmutableMap(Context::id, Function.identity()));

            ImmutableMap<String, Unit> units =
                    element.childElementStream(hasName(XBRLI_NS, "unit"))
                            .map(Unit::new)
                            .collect(ImmutableMap.toImmutableMap(Unit::id, Function.identity()));

            return new XbrlInstance(element, contexts, units);
        }
    }
}
