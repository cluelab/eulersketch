/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.ZoneGeneration;

import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * @author Mattia De Rosa
 */
public class EDData implements Serializable {
	private static final long serialVersionUID = -4875461411253826028L;
	private GaussCodeRBC gcrbc;
	private SegmentCode[] scs;
	private Map<Set<String>, Integer> regionCount;
	private int maxSymLbl = 0;

	public EDData(GaussCodeRBC gaussCodeRBC, SegmentCode[] outer,
			Map<Set<String>, Integer> regionCount) {
		this.gcrbc = gaussCodeRBC;
		this.scs = outer;
		this.regionCount = regionCount;
		for (Symbol[] c : gaussCodeRBC.getGaussCode())
			for (Symbol s : c) {
				int l = Integer.parseInt(s.getLabel());
				if (l > maxSymLbl)
					maxSymLbl = l;
			}
	}

	public GaussCodeRBC getGaussCodeRBC() {
		return gcrbc;
	}

	public SegmentCode[] getOuter() {
		return scs;
	}

	public Map<Set<String>, Integer> getRegionCount() {
		return regionCount;
	}

	public int getMaxSymLbl() {
		return maxSymLbl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + ((gcrbc == null) ? 0 : gcrbc.hashCode());
		result = prime * result + maxSymLbl;
		result = prime * result + Arrays.hashCode(scs);
		return prime * result
				+ ((regionCount == null) ? 0 : regionCount.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EDData other = (EDData) obj;
		if (gcrbc == null) {
			if (other.gcrbc != null)
				return false;
		} else if (!gcrbc.equals(other.gcrbc))
			return false;
		if (maxSymLbl != other.maxSymLbl)
			return false;
		if (!Arrays.equals(scs, other.scs))
			return false;
		if (regionCount == null) {
			if (other.regionCount != null)
				return false;
		} else if (!regionCount.equals(other.regionCount))
			return false;
		return true;
	}

}
