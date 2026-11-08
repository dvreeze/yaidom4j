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

package eu.cdevreeze.yaidom4j.experimental.parse;

import module java.base;
import module java.xml;
import eu.cdevreeze.yaidom4j.experimental.dom.Document;

/**
 * Document parser API.
 *
 * @author Chris de Vreeze
 */
public interface DocumentParser {

    Document parse(InputSource inputSource);

    Document parse(URI inputFile);
}
