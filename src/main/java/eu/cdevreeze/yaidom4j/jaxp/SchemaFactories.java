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

package eu.cdevreeze.yaidom4j.jaxp;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

/**
 * Utility to create SchemaFactory instances.
 *
 * @author Chris de Vreeze
 */
public class SchemaFactories {

    private SchemaFactories() {
    }

    /**
     * Creates a SchemaFactory. The factory is aware of XXE attacks and tries to protect against them.
     */
    public static SchemaFactory newSchemaFactory() {
        SchemaFactory sf = SchemaFactory.newDefaultInstance();
        try {
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return sf;
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
