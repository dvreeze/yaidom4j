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

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.util.function.Consumer;

/**
 * Utility to create TransformerHandler instances.
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

    /**
     * Creates a SAXTransformerFactory. The factory is aware of XXE attacks and tries to protect against them.
     */
    public static SAXTransformerFactory newSaxTransformerFactory() {
        TransformerFactory tf = TransformerFactory.newDefaultInstance();
        Preconditions.checkArgument(tf.getFeature(SAXTransformerFactory.FEATURE));
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return (SAXTransformerFactory) tf;
    }

    public static TransformerHandler newTransformerHandler(SAXTransformerFactory stf, Consumer<TransformerHandler> thInitializer) {
        try {
            TransformerHandler th = stf.newTransformerHandler();
            thInitializer.accept(th);
            return th;
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static TransformerHandler newTransformerHandler(SAXTransformerFactory stf) {
        return newTransformerHandler(stf, Config.indentingAndOmittingXmlDeclaration());
    }

    public static TransformerHandler newTransformerHandler() {
        return newTransformerHandler(newSaxTransformerFactory());
    }

    public static final class Config {

        private Config() {
        }

        public static Consumer<TransformerHandler> indenting(int indent) {
            return th -> {
                th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
                th.getTransformer().setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
            };
        }

        public static Consumer<TransformerHandler> omittingXmlDeclaration() {
            return th -> th.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }

        public static Consumer<TransformerHandler> indentingAndOmittingXmlDeclaration() {
            return indenting(4).andThen(omittingXmlDeclaration());
        }
    }
}
