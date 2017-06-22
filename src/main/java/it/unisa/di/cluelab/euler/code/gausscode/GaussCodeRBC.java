/*******************************************************************************
 * Copyright (c) 2013 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.gausscode;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mattia De Rosa
 */
public class GaussCodeRBC implements Serializable {
	private static final long serialVersionUID = -2735263214793215294L;

	private char[] curveLabels;
	private Symbol[][] gaussCode;
	private List<SegmentCode[]> regionBoundaryCode;

	public GaussCodeRBC(char[] curveLabels, Symbol[][] gaussCode) {
		if ((curveLabels == null ? -1 : curveLabels.length) != (gaussCode == null ? -1
				: gaussCode.length))
			throw new IllegalArgumentException(
					"Different curveLabels/gaussCode length.");
		this.curveLabels = curveLabels;
		this.gaussCode = gaussCode;
		this.regionBoundaryCode = null;
	}

	public GaussCodeRBC(char[] curveLabels, Symbol[][] gaussCode,
			boolean checkRegionBoundaryCode) {
		this(curveLabels, gaussCode);
		if (checkRegionBoundaryCode)
			getRegionBoundaryCode();
	}

	public GaussCodeRBC(String gaussCodeString) throws ParseException {
		String[] rows = gaussCodeString.trim().split("\n");
		gaussCode = new Symbol[rows.length][];
		curveLabels = new char[rows.length];
		regionBoundaryCode = null;
		for (int i = 0; i < rows.length; i++) {
			String[] sp = rows[i].split(": ");
			if (sp.length != 2)
				throw new ParseException("Unparseable line " + (i + 1) + ".",
						i + 1);
			curveLabels[i] = sp[0].charAt(0);
			String[] points = sp[1].split(" ");
			gaussCode[i] = new Symbol[points.length];
			for (int j = 0; j < points.length; j++) {
				String point = points[j];
				int last = point.length() - 1;
				if (last < 1)
					throw new ParseException("Wrong symbol at line " + (i + 1)
							+ ".", i + 1);
				char sign = point.charAt(last);
				if (sign != '+' && sign != '-')
					throw new ParseException("Wrong sign at line " + (i + 1)
							+ ".", i + 1);
				gaussCode[i][j] = new Symbol(point.substring(0,
						point.length() - 1), sign);
			}
		}
	}

	public GaussCodeRBC(String gaussCodeString, boolean checkRegionBoundaryCode)
			throws ParseException {
		this(gaussCodeString);
		try {
			getRegionBoundaryCode();
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), -1);
		}
	}

	public char[] getCurveLabels() {
		return curveLabels;
	}

	public Symbol[][] getGaussCode() {
		return gaussCode;
	}

	protected GaussCodeRBC(char[] curveLabels, Symbol[][] gaussCode,
			List<SegmentCode[]> regionBoundaryCode) {
		this(curveLabels, gaussCode);
		this.regionBoundaryCode = regionBoundaryCode;
	}

	public List<SegmentCode[]> getRegionBoundaryCode() {
		if (gaussCode == null)
			return null;
		if (regionBoundaryCode == null)
			if (!Misc.checkInput(gaussCode))
				throw new IllegalStateException(
						"GaussCodeRBC: incorrect input.");
		if (!Planarity.CheckPlanarity(gaussCode))
			throw new IllegalStateException("GaussCodeRBC: non-planar word.");
		regionBoundaryCode = RegionCode.computeRegionBoundaryCode(gaussCode);

		return regionBoundaryCode;
	}

	public String getGaussCodeString() {
		if (gaussCode == null)
			return null;
		StringBuilder code = new StringBuilder();
		for (int i = 0; i < gaussCode.length; i++) {
			code.append(curveLabels[i]);
			code.append(':');
			Symbol[] syms = gaussCode[i];
			if (syms != null) {
				for (int j = 0; j < syms.length; j++)
					code.append(" " + syms[j].getLabel() + syms[j].getSign());
			}
			code.append('\n');
		}
		code.setLength(Math.max(0, code.length() - 1));
		return code.toString();
	}

	public String getRegionBoundaryCodeString() {
		List<SegmentCode[]> rbc = getRegionBoundaryCode();
		if (rbc == null)
			return null;
		StringBuilder code = new StringBuilder();
		for (SegmentCode[] r : rbc) {
			code.append('{');
			if (r.length != 0) {
				code.append(r[0].getSegmentCode());
				for (int i = 1; i < r.length; i++)
					code.append("," + r[i].getSegmentCode());
			}
			code.append("}\n");
		}
		code.setLength(Math.max(0, code.length() - 1));
		return code.toString();
	}

	public static String getRegionBoundaryCodeString(List<SegmentCode[]> rbc) {
		// = getRegionBoundaryCode();
		if (rbc == null)
			return null;
		StringBuilder code = new StringBuilder();
		for (SegmentCode[] r : rbc) {
			code.append('{');
			if (r.length != 0) {
				code.append(r[0].getSegmentCode());
				for (int i = 1; i < r.length; i++)
					code.append("," + r[i].getSegmentCode());
			}
			code.append("}\n");
		}
		code.setLength(Math.max(0, code.length() - 1));
		return code.toString();
	}

	/**
	 * Invalidate or flush all cached data. After direct manipulation of the
	 * arrays contents, this is necessary to avoid inconsistent results in
	 * methods like <code>getRegionBoundaryCode</code>.
	 */
	public void invalidate() {
		regionBoundaryCode = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + Arrays.hashCode(curveLabels);
		return prime * result + Arrays.deepHashCode(gaussCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GaussCodeRBC))
			return false;
		GaussCodeRBC other = (GaussCodeRBC) obj;
		if (!Arrays.equals(curveLabels, other.curveLabels))
			return false;
		if (!Arrays.deepEquals(gaussCode, other.gaussCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ("GaussCodeRBC [gaussCodeString=" + getGaussCodeString() + "]")
				.replace("\n", "; ");
	}
}
