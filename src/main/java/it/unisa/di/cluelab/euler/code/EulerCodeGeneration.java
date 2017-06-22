/*******************************************************************************
 * Copyright (c) 2013 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.EulerCode.Segment;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;
import it.unisa.di.cluelab.euler.code.EulerCodeUtils.CodeZones;
import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.USymbol;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

/**
 * @author Mattia De Rosa
 */
public class EulerCodeGeneration {
	private static final String DRAW_PLANAR_EXE = getSODependantPath("drawplanar");
	private static final String PLANARITY_EXE = getSODependantPath("planarity");

	/**
	 * This class is not instantiable.
	 */
	private EulerCodeGeneration() {
	}

	private static String getSODependantPath(String program) {
		try {
			String osname = System.getProperty("os.name").toLowerCase();
			String exe = "./"
					+ (osname.contains("win") ? program + ".exe" : (osname
							.contains("mac") ? program + ".mac" : program
							+ ".linux"));
			if (new File(exe).exists())
				return exe;
			else {
				String path = ClassLoader.getSystemClassLoader()
						.getResource(exe).toURI().getPath();
				if (new File(path).exists())
					return path;
				else
					return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static class GenerationErrorException extends Exception {
		private static final long serialVersionUID = 2069205737691541110L;
		private EulerCode wronglyGenerated = null;
		private String differences = null;

		public GenerationErrorException(String message) {
			super(message);
		}

		public GenerationErrorException(String message, Throwable cause) {
			super(message, cause);
		}

		public GenerationErrorException(String message,
				EulerCode wronglyGenerated, String originalCode,
				String generatedCode) {
			super(message);
			this.wronglyGenerated = wronglyGenerated;

			diff_match_patch dmp = new diff_match_patch();
			LinkedList<Diff> diffs = dmp.diff_main(originalCode, generatedCode);
			StringBuilder html = new StringBuilder();
			for (Diff aDiff : diffs) {
				String text = aDiff.text.replace("\n", "<br/>\n");
				switch (aDiff.operation) {
				case INSERT:
					html.append("<b style=\"background:#e6ffe6;\">");
					html.append(text);
					html.append("</b>");
					break;
				case DELETE:
					html.append("<i style=\"background:#ffe6e6;\">");
					html.append(text);
					html.append("</i>");
					break;
				case EQUAL:
					html.append(text);
				}
			}
			differences = html.toString();
		}

		public EulerCode getWronglyGenerated() {
			return wronglyGenerated;
		}

		public String getDifferences() {
			return differences;
		}
	}

	public static EulerCode genFromCode(CodeZones codeZones,
			final Set<Segment> external, int targetWidth, int targetHeight,
			boolean keepGeneratedAspectRatio) throws GenerationErrorException {
		if (PLANARITY_EXE == null)
			throw new GenerationErrorException(
					"planarity executable not found.");
		class AdjacencyList extends LinkedHashMap<Object, ArrayList<Integer>> {
			private static final long serialVersionUID = -3291013445921213929L;

			public ArrayList<Integer> getOrCreate(Object key)
					throws GenerationErrorException {
				ArrayList<Integer> get = super.get(key);
				if (get != null)
					return get;
				if (key == null)
					throw new GenerationErrorException("null key.");
				get = new ArrayList<Integer>();
				get.add(super.size());
				super.put(key, get);
				return get;
			}
		}
		class IntPair {
			final int i1, i2;

			public IntPair(int i1, int i2) {
				this.i1 = i1;
				this.i2 = i2;
			}

			@Override
			public int hashCode() {
				return i1 ^ (i2 << 16) ^ (i2 >>> 16);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null || getClass() != obj.getClass()
						|| i1 != ((IntPair) obj).i1 || i2 != ((IntPair) obj).i2)
					return false;
				return true;
			}
		}

		List<Zone> zones = codeZones.zones;

		boolean alternative = false;
		Collection<Segment> segments;
		if (alternative) {
			segments = new LinkedHashSet<Segment>();
			for (Zone zone : zones) {
				for (List<Segment> s : zone.outlines)
					segments.addAll(s);
				for (List<Segment> s : zone.intlines)
					segments.addAll(s);
			}
		} else {
			segments = new ArrayList<Segment>();
			for (List<Segment> cSegments : codeZones.code.values())
				segments.addAll(cSegments);
			Collections.sort((ArrayList<Segment>) segments,
					new Comparator<Segment>() {
						@Override
						public int compare(Segment arg0, Segment arg1) {
							boolean ext0 = external.contains(arg0);
							boolean ext1 = external.contains(arg1);
							if (ext0 != ext1)
								return ext0 ? -1 : 1;
							int r = arg0.contCurves.length()
									- arg1.contCurves.length();
							if (r != 0)
								return r;
							return arg0.p1 - arg0.p2;
						}
					});
		}

		ArrayList<Integer> pre = null;
		AdjacencyList nodes = new AdjacencyList();
		for (Segment seg : segments) {
			Entry<Zone, Integer> z1 = seg.getFirstZone();
			ArrayList<Integer> nodeZ1;
			ArrayList<Integer> nodeSeg;
			if (alternative && z1.getKey().label.isEmpty()) {
				if (pre == null) {
					nodeZ1 = nodes.getOrCreate(z1.getKey());
					nodeSeg = nodes.getOrCreate(seg);
					pre = nodeZ1;
				} else {
					nodeZ1 = pre;
					nodeSeg = nodes.getOrCreate(seg);
					pre = nodes.getOrCreate(new Object());
					pre.add(nodeSeg.get(0));
					nodeSeg.add(pre.get(0));
				}
			} else {
				// nodeZ1 = nodes.getOrCreate((z1.getValue() <= 0) ? z1.getKey()
				nodeZ1 = nodes.getOrCreate((z1.getValue() <= 0) ? (z1
						.getValue() > -1 ? z1.getKey() : z1.getKey().intlines
						.get(-1 - z1.getValue())) : z1.getKey().outlines.get(z1
						.getValue()));
				nodeSeg = nodes.getOrCreate(seg);
			}
			nodeSeg = nodes.getOrCreate(seg);
			nodeZ1.add(nodeSeg.get(0));
			nodeSeg.add(nodeZ1.get(0));

			ArrayList<Integer> nodeP1 = nodes.getOrCreate(seg.p1);
			nodeSeg.add(nodeP1.get(0));
			nodeP1.add(nodeSeg.get(0));

			ArrayList<Integer> nodeP2;
			if (seg.p1 == seg.p2)
				nodeP2 = nodes.getOrCreate(new Long(seg.p1));
			else
				nodeP2 = nodes.getOrCreate(seg.p2);
			nodeSeg.add(nodeP2.get(0));
			nodeP2.add(nodeSeg.get(0));

			Entry<Zone, Integer> z2 = seg.getSecondZone();
			ArrayList<Integer> nodeZ2 = nodes
					// .getOrCreate((z2.getValue() <= 0) ? z2.getKey() : z2
					.getOrCreate((z2.getValue() <= 0) ? (z2.getValue() > -1 ? z2
							.getKey() : z2.getKey().intlines.get(-1
							- z2.getValue()))
							: z2.getKey().outlines.get(z2.getValue()));
			nodeSeg.add(nodeZ2.get(0));
			nodeZ2.add(nodeSeg.get(0));

			if (seg.p1 == seg.p2) {
				ArrayList<Integer> nodeSegFake = nodes.getOrCreate(new IntPair(
						seg.p1, seg.p2));
				nodeZ1.add(nodeSegFake.get(0));
				nodeSegFake.add(nodeZ1.get(0));
				nodeSegFake.add(nodeZ2.get(0));
				nodeZ2.add(nodeSegFake.get(0));
				nodeP2.add(nodeSegFake.get(0));
				nodeSegFake.add(nodeP2.get(0));
				nodeSegFake.add(nodeP1.get(0));
				nodeP1.add(nodeSegFake.get(0));
			}
		}

		String id = Thread.currentThread().getId() + ".";
		String rand = Math.round(Math.random() * 1000000000) + ".";
		String graphFileName = rand + id + "i.txt";
		String planarityFileName = rand + id + "o.txt";
		PrintWriter pw = null;
		LinkedHashMap<IntPair, int[]> eCoords = new LinkedHashMap<IntPair, int[]>();
		try {
			pw = new PrintWriter(graphFileName);
			pw.println("N=" + nodes.size());
			for (ArrayList<Integer> node : nodes.values()) {
				pw.print(node.get(0) + ": ");
				Collections.sort(node.subList(1, node.size()));
				for (int i = 1, end = node.size(); i < end; i++) {
					pw.print(node.get(i) + " ");
					if (node.get(0) < node.get(i))
						eCoords.put(new IntPair(node.get(0), node.get(i)),
								new int[3]);
				}
				pw.println("-1");
			}
			pw.close();
		} catch (Exception e) {
			if (pw != null)
				pw.close();
			throw new GenerationErrorException("Error writing graph file.", e);
		}
		StringBuffer result = null;
		InputStreamReader isr = null;
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(new String[] { PLANARITY_EXE, "-s", "-d",
					graphFileName, planarityFileName });
			pr.getOutputStream().close();
			if (pr.waitFor() != 0) {
				isr = new InputStreamReader(pr.getInputStream());
				char[] tmp = new char[100];
				result = new StringBuffer();
				for (int n; (n = isr.read(tmp)) != -1; result.append(tmp, 0, n))
					;
				isr.close();
				isr = new InputStreamReader(pr.getErrorStream());
				for (int n; (n = isr.read(tmp)) != -1; result.append(tmp, 0, n))
					;
				isr.close();
			}
		} catch (InterruptedException ie) {
			throw new GenerationErrorException("Error executing planar.", ie);
		} catch (IOException ioe) {
			if (isr != null)
				try {
					isr.close();
				} catch (IOException e1) {
				}
			result = new StringBuffer("Planar output error.");
		}
		if (result != null)
			throw new GenerationErrorException(result.toString());
		ArrayList<int[]> nCoords = new ArrayList<int[]>();
		BufferedReader br = null;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		try {
			String errorMessage = "Error in planar output file.";
			br = new BufferedReader(new FileReader(planarityFileName));
			int nNodes = Integer.parseInt(br.readLine().replaceFirst("N=", ""));
			String line;
			while ((line = br.readLine()) != null
					&& !line.equals("<DrawPlanar>"))
				;
			for (int i = 0; i < nNodes && (line = br.readLine()) != null; i++) {
				String[] sp = line.split(" ");
				if (!sp[0].equals(i + ":")) {
					br.close();
					throw new GenerationErrorException(errorMessage);
				}
				int[] cNode = new int[] { Integer.parseInt(sp[1]),
						Integer.parseInt(sp[2]), Integer.parseInt(sp[3]) };
				if (cNode[1] > cNode[2]){
					br.close();
					throw new GenerationErrorException(errorMessage);
				}
				if (maxX < cNode[2])
					maxX = cNode[2];
				if (maxY < cNode[0])
					maxY = cNode[0];
				nCoords.add(cNode);
			}
			int nEdges = 0;
			for (int[] co : eCoords.values()) {
				String edgeLine1 = br.readLine();
				String edgeLine2 = br.readLine();
				if (!edgeLine1.split(":")[1].equals(edgeLine2.split(":")[1])){
					br.close();
					throw new GenerationErrorException(errorMessage);
				}
				String[] sp = edgeLine1.split(" ");
				if (!sp[0].equals((2 * nEdges++) + ":")){
					br.close();
					throw new GenerationErrorException(errorMessage);
				}
				co[0] = Integer.parseInt(sp[1]);
				co[1] = Integer.parseInt(sp[2]);
				co[2] = Integer.parseInt(sp[3]);
				if (co[1] >= co[2]){
					br.close();
					throw new GenerationErrorException(errorMessage);
				}
				if (maxX < co[0])
					maxX = co[0];
				if (maxY < co[2])
					maxY = co[2];
			}
			if (!br.readLine().equals("</DrawPlanar>")){
				br.close();
				throw new GenerationErrorException(errorMessage);
			}
			br.close();
		} catch (IOException e) {
			if (br != null)
				try {
					br.close();
				} catch (IOException e1) {
				}
			throw new GenerationErrorException("Error reading planar output.",
					e);
		}
		int scaleX = Math.max(2, (targetWidth / (maxX * 2))) * 2;
		int scaleY = Math.max(2, (targetHeight / (maxY * 2))) * 2;
		int diffY = scaleY / 2;
		EulerCode eulerCode;
		if(keepGeneratedAspectRatio) { // remove unnecessary bends
			LinkedHashMap<Character, ArrayList<Polygon>> realCurves = new LinkedHashMap<Character, ArrayList<Polygon>>();
			for (Entry<Character, List<Segment>> e : codeZones.code.entrySet()) {
				ArrayList<Polygon> curve = new ArrayList<Polygon>();
				for (Segment seg : e.getValue()) {
					Polygon poly = new Polygon();
					curve.add(poly);

					int idP1 = nodes.get(seg.p1).get(0);
					int[] cP1 = nCoords.get(idP1);
					poly.addPoint((cP1[1] + (cP1[2] - cP1[1]) / 2) * scaleX, cP1[0]
							* scaleY);

					int idSeg = nodes.get(seg).get(0);
					int[] cSeg = nCoords.get(idSeg);
					int[] eP1Seg = eCoords.get(idP1 < idSeg ? new IntPair(idP1,
							idSeg) : new IntPair(idSeg, idP1));
					if (cP1[0] == eP1Seg[1]) {
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[1] * scaleY
								+ diffY);
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[2] * scaleY
								- diffY);
					} else if (cP1[0] == eP1Seg[2]) {
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[2] * scaleY
								- diffY);
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[1] * scaleY
								+ diffY);
					} else
						throw new RuntimeException("I do not know what to do 1.");
					poly.addPoint((cSeg[1] + (cSeg[2] - cSeg[1]) / 2) * scaleX,
							cSeg[0] * scaleY);

					int idP2;
					if (seg.p1 == seg.p2)
						idP2 = nodes.get(new Long(seg.p1)).get(0);
					else
						idP2 = nodes.get(seg.p2).get(0);
					int[] eSegP2 = eCoords.get(idSeg < idP2 ? new IntPair(idSeg,
							idP2) : new IntPair(idP2, idSeg));
					if (cSeg[0] == eSegP2[1]) {
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[1] * scaleY
								+ diffY);
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[2] * scaleY
								- diffY);
					} else if (cSeg[0] == eSegP2[2]) {
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[2] * scaleY
								- diffY);
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[1] * scaleY
								+ diffY);
					} else
						throw new RuntimeException("I do not know what to do 2.");

					if (seg.p1 == seg.p2) {
						int[] cP2 = nCoords.get(idP2);
						poly.addPoint((cP2[1] + (cP2[2] - cP2[1]) / 2) * scaleX,
								cP2[0] * scaleY);
						int idSegFake = nodes.get(new IntPair(seg.p1, seg.p2)).get(
								0);
						int[] cSegFake = nCoords.get(idSegFake);
						int[] eP2SegFake = eCoords
								.get(idP2 < idSegFake ? new IntPair(idP2, idSegFake)
										: new IntPair(idSegFake, idP2));
						if (cP2[0] == eP2SegFake[1]) {
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[1]
									* scaleY + diffY);
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[2]
									* scaleY - diffY);
						} else if (cP2[0] == eP2SegFake[2]) {
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[2]
									* scaleY - diffY);
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[1]
									* scaleY + diffY);
						} else
							throw new RuntimeException(
									"I do not know what to do 3.");
						poly.addPoint(
								(cSegFake[1] + (cSegFake[2] - cSegFake[1]) / 2)
										* scaleX, cSegFake[0] * scaleY);
						int[] eSegFakeP1 = eCoords
								.get(idSegFake < idP1 ? new IntPair(idSegFake, idP1)
										: new IntPair(idP1, idSegFake));
						if (cSegFake[0] == eSegFakeP1[1]) {
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[1]
									* scaleY + diffY);
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[2]
									* scaleY - diffY);
						} else if (cSegFake[0] == eSegFakeP1[2]) {
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[2]
									* scaleY - diffY);
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[1]
									* scaleY + diffY);
						} else
							throw new RuntimeException(
									"I do not know what to do 4.");
					}
					int[] cP2 = nCoords.get(nodes.get(seg.p2).get(0));
					poly.addPoint((cP2[1] + (cP2[2] - cP2[1]) / 2) * scaleX, cP2[0]
							* scaleY);
				}
				realCurves.put(e.getKey(), curve);
			}
			ArrayList<Entry<Character, ArrayList<Polygon>>> realCurvesSet = new ArrayList<Entry<Character, ArrayList<Polygon>>>(
					realCurves.entrySet());
			boolean changes;
			do {
				changes = false;
				for (Entry<Character, ArrayList<Polygon>> e1 : realCurvesSet) {
					Character curveLabel = e1.getKey();
					for (Polygon poly1 : e1.getValue()) {
						for (int j = poly1.npoints - 1; j > 1; j--) {
							int x0 = poly1.xpoints[j - 2], x1 = poly1.xpoints[j - 1], x2 = poly1.xpoints[j];
							int y0 = poly1.ypoints[j - 2], y1 = poly1.ypoints[j - 1], y2 = poly1.ypoints[j];
							Line2D.Double line = new Line2D.Double(x0, y0, x2,
									y2);
							if (j == 2)
								line.setLine(GeomUtils.pointOverSegment(x0, y0,
										x1, y1, 0.5), line.getP2());
							if (j == poly1.npoints - 1)
								line.setLine(line.getP1(), GeomUtils
										.pointOverSegment(x2, y2, x1, y1, 0.5));
							boolean free = true;
							curves: for (Entry<Character, ArrayList<Polygon>> e2 : realCurvesSet) {
								if (curveLabel.equals(e2.getKey()))
									continue;
								for (Polygon poly2 : e2.getValue()) {
									for (int k = 1; k < poly2.npoints; k++) {
										int x3 = poly2.xpoints[k - 1], x4 = poly2.xpoints[k];
										int y3 = poly2.ypoints[k - 1], y4 = poly2.ypoints[k];
										if ((x0 == x3 && y0 == y3 && x2 == x4 && y2 == y4)
												|| (x0 == x4 && y0 == y4
														&& x2 == x3 && y2 == y3)
												|| line.intersectsLine(x3, y3,
														x4, y4)) {
											free = false;
											break curves;
										}
									}
								}
							}
							if (free) {
								changes = true;
								poly1.npoints--;
								poly1.xpoints[j - 1] = x2;
								poly1.ypoints[j - 1] = y2;
							}
						}
					}
				}
			} while (changes);

			int maxWidth = maxX * scaleX;

			Point[] points = new Point[nodes.size()];
			int[] pointLabels = new int[points.length];
			int n = 0;
			for (Entry<Object, ArrayList<Integer>> e : nodes.entrySet()) {
				if (e.getKey().getClass() == Integer.class) {
					int[] cP = nCoords.get(e.getValue().get(0));
					points[n] = new Point(maxWidth
							- (cP[1] + (cP[2] - cP[1]) / 2) * scaleX, cP[0]
							* scaleY);
					pointLabels[n] = (Integer) e.getKey();
					n++;
				}
			}

			Polygon[] curves = new Polygon[realCurvesSet.size()];
			char[] curveLabels = new char[curves.length];
			n = 0;
			for (Entry<Character, ArrayList<Polygon>> e : realCurvesSet) {
				Polygon poly = new Polygon();
				for (Polygon seg : e.getValue()) {
					for (int i = 0; i < seg.npoints; i++) {
						int x = maxWidth - seg.xpoints[i], y = seg.ypoints[i];
						int lp = poly.npoints - 1;
						if (lp < 0
								|| (poly.xpoints[lp] != x || poly.ypoints[lp] != y))
							poly.addPoint(x, y);
					}
				}
				int tot = 0;
				for (int i = 0; i < poly.npoints; i++) {
					int ip = (i + 1) % poly.npoints;
					tot += (poly.xpoints[i] * poly.ypoints[ip])
							- (poly.ypoints[i] * poly.xpoints[ip]);
				}
				if (tot < 0)
					System.err.println("WARNING: " + e.getKey()
							+ " counterclockwise.");
				curves[n] = poly;
				curveLabels[n] = e.getKey();
				n++;
			}
			eulerCode = new EulerCode(curveLabels, curves, pointLabels, points);
		} else { // keep all bends
			eulerCode = new EulerCode();
			for (Entry<Character, List<Segment>> e : codeZones.code.entrySet()) {
				Character curveLabel = e.getKey();
				Polygon poly = new Polygon();
				poly.addPoint(0, 0);
				List<Segment> cseg = e.getValue();
				for (int i = 0, end = cseg.size(); i < end; i++) {
					Segment seg = cseg.get(i);
	
					int idP1 = nodes.get(seg.p1).get(0);
					int[] cP1 = nCoords.get(idP1);
					poly.addPoint(Integer.MIN_VALUE, cP1[0] * scaleY);
	
					int idSeg = nodes.get(seg).get(0);
					int[] cSeg = nCoords.get(idSeg);
					int[] eP1Seg = eCoords.get(idP1 < idSeg ? new IntPair(idP1,
							idSeg) : new IntPair(idSeg, idP1));
					if (cP1[0] == eP1Seg[1]) {
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[1] * scaleY
								+ diffY);
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[2] * scaleY
								- diffY);
					} else if (cP1[0] == eP1Seg[2]) {
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[2] * scaleY
								- diffY);
						poly.addPoint(eP1Seg[0] * scaleX, eP1Seg[1] * scaleY
								+ diffY);
					} else
						throw new GenerationErrorException(
								"I do not know what to do 1.");
					poly.addPoint(Integer.MIN_VALUE, cSeg[0] * scaleY);
	
					int idP2;
					if (seg.p1 == seg.p2)
						idP2 = nodes.get(new Long(seg.p1)).get(0);
					else
						idP2 = nodes.get(seg.p2).get(0);
					int[] eSegP2 = eCoords.get(idSeg < idP2 ? new IntPair(idSeg,
							idP2) : new IntPair(idP2, idSeg));
					if (cSeg[0] == eSegP2[1]) {
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[1] * scaleY
								+ diffY);
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[2] * scaleY
								- diffY);
					} else if (cSeg[0] == eSegP2[2]) {
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[2] * scaleY
								- diffY);
						poly.addPoint(eSegP2[0] * scaleX, eSegP2[1] * scaleY
								+ diffY);
					} else
						throw new GenerationErrorException(
								"I do not know what to do 2: " + cSeg[0] + "; "
										+ eSegP2[1] + "; " + eSegP2[2]);
	
					if (seg.p1 == seg.p2) {
						int[] cP2 = nCoords.get(idP2);
						poly.addPoint(Integer.MIN_VALUE, cP2[0] * scaleY);
						int idSegFake = nodes.get(new IntPair(seg.p1, seg.p2)).get(
								0);
						int[] cSegFake = nCoords.get(idSegFake);
						int[] eP2SegFake = eCoords
								.get(idP2 < idSegFake ? new IntPair(idP2, idSegFake)
										: new IntPair(idSegFake, idP2));
						if (cP2[0] == eP2SegFake[1]) {
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[1]
									* scaleY + diffY);
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[2]
									* scaleY - diffY);
						} else if (cP2[0] == eP2SegFake[2]) {
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[2]
									* scaleY - diffY);
							poly.addPoint(eP2SegFake[0] * scaleX, eP2SegFake[1]
									* scaleY + diffY);
						} else
							throw new GenerationErrorException(
									"I do not know what to do 3.");
						poly.addPoint(Integer.MIN_VALUE, cSegFake[0] * scaleY);
						int[] eSegFakeP1 = eCoords
								.get(idSegFake < idP1 ? new IntPair(idSegFake, idP1)
										: new IntPair(idP1, idSegFake));
						if (cSegFake[0] == eSegFakeP1[1]) {
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[1]
									* scaleY + diffY);
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[2]
									* scaleY - diffY);
						} else if (cSegFake[0] == eSegFakeP1[2]) {
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[2]
									* scaleY - diffY);
							poly.addPoint(eSegFakeP1[0] * scaleX, eSegFakeP1[1]
									* scaleY + diffY);
						} else
							throw new GenerationErrorException(
									"I do not know what to do 4.");
					}
				}
				poly.npoints--;
				poly.xpoints[0] = poly.xpoints[poly.npoints];
				poly.ypoints[0] = poly.ypoints[poly.npoints];
				for (int i = 0; i < poly.npoints; i++) {
					if (poly.xpoints[i] == Integer.MIN_VALUE) {
						poly.xpoints[i] = (poly.xpoints[(i + poly.npoints - 1)
								% poly.npoints] + poly.xpoints[(i + 1)
								% poly.npoints]) / 2;
					}
				}
				eulerCode.addCurve(curveLabel, poly);
			}
		}
		try {
			new File(graphFileName).delete();
			new File(planarityFileName).delete();
		} catch (Exception e) {
		}

		String orCode = codeZones.getCodeString();
		String genCode = eulerCode.getCode(true, false, false);
		if (!genCode.equals(orCode))
			throw new GenerationErrorException("Generated different code.",
					eulerCode, orCode, genCode);
		return eulerCode;
	}

	public static EulerCode genFromEulerCode(EulerCodeRBC ecrbc, int embIterNo,
			int targetWidth, int targetHeight,
			boolean keepGeneratedAspectRatio, boolean trySplines,
			boolean tryGeomShapes) throws GenerationErrorException {
		GaussCodeRBC[] codes = ecrbc.getGaussCodeRBCs();
		SegmentCode[][] withins = ecrbc.getWithins();
		SegmentCode[][] outers = ecrbc.getOuters();

		int totCurves = 0;
		for (GaussCodeRBC c : codes)
			totCurves += c.getCurveLabels().length;

		int[] firstNodes = new int[codes.length];
		char[] curveLabels = new char[totCurves];
		EmbSym[][] gaussCode = new EmbSym[totCurves][];
		ArrayList<EmbNode> nodes = new ArrayList<EmbNode>();
		HashMap<String, EmbNode> labeledNodes = new HashMap<String, EmbNode>();

		HashMap<String, EmbSym> symbolMap = new HashMap<String, EmbSym>();

		for (int i = 0, k = 0; i < codes.length; i++) {
			EmbGC embGc = createEmbeddingFromGC(codes[i], outers[i]);
			System.arraycopy(embGc.getCurveLabels(), 0, curveLabels, k,
					embGc.getCurveLabels().length);
			Symbol[][] gc = embGc.getGaussCode();
			System.arraycopy(gc, 0, gaussCode, k, gc.length);
			firstNodes[i] = nodes.size();
			nodes.addAll(embGc.nodes);
			labeledNodes.putAll(embGc.labeledNodes);

			for (Symbol[] syms : gc) {
				for (Symbol s : syms) {
					String l = s.getLabel() + s.getSign();
					if (symbolMap.put(l, (EmbSym) s) != null)
						throw new GenerationErrorException("Duplicate symbol "
								+ l + ".");
				}
			}

			k += embGc.getCurveLabels().length;
		}

		EmbGC totEmb = new EmbGC(curveLabels, gaussCode);
		if (codes.length > 1) {
			EmbNode ext = totEmb.newNode();
			for (int i = 0; i < codes.length; i++) {
				SegmentCode[] within = withins[i];
				EmbNode fn = nodes.get(firstNodes[i]);
				if (within == null) {
					ext.addEdge('*', false, fn);
					fn.addEdge('\0', true, ext);
				} else {
					SegmentCode s = within[0];
					Symbol f = s.getFirstSymbol();
					char fSign = f.getSign();
					EmbNode fNode = symbolMap.get(f.getLabel() + fSign).node;
					fNode.addEdge(s.getDirection() != fSign ? ','
							: (fSign == '+' ? '*' : '.'), false, fn);
					fn.addEdge('\0', true, fNode);
				}
			}
		}

		totEmb.nodes.addAll(nodes);
		totEmb.labeledNodes.putAll(labeledNodes);
		assignCoordinates(totEmb, embIterNo, targetWidth, targetHeight, 1,
				keepGeneratedAspectRatio);

		final EulerCode res = drawEd(totEmb, trySplines, tryGeomShapes);

		EulerCodeRBC gen;
		try {
			gen = res.getEulerCodeRBC();
		} catch (Exception e) {
			throw new GenerationErrorException(e.getMessage(), res,
					ecrbc.getEulerCodeRBCString(), "");
		}
		if (ecrbc.equals(gen))
			return res;
		else
			throw new GenerationErrorException("Generated different code.",
					res, ecrbc.getEulerCodeRBCString(),
					gen.getEulerCodeRBCString());
	}

	public static EulerCode genFromGaussCodeRBC(GaussCodeRBC gcrbc,
			SegmentCode[] externalFace, int embIterNo, int targetWidth,
			int targetHeight, boolean keepGeneratedAspectRatio,
			boolean trySplines, boolean tryGeomShapes)
			throws GenerationErrorException {
		EmbGC embGc = createEmbeddingFromGC(gcrbc, externalFace);
		assignCoordinates(embGc, embIterNo, targetWidth, targetHeight, 1,
				keepGeneratedAspectRatio);
		EulerCode res = drawEd(embGc, trySplines, tryGeomShapes);
		if (!gcrbc.equals(res.getGaussCodeRBC()))
			throw new GenerationErrorException("Generated different code.",
					res, gcrbc.getGaussCodeString(), res.getGaussCode(false,
							false));
		return res;
	}

	private static EmbGC createEmbeddingFromGC(GaussCodeRBC gcrbc,
			SegmentCode[] externalFace) {
		char[] lbs = gcrbc.getCurveLabels();
		Symbol[][] gc = gcrbc.getGaussCode();
		EmbSym[][] embGCSyms = new EmbSym[gc.length][];
		EmbGC embGC = new EmbGC(gcrbc.getCurveLabels(), embGCSyms);

		EmbNode extNode = externalFace == null ? new EmbNode() : embGC
				.newNode();

		for (int i = 0; i < embGCSyms.length; i++) {
			String crvLbl = String.valueOf(lbs[i]);
			Symbol[] inSyms = gc[i];
			EmbSym[] embSyms = new EmbSym[inSyms.length];
			embGCSyms[i] = embSyms;
			HashSet<String> symLbs = new HashSet<String>();
			for (int j = 0; j < inSyms.length; j++) {
				Symbol curInSym = inSyms[j];
				boolean added = symLbs.add(curInSym.getLabel());
				if (curInSym instanceof USymbol) {
					EmbNodeU curNode = (EmbNodeU) embGC.newNode(
							curInSym.getLabel(), true, new EmbNodeU());
					if (!added && curNode.ucurves.remove(crvLbl))
						curNode.ucurves.add("-" + crvLbl);
					if (((USymbol) curInSym).isUnder())
						curNode.ucurves.add(crvLbl);
					embSyms[j] = new EmbSym(curInSym, curNode);
				} else {
					EmbNode curNode = embGC.newNode(curInSym.getLabel(), true);
					embSyms[j] = new EmbSym(curInSym, curNode);
				}
			}
		}

		HashSet<UnorderdNodePair> existent = new HashSet<UnorderdNodePair>();

		for (int i = 0; i < embGCSyms.length; i++) {
			EmbSym[] embSyms = embGCSyms[i];
			for (int j = 0; j < embSyms.length; j++) {
				EmbSym curSym = embSyms[j];

				EmbSym sucSym = embSyms[(j + 1) % embSyms.length];

				int extFaceIndex = -1;
				if (externalFace != null) {
					for (int k = 0; k < externalFace.length; k++) {
						SegmentCode sc = externalFace[k];
						Symbol scFirst = sc.getFirstSymbol();
						Symbol scSecond = sc.getSecondSymbol();
						if (curSym.getLabel().equals(scFirst.getLabel())
								&& curSym.getSign() == scFirst.getSign()
								&& sucSym.getLabel()
										.equals(scSecond.getLabel())
								&& sucSym.getSign() == scSecond.getSign()) {
							extFaceIndex = k;
							break;
						}
					}
				}

				char extFace;
				if (extFaceIndex != -1) {
					extFace = externalFace[extFaceIndex].getDirection();
					if (extFace != '+' && extFace != '-')
						throw new RuntimeException("Illegal sign.");
				} else {
					extFace = '\0';
				}

				if (curSym.node == sucSym.node) {
					EmbNode midNode1 = embGC.newNode();
					curSym.bends.add(midNode1);
					EmbNode midNode2 = embGC.newNode();
					curSym.bends.add(midNode2);

					curSym.node.addEdge(curSym.getSign(), false, midNode1);
					sucSym.node.addEdge(sucSym.getSign(), true, midNode2);

					midNode1.addEdge(',', true, curSym.node);
					midNode1.addEdge(',', false, midNode2);

					midNode2.addEdge(',', true, midNode1);
					midNode2.addEdge(',', false, curSym.node);
					if (extFace != '\0') {
						midNode1.addEdge(extFace, false, extNode);
						extNode.addEdge(
								(char) (externalFace.length - extFaceIndex),
								true, midNode1);
					}

				} else if (extFace == '\0'
						&& existent.add(new UnorderdNodePair(curSym.node,
								sucSym.node))) {
					curSym.node.addEdge(curSym.getSign(), false, sucSym.node);
					sucSym.node.addEdge(sucSym.getSign(), true, curSym.node);
				} else {
					EmbNode midNode = embGC.newNode();
					curSym.bends.add(midNode);

					curSym.node.addEdge(curSym.getSign(), false, midNode);
					sucSym.node.addEdge(sucSym.getSign(), true, midNode);

					midNode.addEdge(',', true, curSym.node);
					midNode.addEdge(',', false, sucSym.node);
					if (extFace != '\0') {
						midNode.addEdge(extFace, false, extNode);
						extNode.addEdge(
								(char) (externalFace.length - extFaceIndex),
								true, midNode);
					}
				}
			}
		}
		return embGC;
	}

	private static void assignCoordinates(EmbGC embGC, int embIterNo,
			int targetWidth, int targetHeight, int skipNodTarWidHei,
			boolean keepGeneratedAspectRatio) throws GenerationErrorException {
		if (DRAW_PLANAR_EXE == null)
			throw new GenerationErrorException(
					"drawplanar executable not found.");
		InputStream in = null;
		InputStream err = null;
		OutputStream out = null;
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		int res = -1;
		Exception except = null;
		try {
			Runtime rt = Runtime.getRuntime();
			final Process pr = rt.exec(new String[] { DRAW_PLANAR_EXE,
					String.valueOf(embIterNo) });
			out = pr.getOutputStream();
			out.write(embGC.exportAdjList().getBytes());
			out.flush();
			out.close();
			byte[] tmp = new byte[100];
			in = pr.getInputStream();
			err = pr.getErrorStream();
			for (int nIn = 0, nErr = 0; nIn != -1 || nErr != -1;) {
				nIn = in.read(tmp);
				if (nIn > 0)
					output.append(new String(tmp, 0, nIn));
				nErr = err.read(tmp);
				if (nErr > 0)
					error.append(new String(tmp, 0, nErr));
			}
			res = pr.waitFor();
		} catch (InterruptedException | IOException | RuntimeException ex) {
			except = ex;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e1) {
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e1) {
				}
		}
		if (res != 0)
			throw new GenerationErrorException("drawplanar return value: "
					+ res + ".");
		if (except != null)
			throw new GenerationErrorException("Error executing drawplanar.",
					except);

		String[] rows = output.toString().split("(\r\n|\n\r|\r|\n)");
		if (rows.length != embGC.nodes.size())
			throw new GenerationErrorException(
					"drawplanar output error: different line count.");
		Rectangle2D.Float bound = null;
		Point2D.Float[] points = new Point2D.Float[rows.length];
		double minDist = Double.MAX_VALUE;
		for (int i = 0; i < rows.length; i++) {
			String[] coord = rows[i].split(" ");
			if (coord.length != 2)
				throw new GenerationErrorException(
						"drawplanar output error: wrong coordinates.");
			try {
				Point2D.Float p = new Point2D.Float(Float.parseFloat(coord[0]),
						-Float.parseFloat(coord[1]));
				points[i] = p;
				if (i >= skipNodTarWidHei) {
					if(bound == null) bound = new Rectangle2D.Float(p.x, p.y, 0, 0);
					else bound.add(p);
				}
				for (int j = skipNodTarWidHei; j < i; j++) {
					double d = p.distance(points[j]);
					if (d < minDist)
						minDist = d;
				}
			} catch (NumberFormatException nfe) {
				throw new GenerationErrorException(
						"drawplanar output error: no float coordinates.");
			}
		}
		float minScale = (float) (embIterNo <= 0 ? Math.ceil(4. / minDist)
				: 4. / minDist);
		float scaleX = Math.max(minScale, targetWidth / (bound.width + 1));
		float scaleY = Math.max(minScale, targetHeight / (bound.height + 1));
		if (embIterNo <= 0) {
			scaleX = (int) scaleX;
			scaleY = (int) scaleY;
		}
		if (keepGeneratedAspectRatio) {
			if (scaleX > scaleY)
				scaleX = scaleY;
			else
				scaleY = scaleX;
		}
		for (int i = 0; i < points.length; i++) {
			Point2D.Float p = points[i];
			embGC.nodes.get(i).setLocation(Math.round(p.x * scaleX),
					Math.round(p.y * scaleY));
		}
	}

	private static EulerCode drawEd(EmbGC embGC, boolean trySplines,
			boolean tryGeomShapes) {
		final int splineSteps = 12;
		final int circleSegs = splineSteps * 3;
		char[] curveLabels = embGC.getCurveLabels();
		EmbSym[][] gc = (EmbSym[][]) embGC.getGaussCode();

		int[] ptLbs = new int[embGC.labeledNodes.size()];
		HashSet<Point> ptsSet = new HashSet<Point>();
		Point[] pts = new Point[ptLbs.length];
		Set<Entry<String, EmbNode>> lns = embGC.labeledNodes.entrySet();
		@SuppressWarnings("unchecked")
		Set<String>[] ptUCs = !lns.isEmpty()
				&& lns.iterator().next().getValue() instanceof EmbNodeU ? new Set[ptLbs.length]
				: null;
		int pi = 0;
		for (Entry<String, EmbNode> e : lns) {
			EmbNode n = e.getValue();
			try {
				ptLbs[pi] = Integer.parseInt(e.getKey());
			} catch (NumberFormatException nfe) {
			}
			if (ptUCs != null)
				ptUCs[pi] = ((EmbNodeU) n).ucurves;
			Point p = new Point(n.x, n.y);
			ptsSet.add(p);
			pts[pi++] = p;
		}

		MPolygon[] curves = new MPolygon[gc.length];
		for (int i = 0; i < gc.length; i++) {
			EmbSym[] syms = gc[i];
			Polygon crv = new Polygon();
			for (int j = 0; j < syms.length; j++) {
				EmbSym sym = syms[j];
				crv.addPoint(sym.node.x, sym.node.y);
				for (EmbNode bend : sym.bends)
					crv.addPoint(bend.x, bend.y);
			}

			ArrayList<Polygon> toAdd = new ArrayList<Polygon>();
			if (trySplines) {
				toAdd.add(CurveUtils.createCubicSpline(crv.xpoints,
						crv.ypoints, crv.npoints, splineSteps));
				if (syms.length > 1 && crv.npoints != syms.length) {
					if (syms.length != 2) {
						Polygon noBends = new Polygon();
						for (int j = 0; j < syms.length; j++)
							noBends.addPoint(syms[j].node.x, syms[j].node.y);
						toAdd.add(CurveUtils.createCubicSpline(noBends.xpoints,
								noBends.ypoints, noBends.npoints, splineSteps));
					}
					for (int j = 0, end = crv.npoints - syms.length; j < end; j++) {
						Polygon noBends = new Polygon();
						for (int k = 0, nbn = 0; k < syms.length; k++) {
							EmbSym sym = syms[k];
							noBends.addPoint(sym.node.x, sym.node.y);
							for (EmbNode bn : sym.bends) {
								if (nbn == j)
									noBends.addPoint(bn.x, bn.y);
								nbn++;
							}
						}
						toAdd.add(CurveUtils.createCubicSpline(noBends.xpoints,
								noBends.ypoints, noBends.npoints, splineSteps));
					}
				}
			}
			if (tryGeomShapes) {
				if (crv.npoints == 3)
					toAdd.add(CurveUtils.createCircle(crv.xpoints[0],
							crv.ypoints[0], crv.xpoints[1], crv.ypoints[1],
							crv.xpoints[2], crv.ypoints[2], circleSegs));
				if (syms.length == 3 && crv.npoints != 3)
					toAdd.add(CurveUtils.createCircle(syms[0].node.x,
							syms[0].node.y, syms[1].node.x, syms[1].node.y,
							syms[2].node.x, syms[2].node.y, circleSegs));
				if (syms.length == 2) {
					int tot = 0;
					for (int j = 0; j < crv.npoints; j++) {
						int ip = (j + 1) % crv.npoints;
						tot += (crv.xpoints[j] * crv.ypoints[ip])
								- (crv.ypoints[j] * crv.xpoints[ip]);
					}
					toAdd.add(CurveUtils.createCircle(syms[0].node.x,
							syms[0].node.y, syms[1].node.x, syms[1].node.y,
							circleSegs, tot < 0));
				}
			}

			removeStartIntersection(crv, ptsSet);
			curves[i] = new MPolygon(crv);
			for (Polygon add : toAdd) {
				removeStartIntersection(add, ptsSet);
				curves[i].polys.add(add);
			}
		}

		if (trySplines || tryGeomShapes)
			selectBest(curves, curveLabels, ptLbs, pts, embGC);

		GeomUtils.traslateTo(curves, pts, 1, 1);

		return new EulerCode(curveLabels, curves, ptLbs, pts, ptUCs);
	}

	private static void removeStartIntersection(Polygon curve, Set<Point> pts) {
		if (curve.npoints > 0
				&& pts.contains(new Point(curve.xpoints[0], curve.ypoints[0]))) {
			int last = curve.npoints - 1;
			if (pts.contains(new Point(curve.xpoints[last], curve.ypoints[last]))) {
				int mx = (curve.xpoints[0] + curve.xpoints[last]) / 2;
				int my = (curve.ypoints[0] + curve.ypoints[last]) / 2;
				if ((mx != curve.xpoints[0] || my != curve.ypoints[0])
						&& (mx != curve.xpoints[last] || my != curve.ypoints[last])) {
					curve.addPoint(0, 0);
					for (int j = curve.npoints - 1; j > 0; j--) {
						curve.xpoints[j] = curve.xpoints[j - 1];
						curve.ypoints[j] = curve.ypoints[j - 1];
					}
					curve.xpoints[0] = mx;
					curve.ypoints[0] = my;
					curve.invalidate();
				}
			} else {
				int xl = curve.xpoints[last];
				int yl = curve.ypoints[last];
				for (int j = last; j > 0; j--) {
					curve.xpoints[j] = curve.xpoints[j - 1];
					curve.ypoints[j] = curve.ypoints[j - 1];
				}
				curve.xpoints[0] = xl;
				curve.ypoints[0] = yl;
				curve.invalidate();
			}
		}
	}

	private static void selectBest(MPolygon[] curves, char[] curveLabels,
			int[] ptLbs, Point[] pts, EmbGC embGC) {
		final int sLimit = 32;
		int[] sizes = new int[curves.length];
		long max = 1;
		for (int i = 0; i < sizes.length; i++) {
			int size = curves[i].polys.size();
			sizes[i] = size;
			max *= size;
		}

		ArrayList<byte[]> alts = new ArrayList<byte[]>();
		for (long i = max - 1, skip = Math.max(0, max - sLimit); i >= 0; i--) {
			byte[] sel = new byte[curves.length];
			alts.add(sel);
			long v = i;
			for (int j = 0; j < sel.length; j++) {
				sel[j] = (byte) (v % sizes[j]);
				v = v / sizes[j];
			}
			if (i == skip)
				i = Math.min(i, sLimit);
		}
		Collections.sort(alts, ALT_CMP);

		for (byte[] sel : alts) {
			for (int i = 0; i < sel.length; i++)
				curves[i].setSelected(sel[i]);
			try {
				if ((new EulerCode(curveLabels, curves, ptLbs, pts))
						.getGaussCodeRBC().equals(embGC))
					return;
			} catch (Exception e) {
			}
		}
	}

	private static final Comparator<byte[]> ALT_CMP = new Comparator<byte[]>() {
		@Override
		public int compare(byte[] o1, byte[] o2) {
			int n1 = nNotZero(o1);
			int n2 = nNotZero(o2);
			if (n1 < n2)
				return 1;
			if (n2 > n1)
				return -1;
			int c = Integer.compare(sum(o2), sum(o1));
			if (c != 0)
				return c;
			for (int i = 0; i < o1.length; i++) {
				c = Integer.compare(o2[i], o1[i]);
				if (c != 0)
					return c;
			}
			return Integer.compare(o2.hashCode(), o1.hashCode());
		}

		int nNotZero(byte[] o) {
			int tot = 0;
			for (byte b : o)
				if (b != 0)
					tot++;
			return tot;
		}

		int sum(byte[] o) {
			int tot = 0;
			for (byte b : o)
				tot += b;
			return tot;
		}
	};

	private static class MPolygon extends Polygon {
		static final long serialVersionUID = 3984989778857130598L;
		List<Polygon> polys;

		MPolygon(Polygon poly) {
			super();
			polys = new ArrayList<Polygon>();
			polys.add(poly);
			setSelected(0);
		}

		void setSelected(int index) {
			Polygon poly = polys.get(index);
			xpoints = poly.xpoints;
			ypoints = poly.ypoints;
			npoints = poly.npoints;
			bounds = null;
		}
	}

	private static class EmbGC extends GaussCodeRBC {
		private static final long serialVersionUID = -5098774916870771441L;
		final ArrayList<EmbNode> nodes = new ArrayList<EmbNode>();
		final HashMap<String, EmbNode> labeledNodes = new HashMap<String, EmbNode>();

		EmbGC(char[] curveLabels, EmbSym[][] gaussCode) {
			super(curveLabels, gaussCode);
		}

		EmbNode newNode() {
			EmbNode en = new EmbNode();
			nodes.add(en);
			return en;
		}

		EmbNode newNode(String label, boolean returnIfExist) {
			EmbNode existant = labeledNodes.get(label);
			if (existant != null) {
				if (returnIfExist)
					return existant;
				else
					throw new IllegalArgumentException("Node with label \""
							+ label + "\" already present.");
			}
			EmbNode en = new EmbNode();
			nodes.add(en);
			labeledNodes.put(label, en);
			return en;
		}

		public EmbNode newNode(String label, boolean returnIfExist, EmbNode en) {
			EmbNode existant = labeledNodes.get(label);
			if (existant != null) {
				if (returnIfExist)
					return existant;
				else
					throw new IllegalArgumentException("Node with label \""
							+ label + "\" already present.");
			}
			nodes.add(en);
			labeledNodes.put(label, en);
			return en;
		}

		String exportAdjList() {
			int nNodes = nodes.size();
			StringBuilder out = new StringBuilder("N=" + nNodes + "\n");
			HashMap<EmbNode, Integer> nodeIndices = new HashMap<EmbNode, Integer>();
			for (int i = 0; i < nNodes; i++)
				nodeIndices.put(nodes.get(i), i);
			for (int i = 0; i < nNodes; i++) {
				out.append(i + ":");
				for (EmbEdge ns : nodes.get(i).adjList) {
					out.append(" " + nodeIndices.get(ns.target));
				}
				out.append(" -1\n");
			}
			return out.toString();
		}
	}

	private static class EmbSym extends Symbol {
		private static final long serialVersionUID = 1976455022566872033L;
		final EmbNode node;
		final ArrayList<EmbNode> bends;

		EmbSym(Symbol sym, EmbNode node) {
			super(sym.getLabel(), sym.getSign());
			this.node = node;
			this.bends = new ArrayList<EmbNode>();
		}

	}

	private static class EmbNodeU extends EmbNode {
		Set<String> ucurves;

		public EmbNodeU() {
			super();
			ucurves = new LinkedHashSet<String>();
		}
	}

	private static class EmbNode {
		static final Comparator<EmbEdge> EDGE_CMP = new Comparator<EmbEdge>() {
			@Override
			public int compare(EmbEdge e1, EmbEdge e2) {
				// '*' = 42, '+' = 43, ',' = 44, '-' = 45, '.' = 46
				if (e1.incoming == e2.incoming)
					return Character.compare(e2.sign, e1.sign);
				else if (e1.incoming)
					return -1;
				else
					return 1;
			}
		};
		final ArrayList<EmbEdge> adjList;
		int x;
		int y;

		EmbNode() {
			adjList = new ArrayList<EmbEdge>();
		}

		void setLocation(int x, int y) {
			this.x = x;
			this.y = y;
		}

		void addEdge(char sign, boolean incoming, EmbNode target) {
			adjList.add(new EmbEdge(sign, incoming, target));
			Collections.sort(adjList, EDGE_CMP);
		}
	}

	private static class EmbEdge {
		final boolean incoming;
		final char sign;
		final EmbNode target;

		EmbEdge(char sign, boolean incoming, EmbNode target) {
			this.sign = sign;
			this.incoming = incoming;
			this.target = target;
		}
	}

	private static class UnorderdNodePair {
		final EmbNode first;
		final EmbNode second;

		UnorderdNodePair(EmbNode first, EmbNode second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public int hashCode() {
			return (31 + (first == null ? 0 : first.hashCode()))
					* (31 + (second == null ? 0 : second.hashCode()));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof UnorderdNodePair))
				return false;
			UnorderdNodePair other = (UnorderdNodePair) obj;
			if ((first == null ? other.second == null : first
					.equals(other.second))
					&& (second == null ? other.first == null : second
							.equals(other.first)))
				return true;
			if (first == null) {
				if (other.first != null)
					return false;
			} else if (!first.equals(other.first))
				return false;
			if (second == null) {
				if (other.second != null)
					return false;
			} else if (!second.equals(other.second))
				return false;
			return true;
		}
	}
}
