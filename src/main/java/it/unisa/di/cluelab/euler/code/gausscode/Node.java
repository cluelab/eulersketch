/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;

/**
 * @author Rafiq Saleh
 */
 class Node {
    SegmentCode  segment;
    int count;
     
    public Node(SegmentCode s, int n )
    {
        segment =s;
        count=n;
    }
}