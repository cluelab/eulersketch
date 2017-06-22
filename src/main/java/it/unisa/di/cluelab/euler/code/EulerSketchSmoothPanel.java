/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

/**
 * @author Mattia De Rosa
 */
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointsPolygon;
import it.unisa.di.cluelab.euler.code.gausscode.EulerCodeRBC;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JPanel;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JFormattedTextField;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import ocotillo.graph.Graph;
import ocotillo.graph.layout.fdl.impred.Impred;

import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class EulerSketchSmoothPanel extends JPanel {
	private static final long serialVersionUID = 3858876276137437753L;
	private static final double DEFAULT_SCALE = 8;

	/**
	 * Create the panel.
	 */
	public EulerSketchSmoothPanel(final EulerSketchInputPanel inputPanel) {
		setBorder(new TitledBorder(null, "EulerSmooth", TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		setAlignmentX(LEFT_ALIGNMENT);

		final JToggleButton tglbtnRun = new JToggleButton(
				new ImageIcon(
						EulerSketchSmoothPanel.class
								.getResource("/images/av02049.png")));
		tglbtnRun.setSelectedIcon(new ImageIcon(EulerSketchSmoothPanel.class
				.getResource("/images/av02051.png")));
		tglbtnRun.setToolTipText("Run/Stop");
		add(tglbtnRun);

		final ArrayList<EulerCode> undoList = new ArrayList<EulerCode>();
		final JButton btnUndo = new JButton(
				new ImageIcon(
						EulerSketchSmoothPanel.class
								.getResource("/images/sc_undo.png")));
		btnUndo.setToolTipText("Undo");
		btnUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				inputPanel.setEulerCode(undoList.remove(undoList.size() - 1));
				btnUndo.setEnabled(!undoList.isEmpty());
			}
		});
		btnUndo.setEnabled(false);
		add(btnUndo);

		final JPanel panelOptions = new JPanel();

		final JToggleButton tglbtnOptions = new JToggleButton(new ImageIcon(
				EulerSketchSmoothPanel.class.getResource("/images/im01.png")));
		tglbtnOptions.setToolTipText("Options");
		tglbtnOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panelOptions.setVisible(tglbtnOptions.isSelected());
			}
		});
		add(tglbtnOptions);

		add(panelOptions);
		panelOptions.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panelOptions.setAlignmentX(LEFT_ALIGNMENT);

		JLabel lblIterations = new JLabel("Iterat:");
		panelOptions.add(lblIterations);

		JFormattedTextField.AbstractFormatterFactory posIntForFactory = new JFormattedTextField.AbstractFormatterFactory() {
			@Override
			public AbstractFormatter getFormatter(JFormattedTextField tf) {
				return new JFormattedTextField.AbstractFormatter() {
					private static final long serialVersionUID = 1056669858577470253L;

					@Override
					public String valueToString(Object value)
							throws ParseException {
						if (value == null)
							throw new ParseException(null, 0);
						else
							return value.toString();
					}

					@Override
					public Object stringToValue(String text)
							throws ParseException {
						Integer v = 0;
						try {
							v = Integer.valueOf(text);
						} catch (NumberFormatException e) {
						}
						if (v <= 0)
							throw new ParseException(null, 0);
						else
							return v;
					}
				};
			}
		};

		final JFormattedTextField frmtdtxtfldIterations = new JFormattedTextField(
				posIntForFactory);
		panelOptions.add(frmtdtxtfldIterations);
		frmtdtxtfldIterations.setColumns(3);
		frmtdtxtfldIterations.setValue(Integer.valueOf(100));

		JLabel lblDistance = new JLabel(" Dist:");
		panelOptions.add(lblDistance);

		final JFormattedTextField frmtdtxtfldDistance = new JFormattedTextField(
				posIntForFactory);
		panelOptions.add(frmtdtxtfldDistance);
		frmtdtxtfldDistance.setColumns(2);
		frmtdtxtfldDistance.setValue(Integer.valueOf(5));

		final JCheckBox chckbxInd = new JCheckBox("Ind", true);
		panelOptions.add(chckbxInd);

		final JCheckBox chckbxMov = new JCheckBox("Mov");
		panelOptions.add(chckbxMov);

		final JCheckBox chckbxSep = new JCheckBox("Sep", true);
		panelOptions.add(chckbxSep);
		
		final JCheckBox chckbxEqChk = new JCheckBox("EqChk", true);
		panelOptions.add(chckbxEqChk);

		tglbtnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final boolean inputPanelEnabled = inputPanel.isEnabled();
				if (tglbtnRun.isSelected()) {
					EulerCode ec = inputPanel.getEulerCode();
					Map<Character, IncidentPointsPolygon> curves = ec
							.getCurves();
					if (curves.isEmpty()) {
						tglbtnRun.setSelected(false);
						return;
					}

					if (inputPanelEnabled)
						inputPanel.setEnabled(false);
					btnUndo.setEnabled(false);

					undoList.add(ec);

					Rectangle r = null;
					for (IncidentPointsPolygon p : curves.values()) {
						if (r == null)
							r = p.getBounds();
						else
							r.add(p.getBounds());
					}
					final Rectangle bound = r;

					EulerCodeRBC ecRBC = null;
					try {
						ecRBC = inputPanel.getEulerCode().getEulerCodeRBC();
					} catch (Exception e) {
					}
					final Object orCode = ecRBC == null ? inputPanel
							.getEulerCode().getGaussCodeRBC() : ecRBC;

					final Graph g = EulerSmooth
							.toGraph(ec, true, DEFAULT_SCALE);
					final Impred im = EulerSmooth.getImpred(g,
							(Integer) frmtdtxtfldDistance.getValue()
									* DEFAULT_SCALE, chckbxInd.isSelected(),
							chckbxMov.isSelected(), chckbxSep.isSelected());

					final int nIter = (Integer) frmtdtxtfldIterations
							.getValue();

					new Thread() {
						public void run() {
							for (int i = 0; tglbtnRun.isSelected() && i < nIter; i++) {
								im.iterate(1);
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										EulerCode newED = EulerSmooth
												.toEulerCode(g, bound);
										Object newCode = null;
										try {
											newCode = orCode instanceof EulerCodeRBC ? newED
													.getEulerCodeRBC() : newED
													.getGaussCodeRBC();
										} catch (Exception e) {
										}

										if (newCode != null
												&& (!chckbxEqChk.isSelected() || orCode
														.equals(newCode))) {
											inputPanel.setEulerCode(newED);
										} else {
											System.err
													.println("EulerSmooth skipped: different codes [original: "
															+ orCode
															+ ", generated: "
															+ newCode + "]");
										}
									}
								});
							}
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									tglbtnRun.setSelected(false);
									if (inputPanelEnabled)
										inputPanel.setEnabled(true);
									btnUndo.setEnabled(true);
								}
							});
						}
					}.start();
				} else {
					if (inputPanelEnabled)
						inputPanel.setEnabled(true);
					btnUndo.setEnabled(true);
				}
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panelOptions.setVisible(false);
			}
		});
	}

}
