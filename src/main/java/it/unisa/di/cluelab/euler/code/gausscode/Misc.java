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
public class Misc 
{
    public static Boolean isIn(SegmentCode s, List <SegmentCode> rgb)
    {
        for(int i=0;i<rgb.size();i++)
        {
            SegmentCode t= rgb.get(i);
            if(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) && s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
                   s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) && s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign()
                   && s.getDirection()==t.getDirection())
            {
                return true;
            }
        }
        return false;
    }
    public static Boolean isIn(String s, ArrayList <String> S)
    {
        for(String t:S)
        if(s.equals(t)) return true;
        return false;
    }
    public static Boolean isIn(SegmentCode s, ArrayList <SegmentCode[]> rgb)
    {
        for(SegmentCode [] r: rgb)
            for(int i=0;i<r.length;i++)
            {
                SegmentCode t= r[i];
                if(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) && s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
			   s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) && s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign()
			   && s.getDirection()==t.getDirection())
                {
                    return true;
                }
            }
        return false;
    }

    public static Boolean isEqual(SegmentCode s, SegmentCode t)
    {
        return(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) && 
                s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
           s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) && 
                s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign());
       
           
    } 
    static SegmentCode[] toArray(ArrayList <SegmentCode> rgb)
    {
        //ArrayList <SegmentCode[]> rgb2 = new ArrayList <SegmentCode[]>();
        SegmentCode [] reg = new SegmentCode [rgb.size()];
        for(int i= 0; i<reg.length; i++)
        {
            reg[i]=rgb.get(i);
        }
        return reg;
    }
    public static Boolean checkInput(Symbol [][] input)
    {
        for(Symbol[] gw:input)
        for(Symbol i:gw)
        {
            if(!checkOccurrences(i,input)) return false;
        }
        return true;
    }
    public static Boolean checkOccurrences(Symbol p, Symbol [][] input)
    {
        int j =0;
        int k =0;
        for(Symbol[] gw:input)
        for(Symbol i:gw)
        {
            if(p.getLabel().equals(i.getLabel()) && p.getSign()==i.getSign())
            {
                j=j+1;
            }
            if(p.getLabel().equals(i.getLabel()) && p.getSign()!=i.getSign())
            {
                k=k+1;
            }
        }
        if(j!=k) return false;
        return true;
    }
    
public static List<List<Symbol[]>> checkDisjointWords(Symbol[][] gaussCode)
{ 
	List<String[]>ogp = new ArrayList<String[]>();
	for(Symbol [] gc :gaussCode)
	{
		String[] w= new String[gc.length];
		for(int i=0;i<w.length;i++)
		{
			w[i]=gc[i].getLabel();
		}
		ogp.add(w);
	}
	String [] ds = getDistinctSymbols(ogp);
	List<List<Symbol[]>>ogpList = new ArrayList<List<Symbol[]>>();
	List <String []>list = new ArrayList<String []>();
	for(String [] w:ogp)
	{
		String []w2 = new String[w.length];
		System.arraycopy(w, 0, w2, 0, w.length);
		list.add(w2);
	}
	Map<String,int[]> map = new HashMap<String,int[]>();
	for(int i=0;i<ds.length;i++)
	{
		list=mergeWordsContaining(ds[i],list);
		identifyWordsContaining(ds[i],ogp,map);
	}		 
	if(list.size()>1)
	{
		List<Set<Integer>> myList = new ArrayList<Set<Integer>>();
		
		for(String [] w: list)
		{
			Set<Integer> set = new HashSet<Integer>();
			for(String s: w)
			{
				int [] value=map.get(s);
				//System.out.println(s+"\t"+value[0]+", "+value[1]);
				set.add(value[0]);
				if(value.length>1)
				set.add(value[1]);
			}
			myList.add(set);								
		}
		
		for(Set<Integer> set:myList)
		{
			List<Symbol[]> w= new ArrayList<Symbol[]>();
			for(int i:set)
				w.add(gaussCode[i]);
			ogpList.add(w);					
		}
		
	}
	else 
	{
		ogpList.add(Arrays.asList(gaussCode));
	}		
	return ogpList;

}
public static List<String[]> identifyWordsContaining(String s, List<String[]> ogp,Map<String,int[]> map)
{
	boolean b =false;
	for(int i=0;i<ogp.size();i++)
	{
		String w1 [] = ogp.get(i);

		for(String t:w1)
		{
			if(s.equals(t))
			{	
				b=true; 
				break;
			}
		}
		if(b)
		{
			boolean c= false;
			for(int j=i+1;j<ogp.size();j++)
			{
				String w2 [] = ogp.get(j);
				for(String t:w2)
				{
					if(s.equals(t))
					{	
						map.put(s, new int []{i,j});

						c=true;
						break;
					}
				}
				if(c) break;
				
			}
			if(c) break;
			else map.put(s, new int []{i}); break;
		}

	}
	return ogp;
}public static List<String[]> mergeWordsContaining(String s, List<String[]> ogp)
{
	boolean b =false;
	for(int i=0;i<ogp.size();i++)
	{
		String w1 [] = ogp.get(i);
		if(!b &&!checkForRepitition(w1, s))
		{
			for(String t:w1)
			{
				if(s.equals(t))
				{	
					b=true; 
					break;
				}
			}
			if(b)
			{
				boolean c= false;
				for(int j=i+1;j<ogp.size();j++)
				{
					String w2 [] = ogp.get(j);
					for(String t:w2)
					{
						if(s.equals(t))
						{	
							String [] w3=merge(w1,w2);
							ogp.add(w3);
							ogp.remove(w1);
							ogp.remove(w2);
							
							c=true;
							break;
						}
					}
					if(c) break;
				}
				if(c) break;
			}
		}
	}
	return ogp;
}public static String [] merge(String [] w1,String [] w2)
{
	int n= w1.length+w2.length;
	String [] s1 = new String[n];
	for(int i=0;i<w1.length;i++)
	{
		s1[i]=w1[i];
	}
	int k=w1.length;
	for(int i=0;k<n;i++)
	{
		s1[k]=w2[i];
		k++;
	}
	return s1;
}public static Boolean checkForRepitition(String [] w,String s)
{
	int n = w.length;
	Boolean b=false;
	for(int k = 0; k < n; k++)
	{
		if(s.equals(w[k]))
		{
			for(int q = k+1; q < n; q++)
			{
				if(s.equals(w[q]))
				{
					b=true;
					break;
				}
			}
			if(b) break;
		}
	}
	return b;
}
public static String [] getDistinctSymbols(List<String []> ogp)
{
	List<String> ls= new ArrayList<String>();
	for(String[] w : ogp)
		for(String s: w)
			if(!ls.contains(s)) ls.add(s);
	String [] ds= new String[ls.size()];
	for(int i=0;i<ls.size();i++)
		ds[i]=ls.get(i);

	return ds;
}

}
