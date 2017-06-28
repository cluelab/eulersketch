/*******************************************************************************
 * Copyright (c) 2015 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package it.unisa.di.cluelab.euler.code.zoneGeneration;
import it.unisa.di.cluelab.euler.code.gausscode.RegionCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author Rafiq Saleh
 */
public class EulerCodeData implements Serializable{
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 8265972991583510974L;
	public SegmentCode[] outerFace;
	public char [] curveLabels;
	public Symbol[][] gaussCode;
	public List <Set<String>> zones;
	public List <SegmentCode[]> rgb;
	public EulerCodeData(char[] curveLabels, Symbol[][] gaussCode,
			SegmentCode[] outerFace,List <Set<String>> zones, List <SegmentCode[]> rgb) {
		this.gaussCode=gaussCode;
		this.outerFace = outerFace;
		this.curveLabels=curveLabels;
		this.zones=zones;
		this.rgb=rgb;
	}
	public EulerCodeData(char[] curveLabels, Symbol[][] gaussCode,
			SegmentCode[] outerFace) {
		this.gaussCode=gaussCode;
		this.outerFace = outerFace;
		this.curveLabels=curveLabels;
	}
	public SegmentCode[] getOuterFace() {
		return outerFace;
	}
	public List<SegmentCode[]> getRegionCode() {
		return rgb;
	}
	public List<SegmentCode[]> ComputeRegionCode(Symbol[][] ogp) {
		return RegionCode.computeRegionBoundaryCode(ogp);
	}
	public Symbol [][] getGaussCode() {
		return gaussCode;
	}
	public  char[] getCurveLabels() {
		return curveLabels;
	}
	public List<Set<String>> getZones() {
		return zones;
	}
	
}
