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

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Element step, which is a {@link Function} taking an {@link AncestryAwareNodes.Element} and returning a {@link Stream} of
 * elements.
 * <p>
 * This interface is heavily inspired by the Saxon "Step" API. Unlike Saxon's "Step", this API limits
 * itself only to element nodes. Unlike Saxon's "Step", this interface is a functional interface. The
 * downside of that is that this interface can not have any additional abstract instance methods.
 *
 * @author Chris de Vreeze
 */
@FunctionalInterface
public interface AncestryAwareElementStep extends Function<AncestryAwareNodes.Element, Stream<AncestryAwareNodes.Element>> {

    default AncestryAwareElementStep where(Predicate<? super AncestryAwareNodes.Element> predicate) {
        return e -> AncestryAwareElementStep.this.apply(e).filter(predicate);
    }

    default AncestryAwareElementStep then(AncestryAwareElementStep nextStep) {
        return e -> AncestryAwareElementStep.this.apply(e).flatMap(nextStep);
    }
}
