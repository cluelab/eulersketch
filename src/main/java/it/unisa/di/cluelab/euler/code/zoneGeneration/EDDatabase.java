/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.zoneGeneration;

import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.RegionCode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.iterators.PermutationIterator;

/**
 * @author Mattia De Rosa
 */
public class EDDatabase {
	private static final Comparator<Set<String>> SET_COMPARATOR = new Comparator<Set<String>>() {
		@Override
		public int compare(Set<String> o1, Set<String> o2) {
			Iterator<String> it1 = o1.iterator(), it2 = o2.iterator();
			while (it1.hasNext() && it2.hasNext()) {
				int c = it1.next().compareTo(it2.next());
				if (c != 0)
					return c;
			}
			if (it1.hasNext())
				return 1;
			if (it2.hasNext())
				return -1;
			return 0;
		}
	};

	private static class Node implements Serializable {
		private static final long serialVersionUID = -2511577917359882250L;
		TreeMap<String, Node> next;
		ArrayList<EDData> eddatas;
	}

	private Node root;
	private String dbfile;

	public EDDatabase() {
		this.root = new Node();
	}

	public EDDatabase(String dbfile) throws IOException {
		this.dbfile = dbfile;
		ObjectInputStream in = new ObjectInputStream(
				new FileInputStream(dbfile));
		try {
			this.root = (Node) in.readObject();
		} catch (ClassNotFoundException e) {
			in.close();
			throw new IOException(e);
		}
		in.close();
	}

	public EDDatabase(InputStream txtdb) throws ParseException {
		this(txtdb, null);
	}

	public EDDatabase(InputStream txtdb, String dbfile) throws ParseException {
		this.dbfile = dbfile;
		this.root = new Node();
		Scanner scan = new Scanner(txtdb);
		scan.useDelimiter("\n\n");
		while (scan.hasNext()) {
			String[] blocks = scan.next().split("\n", 3);

			String[] sets = blocks[0].split(",");
			ArrayList<String> zones = new ArrayList<String>(sets.length - 1);
			LinkedHashMap<Set<String>, Integer> zoneCounts = new LinkedHashMap<Set<String>, Integer>();
			for (int i = 0; i < sets.length; i++) {
				String[] sp = sets[i].split("-");
				String zone = sp[0];
				if (!zone.isEmpty())
					zones.add(zone);
				LinkedHashSet<String> zoneSt = new LinkedHashSet<String>();
				for (int j = 0; j < zone.length(); j++) {
					zoneSt.add(zone.substring(j, j + 1));
				}
				zoneCounts.put(zoneSt, Integer.parseInt(sp[1]));
			}
			addED(root, zones, new EDData(new GaussCodeRBC(blocks[2]),
					RegionCode.regionCode(blocks[1]), zoneCounts), false);
		}
		scan.close();
	}

	public List<EDData> getEDs(Set<Set<String>> zones) {
		Entry<Set<Set<String>>, Map<String, String>> cf = canonicalForm(zones);
		List<EDData> res = getEDs(root, zones2strings(cf.getKey()));
		if (res == null)
			return Collections.emptyList();
		else
			return rename(res, cf.getValue());
	}

	public List<EDData> getSuperSetEDs(Set<Set<String>> zones) {
		return getSuperSetEDs(zones, -1);
	}

	public List<EDData> getSuperSetEDs(Set<Set<String>> zones, int maxCurves) {
		Entry<Set<Set<String>>, Map<String, String>> cf = canonicalForm(zones);
		List<String> zonesCfs = zones2strings(cf.getKey());
		char maxc;
		if (maxCurves > Character.MAX_VALUE - '@') {
			maxc = Character.MAX_VALUE;
		} else if (maxCurves >= 0) {
			maxc = (char) ('@' + maxCurves);
		} else {
			maxc = 'A';
			for (String z : zonesCfs) {
				char c = z.charAt(z.length() - 1);
				if (c > maxc)
					maxc = c;
			}
		}
		return rename(getSuperSetEDs(root, zonesCfs, maxc), cf.getValue());
	}

	public boolean addED(EDData ed) {
		LinkedHashSet<Set<String>> zones = new LinkedHashSet<Set<String>>(ed
				.getRegionCount().keySet());
		zones.remove(Collections.EMPTY_SET);
		Entry<Set<Set<String>>, Map<String, String>> cf = canonicalForm(zones);
		EDData cfed = rename(ed, MapUtils.invertMap(cf.getValue()));
		synchronized (root) {
			return addED(root, zones2strings(cf.getKey()), cfed, true);
		}
	}

	public boolean addED(EDData ed, boolean writeDB) throws IOException {
		synchronized (root) {
			boolean res = addED(ed);
			if (writeDB)
				writeDB();
			return res;
		}
	}

	public void writeDB() throws IOException {
		synchronized (root) {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(dbfile));
			out.writeObject(root);
			out.close();
		}
	}

	static Entry<Set<Set<String>>, Map<String, String>> canonicalForm(
			Set<Set<String>> inZones) {
		LinkedHashSet<String> orCurvesSet = new LinkedHashSet<String>();
		for (Set<String> zone : inZones)
			orCurvesSet.addAll(zone);
		List<String> orCurves = new ArrayList<String>(orCurvesSet);
		// ensure that each zone is a comparable treeset
		Set<Set<String>> edzones = rename(inZones, orCurves, orCurves);

		List<String> cnCurves = new ArrayList<String>(orCurves.size());
		for (char c = 'A', end = (char) ('A' + orCurves.size()); c < end; c++)
			cnCurves.add(String.valueOf(c));

		Set<Set<String>> cn = null;
		List<String> cp = null;
		for (PermutationIterator<String> pit = new PermutationIterator<String>(
				cnCurves); pit.hasNext();) {
			List<String> p = pit.next();
			Set<Set<String>> rn = rename(edzones, orCurves, p);
			if (cn == null) {
				cn = rn;
				cp = p;
			} else {
				boolean cnRnEq = cn.equals(rn);
				boolean equals = true;
				for (Iterator<Set<String>> cnit = cn.iterator(), rnit = rn
						.iterator(); cnit.hasNext();) {
					int c = SET_COMPARATOR.compare(cnit.next(), rnit.next());
					if (c != 0) {
						if (c > 0) {
							cn = rn;
							cp = p;
						}
						equals = false;
						break;
					}
				}
				if (equals != cnRnEq)
					throw new IllegalStateException();
			}
		}
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < cp.size(); i++)
			map.put(cp.get(i), orCurves.get(i));
		return new AbstractMap.SimpleEntry<Set<Set<String>>, Map<String, String>>(
				cn, map);
	}

	private static Set<Set<String>> rename(Set<Set<String>> set,
			List<String> original, List<String> replace) {
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < original.size(); i++)
			map.put(original.get(i), replace.get(i));

		TreeSet<Set<String>> res = new TreeSet<Set<String>>(SET_COMPARATOR);
		for (Set<String> z : set) {
			TreeSet<String> nz = new TreeSet<String>();
			for (String c : z)
				nz.add(map.get(c));
			res.add(nz);
		}
		return res;
	}

	private static List<EDData> rename(List<EDData> edds,
			Map<String, String> map) {
		EDData[] rns = new EDData[edds.size()];
		for (int i = 0; i < rns.length; i++)
			rns[i] = rename(edds.get(i), map);
		return Arrays.asList(rns);
	}

	private static EDData rename(EDData edd, Map<String, String> map) {
		char[] orLbls = edd.getGaussCodeRBC().getCurveLabels();
		char[] crLbls = new char[orLbls.length];
		HashMap<String, String> fmap = new HashMap<String, String>(map);
		char nc = 'A';
		for (int i = 0; i < orLbls.length; i++) {
			String ol = String.valueOf(orLbls[i]);
			String cl = fmap.get(ol);
			if (cl == null) {
				cl = ol;
				while (map.containsValue(cl))
					cl = String.valueOf(nc++);
				fmap.put(ol, cl);
			}
			crLbls[i] = cl.charAt(0);
		}

		HashMap<Set<String>, Integer> zoneCounts = new HashMap<Set<String>, Integer>();
		for (Entry<Set<String>, Integer> e : edd.getRegionCount().entrySet()) {
			TreeSet<String> nz = new TreeSet<String>();
			for (String c : e.getKey())
				nz.add(fmap.get(c));
			zoneCounts.put(nz, e.getValue());
		}

		EDData res = new EDData(new GaussCodeRBC(crLbls, edd.getGaussCodeRBC()
				.getGaussCode()), edd.getOuter(), zoneCounts);
		return res;
	}

	private static List<String> zones2strings(Set<Set<String>> zones) {
		ArrayList<String> res = new ArrayList<String>(zones.size());
		for (Set<String> set : zones) {
			StringBuilder sb = new StringBuilder();
			for (String c : set)
				sb.append(c);
			res.add(sb.toString());
		}
		return res;
	}

	private static boolean addED(Node node, List<String> oZones, EDData ed,
			boolean noDuplicates) {
		for (String zone : oZones) {
			Node next = null;
			if (node.next == null)
				node.next = new TreeMap<String, Node>();
			else
				next = node.next.get(zone);

			if (next == null) {
				next = new Node();
				node.next.put(zone, next);
			}
			node = next;
		}
		if (node.eddatas == null)
			node.eddatas = new ArrayList<EDData>();
		if (noDuplicates && node.eddatas.contains(ed))
			return false;
		node.eddatas.add(ed);
		return true;
	}

	private static List<EDData> getEDs(Node node, List<String> oZones) {
		for (String oset : oZones) {
			if (node.next == null)
				return null;
			node = node.next.get(oset);
			if (node == null)
				return null;
		}
		return node.eddatas;
	}

	private static List<EDData> getSuperSetEDs(Node node, List<String> oZones,
			char maxc) {
		ArrayList<EDData> res = new ArrayList<EDData>();
		boolean end = oZones.isEmpty();
		if (end && node.eddatas != null)
			res.addAll(node.eddatas);
		if (node.next != null)
			for (Entry<String, Node> e : (end ? node.next : node.next.headMap(
					oZones.get(0), true)).entrySet()) {
				String key = e.getKey();
				if (key.charAt(key.length() - 1) <= maxc)
					res.addAll(getSuperSetEDs(
							e.getValue(),
							end || !key.equals(oZones.get(0)) ? oZones : oZones
									.subList(1, oZones.size()), maxc));
			}
		return res;
	}

}