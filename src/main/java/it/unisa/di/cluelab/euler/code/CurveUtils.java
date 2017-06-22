/*******************************************************************************
 * Copyright (c) 2012 Mattia De Rosa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package it.unisa.di.cluelab.euler.code;

import java.awt.Polygon;
import java.awt.geom.Point2D;

/**
 * @author Mattia De Rosa
 */
public class CurveUtils {
	/**
	 * This class is not instantiable.
	 */
	private CurveUtils() {
	}

	/**
	 * Create the polygon that approximates the circle that passes through two
	 * points.
	 * 
	 * @param x0
	 *            first point x
	 * @param y0
	 *            first point y
	 * @param x1
	 *            second point x
	 * @param y1
	 *            second point y
	 * @param nSegs
	 *            number of segments
	 * @param decRot
	 *            rotation direction
	 * @return the circle
	 */
	public static Polygon createCircle(int x0, int y0, int x1, int y1,
			int nSegs, boolean decRot) {
		double cx = (x0 + x1) * 0.5;
		double cy = (y0 + y1) * 0.5;
		double radius = Point2D.distance(cx, cy, x0, y0);
		double PI2 = Math.PI * 2.;
		double ang = PI2 / nSegs;
		if (decRot)
			ang = PI2 - ang;

		Polygon poly = new Polygon();
		double t = (Math.atan2(y0 - cy, x0 - cx) + PI2) % PI2;
		for (int i = 0; i < nSegs; i++, t = (t + ang) % PI2) {
			poly.addPoint((int) Math.round(cx + radius * Math.cos(t)),
					(int) Math.round(cy + radius * Math.sin(t)));
		}
		return poly;
	}

	/**
	 * Create the polygon that approximates the smallest circle that passes
	 * through three points.
	 * 
	 * @param x0
	 *            first point x
	 * @param y0
	 *            first point y
	 * @param x1
	 *            second point x
	 * @param y1
	 *            second point y
	 * @param x2
	 *            third point x
	 * @param y2
	 *            third point y
	 * @param nSegs
	 *            number of segments
	 * @return the circle, or null if the three points are collinear
	 */
	public static Polygon createCircle(double x0, double y0, double x1,
			double y1, double x2, double y2, int nSegs) {
		Point2D.Double cp = getCircleCenter(x0, y0, x1, y1, x2, y2);
		if (cp == null)
			return null;

		double radius = cp.distance(x0, y0);
		double PI2 = Math.PI * 2.;

		double ang = PI2 / nSegs;
		if (((x0 * y1) - (y0 * x1)) + ((x1 * y2) - (y1 * x2))
				+ ((x2 * y0) - (y2 * x0)) < 0)
			ang = PI2 - ang;

		Polygon poly = new Polygon();
		double t = (Math.atan2(y0 - cp.y, x0 - cp.x) + PI2) % PI2;
		for (int i = 0; i < nSegs; i++, t = (t + ang) % PI2) {
			poly.addPoint((int) Math.round(cp.x + radius * Math.cos(t)),
					(int) Math.round(cp.y + radius * Math.sin(t)));
		}
		return poly;
	}

	/**
	 * Return the center of the circle defined by three points
	 * 
	 * @param x0
	 *            first point x
	 * @param y0
	 *            first point y
	 * @param x1
	 *            second point x
	 * @param y1
	 *            second point y
	 * @param x2
	 *            third point x
	 * @param y2
	 *            third point y
	 * @return center of the circle, or null if the three points are collinear
	 */
	public static Point2D.Double getCircleCenter(double x0, double y0,
			double x1, double y1, double x2, double y2) {
		double x10 = x1 - x0;
		double y10 = y1 - y0;
		double x20 = x2 - x0;
		double y20 = y2 - y0;

		double v1 = x10 * (x0 + x1) + y10 * (y0 + y1);
		double v2 = x20 * (x0 + x2) + y20 * (y0 + y2);

		double d = 2 * (x10 * (y2 - y1) - y10 * (x2 - x1));
		if (d == 0.0)
			return null; // collinear

		return new Point2D.Double((y20 * v1 - y10 * v2) / d, (x10 * v2 - x20
				* v1)
				/ d);
	}

	/**
	 * Create a closed cubic spline that pass by the input control points.
	 * 
	 * http://www.cse.unsw.edu.au/~lambert/splines/NatCubic.java
	 * 
	 * @param xpoints
	 *            control points x coordinates
	 * @param ypoints
	 *            control points y coordinates
	 * @param npoints
	 *            number of points
	 * @param nSteps
	 * @return the cubic spline, or null if npoints < 3
	 */
	public static Polygon createCubicSpline(int[] xpoints, int[] ypoints,
			int npoints, int nSteps) {
		if (npoints < 3)
			return null;
		Cubic[] xc = calcNaturalCubic(npoints - 1, xpoints);
		Cubic[] yc = calcNaturalCubic(npoints - 1, ypoints);

		// very crude technique - just break each segment up into steps lines
		Polygon p = new Polygon();
		p.addPoint(Math.round(xc[0].eval(0)), Math.round(yc[0].eval(0)));
		for (int i = 0; i < xc.length; i++) {
			for (int j = 1; j <= nSteps; j++) {
				float u = j / (float) nSteps;
				int x = Math.round(xc[i].eval(u));
				int y = Math.round(yc[i].eval(u));
				if (p.xpoints[p.npoints - 1] != x
						|| p.ypoints[p.npoints - 1] != y)
					p.addPoint(x, y);
			}
		}
		return p;
	}

	/**
	 * calculates the closed natural cubic spline that interpolates x[0], x[1],
	 * ... x[n]. The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2
	 * + C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n]
	 * 
	 * http://www.cse.unsw.edu.au/~lambert/splines/NatCubicClosed.java
	 * 
	 * @param n
	 * @param x
	 * @return
	 */
	private static Cubic[] calcNaturalCubic(int n, int[] x) {
		float[] w = new float[n + 1];
		float[] v = new float[n + 1];
		float[] y = new float[n + 1];
		float[] D = new float[n + 1];
		float z, F, G, H;
		int k;
		/*
		 * We solve the equation
		 * [4 1      1] [D[0]]   [3(x[1] - x[n])  ]
		 * |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
		 * |  1 4 1   | | .  | = |      .         |
		 * |    ..... | | .  |   |      .         |
		 * |     1 4 1| | .  |   |3(x[n] - x[n-2])|
		 * [1      1 4] [D[n]]   [3(x[0] - x[n-1])]
		 * by decomposing the matrix into upper triangular and lower matrices
		 * and then back sustitution. See Spath "Spline Algorithms for Curves
		 * and Surfaces" pp 19--21. The D[i] are the derivatives at the knots.
		 */
		w[1] = v[1] = z = 1.0f / 4.0f;
		y[0] = z * 3 * (x[1] - x[n]);
		H = 4;
		F = 3 * (x[0] - x[n - 1]);
		G = 1;
		for (k = 1; k < n; k++) {
			v[k + 1] = z = 1 / (4 - v[k]);
			w[k + 1] = -z * w[k];
			y[k] = z * (3 * (x[k + 1] - x[k - 1]) - y[k - 1]);
			H = H - G * w[k];
			F = F - G * y[k - 1];
			G = -v[k] * G;
		}
		H = H - (G + 1) * (v[n] + w[n]);
		y[n] = F - (G + 1) * y[n - 1];

		D[n] = y[n] / H;
		D[n - 1] = y[n - 1] - (v[n] + w[n]) * D[n]; // This equation is WRONG!
													// in my copy of Spath
		for (k = n - 2; k >= 0; k--) {
			D[k] = y[k] - v[k + 1] * D[k + 1] - w[k + 1] * D[n];
		}

		/* now compute the coefficients of the cubics */
		Cubic[] C = new Cubic[n + 1];
		for (k = 0; k < n; k++) {
			C[k] = new Cubic((float) x[k], D[k], 3 * (x[k + 1] - x[k]) - 2
					* D[k] - D[k + 1], 2 * (x[k] - x[k + 1]) + D[k] + D[k + 1]);
		}
		C[n] = new Cubic((float) x[n], D[n], 3 * (x[0] - x[n]) - 2 * D[n]
				- D[0], 2 * (x[n] - x[0]) + D[n] + D[0]);
		return C;
	}

	private static class Cubic {
		float a, b, c, d; // a + b*u + c*u^2 + d*u^3

		Cubic(float a, float b, float c, float d) {
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
		}

		/* evaluate cubic */
		float eval(float u) {
			return (((d * u) + c) * u + b) * u + a;
		}
	}
}
