/*
 * Copyright (c) 2004 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.pvaccess.util.test;

import junit.framework.TestCase;
import org.epics.pvaccess.util.WildcardMatcher;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 */
public class WildcardMatcherTest extends TestCase {

    public WildcardMatcherTest(String methodName) {
        super(methodName);
    }

    /**
     * Conversion test.
     */
    public void testWildcard() {
        assertTrue(WildcardMatcher.match("[-aa-]*", "01 abAZ"));
        assertTrue(WildcardMatcher.match("[\\!a\\-bc]*", "!!!b-bb-"));
        assertTrue(WildcardMatcher.match("*zz", "zz"));
        assertTrue(WildcardMatcher.match("[abc]*zz", "zz"));

        assertFalse(WildcardMatcher.match("[!abc]*a[def]", "xyzbd"));
        assertTrue(WildcardMatcher.match("[!abc]*a[def]", "xyzad"));
        assertTrue(WildcardMatcher.match("[a-g]l*i?", "gloria"));
        assertTrue(WildcardMatcher.match("[!abc]*e", "smile"));
        assertTrue(WildcardMatcher.match("[-z]", "a"));
        assertFalse(WildcardMatcher.match("[]", ""));
        assertTrue(WildcardMatcher.match("[a-z]*", "java"));
        assertTrue(WildcardMatcher.match("*.*", "command.com"));
        assertFalse(WildcardMatcher.match("*.*", "/var/etc"));
        assertTrue(WildcardMatcher.match("**?*x*[abh-]*Q", "XYZxabbauuZQ"));
    }
}
