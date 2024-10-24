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

package eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;

import eu.cdevreeze.yaidom4j.dom.immutabledom.Document;
import eu.cdevreeze.yaidom4j.jaxp.SaxParsers;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import java.net.URI;

/**
 * Document parser API.
 *
 * @author Chris de Vreeze
 */
public interface DocumentParser {

    Document parse(InputSource inputSource, SAXParserFactory saxParserFactory);

    default Document parse(InputSource inputSource) {
        return parse(inputSource, SaxParsers.newNonValidatingSaxParserFactory());
    }

    Document parse(URI inputFile, SAXParserFactory saxParserFactory);

    default Document parse(URI inputFile) {
        return parse(inputFile, SaxParsers.newNonValidatingSaxParserFactory());
    }
}
