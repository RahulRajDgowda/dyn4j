/*
 * Copyright (c) 2010-2012 William Bittle  http://www.dyn4j.org/
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
package org.dyn4j.testbed.test;

import org.dyn4j.collision.Bounds;
import org.dyn4j.collision.RectangularBounds;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Triangle;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.testbed.ContactCounter;
import org.dyn4j.testbed.Entity;
import org.dyn4j.testbed.Test;

/**
 * Tests a seesaw type configuration.
 * @author William Bittle
 * @version 2.0.0
 * @since 1.0.0
 */
public class Seesaw extends Test {
	/* (non-Javadoc)
	 * @see org.dyn4j.testbed.Test#getName()
	 */
	@Override
	public String getName() {
		return "Seesaw";
	}
	
	/* (non-Javadoc)
	 * @see test.Test#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Tests a seesaw.  A box is dropped onto a seesaw who has another box "
		+ "on the opposite end.  The dropped box has more mass than "
		+ "the other and therefore should hold the other box up.";
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#initialize()
	 */
	@Override
	public void initialize() {
		// call the super method
		super.initialize();
		
		// set the camera
		this.home();
		
		// create the world
		Bounds bounds = new RectangularBounds(Geometry.createRectangle(16.0, 15.0));
		this.world = new World(bounds);
		
		// setup the contact counter
		ContactCounter cc = new ContactCounter();
		this.world.addListener(cc);
		
		// setup the bodies
		this.setup();
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#setup()
	 */
	@Override
	protected void setup() {
		// create the floor
		Rectangle floorShape = new Rectangle(15.0, 1.0);
		Entity floor = new Entity();
		floor.addFixture(new BodyFixture(floorShape));
		floor.setMass(Mass.Type.INFINITE);
		this.world.add(floor);
		
		// create the pivot
		Triangle pivotShape = new Triangle(
				new Vector2(0.0, 0.5),
				new Vector2(-1.0, 0.0),
				new Vector2(1.0, 0.0));
		Entity pivot = new Entity();
		pivot.addFixture(new BodyFixture(pivotShape));
		pivot.setMass(Mass.Type.INFINITE);
		pivot.translate(0.0, 0.5);
		this.world.add(pivot);
		
		// create the plank
		Rectangle plankShape = new Rectangle(10.0, 0.2);
		Entity plank = new Entity();
		plank.addFixture(new BodyFixture(plankShape));
		plank.setMass();
		plank.translate(0.0, 1.5);
		this.world.add(plank);
		
		// create a box
		Rectangle box = new Rectangle(1.0, 1.0);
		
		// create a box on the left side
		Entity lBox = new Entity();
		lBox.addFixture(new BodyFixture(box));
		lBox.setMass();
		lBox.translate(-4.0, 2.5);
		this.world.add(lBox);
		
		box = new Rectangle(1.0, 1.0);
		
		BodyFixture boxFixture = new BodyFixture(box);
		boxFixture.setDensity(3.0);
		
		// create a box on the right side
		Entity rBox = new Entity();
		rBox.addFixture(boxFixture);
		rBox.setMass();
		rBox.translate(4.0, 5.7);
		this.world.add(rBox);
	}
	
	/* (non-Javadoc)
	 * @see org.dyn4j.Test#home()
	 */
	@Override
	public void home() {
		// set the scale
		this.scale = 64.0;
		// set the offset
		this.offset.set(0.0, -2.0);
	}
}