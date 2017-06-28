/*******************************************************************************
 * Copyright (c) 2013 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code.vennGeneration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import it.unisa.di.cluelab.euler.code.gausscode.Misc;
import it.unisa.di.cluelab.euler.code.gausscode.RegionCode;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;

/**
 * @author Rafiq Saleh
 */
public class VennGeneration {
	public static List<List<Symbol>> rewriteOGP(Symbol[][] w, List<List<Symbol>> output) {
		int i = 0;
		int n = findNoOfSymbols(w) + 1;
		List<Symbol> newWord = new ArrayList<Symbol>();
		for (int k = 0; k < w[i].length; k++) {
			Symbol s1 = w[i][k];
			Symbol s2;
			if (w[i][k].getSign() == '+' && isIn(w[i][k], w[w.length - 1])) {
				s2 = new Symbol("" + n, '-');
				s1 = new Symbol(s1.getLabel(), '-');
				add(1, s1, s2, output);
				newWord.add(new Symbol(s2.getLabel(), '+'));
				n++;
				s2 = new Symbol("" + n, '-');
				add(1, w[i][k], s2, output);
				newWord.add(new Symbol(s2.getLabel(), '+'));
				n++;

			} else if (w[i][k].getSign() == '+' && !isIn(w[i][k], w[w.length - 1])) {
				s2 = new Symbol("" + n, '-');
				s1 = new Symbol(s1.getLabel(), '-');
				add(0, s1, s2, output);
				newWord.add(new Symbol(s2.getLabel(), '+'));
				n++;
				s2 = new Symbol("" + n, '+');
				add(1, w[i][k], s2, output);
				newWord.add(new Symbol(s2.getLabel(), '-'));
				n++;

			} else if (w[i][k].getSign() == '-' && isIn(w[i][k], w[w.length - 1])) {
				s2 = new Symbol("" + n, '+');
				s1 = new Symbol(s1.getLabel(), '+');
				add(0, s1, s2, output);
				newWord.add(new Symbol(s2.getLabel(), '-'));
				n++;
				s2 = new Symbol("" + n, '-');
				add(1, w[i][k], s2, output);
				newWord.add(new Symbol(s2.getLabel(), '+'));
				n++;
			} else if (w[i][k].getSign() == '-' && !isIn(w[i][k], w[w.length - 1])) {
				s2 = new Symbol("" + n, '+');
				s1 = new Symbol(s1.getLabel(), '+');
				add(1, s1, s2, output);
				newWord.add(new Symbol(s2.getLabel(), '-'));
				n++;
				s2 = new Symbol("" + n, '+');
				add(1, w[i][k], s2, output);
				newWord.add(new Symbol(s2.getLabel(), '-'));
				n++;
			}

		}
		output.add(newWord);
		return output;
	}

	public static void add(int i, Symbol s1, Symbol s2, List<List<Symbol>> output) {
		done: for (List<Symbol> w : output) {
			for (int j = 0; j < w.size(); j++) {
				Symbol s = w.get(j);
				if (s.getLabel().equals(s1.getLabel()) && s.getSign() == s1.getSign()) {
					if (i == 0) {
						w.add(j, s2);
						break done;
					} else {
						w.add(j + 1, s2);
						break done;
					}
				}
			}
		}

	}

	public static List<List<Symbol>> parseInput(Symbol[][] w) {
		List<List<Symbol>> list = new ArrayList<List<Symbol>>();
		for (int i = 0; i < w.length; i++) {
			List<Symbol> wc = new ArrayList<Symbol>();
			for (int j = 0; j < w[i].length; j++) {
				wc.add(j, w[i][j]);
			}
			list.add(i, wc);
		}
		return list;
	}

	public static int findNoOfSymbols(Symbol[][] w) {
		int n = 0;
		for (Symbol[] w1 : w)
			n += w1.length;

		return n / 2;

	}

	public static Boolean isIn(Symbol s, Symbol[] w) {
		for (Symbol t : w)
			if (s.getLabel().equals(t.getLabel()) && s.getSign() != t.getSign())
				return true;
		return false;
	}

	static SegmentCode[] getOuterFaceCode(SegmentCode s, ArrayList<SegmentCode[]> ar) {
		SegmentCode[] region = new SegmentCode[0];
		for (SegmentCode[] r : ar) {
			for (SegmentCode t : r) {
				if (s.getFirstSymbol().getLabel().equals(t.getFirstSymbol().getLabel())
						&& s.getFirstSymbol().getSign() == t.getFirstSymbol().getSign()
						&& s.getDirection() == t.getDirection()) {
					return r;
				}
			}
		}
		return region;
	}

	public static ArrayList<SegmentCode> computeOuterFaceRGB(SegmentCode s, ArrayList<SegmentCode> S) {
		ArrayList<SegmentCode> r = new ArrayList<SegmentCode>();
		SegmentCode s2 = RegionCode.traverse(s, S);// traverse(s,S);
		r.add(s);
		while (!Misc.isEqual(s, s2)) {
			r.add(s2);
			s2 = RegionCode.traverse(s2, S);
		}
		return r;
	}

	public static void writeToBinary(String path) throws IOException {
		String filename = path + "/venn2.dat";
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
			Symbol[][] w1 = { { new Symbol("1", '-'), new Symbol("2", '+') },
					{ new Symbol("1", '+'), new Symbol("2", '-') } };
			ArrayList<SegmentCode[]> R = RegionCode.computeRegionBoundaryCode(w1);
			Symbol s1 = new Symbol("1", '+');
			Symbol s2 = null;
			SegmentCode[] r = getOuterFaceCode(new SegmentCode(s1, s2, '+'), R);
			DiagramCode obj = new DiagramCode(new char[] { 'A', 'B' }, w1, R, r);
			out.writeObject(obj);
			System.out.println("Object 1" + " copied to " + filename + " as Binary");
		}

		Symbol[][] w = { { new Symbol("3", '-'), new Symbol("4", '-'), new Symbol("5", '+'), new Symbol("6", '+') },
				{ new Symbol("6", '-'), new Symbol("2", '-'), new Symbol("4", '+'), new Symbol("1", '+') },
				{ new Symbol("1", '-'), new Symbol("5", '-'), new Symbol("2", '+'), new Symbol("3", '+') } };

		filename = path + "/venn3.dat";
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
			ArrayList<SegmentCode[]> R = RegionCode.computeRegionBoundaryCode(w);
			Symbol s1 = new Symbol("1", '+');
			Symbol s2 = null;
			SegmentCode[] r = getOuterFaceCode(new SegmentCode(s1, s2, '+'), R);

			DiagramCode obj = new DiagramCode(new char[] { 'A', 'B', 'C' }, w, R, r);
			out.writeObject(obj);
			System.out.println("Object " + 2 + "  copied to " + filename + " as Binary");
		}

		for (int j = 3; j < 18; j++) {
			filename = path + "/venn" + (j + 1) + ".dat";
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
				List<List<Symbol>> output = rewriteOGP(w, parseInput(w));
				w = new Symbol[output.size()][];

				int i = 0;
				for (List<Symbol> wi : output) {
					Symbol[] array = new Symbol[wi.size()];
					wi.toArray(array);
					w[i] = array;
					i++;
				}
				ArrayList<SegmentCode[]> R = RegionCode.computeRegionBoundaryCode(w);
				Symbol s1 = new Symbol("1", '+');
				Symbol s2 = null;
				SegmentCode[] r = getOuterFaceCode(new SegmentCode(s1, s2, '+'), R);

				char[] lbs = new char[j + 1];
				for (int k = 0; k < lbs.length; k++)
					lbs[k] = (char) ('A' + k);
				DiagramCode obj = new DiagramCode(lbs, w, R, r);
				out.writeObject(obj);
				System.out.println("Object " + j + "  copied to " + filename + " as Binary");
			}
		}
	}

	public static DiagramCode getObjectFromBinaryFile(String filename, int m) {
		DiagramCode object = null;
		File file = new File(filename);

		if (file.exists()) {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(filename));
				while (true) {
					DiagramCode obj = (DiagramCode) ois.readObject();
					if (obj.getCurveLabels().length == m) {
						object = obj;
						break;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (ois != null) {
						ois.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return object;
	}

	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				new File(args[0]).mkdirs();
				writeToBinary(args[0]);
			} else {
				System.err.println("Missing output directory.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
