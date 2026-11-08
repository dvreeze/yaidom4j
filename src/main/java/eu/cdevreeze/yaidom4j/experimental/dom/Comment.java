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

package eu.cdevreeze.yaidom4j.experimental.dom;

import module java.base;
import org.jspecify.annotations.Nullable;

/**
 * Immutable comment node.
 * <p>
 * Not a record class, and using reference equality rather than (in this case) very expensive value equality.
 *
 * @author Chris de Vreeze
 */
public final class Comment implements CanBeChild {

    private final int offset;
    private final @Nullable Node parentOrNull;
    private final GreenXmlTrees.Comment greenTreeNode;

    public Comment(int offset, @Nullable Node parentOrNull, GreenXmlTrees.Comment greenTreeNode) {
        this.offset = offset;
        this.parentOrNull = parentOrNull;
        this.greenTreeNode = greenTreeNode;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public Optional<Node> parentOption() {
        return Optional.ofNullable(parentOrNull);
    }

    @Override
    public GreenXmlTrees.Comment greenTreeNode() {
        return greenTreeNode;
    }

    @Override
    public Stream<Node> childStream() {
        return Stream.empty();
    }

    public String value() {
        return greenTreeNode().value();
    }
}
