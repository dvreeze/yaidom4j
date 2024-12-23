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

package eu.cdevreeze.yaidom4j.dom.ancestryaware;

import java.util.Objects;

/**
 * XML processing instruction node.
 *
 * @author Chris de Vreeze
 * @deprecated Use {@link AncestryAwareNodes.ProcessingInstruction} instead.
 */
@Deprecated(forRemoval = true, since = "0.10.0")
public record ProcessingInstruction(String target, String data) implements CanBeDocumentChild {

    public ProcessingInstruction {
        Objects.requireNonNull(target);
        Objects.requireNonNull(data);
    }

    @Override
    public eu.cdevreeze.yaidom4j.dom.immutabledom.ProcessingInstruction underlyingNode() {
        return new eu.cdevreeze.yaidom4j.dom.immutabledom.ProcessingInstruction(target, data);
    }

    @Override
    public boolean isElement() {
        return false;
    }
}
