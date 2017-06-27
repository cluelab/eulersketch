/*******************************************************************************
 * Copyright (c) 2013 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.EulerCode.Segment;
import it.unisa.di.cluelab.euler.code.EulerCodeGeneration.GenerationErrorException;
import it.unisa.di.cluelab.euler.code.EulerCodeUtils.CodeZones;
import it.unisa.di.cluelab.euler.code.EulerSketchInputPanel.Options;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.DisconnectedEDs;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDData;
import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.SegmentCode;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import javax.swing.JCheckBox;
import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.BoxLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Mattia De Rosa
 */
public class EulerSketchEDGenerationPreview extends JPanel {
	private static final long serialVersionUID = 7460495890981926797L;
	private JList<Region> regionList;
	private JTextPane messageTextPane;
	private JScrollPane previewScrollPane;
	private EulerSketchInputPanel previewPanel;
	private int targetWidth;
	private int targetHeight;
	private boolean defaultKeepGeneratedAspectRatio;
	private Object inputCode;
	private HashMap<GenParams, Result> generatedEDs;
	private JCheckBox chckbxKeepGeneratedAspect;
	private JCheckBox chckbxSplines;
	private JCheckBox chckbxGeomshapes;
	private JCheckBox chckbxOutline;
	private JComboBox<String> iterComboBox;
	private ListSelectionListener regionListSelList;
	private EulerSketchSmoothPanel smootPanel;
	private List<JList<Entry<EDData, Set<String>>>> componentsLists;

	public EulerSketchEDGenerationPreview(CodeZones codeZones, int targetWidth, int targetHeight,
			boolean keepGeneratedAspectRatio) {
		this(keepGeneratedAspectRatio, true);
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.inputCode = codeZones;
		if (!codeZones.zones.isEmpty()) {
			List<List<Segment>> intlines = codeZones.zones.get(0).intlines;
			Region[] rgs = new Region[intlines.size()];
			for (int i = 0; i < rgs.length; i++)
				computeED(defaultParams(Arrays.asList((rgs[i] = new Region(intlines.get(i))))));
			regionList.setListData(rgs);
		}
	}

	public EulerSketchEDGenerationPreview(GaussCodeRBC gaussCodeRBC, int targetWidth, int targetHeight,
			boolean keepGeneratedAspectRatio) {
		this(keepGeneratedAspectRatio, true);
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.inputCode = gaussCodeRBC;
		List<SegmentCode[]> rbc = gaussCodeRBC.getRegionBoundaryCode();
		if (!rbc.isEmpty()) {
			Region[] rgs = new Region[rbc.size()];
			for (int i = 0; i < rgs.length; i++)
				computeED(defaultParams(Arrays.asList((rgs[i] = new Region(rbc.get(i))))));
			regionList.setListData(rgs);
		}
		regionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public EulerSketchEDGenerationPreview(GaussCodeRBC gaussCodeRBC, SegmentCode[] externalRegion, int targetWidth,
			int targetHeight, boolean keepGeneratedAspectRatio) {
		this(keepGeneratedAspectRatio, false);
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.inputCode = gaussCodeRBC;
		Region[] rgs = new Region[] { new Region(externalRegion) };
		computeED(defaultParams(Arrays.asList(rgs[0])));
		regionList.setListData(rgs);
		regionList.setSelectedIndex(0);
	}

	public EulerSketchEDGenerationPreview(EulerCodeRBC eulerCodeRBC, int targetWidth, int targetHeight,
			boolean keepGeneratedAspectRatio) {
		this(keepGeneratedAspectRatio, false);
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.inputCode = eulerCodeRBC;
		Region[] rgs = new Region[] { new Region(eulerCodeRBC.getOuters()[0]) };
		computeED(defaultParams(Arrays.asList(rgs[0])));
		regionList.setListData(rgs);
		regionList.setSelectedIndex(0);
	}

	public EulerSketchEDGenerationPreview(List<Entry<List<EDData>, Set<String>>> codes, int targetWidth,
			int targetHeight, boolean keepGeneratedAspectRatio) {
		this(keepGeneratedAspectRatio, false);
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;

		JPanel componentsPanel = new JPanel();
		componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
		JLabel lblSelectComponents = new JLabel("Select component codes:");
		componentsPanel.add(lblSelectComponents);

		componentsLists = new ArrayList<JList<Entry<EDData, Set<String>>>>(codes.size());

		for (Entry<List<EDData>, Set<String>> e : codes) {
			Vector<Entry<EDData, Set<String>>> cdata = new Vector<Entry<EDData, Set<String>>>();
			Set<String> withins = e.getValue();
			for (EDData c : e.getKey()) {
				cdata.add(new AbstractMap.SimpleImmutableEntry<EDData, Set<String>>(c, withins) {
					private static final long serialVersionUID = 1L;

					@Override
					public String toString() {
						StringBuilder sb = new StringBuilder("maxSymLbl=");
						sb.append(getKey().getMaxSymLbl() + "; zoneCount=[");
						for (Entry<Set<String>, Integer> e : getKey().getRegionCount().entrySet()) {
							Set<String> zone = e.getKey();
							if (zone.isEmpty())
								sb.append('\u2205');
							else
								for (String c : zone)
									sb.append(c);
							sb.append("=" + e.getValue() + ", ");
						}
						sb.setLength(sb.length() - 2);
						sb.append(']');
						return sb.toString();
					}
				});
			}
			JList<Entry<EDData, Set<String>>> curjl = new JList<Entry<EDData, Set<String>>>(cdata);
			curjl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			curjl.addListSelectionListener(regionListSelList);
			componentsLists.add(curjl);
			JScrollPane cursp = new JScrollPane(curjl);
			cursp.setSize(new Dimension(250, 250));
			componentsPanel.add(cursp);
		}
		add(componentsPanel, BorderLayout.WEST);
	}

	/**
	 * Create the panel.
	 */
	public EulerSketchEDGenerationPreview() {
		this(true, true);
	}

	public EulerSketchEDGenerationPreview(boolean keepGeneratedAspectRatio, boolean showRegionList) {
		this.defaultKeepGeneratedAspectRatio = keepGeneratedAspectRatio;
		this.generatedEDs = new HashMap<GenParams, Result>();
		regionListSelList = new ListSelectionListener() {
			EulerCode empty = new EulerCode();

			public void valueChanged(ListSelectionEvent lse) {
				if (lse == null || !lse.getValueIsAdjusting()) {
					GenParams genParams = curParams();
					if (genParams.getExternal().isEmpty()) {
						previewPanel.setEulerCode(empty);
						messageTextPane.setText("");
					} else {
						Result gen = generatedEDs.get(genParams);
						if (gen == null) {
							previewPanel.setEulerCode(empty);
							messageTextPane.setText("Running...");
							if (lse != null)
								computeED(genParams);
						} else if (gen.eulerCode != null) {
							previewPanel.setEulerCode(gen.eulerCode);
							Dimension prefSize = previewPanel.getPreferredSize();
							Dimension curSize = previewScrollPane.getSize();
							float scale = previewPanel.getScale();
							float scaleWidth = curSize.width / (prefSize.width / scale);
							float scaleHeight = curSize.height / (prefSize.height / scale);
							previewPanel.setScale(Math.min(scaleWidth, scaleHeight));
							if (gen.exception == null)
								messageTextPane
										.setText(inputCode == null ? gen.eulerCode.getEulerCode(false, true) : "");
							else {
								messageTextPane.setText(
										gen.exception.getMessage() + "<br/>\n" + gen.exception.getDifferences());
								messageTextPane.setCaretPosition(0);
							}
						} else if (gen.exception != null) {
							previewPanel.setEulerCode(empty);
							messageTextPane.setText(gen.exception.getMessage());
							messageTextPane.setCaretPosition(0);
						} else {
							previewPanel.setEulerCode(empty);
							messageTextPane.setText("Running...");
						}
					}
				}
			}
		};
		setLayout(new BorderLayout());

		previewPanel = new EulerSketchInputPanel();
		previewPanel.setEnabled(false);

		previewScrollPane = new JScrollPane(previewPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		previewScrollPane.setPreferredSize(new Dimension(250, 250));
		add(previewScrollPane, BorderLayout.CENTER);

		regionList = new JList<Region>();
		regionList.addListSelectionListener(regionListSelList);
		JScrollPane scrollPane = new JScrollPane(regionList);
		scrollPane.setSize(new Dimension(250, 250));
		if (showRegionList)
			add(scrollPane, BorderLayout.WEST);

		messageTextPane = new JTextPane();
		messageTextPane.setContentType("text/html");
		messageTextPane.setEditable(false);

		JScrollPane messageScrollPane = new JScrollPane(messageTextPane);
		messageScrollPane.setPreferredSize(new Dimension(200, 64));
		add(messageScrollPane, BorderLayout.SOUTH);

		JPanel northPanel = new JPanel();
		add(northPanel, BorderLayout.NORTH);
		northPanel.setMaximumSize(new Dimension(250, 250));
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

		JPanel chckbxPanel = new JPanel();
		chckbxPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		chckbxPanel.setAlignmentX(LEFT_ALIGNMENT);
		northPanel.add(chckbxPanel);

		chckbxKeepGeneratedAspect = new JCheckBox("keep generated aspect ratio", keepGeneratedAspectRatio);
		ActionListener optionsActList = new ActionListener() {
			ListSelectionEvent blank = new ListSelectionEvent(regionList, 0, 0, false);

			public void actionPerformed(ActionEvent arg0) {
				regionListSelList.valueChanged(blank);
			}
		};
		chckbxKeepGeneratedAspect.addActionListener(optionsActList);
		chckbxPanel.add(chckbxKeepGeneratedAspect);

		chckbxSplines = new JCheckBox("splines");
		chckbxSplines.addActionListener(optionsActList);
		chckbxPanel.add(chckbxSplines);

		chckbxGeomshapes = new JCheckBox("geometric shapes");
		chckbxGeomshapes.addActionListener(optionsActList);
		chckbxPanel.add(chckbxGeomshapes);

		JPanel iterPanel = new JPanel();
		iterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		iterPanel.setAlignmentX(LEFT_ALIGNMENT);
		northPanel.add(iterPanel);

		JLabel lblSpringEmbeddingIterations = new JLabel("spring embedding iterations:");
		iterPanel.add(lblSpringEmbeddingIterations);

		iterComboBox = new JComboBox<String>();
		iterComboBox.addActionListener(optionsActList);
		iterPanel.add(iterComboBox);
		iterComboBox.setModel(new DefaultComboBoxModel<String>(
				new String[] { "0", "1", "2", "4", "8", "16", "32", "64", "128", "256", "512" }));
		iterComboBox.setEditable(true);

		chckbxOutline = new JCheckBox("show only outlines");
		chckbxOutline.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Options op = previewPanel.getOptions();
				op.setShowOnlyOutlines(chckbxOutline.isSelected());
				previewPanel.setOptions(op);
			}
		});
		iterPanel.add(chckbxOutline);

		smootPanel = new EulerSketchSmoothPanel(previewPanel);
		northPanel.add(smootPanel);

		JLabel lblSelectExternalRegions = new JLabel("Select external region(s):");
		if (showRegionList)
			northPanel.add(lblSelectExternalRegions);
	}

	public Options getPreviewOptions() {
		return previewPanel.getOptions();
	}

	public void setPreviewOptions(Options options) {
		chckbxOutline.setSelected(options.isShowOnlyOutlines());
		previewPanel.setOptions(options);
	}

	public EulerCode getEulerCode() {
		EulerCode ec = previewPanel.getEulerCode();
		return ec.getCurves().isEmpty() ? null : ec;
	}

	public GenerationErrorException getEulerCodeException() {
		Result res = generatedEDs.get(curParams());
		return res == null ? null : res.exception;
	}

	private void computeED(final GenParams genParams) {
		if (inputCode instanceof CodeZones) {
			final LinkedHashSet<Segment> external;
			if (genParams.getExternal().isEmpty())
				external = null;
			else {
				external = new LinkedHashSet<Segment>();
				for (Region sc : genParams.getExternal())
					external.addAll(sc.zoneIntline);
			}

			final Result res = new Result();
			generatedEDs.put(genParams, res);
			new Thread() {
				@Override
				public void run() {
					try {
						res.eulerCode = EulerCodeGeneration.genFromCode((CodeZones) inputCode, external, targetWidth,
								targetHeight, genParams.keepGeneratedAspectRatio);
					} catch (GenerationErrorException gee) {
						res.eulerCode = gee.getWronglyGenerated();
						res.exception = gee;
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							regionListSelList.valueChanged(null);
							regionList.repaint();
						}
					});
				}
			}.start();
		} else if (inputCode instanceof GaussCodeRBC) {
			final SegmentCode[] external;
			List<Region> ext = genParams.getExternal();
			if (ext.isEmpty())
				external = null;
			else if (ext.size() == 1)
				external = ext.get(0).segmentCodes;
			else {
				ArrayList<SegmentCode> tot = new ArrayList<SegmentCode>();
				for (Region sc : ext) {
					for (int i = 0; i < sc.segmentCodes.length; i++)
						tot.add(sc.segmentCodes[i]);
				}
				external = tot.toArray(new SegmentCode[tot.size()]);
			}

			final Result res = new Result();
			generatedEDs.put(genParams, res);
			new Thread() {
				@Override
				public void run() {
					try {
						res.eulerCode = EulerCodeGeneration.genFromGaussCodeRBC((GaussCodeRBC) inputCode, external,
								genParams.embIterNo, targetWidth, targetHeight, genParams.keepGeneratedAspectRatio,
								genParams.trySplines, genParams.tryGeomShapes);
					} catch (GenerationErrorException gee) {
						res.eulerCode = gee.getWronglyGenerated();
						res.exception = gee;
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							regionListSelList.valueChanged(null);
							regionList.repaint();
						}
					});
				}
			}.start();
		} else {
			final Result res = new Result();
			generatedEDs.put(genParams, res);
			new Thread() {
				@Override
				public void run() {
					try {
						res.eulerCode = EulerCodeGeneration.genFromEulerCode(
								inputCode == null ? DisconnectedEDs.combine(genParams.getComponents())
										: (EulerCodeRBC) inputCode,
								genParams.embIterNo, targetWidth, targetHeight, genParams.keepGeneratedAspectRatio,
								genParams.trySplines, genParams.tryGeomShapes);
					} catch (GenerationErrorException gee) {
						res.eulerCode = gee.getWronglyGenerated();
						res.exception = gee;
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							regionListSelList.valueChanged(null);
							regionList.repaint();
						}
					});
				}
			}.start();
		}
	}

	private GenParams curParams() {
		int iter = 0;
		try {
			iter = Integer.parseInt(iterComboBox.getSelectedItem().toString());
		} catch (NumberFormatException nfe) {
		}
		if (componentsLists != null) {
			ArrayList<Entry<EDData, Set<String>>> sel = new ArrayList<Entry<EDData, Set<String>>>();
			for (JList<Entry<EDData, Set<String>>> jl : componentsLists) {
				Entry<EDData, Set<String>> selVal = jl.getSelectedValue();
				if (selVal == null) {
					sel.clear();
					break;
				}
				sel.add(jl.getSelectedValue());
			}
			return new GenParams(sel, chckbxKeepGeneratedAspect.isSelected(), iter, chckbxSplines.isSelected(),
					chckbxGeomshapes.isSelected());
		} else
			return new GenParams(regionList.getSelectedValuesList(), chckbxKeepGeneratedAspect.isSelected(), iter,
					chckbxSplines.isSelected(), chckbxGeomshapes.isSelected());
	}

	private GenParams defaultParams(List<Region> external) {
		return new GenParams(external, defaultKeepGeneratedAspectRatio, 0, false, false);
	}

	private static class GenParams {
		@SuppressWarnings("rawtypes")
		final List paramList;
		final boolean keepGeneratedAspectRatio;
		final int embIterNo;
		final boolean trySplines;
		final boolean tryGeomShapes;

		private GenParams(@SuppressWarnings("rawtypes") List paramList, boolean keepGeneratedAspectRatio, int embIterNo,
				boolean trySplines, boolean tryGeomShapes) {
			this.paramList = paramList;
			this.keepGeneratedAspectRatio = keepGeneratedAspectRatio;
			this.embIterNo = embIterNo;
			this.trySplines = trySplines;
			this.tryGeomShapes = tryGeomShapes;
		}

		@SuppressWarnings("unchecked")
		public List<Region> getExternal() {
			return paramList;
		}

		@SuppressWarnings("unchecked")
		public List<Entry<EDData, Set<String>>> getComponents() {
			return paramList;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + embIterNo;
			result = prime * result + ((paramList == null) ? 0 : paramList.hashCode());
			result = prime * result + (keepGeneratedAspectRatio ? 1231 : 1237);
			result = prime * result + (tryGeomShapes ? 1231 : 1237);
			return prime * result + (trySplines ? 1231 : 1237);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof GenParams))
				return false;
			GenParams other = (GenParams) obj;
			if (embIterNo != other.embIterNo)
				return false;
			if (paramList == null) {
				if (other.paramList != null)
					return false;
			} else if (!paramList.equals(other.paramList))
				return false;
			if (keepGeneratedAspectRatio != other.keepGeneratedAspectRatio)
				return false;
			if (tryGeomShapes != other.tryGeomShapes)
				return false;
			if (trySplines != other.trySplines)
				return false;
			return true;
		}
	}

	private class Region {
		final List<Segment> zoneIntline;
		final SegmentCode[] segmentCodes;

		Region(List<Segment> zoneIntline) {
			this.zoneIntline = zoneIntline;
			this.segmentCodes = null;
		}

		public Region(SegmentCode[] segmentCodes) {
			this.segmentCodes = segmentCodes;
			this.zoneIntline = null;
		}

		@Override
		public String toString() {
			StringBuilder out = new StringBuilder();
			out.append("<html><body>");
			Result res = generatedEDs.get(defaultParams(Arrays.asList(this)));
			if (res == null || (res.eulerCode == null && res.exception == null))
				out.append("[RUNNING] ");
			else if (res.exception != null) {
				if (res.eulerCode == null)
					out.append("[EXCEPT.] ");
				else
					out.append("[DIFF.] ");
			}
			if (zoneIntline != null) {
				for (int j = 0, end = zoneIntline.size(), pPre = 0, pCur = 0; j < end; j++) {
					Segment s = zoneIntline.get(j);
					if (j == 0 || pPre == s.p1) {
						pPre = s.p2;
						pCur = s.p1;
					} else if (pPre == s.p2) {
						pPre = s.p1;
						pCur = s.p2;
					} else {
						out.append(" ERROR: no ciclic sequence");
						break;
					}
					out.append(pCur + "<sub>" + s.curve + "&#775;" + s.contCurves + "</sub>");
				}
			}
			if (segmentCodes != null) {
				out.append('{');
				for (SegmentCode sc : segmentCodes)
					out.append(sc.getSegmentCode() + ",");
				out.setCharAt(out.length() - 1, '}');
			} else if (zoneIntline == null)
				return "[null, null]";
			out.append("</body></html>");
			return out.toString();
		}
	}

	private static class Result {
		EulerCode eulerCode = null;
		GenerationErrorException exception = null;
	}
}
