/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPoint;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointRef;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointsPolygon;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;

/**
 * @author Mattia De Rosa
 */
public class EulerSketchInputPanel extends JPanel {
	public static class Options {
		public static final int SHOW_NONE = 0;
		public static final int SHOW_POINT = 1;
		public static final int SHOW_NUMBER = 2;
		public static final int SHOW_POINT_WITH_NUMBER = 3;
		public static final int SHOW_POINT_WITH_LABEL = 4;
		private boolean showZones = false;
		private boolean redoOnRemove = true;
		private boolean convexCurves = false;
		private boolean selfIntersect = false;
		private boolean delCurvWrIntGauss = true;
		private boolean showOnlyOutlines = false;
		private boolean showCurveLabels = true;
		private int underGapLen = 0;
		private int releasedPressedDelay = 0;
		private Object antialias = RenderingHints.VALUE_ANTIALIAS_ON;
		private int pointShowType = SHOW_POINT_WITH_NUMBER;
		private int segmentShowType = SHOW_NONE;
		private boolean autoPreferredSize = true;
		private double curveMinArea = 16.; // TODO provisional minimum area
		private Color[] curveColors = new Color[]{Color.YELLOW, Color.CYAN, Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE, Color.PINK};
		private Color[] alphaCurveColors = makeAlphaColors(curveColors, 0.5f);
		private Color backgroundColor = Color.WHITE;
		private Color eraseColor = new Color(64, 64, 64, 64);
		private Color pointColor = Color.BLACK;
		private Color strokeColor = Color.BLACK;
		private Color curveLabelColor = Color.BLACK;
		private Color segmentLabelColor = Color.BLUE;
		
		private static Color[] makeAlphaColors(Color[] colors, float a) {
			Color[] alphaColors = new Color[colors.length];
			for(int i = 0; i < colors.length; i++) {
				Color c = colors[i];
				alphaColors[i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(c.getAlpha() * a))));
			}
			return alphaColors;
		}
		
		public Options() {
		}

		public Options(boolean showZones, boolean redoOnRemove,
				boolean convexCurves, boolean selfIntersect,
				boolean delCurvWrIntGauss, boolean showOnlyOutlines,
				boolean showCurveLabels, int underGapLen,
				int releasedPressedDelay, boolean antialias, int pointShowType,
				int segmentShowType, boolean autoPreferredSize,
				double curveMinArea, Color[] curveColors,
				Color backgroundColor, Color eraseColor, Color pointColor,
				Color strokeColor, Color curveLabelColor,
				Color segmentLabelColor) {
			this.showZones = showZones;
			this.redoOnRemove = redoOnRemove;
			this.convexCurves = convexCurves;
			this.selfIntersect = selfIntersect;
			this.delCurvWrIntGauss = delCurvWrIntGauss;
			this.showOnlyOutlines = showOnlyOutlines;
			this.showCurveLabels = showCurveLabels;
			this.underGapLen = underGapLen;
			this.setReleasedPressedDelay(releasedPressedDelay);
			this.setAntialias(antialias);
			this.setPointShowType(pointShowType);
			this.setSegmentShowType(segmentShowType);
			this.autoPreferredSize = autoPreferredSize;
			this.curveMinArea = curveMinArea;
			this.setBackgroundColor(backgroundColor);
			this.setCurveColors(curveColors);
			this.setEraseColor(eraseColor);
			this.setPointColor(pointColor);
			this.setStrokeColor(strokeColor);
			this.setCurveColors(curveColors);
			this.setSegmentLabelColor(segmentLabelColor);
		}
		
		public Options(Options options) {
			this.setOptions(options);
		}
		
		public void setOptions(Options options) {
			this.showZones = options.isShowZones();
			this.redoOnRemove = options.isRedoOnRemove();
			this.convexCurves = options.isConvexCurves();
			this.selfIntersect = options.isSelfIntersect();
			this.delCurvWrIntGauss = options.isDelCurvWrIntGauss();
			this.showOnlyOutlines = options.isShowOnlyOutlines();
			this.showCurveLabels = options.isShowCurveLabels();
			this.underGapLen = options.getUnderGapLen();
			this.setReleasedPressedDelay(options.getReleasedPressedDelay());
			this.setAntialias(options.isAntialias());
			this.setPointShowType(options.getPointShowType());
			this.setSegmentShowType(options.getSegmentShowType());
			this.autoPreferredSize = options.isAutoPreferredSize();
			this.curveMinArea = options.getCurveMinArea();
			this.setBackgroundColor(options.getBackgroundColor());
			this.setCurveColors(options.getCurveColors());
			this.setEraseColor(options.getEraseColor());
			this.setPointColor(options.getPointColor());
			this.setStrokeColor(options.getStrokeColor());
			this.setCurveLabelColor(options.getCurveLabelColor());
			this.setSegmentLabelColor(options.getSegmentLabelColor());
		}

		public boolean isShowZones() {
			return showZones;
		}

		public void setShowZones(boolean showZones) {
			this.showZones = showZones;
		}

		public boolean isRedoOnRemove() {
			return redoOnRemove;
		}

		public void setRedoOnRemove(boolean redoOnRemove) {
			this.redoOnRemove = redoOnRemove;
		}

		public boolean isConvexCurves() {
			return convexCurves;
		}

		public void setConvexCurves(boolean convexCurves) {
			this.convexCurves = convexCurves;
		}

		public boolean isSelfIntersect() {
			return selfIntersect;
		}

		public void setSelfIntersect(boolean selfIntersect) {
			this.selfIntersect = selfIntersect;
		}

		public boolean isDelCurvWrIntGauss() {
			return delCurvWrIntGauss;
		}

		public void setDelCurvWrIntGauss(boolean delCurvWrIntGauss) {
			this.delCurvWrIntGauss = delCurvWrIntGauss;
		}

		public boolean isShowOnlyOutlines() {
			return showOnlyOutlines;
		}

		public void setShowOnlyOutlines(boolean showOnlyOutlines) {
			this.showOnlyOutlines = showOnlyOutlines;
		}

		public boolean isShowCurveLabels() {
			return showCurveLabels;
		}

		public void setShowCurveLabels(boolean showCurveLabels) {
			this.showCurveLabels = showCurveLabels;
		}

		public int getUnderGapLen() {
			return underGapLen;
		}

		public void setUnderGapLen(int underGapLen) {
			this.underGapLen = underGapLen;
		}

		public int getReleasedPressedDelay() {
			return releasedPressedDelay;
		}

		public void setReleasedPressedDelay(int releasedPressedDelay) {
			if(releasedPressedDelay < 0) throw new IllegalArgumentException("Negative releasedPressedDelay.");
			this.releasedPressedDelay = releasedPressedDelay;
		}

		public boolean isAntialias() {
			return antialias == RenderingHints.VALUE_ANTIALIAS_ON;
		}

		public void setAntialias(boolean antialias) {
			this.antialias = antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
		}

		public int getPointShowType() {
			return pointShowType;
		}

		public void setPointShowType(int pointShowType) {
			if(pointShowType != SHOW_NONE && pointShowType != SHOW_POINT
					&& pointShowType != SHOW_NUMBER
					&& pointShowType != SHOW_POINT_WITH_NUMBER
					&& pointShowType != SHOW_POINT_WITH_LABEL)
				throw new IllegalArgumentException("Invalid pointShowType.");
			this.pointShowType = pointShowType;
		}

		public int getSegmentShowType() {
			return segmentShowType;
		}

		public void setSegmentShowType(int segmentShowType) {
			if(segmentShowType != SHOW_NONE && segmentShowType != SHOW_POINT
					&& segmentShowType != SHOW_NUMBER
					&& segmentShowType != SHOW_POINT_WITH_NUMBER
					&& segmentShowType != SHOW_POINT_WITH_LABEL)
				throw new IllegalArgumentException("Invalid segmentShowType.");
			this.segmentShowType = segmentShowType;
		}

		public boolean isAutoPreferredSize() {
			return autoPreferredSize;
		}

		public void setAutoPreferredSize(boolean autoPreferredSize) {
			this.autoPreferredSize = autoPreferredSize;
		}

		public double getCurveMinArea() {
			return curveMinArea;
		}

		public void setCurveMinArea(double curveMinArea) {
			this.curveMinArea = curveMinArea;
		}

		public Color[] getCurveColors() {
			return curveColors;
		}

		public void setCurveColors(Color[] curveColors) {
			if(curveColors == null) throw new NullPointerException("Null curveColors.");
			if(curveColors.length == 0) throw new IllegalArgumentException("Invalid curveColors.");
			Color[] newColors = new Color[curveColors.length];
			for(int i = 0; i < newColors.length; i++) {
				if(curveColors[i] == null) throw new NullPointerException("Null curveColors[" + i + "].");
				newColors[i] = curveColors[i];
			}
			this.curveColors = newColors;
			this.alphaCurveColors = makeAlphaColors(this.curveColors, 0.5f);
		}

		public Color getBackgroundColor() {
			return backgroundColor;
		}

		public void setBackgroundColor(Color backgroundColor) {
			this.backgroundColor = backgroundColor;
		}

		public Color getEraseColor() {
			return eraseColor;
		}

		public void setEraseColor(Color eraseColor) {
			if(eraseColor == null) throw new NullPointerException("Null eraseColor.");
			this.eraseColor = eraseColor;
		}

		public Color getPointColor() {
			return pointColor;
		}

		public void setPointColor(Color pointColor) {
			if(pointColor == null) throw new NullPointerException("Null pointColor.");
			this.pointColor = pointColor;
		}

		public Color getStrokeColor() {
			return strokeColor;
		}

		public void setStrokeColor(Color strokeColor) {
			if(strokeColor == null) throw new NullPointerException("Null strokeColor.");
			this.strokeColor = strokeColor;
		}
		
		public Color getCurveLabelColor() {
			return curveLabelColor;
		}

		public void setCurveLabelColor(Color curveLabelColor) {
			this.curveLabelColor = curveLabelColor;
		}

		public Color getSegmentLabelColor() {
			return segmentLabelColor;
		}

		public void setSegmentLabelColor(Color segmentLabelColor) {
			this.segmentLabelColor = segmentLabelColor;
		}
	}
	private static final long serialVersionUID = 5368016337993492872L;

	private static final Cursor[] cursors = new Cursor[] {Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR), circleCursor(12), Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)};
	private static final int POINT_CLICK_THRESHOLD = 8; //TODO chose a good threshold
	
	public static final int DRAW_INPUT_MODE = 0;
	public static final int ERASE_INPUT_MODE = 1;
	public static final int MOVE_INPUT_MODE = 2;

	private final Options options;
	private EulerCode eulerCode;
	private Polygon curStroke;
	private Point eraserPos;
	private Point moverStart;
	private Point moverMin;
	private Point moverCur;
	private Entry<Character, IncidentPointsPolygon> movedObject;
	private int inputMode;
	private boolean changedGraphics;
	private BufferedImage paintBuffer = null;
	private float scale = 1f;
	private Timer timer = new Timer(0, null);
	
	public EulerSketchInputPanel() {
		super();
		this.eulerCode = new EulerCode();
		this.options = new Options();
		initialize();
	}
	
	public EulerSketchInputPanel(Options options) {
		super();
		this.eulerCode = new EulerCode();
		this.options = new Options(options);
		initialize();
	}

	private void initialize() {
		this.setInputMode(DRAW_INPUT_MODE);
		this.setAutoscrolls(true);
		final EulerSketchInputPanel tip = this;
		
		this.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (tip.isEnabled() && options.underGapLen > 0
						&& e.getClickCount() == 2) {
					double ex = e.getX() / tip.scale;
					double ey = e.getY() / tip.scale;
					double bestD = Double.MAX_VALUE;
					Entry<Integer, IncidentPoint> best = null;
					for (Entry<Integer, IncidentPoint> ep : tip.eulerCode
							.getIncidentPoints().entrySet()) {
						double d = ep.getValue().distance(ex, ey);
						if (d < bestD) {
							bestD = d;
							best = ep;
						}
					}
					if (bestD <= POINT_CLICK_THRESHOLD) {
						Map<Character, IncidentPointsPolygon> curves = eulerCode
								.getCurves();
						Integer pn = best.getKey();
						for (Character c : best.getValue().getIncidentCurves()) {
							IncidentPointsPolygon ip = curves.get(c);
							IncidentPointRef ipr = ip.getIncidentPointRefsMap()
									.get(pn);
							ipr.setUnder(!ipr.isUnder());
							ipr = ip.getIncidentPointRefsMap().get(-pn);
							if (ipr != null)
								ipr.setUnder(!ipr.isUnder());
						}
						tip.repaint();
						ChangeEvent ce = new ChangeEvent(this);
						for (ChangeListener l : listenerList
								.getListeners(ChangeListener.class))
							l.stateChanged(ce);
					}
				}
			}
			public void mousePressed(java.awt.event.MouseEvent e) {
				if(tip.isEnabled()) {
					if(tip.timer.isRunning()) {
						tip.timer.stop();
						tip.getMouseMotionListeners()[0].mouseDragged(e);
					} else {
						switch(tip.inputMode) {
							case EulerSketchInputPanel.DRAW_INPUT_MODE:
								tip.curStroke = new Polygon();
								tip.curStroke.addPoint(Math.max(0, Math.round(e.getX() / tip.scale)), Math.max(0, Math.round(e.getY() / tip.scale)));
								tip.lightRepaint();
								break;
							case EulerSketchInputPanel.ERASE_INPUT_MODE:
								tip.eraserPos = new Point(Math.max(0, Math.round(e.getX() / tip.scale)), Math.max(0, Math.round(e.getY() / tip.scale)));
								tip.lightRepaint();
								break;
							case EulerSketchInputPanel.MOVE_INPUT_MODE:
								double ex = e.getX() / tip.scale;
								double ey = e.getY() / tip.scale;
								for(Entry<Character, IncidentPointsPolygon> ec : tip.eulerCode.getCurves().entrySet()) {
									if(ec.getValue().contains(ex, ey)) tip.movedObject = ec;
								}
								if(tip.movedObject != null) {
									tip.moverStart = new Point(Math.max(0, Math.round(e.getX() / tip.scale)), Math.max(0, Math.round(e.getY() / tip.scale)));
									Rectangle bound = tip.movedObject.getValue().getBounds();
									tip.moverMin = new Point(tip.moverStart.x - bound.x, tip.moverStart.y - bound.y);
									tip.moverCur = moverStart;
									tip.repaint();
								}
						}
					}
				}
			}
			public void mouseReleased(java.awt.event.MouseEvent e) {
				if(tip.isEnabled()) {
					final java.awt.event.MouseEvent ev = e;
					tip.timer = new Timer(tip.options.releasedPressedDelay, new java.awt.event.ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							switch(tip.inputMode) {
								case EulerSketchInputPanel.DRAW_INPUT_MODE:
									Polygon remInt = tip.options.selfIntersect ? GeomUtils.makeClockwise(GeomUtils.removeMinArea(tip.curStroke, tip.options.curveMinArea), tip.options.curveMinArea) : GeomUtils.removeIntersections(tip.curStroke, tip.options.convexCurves, tip.options.curveMinArea);
									tip.curStroke = null;
									if(remInt != null) {
										Character cl = tip.eulerCode.addCurve(remInt);
										if(options.delCurvWrIntGauss) {
											for(int i = 1; i <= 9; i++) {
												boolean err;
												try {
													String gc = tip.eulerCode.getGaussCode(false, false);
													err = gc.contains("#") || gc.contains("@");
													if(!err) 
														err = tip.eulerCode.getZones().isEmpty();
												} catch(Exception e) {
													err = true;
												}
												if(err) {
													tip.eulerCode.removeCurve(cl, options.redoOnRemove);
													if(i != 9) {
														remInt.translate((i + 1) % 3 == 0 ? -2 : 1, i % 3 == 0 ? i - 4 : 0);
														cl = tip.eulerCode.addCurve(remInt);
													}
												} else
													break;
											}
										}
										tip.repaint();
										tip.updatesPreferredSizeAndNotifiesChangeListeners();
									} else tip.lightRepaint();
									break;
								case EulerSketchInputPanel.ERASE_INPUT_MODE:
									tip.eraserPos = null;
									Character curve = null;
									double erx = ev.getX() / tip.scale;
									double ery = ev.getY() / tip.scale;
									for(Entry<Character, IncidentPointsPolygon> ec : tip.eulerCode.getCurves().entrySet()) {
										if(ec.getValue().contains(erx, ery)) curve = ec.getKey();
									}
									if(curve != null) {
										tip.eulerCode.removeCurve(curve, options.isRedoOnRemove());
										tip.repaint();
										tip.updatesPreferredSizeAndNotifiesChangeListeners();
									} else {
										tip.lightRepaint();
									}
									break;
								case EulerSketchInputPanel.MOVE_INPUT_MODE:
									if(tip.movedObject != null) {
										Character oldCurveLabel = tip.movedObject.getKey();
										Polygon oldCurve = tip.movedObject.getValue();
										tip.eulerCode.removeCurve(oldCurveLabel, options.redoOnRemove);
										Polygon newCurve = new Polygon(oldCurve.xpoints, oldCurve.ypoints, oldCurve.npoints);
										newCurve.translate(tip.moverCur.x - tip.moverStart.x, tip.moverCur.y - tip.moverStart.y);
										tip.eulerCode.addCurve(oldCurveLabel, newCurve);
										if(options.delCurvWrIntGauss) {
											boolean err;
											try {
												String gc = tip.eulerCode.getGaussCode(false, false);
												err = gc.contains("#") || gc.contains("@");
												if(!err) 
													err = tip.eulerCode.getZones().isEmpty();
											} catch(Exception e) {
												err = true;
											}
											if(err) {
												tip.eulerCode.removeCurve(oldCurveLabel, options.redoOnRemove);
												tip.eulerCode.addCurve(oldCurveLabel, oldCurve);
											}
										}
										tip.moverStart = null;
										tip.moverMin = null;
										tip.moverCur = null;
										tip.movedObject = null;
										tip.repaint();
										tip.updatesPreferredSizeAndNotifiesChangeListeners();
									}
							}
						}
					});
					tip.timer.setRepeats(false);
					tip.timer.start();
				}
			}
		});
		this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				if(tip.isEnabled()) {
					switch(tip.inputMode) {
						case EulerSketchInputPanel.DRAW_INPUT_MODE:
							int ex = Math.max(0, Math.round(e.getX() / tip.scale));
							int ey = Math.max(0, Math.round(e.getY() / tip.scale));
							if(tip.curStroke != null
									&& (tip.curStroke.npoints == 0
											|| tip.curStroke.xpoints[tip.curStroke.npoints - 1] != ex
											|| tip.curStroke.ypoints[tip.curStroke.npoints - 1] != ey))
								tip.curStroke.addPoint(ex, ey);
							break;
						case EulerSketchInputPanel.ERASE_INPUT_MODE:
							tip.eraserPos = new Point(Math.max(0, Math.round(e.getX() / tip.scale)), Math.max(0, Math.round(e.getY() / tip.scale)));
							break;
						case EulerSketchInputPanel.MOVE_INPUT_MODE:
							if(tip.movedObject != null) {
								tip.moverCur = new Point(Math.max(tip.moverMin.x, Math.round(e.getX() / tip.scale)), Math.max(tip.moverMin.y, Math.round(e.getY() / tip.scale)));
							}
					}
					tip.lightRepaint();
				}
			}
		});
	}
	
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}
	
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}
	
	public ChangeListener[] getChangeListeners() {
		return listenerList.getListeners(ChangeListener.class);
	}
	
	private void updatesPreferredSizeAndNotifiesChangeListeners() {
		if(options.autoPreferredSize) {
			Dimension d = new Dimension();
			for(IncidentPointsPolygon curve : eulerCode.getCurves().values()) {
				Rectangle bound = curve.getBounds();
				int maxX = bound.x + bound.width;
				if(d.width < maxX) d.width = maxX;
				int maxY = bound.y + bound.height;
				if(d.height < maxY) d.height = maxY;
			}
			d.width = Math.round(d.width * scale) + 3;
			d.height = Math.round(d.height * scale) + 3;
			Dimension ps = this.getPreferredSize();
			if(ps == null || ps.width != d.width || ps.height != d.height) {
				this.setPreferredSize(d);
				this.revalidate();
			}
		}
		ChangeEvent e = new ChangeEvent(this);
		for(ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
			l.stateChanged(e);
		}
	}
	
	private void lightRepaint() {
		super.repaint();
	}
	
	@Override
	public void repaint() {
		this.changedGraphics = true;
		super.repaint();
	}
	
	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);
		if(paintBuffer == null || paintBuffer.getWidth() != getWidth() || paintBuffer.getHeight() != getHeight()) {
			paintBuffer = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());
			changedGraphics = true;
		}
		if(changedGraphics) {
			Graphics2D bfg = (Graphics2D)paintBuffer.getGraphics();
			AffineTransform orAT = bfg.getTransform();
			bfg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, options.antialias);
			bfg.setBackground(options.backgroundColor);
			bfg.clearRect(0, 0, paintBuffer.getWidth(), paintBuffer.getHeight());
			bfg.scale(scale, scale);

			paintEC(bfg, eulerCode, options, movedObject == null ? null
					: movedObject.getKey());

			bfg.setTransform(orAT);
			bfg.dispose();
			changedGraphics = false;
		}
		Graphics2D g = (Graphics2D)graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, options.antialias);
		g.drawImage(paintBuffer, 0, 0, null);
		AffineTransform orAT = g.getTransform();
		g.scale(scale, scale);
		// direct draw on panel
		// draw the current stroke
		if(curStroke != null) {
			g.setColor(options.strokeColor);
			g.drawPolyline(curStroke.xpoints, curStroke.ypoints, curStroke.npoints);
		}
		// draw a gray overlay for the ready to delete object
		if(eraserPos != null) {
			IncidentPointsPolygon curve = null;
			for(IncidentPointsPolygon ec : eulerCode.getCurves().values()) {
				if(ec.contains(eraserPos)) curve = ec;
			}
			if(curve != null) {
				g.setColor(options.eraseColor);
				g.fillPolygon(curve.xpoints, curve.ypoints, curve.npoints);
			}
		}
		// draw the moved object
		if(movedObject != null) {
			Polygon movedCurve = movedObject.getValue();
			movedCurve = new Polygon(movedCurve.xpoints, movedCurve.ypoints, movedCurve.npoints);
			movedCurve.translate(moverCur.x - moverStart.x, moverCur.y - moverStart.y);
			g.setColor(options.alphaCurveColors[movedObject.getKey() % options.curveColors.length]);
			g.fillPolygon(movedCurve.xpoints, movedCurve.ypoints, movedCurve.npoints);
			Rectangle bound = movedCurve.getBounds();
			g.setColor(Color.BLACK);
			g.drawString(movedObject.getKey().toString(), (int)bound.getCenterX(), (int)bound.getCenterY());
		}
		g.setTransform(orAT);
	}

	public static void paintEC(Graphics2D bfg, EulerCode eulerCode,
			Options options, Character skipCurve) {
		if(!options.showOnlyOutlines && options.showZones) {
			List<Zone> zones;
			try {
				zones = eulerCode.getZones();
			} catch(RuntimeException re) {
				zones = new ArrayList<Zone>();
				System.err.println(re.getMessage());
			}
			for(Zone zone : zones) {
				if(!zone.label.isEmpty() && (skipCurve == null || !zone.label.equals(skipCurve.toString()))) {
					int r = 0, g = 0, b = 0, a = 0;
					int div = 0;
					for(int i = 0, end = zone.label.length(); i < end; i++) {
						char c = zone.label.charAt(i);
						if(skipCurve == null || c != skipCurve) {
							Color color = options.alphaCurveColors[c % options.alphaCurveColors.length];
							r += color.getRed();
							g += color.getGreen();
							b += color.getBlue();
							a += color.getAlpha();
							div++;
						}
					}
					bfg.setColor(new Color(r / div, g / div, b / div, a / div));
					bfg.fill(zone.getArea());
				}
			}
		}
		
		// draw the curves
		for(Entry<Character, IncidentPointsPolygon> ec : eulerCode.getCurves().entrySet()) {
			Character label = ec.getKey();
			IncidentPointsPolygon curve = ec.getValue();
			if(skipCurve == null || !label.equals(skipCurve)) {
				int cIndex = label % options.curveColors.length;
				if(!options.showOnlyOutlines && !options.showZones) {
					bfg.setColor(options.alphaCurveColors[cIndex]);
					bfg.fillPolygon(curve.xpoints, curve.ypoints, curve.npoints);
				}
				bfg.setColor(options.curveColors[cIndex]);
				ArrayList<IncidentPointRef> iprs = new ArrayList<IncidentPointRef>();
				for(Entry<IncidentPointRef, Integer> e : curve
						.getOrderedIncidentPointRefs()) {
					if(e.getKey().isUnder())
						iprs.add(e.getKey());
				}
				if (options.underGapLen <= 0 || iprs.isEmpty()) {
					bfg.drawPolygon(curve);
				} else {
					Polygon tmp = new Polygon();
					drawPolyline: for (int i = 0, n = iprs.size(); i < n; i++, tmp.npoints = 0, tmp
							.invalidate()) {
						IncidentPointRef p0 = iprs.get(i);
						IncidentPointRef p1 = iprs.get((i + 1) % n);
						int p0r = p0.getcRef(), p1r = p1.getcRef();
						// poly current segment points
						for (int j = p0r, ns = (p0r == p1r ? (p0.getDis() >= p1
								.getDis() ? curve.npoints : 0)
								: (p0r < p1r ? p1r - p0r
										: (curve.npoints - p0r) + p1r))
								+ p0r + 1; j <= ns; j++)
							tmp.addPoint(curve.xpoints[j % curve.npoints],
									curve.ypoints[j % curve.npoints]);

						// cut polyline end
						for (double toCut = (Point2D.distance(
								tmp.xpoints[tmp.npoints - 1],
								tmp.ypoints[tmp.npoints - 1],
								tmp.xpoints[tmp.npoints - 2],
								tmp.ypoints[tmp.npoints - 2]) - p1.getDis())
								+ options.underGapLen;;) {
							int i0 = tmp.npoints - 1, i1 = tmp.npoints - 2;
							int x0 = tmp.xpoints[i0], y0 = tmp.ypoints[i0];
							int dx = tmp.xpoints[i1] - x0;
							int dy = tmp.ypoints[i1] - y0;
							double dp0p1 = Math.sqrt(dx * dx + dy * dy);
							if (toCut >= dp0p1) {
								if (tmp.npoints <= 2)
									continue drawPolyline;
								tmp.npoints--;
								toCut -= dp0p1;
							} else {
								double d = toCut / dp0p1;
								tmp.xpoints[i0] = x0
										+ (int) Math.round(d * dx);
								tmp.ypoints[i0] = y0
										+ (int) Math.round(d * dy);
								break;
							}
						}

						// cut polyline start
						int j = 0;
						for (double toCut = p0.getDis()
								+ options.underGapLen;;) {
							int x0 = tmp.xpoints[j], y0 = tmp.ypoints[j];
							int dx = tmp.xpoints[j + 1] - x0;
							int dy = tmp.ypoints[j + 1] - y0;
							double dp0p1 = Math.sqrt(dx * dx + dy * dy);
							if (toCut >= dp0p1) {
								if (j + 2 >= curve.npoints)
									continue drawPolyline;
								j++;
								toCut -= dp0p1;
							} else {
								double d = toCut / dp0p1;
								tmp.xpoints[j] = x0
										+ (int) Math.round(d * dx);
								tmp.ypoints[j] = y0
										+ (int) Math.round(d * dy);
								break;
							}
						}
						for (int k = 0, x = tmp.xpoints[j], y = tmp.ypoints[j]; k < j; k++) {
							tmp.xpoints[k] = x;
							tmp.ypoints[k] = y;
						}
						bfg.drawPolyline(tmp.xpoints, tmp.ypoints,
								tmp.npoints);
					}
				}
			}
		}
		FontMetrics fMetrics = bfg.getFontMetrics();
		int hFM = fMetrics.getHeight();
		// draw curve and segment labels 
		for(Entry<Character, IncidentPointsPolygon> ec : eulerCode.getCurves().entrySet()) {
			Character label = ec.getKey();
			if(skipCurve == null || !label.equals(skipCurve)) {
				IncidentPointsPolygon curve = ec.getValue();
				Rectangle bound = curve.getBounds();
				// draw current curve label
				if (options.showCurveLabels) {
					bfg.setColor(options.curveLabelColor == null ? options.curveColors[label
							% options.curveColors.length]
							: options.curveLabelColor);
					bfg.drawString(label.toString(), (int) Math.round(bound
							.getCenterX()
							- (fMetrics.stringWidth(label.toString()) * 0.5)),
							(int) Math.round(bound.getCenterY() + (hFM * 0.5)
									- 2));
				}
				bfg.setColor(options.segmentLabelColor == null ? options.curveColors[label
						% options.curveColors.length]
						: options.segmentLabelColor);
				// draw current curve segments label
				if(options.segmentShowType != Options.SHOW_NONE) {
					Set<Entry<IncidentPointRef, Integer>> curveIPR = curve.getOrderedIncidentPointRefs();
					if(!curveIPR.isEmpty()) {
						Iterator<Entry<IncidentPointRef, Integer>> it = curveIPR.iterator();
						Entry<IncidentPointRef, Integer> first = it.next();
						Entry<IncidentPointRef, Integer> pre = first;
						while(first != null) {
							Entry<IncidentPointRef, Integer> cur;
							if(it.hasNext()) cur = it.next();
							else { cur = first; first = null; }
							Point2D.Double p = pre.getKey().getSuggestedLabelPoint();
							int x = (int) Math.round(p.x);
							int y = (int) Math.round(p.y);
							if(options.segmentShowType != Options.SHOW_NUMBER)
								bfg.fillOval(x - 2, y - 2, 5, 5);
							if(options.segmentShowType == Options.SHOW_NUMBER
									|| options.segmentShowType == Options.SHOW_POINT_WITH_NUMBER) {
								String str = pre.getValue() + "_" + cur.getValue();
								bfg.drawString(str, Math.max(0, x - (fMetrics.stringWidth(str) / 2)), y + hFM);
							}
							else if(options.segmentShowType == Options.SHOW_POINT_WITH_LABEL) {
								String str = label.toString();
								for(Character inc : pre.getKey().getFolContCurves()) str += inc;
								int psx = Math.max(0, x - (fMetrics.stringWidth(str) / 2));
								bfg.drawString(str, psx, y + hFM);
								psx += (fMetrics.stringWidth(label.toString())) / 2; 
								bfg.drawLine(psx - 2, y + 4, psx + 1, y + 4);
							}
							pre = cur;
						}
					}
				}
			}
		}
		// draws the intersection points
		if(options.pointShowType != Options.SHOW_NONE) {
			bfg.setColor(options.pointColor);
			// draws the other points
			int maxLbl = 0;
			for(Entry<Integer, IncidentPoint> ep : eulerCode.getIncidentPoints().entrySet()) {
				int lbl = ep.getKey();
				if(lbl < 0) continue;
				if(maxLbl < lbl) maxLbl = lbl;
				IncidentPoint p = ep.getValue();
				int x = (int) Math.round(p.x);
				int y = (int) Math.round(p.y);
				if(skipCurve == null || !p.getIncidentCurves().contains(skipCurve)) {
					String label = "" + lbl;
					if(options.pointShowType != Options.SHOW_NUMBER)
						bfg.fillOval(x - 2, y - 2, 5, 5);
					if(options.pointShowType != Options.SHOW_POINT) {
						if(options.pointShowType == Options.SHOW_POINT_WITH_LABEL) for(Character l : p.getIncidentCurves()) label += l;
						bfg.drawString(label, Math.max(0, x - (fMetrics.stringWidth(label) / 2)), y + hFM);
					}
				}
			}
			// draws the single points
			for(Entry<Character, IncidentPointsPolygon> ec : eulerCode.getCurves().entrySet()) {
				Character cLbl = ec.getKey();
				IncidentPointsPolygon ipp = ec.getValue();
				if(ipp.getIncidentPointRefsMap().isEmpty()) {
					maxLbl++;
					if(skipCurve == null || !skipCurve.equals(cLbl)) {
						String label = "" + maxLbl;
						if(options.pointShowType != Options.SHOW_NUMBER)
							bfg.fillOval(ipp.xpoints[0] - 2, ipp.ypoints[0] - 2, 5, 5);
						if(options.pointShowType != Options.SHOW_POINT) {
							if(options.pointShowType == Options.SHOW_POINT_WITH_LABEL) for(Character l : ipp.getFirstPointContCurves()) label += l;
							bfg.drawString(label, Math.max(0, ipp.xpoints[0] - (fMetrics.stringWidth(label) / 2)), ipp.ypoints[0] + hFM);
						}
					}
				}
			}
		}
	}

	public int getInputMode() {
		return inputMode;
	}

	public void setInputMode(int inputMode) {
		if(inputMode != DRAW_INPUT_MODE && inputMode != ERASE_INPUT_MODE
				&& inputMode != MOVE_INPUT_MODE)
			throw new IllegalArgumentException("Invalid inputMode.");
		this.inputMode = inputMode;
		this.setCursor(cursors[inputMode]);
		this.curStroke = null;
		this.eraserPos = null;
		this.moverStart = null;
		this.moverMin = null;
		this.movedObject = null;
	}
	
	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		if(scale <= 0) throw new IllegalArgumentException("Invalid scale.");
		this.scale = scale;
		this.repaint();
		this.updatesPreferredSizeAndNotifiesChangeListeners();
	}

	public EulerCode getEulerCode() {
		return eulerCode;
	}

	public void setEulerCode(EulerCode eulerCode) {
		if(eulerCode == null) throw new NullPointerException("Null eulerCode.");
		this.eulerCode = eulerCode;
		this.repaint();
		this.updatesPreferredSizeAndNotifiesChangeListeners();
	}

	public Options getOptions() {
		return new Options(options);
	}

	public void setOptions(Options options) {
		if(options == null) throw new NullPointerException("Null options.");
		boolean notPrev = !this.options.isAutoPreferredSize();
		this.options.setOptions(options);
		this.repaint();
		if(this.options.autoPreferredSize && notPrev)
			this.updatesPreferredSizeAndNotifiesChangeListeners();
	}

	public void clear() {
		this.curStroke = null;
		this.eulerCode.clear();
		this.repaint();
		this.updatesPreferredSizeAndNotifiesChangeListeners();
	}

	private static Cursor circleCursor(int width) {
		Toolkit t = Toolkit.getDefaultToolkit();
		Dimension bestSize = t.getBestCursorSize(width, width);
		BufferedImage cusrImage = new BufferedImage(bestSize.width, bestSize.height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = cusrImage.getGraphics();
		int l = Math.min(width, Math.min(bestSize.width, bestSize.height));
		int lm = Math.max(0, l-1);
		g.fillOval((bestSize.width - l) / 2, (bestSize.height - l) / 2, lm, lm);
		g.setColor(Color.BLACK);
		g.drawOval((bestSize.width - l) / 2, (bestSize.height - l) / 2, lm, lm);
		return t.createCustomCursor(cusrImage, new Point(bestSize.width / 2, bestSize.height / 2), "square" + l);
	}
}
