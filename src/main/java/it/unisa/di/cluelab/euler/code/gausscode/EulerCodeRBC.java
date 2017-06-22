/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;

import java.text.ParseException;
import java.util.Arrays;

/**
 * @author Mattia De Rosa
 */
public class EulerCodeRBC {
	private GaussCodeRBC[] gaussCodeRBCs;
	private SegmentCode[][] withins;
	private SegmentCode[][] outers;

	public EulerCodeRBC(GaussCodeRBC[] gaussCodeRBCs, SegmentCode[][] withins,
			SegmentCode[][] outers) {
		int l = gaussCodeRBCs == null ? -1 : gaussCodeRBCs.length;
		if (l != (withins == null ? -1 : withins.length)
				|| l != (outers == null ? -1 : outers.length))
			throw new IllegalArgumentException(
					"Different gaussCodeRBCs/withins/outers length.");
		this.gaussCodeRBCs = gaussCodeRBCs;
		this.withins = withins;
		this.outers = outers;
	}

	public EulerCodeRBC(GaussCodeRBC[] gaussCodeRBCs, SegmentCode[][] withins,
			SegmentCode[][] outers,
			boolean checkRegionBoundaryCodeOfNonSingleSubEDs) {
		this(gaussCodeRBCs, withins, outers);
		if (checkRegionBoundaryCodeOfNonSingleSubEDs) {
			for (GaussCodeRBC gcrbc : gaussCodeRBCs) {
				Symbol[][] gc = gcrbc.getGaussCode();
				if (!(gc.length == 1 && gc[0].length == 1))
					gcrbc.getRegionBoundaryCode();
			}
		}
	}

	public EulerCodeRBC(String eulerCodeRBCString) throws ParseException {
		String[] diags = eulerCodeRBCString.split("\n\n+");
		gaussCodeRBCs = new GaussCodeRBC[diags.length];
		withins = new SegmentCode[diags.length][];
		outers = new SegmentCode[diags.length][];

		for (int i = 0; i < diags.length; i++) {
			String parts[] = diags[i].split("\n", 2);
			if (parts.length != 2)
				throw new ParseException(
						"Wrong ED specification: missing row(s).", i + 1);

			String[] segs = parts[0].split(";");
			if (segs.length != 3)
				throw new ParseException("Missing \";\".", i + 1);

			String within = segs[1].trim();
			if (!within.startsWith("within:"))
				throw new ParseException("Missing \"within:\".", i + 1);
			within = within.substring(7).trim();
			withins[i] = within.equals("0") ? null : RegionCode
					.regionCode(within);

			String outer = segs[2].trim();
			if (!outer.startsWith("outer:"))
				throw new ParseException("Missing \"outer:\".", i + 1);
			outer = outer.substring(6).trim();
			outers[i] = RegionCode.regionCode(outer);

			gaussCodeRBCs[i] = new GaussCodeRBC(parts[1]);
		}
	}

	public EulerCodeRBC(String eulerCodeRBCString,
			boolean checkRegionBoundaryCodeOfNonSingleSubEDs)
			throws ParseException {
		this(eulerCodeRBCString);
		if (checkRegionBoundaryCodeOfNonSingleSubEDs) {
			try {
				for (GaussCodeRBC gcrbc : gaussCodeRBCs) {
					Symbol[][] gc = gcrbc.getGaussCode();
					if (!(gc.length == 1 && gc[0].length == 1))
						gcrbc.getRegionBoundaryCode();
				}
			} catch (IllegalStateException e) {
				throw new ParseException(e.getMessage(), -1);
			}
		}
	}

	public GaussCodeRBC[] getGaussCodeRBCs() {
		return gaussCodeRBCs;
	}

	public SegmentCode[][] getWithins() {
		return withins;
	}

	public SegmentCode[][] getOuters() {
		return outers;
	}

	public String getEulerCodeRBCString() {
		if (gaussCodeRBCs == null)
			return null;
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < gaussCodeRBCs.length; i++) {
			code.append("d_" + (i + 1) + ": ");
			code.append(Arrays.toString(gaussCodeRBCs[i].getCurveLabels()));
			code.append("; within: ");
			code.append(withins[i] == null ? "0" : RegionCode
					.regionCodeString(withins[i]));
			code.append("; outer: ");
			code.append(RegionCode.regionCodeString(outers[i]));
			code.append('\n');
			code.append(gaussCodeRBCs[i].getGaussCodeString());
			code.append("\n\n");
		}
		if (code.length() > 1)
			code.setLength(code.length() - 2);
		return code.toString();
	}

	@Override
	public String toString() {
		return ("EulerCodeRBC [eulerCodeRBCString=" + getEulerCodeRBCString() + "]")
				.replace("\n", "; ");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + Arrays.hashCode(gaussCodeRBCs);
		result = prime * result + Arrays.hashCode(outers);
		return prime * result + Arrays.hashCode(withins);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof EulerCodeRBC))
			return false;
		EulerCodeRBC other = (EulerCodeRBC) obj;
		if (!Arrays.equals(gaussCodeRBCs, other.gaussCodeRBCs))
			return false;
		if (!Arrays.deepEquals(outers, other.outers))
			return false;
		if (!Arrays.deepEquals(withins, other.withins))
			return false;
		return true;
	}

}
