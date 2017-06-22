/*******************************************************************************
 * Copyright (c) 2015 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import it.unisa.di.cluelab.euler.code.EulerCode.AreaWL;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPoint;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointRef;
import it.unisa.di.cluelab.euler.code.EulerCode.IncidentPointsPolygon;
import it.unisa.di.cluelab.euler.code.EulerCode.Zone;
import ocotillo.geometry.Coordinates;
import ocotillo.geometry.GeomXD;
import ocotillo.graph.Edge;
import ocotillo.graph.EdgeAttribute;
import ocotillo.graph.Graph;
import ocotillo.graph.GraphAttribute;
import ocotillo.graph.Node;
import ocotillo.graph.NodeAttribute;
import ocotillo.graph.StdAttribute;
import ocotillo.graph.StdAttribute.ControlPoints;
import ocotillo.graph.layout.fdl.impred.Impred;
import ocotillo.graph.layout.fdl.impred.ImpredConstraint;
import ocotillo.graph.layout.fdl.impred.ImpredForce;
import ocotillo.graph.layout.fdl.impred.ImpredPostProcessing;

/**
 * @author Mattia De Rosa
 */
public class EulerSmooth {
	private static final String CURVES_ATTR = "curves";
	private static final String UCURVES_ATTR = "ucurves";

	public static Graph toGraph(EulerCode ec, boolean nodeForIncidentPoints,
			double scale) {
		Graph graph = new Graph();
		NodeAttribute<Coordinates> positions = graph
				.nodeAttribute(StdAttribute.nodePosition);
		EdgeAttribute<ControlPoints> edgePoints = graph
				.edgeAttribute(StdAttribute.edgePoints);

		if (nodeForIncidentPoints) {
			Set<String> empty = new LinkedHashSet<String>();
			NodeAttribute<Set<String>> ucurves = graph.newNodeAttribute(
					UCURVES_ATTR, empty);
			HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();
			for (Entry<Integer, IncidentPoint> e : ec.getIncidentPoints()
					.entrySet()) {
				Integer lbl = e.getKey();
				if (lbl >= 0) {
					Node n = graph.newNode(lbl.toString());
					nodes.put(lbl, n);
					IncidentPoint ip = e.getValue();
					positions.set(n,
							new Coordinates(scale * ip.x, scale * ip.y));
					ucurves.set(n, new LinkedHashSet<String>());
				}
			}

			ArrayList<List<Edge>> curves = new ArrayList<List<Edge>>(ec
					.getCurves().size());
			for (Entry<Character, IncidentPointsPolygon> e : ec.getCurves()
					.entrySet()) {
				String crvLbl = e.getKey().toString();
				IncidentPointsPolygon crv = e.getValue();
				Set<Entry<IncidentPointRef, Integer>> oipr = crv
						.getOrderedIncidentPointRefs();
				String stNdId = "s" + crvLbl;
				if (oipr.isEmpty()) {
					Node n = graph.newNode(stNdId);
					positions.set(n, new Coordinates(scale * crv.xpoints[0],
							scale * crv.ypoints[0]));
					Edge edg = graph.newEdge(crvLbl + "0", n, n);
					ControlPoints cps = new ControlPoints();
					for (int i = 1; i < crv.npoints; i++)
						cps.add(new Coordinates(scale * crv.xpoints[i], scale
								* crv.ypoints[i]));
					edgePoints.set(edg, cps);
					List<Edge> crvEdgs = Arrays.asList(edg);
					graph.newSubGraph(Arrays.asList(n), crvEdgs);
					curves.add(crvEdgs);
				} else {
					char nEdge = '0';
					Iterator<Entry<IncidentPointRef, Integer>> it = oipr
							.iterator();
					Entry<IncidentPointRef, Integer> curE = it.next();

					ArrayList<Node> crvNodes = new ArrayList<Node>(oipr.size());
					ArrayList<Edge> crvEdges = new ArrayList<Edge>(
							oipr.size() + 1);
					if (curE.getKey().getcRef() == 0) {
						Coordinates firstPoint = new Coordinates(scale
								* crv.xpoints[0], scale * crv.ypoints[0]);
						Node fInterNode = nodes.get(Math.abs(curE.getValue()));
						if (curE.getKey().isUnder())
							ucurves.get(fInterNode).add(
									curE.getValue() >= 0 ? crvLbl : "-"
											+ crvLbl);
						Coordinates fInterNodePoint = positions.get(fInterNode);
						if (GeomXD.almostEqual(firstPoint, fInterNodePoint)) {
							crvNodes.add(fInterNode);
						} else {
							Node startNode = graph.newNode(stNdId);
							crvNodes.add(startNode);
							positions.set(startNode, firstPoint);
							crvNodes.add(fInterNode);
							crvEdges.add(graph.newEdge(crvLbl + nEdge++,
									startNode, fInterNode));
						}
						curE = it.hasNext() ? it.next() : null;
					} else {
						Node startNode = graph.newNode(stNdId);
						crvNodes.add(startNode);
						positions.set(startNode, new Coordinates(scale
								* crv.xpoints[0], scale * crv.ypoints[0]));
					}

					ControlPoints curCps = new ControlPoints();
					for (int i = 1; i < crv.npoints; i++) {
						curCps.add(new Coordinates(scale * crv.xpoints[i],
								scale * crv.ypoints[i]));
						while (curE != null && curE.getKey().getcRef() == i) {
							Node preNode = crvNodes.get(crvNodes.size() - 1);
							Node newNode = nodes.get(Math.abs(curE.getValue()));
							if (curE.getKey().isUnder())
								ucurves.get(newNode).add(
										curE.getValue() >= 0 ? crvLbl : "-"
												+ crvLbl);
							crvNodes.add(newNode);
							Edge edg = graph.newEdge(crvLbl + nEdge++, preNode,
									newNode);
							crvEdges.add(edg);
							if (!curCps.isEmpty()
									&& GeomXD.almostEqual(
											positions.get(preNode),
											curCps.get(0)))
								curCps.remove(0);
							if (!curCps.isEmpty()
									&& GeomXD.almostEqual(
											positions.get(newNode),
											curCps.get(curCps.size() - 1)))
								curCps.remove(curCps.size() - 1);
							edgePoints.set(edg, curCps);
							curCps = new ControlPoints();
							curE = it.hasNext() ? it.next() : null;
						}
					}
					if (curE != null)
						throw new IllegalStateException();
					Node src = crvNodes.get(crvNodes.size() - 1);
					Node trg = crvNodes.get(0);
					Edge edg = graph.newEdge(crvLbl + nEdge++, src, trg);
					crvEdges.add(edg);
					if (!curCps.isEmpty()
							&& GeomXD.almostEqual(positions.get(src),
									curCps.get(0)))
						curCps.remove(0);
					if (!curCps.isEmpty()
							&& GeomXD.almostEqual(positions.get(trg),
									curCps.get(curCps.size() - 1)))
						curCps.remove(curCps.size() - 1);
					edgePoints.set(edg, curCps);

					graph.newSubGraph(new LinkedHashSet<Node>(crvNodes),
							crvEdges);
					curves.add(crvEdges);
				}
			}
			graph.newGraphAttribute(CURVES_ATTR, curves);
		} else {
			char en = '0';
			for (Entry<Character, IncidentPointsPolygon> e : ec.getCurves()
					.entrySet()) {
				IncidentPointsPolygon crv = e.getValue();
				Graph g = graph.newSubGraph();
				Node n = g.newNode(e.getKey().toString());
				positions.set(n, new Coordinates(scale * crv.xpoints[0], scale
						* crv.ypoints[0]));
				Edge edg = g.newEdge(String.valueOf(en++), n, n);
				ControlPoints cps = new ControlPoints();
				for (int i = 1; i < crv.npoints; i++)
					cps.add(new Coordinates(scale * crv.xpoints[i], scale
							* crv.ypoints[i]));
				edgePoints.set(edg, cps);
			}
		}
		for (Zone zone : ec.getZones()) {
			AreaWL area = zone.getArea();
			List<Polygon> outPolys, intPolys;
			if (zone.label.isEmpty()) {
				outPolys = area.getIntPolys();
				intPolys = area.getOutPolys();
			} else {
				outPolys = area.getOutPolys();
				intPolys = area.getIntPolys();
			}
			for (java.awt.Polygon p : outPolys) {
				Point2D.Double c = GeomUtils.centroid(p);
				for (int i = 0; !contains(c, p, intPolys); i++) {
					c.x = (p.xpoints[i] + p.xpoints[i + 2]) * 0.5;
					c.y = (p.ypoints[i] + p.ypoints[i + 2]) * 0.5;
				}
				Node n = graph.newNode();
				positions.set(n, new Coordinates(scale * c.x, scale * c.y));
			}
		}
		return graph;
	}

	public static Impred getImpred(Graph graph, double optimalDistance,
			boolean independentBoundaries, boolean movableElements,
			boolean separateBoundaries) {
		Collection<Edge> allEdges = new ArrayList<Edge>(graph.edges());

		List<List<Edge>> curveEdges;
		if (graph.hasGraphAttribute(CURVES_ATTR)) {
			GraphAttribute<List<List<Edge>>> attrCurves = graph
					.graphAttribute(CURVES_ATTR);
			curveEdges = attrCurves.get();
		} else {
			curveEdges = new ArrayList<List<Edge>>();
			for (Edge edge : allEdges)
				curveEdges.add(Arrays.asList(edge));
		}

		NodeAttribute<Collection<Edge>> surroundingEdges = new NodeAttribute<Collection<Edge>>(
				allEdges);
		if (independentBoundaries)
			surroundingEdges.setDefault(new ArrayList<Edge>());

		ArrayList<Node> zeroDegreeNodes = new ArrayList<Node>();
		for (Node node : graph.nodes()) {
			if (graph.degree(node) == 0) {
				surroundingEdges.set(node, allEdges);
				zeroDegreeNodes.add(node);
			}
		}

		Impred.ImpredBuilder builder = new Impred.ImpredBuilder(graph)
				.withForce(new ImpredForce.CurveSmoothing(curveEdges))
				.withForce(
						new ImpredForce.SelectedEdgeNodeRepulsion(
								optimalDistance, graph.edges(), zeroDegreeNodes))
				.withForce(
						new ImpredForce.EdgeAttraction(optimalDistance * 0.7))
				.withConstraint(
						new ImpredConstraint.DecreasingMaxMovement(
								optimalDistance))
				.withConstraint(
						new ImpredConstraint.MovementAcceleration(
								optimalDistance))
				.withConstraint(
						new ImpredConstraint.SurroundingEdges(surroundingEdges))
				.withPostProcessing(
						new ImpredPostProcessing.FlexibleEdges(graph.edges(),
								optimalDistance * 1.45, optimalDistance * 1.5));

		if (movableElements) {
			builder.withForce(new ImpredForce.SelectedNodeNodeRepulsion(
					optimalDistance, zeroDegreeNodes));
		} else {
			builder.withConstraint(new ImpredConstraint.PinnedNodes(
					zeroDegreeNodes));
		}

		if (separateBoundaries) {
			builder.withForce(new ImpredForce.EdgeNodeRepulsion(
					optimalDistance / 15));
		}

		return builder.build();
	}

	public static EulerCode toEulerCode(Graph graph, Rectangle2D desiredBound) {
		NodeAttribute<Coordinates> positions = graph
				.nodeAttribute(StdAttribute.nodePosition);
		EdgeAttribute<ControlPoints> edgePoints = graph
				.edgeAttribute(StdAttribute.edgePoints);

		Rectangle2D.Double bound = null;
		for (Edge edge : graph.edges()) {
			Coordinates fp = positions.get(edge.source());
			double x = fp.x(), y = fp.y();
			if (!Double.isNaN(x) && !Double.isNaN(y)) {
				if (bound == null)
					bound = new Rectangle2D.Double(x, y, 0, 0);
				else
					bound.add(x, y);
			}
			for (Coordinates p : edgePoints.get(edge)) {
				x = p.x();
				y = p.y();
				if (!Double.isNaN(x) && !Double.isNaN(y)) {
					if (bound == null)
						bound = new Rectangle2D.Double(x, y, 0, 0);
					else
						bound.add(x, y);
				}
			}
		}

		double ix = desiredBound.getX();
		double iy = desiredBound.getY();
		double sx = desiredBound.getWidth() / bound.width;
		double sy = desiredBound.getHeight() / bound.height;

		if (graph.hasGraphAttribute(CURVES_ATTR)) {
			NodeAttribute<Set<String>> ucurves = graph
					.nodeAttribute(UCURVES_ATTR);
			GraphAttribute<List<List<Edge>>> attrCurves = graph
					.graphAttribute(CURVES_ATTR);
			List<List<Edge>> crvsEdgs = attrCurves.get();

			char[] crvLbs = new char[crvsEdgs.size()];
			Polygon[] curves = new Polygon[crvLbs.length];
			int nc = 0;
			TreeMap<Integer, Entry<Point2D.Double, Set<String>>> nodePositions = new TreeMap<Integer, Entry<Point2D.Double, Set<String>>>();
			for (List<Edge> crvEgds : crvsEdgs) {
				Polygon curve = new Polygon();
				for (Edge edge : crvEgds) {
					Node node = edge.source();
					Integer nLbl;
					try {
						nLbl = Integer.valueOf(node.id());
					} catch (NumberFormatException nfe) {
						nLbl = Integer.MIN_VALUE;
					}
					Entry<Point2D.Double, Set<String>> np = nLbl >= 0 ? nodePositions
							.get(nLbl) : null;
					if (np == null) {
						Coordinates fp = positions.get(edge.source());
						np = new AbstractMap.SimpleEntry<Point2D.Double, Set<String>>(
								new Point2D.Double(
										(fp.x() - bound.x) * sx + ix,
										(fp.y() - bound.y) * sy + iy),
								ucurves.get(edge.source()));
						if (nLbl != Integer.MIN_VALUE)
							nodePositions.put(nLbl, np);
					}
					curve.addPoint((int) Math.round(np.getKey().x),
							(int) Math.round(np.getKey().y));
					for (Coordinates p : edgePoints.get(edge)) {
						curve.addPoint(
								(int) Math.round((p.x() - bound.x) * sx + ix),
								(int) Math.round((p.y() - bound.y) * sy + iy));
					}
				}
				crvLbs[nc] = crvEgds.get(0).id().charAt(0);
				curves[nc] = curve;
				nc++;
			}
			int[] incidentPointLabels = new int[nodePositions.size()];
			Point2D[] incidentPointPositions = new Point2D[incidentPointLabels.length];
			@SuppressWarnings("unchecked")
			Set<String>[] incidentPointUnderCurves = new Set[incidentPointLabels.length];
			int np = 0;
			for (Entry<Integer, Entry<Point2D.Double, Set<String>>> e : nodePositions
					.entrySet()) {
				incidentPointLabels[np] = e.getKey();
				incidentPointPositions[np] = e.getValue().getKey();
				incidentPointUnderCurves[np] = e.getValue().getValue();
				np++;
			}
			return new EulerCode(crvLbs, curves, incidentPointLabels,
					incidentPointPositions, incidentPointUnderCurves);
		} else {
			Collection<Edge> uEdges = graph.edges();
			Edge[] edges = uEdges.toArray(new Edge[uEdges.size()]);
			Arrays.sort(edges, new Comparator<Edge>() {
				@Override
				public int compare(Edge o1, Edge o2) {
					return o1.id().compareTo(o2.id());
				}
			});

			EulerCode ec = new EulerCode();
			for (Edge edge : edges) {
				Polygon curve = new Polygon();
				Node node = edge.source();
				Coordinates fp = positions.get(node);
				curve.addPoint((int) Math.round((fp.x() - bound.x) * sx + ix),
						(int) Math.round((fp.y() - bound.y) * sy + iy));
				for (Coordinates p : edgePoints.get(edge)) {
					curve.addPoint(
							(int) Math.round((p.x() - bound.x) * sx + ix),
							(int) Math.round((p.y() - bound.y) * sy + iy));
				}
				ec.addCurve(node.id().charAt(0), curve);
			}
			return ec;
		}
	}

	private static boolean contains(Point2D point, Polygon outPoly,
			Collection<Polygon> intPolys) {
		if (!outPoly.contains(point))
			return false;
		for (Polygon ip : intPolys) {
			if (ip.contains(point))
				return false;
		}
		return true;
	}
}
