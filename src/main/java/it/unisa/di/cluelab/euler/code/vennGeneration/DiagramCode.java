/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.vennGeneration;

import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import java.util.List;

/**
 * @author Rafiq Saleh
 */
public class DiagramCode extends GaussCodeRBC {
	private static final long serialVersionUID = -3813982507453895939L;

	SegmentCode[] outerFace;

	public DiagramCode(char[] curveLabels, Symbol[][] gaussCode,
			SegmentCode[] outerFace) {
		super(curveLabels, gaussCode);
		this.outerFace = outerFace;
	}

	protected DiagramCode(char[] curveLabels, Symbol[][] gaussCode,
			List<SegmentCode[]> regionBoundaryCode, SegmentCode[] outerFace) {
		super(curveLabels, gaussCode, regionBoundaryCode);
		this.outerFace = outerFace;
	}

	public SegmentCode[] getOuterFace() {
		return outerFace;
	}
}
