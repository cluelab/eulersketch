/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;

import java.io.Serializable;

/**
 * @author Rafiq Saleh
 */
public class SegmentCode implements Serializable
{
	private static final long serialVersionUID = 5526634330630440388L;
    private Symbol p,q;
    private char direction;
   
    public SegmentCode(Symbol p1, Symbol p2, char d )
    {
            p=p1;
            q=p2;
            direction=d;
    }
    public Symbol getFirstSymbol()
    {
            return p;
    }
    public Symbol getSecondSymbol()
    {
            return q;
    }
    public char getDirection()
    {
            return direction;
    }
    public String getSegmentCode()
    {

            String s="("+p.getLabel()+p.getSign()+", "+q.getLabel()+q.getSign()+", "+direction+ ")";
            return s;
    }
    public static SegmentCode inverse(SegmentCode s)
    {
        char c=s.getDirection();

        if(c=='+') s= new SegmentCode(s.getFirstSymbol(),s.getSecondSymbol(),'-');
        if(c=='-') s = new SegmentCode(s.getFirstSymbol(),s.getSecondSymbol(),'+');
        return s;
    }
    public static String identifyCurve(SegmentCode s, Symbol [][] ogp)
    {
        String t="";
        for(int i=0;i<ogp.length;i++)
        {
            for(int j=0;j<ogp[i].length;j++)
            {
                if(s.getFirstSymbol().getLabel().equals(ogp[i][j].getLabel()) && s.getFirstSymbol().getSign()==ogp[i][j].getSign())
                {
                    if(j==ogp[i].length-1)
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][0].getLabel()) && s.getSecondSymbol().getSign()==ogp[i][0].getSign())
                        {
                            t="C"+(i+1);
                            return t;
                        }
                    }
                    else
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][j+1].getLabel())&& s.getSecondSymbol().getSign()==ogp[i][j+1].getSign())
                        {
                            t="C"+(i+1);
                            return t;
                        }
                    }
                }
            }
        }
        return t;
    } public static String identifyCurveLabels(SegmentCode s, Symbol [][] ogp)
    {
        char [] curveLabels = new char[ogp.length];
        char ch='A';
        for(int i=0;i<curveLabels.length;i++)
        	curveLabels[i]=ch++;
        
    	String t="";
        for(int i=0;i<ogp.length;i++)
        {
            for(int j=0;j<ogp[i].length;j++)
            {
                if(s.getFirstSymbol().getLabel().equals(ogp[i][j].getLabel()) && s.getFirstSymbol().getSign()==ogp[i][j].getSign())
                {
                    if(j==ogp[i].length-1)
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][0].getLabel()) && s.getSecondSymbol().getSign()==ogp[i][0].getSign())
                        {
                        	t=""+curveLabels[i];
                            return t;
                        }
                    }
                    else
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][j+1].getLabel())&& s.getSecondSymbol().getSign()==ogp[i][j+1].getSign())
                        {
                            t=""+curveLabels[i];
                            return t;
                        }
                    }
                }
            }
        }
        return t;
    }
    public static String identifyCurveLabels(SegmentCode s, Symbol [][] ogp,char [] curveLabels)
    {
       if(curveLabels ==null){
    	   curveLabels= new char[ogp.length];
        char ch='A';
        for(int i=0;i<ogp.length;i++)
        	curveLabels[i]=ch++;}
        
    	String t="";
        for(int i=0;i<ogp.length;i++)
        {
            for(int j=0;j<ogp[i].length;j++)
            {
                if(s.getFirstSymbol().getLabel().equals(ogp[i][j].getLabel()) && s.getFirstSymbol().getSign()==ogp[i][j].getSign())
                {
                    if(j==ogp[i].length-1)
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][0].getLabel()) && s.getSecondSymbol().getSign()==ogp[i][0].getSign())
                        {
                        	t=""+curveLabels[i];
                            return t;
                        }
                    }
                    else
                    {
                        if(s.getSecondSymbol().getLabel().equals(ogp[i][j+1].getLabel())&& s.getSecondSymbol().getSign()==ogp[i][j+1].getSign())
                        {
                            t=""+curveLabels[i];
                            return t;
                        }
                    }
                }
            }
        }
        return t;
    }

    @Override
    public String toString()
    {

            String s="("+p.getLabel()+p.getSign()+", "+q.getLabel()+q.getSign()+", "+direction+ ")";
            return s;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + direction;
		result = prime * result + ((p == null) ? 0 : p.hashCode());
		return prime * result + ((q == null) ? 0 : q.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SegmentCode))
			return false;
		SegmentCode other = (SegmentCode) obj;
		if (direction != other.direction)
			return false;
		if (p == null) {
			if (other.p != null)
				return false;
		} else if (!p.equals(other.p))
			return false;
		if (q == null) {
			if (other.q != null)
				return false;
		} else if (!q.equals(other.q))
			return false;
		return true;
	}
}
