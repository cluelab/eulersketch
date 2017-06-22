/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.convhull.JarvisMarch2D;

/**
 * @author Mattia De Rosa
 */
public class GeomUtils {
	/**
	 * This class is not instantiable.
	 */
	private GeomUtils() {
	}
	
	/**
	 * Compares two Point with a tolerance (over the two axis).
	 * @param p1 the first point
	 * @param p2 the second point
	 * @param tolerance the tolerance of the equality
	 * @return true if the two points are equals, minus the give tolerance
	 */
	public static boolean equalsTolerance(Point2D.Double p1, Point2D.Double p2, double tolerance) {
		if(p1 == p2) return true;
		else if(p1 == null || p2 == null) return false;
		else return Math.abs(p1.x - p2.x) <= tolerance && Math.abs(p1.y - p2.y) <= tolerance;
	}
	
    /**
     * Returns the intersection point of two segment or null if the intersection doesn't exist.
     * @param x1 start x of the first segment
     * @param y1 start y of the first segment
     * @param x2 end x of the first segment
     * @param y2 end y of the first segment
     * @param x3 start x of the second segment
     * @param y3 start y of the second segment
     * @param x4 end x of the second segment
     * @param y4 end y of the second segment
     * @return the intersection point of the two segment
     */
	public static Point2D.Double getIntersection(double x1, double y1,
			double x2, double y2, double x3, double y3, double x4, double y4) {
		final double dx1 = x1 - x2;
		final double dy1 = y1 - y2;
		final double dx2 = x3 - x4;
		final double dy2 = y3 - y4;
		final double denom = dx1 * dy2 - dy1 * dx2;
		if (denom == 0.)
			return null;

		final double ddx = (x2 - x4);
		final double ddy = (y2 - y4);
		double ua = (dx2 * ddy - dy2 * ddx) / denom;
		double ub = (dx1 * ddy - dy1 * ddx) / denom;
		if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f)
			return new Point2D.Double((x2 + ua * dx1), (y2 + ua * dy1));
		else
			return null;
	}
    
	/**
	 * Returns the point over the segment at the requested distance from the start point.
	 * If the requested distance in greater than the length of segment, the method return the end point of the segment. 
	 * @param x0 start x of the segment
	 * @param y0 start y of the segment
	 * @param x1 end x of the segment
	 * @param y1 end x of the segment
	 * @param dist requested distance from [x0,y0]
	 * @return the point over the segment
	 */
	public static Point2D.Double pointOverSegment(int x0, int y0, int x1, int y1, double dist) {
		final int dx = x1 - x0;
		final int dy = y1 - y0;
		final double d = dist / Math.sqrt(dx*dx + dy*dy);
		if(d >= 1.) return new Point2D.Double(x1, y1);
		else return new Point2D.Double(x0 + d*dx, y0 + d*dy);
	}

	/**
	 * Returns the mean point over two point on a polygon.
	 * @param poly the polygon
	 * @param startIdx the index of the point before the start point
	 * @param startDis the distance between the startIdx point and the start point 
	 * @param endIdx the index of the point before the end point
	 * @param endDis the distance between the endIdx point and the end point 
	 * @return the mean point
	 */
	public static Point2D.Double meanPoint(Polygon poly, int startIdx, double startDis, int endIdx, double endDis) {
		// if the start and last point are over the same segment and start is before last
		if(startIdx == endIdx && startDis <= endDis) return pointOverSegment(
				poly.xpoints[startIdx], poly.ypoints[startIdx],
				poly.xpoints[(startIdx + 1) % poly.npoints], poly.ypoints[(startIdx + 1) % poly.npoints],
				Math.min(startDis, endDis) + (0.5 * Math.abs(endDis - startDis)));
		
		// calculate the distance between start and last point
		double len = endDis - startDis;
		int i = startIdx;
		do{
			int pi = i;
			i = (i + 1) % poly.npoints;
			int dx = poly.xpoints[i] - poly.xpoints[pi];
			int dy = poly.ypoints[i] - poly.ypoints[pi];
			len += Math.sqrt(dx*dx + dy*dy);
		}while(i != endIdx);
		
		// the distance of the mean point
		final double mean = startDis + (0.5 * len);
		
		// find the mean point
		double parLen = 0.;
		i = startIdx;
		do{
			int pi = i;
			i = (i + 1) % poly.npoints;
			int dx = poly.xpoints[i] - poly.xpoints[pi];
			int dy = poly.ypoints[i] - poly.ypoints[pi];
			double segLen = Math.sqrt(dx*dx + dy*dy);
			double newLen = parLen + segLen;
			if(newLen < mean) parLen = newLen;
			else {
				double d = (mean - parLen) / segLen;
				return new Point2D.Double(poly.xpoints[pi] + d*dx, poly.ypoints[pi] + d*dy);
			}
		}while(i != endIdx);
		// if the mean point is after the last point
		final int afterLast = (endIdx + 1) % poly.npoints;
		return pointOverSegment(poly.xpoints[endIdx], poly.ypoints[endIdx], poly.xpoints[afterLast], poly.ypoints[afterLast], mean - parLen);
	}
	
	/**
	 * Returns a polygon (clockwise) without self intersections that is similar to the input polygon.
	 * Returns null if it is impossible to generate a polygon with the minimum specified area.
	 * It make use of splitContinuousCurve (or JarvisMarch2D.convexHull if the previous fails).
	 * @param poly input polygon
	 * @param alwaysConvex specify if the method must return a convex polygon
	 * @param minArea minimum area for the output polygon
	 * @return a intersection-free polygon
	 */
	public static Polygon removeIntersections(Polygon poly, boolean alwaysConvex, double minArea) {
		if(poly == null) return null;
		ArrayList<Coordinate> points = new ArrayList<Coordinate>();
		for (int i = 0; i < poly.npoints; i++)
			points.add(new Coordinate(poly.xpoints[i], poly.ypoints[i]));

		LinkedHashSet<Coordinate> generated = new LinkedHashSet<Coordinate>();
		try {
			Collection<com.vividsolutions.jts.geom.Polygon> polys = splitPolygon(points);
			double maxArea = -1;
			Coordinate[] bigger = null;
			for (com.vividsolutions.jts.geom.Polygon pl : polys) {
				double cArea = pl.getArea();
				if (cArea > maxArea) {
					bigger = pl.getCoordinates();
					maxArea = cArea;
				}
			}
			if (maxArea >= minArea)
				for (Coordinate c : bigger)
					generated.add(c);
		} catch (Exception e) {
			generated.clear();
		}
		
		Polygon result;
		if(generated.size() <= 2) {
			result = convexHull(poly, minArea);
		} else {
			Polygon out = new Polygon();
			for (Coordinate c : generated)
				out.addPoint((int) Math.round(c.x), (int) Math.round(c.y));
			result = alwaysConvex ? convexHull(out, minArea) : out;
		}
		if(result == null) return null;
		
		int tot = 0;
		for(int i = 0; i < result.npoints; i++) {
			int ip = (i + 1) % result.npoints;
            tot += (result.xpoints[i] * result.ypoints[ip]) - (result.ypoints[i] * result.xpoints[ip]);
        }
		if(tot < 0) { // counterclockwise
			Polygon reverse = new Polygon();
			for(int i = result.npoints - 1; i >= 0; i--) reverse.addPoint(result.xpoints[i], result.ypoints[i]);
			return reverse;
		} else {
			return result;
		}
	}

	/**
	 * Returns a self intersecting polygon without composing continuous curves
	 * smaller than minArea
	 * 
	 * @param poly
	 *            input polygon
	 * @param minArea
	 *            minimum area of each
	 * @return the resulting polygon
	 */
	public static Polygon removeMinArea(Polygon poly, double minArea) {
		if (poly == null)
			return null;
		LinkedHashSet<Coordinate> gen = new LinkedHashSet<Coordinate>();
		for (int i = 0; i < poly.npoints; i++)
			gen.add(new Coordinate(poly.xpoints[i], poly.ypoints[i]));
		Collection<com.vividsolutions.jts.geom.Polygon> polygons;
		try {
			polygons = splitPolygon(gen);
		} catch (Exception e) {
			return null;
		}
		for (com.vividsolutions.jts.geom.Polygon pl : polygons) {
			double area = 0;
			try {
				area = pl.getArea();
			} catch (Exception e) {
			}
			if (area < minArea) {
				for (Coordinate c : pl.getCoordinates())
					gen.remove(c);
			}
		}
		if (gen.size() <= 2)
			return null;
		Polygon res = new Polygon();
		for (Coordinate c : gen)
			res.addPoint((int) c.x, (int) c.y);
		return res;
	}

	public static class PolygonR extends Polygon {
		private static final long serialVersionUID = -1593155143200494137L;
		private boolean reversed;

		public PolygonR() {
			super();
		}

		public PolygonR(int[] xpoints, int[] ypoints, int npoints) {
			super(xpoints, ypoints, npoints);
		}

		public boolean isReversed() {
			return reversed;
		}

		public void setReversed(boolean reversed) {
			this.reversed = reversed;
		}
	}

	/**
	 * Returns a clockwise polygon with the minimum specified area or null if
	 * input polygon is too small.
	 * @param minArea minimum area for the output polygon
	 * @return a clockwise polygon
	 */
	public static PolygonR makeClockwise(Polygon poly, double minArea) {
		if (poly == null)
			return null;
		ArrayList<math.geom2d.Point2D> points = new ArrayList<math.geom2d.Point2D>();
		for (int i = 0; i < poly.npoints; i++)
			points.add(new math.geom2d.Point2D(poly.xpoints[i], poly.ypoints[i]));
		SimplePolygon2D poly2D = new SimplePolygon2D(points);
		if (Math.abs(poly2D.area()) < minArea)
			return null;

		int tot = 0;
		for (int i = 0; i < poly.npoints; i++) {
			int ip = (i + 1) % poly.npoints;
			tot += (poly.xpoints[i] * poly.ypoints[ip])
					- (poly.ypoints[i] * poly.xpoints[ip]);
		}
		if (tot < 0) { // counterclockwise
			PolygonR reverse = new PolygonR();
			for (int i = poly.npoints - 1; i >= 0; i--)
				reverse.addPoint(poly.xpoints[i], poly.ypoints[i]);
			reverse.setReversed(true);
			return reverse;
		} else {
			return new PolygonR(poly.xpoints, poly.ypoints, poly.npoints);
		}
	}
	
	/**
	 * Returns a polygon that represent the convex hull of the input polygon.
	 * Returns null if the area of the convex hull is less that the minimum specified area.
	 * @param poly input polygon
	 * @param minArea minimum area for the output polygon
	 * @return the convex hull polygon
	 */
	public static Polygon convexHull(Polygon poly, double minArea) {
		ArrayList<math.geom2d.Point2D> points = new ArrayList<math.geom2d.Point2D>(poly.npoints);
		for(int i = 0; i < poly.npoints; i++) points.add(new math.geom2d.Point2D(poly.xpoints[i], poly.ypoints[i]));
		Polygon2D convexHull;
		try {
			convexHull = (new JarvisMarch2D()).convexHull(points);
		} catch (NullPointerException e) {
			return null;
		}
		LinkedHashSet<math.geom2d.Point2D> intConvexHull = new LinkedHashSet<math.geom2d.Point2D>(convexHull.vertexNumber());
		for(math.geom2d.Point2D p : convexHull.vertices()) {
			intConvexHull.add(new math.geom2d.Point2D((int) Math.rint(p.x()),
					(int) Math.rint(p.y())));
		}
		if(intConvexHull.size() <= 2) return null;
		if((new SimplePolygon2D(intConvexHull)).area() < minArea) return null;
		int xs[] = new int[intConvexHull.size()];
		int ys[] = new int[xs.length];
		int i = 0;
		for(math.geom2d.Point2D p : intConvexHull) {
			xs[i] = (int)p.x();
			ys[i] = (int)p.y();
			i++;
		}
		return new Polygon(xs, ys, xs.length);
	}

	/**
	 * Translate the input curves so that the origin of their boundig box is
	 * positioned in minX, minY
	 * 
	 * @param curves
	 *            input curves
	 * @param pts
	 *            points to translate accordingly to the resize
	 * @param minX
	 * @param minY
	 */
	public static void traslateTo(Polygon[] curves, Point2D[] pts, int minX,
			int minY) {
		if (curves.length > 0) {
			Rectangle bound = curves[0].getBounds();
			for (int i = 1; i < curves.length; i++)
				bound.add(curves[i].getBounds());

			minX -= bound.x;
			minY -= bound.y;

			for (int i = 0; i < curves.length; i++)
				curves[i].translate(minX, minY);

			for (int i = 0; i < pts.length; i++) {
				Point2D p = pts[i];
				p.setLocation(p.getX() + minX, p.getY() + minY);
			}
		}
	}

	/**
	 * Returns the centroid of an input polygon
	 * 
	 * @param polygon
	 *            input polygon
	 * @return the polygon centroid
	 */
	public static Point2D.Double centroid(Polygon polygon) {
		SimplePolygon2D sp = new SimplePolygon2D();
		for (int i = 0; i < polygon.npoints; i++) {
			sp.addVertex(new math.geom2d.Point2D(polygon.xpoints[i],
					polygon.ypoints[i]));
		}
		math.geom2d.Point2D c = sp.centroid();
		return new Point2D.Double(c.x(), c.y());
	}

	/**
	 * Split a polygon which self-intersects into a set of polygons which do not
	 * self-intersect. Inspired by http://stackoverflow.com/q/31473553
	 * 
	 * @param coords
	 *            coordinates of the polygon
	 * @return a set of non-self-intersecting polygons
	 */
	private static Collection<com.vividsolutions.jts.geom.Polygon> splitPolygon(
			Collection<Coordinate> coords) {
		Coordinate[] cs = coords.toArray(new Coordinate[coords.size() + 1]);
		cs[coords.size()] = cs[0];
		com.vividsolutions.jts.geom.Polygon polygon = (new GeometryFactory())
				.createPolygon(cs);
		Polygonizer polygonizer = new Polygonizer();
		for (int i = polygon.getNumInteriorRing(), n = i; i >= 0; i--) {
			LineString lineString = i == n ? polygon.getExteriorRing()
					: polygon.getInteriorRingN(i);
			if (lineString instanceof LinearRing) {
				// LinearRings are treated differently to line strings : we need
				// a LineString NOT a LinearRing
				lineString = lineString.getFactory().createLineString(
						lineString.getCoordinateSequence());
			}

			// unioning the linestring with the point makes any self
			// intersections explicit.
			Geometry toAdd = lineString.union(lineString.getFactory()
					.createPoint(lineString.getCoordinateN(0)));

			// Add result to polygonizer
			polygonizer.add(toAdd);
		}
		@SuppressWarnings("unchecked")
		Collection<com.vividsolutions.jts.geom.Polygon> res = polygonizer
				.getPolygons();
		return res;
	}
}
