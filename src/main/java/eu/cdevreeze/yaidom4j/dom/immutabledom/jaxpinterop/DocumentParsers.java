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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Optional;

/**
 * Simple utility to parse input XML into immutable Documents.
 * <p>
 * Do not use this utility when the parsed documents take too much memory.
 *
 * @author Chris de Vreeze
 */
public class DocumentParsers {

    private DocumentParsers() {
    }

    public static Builder builder(SAXParserFactory saxParserFactory) {
        return new Builder(saxParserFactory);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final SAXParserFactory saxParserFactory;
        private boolean removeInterElementWhitespace;

        public Builder(SAXParserFactory saxParserFactory) {
            this.saxParserFactory = saxParserFactory;
        }

        public Builder() {
            this(SaxParsers.newNonValidatingSaxParserFactory());
        }

        public Builder removingInterElementWhitespace() {
            this.removeInterElementWhitespace = true;
            return this;
        }

        public DocumentParser build() {
            return new DefaultDocumentParser(saxParserFactory, removeInterElementWhitespace);
        }
    }

    public static final class DefaultDocumentParser implements DocumentParser {

        private final SAXParserFactory saxParserFactory;
        private final boolean removeInterElementWhitespace;

        public DefaultDocumentParser(SAXParserFactory saxParserFactory, boolean removeInterElementWhitespace) {
            this.saxParserFactory = saxParserFactory;
            this.removeInterElementWhitespace = removeInterElementWhitespace;
        }

        public Document parse(InputSource inputSource) {
            ImmutableDomProducingSaxHandler saxHandler = new ImmutableDomProducingSaxHandler();
            Optional<URI> optDocUri = Optional.ofNullable(inputSource.getSystemId()).map(URI::create);
            SaxParsers.parse(inputSource, saxHandler, saxParserFactory);
            Document rawDoc = saxHandler.resultingDocument();
            Document doc = removeInterElementWhitespace ? rawDoc.removeInterElementWhitespace() : rawDoc;
            return optDocUri.map(doc::withUri).orElse(doc);
        }

        public Document parse(URI inputFile) {
            try {
                return parse(new InputSource(inputFile.toURL().openStream()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
