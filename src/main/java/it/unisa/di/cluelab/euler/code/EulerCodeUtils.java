/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPoint;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointRef;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointsPolygon;
import it.unisa.di.cluelab.euler.code.EulerCode.Segment;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;

/**
 * @author Mattia De Rosa
 */
public class EulerCodeUtils {
	private static final String HEADER_CURVES = "EulerSketchCurves_0.3";
	private static final String HEADER_HTML = "<!-- EulerSketchCode_0.2 -->";
	private static final String HEADER_TXT = "EulerSketchCode_0.2";
	private static final String HEADER_GAUSS_HTML = "<!-- EulerSketchGaussCode_0.2 -->";
	private static final String HEADER_GAUSS_TXT = "EulerSketchGaussCode_0.2";

	/**
	 * This class is not instantiable.
	 */
	private EulerCodeUtils() {
	}

	/**
	 * @param curvesFile
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	public static EulerCode loadCurves(File curvesFile) throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader(curvesFile));
		String line = br.readLine();
		if (!HEADER_CURVES.equals(line)
				&& !"EulerSketchCurves_0.2".equals(line)) {
			br.close();
			throw new ParseException("Unsupported file format.", 1);
		}		
		String labels = "";
		ArrayList<Polygon> crvs = new ArrayList<Polygon>();
		int[] incidentPointLabels = null;
		Point2D[] incidentPointPositions = null;
		Set<String>[] incidentPointUnderCurves = null;
		for(int ln = 2; (line = br.readLine()) != null; ln++) {
			String[] columns = line.split(";");
			if (columns[0].equals("POINTS")) {
				if (incidentPointLabels != null) {
					br.close();
					throw new ParseException(
							"Duplicate POINTS definition on line " + ln + ".",
							ln);
				}
				try {
					incidentPointLabels = new int[columns.length - 1];
					incidentPointPositions = new Point2D[incidentPointLabels.length];
					incidentPointUnderCurves = new Set[incidentPointLabels.length];
					
					for (int i = 1; i < columns.length; i++) {
						String[] sp = columns[i].split(":");
						int im = i - 1;
						incidentPointLabels[im] = Integer.parseInt(sp[0]);
						String[] xy = sp[1].split(",");
						incidentPointPositions[im] = new Point2D.Double(
								Double.parseDouble(xy[0]),
								Double.parseDouble(xy[1]));
						incidentPointUnderCurves[im] = new LinkedHashSet<String>(
								Arrays.asList(sp[2].split(",")));
					}
				} catch (Exception e) {
					br.close();
					throw new ParseException("Unparseable line " + ln + ".", ln);
				}
				continue;
			}
			if(columns.length < 4 || columns[0].length() != 1) {
				br.close();
				throw new ParseException("Unparseable line " + ln + ".", ln);
			}
			char curveLabel = columns[0].charAt(0);
			if (labels.indexOf(curveLabel) != -1) {
				br.close();
				throw new ParseException("Duplicate curve label " + curveLabel,
						ln);
			}
			labels += curveLabel;
			int[] xs = new int[columns.length - 1];
			int[] ys = new int[xs.length];
			for(int i = 0; i < xs.length; i++) {
				String[] xy = columns[i + 1].split(",");
				try {
					xs[i] = Integer.parseInt(xy[0]);
					ys[i] = Integer.parseInt(xy[1]);
				} catch(RuntimeException re) {
					br.close();
					throw new ParseException("Unparseable line " + ln + ", column " + (i + 2) + ".", ln);
				}
				if(xy.length != 2 || xs[i] < 0 || ys[i] < 0) {
					br.close();
					throw new ParseException("Unparseable line " + ln + ", column " + (i + 2) + ".", ln);
				}
			}
			// TODO launch an exception if the curve is too small
			// duplicated points skipped by IncidentPointsPolygon
			crvs.add(new Polygon(xs, ys, xs.length));
		}
		br.close();
		return new EulerCode(labels.toCharArray(),
				crvs.toArray(new Polygon[crvs.size()]), incidentPointLabels,
				incidentPointPositions, incidentPointUnderCurves);
	}

	public static void saveCurves(EulerCode eulerCode, File curvesFile) throws IOException {
		if(eulerCode == null || eulerCode.getCurves().isEmpty())
			throw new NullPointerException("Empty code");
		
		NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(2);
		
		LinkedHashMap<Integer, StringBuilder> ppos = new LinkedHashMap<Integer, StringBuilder>();
		for (Entry<Integer, IncidentPoint> e : eulerCode.getIncidentPoints()
				.entrySet()) {
			Integer n = e.getKey();
			IncidentPoint p = e.getValue();
			if (n >= 0)
				ppos.put(
						n,
						new StringBuilder(":" + nf.format(p.x) + ","
								+ nf.format(p.y) + ":"));
		}
		PrintWriter pw = new PrintWriter(new FileWriter(curvesFile), true);
		pw.println(HEADER_CURVES);
		for(Entry<Character, IncidentPointsPolygon> e : eulerCode.getCurves().entrySet()) {
			Character c = e.getKey();
			pw.print(c);
			IncidentPointsPolygon curve = e.getValue();
			for(int i = 0; i < curve.npoints; i++) pw.print(";" + curve.xpoints[i] + "," + curve.ypoints[i]);
			pw.println();
			for (Entry<IncidentPointRef, Integer> ep : curve
					.getOrderedIncidentPointRefs()) {
				int n = ep.getValue();
				if (ep.getKey().isUnder()) {
					StringBuilder s = ppos.get(Math.abs(n));
					if (s.charAt(s.length() - 1) != ':')
						s.append(',');
					s.append(n < 0 ? "-" + c : c.toString());
				}
			}
		}
		pw.print("POINTS");
		for (Entry<Integer, StringBuilder> e : ppos.entrySet())
			pw.print(";" + e.getKey() + e.getValue());
		pw.close();
	}

	public static void saveCode(EulerCode eulerCode, File destination,
			boolean html) throws IOException {
		String code;
		if (eulerCode == null
				|| (code = eulerCode.getCode(true, false, html)) == null)
			throw new NullPointerException("Empty code");
		PrintWriter pw = new PrintWriter(new FileWriter(destination), true);
		pw.println(html ? HEADER_HTML : HEADER_TXT);
		pw.println(code);
		pw.close();
	}

	public static void saveGaussCode(EulerCode eulerCode, File destination,
			boolean html) throws IOException {
		String code;
		if (eulerCode == null
				|| (code = eulerCode.getGaussCode(false, html)) == null)
			throw new NullPointerException("Empty code");
		PrintWriter pw = new PrintWriter(new FileWriter(destination), true);
		pw.println(html ? HEADER_GAUSS_HTML : HEADER_GAUSS_TXT);
		pw.println(code);
		pw.close();
	}

	public static class LCode {
		public final String code;
		public final boolean gauss;
		public final boolean html;

		public LCode(String code, boolean gauss, boolean html) {
			this.code = code;
			this.gauss = gauss;
			this.html = html;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + ((code == null) ? 0 : code.hashCode());
			result = prime * result + (gauss ? 1231 : 1237);
			return prime * result + (html ? 1231 : 1237);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof LCode))
				return false;
			LCode other = (LCode) obj;
			if (code == null) {
				if (other.code != null)
					return false;
			} else if (!code.equals(other.code))
				return false;
			if (gauss != other.gauss)
				return false;
			if (html != other.html)
				return false;
			return true;
		}
	}

	public static LCode loadCodeAutodetect(File codeFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(codeFile));
		String header = br.readLine();
		boolean html, gauss;
		if (HEADER_HTML.equals(header)) {
			html = true;
			gauss = false;
		} else if (HEADER_GAUSS_HTML.equals(header)) {
			html = true;
			gauss = true;
		} else if (HEADER_TXT.equals(header)) {
			html = false;
			gauss = false;
		} else if (HEADER_GAUSS_TXT.equals(header)) {
			html = false;
			gauss = true;
		} else {
			br.close();
			throw new IOException("Unsupported file format.");
		}
		StringBuffer infile = new StringBuffer();
		for (String line; (line = br.readLine()) != null;) {
			infile.append(line);
			infile.append('\n');
		}
		br.close();
		return new LCode(infile.toString(), gauss, html);
	}

	public static String stripHtml(String code) {
		return code.replace("\n", "").replace("<sub>", "_")
				.replace("</sub>", " ").replace("&empty;", "0")
				.replaceAll("<br[ ]*[/]?>", "\n").replaceAll("<[^>]+>", "");
	}

	public static class CodeZones {
		public final Map<Character, List<Segment>> code;
		public final List<Zone> zones;

		public CodeZones(Map<Character, List<Segment>> code) {
			this.code = code;
			this.zones = EulerCode.computeZones(code.values());
		}

		public CodeZones(String codeString) throws ParseException {
			boolean html = codeString.startsWith("<html>");
			String[] rows = codeString.split("\n");
			code = new LinkedHashMap<Character, List<Segment>>();
			int nRows = rows.length;
			for (; nRows > 0 && rows[nRows - 1].trim().isEmpty(); nRows--)
				;
			for (int ln = 1; ln <= nRows; ln++) {
				String line = rows[ln - 1];
				if (line.isEmpty())
					throw new ParseException("Unparseable line " + ln + ".", ln);
				if (html) {
					if (ln == 0) {
						if (!line.equals("<html>"))
							throw new ParseException("No <html> tag.", ln);

						continue;
					} else if (ln == 1) {
						if (!line.equals("<body>"))
							throw new ParseException("No <body> tag.", ln);

						continue;
					} else if (line.equals("</body>") || line.equals("</html>"))
						continue;
				}
				String[] columns = line.split(" |</sub>");
				if (columns.length == 0 || columns[0].length() != 2
						|| columns[0].charAt(1) != ':')
					throw new ParseException("Unparseable line " + ln + ".", ln);
				char curveLabel = columns[0].charAt(0);
				if (html && !columns[columns.length - 1].equals("<br/>"))
					throw new ParseException("Unparseable line " + ln + ".", ln);

				ArrayList<Segment> curveSegments = new ArrayList<Segment>();
				if (columns.length != 1) {
					int prevPoint = 0;
					int firstPoint = 0;
					String prevContCurves = null;
					for (int i = 1, last = html ? columns.length - 1
							: columns.length; i < last; i++) {
						String[] seg = columns[i].split(html ? "<sub>" : "_");
						if (seg.length != 2)
							throw new ParseException("Unparseable line " + ln
									+ ", column " + (i + (html ? 4 : 2)) + ".",
									ln);
						try {
							int point = Integer.parseInt(seg[0]);
							if (i != 1) {
								curveSegments.add(new Segment(prevPoint, point,
										curveLabel, prevContCurves));
							} else {
								firstPoint = point;
							}
							prevPoint = point;
							prevContCurves = seg[1].equals("0")
									|| seg[1].equals("&empty;") ? "" : seg[1];
						} catch (NumberFormatException nfe) {
							throw new ParseException("Unparseable line " + ln
									+ ", column " + (i + 2) + ".", ln);
						}
					}
					curveSegments.add(new Segment(prevPoint, firstPoint,
							curveLabel, prevContCurves));
				}
				if (code.put(curveLabel, curveSegments) != null)
					throw new ParseException("Duplicate curve label "
							+ curveLabel + ".", ln);
			}
			try {
				this.zones = EulerCode.computeZones(code.values());
			} catch (RuntimeException re) {
				throw new ParseException(re.getMessage(), -1);
			}
		}

		public String getCodeString() {
			StringBuilder out = new StringBuilder();
			for (Entry<Character, List<Segment>> e : code.entrySet()) {
				out.append(e.getKey());
				out.append(':');
				for (Segment seg : e.getValue()) {
					out.append(' ');
					out.append(seg.p1);
					out.append('_');
					out.append(seg.contCurves.length() == 0 ? "0"
							: seg.contCurves);
				}
				out.append('\n');
			}
			out.setLength(Math.max(0, out.length() - 1));
			return out.toString();
		}
	}
}
