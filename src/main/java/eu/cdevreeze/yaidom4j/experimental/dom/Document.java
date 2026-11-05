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

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Immutable document node.
 * <p>
 * Not a record class, and using reference equality rather than (in this case) rather expensive value equality.
 *
 * @author Chris de Vreeze
 */
public final class Document implements Node {

    private final GreenXmlTrees.Document greenTreeNode;

    public Document(GreenXmlTrees.Document greenTreeNode) {
        this.greenTreeNode = greenTreeNode;
    }

    @Override
    public int offset() {
        return 0;
    }

    @Override
    public Optional<Node> parentOption() {
        return Optional.empty();
    }

    @Override
    public GreenXmlTrees.Document greenTreeNode() {
        return greenTreeNode;
    }

    @Override
    public Stream<Node> childStream() {
        List<GreenXmlTrees.CanBeChild> children = greenTreeNode.children();
        int numberOfChildren = children.size();
        return IntStream.range(0, numberOfChildren)
                .mapToObj(idx -> switch (children.get(idx)) {
                    case GreenXmlTrees.Element e -> new Element(idx, this, e);
                    case GreenXmlTrees.Text t -> new Text(idx, this, t);
                    case GreenXmlTrees.Comment c -> new Comment(idx, this, c);
                    case GreenXmlTrees.ProcessingInstruction p -> new ProcessingInstruction(idx, this, p);
                });
    }

    public Optional<URI> uriOption() {
        return greenTreeNode().uriOption();
    }

    public List<CanBeChild> children() {
        // Immutable List
        return List.copyOf(
                childStream().map(n -> (CanBeChild) n).toList()
        );
    }
}
