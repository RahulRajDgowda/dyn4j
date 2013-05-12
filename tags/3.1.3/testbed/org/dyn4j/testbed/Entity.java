/*
 * Copyright (c) 2011 William Bittle  http://www.dyn4j.org/
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
package org.dyn4j.testbed;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Segment;
import org.dyn4j.geometry.Shape;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.geometry.Wound;

/**
 * Represents a game entity.
 * @author William Bittle
 * @version 3.0.2
 * @since 1.0.0
 */
public class Entity extends Body {
	/** Fill color for frozen bodies */
	private static final float[] FROZEN_COLOR = new float[] { 1.0f, 1.0f, 200.0f / 255.0f };
	
	/** Fill color for asleep bodies */
	private static final float[] ASLEEP_COLOR = new float[] { 200.0f / 255.0f, 200.0f / 255.0f, 1.0f };
	
	/** The length of an edge normal */
	private static final double NORMAL_LENGTH = 0.1;
	
	/** The random fill color */
	private float[] color = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
	
	/**
	 * Optional constructor.
	 */
	public Entity() {
		this(1.0f);
	}
	
	/**
	 * Full constructor.
	 * @param alpha the alpha value used for the colors; in the range [0.0, 1.0]
	 */
	public Entity(float alpha) {
		this.initialize(alpha);
	}
	
	/**
	 * Full constructor.
	 * @param red the red component; in the range [0.0, 1.0]
	 * @param green the green component; in the range [0.0, 1.0]
	 * @param blue the blue component; in the range [0.0, 1.0]
	 * @param alpha the alpha component; in the range [0.0, 1.0]
	 */
	public Entity(float red, float green, float blue, float alpha) {
		this.color[0] = red;
		this.color[1] = green;
		this.color[2] = blue;
		this.color[3] = alpha;
	}
	
	/**
	 * Initializes the entity.
	 * @param alpha the alpha component
	 */
	private void initialize(float alpha) {
		// create a random (pastel) fill color
		this.color[0] = (float)Math.random() * 0.5f + 0.5f;
		this.color[1] = (float)Math.random() * 0.5f + 0.5f;
		this.color[2] = (float)Math.random() * 0.5f + 0.5f;
		this.color[3] = alpha;
	}
	
	/**
	 * Renders the body to the given graphics object using the given
	 * world to device/screen space transform.
	 * @param gl the OpenGL graphics context
	 */
	public void render(GL2 gl) {
		Draw draw = Draw.getInstance();
		
		// get the transform
		Transform tx = this.transform;
		
		// save the current matrix
		gl.glPushMatrix();
		// translate in the x-y plane
		gl.glTranslated(tx.getTranslationX(), tx.getTranslationY(), 0);
		// rotate about the z axis
		gl.glRotated(Math.toDegrees(tx.getRotation()), 0, 0, 1);
		
		int size = this.getFixtureCount();
		// draw the shapes
		for (int i = 0; i < size; i++) {
			Fixture f = this.getFixture(i);
			Convex c = f.getShape();
			// check if we should render fill color
			if (draw.drawFill()) {
				// set the color
				gl.glColor4fv(color, 0);
				// check for asleep
				if (this.isAsleep())
					gl.glColor4f(Entity.ASLEEP_COLOR[0], 
							     Entity.ASLEEP_COLOR[1], 
							     Entity.ASLEEP_COLOR[2], 
							     color[3]); // use the desired color's alpha setting
				// check for inactive
				if (!this.isActive())
					gl.glColor4f(Entity.FROZEN_COLOR[0], 
						         Entity.FROZEN_COLOR[1], 
						         Entity.FROZEN_COLOR[2], 
						         color[3]); // use the desired color's alpha setting
				
				// fill the shape
				this.fillShape(gl, c);
			}
			
			// check if we should render outlines
			if (draw.drawOutline()) {
				// all the outline colors are multiplied by 0.8f to make them
				// slightly darker than the fill color
				
				// set the color
				gl.glColor4f(color[0] * 0.8f, color[1] * 0.8f, color[2] * 0.8f, color[3]);
				// check for asleep
				if (this.isAsleep())
					gl.glColor4f(Entity.ASLEEP_COLOR[0] * 0.8f, 
							     Entity.ASLEEP_COLOR[1] * 0.8f, 
							     Entity.ASLEEP_COLOR[2] * 0.8f, 
							     color[3]); // use the desired color's alpha setting
				// check for inactive
				if (!this.isActive())
					gl.glColor4f(Entity.FROZEN_COLOR[0] * 0.8f, 
						         Entity.FROZEN_COLOR[1] * 0.8f, 
						         Entity.FROZEN_COLOR[2] * 0.8f, 
						         color[3]); // use the desired color's alpha setting
				
				// draw the shape
				this.drawShape(gl, c);
			}
			
			// check if we should draw edge normals
			if (draw.drawNormals()) {
				// is the current shape a wound shape?
				if (c instanceof Wound) {
					// if so then set the color
					float[] color = draw.getNormalsColor();
					gl.glColor4fv(color, 0);
					// render the normals
					this.renderNormals(gl, (Wound)c);
				}
			}
		}
		

		// check if we should draw the rotation disc
		if (draw.drawRotationDisc()) {
			Vector2 c = this.mass.getCenter();
			// set the color
			float[] color = draw.getRotationDiscColor();
			gl.glColor4fv(color, 0);
			// get the radius
			double r = this.getRotationDiscRadius();
			
			// draw a circle
			GLHelper.renderCircle(gl, c.x, c.y, r, 20);
		}
		
		// check if we should draw center points
		if (draw.drawCenter()) {
			// get the center of mass
			Vector2 c = this.mass.getCenter();
			// set the color
			float[] color = draw.getCenterColor();
			gl.glColor4fv(color, 0);
			// draw a circle a tenth of the size of the rotation disc
			GLHelper.renderCircle(gl, c.x, c.y, this.getRotationDiscRadius() * 0.1, 20);
		}
		
		// restore the original transformation
		gl.glPopMatrix();
		
		// check if we should draw velocity vectors
		if (draw.drawVelocity()) {
			// set the color
			float[] color = draw.getVelocityColor();
			gl.glColor4fv(color, 0);
			// draw the velocities
				Vector2 c = this.getWorldCenter();
				Vector2 v = this.getVelocity();
				double av = this.getAngularVelocity();
				
				// draw the linear velocity for each body
				gl.glBegin(GL.GL_LINES);
					gl.glVertex2d(c.x, c.y);
					gl.glVertex2d(c.x + v.x, c.y + v.y);
				gl.glEnd();
				
				// draw an arc
				GLHelper.renderArc(gl, c.x, c.y, 0.125, 0, av, 20);
		}
	}
	
	/**
	 * Renders an outline of the given convex object.
	 * @param gl the OpenGL graphics context
	 * @param c the convex object to render
	 */
	private void drawShape(GL2 gl, Convex c) {
		// check for polygon
		if (c instanceof Polygon) {
			// cast and get the data
			Polygon p = (Polygon) c;
			Vector2[] vertices = p.getVertices();
			int size = vertices.length;
			
			// render each vertex in a line loop
			Vector2 v;
			gl.glBegin(GL.GL_LINE_LOOP);
			for (int i = 0; i < size; i++) {
				v = vertices[i];
				gl.glVertex2d(v.x, v.y);
			}
			gl.glEnd();
		// check for circle
		} else if (c instanceof Circle) {
			// cast and get data
			Circle circle = (Circle) c;
			double radius = circle.getRadius();
			Vector2 cc = circle.getCenter();
			
			// render a line loop using the stored sin and cos tables
			GLHelper.renderCircle(gl, cc.x, cc.y, radius, 20);
			
			// render a line from the center to the extent to show rotation
			Vector2 e = circle.getCenter().sum(radius, 0.0);
			gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(cc.x, cc.y);
				gl.glVertex2d(e.x, e.y);
			gl.glEnd();
		} else if (c instanceof Segment) {
			Segment seg = (Segment) c;
			Vector2 p1 = seg.getPoint1();
			Vector2 p2 = seg.getPoint2();
			
			gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(p1.x, p1.y);
				gl.glVertex2d(p2.x, p2.y);
			gl.glEnd();
		}
	}
	
	/**
	 * Renders a fill color for the given convex object.
	 * @param gl the OpenGL graphics context
	 * @param c the convex object to render
	 */
	private void fillShape(GL2 gl, Convex c) {
		// check for polygon
		if (c instanceof Polygon) {
			// cast and get data
			Polygon p = (Polygon) c;
			Vector2[] vertices = p.getVertices();
			int size = vertices.length;
			
			// fill a polygon bound by vertices
			Vector2 v;
			gl.glBegin(GL2.GL_POLYGON);
			for (int i = 0; i < size; i++) {
				v = vertices[i];
				gl.glVertex2d(v.x, v.y);
			}
			gl.glEnd();
		// check for circle
		} else if (c instanceof Circle) {
			// cast and get data
			Circle circle = (Circle) c;
			double radius = circle.getRadius();
			Vector2 cc = circle.getCenter();
			
			// use a triangle fan about the unit circle
			GLHelper.fillCircle(gl, cc.x, cc.y, radius, 20);
		}
		// nothing to do for segment
	}
	
	/**
	 * Renders the edge normals of the given {@link Wound} {@link Shape}.
	 * @param gl the OpenGL graphics context
	 * @param w the {@link Wound} {@link Shape} to render normals
	 */
	private void renderNormals(GL2 gl, Wound w) {
		// get the data
		Vector2[] vertices = w.getVertices();
		Vector2[] normals = w.getNormals();
		int size = normals.length;
		
		// declare some place holders
		Vector2 p1, p2, n;
		Vector2 mid = new Vector2();
					
		// render all the normals
		for (int i = 0; i < size; i++) {
			// get the points and the normal
			p1 = vertices[i];
			p2 = vertices[(i + 1 == size) ? 0 : i + 1];
			n = normals[i];
			
			// find the mid point between p1 and p2
			mid.set(p2).subtract(p1).multiply(0.5).add(p1);
			
			gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(mid.x, mid.y);
				gl.glVertex2d(mid.x + n.x * NORMAL_LENGTH, mid.y + n.y * NORMAL_LENGTH);
			gl.glEnd();
		}
	}
	
	/**
	 * Returns this entity's color.
	 * @return Color
	 * @since 1.0.3
	 */
	public float[] getColor() {
		return this.color;
	}
}
