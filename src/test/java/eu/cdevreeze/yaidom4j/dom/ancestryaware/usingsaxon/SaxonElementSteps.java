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

package eu.cdevreeze.yaidom4j.dom.ancestryaware.usingsaxon;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.streams.Step;
import net.sf.saxon.s9api.streams.Steps;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Element-node-centric Saxon Step factory methods.
 * <p>
 * Maybe such a Step factory utility could be useful outside test code when using Saxon.
 *
 * @author Chris de Vreeze
 */
public class SaxonElementSteps {

    // Self element nodes

    public static Step<XdmNode> selfElements() {
        return Steps.self().where(isElementNode());
    }

    public static Step<XdmNode> selfElements(String uri, String localName) {
        return Steps.self(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> selfElements(String localName) {
        return Steps.self(localName).where(isElementNode());
    }

    public static Step<XdmNode> selfElements(Predicate<? super XdmNode> predicate) {
        return Steps.self(predicate).where(isElementNode());
    }

    // Child element nodes

    public static Step<XdmNode> childElements() {
        return Steps.child().where(isElementNode());
    }

    public static Step<XdmNode> childElements(String uri, String localName) {
        return Steps.child(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> childElements(String localName) {
        return Steps.child(localName).where(isElementNode());
    }

    public static Step<XdmNode> childElements(Predicate<? super XdmNode> predicate) {
        return Steps.child(predicate).where(isElementNode());
    }

    // Descendant-or-self element nodes

    public static Step<XdmNode> descendantOrSelfElements() {
        return Steps.descendantOrSelf().where(isElementNode());
    }

    public static Step<XdmNode> descendantOrSelfElements(String uri, String localName) {
        return Steps.descendantOrSelf(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> descendantOrSelfElements(String localName) {
        return Steps.descendantOrSelf(localName).where(isElementNode());
    }

    public static Step<XdmNode> descendantOrSelfElements(Predicate<? super XdmNode> predicate) {
        return Steps.descendantOrSelf(predicate).where(isElementNode());
    }

    // Descendant element nodes

    public static Step<XdmNode> descendantElements() {
        return Steps.descendant().where(isElementNode());
    }

    public static Step<XdmNode> descendantElements(String uri, String localName) {
        return Steps.descendant(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> descendantElements(String localName) {
        return Steps.descendant(localName).where(isElementNode());
    }

    public static Step<XdmNode> descendantElements(Predicate<? super XdmNode> predicate) {
        return Steps.descendant(predicate).where(isElementNode());
    }

    // Parent element nodes

    public static Step<XdmNode> parentElements() {
        return Steps.parent().where(isElementNode());
    }

    public static Step<XdmNode> parentElements(String uri, String localName) {
        return Steps.parent(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> parentElements(String localName) {
        return Steps.parent(localName).where(isElementNode());
    }

    public static Step<XdmNode> parentElements(Predicate<? super XdmNode> predicate) {
        return Steps.parent(predicate).where(isElementNode());
    }

    // Ancestor-or-self element nodes

    public static Step<XdmNode> ancestorOrSelfElements() {
        return Steps.ancestorOrSelf().where(isElementNode());
    }

    public static Step<XdmNode> ancestorOrSelfElements(String uri, String localName) {
        return Steps.ancestorOrSelf(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> ancestorOrSelfElements(String localName) {
        return Steps.ancestorOrSelf(localName).where(isElementNode());
    }

    public static Step<XdmNode> ancestorOrSelfElements(Predicate<? super XdmNode> predicate) {
        return Steps.ancestorOrSelf(predicate).where(isElementNode());
    }

    // Ancestor element nodes

    public static Step<XdmNode> ancestorElements() {
        return Steps.ancestor().where(isElementNode());
    }

    public static Step<XdmNode> ancestorElements(String uri, String localName) {
        return Steps.ancestor(uri, localName).where(isElementNode());
    }

    public static Step<XdmNode> ancestorElements(String localName) {
        return Steps.ancestor(localName).where(isElementNode());
    }

    public static Step<XdmNode> ancestorElements(Predicate<? super XdmNode> predicate) {
        return Steps.ancestor(predicate).where(isElementNode());
    }

    // Topmost descendant-or-self element nodes

    public static Step<XdmNode> topmostDescendantOrSelfElements(String uri, String localName) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                if (xdmItem.select(selfElements(uri, localName)).findAny().isPresent()) {
                    return xdmItem.select(selfElements(uri, localName));
                } else {
                    // Recursion
                    return xdmItem.select(
                            childElements().then(topmostDescendantOrSelfElements(uri, localName))
                    );
                }
            }
        };
    }

    public static Step<XdmNode> topmostDescendantOrSelfElements(String localName) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                if (xdmItem.select(selfElements(localName)).findAny().isPresent()) {
                    return xdmItem.select(selfElements(localName));
                } else {
                    // Recursion
                    return xdmItem.select(
                            childElements().then(topmostDescendantOrSelfElements(localName))
                    );
                }
            }
        };
    }

    public static Step<XdmNode> topmostDescendantOrSelfElements(Predicate<? super XdmNode> predicate) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                if (xdmItem.select(selfElements(predicate)).findAny().isPresent()) {
                    return xdmItem.select(selfElements(predicate));
                } else {
                    // Recursion
                    return xdmItem.select(
                            childElements().then(topmostDescendantOrSelfElements(predicate))
                    );
                }
            }
        };
    }

    // Topmost descendant element nodes

    public static Step<XdmNode> topmostDescendantElements(String uri, String localName) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                return xdmItem.select(
                        childElements().then(topmostDescendantOrSelfElements(uri, localName))
                );
            }
        };
    }

    public static Step<XdmNode> topmostDescendantElements(String localName) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                return xdmItem.select(
                        childElements().then(topmostDescendantOrSelfElements(localName))
                );
            }
        };
    }

    public static Step<XdmNode> topmostDescendantElements(Predicate<? super XdmNode> predicate) {
        return new Step<>() {

            @Override
            public Stream<XdmNode> apply(XdmItem xdmItem) {
                return xdmItem.select(
                        childElements().then(topmostDescendantOrSelfElements(predicate))
                );
            }
        };
    }

    // Private methods

    private static Predicate<? super XdmNode> isElementNode() {
        return n -> n.getNodeKind().equals(XdmNodeKind.ELEMENT);
    }
}
