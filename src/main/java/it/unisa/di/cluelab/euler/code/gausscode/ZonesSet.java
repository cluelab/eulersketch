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
public class ZonesSet 
{
	
	public static ArrayList <String> computeZonesSet(Symbol[][] ogp, SegmentCode []  r, Collection <SegmentCode[]> R)
	    {
	        ArrayList <String> zones = new ArrayList < String>();
	        for(List <List <String>> zone : computeZonesSet(ogp, R, r))
	            zones.add(zone.get(0).toString()+ "," + zone.get(1).toString());
	        return zones;
	    }
	public static List<Set<String>> computeAllZones(Symbol[][] ogp, SegmentCode []  r, Collection <SegmentCode[]> R)
    {
        List<Set<String>> zoneSet = new ArrayList<Set<String>>();

		for(List <List <String>> zone : computeZonesSet(ogp, R, r))
		{
			Set<String> zs = new HashSet <String>();
			for(String ls : zone.get(0))
			{
				String t="";
	        	for(int i=0;i<ls.length();i++) 
	        	{
	        		t+=ls.charAt(i);
	        	}
	        	zs.add(t);
			}
			if(zs.size()>0) zoneSet.add(zs);
			else zoneSet.add(new HashSet <String>(Arrays.asList("?")));
		}
        return zoneSet;
    }
	 public static List <List <List <String>>> computeZonesSet(Symbol[][] ogp, Collection <SegmentCode[]> rbc, SegmentCode [] r)
	    {
	        ArrayList <SegmentCode[]> R = rbc instanceof ArrayList ? (ArrayList <SegmentCode[]>) rbc : new ArrayList<SegmentCode[]>(rbc);
	        ArrayList <List <List <String>>> zones = new ArrayList <List <List <String>>>();
	        ArrayList <SegmentCode>  CW= new ArrayList<SegmentCode>();
	        ArrayList <SegmentCode []>  M= new ArrayList<>();
	        for(int i=0;i<ogp.length;i++)
	        {
	            Symbol [] w =ogp[i];
				ArrayList <SegmentCode> S = computeSegmentsCode(w);         
				for(SegmentCode s:S)
				{
					 M= new ArrayList<>();
					Node node= new Node(s,0);
					ArrayList <Node> list= new ArrayList <Node>();
					Orientation.rank(node,S,r,R,M,list);
					int count =0;
					for(Node n:list)
					{
						if(Orientation.isIn(n.segment,r)) 
						{
							count=n.count;
							break;
						}
					}
					 if(count%2==0) CW.add(s);                    
				}
			} 
			for(int i=0;i<R.size();i++)
	        {
	            ArrayList <String> X =new ArrayList <String>();
	            ArrayList <String> Y =new ArrayList <String>();
	              M= new ArrayList <SegmentCode []>();
	            ArrayList <SegmentCode []>  rgb= new ArrayList <SegmentCode []>();
	            rgb.add(R.get(i));
	            breadthFirstSearch(ogp,rgb,R,M,X,Y,CW,null);
	           zones.add(Arrays.asList((List<String>)X, (List<String>)Y));
	        }
	        return zones;
	    }
	 public static List <List <List <String>>> computeZonesSet(Symbol[][] ogp, Collection <SegmentCode[]> rbc, SegmentCode [] r, char [] curveLabels)
	    {
	        ArrayList <SegmentCode[]> R = rbc instanceof ArrayList ? (ArrayList <SegmentCode[]>) rbc : new ArrayList<SegmentCode[]>(rbc);
	        ArrayList <List <List <String>>> zones = new ArrayList <List <List <String>>>();
	        ArrayList <SegmentCode>  CW= new ArrayList<SegmentCode>();
	        ArrayList <SegmentCode []>  M= new ArrayList<>();
	        if(ogp==null) return null;
	        if(ogp.length==1)
	        {
	        	if(ogp[0].length==1)
	        	{
	        		ArrayList <String> X =new ArrayList <String>();
		            ArrayList <String> Y =new ArrayList <String>();
		            String s=""+curveLabels[0];
		            Y.add(s);
	        		 zones.add(Arrays.asList((List<String>)X, (List<String>)Y));
	        		 Y.clear();
	        		 X.add(s);
	        		 zones.add(Arrays.asList((List<String>)X, (List<String>)Y));
	        		 return zones;
	        	}
	        }
	        for(int i=0;i<ogp.length;i++)
	        {
	            Symbol [] w =ogp[i];
				ArrayList <SegmentCode> S = computeSegmentsCode(w);         
				for(SegmentCode s:S)
				{
					 M= new ArrayList<>();
					Node node= new Node(s,0);
					ArrayList <Node> list= new ArrayList <Node>();
					Orientation.rank(node,S,r,R,M,list);
					int count =0;
					for(Node n:list)
					{
						if(Orientation.isIn(n.segment,r)) 
						{
							count=n.count;
							break;
						}
					}
					 if(count%2==0) CW.add(s);                    
				}
			} 
			for(int i=0;i<R.size();i++)
	        {
	            ArrayList <String> X =new ArrayList <String>();
	            ArrayList <String> Y =new ArrayList <String>();
	              M= new ArrayList <SegmentCode []>();
	            ArrayList <SegmentCode []>  rgb= new ArrayList <SegmentCode []>();
	            rgb.add(R.get(i));
	            breadthFirstSearch(ogp,rgb,R,M,X,Y,CW,curveLabels);
	           zones.add(Arrays.asList((List<String>)X, (List<String>)Y));
	        }
	        return zones;
	    }
	 public static  Map <List<String>, List<List<String>>> computeTreeZoneSet(Symbol[][] ogp, Collection <SegmentCode[]> rbc, SegmentCode [] r)
	    {
	        ArrayList <SegmentCode[]> R = rbc instanceof ArrayList ? (ArrayList <SegmentCode[]>) rbc : new ArrayList<SegmentCode[]>(rbc);
	        //ArrayList <List <List <String>>> zones = new ArrayList <List <List <String>>>();
	        Map <List<String>, List<List<String>>> dualGraph = new HashMap<List<String>, List<List<String>>>();
	        ArrayList <SegmentCode>  CW= new ArrayList<SegmentCode>();
	        ArrayList <SegmentCode []>  M= new ArrayList<>();
	        for(int i=0;i<ogp.length;i++)
	        {
	            Symbol [] w =ogp[i];
				ArrayList <SegmentCode> S = computeSegmentsCode(w);         
				for(SegmentCode s:S)
				{
					 M= new ArrayList<>();
					Node node= new Node(s,0);
					ArrayList <Node> list= new ArrayList <Node>();
					Orientation.rank(node,S,r,R,M,list);
					int count =0;
					for(Node n:list)
					{
						if(Orientation.isIn(n.segment,r)) 
						{
							count=n.count;
							break;
						}
					}
					 if(count%2==0) CW.add(s);                    
				}
			} 
			for(int i=0;i<R.size();i++)
	        {
	            ArrayList <String> X =new ArrayList <String>();
	            ArrayList <String> Y =new ArrayList <String>();
	              M= new ArrayList <SegmentCode []>();
	            ArrayList <SegmentCode []>  rgb= new ArrayList <SegmentCode []>();
	            rgb.add(R.get(i));
	            breadthFirstSearch(ogp,rgb,R,M,X,Y,CW,null);
	            List <SegmentCode[]>  cr= new ArrayList <SegmentCode[]>();
	            List <List<String>>  value= new ArrayList <List<String>>();
	            for(int j=0; j<R.get(i).length;j++)
                {
	            	
	            	SegmentCode sr=SegmentCode.inverse(R.get(i)[j]);
                    if(!belongsTo(sr,cr))
                    {
                    	ArrayList <String>X2 =new ArrayList <String>();
        	            ArrayList <String> Y2 =new ArrayList <String>();
        	            ArrayList <SegmentCode []>   M2= new ArrayList <SegmentCode []>();
                        ArrayList<SegmentCode []>rgb2= new ArrayList < SegmentCode []>();
                        SegmentCode[] currentRegion=RegionCode.getRegionCode(sr,R);
                        rgb2.add(currentRegion);
                        cr.add(currentRegion);
                        breadthFirstSearch(ogp,rgb2,R,M2,X2,Y2,CW,null);
                        value.add(new ArrayList<String>(X2));
                    }
                }
	            dualGraph.put(X, value);
	        }
	        return dualGraph;
	    }
	    private static boolean belongsTo(SegmentCode s, List<SegmentCode[]> list) {
	    	for(SegmentCode [] r:list)
	    		for(SegmentCode t: r)
	    			if(s.getSegmentCode().equals(t.getSegmentCode())) return true;
	    		
		return false;
	}
		public static void breadthFirstSearch(Symbol[][] ogp, ArrayList <SegmentCode []> currentRegions, ArrayList <SegmentCode []>  R, ArrayList <SegmentCode []>  M,
	        ArrayList <String>  X, ArrayList <String>  Y, ArrayList <SegmentCode>  CW,char[] curveLabels)
	        {
	            while(X.size()+Y.size()<ogp.length)
	            {
	                ArrayList <SegmentCode []> currentRegions2 = new ArrayList<SegmentCode[]>();
	                for(int i=0;i<currentRegions.size();i++)
	                {
	                    SegmentCode [] rgb = currentRegions.get(i);
	                    SegmentCode r= rgb[0];
	                    if(!Misc.isIn(r,M))
	                    {
	                        M.add(rgb);
	                        identifyContainingCurves(ogp,rgb,X,Y,CW,curveLabels);
	                        if(X.size()+Y.size()==ogp.length)
	                        {
	                                return;
	                        }
	                        //currentRegions2.add(rgb);
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
	    }       
	     public static void identifyContainingCurves( Symbol [][]  ogp,SegmentCode []rgb, ArrayList <String> X,
	             ArrayList <String> Y, ArrayList <SegmentCode>  CW)
	    {	
	    	 for(SegmentCode s:rgb)
	    	 {
	    		 String t =SegmentCode.identifyCurveLabels(s,ogp);
	    		 if(!Misc.isIn(t,X) && !Misc.isIn(t,Y))
	    		 {
	    			 if(Misc.isIn(s,CW)) Y.add(t);                
	    			 else X.add(t);
	    		 }
        }	
	         
	    } 
	     public static void identifyContainingCurves( Symbol [][]  ogp,SegmentCode []rgb, ArrayList <String> X,
	             ArrayList <String> Y, ArrayList <SegmentCode>  CW, char [] curveLabels)
	    {	
	    	 for(SegmentCode s:rgb)
	    	 {
	    		 String t =SegmentCode.identifyCurveLabels(s,ogp,curveLabels);
	    		 if(!Misc.isIn(t,X) && !Misc.isIn(t,Y))
	    		 {
	    			 if(Misc.isIn(s,CW)) Y.add(t);                
	    			 else X.add(t);
	    		 }
        }	
	         
	    } 
	    static ArrayList <SegmentCode>  computeSegmentsCode(Symbol [] w)
	    {
	        ArrayList <SegmentCode> S= new ArrayList<SegmentCode>();
	       
	        for(int j=0;j<w.length;j++)
	        {
	                if(j==w.length-1)
	                {
	                    S.add(new SegmentCode(w[w.length-1],w[0],'+'));
	                    S.add(new SegmentCode(w[w.length-1],w[0],'-'));   
	                }
	                else
	                {
	                    S.add(new SegmentCode(w[j],w[j+1],'+'));
	                    S.add(new SegmentCode(w[j],w[j+1],'-')); 
	                }
	            }
	            
	        return S;
	    }
	    public static ArrayList <SegmentCode>  computeSegmentsCode(Symbol [][] ogp)
	    {
	        ArrayList <SegmentCode> S= new ArrayList<SegmentCode>();
	        for(int i=0;i<ogp.length;i++)
	        {
	            Symbol [] w=ogp[i];
	            for(int j=0;j<w.length;j++)
	            {
	                    if(j==w.length-1)
	                    {
	                        S.add(new SegmentCode(w[w.length-1],w[0],'+'));
	                        S.add(new SegmentCode(w[w.length-1],w[0],'-'));   
	                    }
	                    else
	                    {
	                        S.add(new SegmentCode(w[j],w[j+1],'+'));
	                        S.add(new SegmentCode(w[j],w[j+1],'-')); 
	                    }
	                }
	            } 
	        return S;
	    } 
	   
}
