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
public class Symbol implements Serializable
{
	private static final long serialVersionUID = 3924630718670063262L;
    private String label;
    private char sign;
    public Symbol(String l, char s )
    {
        label =l;
        sign=s;
    }
    public String getLabel()
    {
        return label;
    }
    public char getSign()
    {
        return sign;
    }
    @Override
	public String toString() {
		String symbol=label+sign;
    	
    	return symbol;
    	
    }
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + ((label == null) ? 0 : label.hashCode());
		return prime * result + sign;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Symbol))
			return false;
		Symbol other = (Symbol) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (sign != other.sign)
			return false;
		return true;
	}
}
