/*******************************************************************************
 * Copyright (c) 2013 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.EulerCodeUtils.CodeZones;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.DisconnectedEDs;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDDatabase;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDData;
import it.unisa.di.cluelab.euler.code.ZoneGeneration.ZonesInputDialog;
import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.GaussCodeRBC;
import it.unisa.di.cluelab.euler.code.gausscode.Symbol;
import it.unisa.di.cluelab.euler.code.gausscode.USymbol;
import it.unisa.di.cluelab.euler.code.vennGeneration.DiagramCode;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import uk.co.timwise.wraplayout.WrapLayout;
import javax.swing.JRadioButton;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * @author Mattia De Rosa
 */
public class EulerSketchEDGenerationDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VENN_CODE = "Venn: 2";
	private static final String DEFAULT_ZONES_CODE = "A, B, AB";
	private int numberOfCuvesSelected;
	private EulerCode generated;
	private String curCode;
	private String curGaussCode;
	private String curEulerCode;
	private String curKnotCode;
	private String curVennCode;
	private String curZonesCode;
	private JTextField widthTextField;
	private JTextField heightTextField;
	private JCheckBox chckbxKeepGeneratedAspectRatio;
	private JTextArea codeTextArea;
	private boolean openNewWindow;
	private JPanel optionPanel;
	private JPanel zoneOptionsPanel;
	private JButton okButton;
	private JComboBox<String> noCurves;
	private JRadioButton rdbtnStatic;
	private JRadioButton rdbtnGauss;
	private JRadioButton rdbtnEuler;
	private JRadioButton rdbtnKnot;
	private JRadioButton rdbtnVenn;
	private JRadioButton rdbtnZones;
	private EulerSketchInputPanel.Options previewOptions;

	/**
	 * Create the dialog.
	 */
	public EulerSketchEDGenerationDialog() {
		this(null);
	}

	public EulerSketchEDGenerationDialog(final EDDatabase edDatabase) {
		setTitle("ED Generation");
		setModalityType(ModalityType.APPLICATION_MODAL);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				generated = null;
			}
		});
		setBounds(100, 100, 595, 340);
		setResizable(true);
		getContentPane().setLayout(new BorderLayout());

		optionPanel = new JPanel();
		getContentPane().add(optionPanel, BorderLayout.NORTH);
		optionPanel.setLayout(new WrapLayout(WrapLayout.LEFT));

		ButtonGroup codeTypeGroup = new ButtonGroup();

		rdbtnStatic = new JRadioButton("static");
		rdbtnStatic.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curCode);
					codeTextArea.setCaretPosition(0);
					break;
				case ItemEvent.DESELECTED:
					curCode = codeTextArea.getText();
				}
			}
		});
		codeTypeGroup.add(rdbtnStatic);
		optionPanel.add(rdbtnStatic);

		rdbtnGauss = new JRadioButton("gauss");
		rdbtnGauss.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curGaussCode);
					codeTextArea.setCaretPosition(0);
					break;
				case ItemEvent.DESELECTED:
					curGaussCode = codeTextArea.getText();
				}
			}
		});
		codeTypeGroup.add(rdbtnGauss);
		optionPanel.add(rdbtnGauss);

		rdbtnEuler = new JRadioButton("euler");
		rdbtnEuler.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curEulerCode);
					codeTextArea.setCaretPosition(0);
					break;
				case ItemEvent.DESELECTED:
					curEulerCode = codeTextArea.getText();
				}
			}
		});
		codeTypeGroup.add(rdbtnEuler);
		optionPanel.add(rdbtnEuler);
		
		rdbtnKnot = new JRadioButton("knot");
		rdbtnKnot.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curKnotCode);
					codeTextArea.setCaretPosition(0);
					break;
				case ItemEvent.DESELECTED:
					curKnotCode = codeTextArea.getText();
				}
			}
		});
		codeTypeGroup.add(rdbtnKnot);
		optionPanel.add(rdbtnKnot);

		rdbtnVenn = new JRadioButton("venn");
		rdbtnVenn.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curVennCode);
					codeTextArea.setCaretPosition(0);
					break;
				case ItemEvent.DESELECTED:
					curVennCode = codeTextArea.getText();
				}
			}
		});
		codeTypeGroup.add(rdbtnVenn);
		optionPanel.add(rdbtnVenn);

		rdbtnZones = new JRadioButton("zones");
		rdbtnZones.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				switch (event.getStateChange()) {
				case ItemEvent.SELECTED:
					codeTextArea.setText(curZonesCode);
					codeTextArea.setCaretPosition(0);
					zoneOptionsPanel.setVisible(true);
					noCurves.setSelectedIndex(0);
					break;
				case ItemEvent.DESELECTED:
					curZonesCode = codeTextArea.getText();
					zoneOptionsPanel.setVisible(false);
				}
			}
		});
		codeTypeGroup.add(rdbtnZones);
		optionPanel.add(rdbtnZones);

		JSeparator separator = new JSeparator();
		optionPanel.add(separator);

		JPanel targetSizePanel = new JPanel();
		targetSizePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		optionPanel.add(targetSizePanel);
		JLabel lblTargetSize = new JLabel("target size:");
		targetSizePanel.add(lblTargetSize);

		widthTextField = new JTextField();
		targetSizePanel.add(widthTextField);
		widthTextField.setHorizontalAlignment(JTextField.RIGHT);
		widthTextField.setColumns(4);

		JLabel lblX = new JLabel("x");
		targetSizePanel.add(lblX);

		heightTextField = new JTextField();
		targetSizePanel.add(heightTextField);
		heightTextField.setColumns(4);

		chckbxKeepGeneratedAspectRatio = new JCheckBox(
				"keep generated aspect ratio", true);
		optionPanel.add(chckbxKeepGeneratedAspectRatio);

		zoneOptionsPanel = new JPanel();
		zoneOptionsPanel.setBorder(
				BorderFactory.createTitledBorder("Zone label wizard"));
		zoneOptionsPanel.setVisible(false);
		optionPanel.add(separator);

		zoneOptionsPanel.add(new JLabel("No. of curves:"));

		noCurves = new JComboBox<String>();
		noCurves.setModel(new DefaultComboBoxModel<String>(new String[] { "2",
				"3", "4", "5", "6", "7" }));
		noCurves.setSelectedIndex(0);
		noCurves.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				@SuppressWarnings("unchecked")
				JComboBox<String> combo = (JComboBox<String>) event.getSource();
				numberOfCuvesSelected = Integer.parseInt((String) combo
						.getSelectedItem());
			}
		});
		zoneOptionsPanel.add(noCurves);
		JButton zoneLabels = new JButton("Generate");
		zoneOptionsPanel.add(zoneLabels);
		optionPanel.add(zoneOptionsPanel);
		zoneLabels.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				numberOfCuvesSelected = Integer.parseInt((String) noCurves
						.getSelectedItem());
				ZonesInputDialog zid = new ZonesInputDialog(codeTextArea,
						numberOfCuvesSelected);
				int res = JOptionPane.showOptionDialog(
						EulerSketchEDGenerationDialog.this, zid,
						"Euler Code Generation Preview",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK",
								"Cancel" }, null);
				if (res != JOptionPane.NO_OPTION) {
					String defaultZonesCode = (zid.selectedZones.length > 1) ? "["
							: "";
					Boolean selected = false;
					for (int i = 0; i < zid.selectedZones.length; i++) {
						if (zid.selectedZones[i]) {
							selected = true;
							String temp = zid.zones[i]
									.getText()
									.substring(1,
											zid.zones[i].getText().length() - 1)
									.replaceAll(",| ", "");
							defaultZonesCode += temp + ", ";
						}

					}
					defaultZonesCode = defaultZonesCode.substring(0,
							defaultZonesCode.length() - 2) + "]";
					if (selected)
						codeTextArea.setText(defaultZonesCode);
				}

			}
		});

		codeTextArea = new JTextArea();

		JScrollPane codeScrollPane = new JScrollPane(codeTextArea);
		getContentPane().add(codeScrollPane, BorderLayout.CENTER);

		JPanel okCancelButtonPanel = new JPanel();
		okCancelButtonPanel.setLayout(new WrapLayout(WrapLayout.CENTER));
		getContentPane().add(okCancelButtonPanel, BorderLayout.SOUTH);

		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				generated = null;
				int width = 0;
				try {
					width = Integer.parseInt(widthTextField.getText());
				} catch (NumberFormatException nfe) {
				}
				int height = 0;
				try {
					height = Integer.parseInt(heightTextField.getText());
				} catch (NumberFormatException nfe) {
				}

				try {
					final EulerSketchEDGenerationPreview preview;
					if (rdbtnVenn.isSelected()) {
						String text = codeTextArea.getText();
						int n = 0;
						if (text.toLowerCase().startsWith("venn:")) {
							try {
								n = Integer.parseInt(text.substring(5)
										.trim());
							} catch (NumberFormatException nfe) {
							}
						}
						if (n <= 0)
							throw new ParseException("Illegal value.", 0);
						ObjectInputStream ois = null;
						DiagramCode dc = null;
						try {
							ois = new ObjectInputStream(getClass()
									.getResourceAsStream(
										"/diagrams/venn" + n + ".dat"));
							dc = (DiagramCode) ois.readObject();
							preview = new EulerSketchEDGenerationPreview(
									dc, dc.getOuterFace(), width, height,
									chckbxKeepGeneratedAspectRatio
											.isSelected());
						} catch (FileNotFoundException fnfe) {
							throw new ParseException("Illegal value.", 0);
						} catch (IOException | ClassNotFoundException e) {
							throw new ParseException(e.getMessage(), 0);
						} finally {
							if (ois != null)
								try {
									ois.close();
								} catch (IOException e) {
								}
						}
					} else if (rdbtnGauss.isSelected()) {
						GaussCodeRBC gc = new GaussCodeRBC(codeTextArea
								.getText(), true);
						preview = new EulerSketchEDGenerationPreview(gc,
								width, height,
								chckbxKeepGeneratedAspectRatio.isSelected());
					} else if (rdbtnEuler.isSelected()) {
						EulerCodeRBC ec = new EulerCodeRBC(codeTextArea
								.getText(), true);
						preview = new EulerSketchEDGenerationPreview(ec,
								width, height,
								chckbxKeepGeneratedAspectRatio.isSelected());
					} else if (rdbtnKnot.isSelected()) {
						String[] rows = codeTextArea.getText().trim()
								.split(",[ ]*");
						Symbol[][] gaussCode = new Symbol[rows.length][];
						char[] curveLabels = new char[rows.length];
						for (int i = 0; i < rows.length; i++) {
							curveLabels[i] = (char) ('A' + i);
							String[] points = rows[i].split(" ");
							gaussCode[i] = new Symbol[points.length];
							for (int j = 0; j < points.length; j++) {
								String point = points[j];
								int last = point.length() - 1;
								if (last < 2)
									throw new ParseException(
											"Wrong symbol at line "
													+ (i + 1) + ".", i + 1);
								char uo = point.charAt(0);
								if (uo != 'U' && uo != 'u' && uo != 'O'
										&& uo != 'o')
									throw new ParseException(
											"Wrong U/O at line " + (i + 1)
													+ ".", i + 1);
								char sign = point.charAt(last);
								if (sign != '+' && sign != '-')
									throw new ParseException(
											"Wrong sign at line " + (i + 1)
													+ ".", i + 1);
									gaussCode[i][j] = new USymbol(point
											.substring(1, point.length() - 1),
											sign, uo == 'U' || uo == 'u');
							}
						}
						GaussCodeRBC gc = new GaussCodeRBC(curveLabels,
								gaussCode, true);
						preview = new EulerSketchEDGenerationPreview(gc,
								width, height,
								chckbxKeepGeneratedAspectRatio.isSelected());
					} else if (rdbtnStatic.isSelected()) {
						CodeZones cz = new CodeZones(codeTextArea.getText());
						preview = new EulerSketchEDGenerationPreview(cz, width,
								height, chckbxKeepGeneratedAspectRatio
										.isSelected());
					} else if (rdbtnZones.isSelected()) {
						LinkedHashSet<Set<String>> zones = new LinkedHashSet<Set<String>>();
						for (String s : codeTextArea.getText().split(
								"[ ]*[,\\[\\]][ ]*")) {
							if (!s.isEmpty()) {
								LinkedHashSet<String> zone = new LinkedHashSet<String>();
								for (int i = 0; i < s.length(); i++)
									zone.add(s.substring(i, i + 1));
								zones.add(zone);
							}
						}
						Map<Set<Set<String>>, Set<String>> dzs = DisconnectedEDs
								.renameDuplicateCurves(DisconnectedEDs
										.disconnectedEDZones(zones));
						boolean askSuperSet = true;
						if (dzs.size() > 1) {
							Object[] options = { "Exact match",
									"Alternative diagrams (superset)" };
							if (JOptionPane.showOptionDialog(
									EulerSketchEDGenerationDialog.this,
									"ED with disconnected/duplicated curves."
											+ "\nView alternative diagrams containing the given zones as a subset?",
									"View Alternative Diagrams",
									JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE, null, options,
									options[0]) == 1) {
								askSuperSet = false;
								dzs = new HashMap<Set<Set<String>>, Set<String>>();
								dzs.put(zones, Collections.<String> emptySet());
							}
						}
						ArrayList<Entry<List<EDData>, Set<String>>> codes = new ArrayList<Entry<List<EDData>, Set<String>>>();
						for (Entry<Set<Set<String>>, Set<String>> e : dzs
								.entrySet()) {
							Set<Set<String>> cZones = e.getKey();
							List<EDData> edlist = edDatabase.getEDs(cZones);
							if (edlist.isEmpty()) {
								if (askSuperSet) {
									int selectedOption = JOptionPane
											.showConfirmDialog(
													EulerSketchEDGenerationDialog.this,
													"No exact match found!"
															+ "\nView alternative diagrams containing the given zones as a subset?",
													"View Alternative Diagrams",
													JOptionPane.YES_NO_OPTION);
									if (selectedOption == JOptionPane.YES_OPTION)
										askSuperSet = false;
									else
										return;
								}
								edlist = edDatabase.getSuperSetEDs(cZones);
								if (edlist.isEmpty()) {
									JOptionPane.showMessageDialog(
											EulerSketchEDGenerationDialog.this,
											"No superset match found.");
									return;
								}
							}
							codes.add(new AbstractMap.SimpleEntry<List<EDData>, Set<String>>(
									edlist, e.getValue()));
						}
						preview = new EulerSketchEDGenerationPreview(codes,
								width, height, chckbxKeepGeneratedAspectRatio
										.isSelected());
					} else {
						return;
					}
					if (previewOptions != null)
						preview.setPreviewOptions(previewOptions);
					int res = JOptionPane.showOptionDialog(
							EulerSketchEDGenerationDialog.this, preview,
							"Preview", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE, null, new String[] {
									"OK", "OK (new window)", "Cancel" }, null);
					openNewWindow = res == JOptionPane.NO_OPTION;
					if (res != JOptionPane.CANCEL_OPTION) {
						generated = preview.getEulerCode();
						if (generated != null) {
							if (preview.getEulerCodeException() == null
									|| JOptionPane
											.showConfirmDialog(
													EulerSketchEDGenerationDialog.this,
													"ED generated with error(s). Select it anyway?",
													"Generation error",
													JOptionPane.YES_NO_OPTION,
													JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION)
								setVisible(false);
						}
					}
				} catch (ParseException | IllegalStateException pe) {
					if (!rdbtnZones.isSelected()) {
						JOptionPane.showMessageDialog(
								EulerSketchEDGenerationDialog.this,
								pe.getMessage(), "Input code error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		okCancelButtonPanel.add(okButton);
		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				generated = null;
				setVisible(false);
			}
		});
		okCancelButtonPanel.add(cancelButton);

		reset(null, null, null, null, null, 1, 1, null);
	}

	public void reset(String defaultCode, String defaultGaussCode,
			String defaultEulerCode, String defaultKnotCode,
			String defaultZoneCode, int targetWidth, int targetHeight,
			EulerSketchInputPanel.Options previewOptions) {
		String defText = "write code here";
		curCode = defaultCode == null ? defText : defaultCode;
		curGaussCode = defaultGaussCode == null ? defText : defaultGaussCode;
		curEulerCode = defaultEulerCode == null ? defText : defaultEulerCode;
		curKnotCode = defaultKnotCode == null ? defText : defaultKnotCode;
		curVennCode = DEFAULT_VENN_CODE;
		curZonesCode = defaultZoneCode == null ? DEFAULT_ZONES_CODE
				: defaultZoneCode;
		generated = null;
		if (rdbtnStatic.isSelected())
			codeTextArea.setText(curCode);
		else if (rdbtnGauss.isSelected())
			codeTextArea.setText(curGaussCode);
		else if (rdbtnEuler.isSelected())
			codeTextArea.setText(curEulerCode);
		else if (rdbtnKnot.isSelected())
			codeTextArea.setText(curKnotCode);
		else if (rdbtnVenn.isSelected())
			codeTextArea.setText(curVennCode);
		else if (rdbtnZones.isSelected())
			codeTextArea.setText(curZonesCode);
		widthTextField.setText(String.valueOf(targetWidth));
		heightTextField.setText(String.valueOf(targetHeight));
		this.previewOptions = previewOptions;
	}

	public EulerCode getGenerated() {
		return generated;
	}

	public boolean isOpenNewWindow() {
		return openNewWindow;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				EulerSketchEDGenerationDialog sd = new EulerSketchEDGenerationDialog();
				sd.setVisible(true);
			}
		});
	}
}