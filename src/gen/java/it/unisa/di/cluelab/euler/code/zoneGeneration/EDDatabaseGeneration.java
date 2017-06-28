/*******************************************************************************
 * Copyright (c) 2015 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package it.unisa.di.cluelab.euler.code.zoneGeneration;

import it.unisa.di.cluelab.euler.code.EulerCode;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;
import it.unisa.di.cluelab.euler.code.EulerCodeGeneration;
import it.unisa.di.cluelab.euler.code.EulerCodeGeneration.GenerationErrorException;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.Planarity;
import it.unisa.di.cluelab.euler.code.gausscode.RegionCode;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.ZonesSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.multiset.HashMultiSet;

/**
 * @author Rafiq Saleh
 */
public class EDDatabaseGeneration {
	private static PrintWriter dbWriter;
	// private static List<List<String[]>>ogps =new ArrayList<List<String[]>>();
	public static String[] ds;
	private static int m = 0;
	public static Map<Integer, List<String[]>> cyclicShiftMap = null;
	public static Map<Integer, List<String[]>> permutationMap = null;
	public static List<char[]> signs = new ArrayList<char[]>();
	public static List<String[]> dsLabelPerm = null;

	public static void GenerateSignedWords(List<String[]> unsignedOGP, String[] ds) {
		for (char[] c : signs) {
			List<String[]> list = new ArrayList<String[]>();
			list = associateSignsWithLabels(unsignedOGP, ds, c);

			Symbol[][] input = toGauss(list);
			// boolean b=false;
			if (Planarity.CheckPlanarity(input)) {
				System.out.println(printList(list));
				// b=true;
				List<SegmentCode[]> rgb = RegionCode.computeRegionBoundaryCode(input);
				List<List<Set<String>>> zonesList = new ArrayList<List<Set<String>>>();
				List<Integer> rgbSize = new ArrayList<Integer>();
				List<String[]> CurveLabelsPerm = Permutation.permuteCurveLabels(input.length);
				String[] initialCurveLabels = CurveLabelsPerm.get(0);
				for (SegmentCode[] r : rgb)// for each outer region
				{
					List<Set<String>> zones = ZonesSet.computeAllZones(input, r, rgb);
					for (int k = 0; k < CurveLabelsPerm.size(); k++) {
						String[] newCurveLabels = CurveLabelsPerm.get(k);
						List<Set<String>> zonesRenamed = Permutation.renameZoneSet(zones, initialCurveLabels,
								newCurveLabels);

						// try {
						if (containsZone(zonesRenamed, zonesList, rgbSize, r.length)) {
							break;
						} else if (k == CurveLabelsPerm.size() - 1) {
							zonesList.add(zones);
							rgbSize.add(r.length);

						}
					}
				}

			} else {
				System.out.println(printList(list) + "Non planar");
			}

			// considering only one kind of signing for the planar code input
			// if(b) break;

		}
	}

	public static void computePerm(List<String[]> ogp) throws IOException, GenerationErrorException {
		List<List<String[]>> listOfPermutedWords = new ArrayList<List<String[]>>();
		for (int i = 0; i < ogp.size(); i++) {
			String[] w = ogp.get(i);
			if (isSubword(w, i + 1, ogp)) {
				List<String[]> list = new ArrayList<String[]>();
				list.add(w);
				listOfPermutedWords.add(list);
			} else {
				List<String[]> list = permutationMap.get(w.length);
				List<String[]> list2 = new ArrayList<String[]>(list);
				listOfPermutedWords.add(renameLabels(list2, setWordLabels(w.length), w));
			}
		}
		List<String[]> myList = new ArrayList<String[]>();
		List<List<String[]>> PermutedList = new ArrayList<List<String[]>>();
		System.out.println("Checking Equivalences of OGP :" + (m++));
		PermutedList = traverseAllOGPs(listOfPermutedWords, 0, myList, PermutedList, ds);
		System.out.println(PermutedList.size());

		for (List<String[]> unsignedOGP : PermutedList) {
			GenerateSignedWords(unsignedOGP, ds);
			// break;
			insertIntoDbase(unsignedOGP, ds);
		}
	}

	public static void updatePermutationMap(int n) {

		for (int i = 0; i < n; i = i + 2) {
			int m = n - i;
			String[] w = setWordLabels(m);
			if (m == 2) {
				List<String[]> list = new ArrayList<String[]>();
				list.add(w);
				permutationMap.put(m, list);
			} else {

				String[] w2 = new String[m - 1];
				System.arraycopy(w, 1, w2, 0, w2.length);
				List<String[]> list1 = new ArrayList<String[]>();
				List<String[]> list2 = new ArrayList<String[]>();
				list1 = Permutation.permuteUpToReversals(w2, w2.length);
				for (int j = 0; j < (list1.size()); j++) {
					String[] w3 = list1.get(j);
					String[] w4 = new String[w3.length + 1];
					w4[0] = w[0];
					System.arraycopy(w3, 0, w4, 1, w3.length);
					list2.add(w4);
				}
				permutationMap.put(m, list2);
			}
		}
	}

	public static String[] setWordLabels(int n) {
		String[] wordLabels = new String[n];
		char ch = 'a';
		for (int i = 0; i < n; i++) {
			wordLabels[i] = "" + ch++;
		}
		return wordLabels;
	}

	private static boolean isSubword(String[] w, int i, List<String[]> ogp) {
		String s1 = Arrays.toString(w);
		s1 = s1.substring(1, s1.length() - 1);
		for (int j = i; j < ogp.size(); j++) {
			String s2 = Arrays.toString(ogp.get(j));
			if (s2.contains(s1))
				return true;
		}
		return false;
	}

	public static Boolean checkForRepitition(String[] w, String s) {
		int n = w.length;
		Boolean b = false;
		for (int k = 0; k < n; k++) {
			if (s.equals(w[k])) {
				for (int q = k + 1; q < n; q++) {
					if (s.equals(w[q])) {
						b = true;
						break;
					}
				}
				if (b)
					break;
			}
		}
		return b;
	}

	public static String[] getDistinctSymbols(String[] a1) {
		String[] a2 = new String[a1.length / 2];
		if (a1.length > 0) {
			int k = 0;
			Boolean b = false;
			String t = a1[0];
			a2[k] = a1[0];
			for (int i = 0; i < a1.length; i++) {
				b = false;
				if (k == a2.length - 1)
					break;
				t = a1[i];
				for (int j = 0; j <= k; j++) {
					if (t.equals(a2[j])) {
						b = true;
						break;
					}
				}
				if (!b) {
					k++;
					a2[k] = a1[i];
				}
			}
		}
		return a2;
	}

	public static void addSign(List<String[]> ogp, String label, char sign) {
		// int n = w.length;
		Boolean b = false;
		for (String[] w : ogp)
			for (int i = 0; i < w.length; i++) {
				if (label.equals(w[i])) {
					if (!b) {
						w[i] = w[i] + sign;
						b = true;
					} else {
						if (sign == '+')
							w[i] = w[i] + '-';
						else
							w[i] = w[i] + '+';

						break;
					}
				}
			}
	}

	static List<char[]> computeSignsUpToReversal(int n) {
		int m = (int) Math.pow(2, n) / 2;// remove /2 to include reversals
		List<char[]> list = new ArrayList<char[]>();
		for (int i = 0; i < m; i++) {
			char[] temp = new char[n];
			for (int j = n - 1; j >= 0; j--) {
				if (i / ((int) Math.pow(2, j)) % 2 == 0)
					temp[j] = '+';
				else
					temp[j] = '-';
			}
			list.add(temp);
		}
		return list;
	}

	public static List<String[]> associateSignsWithLabels(List<String[]> unsignedOGP, String[] labels, char[] signs) {
		List<String[]> signedOGP = new ArrayList<String[]>();
		for (String[] w : unsignedOGP) {
			String[] w2 = new String[w.length];
			System.arraycopy(w, 0, w2, 0, w.length);
			signedOGP.add(w2);
		}
		for (int i = 0; i < labels.length; i++) {
			addSign(signedOGP, labels[i], signs[i]);
		}
		return signedOGP;
	}

	public static boolean checkDisjointWords(List<String[]> ogp, String[] ds) {
		boolean b = false;
		List<String[]> list = new ArrayList<String[]>();
		for (String[] w : ogp) {
			String[] w2 = new String[w.length];
			System.arraycopy(w, 0, w2, 0, w.length);
			list.add(w2);
		}
		for (int i = 0; i < ds.length; i++) {
			list = identifyWordsContaining(ds[i], list);
		}
		if (list.size() > 1)
			b = true;
		return b;
	}

	public static List<String[]> identifyWordsContaining(String s, List<String[]> ogp) {
		boolean b = false;
		for (int i = 0; i < ogp.size(); i++) {
			String w1[] = ogp.get(i);
			if (!b && !checkForRepitition(w1, s)) {
				for (String t : w1) {
					if (s.equals(t)) {
						b = true;
						break;
					}
				}
				if (b) {
					boolean c = false;
					for (int j = i + 1; j < ogp.size(); j++) {
						String w2[] = ogp.get(j);
						for (String t : w2) {
							if (s.equals(t)) {
								String[] w3 = merge(w1, w2);
								ogp.add(w3);
								ogp.remove(w1);
								ogp.remove(w2);

								c = true;
								break;
							}
						}
						if (c)
							break;
					}
					if (c)
						break;

				}
			}
		}
		return ogp;
	}

	public static String[] merge(String[] w1, String[] w2) {
		int n = w1.length + w2.length;
		String[] s1 = new String[n];
		for (int i = 0; i < w1.length; i++) {
			s1[i] = w1[i];
		}
		int k = w1.length;
		for (int i = 0; k < n; i++) {
			s1[k] = w2[i];
			k++;
		}
		return s1;
	}

	public static boolean selfIntersection(List<String[]> ogp, String[] ds) {
		for (String s : ds)
			for (String[] w : ogp)
				if (checkForRepitition(w, s))
					return true;

		return false;
	}

	public static List<List<String[]>> traverseAllOGPs(List<List<String[]>> listOfPermutedWords, int n,
			List<String[]> myPath, List<List<String[]>> PermutedList, String[] dss) {

		if (n < listOfPermutedWords.size() - 1) {
			List<String[]> currentNodes = new ArrayList<String[]>(listOfPermutedWords.get(n));
			for (int i = 0; i < currentNodes.size(); i++) {
				List<String[]> temp = new ArrayList<String[]>(myPath);
				temp.add(currentNodes.get(i));
				int m = n + 1;
				traverseAllOGPs(listOfPermutedWords, m, temp, PermutedList, ds);
			}
		} else {
			List<String[]> currentNodes = listOfPermutedWords.get(n);
			for (String[] w : currentNodes) {

				List<String[]> temp = new ArrayList<String[]>(myPath);
				temp.add(w);
				String[][] unsignedOgp = new String[temp.size()][];
				for (int k = 0; k < temp.size(); k++)
					unsignedOgp[k] = temp.get(k);
				if (checkUnsignedPlanarity(unsignedOgp))
					if (!checkEquivalence(temp, PermutedList)) {
						PermutedList.add(temp);
						// System.out.println(Arrays.deepToString(unsignedOgp));
					}
				// else
				// System.out.println(Arrays.deepToString(unsignedOgp)+ " = "+
				// printList(temp));
				//
				// else
				// System.out.println(Arrays.deepToString(unsignedOgp)+"Non-planar");
			}
			return PermutedList;
		}
		return PermutedList;
	}

	private static boolean checkUnsignedPlanarity(String[][] unsignedOgp) {
		Symbol[][] input = new Symbol[unsignedOgp.length][];
		for (char[] c : signs) {
			List<String[]> list = new ArrayList<String[]>();
			list = associateSignsWithLabels(Arrays.asList(unsignedOgp), ds, c);
			input = toGauss(list);
			if (Planarity.CheckPlanarity(input)) {
				return true;
			}
		}
		return false;
	}

	public static boolean checkEquivalence(List<String[]> unsignedOgp, List<List<String[]>> permutedList) {
		String[] ds1 = ds;
		for (List<String[]> ogp : permutedList) {
			if (ogp.size() == unsignedOgp.size()) {
				for (String[] ds2 : dsLabelPerm)
					if (isEqualUpToRenaming(unsignedOgp, ogp, ds1, ds2))
						return true;

			}
		}
		return false;
	}

	public static boolean isEqualUpToRenaming(List<String[]> unsignedOgp, List<String[]> ogp, String[] ds1,
			String[] ds2) {
		// check if positions match upto cs and reversals
		Map<String, Integer> ds1ToPositions = new HashMap<String, Integer>();
		Map<String, Integer> ds2ToPositions = new HashMap<String, Integer>();
		for (int i = 0; i < ds1.length; i++) {
			ds1ToPositions.put(ds1[i], i);
			ds2ToPositions.put(ds2[i], i);
		}
		boolean[] matchedWords = new boolean[ogp.size()];
		for (String w[] : unsignedOgp) {
			if (!isEqualUpToPermutationOfWords(w, ogp, matchedWords, ds1ToPositions, ds2ToPositions))
				return false;
		}

		return true;
	}

	private static boolean isEqualUpToPermutationOfWords(String[] w, List<String[]> ogp, boolean[] matchedWords,
			Map<String, Integer> ds1ToPositions, Map<String, Integer> ds2ToPositions) {

		List<String[]> cyclicShiftWords = renameLabels(cyclicShiftMap.get(w.length), setWordLabels(w.length), w);

		for (int i = 0; i < ogp.size(); i++) {
			if (!matchedWords[i] && ogp.get(i).length == w.length)
				for (String[] ws : cyclicShiftWords) {
					if (matchPostions(ogp.get(i), ws, ds1ToPositions, ds2ToPositions)) {
						matchedWords[i] = true;
						return true;
					}
					if (matchPostions(ogp.get(i), reverse(ws), ds1ToPositions, ds2ToPositions)) {
						matchedWords[i] = true;
						return true;
					}
				}

		}
		return false;
	}

	private static boolean matchPostions(String[] w, String[] ws, Map<String, Integer> ds1ToPositions,
			Map<String, Integer> ds2ToPositions) {
		for (int i = 0; i < w.length; i++) {
			if (ds1ToPositions.get(w[i]) != ds2ToPositions.get(ws[i]))
				return false;
		}
		return true;
	}

	public static String printList(List<String[]> unsignedOgp) {
		String s = "";
		for (String[] w : unsignedOgp)
			s += Arrays.toString(w);

		return s;
	}

	public static String printList2(List<String[]> unsignedOgp) {
		String s = "";
		for (int i = 0; i < unsignedOgp.size(); i++) {
			String[] w = unsignedOgp.get(i);
			for (int j = 0; j < w.length; j++)
				s += w[j] + (j < w.length - 1 ? "," : "");
			s += (i < unsignedOgp.size() - 1 ? ";" : "");
		}

		return s;
	}

	public static String[] reverse(String[] w) {
		int j = 0;
		String[] w2 = new String[w.length];
		for (int k = w.length - 1; k >= 0; k--) {
			w2[j] = w[k];
			j++;
		}

		return w2;
	}

	public static void updateCyclicShiftMap(int n) {
		for (int i = 0; i < n; i = i + 2) {
			int m = n - i;
			String[] w = setWordLabels(m);
			List<String[]> list = getCyclicShift(w);
			list.add(w);
			cyclicShiftMap.put(m, list);
		}
		// return permutationMap;
	}

	public static List<String[]> renameLabels(List<String[]> ogp, String[] ds1, String[] ds2) {
		if (ds1.length != ds2.length) {
			System.out.println("Can not Rename labels: ds1!=ds2");
			return null;
		}

		List<String[]> renamedOgp = new ArrayList<String[]>();
		for (String[] w : ogp) {
			String[] w2 = new String[w.length];
			System.arraycopy(w, 0, w2, 0, w.length);
			renamedOgp.add(w2);
		}
		for (int i = 0; i < ds1.length; i++) {

			for (String[] w : renamedOgp) {
				for (int j = 0; j < w.length; j++) {
					if (w[j].equals(ds1[i])) {
						w[j] = ds2[i];
						break;
					}
				}
			}
		}
		return renamedOgp;
	}

	public static List<String[]> renameLabels(List<String[]> ogp, String[] ds) {
		List<String[]> renamedOgp = new ArrayList<String[]>();
		for (String[] w : ogp) {
			String[] w2 = new String[w.length];
			System.arraycopy(w, 0, w2, 0, w.length);
			renamedOgp.add(w2);
		}

		char c = 'a';
		for (int i = 0; i < ds.length; i++) {

			for (String[] w : renamedOgp) {
				for (int j = 0; j < w.length; j++) {
					if (w[j].equals(ds[i]))
						w[j] = "" + c;
				}
			}
			c++;
		}
		return renamedOgp;
	}

	public static List<String[]> getCyclicShift(String[] w) {
		List<String[]> list = new ArrayList<String[]>();
		int n = w.length;
		for (int i = 1; i < n; i++) {
			String[] newArray = new String[n];
			System.arraycopy(w, i, newArray, 0, n - i);
			System.arraycopy(w, 0, newArray, n - i, i);
			list.add(newArray);
		}

		return list;
	}

	static Symbol[][] toGauss(List<String[]> ls) {
		int n = ls.size();
		Symbol[][] ogp = new Symbol[n][];
		for (int i = 0; i < ls.size(); i++) {
			String[] w = ls.get(i);
			int m = w.length;
			Symbol[] w2 = new Symbol[m];
			for (int j = 0; j < m; j++) {
				String s = w[j];
				String label = s.substring(0, s.length() - 1);
				char sign = s.charAt(s.length() - 1);
				w2[j] = new Symbol(label, sign);
			}
			ogp[i] = w2;
		}

		return ogp;
	}

	public static void insertIntoDbase(List<String[]> unsignedOGP, String[] ds)
			throws IOException, GenerationErrorException {
		for (char[] c : signs) {
			List<String[]> list = new ArrayList<String[]>();
			list = associateSignsWithLabels(unsignedOGP, ds, c);
			Symbol[][] input = toGauss(list);
			boolean b = false;
			if (Planarity.CheckPlanarity(input)) {
				b = true;
				List<SegmentCode[]> rgb = RegionCode.computeRegionBoundaryCode(input);
				List<List<Set<String>>> zonesList = new ArrayList<List<Set<String>>>();
				List<Integer> rgbSize = new ArrayList<Integer>();
				List<String[]> CurveLabelsPerm = Permutation.permuteCurveLabels(input.length);
				String[] initialCurveLabels = CurveLabelsPerm.get(0);
				for (SegmentCode[] r : rgb)// for each outer region
				{
					List<Set<String>> zones = ZonesSet.computeAllZones(input, r, rgb);
					for (int k = 0; k < CurveLabelsPerm.size(); k++) {
						String[] newCurveLabels = CurveLabelsPerm.get(k);
						List<Set<String>> zonesRenamed = Permutation.renameZoneSet(zones, initialCurveLabels,
								newCurveLabels);

						// try {
						if (containsZone(zonesRenamed, zonesList, rgbSize, r.length))
							break;
						else if (k == CurveLabelsPerm.size() - 1) {
							zonesList.add(zones);
							rgbSize.add(r.length);

							EulerCodeData ec = new EulerCodeData(setCurveLabels(input.length), input, r, zones, rgb);
							dbWriter.println(normalString(ec));
						}
					}
				}

			} // considering only one kind of signing for the planar code input
			if (b)
				break;
		}
	}

	private static String normalString(EulerCodeData eco) throws GenerationErrorException {
		LinkedHashSet<Set<String>> zones = new LinkedHashSet<Set<String>>(eco.zones);
		zones.remove(Collections.singleton("?"));

		Entry<Set<Set<String>>, Map<String, String>> cf = EDDatabase.canonicalForm(zones);
		Map<String, String> map = MapUtils.invertMap(cf.getValue());
		char[] newCurveLabels = new char[eco.curveLabels.length];
		for (int i = 0; i < eco.curveLabels.length; i++) {
			newCurveLabels[i] = map.get(String.valueOf(eco.curveLabels[i])).charAt(0);
		}
		GaussCodeRBC gcrbc = new GaussCodeRBC(newCurveLabels, eco.gaussCode);

		EulerCode ecCF = EulerCodeGeneration.genFromGaussCodeRBC(gcrbc, eco.outerFace, 0, 1000, 1000, false, false,
				false);

		HashMultiSet<Set<String>> genZonesCF = new HashMultiSet<Set<String>>();
		for (Zone z : ecCF.getZones()) {
			LinkedHashSet<String> zs = new LinkedHashSet<String>();
			for (int i = 0; i < z.label.length(); i++)
				zs.add(z.label.substring(i, i + 1));
			if (genZonesCF.contains(zs))
				throw new GenerationErrorException("Duplicate zone");
			genZonesCF.add(zs, z.intlines.size() + z.outlines.size());
		}

		HashMultiSet<Set<String>> cfZonesMl = new HashMultiSet<Set<String>>();
		for (Set<String> z : eco.zones) {
			TreeSet<String> nz = new TreeSet<String>();
			for (String c : z) {
				if (!c.equals("?"))
					nz.add(map.get(c));
			}
			cfZonesMl.add(nz);
		}

		HashSet<Set<String>> genZonesCFset = new HashSet<Set<String>>(genZonesCF);
		genZonesCFset.remove(Collections.EMPTY_SET);
		if (!genZonesCFset.equals(cf.getKey()) || !genZonesCF.equals(cfZonesMl)) {
			throw new GenerationErrorException(zones.toString() + genZonesCF + cfZonesMl + cf.getKey() + map
					+ Arrays.toString(eco.curveLabels) + Arrays.toString(newCurveLabels));
		} else {
			StringBuilder out = new StringBuilder();
			out.append("-" + cfZonesMl.getCount(Collections.EMPTY_SET) + ',');
			for (Set<String> zone : cf.getKey()) {
				for (String c : zone)
					out.append(c);
				out.append("-" + cfZonesMl.getCount(zone) + ',');
			}
			out.setLength(out.length() - 1);
			return out + "\n" + RegionCode.regionCodeString(eco.outerFace) + "\n" + gcrbc.getGaussCodeString() + "\n";
		}
	}

	public static char[] setCurveLabels(int n) {
		char[] curveLabels = new char[n];
		char ch = 'A';
		for (int i = 0; i < n; i++) {
			curveLabels[i] = ch++;
		}
		return curveLabels;
	}

	public static boolean containsZone(List<Set<String>> zone, List<List<Set<String>>> zonesList, List<Integer> rgbSize,
			int m) {

		boolean b = false;
		for (int j = 0; j < zonesList.size(); j++) {
			List<Set<String>> zl = zonesList.get(j);
			if (zl.size() == zone.size()) {
				b = true;
				for (int i = 0; i < zl.size(); i++) {
					if (!matchOccurencesOfZone(zl.get(i), zl, zone)) {
						b = false;
						break;
					}
				}
				if (b && rgbSize.get(j) == m) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean matchOccurencesOfZone(Set<String> s, List<Set<String>> zl, List<Set<String>> zone) {
		int countA = 0;
		int countB = 0;
		String t = s.toString();
		for (int i = 0; i < zone.size(); i++) {
			String t1 = zone.get(i).toString();
			if (t1.equals(t))
				countA++;
			String t2 = zl.get(i).toString();
			if (t2.equals(t))
				countB++;
		}
		return (countA == countB) ? true : false;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Missing output directory.");
			return;
		}
		new File(args[0]).mkdirs();
		try (PrintWriter logWriter = new PrintWriter(new FileOutputStream(args[0] + "/eddatabase.log"), true)) {
			dbWriter = new PrintWriter(new FileOutputStream(args[0] + "/eddatabase.txt"), true);
			dbWriter.println("-1,A-1\n{(1+ 1+,+)}\nA: 1+\n");

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();

			for (int n = 2; n <= 10; n += 2) {
				String[] w = new String[n * 2];
				for (int i = 0; i < w.length; i++)
					w[i] = String.valueOf(i % n + 1);

				String ts = "N: " + n + "\nTime Started: " + dateFormat.format(cal.getTime());
				System.out.println(ts);
				logWriter.println(ts);

				ds = getDistinctSymbols(w);
				signs = computeSignsUpToReversal(ds.length);
				cyclicShiftMap = new HashMap<Integer, List<String[]>>();
				updateCyclicShiftMap(w.length / 2);
				permutationMap = new HashMap<Integer, List<String[]>>();
				updatePermutationMap(w.length / 2);
				dsLabelPerm = new ArrayList<String[]>();
				dsLabelPerm = Permutation.permuteCrossingLabels(ds);
				List<List<String[]>> ogps = new ArrayList<List<String[]>>();
				List<List<String[]>> simpleOgps = new ArrayList<List<String[]>>();
				List<List<String[]>> distinctOgps = new ArrayList<List<String[]>>();
				ogps = Permutation.splitIntoSubsetsOfEvenLength(new ArrayList<String[]>(), w, ogps);
				String before = "before: " + ogps.size();
				System.out.println(before);
				logWriter.println(before);
				for (List<String[]> ls : ogps) {
					// System.out.println(printList(ls));
					if (!checkDisjointWords(ls, ds) && !selfIntersection(ls, ds))
						simpleOgps.add(ls);
				}
				String simple = "Simple: " + simpleOgps.size();
				System.out.println(simple);
				logWriter.println(simple);
				for (List<String[]> ls : simpleOgps) {
					if (!checkEquivalence(ls, distinctOgps))
						distinctOgps.add(ls);
					// System.out.println(printList(ls));
				}
				ogps = distinctOgps;
				String distinct = "distinct " + distinctOgps.size();
				System.out.println(distinct);
				logWriter.println(distinct);

				for (int i = 0; i < ogps.size(); i++)
					computePerm(ogps.get(i));
				cal = Calendar.getInstance();
				String tf = "Time Finished: " + dateFormat.format(cal.getTime()) + "\n";
				System.out.println(tf);
				logWriter.println(tf);
			}
			dbWriter.close();
		} catch (IOException | GenerationErrorException e) {
			e.printStackTrace();
		}
	}
}