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
 * Module descriptor of yaidom4j. It mainly provides an XML element query API, both as abstract
 * API and as concrete XML element implementations offering that query API.
 * <p>
 * The dependencies outside the JDK are Guava for its immutable collections and JSpecify for its
 * "nullness" annotations. Both dependencies are exposed in the API, so they are transitive dependencies.
 *
 * @author Chris de Vreeze
 */
module eu.cdevreeze.yaidom4j {
    requires transitive java.xml;
    // Needed for the immutable collections Guava provides
    requires transitive com.google.common;
    requires transitive org.jspecify;

    exports eu.cdevreeze.yaidom4j.core;

    // Several "DOM" implementations
    exports eu.cdevreeze.yaidom4j.dom.ancestryaware;
    exports eu.cdevreeze.yaidom4j.dom.clark;
    exports eu.cdevreeze.yaidom4j.dom.clark.jaxpinterop;
    exports eu.cdevreeze.yaidom4j.dom.immutabledom;
    exports eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;

    exports eu.cdevreeze.yaidom4j.jaxp;

    // Abstract query (and transformation API) implemented by the "DOM" implementations above
    exports eu.cdevreeze.yaidom4j.queryapi;
    exports eu.cdevreeze.yaidom4j.transformationapi;
}
