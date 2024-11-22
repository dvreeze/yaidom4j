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

package eu.cdevreeze.yaidom4j.examples.dialects;

import eu.cdevreeze.yaidom4j.dom.ancestryaware.Document;
import eu.cdevreeze.yaidom4j.dom.immutabledom.Element;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentParsers;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinter;
import eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop.DocumentPrinters;
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.Names;
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.servlet.Servlet;
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.servlet.ServletMapping;
import eu.cdevreeze.yaidom4j.examples.dialects.jakartaee.servlet.WebApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Servlet dialect tests.
 * <p>
 * This is not a regular unit test.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServletDialectTests {

    private Document parseDocument() {
        InputStream inputStream = ServletDialectTests.class.getResourceAsStream("/dialects/sample-web.xml");
        return Document.from(
                DocumentParsers.builder().removingInterElementWhitespace().build()
                        .parse(new InputSource(inputStream))
        );
    }

    @Test
    public void testWebAppParsing() {
        Document doc = parseDocument();

        WebApp webApp = WebApp.parse(doc.documentElement());

        assertEquals(
                List.of("welcome", "ServletErrorPage", "IncludedServlet", "ForwardedServlet"),
                webApp.servlets().stream().map(Servlet::name).toList()
        );

        assertEquals(
                List.of("welcome", "ServletErrorPage", "IncludedServlet", "ForwardedServlet"),
                webApp.servletMappings().stream().map(ServletMapping::servletName).toList()
        );
        assertEquals(
                List.of("/hello.welcome", "/ServletErrorPage", "/IncludedServlet", "/ForwardedServlet"),
                webApp.servletMappings().stream().flatMap(m -> m.urlPatterns().stream()).toList()
        );

        Element newDocElem = webApp.toXml(new QName(NS, "web-app", "w"));

        DocumentPrinter docPrinter = DocumentPrinters.instance();

        System.out.println(docPrinter.print(newDocElem));

        Element newDocElem2 = webApp.toXml(new QName(NS, "web-app"));

        System.out.println();
        System.out.println(docPrinter.print(newDocElem2));

        assertEquals(newDocElem2.toClarkNode(), newDocElem.toClarkNode());
    }

    private static final String NS = Names.JAKARTAEE_NS;
}
