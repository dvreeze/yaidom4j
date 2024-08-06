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

package eu.cdevreeze.yaidom4j.core;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Optional;
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
            Optional<QName> expected
    ) {
        if (expected.isEmpty()) {
            assertThrows(RuntimeException.class, () -> scope.resolveSyntacticQName(syntacticQName));
        } else {
            assertEquals(expected.orElseThrow(), scope.resolveSyntacticQName(syntacticQName));
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
                        Optional.of(new QName("instance"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xbrl",
                        Optional.of(new QName(XBRLI_NS, "xbrl"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "xbrli:xbrl",
                        Optional.of(new QName(XBRLI_NS, "xbrl"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xbrli:xbrl",
                        Optional.of(new QName(XBRLI_NS, "xbrl"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "notxbrli:xbrl",
                        Optional.empty()
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "notxbrli:xbrl",
                        Optional.empty()
                ),
                Arguments.of(
                        NamespaceScope.empty(),
                        "xml:base",
                        Optional.of(new QName(XMLConstants.XML_NS_URI, "base"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL,
                        "xml:base",
                        Optional.of(new QName(XMLConstants.XML_NS_URI, "base"))
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "xml:base",
                        Optional.of(new QName(XMLConstants.XML_NS_URI, "base"))
                ),
                Arguments.of(
                        NamespaceScope.empty(),
                        "a:b:c",
                        Optional.empty()
                ),
                Arguments.of(
                        SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT,
                        "a:b:c",
                        Optional.empty()
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
                        new NamespaceScope(ImmutableMap.of("link", "http://www.xbrl.org/2003/NOT-linkbase")),
                        false),
                Arguments.of(
                        new NamespaceScope(ImmutableMap.of("link", "http://www.xbrl.org/2003/NOT-linkbase")),
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
                        new NamespaceScope(ImmutableMap.of(
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
            new NamespaceScope(ImmutableMap.of(
                    "link", LINK_NS
            ));
    private static final NamespaceScope SCOPE_XLINK =
            new NamespaceScope(ImmutableMap.of(
                    "xlink", "http://www.w3.org/1999/xlink"
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK =
            new NamespaceScope(ImmutableMap.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink"
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK_XBRL =
            new NamespaceScope(ImmutableMap.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink",
                    "xbrli", XBRLI_NS
            ));
    private static final NamespaceScope SCOPE_LINK_XLINK_XBRL_WITH_DEFAULT =
            new NamespaceScope(ImmutableMap.of(
                    "link", LINK_NS, "xlink", "http://www.w3.org/1999/xlink",
                    "xbrli", XBRLI_NS, "", XBRLI_NS
            ));
}
