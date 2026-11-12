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

package eu.cdevreeze.yaidom4j.experimental.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test of NamespaceScope.
 *
 * @author Chris de Vreeze
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NamespaceScopeTests {

    // TODO More tests

    @ParameterizedTest
    @MethodSource("provideInputsForResolveSyntacticQName")
    void resolveSyntacticQName_ShouldResolveQNameUnlessUnknownPrefix(
            NamespaceScope scope,
            String syntacticQName,
            @Nullable QName expectedOrNull
    ) {
        if (expectedOrNull == null) {
            assertThrows(RuntimeException.class, () -> scope.resolveSyntacticElementQName(syntacticQName));
        } else {
            assertEquals(expectedOrNull, scope.resolveSyntacticElementQName(syntacticQName));
        }
    }

    @ParameterizedTest
    @MethodSource("provideInputsForSubScopeOf")
    void subScopeOf_ShouldBeBasedOnEntrySetSubset(
            NamespaceScope scope1,
            NamespaceScope scope2,
            boolean expected
    ) {
        assertEquals(expected, scope1.subScopeOf(scope2));
    }

    private static Stream<Arguments> provideInputsForResolveSyntacticQName() {
        return Stream.of(
                Arguments.of(
                        SCOPE_XLINK,
                        "instance",
                        new QName("instance")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xbrl",
                        new QName(XBRLI_NS, "xbrl")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "xbrli:xbrl",
                        new QName(XBRLI_NS, "xbrl")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xbrli:xbrl",
                        new QName(XBRLI_NS, "xbrl")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "notxbrli:xbrl",
                        null
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "notxbrli:xbrl",
                        null
                ),
                Arguments.of(
                        NamespaceScope.empty(),
                        "xml:base",
                        new QName(XMLConstants.XML_NS_URI, "base")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "xml:base",
                        new QName(XMLConstants.XML_NS_URI, "base")
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xml:base",
                        new QName(XMLConstants.XML_NS_URI, "base")
                ),
                Arguments.of(
                        NamespaceScope.empty(),
                        "a:b:c",
                        null
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "a:b:c",
                        null
                )
        );
    }

    private static Stream<Arguments> provideInputsForSubScopeOf() {
        return Stream.of(
                Arguments.of(
                        NamespaceScope.empty(),
                        NamespaceScope.empty(),
                        true),
                Arguments.of(
                        NamespaceScope.empty(),
                        SCOPE_LINK,
                        true),
                Arguments.of(
                        SCOPE_LINK,
                        NamespaceScope.empty(),
                        false),
                Arguments.of(
                        SCOPE_LINK,
                        new NamespaceScope(Map.of("link", "http://www.xbrl.org/2003/NOT-linkbase")),
                        false),
                Arguments.of(
                        new NamespaceScope(Map.of("link", "http://www.xbrl.org/2003/NOT-linkbase")),
                        SCOPE_LINK_XLINK,
                        false),
                Arguments.of(
                        SCOPE_XLINK,
                        SCOPE_LINK,
                        false),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        true),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        SCOPE_LINK_XLINK_XBRL,
                        false),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        true),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        new NamespaceScope(Map.of(
                                "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink",
                                "xbrli", XBRLI_NS, "", "http://www.xbrl.org/2003/NOT-instance"
                        )),
                        false)
        );
    }

    private static final String XBRLI_NS = "http://www.xbrl.org/2003/instance";
    private static final String XBRLDI_NS = "http://xbrl.org/2006/xbrldi";
    private static final String LINK_NS = "http://www.xbrl.org/2003/linkbase";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";
    private static final String ISO4217_NS = "http://www.xbrl.org/2003/iso4217";
    private static final String GAAP_NS = "http://xasb.org/gaap";

    private static final NamespaceScope SCOPE_LINK =
            new NamespaceScope(Map.of(
                    "link", LINK_NS
            ));
    private static final NamespaceScope SCOPE_XLINK =
            new NamespaceScope(Map.of(
                    "xlink", "http://www.w3.org/1999/xlink"
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK =
            new NamespaceScope(Map.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink"
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK_XBRL =
            new NamespaceScope(Map.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink",
                    "xbrli", XBRLI_NS
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT =
            new NamespaceScope(Map.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink",
                    "xbrli", XBRLI_NS, "", XBRLI_NS
            ));
}
