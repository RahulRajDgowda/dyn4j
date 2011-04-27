/*
 * Copyright (c) 2010, William Bittle
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted 
 * provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions 
 *     and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 *     and the following disclaimer in the documentation and/or other materials provided with the 
 *     distribution.
 *   * Neither the name of dyn4j nor the names of its contributors may be used to endorse or 
 *     promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dyn4j.game2d.collision.manifold;

import java.util.ArrayList;
import java.util.List;

import org.dyn4j.game2d.collision.narrowphase.NarrowphaseDetector;
import org.dyn4j.game2d.collision.narrowphase.Penetration;
import org.dyn4j.game2d.geometry.Convex;
import org.dyn4j.game2d.geometry.Edge;
import org.dyn4j.game2d.geometry.Feature;
import org.dyn4j.game2d.geometry.Shape;
import org.dyn4j.game2d.geometry.Transform;
import org.dyn4j.game2d.geometry.Vector2;
import org.dyn4j.game2d.geometry.Vertex;

/**
 * Represents an {@link ManifoldSolver} that uses a clipping method to determine
 * the contact manifold.
 * <p>
 * A {@link NarrowphaseDetector} should return a penetration normal and depth when two {@link Convex} {@link Shape}s are
 * intersecting.  The penetration normal should always point from the first {@link Shape} to the second.
 * @author William Bittle
 */
public class ClippingManifoldSolver implements ManifoldSolver {
	/* (non-Javadoc)
	 * @see org.dyn4j.game2d.collision.manifold.ManifoldSolver#getManifold(org.dyn4j.game2d.collision.narrowphase.Penetration, org.dyn4j.game2d.geometry.Convex, org.dyn4j.game2d.geometry.Transform, org.dyn4j.game2d.geometry.Convex, org.dyn4j.game2d.geometry.Transform, org.dyn4j.game2d.collision.manifold.Manifold)
	 */
	@Override
	public boolean getManifold(Penetration penetration, Convex convex1, Transform transform1, Convex convex2, Transform transform2, Manifold manifold) {
		// make sure the manifold passed in is cleared
		manifold.clear();
		
		// get the penetration normal
		Vector2 n = penetration.getNormal();
		
		// get the reference feature for the first convex shape
		Feature feature1 = convex1.getFarthestFeature(n, transform1);
		// check for vertex
		if (feature1.isVertex()) {
			// if the maximum
			Vertex vertex = (Vertex) feature1;
			ManifoldPoint mp = new ManifoldPoint(ManifoldPointId.DISTANCE, vertex.getPoint(), penetration.getDepth());
			manifold.points.add(mp);
			manifold.normal = n.negate();
			return true;
		}
		
		// get the reference feature for the second convex shape
		Feature feature2 = convex2.getFarthestFeature(n.getNegative(), transform2);
		// check for vertex
		if (feature2.isVertex()) {
			Vertex vertex = (Vertex) feature2;
			ManifoldPoint mp = new ManifoldPoint(ManifoldPointId.DISTANCE, vertex.getPoint(), penetration.getDepth());
			manifold.points.add(mp);
			manifold.normal = n.negate();
			return true;
		}
		
		// both features are edge features
		Edge reference = (Edge) feature1;
		Edge incident = (Edge) feature2;
		
		// choose the reference and incident edges
//		boolean flipped = false;
//		Edge reference, incident;
//		// which edge is more perpendicular?
//		Vector e1 = fe1.getEdge();
//		Vector e2 = fe2.getEdge();
//		if (Math.abs(e1.dot(n)) > Math.abs(e2.dot(n))) {
//			// shape2's edge is more perpendicular
//			reference = fe2;
//			incident = fe1;
//			flipped = true;
//		} else {
//			reference = fe1;
//			incident = fe2;
//		}
		
		// create the reference edge vector
		Vector2 refev = reference.getEdge();
		// normalize it
		refev.normalize();
		
		// compute the offestes of the reference edge points along the reference edge
		double offset1 = -refev.dot(reference.getVertex1().getPoint());
		double offset2 = refev.dot(reference.getVertex2().getPoint());
		
		// clip the incident edge by the reference edge's left edge
		List<Vertex> clip1 = this.clip(incident.getVertex1(), incident.getVertex2(), refev.getNegative(), offset1);
		// check the number of points
		if (clip1.size() < 2) {
			return false;
		}
		
		// clip the clip1 edge by the reference edge's right edge
		List<Vertex> clip2 = this.clip(clip1.get(0), clip1.get(1), refev, offset2);
		// check the number of points
		if (clip2.size() < 2) {
			return false;
		}
		
		// we need to change the normal to the reference edge's normal
		// since they may not have been the same
		Vector2 frontNormal = refev.cross(1.0);
		// also get the maximum point's depth
		double frontOffset = frontNormal.dot(reference.getMaximum().getPoint());
		
		// set the normal
//		m.normal = flipped ? frontNormal.getNegative() : frontNormal;
		manifold.normal = frontNormal;
		
		// test if the clip points are behind the reference edge
		for (int i = 0; i < clip2.size(); i++) {
			Vertex vertex = clip2.get(i);
			Vector2 point = vertex.getPoint();
			double depth = frontNormal.dot(point) - frontOffset;
			// make sure the point is behind the front normal
			if (depth >= 0.0) {
				// create an id for the manifold point
				IndexedManifoldPointId id = new IndexedManifoldPointId(reference.getIndex(), incident.getIndex(), vertex.getIndex(), false /* flipped */);
				// create the manifold point
				ManifoldPoint mp = new ManifoldPoint(id, point, depth);
				// add it to the list
				manifold.points.add(mp);
			}
		}
		// return the clipped points
		return true;
	}
	
	/**
	 * Clips the segment given by s1 and s2 by n.
	 * @param v1 the first vertex of the segment to be clipped
	 * @param v2 the second vertex of the segment to be clipped
	 * @param n the clipping plane/line
	 * @param offset the offset of the end point of the segment to be clipped
	 * @return List&lt;{@link Vector2}&gt; the clipped segment
	 */
	protected List<Vertex> clip(Vertex v1, Vertex v2, Vector2 n, double offset) {
		List<Vertex> points = new ArrayList<Vertex>(2);
		Vector2 p1 = v1.getPoint();
		Vector2 p2 = v2.getPoint();
		Vector2 e = p1.to(p2);
		
		// calculate the distance between the end points of the edge and the clip line
		double d1 = n.dot(p1) - offset;
		double d2 = n.dot(p2) - offset;
		
		// add the points if they are behind the line
		if (d1 <= 0.0) points.add(v1);
		if (d2 <= 0.0) points.add(v2);
		
		// check if they are on opposing sides of the line
		if (d1 * d2 < 0.0) {
			// clip to obtain another point
			double u = d1 / (d1 - d2);
			e.multiply(u);
			e.add(p1);
			if (d1 > 0.0) {
				points.add(new Vertex(e, v1.getIndex()));
			} else {
				points.add(new Vertex(e, v2.getIndex()));
			}
		}
		return points;
	}
}