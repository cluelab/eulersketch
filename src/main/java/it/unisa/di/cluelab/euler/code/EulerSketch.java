/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import it.unisa.di.cluelab.euler.code.ZoneGeneration.EDDatabase;

import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * @author Mattia De Rosa
 */
public class EulerSketch extends JFrame {
	private static final long serialVersionUID = 5609173468102120039L;
	private static final String EDDB_FILE;
	private static final String PROPERTIES_FILE;
	static {
		String path = null;
		try {
			path = EulerSketch.class.getProtectionDomain().getCodeSource()
					.getLocation().toURI().getPath();
		} catch (Exception e) {
		}
		if (path == null) {
			try {
				path = ClassLoader.getSystemClassLoader().getResource(".")
						.toURI().getPath();
			} catch (Exception e) {
			}
		}
		if(path != null) {
			try {
				File pf = new File(path);
				if (!pf.isDirectory())
					path = pf.getParent();
				if (!path.endsWith("/") && !path.isEmpty())
					path += "/";
				if(!new File(path).canWrite())
					path = null;
			} catch (Exception e) {
				path = null;
			}
		}
		if (path == null) {
			path = "";
		}
		EDDB_FILE = path + "eddb.dat";
		PROPERTIES_FILE = path + "config.properties";
	}
	private EulerSketchGUI contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Object version = null;
		final Properties props = new Properties();
		try (InputStream stream = EulerSketch.class.getResourceAsStream("/config.properties")) {
			props.load(stream);
			version = props.remove("version"+"");
		} catch (IOException e1) {
		}
		String aboutMessage = "<html><body>EulerSketch " + (version == null ? "" : version)
				+ " by <i>Mattia De Rosa &lt;matderosa@unisa.it&gt;</i><br />with contributions from <i>Rafiq Saleh &lt;rafiqsaleh@hotmail.co.uk&gt;</i><br />All rights reserved &copy;2012-2017. <a href=\"http://cluelab.di.unisa.it\">http://cluelab.di.unisa.it</a><br /><br />The development of the theory and this tool were partially funded by UK EPSRC<br />funded project EP/J010898/1, entitled ADIGE: Automatic Diagram Generation,<br />and by University of Salerno, grant &#8220;Cofinanziamento per attrezzature<br />scientifiche e di supporto, grandi e medie (2005)&#8221;.<br />Research programme provided by Andrew Fish and Gennaro Costagliola.<br /><br />Based on:<ul><li>P. Bottoni, G. Costagliola, and A. Fish. <i>&#8220;Euler diagram encodings&#8221;.<br />Lecture Notes in Artificial Intelligence. 2012. ISSN 0302-9743</i>;</li><li>P. Bottoni, G. Costagliola, M. De Rosa, A. Fish and V. Fuccella.<br /><i>&#8220;Euler diagram codes: interpretation and generation&#8221;</i>. In proc. VINCI 2013;</li><li>G. Costagliola, M. De Rosa, A. Fish, V. Fuccella and R. Saleh.<br /><i>&#8220;Curve-based diagram specification and construction&#8221;</i>. In proc. VL/HCC 2013.</li></ul><br />This program is made available under the terms of the GNU General Public<br />License v3.0 which accompanies this distribution, and is available at<br />http://www.gnu.org/licenses/gpl.html<br /><br />This software include works form:<ul><li>Open Graph Drawing Framework http://www.ogdf.net</li><li>Ocotillo http://www.cs.arizona.edu/~paolosimonetto/</li><li>Planarity-Related Graph Algorithms http://code.google.com/p/planarity/</li><li>Diff Match and Patch http://code.google.com/p/google-diff-match-patch/</li><li>JavaGeom http://geom-java.sourceforge.net/</li><li>Apache Commons Collections http://commons.apache.org/collections/</li><li>Tango LibreOffice Icon Theme</li><li>Wrap Layout</li></ul></body></html>";
		JOptionPane.showMessageDialog(null, aboutMessage,
				"EulerSketch - About", JOptionPane.INFORMATION_MESSAGE);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EDDatabase eddb = null;
					try {
						if (new File(EDDB_FILE).exists()) {
							eddb = new EDDatabase(EDDB_FILE);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (eddb == null)
						eddb = new EDDatabase(getClass().getResourceAsStream(
								"/diagrams/eddatabase.txt"), EDDB_FILE);
					try (FileInputStream in = new FileInputStream(
							PROPERTIES_FILE)) {
						props.load(in);
					} catch (IOException e) {
					}
					EulerSketch frame = new EulerSketch(null, eddb, props);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public EulerSketch() {
		this(null, null, null);
	}

	public EulerSketch(EulerCode eulerCode) {
		this(eulerCode, null, null);
	}

	public EulerSketch(EulerCode eulerCode, EDDatabase edDatabase,
			final Properties properties) {
		setTitle("EulerSketch");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				if (JOptionPane.showConfirmDialog(contentPane,
						"Are you sure you want to quit?", "Close EulerSketch",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
					dispose();
					boolean noOthVis = true;
					for (Window win : Window.getWindows()) {
						if (win != EulerSketch.this && win.isVisible()) {
							noOthVis = false;
							break;
						}
					}
					if (noOthVis) {
						try (FileOutputStream out = new FileOutputStream(
								PROPERTIES_FILE)) {
							contentPane.getProperties().store(out, null);
						} catch (IOException e1) {
						}
						System.exit(0);
					}
				}
			}
		});
		setBounds(0, 0, 840, 440);
		contentPane = new EulerSketchGUI(eulerCode, edDatabase, properties);
		setContentPane(contentPane);
	}
}