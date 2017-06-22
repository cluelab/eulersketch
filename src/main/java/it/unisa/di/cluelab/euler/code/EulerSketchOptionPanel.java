/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.util.Collections;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import it.unisa.di.cluelab.euler.code.EulerSketchInputPanel.Options;

/**
 * @author Mattia De Rosa
 */
public class EulerSketchOptionPanel extends JPanel {
	private static final long serialVersionUID = -7933489416151619248L;
	private EulerSketchInputPanel.Options options;
	private JComboBox<Integer> cbxInputDelay;
	private JCheckBox chckbxAntialias;
	private JPanel panelSegmentShow;
	private JRadioButton rdbtnSegmenShowNone;
	private JRadioButton rdbtnSegmentShowPoint;
	private JRadioButton rdbtnSegmentShowNumber;
	private JRadioButton rdbtnSegmentShowPointWithNumber;
	private JRadioButton rdbtnSegmentShowPointWithLabel;
	private JPanel panelInputDelay;
	private JLabel lblShowSegmentType;
	private JLabel lblShowPointType;
	private JPanel panelPointShow;
	private JRadioButton rdbtnPointShowNone;
	private JRadioButton rdbtnPointShowPoint;
	private JRadioButton rdbtnPointShowNumber;
	private JRadioButton rdbtnPointShowPointWithNumber;
	private JRadioButton rdbtnPointShowPointWithLabel;
	private JSeparator separator;
	private JSeparator separator_1;
	private JSeparator separator_2;
	private JCheckBox chckbxMakeNewDrawn;
	private JSeparator separator_3;
	private JCheckBox chckbxRedoOnRemove;
	private JSeparator separator_4;
	private JCheckBox chckbxShowZones;
	private JSeparator separator_5;
	private JCheckBox chckbxAllowSelfIntersect;
	private JSeparator separator_6;
	private JCheckBox chckbxShowOnlyOutlines;
	private JSeparator separator_7;
	private JSeparator separator_8;
	private JCheckBox chckbxDelCurvWrIntGauss;
	private JSeparator separator_9;
	private JCheckBox chckbxShowCurveLabels;
	private JSeparator separator_10;
	private JPanel panelUnder;
	private JComboBox<Integer> cbxUnder;

	/**
	 * Create the panel.
	 */
	public EulerSketchOptionPanel(EulerSketchInputPanel.Options options) {
		this.options = options;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		chckbxRedoOnRemove = new JCheckBox("Sequential point numbers on delete (less efficiency)");
		chckbxRedoOnRemove.setSelected(options.isRedoOnRemove());
		add(chckbxRedoOnRemove);
		
		separator_4 = new JSeparator();
		add(separator_4);
		
		chckbxMakeNewDrawn = new JCheckBox("Make new drawn curve convex");
		chckbxMakeNewDrawn.setSelected(options.isConvexCurves());
		add(chckbxMakeNewDrawn);
		
		separator_6 = new JSeparator();
		add(separator_6);
		
		chckbxAllowSelfIntersect = new JCheckBox("Allow self-intersecting curves");
		chckbxAllowSelfIntersect.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				chckbxMakeNewDrawn.setEnabled(!chckbxAllowSelfIntersect.isSelected());
			}
		});
		chckbxAllowSelfIntersect.setSelected(options.isSelfIntersect());
		add(chckbxAllowSelfIntersect);
		
		separator_3 = new JSeparator();
		add(separator_3);
		
		chckbxDelCurvWrIntGauss = new JCheckBox("Delete curves with (gauss) non valid intersections");
		chckbxDelCurvWrIntGauss.setSelected(options.isDelCurvWrIntGauss());
		add(chckbxDelCurvWrIntGauss);
		
		separator_8 = new JSeparator();
		add(separator_8);
		
		chckbxAntialias = new JCheckBox("Antialias");
		chckbxAntialias.setSelected(options.isAntialias());
		add(chckbxAntialias);
		
		separator_5 = new JSeparator();
		add(separator_5);
		
		chckbxShowZones = new JCheckBox("Colors by zones algorithm");
		chckbxShowZones.setSelected(options.isShowZones());
		add(chckbxShowZones);
		
		separator_7 = new JSeparator();
		add(separator_7);
		
		chckbxShowOnlyOutlines = new JCheckBox("Show only outlines");
		chckbxShowOnlyOutlines.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				chckbxShowZones.setEnabled(!chckbxShowOnlyOutlines.isSelected());
			}
		});
		chckbxShowOnlyOutlines.setSelected(options.isShowOnlyOutlines());
		add(chckbxShowOnlyOutlines);
		
		separator_10 = new JSeparator();
		add(separator_10);
		
		chckbxShowCurveLabels = new JCheckBox("Show curve labels");
		chckbxShowCurveLabels.setSelected(options.isShowCurveLabels());
		add(chckbxShowCurveLabels);
		
		separator = new JSeparator();
		add(separator);
		
		panelUnder = new JPanel();
		panelUnder.setAlignmentX(LEFT_ALIGNMENT);
		add(panelUnder);
		panelUnder.setLayout(new BoxLayout(panelUnder, BoxLayout.X_AXIS));
		
		JLabel lblUnder = new JLabel("Under intersection gap (0=disabled): ");
		panelUnder.add(lblUnder);
		
		TreeSet<Integer> cbxUnderOptions = new TreeSet<Integer>();
		Integer cbxUnderDefaultOption = options.getUnderGapLen();
		Collections.addAll(cbxUnderOptions, cbxUnderDefaultOption, 0, 5, 10);
		cbxUnder = new JComboBox<Integer>(cbxUnderOptions.toArray(new Integer[cbxUnderOptions.size()]));
		cbxUnder.setSelectedItem(cbxUnderDefaultOption);
		cbxUnder.setEditable(true);
		panelUnder.add(cbxUnder);
		
		separator_9 = new JSeparator();
		add(separator_9);
		
		panelInputDelay = new JPanel();
		panelInputDelay.setAlignmentX(LEFT_ALIGNMENT);
		add(panelInputDelay);
		panelInputDelay.setLayout(new BoxLayout(panelInputDelay, BoxLayout.X_AXIS));
		
		JLabel lblInputDelay = new JLabel("Click release->press delay (ms): ");
		panelInputDelay.add(lblInputDelay);
		
		TreeSet<Integer> cbxInputDelayOptions = new TreeSet<Integer>();
		Integer cbxInputDelayDefaultOptions = options.getReleasedPressedDelay();
		Collections.addAll(cbxInputDelayOptions, cbxInputDelayDefaultOptions, 0, 80, 100);
		cbxInputDelay = new JComboBox<Integer>(cbxInputDelayOptions.toArray(new Integer[cbxInputDelayOptions.size()]));
		cbxInputDelay.setSelectedItem(cbxInputDelayDefaultOptions);
		cbxInputDelay.setEditable(true);
		panelInputDelay.add(cbxInputDelay);
		
		separator_1 = new JSeparator();
		add(separator_1);
		
		lblShowPointType = new JLabel("Show points:");
		add(lblShowPointType);
		
		panelPointShow = new JPanel();
		panelPointShow.setAlignmentX(LEFT_ALIGNMENT);
		add(panelPointShow);
		
		ButtonGroup pointShowGroup = new ButtonGroup();
		panelPointShow.setLayout(new BoxLayout(panelPointShow, BoxLayout.X_AXIS));
		rdbtnPointShowNone = new JRadioButton("none", options.getPointShowType() == Options.SHOW_NONE);
		pointShowGroup.add(rdbtnPointShowNone);
		panelPointShow.add(rdbtnPointShowNone);
		
		rdbtnPointShowPoint = new JRadioButton("point", options.getPointShowType() == Options.SHOW_POINT);
		pointShowGroup.add(rdbtnPointShowPoint);
		panelPointShow.add(rdbtnPointShowPoint);
		
		rdbtnPointShowNumber = new JRadioButton("number", options.getPointShowType() == Options.SHOW_NUMBER);
		pointShowGroup.add(rdbtnPointShowNumber);
		panelPointShow.add(rdbtnPointShowNumber);
		
		rdbtnPointShowPointWithNumber = new JRadioButton("point+number", options.getPointShowType() == Options.SHOW_POINT_WITH_NUMBER);
		pointShowGroup.add(rdbtnPointShowPointWithNumber);
		panelPointShow.add(rdbtnPointShowPointWithNumber);
		
		rdbtnPointShowPointWithLabel = new JRadioButton("point+label", options.getPointShowType() == Options.SHOW_POINT_WITH_LABEL);
		pointShowGroup.add(rdbtnPointShowPointWithLabel);
		panelPointShow.add(rdbtnPointShowPointWithLabel);
		
		separator_2 = new JSeparator();
		add(separator_2);
		
		lblShowSegmentType = new JLabel("Show segments:");
		add(lblShowSegmentType);
		
		panelSegmentShow = new JPanel();
		panelSegmentShow.setAlignmentX(LEFT_ALIGNMENT);
		add(panelSegmentShow);
		
		ButtonGroup segmenentShowGroup = new ButtonGroup();
		panelSegmentShow.setLayout(new BoxLayout(panelSegmentShow, BoxLayout.X_AXIS));
		rdbtnSegmenShowNone = new JRadioButton("none", options.getSegmentShowType() == Options.SHOW_NONE);
		segmenentShowGroup.add(rdbtnSegmenShowNone);
		panelSegmentShow.add(rdbtnSegmenShowNone);
		
		rdbtnSegmentShowPoint = new JRadioButton("point", options.getSegmentShowType() == Options.SHOW_POINT);
		segmenentShowGroup.add(rdbtnSegmentShowPoint);
		panelSegmentShow.add(rdbtnSegmentShowPoint);
		
		rdbtnSegmentShowNumber = new JRadioButton("number", options.getSegmentShowType() == Options.SHOW_NUMBER);
		segmenentShowGroup.add(rdbtnSegmentShowNumber);
		panelSegmentShow.add(rdbtnSegmentShowNumber);
		
		rdbtnSegmentShowPointWithNumber = new JRadioButton("point+number", options.getSegmentShowType() == Options.SHOW_POINT_WITH_NUMBER);
		segmenentShowGroup.add(rdbtnSegmentShowPointWithNumber);
		panelSegmentShow.add(rdbtnSegmentShowPointWithNumber);
		
		rdbtnSegmentShowPointWithLabel = new JRadioButton("point+label", options.getSegmentShowType() == Options.SHOW_POINT_WITH_LABEL);
		segmenentShowGroup.add(rdbtnSegmentShowPointWithLabel);
		panelSegmentShow.add(rdbtnSegmentShowPointWithLabel);

	}

	public EulerSketchInputPanel.Options getOptions() {
		EulerSketchInputPanel.Options newOptions = new EulerSketchInputPanel.Options(options);
		newOptions.setShowZones(chckbxShowZones.isSelected());
		newOptions.setRedoOnRemove(chckbxRedoOnRemove.isSelected());
		newOptions.setSelfIntersect(chckbxAllowSelfIntersect.isSelected());
		newOptions.setDelCurvWrIntGauss(chckbxDelCurvWrIntGauss.isSelected());
		newOptions.setShowOnlyOutlines(chckbxShowOnlyOutlines.isSelected());
		newOptions.setShowCurveLabels(chckbxShowCurveLabels.isSelected());
		newOptions.setConvexCurves(chckbxMakeNewDrawn.isSelected());
		newOptions.setAntialias(chckbxAntialias.isSelected());
		Object selUnder = cbxUnder.getSelectedItem();
		try {
			newOptions.setUnderGapLen(selUnder instanceof Integer ? (Integer)selUnder
					: Integer.parseInt(selUnder.toString().trim()));
		} catch(IllegalArgumentException iae) {}
		Object selInputDelay = cbxInputDelay.getSelectedItem();
		try {
			newOptions.setReleasedPressedDelay(selInputDelay instanceof Integer ? (Integer)selInputDelay
					: Integer.parseInt(selInputDelay.toString().trim()));
		} catch(IllegalArgumentException iae) {}
		if(rdbtnPointShowNone.isSelected()) newOptions.setPointShowType(Options.SHOW_NONE);
		else if(rdbtnPointShowPoint.isSelected()) newOptions.setPointShowType(Options.SHOW_POINT);
		else if(rdbtnPointShowNumber.isSelected()) newOptions.setPointShowType(Options.SHOW_NUMBER);
		else if(rdbtnPointShowPointWithNumber.isSelected()) newOptions.setPointShowType(Options.SHOW_POINT_WITH_NUMBER);
		else if(rdbtnPointShowPointWithLabel.isSelected()) newOptions.setPointShowType(Options.SHOW_POINT_WITH_LABEL);
		if(rdbtnSegmenShowNone.isSelected()) newOptions.setSegmentShowType(Options.SHOW_NONE);
		else if(rdbtnSegmentShowPoint.isSelected()) newOptions.setSegmentShowType(Options.SHOW_POINT);
		else if(rdbtnSegmentShowNumber.isSelected()) newOptions.setSegmentShowType(Options.SHOW_NUMBER);
		else if(rdbtnSegmentShowPointWithNumber.isSelected()) newOptions.setSegmentShowType(Options.SHOW_POINT_WITH_NUMBER);
		else if(rdbtnSegmentShowPointWithLabel.isSelected()) newOptions.setSegmentShowType(Options.SHOW_POINT_WITH_LABEL);
		return newOptions;
	}
}
