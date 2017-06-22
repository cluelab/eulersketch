/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointsPolygon;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;
import it.unisa.di.cluelab.euler.code.EulerCodeUtils.LCode;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDData;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDDatabase;
import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.Misc;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

import uk.co.timwise.wraplayout.WrapLayout;

/**
 * @author Mattia De Rosa
 */
public class EulerSketchGUI extends JPanel {
	private static final long serialVersionUID = -8079380092488677226L;
	private EulerSketchInputPanel inputPanel;
	private JScrollPane scrollPaneInput;
	private EulerSketchEDGenerationDialog generationDialog;
	private EDDatabase edDatabase;
	private Properties properties;
	private JCheckBox chckbxGaussLeft;
	private JCheckBox chckbxEulerLeft;
	private JCheckBox chckbxClosedcircleLeft;
	private JCheckBox chckbxInfixLeft;
	private JCheckBox chckbxGaussRight;
	private JCheckBox chckbxEulerRight;
	private JCheckBox chckbxClosedcircleRight;
	private JCheckBox chckbxInfixRight;
	private JComboBox<String> cbxZoom;

	private static Color getPropertyColor(Properties properties, String key,
			Color defaultValue) {
		String strcol = properties.getProperty(key);
		if (strcol == null)
			return defaultValue;
		long col = Long.parseLong(strcol, 16);
		return col < 0 ? null : new Color((int) col, true);
	}

	private static String colorToString(Color color) {
		return color == null ? "-1" : Integer.toHexString(color.getRGB());
	}

	/**
	 * Create the panel.
	 */
	public EulerSketchGUI() {
		this(null, null, null);
	}

	public EulerSketchGUI(EulerCode eulerCode, EDDatabase edDb,
			Properties properties) {
		this.edDatabase = edDb;
		this.properties = properties == null ? new Properties() : properties;
		final EulerSketchGUI aThis = this;
		setLayout(new BorderLayout());

		inputPanel = new EulerSketchInputPanel();
		EulerSketchInputPanel.Options opt = inputPanel.getOptions();
		opt.setRedoOnRemove(Boolean.parseBoolean(properties.getProperty(
				"redoOnRemove", String.valueOf(opt.isRedoOnRemove()))));
		opt.setConvexCurves(Boolean.parseBoolean(properties.getProperty(
				"convexCurves", String.valueOf(opt.isConvexCurves()))));
		opt.setSelfIntersect(Boolean.parseBoolean(properties.getProperty(
				"selfIntersect", String.valueOf(opt.isSelfIntersect()))));
		opt.setDelCurvWrIntGauss(Boolean.parseBoolean(properties.getProperty(
				"delCurvWrIntGauss", String.valueOf(opt.isDelCurvWrIntGauss()))));
		opt.setAntialias(Boolean.parseBoolean(properties.getProperty(
				"antialias", String.valueOf(opt.isAntialias()))));
		opt.setShowZones(Boolean.parseBoolean(properties.getProperty(
				"showZones", String.valueOf(opt.isShowZones()))));
		opt.setShowOnlyOutlines(Boolean.parseBoolean(properties.getProperty(
				"showOnlyOutlines", String.valueOf(opt.isShowOnlyOutlines()))));
		opt.setShowCurveLabels(Boolean.parseBoolean(properties.getProperty(
				"showCurveLabels", String.valueOf(opt.isShowCurveLabels()))));
		opt.setUnderGapLen(Integer.parseInt(properties.getProperty(
				"underGapLen", String.valueOf(opt.getUnderGapLen()))));
		opt.setReleasedPressedDelay(Integer.parseInt(properties.getProperty(
				"releasedPressedDelay",
				String.valueOf(opt.getReleasedPressedDelay()))));
		opt.setPointShowType(Integer.parseInt(properties.getProperty(
				"pointShowType", String.valueOf(opt.getPointShowType()))));
		opt.setSegmentShowType(Integer.parseInt(properties.getProperty(
				"segmentShowType", String.valueOf(opt.getSegmentShowType()))));
		String ccols = properties.getProperty("curveColors");
		if (ccols != null) {
			String[] sp = ccols.split(",");
			Color[] cs = new Color[sp.length];
			for (int i = 0; i < sp.length; i++)
				cs[i] = new Color((int) Long.parseLong(sp[i], 16), true);
			opt.setCurveColors(cs);
		}
		opt.setBackgroundColor(getPropertyColor(properties, "backgroundColor",
				opt.getBackgroundColor()));
		opt.setEraseColor(getPropertyColor(properties, "eraseColor",
				opt.getEraseColor()));
		opt.setPointColor(getPropertyColor(properties, "pointColor",
				opt.getPointColor()));
		opt.setStrokeColor(getPropertyColor(properties, "strokeColor",
				opt.getStrokeColor()));
		opt.setCurveLabelColor(getPropertyColor(properties, "curveLabelColor",
				opt.getCurveLabelColor()));
		opt.setSegmentLabelColor(getPropertyColor(properties,
				"segmentLabelColor", opt.getSegmentLabelColor()));
		
		inputPanel.setOptions(opt);
		if(eulerCode != null) inputPanel.setEulerCode(eulerCode);

		final JTextPane txtpnCodeLeft = new JTextPane();
		txtpnCodeLeft.setContentType("text/html");
		txtpnCodeLeft.setEditable(false);

		JScrollPane scrollPaneCodeLeft = new JScrollPane(txtpnCodeLeft);

		JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BorderLayout());

		JPanel chosePanelLeft = new JPanel();
		panelLeft.add(chosePanelLeft, BorderLayout.NORTH);

		ButtonGroup choseLeftGroup = new ButtonGroup();
		chosePanelLeft.setLayout(new BoxLayout(chosePanelLeft, BoxLayout.X_AXIS));
		
		chckbxGaussLeft = new JCheckBox("gauss",
				Boolean.parseBoolean(properties
						.getProperty("gaussLeft", "true")));
		chckbxEulerLeft = new JCheckBox("knot",
				Boolean.parseBoolean(properties.getProperty("eulerLeft",
						"false")));
		chckbxClosedcircleLeft = new JCheckBox("closed",
				Boolean.parseBoolean(properties.getProperty("closedcircleLeft",
						"false")));
		chckbxInfixLeft = new JCheckBox("infix",
				Boolean.parseBoolean(properties
						.getProperty("infixLeft", "true")));

		final JRadioButton rdbtnCodeLeft = new JRadioButton("code", true);
		choseLeftGroup.add(rdbtnCodeLeft);
		chosePanelLeft.add(rdbtnCodeLeft);

		final JRadioButton rdbtnZoneCodeLeft = new JRadioButton("zones");
		choseLeftGroup.add(rdbtnZoneCodeLeft);
		chosePanelLeft.add(rdbtnZoneCodeLeft);

		chosePanelLeft.add(chckbxGaussLeft);
		chosePanelLeft.add(chckbxEulerLeft);
		chosePanelLeft.add(chckbxClosedcircleLeft);
		chosePanelLeft.add(chckbxInfixLeft);

		ChangeListener leftChange = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				EulerCode ec = inputPanel.getEulerCode();
				try {
					boolean isGauss = chckbxGaussLeft.isSelected();
					boolean isEuler = chckbxEulerLeft.isSelected(); 					
					//chckbxClosedcircleLeft.setEnabled(!isGauss);
					if (rdbtnCodeLeft.isSelected()) {
						/*if (isGauss) {
							if (chckbxEulerLeft.isSelected())
								txtpnCodeLeft.setText(ec.getEulerCode(
										chckbxClosedcircleLeft.isSelected(),
										true));
							else
								txtpnCodeLeft.setText(ec.getGaussCode(
										chckbxClosedcircleLeft.isSelected(),
										true));
						} else
							txtpnCodeLeft.setText(ec.getCode(
									chckbxInfixLeft.isSelected(),
									chckbxClosedcircleLeft.isSelected(), true));*/
						if (isGauss && (e.getSource().equals(chckbxGaussLeft)))  
						{
							chckbxEulerLeft.setSelected(false);
							chckbxInfixLeft.setEnabled(false);
							chckbxClosedcircleLeft.setEnabled(true); 
						}
						else if (isEuler && (e.getSource().equals(chckbxEulerLeft)))
						{
							chckbxGaussLeft.setSelected(false);
							chckbxInfixLeft.setEnabled(false);
							chckbxClosedcircleLeft.setEnabled(true); 

						}
						if (isGauss)  
						{ 
							txtpnCodeLeft.setText(ec.getGaussCode(chckbxClosedcircleLeft.isSelected(),true));
						}
						if (isEuler)
						{
							txtpnCodeLeft.setText(ec.getKnotCode(chckbxClosedcircleLeft.isSelected(),true));
						}
						if (!isEuler && !isGauss)
						{
							chckbxInfixLeft.setEnabled(true); 
							chckbxClosedcircleLeft.setEnabled(true);
							txtpnCodeLeft.setText(ec.getCode(chckbxInfixLeft.isSelected(), chckbxClosedcircleLeft.isSelected(), true));
						}

					} else if(rdbtnZoneCodeLeft.isSelected()) {
						if (isGauss || isEuler) {

							if (isGauss && (e.getSource().equals(chckbxGaussLeft)|| e.getSource().equals(rdbtnZoneCodeLeft)))  
							{
								chckbxEulerLeft.setSelected(false); 
							}
							else if (isEuler && (e.getSource().equals(chckbxEulerLeft)|| e.getSource().equals(rdbtnZoneCodeLeft)))
							{								 
								chckbxGaussLeft.setSelected(false);  
							}
							chckbxInfixLeft.setEnabled(false);
							chckbxClosedcircleLeft.setEnabled(false);
							Symbol[][] gaussCode=ec.getGaussCodeRBC().getGaussCode();
							char [] curveLabels = ec.getGaussCodeRBC().getCurveLabels();
							List<List<Symbol[]>> listOfDisjointGaussCodes=Misc.checkDisjointWords(gaussCode);
							boolean isDisjoint = listOfDisjointGaussCodes.size()>1;
							boolean isNonIntersectingCurve= gaussCode.length==1 && gaussCode[0].length==1;
							if(isNonIntersectingCurve ||isDisjoint )
								txtpnCodeLeft.setText(ec.getGaussZonesCodeUsingStaticCodeMethod(true,gaussCode,curveLabels));
							else 
							{ 								
								txtpnCodeLeft.setText(ec.getGaussZonesCode(true));
							}

						}
						else
						{
							chckbxInfixLeft.setEnabled(true); 
							chckbxClosedcircleLeft.setEnabled(true);
							txtpnCodeLeft.setText(ec.getZonesCode(chckbxInfixLeft.isSelected(), chckbxClosedcircleLeft.isSelected(), true));
						}
					}
				} catch(RuntimeException re) {
					txtpnCodeLeft.setText(re.getMessage());
				}
			}
		};
		rdbtnCodeLeft.addChangeListener(leftChange);
		rdbtnZoneCodeLeft.addChangeListener(leftChange);
		chckbxGaussLeft.addChangeListener(leftChange);
		chckbxEulerLeft.addChangeListener(leftChange);
		chckbxInfixLeft.addChangeListener(leftChange);
		chckbxClosedcircleLeft.addChangeListener(leftChange);

		panelLeft.add(scrollPaneCodeLeft, BorderLayout.CENTER);

		final JTextPane txtpnCodeRight = new JTextPane();
		txtpnCodeRight.setContentType("text/html");
		txtpnCodeRight.setEditable(false);

		JScrollPane scrollPaneCodeRight = new JScrollPane(txtpnCodeRight);

		JPanel panelRight = new JPanel();
		panelRight.setLayout(new BorderLayout());

		JPanel chosePanelRight = new JPanel();
		panelRight.add(chosePanelRight, BorderLayout.NORTH);

		ButtonGroup choseRightGroup = new ButtonGroup();
		chosePanelRight.setLayout(new BoxLayout(chosePanelRight, BoxLayout.X_AXIS));

		chckbxGaussRight = new JCheckBox("gauss",
				Boolean.parseBoolean(properties.getProperty("gaussRight",
						"true")));
		chckbxEulerRight = new JCheckBox("euler",
				Boolean.parseBoolean(properties.getProperty("eulerRight",
						"false")));
		chckbxClosedcircleRight = new JCheckBox("closed",
				Boolean.parseBoolean(properties.getProperty("closedRight",
						"false")));
		chckbxInfixRight = new JCheckBox("infix",
				Boolean.parseBoolean(properties.getProperty(
						"infixRight", "true")));

		final JRadioButton rdbtnCodeRight = new JRadioButton("code");
		choseRightGroup.add(rdbtnCodeRight);
		chosePanelRight.add(rdbtnCodeRight);

		final JRadioButton rdbtnZoneCodeRight = new JRadioButton("zones");
		choseRightGroup.add(rdbtnZoneCodeRight);
		chosePanelRight.add(rdbtnZoneCodeRight);

		chosePanelRight.add(chckbxGaussRight);
		chosePanelRight.add(chckbxEulerRight);
		chosePanelRight.add(chckbxClosedcircleRight);
		chosePanelRight.add(chckbxInfixRight);

		ChangeListener rightChange = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				EulerCode ec = inputPanel.getEulerCode();
				try {
					boolean isGauss = chckbxGaussRight.isSelected();
					boolean isEuler = chckbxEulerRight.isSelected(); 					
					//chckbxClosedcircleRight.setEnabled(!isGauss);
					if (rdbtnCodeRight.isSelected()) {
						/*if (isGauss) {
							if (chckbxEulerRight.isSelected())
								txtpnCodeRight.setText(ec.getEulerCode(
										chckbxClosedcircleRight.isSelected(),
										true));
							else
								txtpnCodeRight.setText(ec.getGaussCode(
										chckbxClosedcircleRight.isSelected(),
										true));
						} else
							txtpnCodeRight.setText(ec.getCode(
									chckbxInfixRight.isSelected(),
									chckbxClosedcircleRight.isSelected(), true));*/
						
						if (isGauss && (e.getSource().equals(chckbxGaussRight)))  
						{
							chckbxEulerRight.setSelected(false);
							chckbxInfixRight.setEnabled(false);
							chckbxClosedcircleRight.setEnabled(true); 
						}
						else if (isEuler && (e.getSource().equals(chckbxEulerRight)))
						{
							chckbxGaussRight.setSelected(false);
							chckbxInfixRight.setEnabled(false);
							chckbxClosedcircleRight.setEnabled(true); 

						}
						if (isGauss)  
						{ 
							txtpnCodeRight.setText(ec.getGaussCode(chckbxClosedcircleRight.isSelected(),true));
						}
						if (isEuler)
						{
							txtpnCodeRight.setText(ec.getEulerCode(chckbxClosedcircleRight.isSelected(),true));
						}
						if (!isEuler && !isGauss)
						{
							chckbxInfixRight.setEnabled(true); 
							chckbxClosedcircleRight.setEnabled(true);
							txtpnCodeRight.setText(ec.getCode(chckbxInfixRight.isSelected(), chckbxClosedcircleRight.isSelected(), true));
						}

					} else if(rdbtnZoneCodeRight.isSelected()) {
						if (isGauss || isEuler) {

							if (isGauss && (e.getSource().equals(chckbxGaussRight)|| e.getSource().equals(rdbtnZoneCodeRight)))  
							{
								chckbxEulerRight.setSelected(false); 
							}
							else if (isEuler && (e.getSource().equals(chckbxEulerRight)|| e.getSource().equals(rdbtnZoneCodeRight)))
							{								 
								chckbxGaussRight.setSelected(false);  
							}
							chckbxInfixRight.setEnabled(false);
							chckbxClosedcircleRight.setEnabled(false);
							Symbol[][] gaussCode=ec.getGaussCodeRBC().getGaussCode();
							char [] curveLabels = ec.getGaussCodeRBC().getCurveLabels();
							List<List<Symbol[]>> listOfDisjointGaussCodes=Misc.checkDisjointWords(gaussCode);
							boolean isDisjoint = listOfDisjointGaussCodes.size()>1;
							boolean isNonIntersectingCurve= gaussCode.length==1 && gaussCode[0].length==1;
							if(isNonIntersectingCurve ||isDisjoint )
								txtpnCodeRight.setText(ec.getGaussZonesCodeUsingStaticCodeMethod(true,gaussCode,curveLabels));
							else 
							{ 								
								txtpnCodeRight.setText(ec.getGaussZonesCode(true));
							}
						}
						else
						{
							chckbxInfixRight.setEnabled(true); 
							chckbxClosedcircleRight.setEnabled(true);
							txtpnCodeRight.setText(ec.getZonesCode(chckbxInfixRight.isSelected(), chckbxClosedcircleRight.isSelected(), true));
						}
					}
				} catch(RuntimeException re) {
					txtpnCodeRight.setText(re.getMessage());
				}
			}
		};

		rdbtnCodeRight.addChangeListener(rightChange);
		rdbtnZoneCodeRight.addChangeListener(rightChange);
		chckbxGaussRight.addChangeListener(rightChange);
		chckbxEulerRight.addChangeListener(rightChange);
		chckbxInfixRight.addChangeListener(rightChange);
		chckbxClosedcircleRight.addChangeListener(rightChange);

		panelRight.add(scrollPaneCodeRight, BorderLayout.CENTER);

		inputPanel.addChangeListener(leftChange);
		inputPanel.addChangeListener(rightChange);
		scrollPaneInput = new JScrollPane(inputPanel);

		JSplitPane splitPaneCodetexts = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
		splitPaneCodetexts.setPreferredSize(new Dimension(100, 100));
		splitPaneCodetexts.setOneTouchExpandable(true);
		splitPaneCodetexts.setResizeWeight(0.5);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPaneInput, splitPaneCodetexts);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(1.0);
		add(splitPane, BorderLayout.CENTER);

		ButtonGroup inputModeGroup = new ButtonGroup();
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new WrapLayout());
		add(buttonsPanel, BorderLayout.NORTH);

		JToggleButton tglbtnDraw = new JToggleButton(new ImageIcon(
				EulerSketchGUI.class
				.getResource("/images/sc_freeline_unfilled.png")), true);
		tglbtnDraw.setToolTipText("Drawing");
		tglbtnDraw.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputPanel.setInputMode(EulerSketchInputPanel.DRAW_INPUT_MODE);
			}
		});
		inputModeGroup.add(tglbtnDraw);
		buttonsPanel.add(tglbtnDraw);

		JToggleButton tglbtnErase = new JToggleButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/del1bmp.png")));
		tglbtnErase.setToolTipText("Erase");
		tglbtnErase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputPanel.setInputMode(EulerSketchInputPanel.ERASE_INPUT_MODE);
			}
		});
		inputModeGroup.add(tglbtnErase);
		buttonsPanel.add(tglbtnErase);

		JToggleButton tglbtnMove = new JToggleButton(new ImageIcon(
				EulerSketchGUI.class
				.getResource("/images/sc_arrowshapes.quad-arrow.png")));
		tglbtnMove.setToolTipText("Move");
		tglbtnMove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputPanel.setInputMode(EulerSketchInputPanel.MOVE_INPUT_MODE);
			}
		});
		inputModeGroup.add(tglbtnMove);
		buttonsPanel.add(tglbtnMove);

		JSeparator separator = new JSeparator();
		buttonsPanel.add(separator);

		Vector<String> pcts = new Vector<String>(Arrays.asList("25%", "50%",
				"100%", "200%", "400%"));
		String pZoom = properties.getProperty("zoom", "100%");
		int pInd = pcts.indexOf(pZoom);
		if (pInd == -1) {
			pInd = pcts.size();
			pcts.add(pZoom);
		}
		cbxZoom = new JComboBox<String>(pcts);
		cbxZoom.setEditable(true);
		Dimension comboBoxPS = cbxZoom.getPreferredSize();
		comboBoxPS.width = 64;
		cbxZoom.setPreferredSize(comboBoxPS);
		cbxZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String sel = cbxZoom.getSelectedItem().toString().trim();
				try {
					inputPanel.setScale(Integer.parseInt(sel.endsWith("%") ? sel.substring(0, sel.length() - 1).trim() : sel) / 100f);
				} catch(IllegalArgumentException iae) {}
				cbxZoom.setSelectedItem(Math.round(inputPanel.getScale() * 100f) + "%");
			}
		});
		cbxZoom.setSelectedIndex(pInd);
		buttonsPanel.add(cbxZoom);

		JButton btnClear = new JButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/delall.png")));
		btnClear.setToolTipText("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputPanel.clear();
			}
		});
		buttonsPanel.add(btnClear);

		JSeparator separator_1 = new JSeparator();
		buttonsPanel.add(separator_1);

		generationDialog = new EulerSketchEDGenerationDialog(edDatabase);
		final JButton btnEDGeneration = new JButton("ED Generation");
		btnEDGeneration.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				EulerCode e = inputPanel.getEulerCode();
				String ec;
				try {
					ec = e.getEulerCode(false, false);
				} catch (Exception ex) {
					ec = ex.getMessage();
				}
				List<Zone> zones = e.getZones();
				if (zones.isEmpty()) {
					generation(e.getCode(true, false, false),
							e.getGaussCode(false, false), ec,
							e.getKnotCode(false, false), null);
				} else {
					StringBuilder zc = new StringBuilder();
					for (Zone z : e.getZones())
						if (!z.label.isEmpty())
							zc.append(z.label + ", ");
					zc.setLength(zc.length() - 2);
					generation(e.getCode(true, false, false),
							e.getGaussCode(false, false), ec,
							e.getKnotCode(false, false), zc.toString());
				}
			}
		});
		buttonsPanel.add(btnEDGeneration);

		final FileNameExtensionFilter fnefCsv = new FileNameExtensionFilter(".csv: curve drawing data", "csv");
		final FileNameExtensionFilter fnefTxt = new FileNameExtensionFilter(".txt: generated euler code", "txt");
		final FileNameExtensionFilter fnefHtml = new FileNameExtensionFilter(".html: generated euler code", "html", "htm");
		final FileNameExtensionFilter fnefGaussTxt = new FileNameExtensionFilter(".txt: generated Gauss code", "txt");
		final FileNameExtensionFilter fnefGaussHtml = new FileNameExtensionFilter(".html: generated Gauss code", "html", "htm");
		final FileNameExtensionFilter fnefSvg = new FileNameExtensionFilter(".svg: scalable vector graphics", "svg");
		final FileNameExtensionFilter fnefHtmlSvg = new FileNameExtensionFilter(".html: scalable vector graphics", "html", "htm");
		final JFileChooser fc = new JFileChooser(){
			private static final long serialVersionUID = -1100560182424627094L;
			private final String defaultExtension = fnefCsv.getExtensions()[0];
			@Override
			// add extension if needed and add confirm dialog
			public void approveSelection() {
				if(getDialogType() == SAVE_DIALOG) {
					File f = getSelectedFile();
					if(f != null) {
						boolean hasWrongExtension = !getFileFilter().accept(f);
						if(hasWrongExtension) {
							for(FileFilter ff : getChoosableFileFilters()) {
								if(ff != getAcceptAllFileFilter() && ff.accept(f)) {
									hasWrongExtension = false;
									setFileFilter(ff);
									break;
								}
							}
						}
						if(hasWrongExtension) {
							FileFilter ff = getFileFilter();
							if(ff instanceof FileNameExtensionFilter) f = new File(f.getPath() + "." + ((FileNameExtensionFilter)ff).getExtensions()[0]);
							else f = new File(f.getPath() + "." + defaultExtension);
							setSelectedFile(f);
						}
						if(f.exists()
								&& JOptionPane.showConfirmDialog(this,
										f.getName() + " exists, overwrite?",
										"Existing file",
										JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION)
							return;
					}
				}
				super.approveSelection();
			}
		};
		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(fnefCsv);
		fc.addChoosableFileFilter(fnefTxt);
		fc.addChoosableFileFilter(fnefHtml);
		fc.addChoosableFileFilter(fnefGaussTxt);
		fc.addChoosableFileFilter(fnefGaussHtml);
		fc.setFileFilter(fnefCsv);
		fc.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(fc.getDialogType() == JFileChooser.SAVE_DIALOG && fc.getSelectedFile() == null) fc.setSelectedFile(new File(""));
			}
		});

		JButton btnLoad = new JButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/sc_open.png")));
		btnLoad.setToolTipText("Load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				fc.removeChoosableFileFilter(fnefSvg);
				fc.removeChoosableFileFilter(fnefHtmlSvg);
				if(fc.showOpenDialog(aThis) == JFileChooser.APPROVE_OPTION) {
					try {
						File f = fc.getSelectedFile();
						if (fnefHtml.accept(f) || fnefTxt.accept(f)) {
							LCode lCode = EulerCodeUtils.loadCodeAutodetect(f);
							String code = lCode.html ? EulerCodeUtils
									.stripHtml(lCode.code) : lCode.code;
							generation(lCode.gauss ? null : code,
									lCode.gauss ? code : null, null, null, null);
						} else {
							EulerCode ec = EulerCodeUtils.loadCurves(f);
							inputPanel.setEulerCode(ec);
						}
					} catch(IOException | ParseException el) {
						JOptionPane.showMessageDialog(aThis, el.getMessage(), "Load error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		buttonsPanel.add(btnLoad);

		JButton btnSave = new JButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/sc_saveas.png")));
		btnSave.setToolTipText("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fc.addChoosableFileFilter(fnefSvg);
				fc.addChoosableFileFilter(fnefHtmlSvg);
				if(fc.showSaveDialog(aThis) == JFileChooser.APPROVE_OPTION) {
					try {
						FileFilter saveType = fc.getFileFilter();
						if(saveType == fnefTxt || saveType == fnefHtml) EulerCodeUtils.saveCode(inputPanel.getEulerCode(), fc.getSelectedFile(), saveType == fnefHtml);
						else if(saveType == fnefGaussTxt || saveType == fnefGaussHtml) EulerCodeUtils.saveGaussCode(inputPanel.getEulerCode(), fc.getSelectedFile(), saveType == fnefGaussHtml);
						else if (saveType == fnefSvg || saveType == fnefHtmlSvg) {
							EulerCode ec = inputPanel.getEulerCode();
							Rectangle bounds = new Rectangle(0, 0, -1, -1);
							for (IncidentPointsPolygon c : ec.getCurves()
									.values())
								bounds.add(c.getBounds());
							SVGGraphics2D g2 = new SVGGraphics2D(
									bounds.width + 2, bounds.height + 2);
							g2.translate(1 - bounds.x, 1 - bounds.y);
							EulerSketchInputPanel.paintEC(g2,
									inputPanel.getEulerCode(),
									inputPanel.getOptions(), null);
							if (saveType == fnefSvg)
								SVGUtils.writeToSVG(fc.getSelectedFile(),
										g2.getSVGElement());
							else
								SVGUtils.writeToHTML(fc.getSelectedFile(),
										"ED", g2.getSVGElement());
						} else EulerCodeUtils.saveCurves(inputPanel.getEulerCode(), fc.getSelectedFile());
					} catch(Exception es) {
						JOptionPane.showMessageDialog(aThis, es.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		buttonsPanel.add(btnSave);

		//System.out.println(EulerSketchGUI.class.getResource("/images/sc_saveas.png")+"\n"+EulerSketchGUI.class.getResource("/images/db_save.png"));
		JButton btnSaveToDatabase = new JButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/db_save.png")));
		
		//JButton btnSaveToDatabase = new JButton(new ImageIcon(EulerSketchGUI.class.getResource("/images/SaveDB.png")));
		btnSaveToDatabase.setToolTipText("Save to Database");
		btnSaveToDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int res = JOptionPane.showOptionDialog(
						null, "Would you like to store it in a database?",
						"Save to a database", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, new String[] {
								"Yes", "No" }, null);

				if (res == JOptionPane.OK_OPTION) { 
					if (edDatabase != null) { // new code
						String dTilte = "Save to a database";
						EulerCode ec = inputPanel.getEulerCode();
						EulerCodeRBC ecrbc = ec.getEulerCodeRBC();
						GaussCodeRBC[] gcs = ecrbc.getGaussCodeRBCs();
						if (gcs.length != 1) {
							JOptionPane.showMessageDialog(EulerSketchGUI.this,
									"The diagram is disconnected.", dTilte,
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						LinkedHashMap<Set<String>, Integer> regionCount = new LinkedHashMap<Set<String>, Integer>();
						for (Zone z : ec.getZones()) {
							LinkedHashSet<String> zs = new LinkedHashSet<String>();
							for (int i = 0; i < z.label.length(); i++)
								zs.add(z.label.substring(i, i + 1));
							regionCount.put(zs,
									z.intlines.size() + z.outlines.size());
						}
						try {
							if (edDatabase.addED(
									new EDData(gcs[0], ecrbc.getOuters()[0],
											regionCount), true))
								JOptionPane.showMessageDialog(
										EulerSketchGUI.this,
										"Diagram added successfully.", dTilte,
										JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane
										.showMessageDialog(
												EulerSketchGUI.this,
												"The diagram is already present in the database.",
												dTilte,
												JOptionPane.ERROR_MESSAGE);
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(EulerSketchGUI.this,
									e1.getMessage(), dTilte,
									JOptionPane.WARNING_MESSAGE);
						}
						return;
					}
					/* OLD DATABASE
					if(inputPanel.getEulerCode().getCurves().size()==0) 
					{
						JOptionPane.showMessageDialog(
								null, "There is no diagram to save",
								"Save to a database",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					GaussCodeRBC ec = inputPanel.getEulerCode().getGaussCodeRBC();
					//inputPanel.getEulerCode().getCurves().entrySet().
					boolean isDisjoint=false;
					boolean isSelfIntersection=false;
					for(Symbol[] w: ec.getGaussCode())
					{
						if((w.length& 1) == 1)
						{
							isDisjoint=true;
							break;
						}
						List<String> list = new ArrayList<String>();
						for(Symbol sym:w )
						{
							if(list.contains(sym.getLabel()))
							{
								isSelfIntersection=true;
								break;
							}
							else list.add(sym.getLabel());
						}
						
					}
					List<List<Symbol[]>> listOfDisjointGaussCodes=Misc.checkDisjointWords(ec.getGaussCode());
					//boolean isDisjoint = listOfDisjointGaussCodes.size()>1;
					if(isDisjoint ||isSelfIntersection || listOfDisjointGaussCodes.size()>1)
					{ 								
						JOptionPane.showMessageDialog(aThis, "No disjoint diagrams or self-intersecting curves are allowed!", "Diagram Restrictions", JOptionPane.ERROR_MESSAGE);
						return;
					}
					Symbol[][] gaussCode=ec.getGaussCode();
					char[] curveLabels=ec.getCurveLabels();
					List <SegmentCode[]> rgb=ec.getRegionBoundaryCode(); 
					btnEDGeneration.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));		   
					  
					Database connect=new Database();
					try 
					{
						
						if(connect.saveToDatabase(gaussCode, curveLabels, rgb))
							JOptionPane.showMessageDialog(aThis,  "Diagram added successfuly", "Success", JOptionPane.INFORMATION_MESSAGE);
					}
					catch(Exception ex)
					{		
						JOptionPane.showMessageDialog(aThis, ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
						btnEDGeneration.setCursor(Cursor.getDefaultCursor());
					}
					btnEDGeneration.setCursor(Cursor.getDefaultCursor());
					*/
					//List<List<Symbol[]>> listOfDisjointGaussCodes=Misc.checkDisjointWords(gaussCode);
					//boolean isDisjoint = listOfDisjointGaussCodes.size()>1;
//					boolean isNonIntersectingCurve= gaussCode.length==1 && gaussCode[0].length==1;
//					if(isNonIntersectingCurve )
//					{ 								
//						JOptionPane.showMessageDialog(aThis, "Disjoint diagram", "No disjoint diagrams allowed!", JOptionPane.INFORMATION_MESSAGE);
//						return;
//					}
//					else
//					{ 
//						btnEDGeneration.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));		   
//						  
//						Database connect=new Database();
//						try 
//						{
//							
//							if(connect.saveToDatabase(gaussCode, curveLabels, rgb))
//								JOptionPane.showMessageDialog(aThis,  "Diagram added successfuly", "Success", JOptionPane.INFORMATION_MESSAGE);
//						}
//						catch(Exception ex)
//						{		
//							JOptionPane.showMessageDialog(aThis, ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
//							btnEDGeneration.setCursor(Cursor.getDefaultCursor());
//						}
//						btnEDGeneration.setCursor(Cursor.getDefaultCursor());
//						
//					}
				} 

			}

		});
		buttonsPanel.add(btnSaveToDatabase);


		JButton btnOptions = new JButton(new ImageIcon(
				EulerSketchGUI.class.getResource("/images/im01.png")));
		btnOptions.setToolTipText("Options");
		btnOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EulerSketchOptionPanel optionPanel = new EulerSketchOptionPanel(inputPanel.getOptions());
				if(JOptionPane.showConfirmDialog(aThis, optionPanel,
						"Options", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
					inputPanel.setOptions(optionPanel.getOptions());
			}
		});
		buttonsPanel.add(btnOptions);

		final EulerSketchSmoothPanel smootPanel = new EulerSketchSmoothPanel(
				inputPanel);
		smootPanel.setVisible(false);

		final JToggleButton btnEulerSmooth = new JToggleButton("EulerSmooth");
		btnEulerSmooth.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				smootPanel.setVisible(btnEulerSmooth.isSelected());
			}
		});
		buttonsPanel.add(btnEulerSmooth);

		buttonsPanel.add(smootPanel);
	}

	public Properties getProperties() {
		EulerSketchInputPanel.Options opt = inputPanel.getOptions();
		properties.setProperty("redoOnRemove",
				String.valueOf(opt.isRedoOnRemove()));
		properties.setProperty("convexCurves",
				String.valueOf(opt.isConvexCurves()));
		properties.setProperty("selfIntersect",
				String.valueOf(opt.isSelfIntersect()));
		properties.setProperty("delCurvWrIntGauss",
				String.valueOf(opt.isDelCurvWrIntGauss()));
		properties.setProperty("antialias", String.valueOf(opt.isAntialias()));
		properties.setProperty("showZones", String.valueOf(opt.isShowZones()));
		properties.setProperty("showOnlyOutlines",
				String.valueOf(opt.isShowOnlyOutlines()));
		properties.setProperty("showCurveLabels",
				String.valueOf(opt.isShowCurveLabels()));
		properties.setProperty("underGapLen",
				String.valueOf(opt.getUnderGapLen()));
		properties.setProperty("releasedPressedDelay",
				String.valueOf(opt.getReleasedPressedDelay()));
		properties.setProperty("pointShowType",
				String.valueOf(opt.getPointShowType()));
		properties.setProperty("segmentShowType",
				String.valueOf(opt.getSegmentShowType()));
		StringBuilder ccols = new StringBuilder();
		for (Color col : opt.getCurveColors())
			ccols.append(colorToString(col) + ",");
		ccols.setLength(ccols.length() - 1);
		properties.setProperty("curveColors", ccols.toString());
		properties.setProperty("backgroundColor",
				colorToString(opt.getBackgroundColor()));
		properties
				.setProperty("eraseColor", colorToString(opt.getEraseColor()));
		properties
				.setProperty("pointColor", colorToString(opt.getPointColor()));
		properties.setProperty("strokeColor",
				colorToString(opt.getStrokeColor()));
		properties.setProperty("curveLabelColor",
				colorToString(opt.getCurveLabelColor()));
		properties.setProperty("segmentLabelColor",
				colorToString(opt.getSegmentLabelColor()));

		properties.setProperty("gaussLeft",
				String.valueOf(chckbxGaussLeft.isSelected()));
		properties.setProperty("eulerLeft",
				String.valueOf(chckbxEulerLeft.isSelected()));
		properties.setProperty("closedcircleLeft",
				String.valueOf(chckbxClosedcircleLeft.isSelected()));
		properties.setProperty("infixLeft",
				String.valueOf(chckbxInfixLeft.isSelected()));

		properties.setProperty("gaussRight",
				String.valueOf(chckbxGaussRight.isSelected()));
		properties.setProperty("eulerRight",
				String.valueOf(chckbxEulerRight.isSelected()));
		properties.setProperty("infixRight",
				String.valueOf(chckbxInfixRight.isSelected()));
		properties.setProperty("closedcircleRight",
				String.valueOf(chckbxClosedcircleRight.isSelected()));

		properties.setProperty("zoom", cbxZoom.getSelectedItem().toString());

		return properties;
	}

	private void generation(String defaultCode, String defaultGaussCode,
			String defaultEulerCode, String defaultKnotCode,
			String defaultZoneCode) {
		Dimension targetSize = scrollPaneInput.getSize();
		generationDialog.reset(
				defaultCode,
				defaultGaussCode,
				defaultEulerCode,
				defaultKnotCode,
				defaultZoneCode,
				Math.round(Math.max(1,
						(targetSize.width - 10) / inputPanel.getScale())),
				Math.round(Math.max(1,
						(targetSize.height - 10) / inputPanel.getScale())),
				inputPanel.getOptions());
		generationDialog.setLocationRelativeTo(this);
		generationDialog.setVisible(true);
		final EulerCode generated = generationDialog.getGenerated();
		if (generated != null) {
			if (generationDialog.isOpenNewWindow()) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							EulerSketch frame = new EulerSketch(generated,
									edDatabase, getProperties());
							frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			} else
				inputPanel.setEulerCode(generated);
		}
	}
}
