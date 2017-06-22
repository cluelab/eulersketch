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
public class Planarity 
{
    public static Boolean CheckPlanarity(Symbol [][]  word)
    {
        int faces=RegionCode.computeRegionBoundaryCode(word).size();
        int edges = getNoOfPoints(word);
        int vertices =edges/2;
        int Euler_Characteristics=vertices -edges+faces;
        if(Euler_Characteristics==2) return true;
        return false;
    }
    public static int getNoOfPoints(Symbol [][] input)
    {
        int n=0;
        for(int i=0;i<input.length;i++)
        {
                n+=input[i].length;
        }
        return n;
    }
}