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

import com.google.common.base.Preconditions;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

/**
 * Opinionated utility to create TransformerHandler instances.
 * <p>
 * The created TransformerHandler instances can be used with class "ImmutableDomConsumingSaxEventGenerator"
 * to output an immutable DOM tree. The idea is to first set a Result on the TransformerHandler (which
 * is a SAX handler), and then to generate SAX events on that TransformerHandler.
 *
 * @author Chris de Vreeze
 */
public class TransformerHandlers {

    private TransformerHandlers() {
    }

    public static SAXTransformerFactory newSaxTransformerFactory() {
        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        Preconditions.checkArgument(tf.getFeature(SAXTransformerFactory.FEATURE));
        return (SAXTransformerFactory) tf;
    }

    public static TransformerHandler newTransformerHandler(SAXTransformerFactory stf) {
        try {
            TransformerHandler th = stf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
            th.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            th.getTransformer().setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            return th;
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static TransformerHandler newTransformerHandler() {
        return newTransformerHandler(newSaxTransformerFactory());
    }
}
