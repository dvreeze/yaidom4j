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

import java.util.Collection;
import java.util.Map;

/**
 * Module descriptor of yaidom4j. It mainly provides an XML element query API, both as abstract
 * API and as concrete XML element implementations offering that query API.
 * <p>
 * The concrete XML element implementations are immutable and thread-safe, without depending on
 * immutable collections such as those provided by Guava. Instead, immutability of collections is
 * achieved with calls such as {@link java.util.List#copyOf(Collection)} and {@link java.util.Map#copyOf(Map)}.
 * <p>
 * The dependency outside the JDK is only JSpecify for its "nullness" annotations. It is exposed in the API,
 * therefore it is a transitive dependency. Of the JDK, only modules java.base and java.xml are needed.
 *
 * @author Chris de Vreeze
 */
module eu.cdevreeze.yaidom4j {
    requires transitive java.xml;
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

    // Experimental new API

    exports eu.cdevreeze.yaidom4j.experimental.jaxp;
    exports eu.cdevreeze.yaidom4j.experimental.core;
    exports eu.cdevreeze.yaidom4j.experimental.queryapi;
    exports eu.cdevreeze.yaidom4j.experimental.dom;
    exports eu.cdevreeze.yaidom4j.experimental.parse;
    exports eu.cdevreeze.yaidom4j.experimental.print;
}
