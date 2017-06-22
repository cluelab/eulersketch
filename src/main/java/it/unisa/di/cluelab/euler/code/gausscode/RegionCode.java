/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;
import java.text.ParseException;
import java.util.*;

/**
 * @author Rafiq Saleh
 */
public class RegionCode 
{
	public static ArrayList <SegmentCode[]>  computeRegionBoundaryCode(Symbol [][] ogp)
    {
        
        ArrayList <SegmentCode[]> R = new ArrayList<SegmentCode[]>();
        if(ogp==null) return null;
        if(ogp.length==1)
        {
        	if(ogp[0].length==1)
        	{ 
        		String label=ogp[0][0].getLabel();
        		Symbol p1= new Symbol(label,' ');
        		Symbol p2= new Symbol(label,' ');
        		SegmentCode s1= new SegmentCode(p1,p2,'+');
        		SegmentCode s2= new SegmentCode(p1,p2,'-');
        		SegmentCode []r1= {s1};
        		SegmentCode []r2= {s2};
        		R.add(r1);
        		R.add(r2);
        		return R;        		
        				
        	}
        }
        ArrayList <SegmentCode> m= new ArrayList<SegmentCode>();
        ArrayList <SegmentCode> S= computeSegmentsCode(ogp);
        for(SegmentCode s:S )
        {
            if(!Misc.isIn(s, m))
            {
                ArrayList <SegmentCode> r= new ArrayList<SegmentCode>();
                r.add(s);m.add(s);
                SegmentCode s2 =traverse(s,S);
                while(!Misc.isEqual(s, s2))
                {
                    r.add(s2);
                    m.add(s2);
                    s2 =traverse(s2,S);
                }
                
                R.add(Misc.toArray(r));
            } 
        }
        return R;
    }
    public static SegmentCode traverse(SegmentCode s,ArrayList <SegmentCode> S)
    {
        SegmentCode s2;
        char c=s.getDirection();
        if(c=='+')
        {
            Symbol l=s.getSecondSymbol();
            if(l.getSign()=='+')
            {
                s2= getNextSegment(new Symbol(l.getLabel(),'-'),'-',S);
            }
            else
            {
                s2= getNextSegment(new Symbol(l.getLabel(),'+'),'+',S);  
            }
        }
        else
        {
            Symbol l=s.getFirstSymbol();
            if(l.getSign()=='+')
            {
                s2= getNextSegment(new Symbol(l.getLabel(),'-'),'+',S);
            }
            else
            {
                s2= getNextSegment(new Symbol(l.getLabel(),'+'),'-',S);  
            }   
        }
        return s2;
    }
    static SegmentCode getNextSegment(Symbol l, char c,ArrayList <SegmentCode> S)
    {       
        for(SegmentCode s: S)
        {
            if(c=='+')
            {
                if(s.getFirstSymbol().getLabel().equals(l.getLabel()) &&
                        s.getFirstSymbol().getSign()==l.getSign() &&
                        s.getDirection()==c)
                {
                    return s;
                }
            }
            else
            {
                if(s.getSecondSymbol().getLabel().equals(l.getLabel()) &&
                        s.getSecondSymbol().getSign()==l.getSign() &&
                        s.getDirection()==c)
                {
                    return s;
                }
            }
        }
        return null;
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
    
    public static SegmentCode[] getRegionCode(SegmentCode s,ArrayList <SegmentCode[]> ar)
    {
        SegmentCode[] region= new SegmentCode[0];
        for(SegmentCode[] r: ar)
        {
            for(SegmentCode t:r)
            {
                if(s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel()) &&
                   s.getFirstSymbol().getSign()== t.getFirstSymbol().getSign() &&
                   s.getSecondSymbol().getLabel().equals(t.getSecondSymbol().getLabel()) &&
                   s.getSecondSymbol().getSign()== t.getSecondSymbol().getSign()&& s.getDirection()==t.getDirection())
                {
                    return r;
                }
            }
        }
        return region;
    }

	public static String regionCodeString(SegmentCode[] scs) {
		if (scs.length == 0)
			return "{}";
		StringBuilder res = new StringBuilder("{");
		for (SegmentCode sc : scs) {
			Symbol s1 = sc.getFirstSymbol();
			Symbol s2 = sc.getSecondSymbol();
			res.append("(" + s1.getLabel() + s1.getSign() + " " + s2.getLabel()
					+ s2.getSign() + "," + sc.getDirection() + "),");
		}
		res.setCharAt(res.length() - 1, '}');
		return res.toString();
	}

	public static SegmentCode[] regionCode(String code) throws ParseException {
		if (!code.startsWith("{("))
			throw new ParseException("\"" + code
					+ "\" does non start with \"{(\".", 0);
		if (!code.endsWith(")}"))
			throw new ParseException("\"" + code
					+ "\" does non end with \")}\".", 0);
		String[] segs = code.substring(2, code.length() - 2).split("\\),\\(");
		SegmentCode[] res = new SegmentCode[segs.length];
		for (int i = 0; i < segs.length; i++) {
			String[] parts = segs[i].split(",", 2);
			String[] nums = parts[0].split(" ", 2);
			String n1 = nums[0].trim();
			String n2 = nums[1].trim();
			String d = parts[1].trim();
			if ((!n1.endsWith("+") && !n1.endsWith("-"))
					|| (!n2.endsWith("+") && !n2.endsWith("-"))
					|| (!d.equals("+") && !d.equals("-")))
				throw new ParseException("Illegal sign.", 0);

			int n1last = n1.length() - 1, n2last = n2.length() - 1;
			res[i] = new SegmentCode(new Symbol(n1.substring(0, n1last),
					n1.charAt(n1last)), new Symbol(n2.substring(0, n2last),
					n2.charAt(n2last)), d.charAt(0));
		}
		return res;
	}
}
   