/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.zoneGeneration;

import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.ZonesSet;

import java.text.Normalizer;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jgrapht.alg.BiconnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/**
 * @author Mattia De Rosa
 */
public class DisconnectedEDs {

	private DisconnectedEDs() {
	}

	private static <K> Set<Set<K>> minusOneSets(Set<K> zone) {
		LinkedHashSet<Set<K>> res = new LinkedHashSet<Set<K>>();
		for (int i = 0, size = zone.size(); i < size; i++) {
			LinkedHashSet<K> dst = new LinkedHashSet<K>();
			int n = 0;
			for (K c : zone)
				if (n++ != i)
					dst.add(c);
			res.add(dst);
		}
		return res;
	}

	public static Map<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>> disconnectedEDZones(
			Set<Set<String>> zones) {
		SimpleGraph<Set<String>, DefaultEdge> g = new SimpleGraph<Set<String>, DefaultEdge>(
				DefaultEdge.class);
		// initialize graph vertexes
		for (Set<String> zone : zones)
			g.addVertex(zone);
		g.addVertex(Collections.<String> emptySet());

		// initialize graph edges
		for (Set<String> zone : zones) {
			Set<Set<String>> min = minusOneSets(zone);
			while (!min.isEmpty()) {
				boolean noNewEdge = true;
				for (Set<String> dst : min) {
					if (g.containsVertex(dst)) {
						g.addEdge(zone, dst);
						noNewEdge = false;
					}
				}
				Set<Set<String>> oldMin = min;
				min = new LinkedHashSet<Set<String>>();
				if (noNewEdge) {
					for (Set<String> mdst : oldMin)
						min.addAll(minusOneSets(mdst));
				}
			}
		}

		BiconnectivityInspector<Set<String>, DefaultEdge> bi = new BiconnectivityInspector<Set<String>, DefaultEdge>(
				g);
		HashMap<Set<Set<String>>, Set<String>> withinMap = new HashMap<Set<Set<String>>, Set<String>>();
		for (Set<String> cp : bi.getCutpoints()) {
			for (Set<Set<String>> comp : bi.getBiconnectedVertexComponents(cp)) {
				boolean allCont = true;
				for (Set<String> zone : comp) {
					if (!zone.containsAll(cp)) {
						allCont = false;
						break;
					}
				}
				if (allCont) {
					Set<String> or = withinMap.put(comp, cp);
					if (or != null)
						throw new IllegalStateException("Multiple within for "
								+ comp + ": " + or + ", " + cp + ".");
				}
			}
		}

		HashMap<Set<Entry<Set<String>, Integer>>, Set<String>> edMap = new HashMap<Set<Entry<Set<String>, Integer>>, Set<String>>();
		DualHashBidiMap<Set<String>, Entry<Set<String>, Integer>> renMap = new DualHashBidiMap<Set<String>, Entry<Set<String>, Integer>>();
		renMap.put(
				Collections.<String> emptySet(),
				new AbstractMap.SimpleEntry<Set<String>, Integer>(Collections
						.<String> emptySet(), 0));
		Set<Set<Set<String>>> bvc = bi.getBiconnectedVertexComponents();
		for (Set<Set<String>> comp : bvc) {
			Set<String> within = withinMap.get(comp);
			if (within == null)
				within = Collections.<String> emptySet();
			LinkedHashSet<Entry<Set<String>, Integer>> compR = new LinkedHashSet<Entry<Set<String>, Integer>>();
			for (Set<String> r : comp) {
				if (!r.equals(within)) {
					Set<String> nr = new LinkedHashSet<String>(r);
					nr.removeAll(within);
					Entry<Set<String>, Integer> ent;
					int i = 0;
					do {
						ent = new AbstractMap.SimpleEntry<Set<String>, Integer>(
								nr, i++);
					} while (renMap.containsValue(ent));
					compR.add(ent);
					Entry<Set<String>, Integer> or = renMap.put(r, ent);
					if (or != null)
						throw new IllegalStateException("Duplicate rename for "
								+ r + ": " + or + ", " + nr + ".");
				}
			}
			if (edMap.put(compR, within) != null)
				throw new IllegalStateException("Duplicate within for " + compR
						+ ": " + within + ".");
		}

		LinkedHashMap<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>> res = new LinkedHashMap<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>>();
		// single curves at the end
		LinkedHashMap<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>> last = new LinkedHashMap<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>>();
		for (Entry<Set<Entry<Set<String>, Integer>>, Set<String>> e : edMap
				.entrySet()) {
			Set<Entry<Set<String>, Integer>> zs = e.getKey();
			Entry<Set<String>, Integer> ren = renMap.get(e.getValue());
			if (ren == null)
				throw new IllegalStateException("No rename for " + e.getValue()
						+ ".");
			if (zs.size() == 1 && zs.iterator().next().getKey().size() == 1)
				last.put(zs, ren);
			else
				res.put(zs, ren);
		}
		res.putAll(last);
		return res;
	}

	public static Map<Set<Set<String>>, Set<String>> renameDuplicateCurves(
			Map<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>> disconnectedEDZones) {
		HashSet<String> usedLabels = new HashSet<String>();
		HashMap<Entry<Set<String>, Integer>, Set<String>> renMap = new HashMap<Entry<Set<String>, Integer>, Set<String>>();
		renMap.put(new AbstractMap.SimpleEntry<Set<String>, Integer>(
				Collections.<String> emptySet(), 0), Collections
				.<String> emptySet());
		for (Set<Entry<Set<String>, Integer>> comp : disconnectedEDZones
				.keySet()) {
			HashSet<String> curLabels = new HashSet<String>();
			for (Entry<Set<String>, Integer> zone : comp)
				curLabels.addAll(zone.getKey());

			HashSet<String> intersect = new HashSet<String>(curLabels);
			intersect.retainAll(usedLabels);
			usedLabels.addAll(curLabels);

			HashMap<String, String> trMap = new HashMap<String, String>();
			for (String c : intersect) {
				String first = String.valueOf(c.charAt(0));
				String oth = c.substring(1);
				char diacriticalMark = '\u0307';
				String newC;
				do {
					newC = Normalizer.normalize(first + diacriticalMark++,
							Normalizer.Form.NFC).charAt(0)
							+ oth;
				} while (usedLabels.contains(newC));
				trMap.put(c, newC);
				usedLabels.add(newC);
			}

			for (Entry<Set<String>, Integer> zone : comp) {
				LinkedHashSet<String> transf = new LinkedHashSet<String>();
				for (String c : zone.getKey()) {
					String mc = trMap.get(c);
					transf.add(mc == null ? c : mc);
				}
				if (renMap.put(zone, transf) != null)
					throw new IllegalStateException("Duplicate rename for "
							+ zone + ":  " + transf + ".");
			}
		}

		LinkedHashMap<Set<Set<String>>, Set<String>> res = new LinkedHashMap<Set<Set<String>>, Set<String>>();
		for (Entry<Set<Entry<Set<String>, Integer>>, Entry<Set<String>, Integer>> e : disconnectedEDZones
				.entrySet()) {
			Set<String> within = renMap.get(e.getValue());
			if (within == null)
				throw new IllegalStateException("No rename for " + e.getValue()
						+ ".");
			LinkedHashSet<Set<String>> compTr = new LinkedHashSet<Set<String>>();
			for (Entry<Set<String>, Integer> zone : e.getKey()) {
				Set<String> ren = renMap.get(zone);
				if (ren == null)
					throw new IllegalStateException("No rename for " + zone
							+ ".");
				compTr.add(ren);
			}
			if (res.put(compTr, within) != null)
				throw new IllegalStateException(
						"Duplicate within for (renamed) " + e.getKey() + "  "
								+ compTr + ": " + within + ".");
		}
		return res;
	}

	public static EulerCodeRBC combine(
			List<Entry<EDData, Set<String>>> disconnectedEDs) {
		GaussCodeRBC[] gaussCodeRBCs = new GaussCodeRBC[disconnectedEDs.size()];
		SegmentCode[][] withins = new SegmentCode[gaussCodeRBCs.length][];
		SegmentCode[][] outers = new SegmentCode[gaussCodeRBCs.length][];

		HashMap<Set<String>, SegmentCode[]> zoneMapping = new HashMap<Set<String>, SegmentCode[]>();
		for (int i = 0, maxLbl = 0; i < gaussCodeRBCs.length; i++) {
			Entry<EDData, Set<String>> e = disconnectedEDs.get(i);
			EDData edd = e.getKey();
			gaussCodeRBCs[i] = increaseLbl(maxLbl, edd.getGaussCodeRBC());
			outers[i] = increaseLbl(maxLbl, edd.getOuter());

			Symbol[][] gc = gaussCodeRBCs[i].getGaussCode();
			if (gc.length == 1 && gc[0].length == 1) {
				Symbol s = new Symbol(gc[0][0].getLabel(), '+');
				zoneMapping.put(Collections.singleton(String
						.valueOf(gaussCodeRBCs[i].getCurveLabels()[0])),
						new SegmentCode[] { new SegmentCode(s, s, '-') });
			} else {
				List<SegmentCode[]> rbc = gaussCodeRBCs[i]
						.getRegionBoundaryCode();
				List<List<List<String>>> zones = ZonesSet.computeZonesSet(
						gaussCodeRBCs[i].getGaussCode(), rbc, outers[i],
						gaussCodeRBCs[i].getCurveLabels());
				for (int j = 0; j < rbc.size(); j++) {
					zoneMapping.put(new HashSet<String>(zones.get(j).get(0)),
							rbc.get(j));
				}
			}
			maxLbl += edd.getMaxSymLbl();
		}

		for (int i = 0; i < withins.length; i++) {
			Set<String> wt = disconnectedEDs.get(i).getValue();
			if (!wt.isEmpty())
				withins[i] = zoneMapping.get(wt);
		}

		return new EulerCodeRBC(gaussCodeRBCs, withins, outers);
	}

	private static GaussCodeRBC increaseLbl(int toAdd, GaussCodeRBC gaussCodeRBC) {
		Symbol[][] orGaussCode = gaussCodeRBC.getGaussCode();
		Symbol[][] gaussCode = new Symbol[orGaussCode.length][];
		for (int i = 0; i < gaussCode.length; i++) {
			gaussCode[i] = new Symbol[orGaussCode[i].length];
			for (int j = 0; j < gaussCode[i].length; j++) {
				gaussCode[i][j] = increaseLbl(toAdd, orGaussCode[i][j]);
			}
		}
		return new GaussCodeRBC(gaussCodeRBC.getCurveLabels(), gaussCode);
	}

	private static SegmentCode[] increaseLbl(int toAdd, SegmentCode[] scs) {
		SegmentCode[] res = new SegmentCode[scs.length];
		for (int i = 0; i < scs.length; i++) {
			res[i] = new SegmentCode(
					increaseLbl(toAdd, scs[i].getFirstSymbol()), increaseLbl(
							toAdd, scs[i].getSecondSymbol()),
					scs[i].getDirection());
		}
		return res;
	}

	private static Symbol increaseLbl(int toAdd, Symbol symbol) {
		return new Symbol(String.valueOf(Integer.parseInt(symbol.getLabel())
				+ toAdd), symbol.getSign());
	}

}
