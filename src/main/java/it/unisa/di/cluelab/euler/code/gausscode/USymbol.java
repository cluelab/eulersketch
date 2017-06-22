/*******************************************************************************
 * Copyright (c) 2016 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;

/**
 * @author Mattia De Rosa
 */
public class USymbol extends Symbol {
	private static final long serialVersionUID = 62513798300362665L;
	private boolean under;

	public USymbol(String label, char sign, boolean under) {
		super(label, sign);
		this.under = under;
	}

	public boolean isUnder() {
		return under;
	}

	public void setUnder(boolean under) {
		this.under = under;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		return prime * result + (under ? 1231 : 1237);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof USymbol))
			return false;
		USymbol other = (USymbol) obj;
		if (under != other.under)
			return false;
		return true;
	}

}
