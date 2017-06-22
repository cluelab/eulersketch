/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;
import java.util.*;

/**
 * @author Rafiq Saleh
 */
public class Orientation 
{  
	   
	   public static ArrayList<String> computeOrientation(Symbol [][] ogp, SegmentCode [] outerFace, 
	            ArrayList <SegmentCode []>  R)
	    {
	        ArrayList <String>  CW= new ArrayList <String>();
	        ArrayList <String>  AC= new ArrayList <String>();
	         ArrayList <SegmentCode []>  M = new ArrayList<SegmentCode []>();
	          ArrayList <SegmentCode []> currentRegions= new ArrayList<SegmentCode []>();
	          currentRegions.add(outerFace);
	        while(CW.size()+AC.size()<ogp.length)
	        {
	            ArrayList <SegmentCode []> currentRegions2 = new ArrayList<SegmentCode[]>();
	            for(int i=0;i<currentRegions.size();i++)
	            {
	                SegmentCode [] rgb = currentRegions.get(i);
	                SegmentCode r= rgb[0];
	                if(!Misc.isIn(r,M))
	                {
	                    M.add(rgb);
	                    identifyOrientationOfCurves(rgb,CW,AC,ogp);
	                    if(CW.size()+AC.size()==ogp.length)
	                    {
	                        return CW;
	                    }
	                    currentRegions2.add(rgb);
	                }
	                for(int j=0; j<rgb.length;j++)
	                {
	                    r=SegmentCode.inverse(rgb[j]);
	                    if(!Misc.isIn(r,M))
	                    {
	                        SegmentCode []rgb2=RegionCode.getRegionCode(r,R);
	                        currentRegions2.add(rgb2);
	                    }
	                }
	            }
	            currentRegions= currentRegions2;
	        }
	        return CW;
	    } 
 	    public static void identifyOrientationOfCurves(SegmentCode []rgb,ArrayList <String>  CW,
 	            ArrayList <String>  AC,Symbol [][] ogp)
 	    {
 	        for(SegmentCode s:rgb)
 	        {
 	            String t =SegmentCode.identifyCurve(s,ogp);
 	            if(!Misc.isIn(t,CW)&& !Misc.isIn(t,AC))
 	            {
 	                if(s.getDirection()=='+')
 	                {
 	                    CW.add(t);
 	                }
 	                else
 	                {
 	                    AC.add(t);
 	                }
 	            }
 	        }
 	    }
 	   public static ArrayList <Node> rank(Node node, ArrayList <SegmentCode>  S, SegmentCode []  outerFace,
 	            ArrayList <SegmentCode []>  R,ArrayList <SegmentCode []>  M,ArrayList <Node> list ) 
 	    {           
 	        int count=node.count;
 	        SegmentCode s=node.segment;         
 	        if(isIn(s, outerFace)) return list;
 	        else    
 	        {             
 	            if(!Misc.isIn(s, M))
 	            { 
 	               Node parent= new Node(s,count);
 	               list.add(parent);
 	                SegmentCode []  r= RegionCode.getRegionCode(s,R);
 	                M.add(r);               
 	                for(SegmentCode s1: r)
 	                {
 	                    SegmentCode s2=SegmentCode.inverse(s1);
 	                    if(!Misc.isIn(s2, M))
 	                    {
 	                        Node child=new Node(s2,parent.count);
 	                        if(Misc.isIn(s2,S)) 
 	                        {
 	                             count=parent.count+1; 
 	                             child=new Node(s2,count);                             
 	                        }
 	                        list.add(child);
 	                        rank(child,S,outerFace,R,M,list);
 	                    }
 	                }
 	            }
 	        }
 	        return list;  
 	    }  	   
	    public static Boolean isIn(SegmentCode s, SegmentCode[] rgb)
	    {
	        for(SegmentCode  t: rgb)
	        {
	            if(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) && s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
	                       s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) && s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign()
	                       && s.getDirection()==t.getDirection())
	            {
	                return true;
	            }
	        }
	        return false;
	    } 
 	   
 	   public static Boolean isIn(SegmentCode s, ArrayList <SegmentCode> rgb)
	    {
	        for(SegmentCode  t: rgb)
	        {
	            if(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) &&
	                    s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
	                    s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) &&
	                    s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign())
	            {
	                return true;
	            }
	         }
	        return false;
	                
	    } 	   
	     	    
	}
	    