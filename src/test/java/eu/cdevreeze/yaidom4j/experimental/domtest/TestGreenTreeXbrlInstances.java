/*
 * Copyright 2024-2026 Chris de Vreeze
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

package eu.cdevreeze.yaidom4j.experimental.domtest;

import eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.Element;
import eu.cdevreeze.yaidom4j.experimental.internal.PreconditionUtil;

import javax.xml.namespace.QName;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.cdevreeze.yaidom4j.experimental.dom.GreenXmlTrees.ElementPredicates.hasName;

/**
 * XBRL instance data model. Incorrect and incomplete, but good enough for the tests it is used in.
 *
 * @author Chris de Vreeze
 */
public class TestGreenTreeXbrlInstances {

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private TestGreenTreeXbrlInstances() {
    }

    public interface XbrlInstancePart {

        Element element();
    }

    public record Identifier(Element element) implements XbrlInstancePart {

        public Identifier {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "identifier")));
        }

        public String scheme() {
            return element.attribute(new QName("scheme"));
        }

        public String identifierValue() {
            return element.text().strip();
        }
    }

    public record ExplicitMember(Element element) implements XbrlInstancePart {

        public ExplicitMember {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLDI_NS, "explicitMember")));
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

    public record Segment(Element element) implements XbrlInstancePart {

        public Segment {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "segment")));
        }

        public List<ExplicitMember> explicitMembers() {
            return element.childElementStream(hasName(XBRLDI_NS, "explicitMember"))
                    .map(ExplicitMember::new)
                    .toList();
        }
    }

    public record Entity(Element element) implements XbrlInstancePart {

        public Entity {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "entity")));
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
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));

            if (element.childElementStream(hasName(XBRLI_NS, "instant")).findAny().isPresent()) {
                return new InstantPeriod(element);
            } else {
                return new StartEndDatePeriod(element);
            }
        }
    }

    public record InstantPeriod(Element element) implements Period {

        public InstantPeriod {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));
            PreconditionUtil.checkArgument(element.childElementStream(hasName(XBRLI_NS, "instant")).findAny().isPresent());
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

    public record StartEndDatePeriod(Element element) implements Period {

        public StartEndDatePeriod {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "period")));
            PreconditionUtil.checkArgument(element.childElementStream(hasName(XBRLI_NS, "startDate")).findAny().isPresent());
            PreconditionUtil.checkArgument(element.childElementStream(hasName(XBRLI_NS, "endDate")).findAny().isPresent());
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

    public record Context(Element element) implements XbrlInstancePart {

        public Context {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "context")));
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

    public record Measure(Element element) implements XbrlInstancePart {

        public Measure {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "measure")));
        }

        public QName measure() {
            String measureAsString = element.text();
            return element.namespaceScope().resolveSyntacticQNameInContent(measureAsString);
        }
    }

    public record Unit(Element element) implements XbrlInstancePart {

        public Unit {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "unit")));
        }

        public String id() {
            return element.attribute(new QName("id"));
        }

        public List<Measure> measures() {
            return element.childElementStream(hasName(XBRLI_NS, "measure"))
                    .map(Measure::new)
                    .toList();
        }
    }

    public interface Fact extends XbrlInstancePart {
    }

    public record ItemFact(Element element) implements Fact {

        public ItemFact {
            PreconditionUtil.checkArgument(!Set.of(XBRLI_NS, LINK_NS).contains(element.name().getNamespaceURI()));
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
        private final Map<String, Context> contexts;
        private final Map<String, Unit> units;

        private XbrlInstance(
                Element element,
                Map<String, Context> contexts,
                Map<String, Unit> units
        ) {
            PreconditionUtil.checkArgument(element.name().equals(new QName(XBRLI_NS, "xbrl")));
            this.element = element;
            this.contexts = Map.copyOf(contexts);
            this.units = Map.copyOf(units);
        }

        @Override
        public Element element() {
            return element;
        }

        public Map<String, Context> contexts() {
            return contexts;
        }

        public Map<String, Unit> units() {
            return units;
        }

        public List<ItemFact> topLevelItemFacts() {
            return element
                    .childElementStream(e -> !Set.of(XBRLI_NS, LINK_NS).contains(e.name().getNamespaceURI()))
                    .map(ItemFact::new)
                    .toList();
        }

        public List<ItemFact> topLevelItemFacts(QName factName) {
            return topLevelItemFacts()
                    .stream()
                    .filter(f -> f.factName().equals(factName))
                    .toList();
        }

        public static XbrlInstance from(Element element) {
            Map<String, Context> contexts =
                    element.childElementStream(hasName(XBRLI_NS, "context"))
                            .map(Context::new)
                            .collect(Collectors.toUnmodifiableMap(Context::id, Function.identity()));

            Map<String, Unit> units =
                    element.childElementStream(hasName(XBRLI_NS, "unit"))
                            .map(Unit::new)
                            .collect(Collectors.toUnmodifiableMap(Unit::id, Function.identity()));

            return new XbrlInstance(element, contexts, units);
        }
    }
}
