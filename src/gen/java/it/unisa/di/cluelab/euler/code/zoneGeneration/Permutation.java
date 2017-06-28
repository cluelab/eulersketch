/*******************************************************************************
 * Copyright (c) 2015 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package it.unisa.di.cluelab.euler.code.zoneGeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Rafiq Saleh
 */
public class Permutation {
	public static List<List<String[]>> splitIntoSubsetsOfEvenLength(List<String[]> prefix, String[] sequence,
			List<List<String[]>> listOfogps) {
		List<String[]> ogp = new ArrayList<String[]>();
		for (int i = 2; i < sequence.length; i = i + 2) {
			String[] p = new String[i];
			System.arraycopy(sequence, 0, p, 0, i);
			int m = sequence.length - i;
			String[] s = new String[m];
			System.arraycopy(sequence, i, s, 0, m);
			List<String[]> list1 = new ArrayList<String[]>();
			List<String[]> list2 = new ArrayList<String[]>();
			list1.addAll(prefix);
			list1.add(p);
			list2.add(s);
			ogp.addAll(list1);
			ogp.addAll(list2);
			listOfogps.add(ogp);
			ogp = new ArrayList<String[]>();
			if (s.length > 2)
				splitIntoSubsetsOfEvenLength(list1, s, listOfogps);

		}
		return listOfogps;
	}

	public static boolean isEqual(List<String[]> ogp, List<String[]> list) {
		for (String[] w : ogp) {
			boolean b = false;
			for (String[] w2 : list) {
				if (Arrays.toString(w).equals(Arrays.toString(w2))) {
					b = true;
					break;
				}
			}
			if (!b)
				return false;

		}
		return true;
	}

	public static ArrayList<String[]> permuteUpToReversals(String[] myString, int n) {
		ArrayList<String[]> result = new ArrayList<String[]>();
		if (myString.length == 1) {
			// return a new ArrayList containing just s
			result.add(myString);
			return result;

		} else {
			// separate the first character from the rest
			String first = myString[0];
			String[] rest = Arrays.copyOfRange(myString, 1, myString.length);// myString
																				// .substring(1);
			// get all permutationsOf the rest of the characters

			ArrayList<String[]> simpler = permuteUpToReversals(rest, n); // recursive
																			// step
			/* remove the following if-else block to include reversals */
			int m = 0;
			if (simpler.get(0).length == n - 1) {
				m = simpler.size() / 2;
			} else
				m = simpler.size();
			// for each permutation,
			for (int i = 0; i < m; i++) {
				String[] permutation = simpler.get(i);
				// extra work
				// add the first character in all possible positions, and
				// ArrayList additions = insertAtAllPositions(first,
				// permutation);
				ArrayList<String[]> additions = insertAtAllPositions(first, permutation);
				// ArrayList<? extends String> additions =
				// insertAtAllPositions(first, permutation);
				// put each result into a new ArrayList
				result.addAll(additions);
			}
		}

		return result;
	}

	private static ArrayList<String[]> insertAtAllPositions(String ch, String[] s) {
		ArrayList<String[]> result = new ArrayList<String[]>();

		for (int i = 0; i <= s.length; i++) {
			String[] inserted = new String[s.length + 1];
			System.arraycopy(s, 0, inserted, 0, i);
			// inserted[0]=s[0];
			inserted[i] = ch;
			System.arraycopy(s, i, inserted, i + 1, s.length - i);
			// System.out.println(i+" :"+" first "+ch +" rest "+
			// Arrays.toString(s)+" all: "+Arrays.toString(inserted));
			result.add(inserted);
		}
		// for(String []t :result)
		// System.out.println(Arrays.toString(t)+"\t"+n++);

		return result;
	}

	public static List<int[]> permute(int[] s, List<int[]> list) {
		// Print initial string, as only the alterations will be printed later
		int[] newArr = new int[s.length];
		System.arraycopy(s, 0, newArr, 0, s.length);
		list.add(newArr);
		// char[] a = s.toCharArray();
		int n = s.length;
		int[] p = new int[n]; // Index control array initially all zeros
		int i = 1;
		while (i < n) {
			if (p[i] < i) {
				int j = ((i % 2) == 0) ? 0 : p[i];
				swap(s, i, j);
				// Print current
				newArr = new int[s.length];
				System.arraycopy(s, 0, newArr, 0, s.length);
				list.add(newArr);
				p[i]++;
				i = 1;
			} else {
				p[i] = 0;
				i++;
			}
		}
		return list;
	}

	private static void swap(int[] s, int i, int j) {
		int temp = s[i];
		s[i] = s[j];
		s[j] = temp;
	}

	public static List<List<String[]>> permutateOGPWords(List<String[]> ogp) {
		List<List<String[]>> list = new ArrayList<List<String[]>>();

		int[] indicesPermArr = new int[ogp.size()];
		for (int i = 0; i < ogp.size(); i++)
			indicesPermArr[i] = i;
		List<int[]> indicesPermList = permute(indicesPermArr, new ArrayList<int[]>());
		for (int[] arr : indicesPermList) {
			List<String[]> ls = new ArrayList<String[]>();
			for (int i = 0; i < arr.length; i++) {
				ls.add(ogp.get(arr[i]));
			}
			list.add(ls);
		}
		return list;
	}

	public static List<List<String[]>> permutateOGPWordsUpToWordsLengthOrder(List<String[]> ogp, int[] wordsLength) {
		List<List<String[]>> list = new ArrayList<List<String[]>>();

		int[] indicesPermArr = new int[ogp.size()];
		for (int i = 0; i < ogp.size(); i++) {
			indicesPermArr[i] = i;
		}
		List<int[]> indicesPermList = permute(indicesPermArr, new ArrayList<int[]>());
		for (int[] arr : indicesPermList) {
			List<String[]> ls = new ArrayList<String[]>();
			boolean checkWordLengths = true;
			for (int i = 0; i < arr.length; i++) {
				if (ogp.get(arr[i]).length != wordsLength[i]) {
					checkWordLengths = false;
					break;
				}
				ls.add(ogp.get(arr[i]));
			}
			if (checkWordLengths)
				list.add(ls);
		}
		return list;
	}

	public static List<String[]> permuteCurveLabels(int n) {
		List<String[]> list = new ArrayList<String[]>();

		int[] indicesPermArr = new int[n];
		String[] curveLabels = new String[n];
		char c = 'A';
		for (int i = 0; i < n; i++) {
			indicesPermArr[i] = i;
			curveLabels[i] = "" + c;
			c++;
		}
		List<int[]> indicesPermList = permute(indicesPermArr, new ArrayList<int[]>());
		for (int[] arr : indicesPermList) {
			String[] ls = new String[n];
			for (int i = 0; i < arr.length; i++) {
				ls[i] = curveLabels[arr[i]];

			}
			list.add(ls);
		}
		return list;
	}

	public static List<String[]> permuteCrossingLabels(String[] w) throws OutOfMemoryError {
		List<String[]> list = new ArrayList<String[]>();
		int n = w.length;
		int[] indicesPermArr = new int[n];

		for (int i = 0; i < n; i++) {
			indicesPermArr[i] = i;
		}
		List<int[]> indicesPermList = permute(indicesPermArr, new ArrayList<int[]>());
		for (int[] arr : indicesPermList) {
			String[] ls = new String[n];
			for (int i = 0; i < arr.length; i++) {
				ls[i] = w[arr[i]];

			}
			list.add(ls);
		}
		return list;
	}

	public static List<Set<String>> renameZoneSet(List<Set<String>> zones, String[] initialCurveLabels,
			String[] newCurveLabels) {
		List<Set<String>> zonesList = new ArrayList<Set<String>>();
		String zs = "";
		for (Set<String> set : zones) {
			for (String s : set)
				zs += s;
			zs += ",";
		}
		char c = 'a';
		for (int i = 0; i < initialCurveLabels.length; i++) {
			zs = zs.replaceAll(initialCurveLabels[i], "" + c);
			c++;

		}
		c = 'a';
		for (int i = 0; i < newCurveLabels.length; i++) {
			String s = c + "";
			zs = zs.replaceAll(s, newCurveLabels[i]);
			c++;
		}
		String[] zone = zs.split(",");

		for (String s : zone) {
			Set<String> zoneSet = new HashSet<String>();
			for (int i = 0; i < s.length(); i++)
				zoneSet.add(s.substring(i, i + 1));
			zonesList.add(zoneSet);
		}
		return zonesList;
	}

}