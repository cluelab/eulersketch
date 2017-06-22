/*******************************************************************************
 * Copyright (c) 2017 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Mattia De Rosa
 *
 */
public class EulerCodeUtilsTest {
    @Test
    public void testAppHasAGreeting() {
        assertEquals("_", EulerCodeUtils.stripHtml("<sub>"));
    }
}
