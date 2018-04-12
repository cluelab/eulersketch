/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.RegionCode;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.USymbol;
import it.unisa.di.cluelab.euler.code.gausscode.ZonesSet;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.jgrapht.alg.util.UnionFind;

/**
 * @author Mattia De Rosa
 */
public class EulerCode {
	private static final double POINTS_MIN_DISTANCE = 1.; //TODO chose a good min distance
	private static final double DOUBLE_EQUALITY_TOLERANCE = 0.000976562; //TODO chose a good tolerance
	private final LinkedHashMap<Character, IncidentPointsPolygon> curves = new LinkedHashMap<Character, IncidentPointsPolygon>();
	private final LinkedHashMap<Integer, IncidentPoint> incidentPoints = new LinkedHashMap<Integer, IncidentPoint>();
	private final TreeSet<Character> unusedCurveLabels = new TreeSet<Character>();
	private final LinkedList<Integer> unusedIncidentPointLabels = new LinkedList<Integer>();
	// unmodifiable collections for getter methods
	private final Map<Character, IncidentPointsPolygon> curvesUnm = Collections.unmodifiableMap(curves);
	private final Map<Integer, IncidentPoint> incidentPointsUnm = Collections.unmodifiableMap(incidentPoints);
	private List<Zone> zones = null;

	public EulerCode() {
		this.unusedCurveLabels.add('A');
		this.unusedIncidentPointLabels.add(1);
	}
	
	public EulerCode(char[] preferredCurveLabels, Polygon[] polys,
			int[] preferredIncidentPointLabels,
			Point2D[] preferredIncidentPointPositions) {
		this(preferredCurveLabels, polys, preferredIncidentPointLabels,
				preferredIncidentPointPositions, null);
	}
	
	public EulerCode(char[] preferredCurveLabels, Polygon[] polys,
			int[] preferredIncidentPointLabels,
			Point2D[] preferredIncidentPointPositions,
			Set<String>[] preferredIncidentPointUnderCurves) {
		this.unusedCurveLabels.add('A');

		if (preferredCurveLabels.length != polys.length)
			throw new IllegalArgumentException(
					"Different length: preferredCurveLabels, polys.");

		if (preferredIncidentPointLabels != null
				|| preferredIncidentPointPositions != null) {
			if (preferredIncidentPointLabels.length != preferredIncidentPointPositions.length)
				throw new IllegalArgumentException(
						"Different length: preferredIncidentPointLabes, preferredIncidentPointPositions.");

			TreeMap<Integer, Entry<Point2D, Set<String>>> sipl = new TreeMap<Integer, Entry<Point2D, Set<String>>>();
			for (int i = 0; i < preferredIncidentPointLabels.length; i++) {
				int lbl = preferredIncidentPointLabels[i];
				Point2D pt = preferredIncidentPointPositions[i];
				if (lbl > 0 && pt != null)
					sipl.put(
							lbl,
							new AbstractMap.SimpleEntry<Point2D, Set<String>>(
									pt,
									preferredIncidentPointUnderCurves == null ? null
											: preferredIncidentPointUnderCurves[i]));
			}

			int lastIndex = sipl.isEmpty() ? 1 : sipl.lastKey() + 1;
			for (Integer i = 1; i <= lastIndex; i++) {
				if (!sipl.containsKey(i))
					this.unusedIncidentPointLabels.add(i);
			}
			for (int i = 0; i < preferredCurveLabels.length; i++)
				addCurve(preferredCurveLabels[i], polys[i], sipl);

			// reset unusedIncidentPointLabels
			this.unusedIncidentPointLabels.clear();
			int maxLbl = 0;
			for (Entry<Integer, IncidentPoint> ep : incidentPoints.entrySet()) {
				int lbl = ep.getKey();
				if (maxLbl < lbl)
					maxLbl = lbl;
			}
			for (Integer i = 1; i < maxLbl; i++) {
				if (!this.incidentPoints.containsKey(i))
					this.unusedIncidentPointLabels.add(i);
			}
			this.unusedIncidentPointLabels.add(maxLbl + 1);
		} else {
			this.unusedIncidentPointLabels.add(1);
			for (int i = 0; i < preferredCurveLabels.length; i++)
				addCurve(preferredCurveLabels[i], polys[i], null);
		}
	}
	
	public void clear() {
		this.curves.clear();
		this.incidentPoints.clear();
		this.unusedCurveLabels.clear();
		this.unusedIncidentPointLabels.clear();
		this.unusedCurveLabels.add('A');
		this.unusedIncidentPointLabels.add(1);
		this.zones = null;
	}
	
	public Map<Character, IncidentPointsPolygon> getCurves() {
		return this.curvesUnm;
	}

	public Map<Integer, IncidentPoint> getIncidentPoints() {
		return this.incidentPointsUnm;
	}

	public Character addCurve(Polygon newCurve) {
		return this.addCurve(null, newCurve);
	}
	
	public Character addCurve(Character preferredCurveLabel, Polygon poly) {
		return addCurve(preferredCurveLabel, poly, null);
	}
	
	private Character addCurve(Character preferredCurveLabel, Polygon poly,
			Map<Integer, Entry<Point2D, Set<String>>> suggestedIncidentPointLabels) {
		// basic check (also a polygon with > 2 points can be invalid)
		if(poly == null || poly.npoints <= 2) throw new IllegalArgumentException("Illegal input polygon");
		this.zones = null;
		if(preferredCurveLabel == null || this.curves.containsKey(preferredCurveLabel)) {
			preferredCurveLabel = this.unusedCurveLabels.pollFirst();
			if(this.unusedCurveLabels.isEmpty()) this.unusedCurveLabels.add((char)(preferredCurveLabel + 1));
		} else {
			for(char c = (char)(this.unusedCurveLabels.last() + 1), end = (char)(preferredCurveLabel + 1); c <= end; c++) { 
				this.unusedCurveLabels.add(c);
			}
			this.unusedCurveLabels.remove(preferredCurveLabel);
		}
		IncidentPointsPolygon pl = new IncidentPointsPolygon(poly.xpoints, poly.ypoints, poly.npoints);
		for (int i = 0; i < pl.npoints; i++) {
			Iterator<Entry<Character, IncidentPointsPolygon>> itr = this.curves
					.entrySet().iterator();
			boolean notFirst = false;
			do {
				Character ccl;
				IncidentPointsPolygon cc;
				if (notFirst) {
					Entry<Character, IncidentPointsPolygon> ecc = itr.next();
					ccl = ecc.getKey();
					cc = ecc.getValue();
				} else {
					ccl = preferredCurveLabel;
					cc = pl;
				}
				for (int k = 0; k < cc.npoints; k++) {
					Point2D.Double intp = null;
					if (notFirst
							|| (i != k && ((i + 1) % pl.npoints) != k && ((i - 1 + pl.npoints) % pl.npoints) != k))
						intp = GeomUtils.getIntersection(pl.xpoints[i],
								pl.ypoints[i],
								pl.xpoints[(i + 1) % pl.npoints],
								pl.ypoints[(i + 1) % pl.npoints],
								cc.xpoints[k], cc.ypoints[k],
								cc.xpoints[(k + 1) % cc.npoints],
								cc.ypoints[(k + 1) % cc.npoints]);
					if (intp != null) {
						Entry<Integer, IncidentPoint> found = null;
						double dist = POINTS_MIN_DISTANCE;
						for (Entry<Integer, IncidentPoint> ep : this.incidentPoints
								.entrySet()) {
							double cdist = intp.distance(ep.getValue());
							if (dist > cdist) {
								dist = cdist;
								found = ep;
							}
						}

						if (found == null) {
							IncidentPoint ip = new IncidentPoint(intp.x, intp.y);
							ip.incidentCurves.add(ccl);
							ip.incidentCurves.add(preferredCurveLabel);
							Integer newLabel = null;
							Set<String> ucurves = null;
							if(suggestedIncidentPointLabels != null) {
								for (Iterator<Entry<Integer, Entry<Point2D, Set<String>>>> it = suggestedIncidentPointLabels
										.entrySet().iterator(); it.hasNext();) {
									Entry<Integer, Entry<Point2D, Set<String>>> e = it.next();
									Entry<Point2D, Set<String>> ptc = e.getValue();
									if (ip.distance(ptc.getKey()) < POINTS_MIN_DISTANCE) {
										newLabel = e.getKey();
										ucurves = ptc.getValue();
										it.remove();
										break;
									}
								}
							}
							if(newLabel == null){
								newLabel = this.unusedIncidentPointLabels
										.removeFirst();
								if (this.unusedIncidentPointLabels
										.isEmpty())
									this.unusedIncidentPointLabels
											.add(newLabel + 1);
							}
							this.incidentPoints.put(newLabel, ip);
							IncidentPointRef ccipr = new IncidentPointRef(k,
									intp.distance(cc.xpoints[k], cc.ypoints[k]));
							IncidentPointRef plipr = new IncidentPointRef(i,
									intp.distance(pl.xpoints[i], pl.ypoints[i]));
							cc.incidentPointRefs.put(ccipr, newLabel);
							if (notFirst)
								pl.incidentPointRefs.put(plipr, newLabel);
							else {
								Integer nNewLabel = -newLabel;
								pl.incidentPointRefs.put(plipr, nNewLabel);
								this.incidentPoints.put(nNewLabel, ip);
							}
							// chose under curve (by gauss sign)
							double x0 = pl.xpoints[i];
							double y0 = pl.ypoints[i];
							if(ip.x == x0 && ip.y == y0) {
								x0 = pl.xpoints[(i + pl.npoints - 1) % pl.npoints];
								y0 = pl.ypoints[(i + pl.npoints - 1) % pl.npoints];
							}
							double x1 = pl.xpoints[(i + 1) % pl.npoints];
							double y1 = pl.ypoints[(i + 1) % pl.npoints];
							if(ip.x == x1 && ip.y == y1) {
								x1 = pl.xpoints[(i + 2) % pl.npoints];
								y1 = pl.ypoints[(i + 2) % pl.npoints];
							}
							double px = cc.xpoints[(k + 1) % cc.npoints];
							double py = cc.ypoints[(k + 1) % cc.npoints];
							if(ip.x == px && ip.y == py) {
								px = cc.xpoints[(k + 2) % cc.npoints];
								py = cc.ypoints[(k + 2) % cc.npoints];
							}
							if (ucurves == null) { // default under/over
								(notFirst
										|| (poly instanceof GeomUtils.PolygonR && ((GeomUtils.PolygonR) poly)
												.isReversed()) ? ccipr : plipr)
										.setUnder(true);
								// // +/O and -/U
								// // center over ip
								// x0 -= ip.x;
								// y0 -= ip.y;
								// x1 -= ip.x;
								// y1 -= ip.y;
								// px -= ip.x;
								// py -= ip.y;
								// double pi2 = 2 * Math.PI;
								// double aRot = pi2 - Math.atan2(y0, x0);
								// double a1 = (Math.atan2(y1, x1) + aRot) % pi2;
								// double ap = (Math.atan2(py, px) + aRot) % pi2;
								// (ap < a1 ? plipr : ccipr).setUnder(true);
							} else {
								if (ucurves.contains(ccl.toString()))
									ccipr.setUnder(true);
								if (ucurves.contains((notFirst ? "" : "-")
										+ preferredCurveLabel))
									plipr.setUnder(true);
							}
						} else { // TODO examine special cases
							IncidentPoint ip = found.getValue();
							boolean ccCont = ip.incidentCurves.contains(ccl);
							boolean plCont = ip.incidentCurves
									.contains(preferredCurveLabel);
							if (ccCont && plCont) {
								// do nothing: same point found two times on
								if (found.getValue().x != intp.x
										|| found.getValue().y != intp.y)
									System.out.println("T0 Double point s="
											+ ip.incidentCurves.size() + " ("
											+ found.getValue().x + ","
											+ found.getValue().y + ") ("
											+ intp.x + "," + intp.y + ")");
							} else if (ccCont && !plCont) {
								System.out.println("T1 Triple point ("
										+ found.getValue().x + ","
										+ found.getValue().y + ")");
								ip.incidentCurves.add(preferredCurveLabel);
								IncidentPointRef plipr = new IncidentPointRef(
										i, intp.distance(pl.xpoints[i],
												pl.ypoints[i]));
								pl.incidentPointRefs.put(plipr, found.getKey());
							} else if (!ccCont && plCont) {
								System.out.println("T2 Triple point ("
										+ found.getValue().x + ","
										+ found.getValue().y + ")");
								ip.incidentCurves.add(ccl);
								IncidentPointRef ccipr = new IncidentPointRef(
										k, intp.distance(cc.xpoints[k],
												cc.ypoints[k]));
								cc.incidentPointRefs.put(ccipr, found.getKey());
							} else {
								System.out.println("T3 Triple point ("
										+ found.getValue().x + ","
										+ found.getValue().y + ")");
								ip.incidentCurves.add(ccl);
								ip.incidentCurves.add(preferredCurveLabel);
								IncidentPointRef ccipr = new IncidentPointRef(
										k, intp.distance(cc.xpoints[k],
												cc.ypoints[k]));
								IncidentPointRef plipr = new IncidentPointRef(
										i, intp.distance(pl.xpoints[i],
												pl.ypoints[i]));
								cc.incidentPointRefs.put(ccipr, found.getKey());
								pl.incidentPointRefs.put(plipr, found.getKey());
							}
						}
					}
				}
				notFirst = true;
			} while (itr.hasNext());
		}

		for (Entry<Character, IncidentPointsPolygon> ecc : this.curves
				.entrySet()) {
			Character ippLbl = ecc.getKey();
			IncidentPointsPolygon ipp = ecc.getValue();
			// test the first point of the existent
			if (pl.contains(ipp.xpoints[0], ipp.ypoints[0]))
				ipp.firstPointContCurves.add(preferredCurveLabel);
			// test the first point of the new
			if (ipp.contains(pl.xpoints[0], pl.ypoints[0]))
				pl.firstPointContCurves.add(ippLbl);
			// points in the ecc pre-existent curve
			if (!ipp.incidentPointRefs.isEmpty()) {
				Iterator<IncidentPointRef> it = ipp.incidentPointRefs.keySet()
						.iterator();
				for (IncidentPointRef first = it.next(), pre = first, cur; first != null; pre = cur) {
					if (it.hasNext())
						cur = it.next();
					else {
						cur = first;
						first = null;
					}
					Point2D.Double oldSlp = pre.suggestedLabelPoint;
					pre.suggestedLabelPoint = GeomUtils.meanPoint(ipp,
							pre.cRef, pre.dis, cur.cRef, cur.dis);
					// if the meanPoint have changed position it must be
					// retested for containment on all the other existent curves
					if (!GeomUtils.equalsTolerance(oldSlp,
							pre.suggestedLabelPoint, DOUBLE_EQUALITY_TOLERANCE)) {
						pre.folContCurves.clear();
						for (Entry<Character, IncidentPointsPolygon> eOthCurve : this.curves
								.entrySet()) {
							Character othCurveLabel = eOthCurve.getKey();
							if (!othCurveLabel.equals(ippLbl)
									&& eOthCurve.getValue().contains(
											pre.suggestedLabelPoint))
								pre.folContCurves.add(othCurveLabel);
						}
					}
					// containment on the new curve
					if (pl.contains(pre.suggestedLabelPoint))
						pre.folContCurves.add(preferredCurveLabel);
					pre = cur;
				}
			}

			// points in the new curve tested against the containment in the ecc
			// pre-existent curve
			if (!pl.incidentPointRefs.isEmpty()) {
				Iterator<IncidentPointRef> it = pl.incidentPointRefs.keySet()
						.iterator();
				for (IncidentPointRef first = it.next(), pre = first, cur; first != null; pre = cur) {
					if (it.hasNext())
						cur = it.next();
					else {
						cur = first;
						first = null;
					}
					pre.suggestedLabelPoint = GeomUtils.meanPoint(pl, pre.cRef,
							pre.dis, cur.cRef, cur.dis);
					// containment on the new curve
					if (ipp.contains(pre.suggestedLabelPoint))
						pre.folContCurves.add(ippLbl);
					pre = cur;
				}
			}
		}
		// calculate the suggestedLabelPoints for the first curve
		if (this.curves.isEmpty() && !pl.incidentPointRefs.isEmpty()) {
			Iterator<IncidentPointRef> it = pl.incidentPointRefs.keySet().iterator();
			for (IncidentPointRef first = it.next(), pre = first, cur; first != null; pre = cur) {
				if (it.hasNext())
					cur = it.next();
				else {
					cur = first;
					first = null;
				}
				pre.suggestedLabelPoint = GeomUtils.meanPoint(pl, pre.cRef, pre.dis, cur.cRef, cur.dis);
			}
		}
		this.curves.put(preferredCurveLabel, pl);
		return preferredCurveLabel;
	}

	public IncidentPointsPolygon removeCurve(Character curveLabel, boolean redo) {
		IncidentPointsPolygon removedCurve = this.curves.remove(curveLabel);
		if(removedCurve != null) {
			if(redo) {
				ArrayList<Entry<Character, IncidentPointsPolygon>> oldCurves = new ArrayList<Entry<Character, IncidentPointsPolygon>>(this.curves.entrySet());
				this.clear();
				for(Entry<Character, IncidentPointsPolygon> e : oldCurves) addCurve(e.getKey(), e.getValue());
			} else {
				this.zones = null;
				this.unusedCurveLabels.add(curveLabel);
				for(Integer l : removedCurve.incidentPointRefs.values()) {
					IncidentPoint ip = incidentPoints.get(l);
					if(!ip.incidentCurves.remove(curveLabel)) throw new RuntimeException("EulerCode: incoerent state 1.");
					if(ip.incidentCurves.size() <= 1) {
						if(this.incidentPoints.remove(l) == null) throw new RuntimeException("EulerCode: incoerent state 2.");
						this.unusedIncidentPointLabels.addFirst(l);
						if(ip.incidentCurves.isEmpty()) {
							System.out.println("EulerCode: incoerent state 3.");
						} else {
							Character cl = ip.incidentCurves.iterator().next();
							IncidentPointsPolygon curCurve = curves.get(cl);
							IncidentPointRef p = curCurve.incidentPointRefs.getKey(l);
							IncidentPointRef before = curCurve.incidentPointRefs.previousKey(p);
							if(before == null) before = curCurve.incidentPointRefs.lastKey();
							IncidentPointRef after = curCurve.incidentPointRefs.nextKey(p);
							if(after == null) after = curCurve.incidentPointRefs.firstKey();
							curCurve.incidentPointRefs.removeValue(l);
							if(before != null && after != null) before.suggestedLabelPoint = GeomUtils.meanPoint(curCurve, before.cRef, before.dis, after.cRef, after.dis);
						}
					}
				}
				// To update the state in a more efficient way a collection that link curves and CONTAINED segments is needed
				for(IncidentPointsPolygon curve : curves.values()) {
					curve.firstPointContCurves.remove(curveLabel);
					for(IncidentPointRef ipr : curve.incidentPointRefs.keySet()) {
						ipr.folContCurves.remove(curveLabel);
					}
				}
			}
		}
		return removedCurve;
	}
	
	public static class AreaWL extends Area {
		private List<Polygon> outPolys = new ArrayList<Polygon>();
		private List<Polygon> intPolys = new ArrayList<Polygon>();

		private AreaWL(Shape s) {
			super(s);
		}

		public List<Polygon> getOutPolys() {
			return Collections.unmodifiableList(outPolys);
		}

		public List<Polygon> getIntPolys() {
			return Collections.unmodifiableList(intPolys);
		}
	}
	
	public List<Zone> getZones() {
		if(zones == null) {
			class SegmentWithPolygon extends Segment {
				final Polygon poly;
				final int x1, y1, x2, y2, startIndex, endIndex;
				public SegmentWithPolygon(int p1, int p2, char curve, Set<Character> contCurves, Polygon poly, int start, Point2D.Double startPoint, int end, Point2D.Double endPoint) {
					super(p1, p2, curve, contCurves);
					this.poly = poly;
					this.startIndex = start;
					this.x1 = (int) Math.round(startPoint.x);
					this.y1 = (int) Math.round(startPoint.y);
					this.endIndex = end;
					this.x2 = (int) Math.round(endPoint.x);
					this.y2 = (int) Math.round(endPoint.y);
				}
			}
			
			int maxLbl = unusedIncidentPointLabels.getLast();
			HashMap<String, LinkedList<Segment>> segmentsGroups = new HashMap<String, LinkedList<Segment>>();
			// ?? curve contains a list of consecutive points for each curve
			for(Entry<Character, IncidentPointsPolygon> ec : curves.entrySet()) {
				char cL = ec.getKey();
				IncidentPointsPolygon cIPP = ec.getValue();
				Set<Entry<IncidentPointRef, Integer>> curveIPR = cIPP.incidentPointRefs.entrySet();
				Iterator<Entry<IncidentPointRef, Integer>> it = curveIPR.iterator();
				for(Entry<IncidentPointRef, Integer> dummy = new AbstractMap.SimpleImmutableEntry<IncidentPointRef, Integer>(null, null),
						first = it.hasNext() ? it.next() : dummy, pre = first, cur; first != null; pre = cur) {
					SegmentWithPolygon seg;
					if(first == dummy) {
						first = null;
						cur = null;
						seg = new SegmentWithPolygon(maxLbl, maxLbl, cL, cIPP.firstPointContCurves, cIPP, 0, new Point2D.Double(cIPP.xpoints[0], cIPP.ypoints[0]), cIPP.npoints - 1, new Point2D.Double(cIPP.xpoints[cIPP.npoints - 1], cIPP.ypoints[cIPP.npoints - 1]));
						maxLbl++;
					} else {
						if(it.hasNext()) cur = it.next();
						else { cur = first; first = null; }
						Integer preP = pre.getValue();
						IncidentPointRef preIPR = pre.getKey();
						Integer curP = cur.getValue();
						IncidentPointRef curIPR = cur.getKey();
						seg = new SegmentWithPolygon(preP, curP,
								cL, preIPR.folContCurves, cIPP,
								(preIPR.cRef + 1) % cIPP.npoints,
								incidentPoints.get(preP),
								(curIPR.cRef + 1) % cIPP.npoints,
								incidentPoints.get(curP));
					}
					LinkedList<Segment> lsp = segmentsGroups.get(seg.label);
					if(lsp == null) {
						lsp = new LinkedList<Segment>();
						lsp.add(seg);
						segmentsGroups.put(seg.label, lsp);
					} else lsp.add(seg);
					lsp = segmentsGroups.get(seg.contCurves);
					if(lsp == null) {
						lsp = new LinkedList<Segment>();
						lsp.add(seg);
						segmentsGroups.put(seg.contCurves, lsp);
					} else lsp.add(seg);
				}
			}
			
			List<Zone> zoneList = toZones(segmentsGroups);
			
			for(Zone zone : zoneList) {
				boolean sub = zone.outlines.isEmpty();
				for(Iterator<List<Segment>> it1 = (sub ? zone.intlines : zone.outlines).iterator(); it1.hasNext();) {
					List<Segment> segments = it1.next();
					Polygon outline = new Polygon();
					int lastPoint = 0;
					for(Iterator<Segment> it2 = segments.iterator(); it2.hasNext();) {
						SegmentWithPolygon seg = (SegmentWithPolygon)it2.next();
						if(outline.npoints != 0 && lastPoint == seg.p2) {
							outline.addPoint(seg.x2, seg.y2);
							int dir = seg.poly.npoints - 1;
							int startIndex = (seg.endIndex + dir) % seg.poly.npoints;
							int endIndex = (seg.startIndex + dir) % seg.poly.npoints;
							if(startIndex == endIndex) {
								double x = seg.poly.xpoints[startIndex], y = seg.poly.ypoints[startIndex];
								if(Point2D.distance(seg.x1, seg.y1, x, y) > Point2D.distance(seg.x2, seg.y2, x, y)) {
									outline.addPoint(seg.poly.xpoints[startIndex], seg.poly.ypoints[startIndex]);
									startIndex = (startIndex + dir) % seg.poly.npoints;
								}
							}
							for(int i = startIndex; i != endIndex; i = (i + dir) % seg.poly.npoints) {
								outline.addPoint(seg.poly.xpoints[i], seg.poly.ypoints[i]);
							}
							lastPoint = seg.p1;
						} else {
							outline.addPoint(seg.x1, seg.y1);
							int startIndex = seg.startIndex;
							if(startIndex == seg.endIndex) {
								double x = seg.poly.xpoints[startIndex], y = seg.poly.ypoints[startIndex];
								if(Point2D.distance(seg.x1, seg.y1, x, y) < Point2D.distance(seg.x2, seg.y2, x, y)) {
									outline.addPoint(seg.poly.xpoints[startIndex], seg.poly.ypoints[startIndex]);
									startIndex = (startIndex + 1) % seg.poly.npoints;
								}
							}
							for(int i = startIndex; i != seg.endIndex; i = (i + 1) % seg.poly.npoints) {
								outline.addPoint(seg.poly.xpoints[i], seg.poly.ypoints[i]);
							}
							lastPoint = seg.p2;
						}
					}
					if(segments.size() == 1) {
						SegmentWithPolygon seg = (SegmentWithPolygon)segments.get(0);
						outline.addPoint(seg.x2, seg.y2);
					}
					if(sub) {
						if(zone.area == null) zone.area = new AreaWL(new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE));
						zone.area.subtract(new Area(outline));
						zone.area.intPolys.add(outline);
					} else {
						if(zone.area == null) zone.area = new AreaWL(outline);
						else zone.area.add(new Area(outline));
						zone.area.outPolys.add(outline);
						if(!it1.hasNext()) {
							it1 = zone.intlines.iterator();
							sub = true;
						}
					}
				}
			}
			zones = Collections.unmodifiableList(zoneList);
		}
		return zones;
	}
	
	public static List<Zone> computeZones(Collection<? extends Collection<Segment>> segments) {
		HashMap<String, LinkedList<Segment>> segmentsGroups = new HashMap<String, LinkedList<Segment>>();
		for(Collection<Segment> cSegments : segments) {
			for(Segment seg : cSegments) {
				LinkedList<Segment> lsp = segmentsGroups.get(seg.label);
				if(lsp == null) {
					lsp = new LinkedList<Segment>();
					lsp.add(seg);
					segmentsGroups.put(seg.label, lsp);
				} else lsp.add(seg);
				lsp = segmentsGroups.get(seg.contCurves);
				if(lsp == null) {
					lsp = new LinkedList<Segment>();
					lsp.add(seg);
					segmentsGroups.put(seg.contCurves, lsp);
				} else lsp.add(seg);
			}
		}
		return toZones(segmentsGroups);
	}
	
	private static List<Zone> toZones(Map<String, LinkedList<Segment>> segmentsGroups) {
		ArrayList<Zone> zones = new ArrayList<Zone>(segmentsGroups.size());
		for(Entry<String, LinkedList<Segment>> e : segmentsGroups.entrySet()) {
			String label = e.getKey();
			int labelLen = label.length();
			LinkedList<Segment> group = e.getValue();
			List<List<Segment>> outlines = new ArrayList<List<Segment>>();
			List<List<Segment>> intlines = new ArrayList<List<Segment>>();
			Zone zone = new Zone(label, Collections.unmodifiableList(outlines), Collections.unmodifiableList(intlines));
			List<Segment> segments = new ArrayList<Segment>();
			boolean outline = false;
			int lastPoint = 0;
			while(!group.isEmpty()) {
				if(segments.isEmpty()) {
					Segment first = group.removeFirst();
					if(first.label.length() == labelLen) outline = true;
					segments.add(first);
					lastPoint = first.p2; 
				} else {
					boolean notFound = true;
					for(Iterator<Segment> it = group.iterator(); it.hasNext();) {
						Segment seg = it.next();
						if(lastPoint == seg.p1) {
							if(seg.label.length() == labelLen) outline = true;
							segments.add(seg);
							lastPoint = seg.p2;
							it.remove();
							notFound = false;
							break;
						} else if(lastPoint == seg.p2) {
							if(seg.label.length() == labelLen) outline = true;
							segments.add(seg);
							lastPoint = seg.p1;
							it.remove();
							notFound = false;
							break;
						}
					}
					if(notFound) throw new RuntimeException("getZones: infinite loop.");
				}
				if(lastPoint == segments.get(0).p1) {
					if(outline) {
						outline = false;
						Entry<Zone, Integer> ze = new AbstractMap.SimpleImmutableEntry<Zone, Integer>(zone, outlines.size());
						for(Segment s : segments) s.addZone(label, ze);
						outlines.add(Collections.unmodifiableList(segments));
						segments = new ArrayList<Segment>();
					} else {
						Entry<Zone, Integer> ze = new AbstractMap.SimpleImmutableEntry<Zone, Integer>(zone, -1 - intlines.size());
						for(Segment s : segments) s.addZone(label, ze);
						intlines.add(Collections.unmodifiableList(segments));
						segments = new ArrayList<Segment>();
					}
				}
			}
			if(!segments.isEmpty()) throw new RuntimeException("getZones: missed segments.");
			zones.add(zone);
		}
		Collections.sort(zones, new Comparator<Zone>(){
			@Override
			public int compare(Zone o1, Zone o2) {
				return o1.label.compareTo(o2.label);
			}
		});
		return zones;
	}
	
	public String getCode(boolean infix, boolean closedCircle, boolean html) {
		if(curves.isEmpty()) return null;
		StringBuffer out = new StringBuffer();
		if(html) out.append("<html>\n<body>\n");
		int maxLbl = unusedIncidentPointLabels.getLast();
		for(Entry<Character, IncidentPointsPolygon> ec : curves.entrySet()) {
			out.append(ec.getKey() + ": ");
			IncidentPointsPolygon c = ec.getValue();
			Set<Entry<IncidentPointRef, Integer>> curveIPR = c.incidentPointRefs.entrySet();
			if(curveIPR.isEmpty()) {
				String pointNumber = "" + maxLbl++;
				if(closedCircle && !infix) out.append(pointNumber + (html ? "<sub> </sub>" : " "));
				out.append(pointNumber);
				out.append(html ? "<sub>" : "_");
				if(c.firstPointContCurves.isEmpty()) {
					out.append(html ? "&empty;" : "0");
				} else {
					for(Character inc : c.firstPointContCurves) out.append(inc);
				}
				out.append(html ? "</sub>" : " ");
				if(closedCircle && infix) out.append(pointNumber);
			} else {
				Iterator<Entry<IncidentPointRef, Integer>> it = curveIPR.iterator();
				if(infix) {
					while(it.hasNext()) {
						Entry<IncidentPointRef, Integer> cur = it.next();
						out.append(cur.getValue());
						IncidentPointRef ipr = cur.getKey();
						out.append(html ? "<sub>" : "_");
						if(ipr.folContCurves.isEmpty()) {
							out.append(html ? "&empty;" : "0");
						} else {
							for(Character inc : ipr.folContCurves) out.append(inc);
						}
						out.append(html ? "</sub>" : " ");
					}
					if(closedCircle) out.append(curveIPR.iterator().next().getValue());
				} else {
					Entry<IncidentPointRef, Integer> pre = it.next();
					Entry<IncidentPointRef, Integer> cur = pre;
					StringBuffer start = new StringBuffer();
					start.append(cur.getValue());
					StringBuffer cont = new StringBuffer();
					while(it.hasNext()) {
						cur = it.next();
						cont.append(cur.getValue());
						cont.append(html ? "<sub>" : "_");
						if(pre.getKey().folContCurves.isEmpty()) {
							cont.append(html ? "&empty;" : "0");
						} else {
							for(Character inc : pre.getKey().folContCurves) cont.append(inc);
						}
						cont.append(html ? "</sub>" : " ");
						pre = cur;
					}
					start.append(html ? "<sub>" : "_");
					if(cur.getKey().folContCurves.isEmpty()) {
						start.append(html ? "&empty;" : "0");
					} else {
						for(Character inc : cur.getKey().folContCurves) start.append(inc);
					}
					start.append(html ? "</sub>" : " ");
					if(closedCircle) {
						out.append(start.charAt(0));
						out.append(html ? "<sub> </sub>" : " ");
						out.append(cont);
						out.append(start);	
					} else {
						out.append(start);
						if(!html) out.append(' ');
						out.append(cont);
					}
				}
			}
			if(out.charAt(out.length() - 1) == ' ') out.setLength(out.length() - 1);
			out.append(html ? "<br/>\n" : "\n");
		}
		if(html) {
			out.setLength(out.length() - 6);
			out.append("\n</body>\n</html>");
		} else {
			out.setLength(out.length() - 1);
		}
		return out.toString().replace("-", "");
	}
	
	/**
	 * An alternative code (unused)
	 */
	public String getCodeAlt(boolean html) {
		if(curves.isEmpty()) return null;
		
		class Pair {
			final Collection<Integer> points;
			final TreeSet<Integer> contained;
			public Pair(Collection<Integer> points) {
				this.points = points;
				this.contained = new TreeSet<Integer>();
			}
		}
		LinkedHashMap<Character, Pair> curvesPoints = new LinkedHashMap<Character, Pair>();
		Set<Entry<Character, IncidentPointsPolygon>> curveSet = curves.entrySet();
		for(Entry<Character, IncidentPointsPolygon> e : curveSet) {
			curvesPoints.put(e.getKey(), new Pair(e.getValue().incidentPointRefs.values()));
		}
		int maxLbl = unusedIncidentPointLabels.getLast();
		for(Entry<Character, IncidentPointsPolygon> ec : curveSet) {
			IncidentPointsPolygon ipp = ec.getValue();
			if(ipp.incidentPointRefs.isEmpty()) {
				Integer fp = maxLbl++;
				for(Character c : ipp.firstPointContCurves) curvesPoints.get(c).contained.add(fp);
			} else {
				for(Entry<IncidentPointRef, Integer> ep : ipp.incidentPointRefs.entrySet()) {
					Integer p = ep.getValue();
					for(Character c : ep.getKey().folContCurves) curvesPoints.get(c).contained.add(p);
				}	
			}
		}
				
		StringBuffer out = new StringBuffer();
		if(html) out.append("<html>\n<body>\n");
		for(Entry<Character, Pair> e : curvesPoints.entrySet()) {
			out.append(e.getKey() + ":");
			Pair c = e.getValue();
			for(Integer p : c.points) {
				out.append(" " + p);
				c.contained.remove(p);
			}
			if(!c.contained.isEmpty()) {
				out.append(" {");
				for(Integer p : c.contained) out.append(p + " ");
				out.setCharAt(out.length() - 1, '}');
			}
			out.append(html ? "<br/>\n" : "\n");
		}
		if(html) {
			out.setLength(out.length() - 6);
			out.append("\n</body>\n</html>");
		} else {
			out.setLength(out.length() - 1);
		}
		return out.toString().replace("-", "");
	}
	
	private static class IntPair {
		int first;
		int second;

		public IntPair(int first, int second) {
			this.first = first;
			this.second = second;
		}
	}

	private static SegmentCode[] segmentsToSegmentCodes(List<Segment> segments,
			GaussCodeRBC[] gcRBCs, List<SegmentCode[]>[] regionBoundaryCodes,
			Map<Character, IntPair> curveGcsIndMap) {
		HashSet<String> extSC = new HashSet<String>();
		for (Segment seg : segments) {
			IntPair gcCrv = curveGcsIndMap.get(seg.curve);
			Symbol[] curve = gcRBCs[gcCrv.first].getGaussCode()[gcCrv.second];

			String p1 = String.valueOf(Math.abs(seg.p1));
			String p2 = String.valueOf(Math.abs(seg.p2));
			for (int j = 0; j < curve.length; j++) {
				Symbol s1 = curve[j];
				Symbol s2 = curve[(j + 1) % curve.length];
				if (p1.equals(s1.getLabel()) && p2.equals(s2.getLabel())) {
					if (extSC.add(p1 + s1.getSign() + p2 + s2.getSign()))
						break;
				}
			}
		}

		ArrayList<SegmentCode[]> bcCs = new ArrayList<SegmentCode[]>();
		int len = 0;
		List<SegmentCode[]> curRBC = regionBoundaryCodes[curveGcsIndMap
				.get(segments.get(0).curve).first];
		for (SegmentCode[] bc : curRBC) {
			if (bc.length >= len) {
				boolean match = true;
				for (SegmentCode sc : bc) {
					Symbol s1 = sc.getFirstSymbol();
					Symbol s2 = sc.getSecondSymbol();
					if (!extSC.contains((s1.getLabel() + s1.getSign()
							+ s2.getLabel() + s2.getSign()))) {
						match = false;
						break;
					}
				}
				if (match) {
					if (bc.length > len) {
						len = bc.length;
						bcCs.clear();
					}
					bcCs.add(bc);
				}
			}
		}
		return bcCs.get(0);
	}
	
	public EulerCodeRBC getEulerCodeRBC() {
		// groups the curves that share an intersection point
		UnionFind<Character> cgrp = new UnionFind<Character>(curves.keySet());
		for (IncidentPoint ip : incidentPoints.values()) {
			Character first = null;
			for (Character cur : ip.incidentCurves) {
				if (first == null)
					first = cur;
				else
					cgrp.union(first, cur);
			}
		}

		// general gauss code
		GaussCodeRBC allCrvGcRBCs = getGaussCodeRBC();
		char[] allCrvLbs = allCrvGcRBCs.getCurveLabels();
		Symbol[][] allCrvGcs = allCrvGcRBCs.getGaussCode();

		// creates a list for each curve group
		// each list contains the indices of the curves of the group
		LinkedHashMap<Character, ArrayList<Integer>> subsets = new LinkedHashMap<Character, ArrayList<Integer>>();
		for (int i = 0; i < allCrvLbs.length; i++) {
			Character reprElem = cgrp.find(allCrvLbs[i]);
			ArrayList<Integer> list = subsets.get(reprElem);
			if (list == null) {
				list = new ArrayList<Integer>();
				subsets.put(reprElem, list);
			}
			list.add(i);
		}

		// saves GaussCodeRBC and RegionBoundaryCode for each curve group
		GaussCodeRBC[] divGcRBCs = new GaussCodeRBC[subsets.size()];
		@SuppressWarnings("unchecked")
		List<SegmentCode[]>[] divRgBnCd = new List[divGcRBCs.length];
		LinkedHashMap<Character, IntPair> allCrvGcsMap = new LinkedHashMap<Character, IntPair>();
		int nAllCrv = 0;
		for (ArrayList<Integer> sub : subsets.values()) {
			Symbol[][] gc = new Symbol[sub.size()][];
			char[] crvLbls = new char[gc.length];
			for (int i = 0; i < gc.length; i++) {
				int n = sub.get(i);
				gc[i] = allCrvGcs[n];
				char lb = allCrvLbs[n];
				crvLbls[i] = lb;
				allCrvGcsMap.put(lb, new IntPair(nAllCrv, i));
			}
			divGcRBCs[nAllCrv] = new GaussCodeRBC(crvLbls, gc);
			divRgBnCd[nAllCrv] = gc.length == 1 ? Arrays
					.asList(new SegmentCode[][] { { new SegmentCode(gc[0][0],
							gc[0][0], '-') } }) // only within
					: divGcRBCs[nAllCrv].getRegionBoundaryCode();
			nAllCrv++;
		}

		getZones();

		// for each curve group search the region in which it is located
		SegmentCode[][] withins = new SegmentCode[divGcRBCs.length][];
		SegmentCode[][] outers = new SegmentCode[divGcRBCs.length][];
		for (int i = 0; i < divGcRBCs.length; i++) {
			char[] curCrvLbs = divGcRBCs[i].getCurveLabels();
			Polygon fc = curves.get(curCrvLbs[0]);

			// search the region with the longest label that contains the
			// current curves
			Zone cZone = null;
			List<Segment> cOutline = null;
			zoneFor: for (Zone zone : zones) {
				// if the zone is associated with one of the current curves skip
				// to the next zone
				for (int j = 0; j < curCrvLbs.length; j++)
					if (zone.label.contains(String.valueOf(curCrvLbs[j])))
						continue zoneFor;

				if (cZone == null || cZone.label.length() < zone.label.length()) {
					if (zone.label.isEmpty())
						cZone = zone; // everything is contained in the ext zone
					else
						for (int j = 0; j < zone.area.outPolys.size(); j++) {
							if (zone.area.outPolys.get(j).contains(
									fc.xpoints[0], fc.ypoints[0])) {
								cZone = zone;
								cOutline = zone.outlines.get(j);
								break;
							}
						}
				}
			}
			if (cOutline != null)
				withins[i] = segmentsToSegmentCodes(cOutline, divGcRBCs,
						divRgBnCd, allCrvGcsMap);

			List<Segment> bIntline = null;
			Rectangle bBound = null;
			for (int j = 0; j < cZone.intlines.size(); j++) {
				List<Segment> intLine = cZone.intlines.get(j);
				Rectangle intBound = cZone.area.intPolys.get(j).getBounds();
				if (bBound == null || !bBound.contains(intBound)) {
					for (Segment seg : intLine) {
						for (int k = 0; k < curCrvLbs.length; k++) {
							if (seg.curve == curCrvLbs[k]) {
								bIntline = intLine;
								bBound = intBound;
								break;
							}
						}
					}
				}
			}
			outers[i] = segmentsToSegmentCodes(bIntline, divGcRBCs, divRgBnCd,
					allCrvGcsMap);
			if (outers[i].length == 1)
				outers[i] = new SegmentCode[] { new SegmentCode(
						outers[i][0].getFirstSymbol(),
						outers[i][0].getSecondSymbol(), '+') };
		}
		return new EulerCodeRBC(divGcRBCs, withins, outers);
	}
	
	public String getKnotCode(boolean closedCircle, boolean html) {
		if (curves.isEmpty())
			return null;
		GaussCodeRBC gaussCodeRBC = getGaussCodeRBC();
		Symbol[][] gaussCode = gaussCodeRBC.getGaussCode();
		if (gaussCode == null)
			return null;

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < gaussCode.length; i++) {
			out.append(',');
			Symbol[] syms = gaussCode[i];
			if (syms != null) {
				for (int j = 0, n = closedCircle ? syms.length + 1
						: syms.length; j < n; j++) {
					USymbol sym = (USymbol) syms[j % syms.length];
					out.append((sym.isUnder() ? " U" : " O") + sym.getLabel()
							+ sym.getSign());
				}
			}
		}
		String code = out.substring(Math.min(out.length(), 2));
		return (html ? "<html>\n<body>\n"
				+ code.replace("\n", "<br/>\n").replaceAll("([OU])(\\d+[+-])",
						"$1<sub>$2</sub>") + "\n</body>\n</html>" : code);
	}
	
	public String getGaussCode(boolean closedCircle, boolean html) {
		if (curves.isEmpty())
			return null;
		GaussCodeRBC gaussCodeRBC = getGaussCodeRBC();
		Symbol[][] gaussCode = gaussCodeRBC.getGaussCode();
		if (gaussCode == null)
			return null;

		String code;
		if (closedCircle) {
			StringBuilder out = new StringBuilder();
			char[] curveLabels = gaussCodeRBC.getCurveLabels();
			for (int i = 0; i < gaussCode.length; i++) {
				out.append(curveLabels[i]);
				out.append(':');
				Symbol[] syms = gaussCode[i];
				if (syms != null) {
					for (int j = 0; j <= syms.length; j++) {
						Symbol sym = syms[j % syms.length];
						out.append(" " + sym.getLabel() + sym.getSign());
					}
				}
				out.append('\n');
			}
			out.setLength(Math.max(0, out.length() - 1));
			code = out.toString();
		} else {
			code = gaussCodeRBC.getGaussCodeString();
		}
		return (html ? "<html>\n<body>\n" + code.replace("\n", "<br/>\n")
				+ "\n</body>\n</html>" : code);
	}

	public String getEulerCode(boolean closedCircle, boolean html) {
		if (curves.isEmpty())
			return null;
		String code = getEulerCodeRBC().getEulerCodeRBCString();
		if (closedCircle)
			code = code.replaceAll("(\\n\\w:)( \\d+[+-])(.*)", "$1$2$3$2");
		return html ? "<html>\n<body>\n"
				+ code.replace("\n", "<br/>\n")
						.replace("within: 0", "within: &empty;")
						.replaceAll("d_(\\d+)", "<b>d<sub>$1</sub></b>")
				+ "\n</body>\n</html>" : code;
	}
	
	/**
	 * Sometimes gives wrong output
	 */
	private String getEulerCodeOldMethod(boolean closedCircle, boolean html) {
		if (curves.isEmpty())
			return null;

		// containment grouping
		Comparator<TreeSet<Character>> cmp = new Comparator<TreeSet<Character>>() {
			@Override
			public int compare(TreeSet<Character> o1, TreeSet<Character> o2) {
				int c = Integer.compare(o1.size(), o2.size());
				return c == 0 ? o1.toString().compareTo(o2.toString()) : c;
			}
		};
		TreeMap<TreeSet<Character>, TreeMap<TreeSet<Character>, ArrayList<Integer>>> cGroups = new TreeMap<TreeSet<Character>, TreeMap<TreeSet<Character>, ArrayList<Integer>>>(
				cmp);
		Integer cn = 0;
		for (Entry<Character, IncidentPointsPolygon> e1 : curves.entrySet()) {
			Character c = e1.getKey();
			IncidentPointsPolygon ipp = e1.getValue();
			TreeSet<Character> crvIntSet = new TreeSet<Character>(
					ipp.firstPointContCurves);
			TreeSet<Character> crvOthSet = new TreeSet<Character>(
					ipp.firstPointContCurves);
			crvOthSet.add(c);
			for (IncidentPointRef ipr : ipp.incidentPointRefs.keySet()) {
				crvIntSet.retainAll(ipr.folContCurves);
				crvOthSet.addAll(ipr.folContCurves);
			}
			crvOthSet.removeAll(crvIntSet);

			TreeMap<TreeSet<Character>, ArrayList<Integer>> cList = cGroups
					.get(crvIntSet);
			ArrayList<Integer> cnL = new ArrayList<Integer>();
			cnL.add(cn);
			if (cList == null) {
				cList = new TreeMap<TreeSet<Character>, ArrayList<Integer>>(cmp);
			} else {
				for (Iterator<Entry<TreeSet<Character>, ArrayList<Integer>>> it = cList
						.entrySet().iterator(); it.hasNext();) {
					Entry<TreeSet<Character>, ArrayList<Integer>> e2 = it
							.next();
					TreeSet<Character> exSet = e2.getKey();
					for (Character cOth : crvOthSet) {
						if (exSet.contains(cOth)) {
							it.remove();
							crvOthSet.addAll(exSet);
							cnL.addAll(e2.getValue());
							break;
						}
					}
				}
			}
			cList.put(crvOthSet, cnL);
			cGroups.put(crvIntSet, cList);
			cn++;
		}

		GaussCodeRBC gaussCodeRBC = getGaussCodeRBC();
		Symbol[][] gaussCode = gaussCodeRBC.getGaussCode();
		if (gaussCode == null)
			return null;

		if (zones == null)
			getZones();
		// code from getGaussZonesCodeUsingStaticCodeMethod
		Map<String, String> signedCodeLabels = new HashMap<String, String>();
		for (int i = 0; i < gaussCode.length; i++) {
			for (Symbol s : gaussCode[i]) {
				String key = gaussCodeRBC.getCurveLabels()[i] + s.getLabel();
				signedCodeLabels.put(key, s.getLabel() + s.getSign());
			}
		}
		StringBuilder out = new StringBuilder();
		char[] curveLabels = gaussCodeRBC.getCurveLabels();
		int dn = 1;
		for (Entry<TreeSet<Character>, TreeMap<TreeSet<Character>, ArrayList<Integer>>> e1 : cGroups
				.entrySet()) {
			String within = "";
			for (Character cl : e1.getKey())
				within += cl;
			for (Entry<TreeSet<Character>, ArrayList<Integer>> e2 : e1
					.getValue().entrySet()) 
			{
				TreeSet<Character> cCurves = e2.getKey();
				LinkedList<Entry<List<Segment>, Polygon>> extZones = new LinkedList<Entry<List<Segment>, Polygon>>();
				String rgb = ""; 
				for (Zone zone : zones) {
					if (zone.label.equals(within)) {
						for (int i = 0, end = zone.outlines.size(); i < end; i++) 
						{ 
							rgb += "{";
							//if(zone.outlines.size()>1)
						 		// find which region applies
							List<Segment> zoneOutLines = zone.outlines.get(i);
							for(int j=0;j<zoneOutLines.size();j++)
							{ 
								Segment s =zoneOutLines.get(j);
								char d = within.contains(s.curve + "") ? '-' : '+';
								
								String s1="";
								String s2="";
								if(s.p1<0)
								{
									String t=signedCodeLabels.get(s.curve+""+(s.p1*-1));
									int n=t.length()-1;
									String t2=t.substring(0, n);
									if(t.charAt(n)=='+') 
									{ 
										s1= t2+"-";
									}
									else s1= t2+"+";							
								}
								else s1=signedCodeLabels.get(s.curve+""+s.p1);
								if(s.p2<0)
								{
									String t=signedCodeLabels.get(s.curve+""+(s.p2*-1));
									int n=t.length()-1;
									String t2=t.substring(0, n);
									if(t.charAt(n)=='+') 
									{ 
										s2= t2+"-";
									}
									else s2= t2+"+";			
								}
								else s2=signedCodeLabels.get(s.curve+""+s.p2); 
								rgb+="(" + s1
										+ " " + s2
										+ "," + d + ")"
										+ (j != zoneOutLines.size() - 1 ? "," : "");
								
							}
							rgb += "}";
							rgb+= (i != zone.outlines.size() - 1 ? ";" : ""); 
							
						}
						
						for (int i = 0, end = zone.intlines.size(); i < end; i++) {
							List<Segment> sl = zone.intlines.get(i);							 
							if (cCurves.contains(sl.get(0).curve)) {
								extZones.add(new AbstractMap.SimpleImmutableEntry<List<Segment>, Polygon>(
										sl, zone.area.intPolys.get(i)));
							}
						}
						break;
					}
				}
				out.append((html ? "<b>d<sub>" + dn++ + "</sub></b>" : "d_"
						+ dn++)
						+ ": "
						+ cCurves
						+ "; within: "
						+ (within.isEmpty() ? (html ? "&empty;" : "0") : rgb));
				// remove inside areas
				for (Iterator<Entry<List<Segment>, Polygon>> it = extZones
						.iterator(); it.hasNext();) {
					Entry<List<Segment>, Polygon> cInl = it.next();
					for (Entry<List<Segment>, Polygon> oInl : extZones) {
						if (cInl != oInl) {
							if (oInl.getValue().contains(
									cInl.getValue().xpoints[0],
									cInl.getValue().ypoints[0])) {
								it.remove();
								break;
							}
						}
					}
				}

				out.append("; outer: ");
				// code from getGaussZonesCodeUsingStaticCodeMethod
				for (Entry<List<Segment>, Polygon> lsa : extZones) {
					List<Segment> ls = lsa.getKey();
					out.append("{");
					for (int j = 0; j < ls.size(); j++) {
						Segment s = ls.get(j);
						char d = within.contains(s.curve + "") ? '-' : '+';
						
						String s1="";
						String s2="";
						if(s.p1<0)
						{
							String t=signedCodeLabels.get(s.curve+""+(s.p1*-1));
							int n=t.length()-1;
							String t2=t.substring(0, n);
							if(t.charAt(n)=='+') 
							{ 
								s1= t2+"-";
							}
							else s1= t2+"+";							
						}
						else s1=signedCodeLabels.get(s.curve+""+s.p1);
						if(s.p2<0)
						{
							String t=signedCodeLabels.get(s.curve+""+(s.p2*-1));
							int n=t.length()-1;
							String t2=t.substring(0, n);
							if(t.charAt(n)=='+') 
							{ 
								s2= t2+"-";
							}
							else s2= t2+"+";			
						}
						else s2=signedCodeLabels.get(s.curve+""+s.p2);
						
						out.append("(" + s1
								+ " " + s2
								+ "," + d + ")"
								+ (j != ls.size() - 1 ? "," : ""));
						
						
						/*String s1 = Integer.toString(s.p1);
						String s2 = Integer.toString(s.p2);
						out.append("(" + signedCodeLabels.get(s.curve + s1)
								+ " " + signedCodeLabels.get(s.curve + s2)
								+ "," + d + ")"
								+ (j != ls.size() - 1 ? "," : ""));*/
					}
					out.append("}; ");
				}

				out.setLength(out.length() - 2);
				out.append("\n");
				ArrayList<Integer> cIdxs = e2.getValue();
				Collections.sort(cIdxs);
				for (int i : cIdxs) {
					out.append(curveLabels[i]);
					out.append(':');
					Symbol[] syms = gaussCode[i];
					if (syms != null) {
						if (closedCircle) {
							for (int j = 0; j <= syms.length; j++) {
								Symbol sym = syms[j % syms.length];
								out.append(" " + sym.getLabel() + sym.getSign());
							}
						} else {
							for (int j = 0; j < syms.length; j++) {
								Symbol sym = syms[j];
								out.append(" " + sym.getLabel() + sym.getSign());
							}
						}
					}
					out.append('\n');
				}
				out.append('\n');
			}
		}
		int len = out.length();
		while (len != 0) {
			if (out.charAt(len - 1) == '\n')
				len--;
			else
				break;
		}
		out.setLength(len);
		String code = out.toString();
		return (html ? "<html>\n<body>\n" + code.replace("\n", "<br/>\n")
				+ "\n</body>\n</html>" : code);
	}
	
	public String getZonesCode(boolean infix, boolean closedCircle, boolean html) {
		if(zones == null) getZones();
		StringBuffer out = new StringBuffer();
		if(html) out.append("<html>\n<body>\n");
		for(Zone zone : zones) {
			out.append((zone.label.isEmpty() ? (html ? "&empty;" : "0") : zone.label) + ": ");
			for(int i = 0; i < 2; i++) {
				for(List<Segment> o : i == 0 ? zone.outlines : zone.intlines) {
					for(int j = 0, end = o.size(), pPre = 0, pCur = 0; j < end; j++) {
						Segment s = o.get(j);
						if(j == 0 || pPre == s.p1) {
							pPre = s.p2;
							pCur = s.p1;
						} else if(pPre == s.p2) {
							pPre = s.p1;
							pCur = s.p2;
						}
						else {
							out.append(" ERROR: no ciclic sequence\n");
							break;
						}
						if(j == 0 && !infix && closedCircle) out.append(pPre + (html ? "<sub> </sub>" : " "));
						else out.append((infix ? pCur : pPre) + (html ? "<sub>" : "_") + s.curve + "&#775;" + s.contCurves + (html ? "</sub>" : " "));
					}
					if(closedCircle) {
						Segment fs = o.get(0);
						out.append(infix ? fs.p1 : fs.p2 + (html ? "<sub>" : "_") + fs.curve + "&#775;" + fs.contCurves + (html ? "</sub>" : " "));
					}
					if(html) {
						out.append(", ");
					} else {
						out.setCharAt(out.length() - 1, ',');
						out.append(' ');	
					}
				}
			}
			out.setLength(out.length() - 2);
			out.append(html ? "<br/>\n" : "\n");
		}
		if(html) {
			out.setLength(out.length() - 6);
			out.append("\n</body>\n</html>");
		} else {
			out.setLength(out.length() - 1);
		}
		return out.toString().replace("-", "");
	}
	
	public String getGaussZonesCodeUsingStaticCodeMethod(boolean html,Symbol[][] gaussCode,char [] curveLabels) {
		
		StringBuilder out = new StringBuilder();
		Map<String, String> signedCodeLabels = new HashMap<String, String>();
		TreeMap<String, CharSequence> map = new TreeMap<String, CharSequence>();
		if (html)
			out.append("<html>\n<body>\n");
		
		for(int i=0;i<gaussCode.length;i++)
		{
			for(Symbol s: gaussCode[i])
			{
				String key =curveLabels[i]+s.getLabel(); 
				signedCodeLabels.put(key,s.getLabel()+s.getSign());				
			}
		}
		if(zones == null) getZones(); 
		for(Zone zone : zones) 
		{	
			for(int i = 0; i < 2; i++) 
			for(List<Segment> ls : i == 0 ?zone.outlines : zone.intlines) 
			{
				String zoneName=(zone.label.isEmpty() ? (html ? "&empty;" : "0") :zone.label);
				String regionCode ="  {";
				for(int j=0;j<ls.size();j++)
				{
					Segment s =ls.get(j);
					char d=zoneName.contains(s.curve+"")?'-':'+';
					//String s1=Integer.toString((s.p1<0)?s.p1*-1:s.p1);
					//String s2=Integer.toString((s.p2<0)?s.p2*-1:s.p2);
					String s1="";
					String s2="";
					if(s.p1<0)
					{
						String t=signedCodeLabels.get(s.curve+""+(s.p1*-1));
						int n=t.length()-1;
						String t2=t.substring(0, n);
						if(t.charAt(n)=='+') 
						{ 
							s1= t2+"-";
						}
						else s1= t2+"+";							
					}
					else s1=signedCodeLabels.get(s.curve+""+s.p1);
					if(s.p2<0)
					{
						String t=signedCodeLabels.get(s.curve+""+(s.p2*-1));
						int n=t.length()-1;
						String t2=t.substring(0, n);
						if(t.charAt(n)=='+') 
						{ 
							s2= t2+"-";
						}
						else s2= t2+"+";			
					}
					else s2=signedCodeLabels.get(s.curve+""+s.p2);
					//regionCode+="("+signedCodeLabels.get(s.curve+s1)+" "+signedCodeLabels.get(s.curve+s2)+","+d+")"+(j!= ls.size()-1?",":"");
					regionCode+="("+s1+" "+s2+","+d+")"+(j!= ls.size()-1?",":"");
				}
				regionCode+="}";
				CharSequence val = map.get(zoneName);
				if (val == null)
					map.put(zoneName, regionCode);
				else
					map.put(zoneName, val + ";" + regionCode);
			}
		}
		for (Entry<String, CharSequence> e : map.entrySet()) {
				out.append(e.getKey() + ": " + e.getValue()+(html ? "<br />\n" : "\n"));
			
		}
		if (html) {
			out.setLength(out.length() - 6);
			out.append("\n</body>\n</html>");
		} else {
			out.setLength(out.length() - 1);
		}
			return out.toString();
	}

	public String getGaussZonesCode(boolean html) {
		if (curves.isEmpty())
			return null;
		GaussCodeRBC gaussCodeRBC = getGaussCodeRBC();
		List<SegmentCode[]> rbc = gaussCodeRBC.getRegionBoundaryCode();
		if (rbc == null)
			return null;
		String[] zoneCodes = gaussCodeRBC.getRegionBoundaryCodeString().split(
				"\n");

		StringBuilder out = new StringBuilder();
		if (html)
			out.append("<html>\n<body>\n");

		char[] curvLabels = gaussCodeRBC.getCurveLabels();
		Symbol[][] gaussCode = gaussCodeRBC.getGaussCode();

		List<List<Segment>> z0intlines = getZones().get(0).intlines;
		ArrayList<SegmentCode[]> bcCs = new ArrayList<SegmentCode[]>();
		for (int i = 0, end = z0intlines.size(); i <= end && bcCs.isEmpty(); i++) {
			List<Segment> extSegments;
			if (i == end) {
				extSegments = new ArrayList<Segment>();
				for (List<Segment> ls : z0intlines)
					extSegments.addAll(ls);
			} else
				extSegments = z0intlines.get(i);
			HashSet<String> extSC = new HashSet<String>();
			for (Segment seg : extSegments) {
				Symbol[] curve = null;
				for (int j = 0; j < curvLabels.length; j++) {
					if (seg.curve == curvLabels[j]) {
						curve = gaussCode[j];
						break;
					}
				}
				String p1 = String.valueOf(Math.abs(seg.p1));
				String p2 = String.valueOf(Math.abs(seg.p2));
				for (int j = 0; j < curve.length; j++) {
					Symbol s1 = curve[j];
					Symbol s2 = curve[(j + 1) % curve.length];
					if (p1.equals(s1.getLabel()) && p2.equals(s2.getLabel())) {
						if (extSC.add(p1 + s1.getSign() + p2 + s2.getSign()))
							break;
					}
				}
			}

			int len = 0;
			for (SegmentCode[] bc : rbc) {
				if (bc.length >= len) {
					boolean match = true;
					for (SegmentCode sc : bc) {
						Symbol s1 = sc.getFirstSymbol();
						Symbol s2 = sc.getSecondSymbol();
						if (!extSC.contains((s1.getLabel() + s1.getSign()
								+ s2.getLabel() + s2.getSign()))) {
							match = false;
							break;
						}
					}
					if (match) {
						if (bc.length > len) {
							len = bc.length;
							bcCs.clear();
						}
						bcCs.add(bc);
					}
				}
			}
		}

		if (bcCs.isEmpty()) {
			for (String z : zoneCodes) {
				out.append(z);
				out.append(html ? "<br />\n" : "\n");
			}
		} else {
			List<List<List<String>>> zones = ZonesSet.computeZonesSet(
					gaussCode, rbc, bcCs.get(0),curvLabels);
			TreeMap<String, CharSequence> map = new TreeMap<String, CharSequence>();
			for (int k = 0; k < zoneCodes.length; k++) {
				TreeSet<Character> zoneCurves = new TreeSet<Character>();
				for (String c : zones.get(k).get(0))
					zoneCurves.add(c.charAt(0));//zoneCurves.add(curvLabels[Integer.parseInt(c.substring(1)) - 1]);
				String zoneName;
				if (zoneCurves.isEmpty())
					zoneName = html ? "&empty;" : "0";
				else {
					zoneName = "";
					for (Character c : zoneCurves)
						zoneName += c;
				}
				CharSequence val = map.get(zoneName);
				if (val == null)
					map.put(zoneName, zoneCodes[k]);
				else
					map.put(zoneName, val + ";" + zoneCodes[k]);
			}
			for (Entry<String, CharSequence> e : map.entrySet()) {
				out.append(e.getKey() + ": " + e.getValue()
						+ (html ? "<br />\n" : "\n"));
			}
		}
		if (html) {
			out.setLength(out.length() - 6);
			out.append("\n</body>\n</html>");
		} else {
			out.setLength(out.length() - 1);
		}
		return out.toString();
	}

	private String getEulerCodeUnused(List<List<Symbol[]>> gaussCodesList,
			List<char[]> CurveLabelsList, boolean html) {
		if (curves.isEmpty())
			return null;
		
		StringBuilder out = new StringBuilder();
		for(int m=0;m<gaussCodesList.size();m++)
		{
			Symbol[][] gaussCode= new Symbol[gaussCodesList.get(m).size()][];
			for(int j=0;j<gaussCode.length;j++)
			{
				gaussCode[j]=gaussCodesList.get(m).get(j);
			}
			char [] curveLabels=CurveLabelsList.get(m);

			GaussCodeRBC gaussCodeRBC = new GaussCodeRBC(curveLabels,gaussCode);
			System.out.println(gaussCodeRBC.getGaussCodeString());
			List<SegmentCode[]> rbc = RegionCode.computeRegionBoundaryCode(gaussCode);
			if (rbc == null)
				return null;
			String[] zoneCodes = GaussCodeRBC.getRegionBoundaryCodeString(rbc).split(
					"\n");
			System.out.println(Arrays.toString(zoneCodes));
			String gaussCodeString="";
			for(int i=0;i<gaussCode.length;i++)
			{
				Symbol [] w=gaussCode[i];
				String gaussString =curveLabels[i]+"";
				for(Symbol s:w)
					gaussString+=s.getLabel()+s.getSign()+" ";
				gaussCodeString+=gaussString+(html ? "<br />\n" : "\n");
			}
			List<List<Segment>> z0intlines = getZones().get(0).intlines;
			ArrayList<SegmentCode[]> bcCs = new ArrayList<SegmentCode[]>();
			for (int i = 0, end = z0intlines.size(); i <= end && bcCs.isEmpty(); i++) {
				List<Segment> extSegments;
				if (i == end) {
					extSegments = new ArrayList<Segment>();
					for (List<Segment> ls : z0intlines)
						extSegments.addAll(ls);
				} else
					extSegments = z0intlines.get(i);
				HashSet<String> extSC = new HashSet<String>();
				for (Segment seg : extSegments) {
					Symbol[] curve = null;
					for (int j = 0; j < curveLabels.length; j++) {
						if (seg.curve == curveLabels[j]) {
							curve = gaussCode[j];
							break;
						}
					}
					String p1 = String.valueOf(Math.abs(seg.p1));
					String p2 = String.valueOf(Math.abs(seg.p2));
					for (int j = 0; j < curve.length; j++) {
						Symbol s1 = curve[j];
						Symbol s2 = curve[(j + 1) % curve.length];
						if (p1.equals(s1.getLabel()) && p2.equals(s2.getLabel())) {
							if (extSC.add(p1 + s1.getSign() + p2 + s2.getSign()))
								break;
						}
					}
				}

				int len = 0;
				for (SegmentCode[] bc : rbc) {
					if (bc.length >= len) {
						boolean match = true;
						for (SegmentCode sc : bc) {
							Symbol s1 = sc.getFirstSymbol();
							Symbol s2 = sc.getSecondSymbol();
							if (!extSC.contains((s1.getLabel() + s1.getSign()
									+ s2.getLabel() + s2.getSign()))) {
								match = false;
								break;
							}
						}
						if (match) {
							if (bc.length > len) {
								len = bc.length;
								bcCs.clear();
							}
							bcCs.add(bc);
						}
					}
				}
			}

			//if (bcCs.isEmpty()) {
			for (String z : zoneCodes) {
				out.append("d_"+(m+1));
				out.append(html ? "<br />\n" : "\n");
				out.append("outer: ");
				out.append(z);
				out.append(html ? "<br />\n" : "\n");
				out.append(gaussCodeString);
				out.append(html ? "<br />\n" : "\n");
			}
			//}


		}
 		return out.toString();
	}
	public GaussCodeRBC getGaussCodeRBC() {
		int maxLbl = unusedIncidentPointLabels.getLast();
		char[] curvLabels = new char[curves.size()];
		Symbol[][] ogp = new Symbol[curves.size()][];
		int i = 0;
		for (Entry<Character, IncidentPointsPolygon> ec : curves.entrySet()) {
			char curCl = ec.getKey();
			curvLabels[i] = curCl;
			IncidentPointsPolygon poly = ec.getValue();
			if (poly.incidentPointRefs.isEmpty()) {
				String pointNumber = Integer.toString(maxLbl++);
				ogp[i] = new Symbol[] { new USymbol(pointNumber, '+', false) };
			} else {
				ogp[i] = new Symbol[poly.incidentPointRefs.size()];
				int j = 0;
				for(Entry<IncidentPointRef, Integer> e : poly.incidentPointRefs
						.entrySet()) {
					Integer pid = e.getValue();
					IncidentPoint ip = incidentPoints.get(pid);
					IncidentPointRef ipr = e.getKey();
					double x0 = poly.xpoints[ipr.cRef];
					double y0 = poly.ypoints[ipr.cRef];
					if(ip.x == x0 && ip.y == y0) {
						x0 = poly.xpoints[(ipr.cRef + poly.npoints - 1) % poly.npoints];
						y0 = poly.ypoints[(ipr.cRef + poly.npoints - 1) % poly.npoints];
					}
					double x1 = poly.xpoints[(ipr.cRef + 1) % poly.npoints];
					double y1 = poly.ypoints[(ipr.cRef + 1) % poly.npoints];
					if(ip.x == x1 && ip.y == y1) {
						x1 = poly.xpoints[(ipr.cRef + 2) % poly.npoints];
						y1 = poly.ypoints[(ipr.cRef + 2) % poly.npoints];
					}
					IncidentPointsPolygon othPoly;
					IncidentPointRef othIpr;
					if(ip.incidentCurves.size() == 1)  {
						othPoly = poly;
						othIpr = poly.incidentPointRefsInvUmn.get(-pid);
					} else if(ip.incidentCurves.size() == 2) {
						Character otherCurve = null;
						for(Character cl : ip.incidentCurves) {
							if(curCl != cl) {
								otherCurve = cl;
								break;
							}
						}
						othPoly = curves.get(otherCurve);
						othIpr = othPoly.incidentPointRefsInvUmn.get(pid);
					} else {
						ogp[i][j++] = new USymbol(
								String.valueOf(Math.abs(pid)), '#', ipr.under);
						continue;
					}
					double px = othPoly.xpoints[(othIpr.cRef + 1) % othPoly.npoints];
					double py = othPoly.ypoints[(othIpr.cRef + 1) % othPoly.npoints];
					if(ip.x == px && ip.y == py) {
						px = othPoly.xpoints[(othIpr.cRef + 2) % othPoly.npoints];
						py = othPoly.ypoints[(othIpr.cRef + 2) % othPoly.npoints];
					}
					// center over ip
					x0 -= ip.x;
					y0 -= ip.y;
					x1 -= ip.x;
					y1 -= ip.y;
					px -= ip.x;
					py -= ip.y;
					double pi2 = 2 * Math.PI;
					double aRot = pi2 - Math.atan2(y0, x0);
					double a1 = (Math.atan2(y1, x1) + aRot) % pi2;
					double ap = (Math.atan2(py, px) + aRot) % pi2;
					ogp[i][j++] = new USymbol(String.valueOf(Math.abs(pid)),
							ap < a1 ? '-' : (ap == a1 ? '@' : '+'), ipr.under);
				}
			}
			i++;
		}
		return new GaussCodeRBC(curvLabels, ogp);
	}

	/**
	 * Problems with mixed clockwise/counterclockwise curves and self
	 * intersecting curves (if conversionFromCode = true)
	 */
	public GaussCodeRBC getGaussCodeRBC(boolean conversionFromCode) {
		if(!conversionFromCode) return getGaussCodeRBC();
		int maxLbl = unusedIncidentPointLabels.getLast();
		char[] curvLabels = new char[curves.size()];
		Symbol[][] ogp = new Symbol[curves.size()][];
		int i = 0;
		for (Entry<Character, IncidentPointsPolygon> ec : curves.entrySet()) {
			curvLabels[i] = ec.getKey();
			IncidentPointsPolygon c = ec.getValue();
			Set<Entry<IncidentPointRef, Integer>> curveIPR = c.incidentPointRefs
					.entrySet();
			if (curveIPR.isEmpty()) {
				String pointNumber = Integer.toString(maxLbl++);
				ogp[i] = new Symbol[] { new Symbol(pointNumber, '+')};
			} else {
				ogp[i] = new Symbol[curveIPR.size()];
				Iterator<Entry<IncidentPointRef, Integer>> it = curveIPR
						.iterator();
				Entry<IncidentPointRef, Integer> pre = it.next();
				Entry<IncidentPointRef, Integer> cur = pre;
				Integer stVal = cur.getValue();
				int stSize = cur.getKey().folContCurves.size();
				int j = 1;
				while (it.hasNext()) {
					cur = it.next();
					if (cur.getValue() < 0)
						ogp[i][j++] = new Symbol(Integer.toString(-cur
								.getValue()),
								pre.getKey().folContCurves.size() > cur
										.getKey().folContCurves.size() ? '-'
										: '+');
					else
						ogp[i][j++] = new Symbol(cur.getValue().toString(),
								pre.getKey().folContCurves.size() > cur
										.getKey().folContCurves.size() ? '+'
										: '-');
					pre = cur;
				}
				if (stVal < 0)
					ogp[i][0] = new Symbol(Integer.toString(-stVal),
							cur.getKey().folContCurves.size() > stSize ? '-'
									: '+');
				else
					ogp[i][0] = new Symbol(stVal.toString(),
							cur.getKey().folContCurves.size() > stSize ? '+'
									: '-');
			}
			i++;
		}
		return new GaussCodeRBC(curvLabels, ogp);
	}

	public static class IncidentPoint extends Point2D.Double {
		private static final long serialVersionUID = 6722035294677566285L;
		private final LinkedHashSet<Character> incidentCurves = new LinkedHashSet<Character>();
		private final Set<Character> incidentCurvesUnm = Collections.unmodifiableSet(incidentCurves);
		public IncidentPoint() {
			super();
		}
		public IncidentPoint(double x, double y) {
			super(x, y);
		}
		public Set<Character> getIncidentCurves() {
			return incidentCurvesUnm;
		}
	}
	
	public static class IncidentPointsPolygon extends Polygon {
		private static final long serialVersionUID = -6097022565565494853L;
		private final LinkedHashSet<Character> firstPointContCurves = new LinkedHashSet<Character>();
		private final Set<Character> firstPointContCurvesUnmSet = Collections.unmodifiableSet(firstPointContCurves);
		private final TreeBidiMap<IncidentPointRef, Integer> incidentPointRefs = new TreeBidiMap<IncidentPointRef, Integer>();
		private final Set<Entry<IncidentPointRef, Integer>> incidentPointRefsUnmSet = Collections.unmodifiableSet(incidentPointRefs.entrySet());
		private final Map<Integer, IncidentPointRef> incidentPointRefsInvUmn = Collections.unmodifiableMap(incidentPointRefs.inverseBidiMap());
		public IncidentPointsPolygon() {
			super();
		}
		public IncidentPointsPolygon(int[] xpoints, int[] ypoints, int npoints) {
			super();
			for(int i = 0; i < npoints; i++) {
				int x = xpoints[i], y = ypoints[i];
				int sp = (i + 1) % npoints;
				if(x != xpoints[sp] || y != ypoints[sp]) addPoint(x, y);
			}
		}
		public Set<Entry<IncidentPointRef, Integer>> getOrderedIncidentPointRefs() {
			return incidentPointRefsUnmSet;
		}
		public Map<Integer, IncidentPointRef> getIncidentPointRefsMap() {
			return incidentPointRefsInvUmn;
		}
		public Set<Character> getFirstPointContCurves() {
			return firstPointContCurvesUnmSet;
		}
	}
	
	public static class IncidentPointRef implements Comparable<IncidentPointRef> {
		private final int cRef;
		private final double dis;
		private final LinkedHashSet<Character> folContCurves = new LinkedHashSet<Character>();
		private final Set<Character> folContCurvesUnm = Collections.unmodifiableSet(folContCurves);
		private boolean under = false;
		private Point2D.Double suggestedLabelPoint = null;
		public IncidentPointRef(int cRef, double dis) {
			this.cRef = cRef;
			this.dis = dis;
		}
		@Override
		public int compareTo(IncidentPointRef o) {
			if(this == o) return 0;
			if(this.cRef < o.cRef) return -1;
			if(this.cRef > o.cRef) return 1;
			int c = Double.compare(this.dis, o.dis);
			if(c != 0) return c;
			if(this.folContCurves.equals(o.folContCurves)
					&& (this.suggestedLabelPoint == o.suggestedLabelPoint || (this.suggestedLabelPoint != null
							&& this.suggestedLabelPoint.equals(o.suggestedLabelPoint))))
				return 0;
			if(this.suggestedLabelPoint != null) {
				if(o.suggestedLabelPoint != null) {
					if(!this.suggestedLabelPoint.equals(o.suggestedLabelPoint))
						return this.suggestedLabelPoint.hashCode() < o.suggestedLabelPoint.hashCode() ? -1 : 1;
				} else {
					return 1;
				}
			} else if(o.suggestedLabelPoint != null) {
					return -1;
			}
			return this.under != o.under ? (this.under ? 1 : -1) : this
					.hashCode() < o.hashCode() ? -1 : 1;
		}
		public int getcRef() {
			return cRef;
		}
		public double getDis() {
			return dis;
		}
		public Set<Character> getFolContCurves() {
			return folContCurvesUnm;
		}
		public boolean isUnder() {
			return under;
		}
		public void setUnder(boolean under) {
			this.under = under;
		}
		public Point2D.Double getSuggestedLabelPoint() {
			if(this.suggestedLabelPoint == null) return null;
			else return new Point2D.Double(suggestedLabelPoint.x, suggestedLabelPoint.y);
		}
	}
	
	public static class Segment {
		public final int p1;
		public final int p2;
		public final char curve;
		public final String contCurves;
		public final String label;
		private Entry<Zone, Integer> zone1 = null, zone2 = null;
		public Segment(int p1, int p2, char curve, String contCurves) {
			this.p1 = p1;
			this.p2 = p2;
			this.curve = curve;
			TreeSet<Character> sorted = new TreeSet<Character>();
			int contCurvesLength = contCurves.length();
			for(int i = 0; i < contCurvesLength; i++) sorted.add(contCurves.charAt(i));
			if(sorted.size() != contCurvesLength) throw new IllegalArgumentException("duplicate in contCurves");
			StringBuffer sortedBF = new StringBuffer();
			for(Character c : sorted) sortedBF.append(c);
			this.contCurves = sortedBF.toString();
			if(!sorted.add(curve)) throw new IllegalArgumentException("curve presents in contCurves");
			sortedBF.setLength(0);
			for(Character c : sorted) sortedBF.append(c);
			this.label = sortedBF.toString();
		}
		public Segment(int p1, int p2, char curve, Collection<Character> contCurves) {
			this.p1 = p1;
			this.p2 = p2;
			this.curve = curve;
			TreeSet<Character> sorted = new TreeSet<Character>(contCurves);
			if(sorted.size() != contCurves.size()) throw new IllegalArgumentException("duplicate in contCurves");
			StringBuffer sortedBF = new StringBuffer();
			for(Character c : sorted) sortedBF.append(c);
			this.contCurves = sortedBF.toString();
			if(!sorted.add(curve)) throw new IllegalArgumentException("curve presents in contCurves");
			sortedBF.setLength(0);
			for(Character c : sorted) sortedBF.append(c);
			this.label = sortedBF.toString();
		} 
		public Entry<Zone, Integer> getFirstZone() {
			return zone1;
		}
		public Entry<Zone, Integer> getSecondZone() {
			return zone2;
		}
		private void addZone(String zLabel, Entry<Zone, Integer> zone) {
			if(zone == null) throw new IllegalArgumentException("null zone");
			if(contCurves.equals(zLabel)) {
				if(zone1 != null) throw new IllegalStateException("first zone already added");
				zone1 = zone;
			} else if(label.equals(zLabel)) {
				if(zone2 != null) throw new IllegalStateException("second zone already added");
				zone2 = zone;
			} else throw new IllegalArgumentException("illegal label");
		}
	}
	
	public static class Zone {
		public final String label;
		public final List<List<Segment>> outlines;
		public final List<List<Segment>> intlines;
		private AreaWL area;
		private Zone(String label, List<List<Segment>> outlines, List<List<Segment>> intlines) {
			this.label = label;
			this.outlines = outlines;
			this.intlines = intlines;
			this.area = null;
		}
		public AreaWL getArea() {
			return area;
		}
	}
}
