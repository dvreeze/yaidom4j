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

/**
 * Concrete yaidom4j element implementations, implementing the abstract query API and/or transformation
 * API in packages {@link eu.cdevreeze.yaidom4j.queryapi} and {@link eu.cdevreeze.yaidom4j.transformationapi}, respectively.
 * <p>
 * This package and its subpackages directly depend on the {@link eu.cdevreeze.yaidom4j.queryapi},
 * {@link eu.cdevreeze.yaidom4j.transformationapi} and {@link eu.cdevreeze.yaidom4j.jaxp} Java packages.
 * <p>
 * The {@link eu.cdevreeze.yaidom4j.dom.clark} subpackage does not depend on the other {@link eu.cdevreeze.yaidom4j.dom}
 * subpackages. The {@link eu.cdevreeze.yaidom4j.dom.immutabledom} subpackage depends on the
 * {@link eu.cdevreeze.yaidom4j.dom.clark} subpackage, due to conversions to "Clark nodes". Similarly, the
 * {@link eu.cdevreeze.yaidom4j.dom.ancestryaware} subpackage depends both on the {@link eu.cdevreeze.yaidom4j.dom.clark}
 * and {@link eu.cdevreeze.yaidom4j.dom.immutabledom} subpackages.
 *
 * @author Chris de Vreeze
 */
@NullMarked
package eu.cdevreeze.yaidom4j.dom;

import org.jspecify.annotations.NullMarked;