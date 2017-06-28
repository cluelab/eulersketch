/*******************************************************************************
 * Copyright (c) 2015 Rafiq Saleh.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package it.unisa.di.cluelab.euler.code.zoneGeneration;
import it.unisa.di.cluelab.euler.code.EulerCode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author Rafiq Saleh
 */
public class ZonesInputDialog extends JPanel {
	private static final long serialVersionUID = 1L;
	protected EulerCode generated;
	protected String defaultCode;
	public String defaultGaussCode;
 	protected JCheckBox chckbxKeepGeneratedAspectRatio;
	protected JTextArea codeTextArea;
	protected boolean openNewWindow;
	protected JPanel optionPanel;
	protected JButton okButton;
	public int numberOfCuves=1; 
	public JButton genBtn;
	public JCheckBox []zones;
	public  List<Set<Character>> zoneSet;
	public boolean [] selectedZones;
	/**
	 * Create the dialog.
	 */
 
	public ZonesInputDialog(final JTextArea codeTextArea, int numberOfCuves)
	{ 
 		this.numberOfCuves=numberOfCuves; 
		this.codeTextArea=codeTextArea; 


		JPanel panel = new JPanel(new GridLayout(1,4));
		JPanel panel2 = new JPanel(new GridLayout(0,4));
		panel2.setBackground(Color.WHITE);
		genBtn = new JButton( "Generate");
		JLabel label= new JLabel("Select a set of zones:");

		JButton selectAllbtn = new JButton("Select All");
		panel.add(label); 
		panel.add(selectAllbtn);


		int n=(int) Math.pow(2,numberOfCuves);
		Set<Character> curveLabels = new HashSet<Character>();
		char ch='A';
		for(int i=0;i<numberOfCuves;i++)
			curveLabels.add(ch++);
		final List<Set<Character>> zoneSet=powerSet(curveLabels);
		zoneSet.remove(new HashSet<Character>());
		//System.out.println(zoneSet +"\n Size"+n);
		zones = new JCheckBox[n-1];
		selectedZones =new boolean[n-1];
		int j=0;
		for(Set<Character> zs:zoneSet)
		{
			String s=  zs.toString();
			panel2.add(zones[j]=new JCheckBox(s)).setBackground(Color.WHITE);
			zones[j].addItemListener(new CheckBoxListener());

			j++;

		}
		setLayout(new BorderLayout());
  		add(panel, BorderLayout.NORTH);
  		add(panel2, BorderLayout.SOUTH);
		JScrollPane scroll = new JScrollPane(panel2);
		setPreferredSize(new Dimension(450, 450));
		add(scroll);

       // setSize(400, 600);
        setVisible(true);

		selectAllbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(JCheckBox zone:zones)
					zone.setSelected(true);
			}
		}); 
		  
		
 		 
	}
	 		
	
	public String getGaussCode() {
		return (generated!=null)? defaultGaussCode : "Null input";
	}
	public EulerCode getGenerated() {
		return generated;
	}
	public boolean isOpenNewWindow() {
		return openNewWindow;
	}
	private class CheckBoxListener implements ItemListener{
        public void itemStateChanged(ItemEvent e)
        {
			for(int i=0; i< zones.length;i++)
			{
				if(e.getSource()== zones[i])
					if(zones[i].isSelected()) {
						selectedZones[i]=true; break;
					}
					else {
						selectedZones[i]=false; break;
					}
				
				
			}
			
        }
     }
	public static List<Set<Character>> powerSet(Set<Character> originalSet)
	{
		List<Set<Character>> sets = new ArrayList<Set<Character>>();
		if (originalSet.isEmpty()) {
			sets.add(new HashSet<Character>());
			return sets;
		}
		List<Character> list = new ArrayList<Character>(originalSet);
		char head = list.get(0);
		Set<Character> rest = new HashSet<Character>(list.subList(1, list.size()));
		for (Set<Character> set : powerSet(rest)) {
			Set<Character> newSet = new HashSet<Character>();
			newSet.add(head);
			newSet.addAll(set);
			sets.add(newSet);
			sets.add(set);
		}
		return sets;
	}

}
