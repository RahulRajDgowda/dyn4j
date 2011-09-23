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
package org.dyn4j.dynamics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.dyn4j.collision.Bounds;
import org.dyn4j.collision.BoundsAdapter;
import org.dyn4j.collision.BoundsListener;
import org.dyn4j.collision.Filter;
import org.dyn4j.collision.broadphase.BroadphaseDetector;
import org.dyn4j.collision.broadphase.BroadphasePair;
import org.dyn4j.collision.broadphase.DynamicAABBTree;
import org.dyn4j.collision.broadphase.SapIncremental;
import org.dyn4j.collision.continuous.ConservativeAdvancement;
import org.dyn4j.collision.continuous.TimeOfImpact;
import org.dyn4j.collision.continuous.TimeOfImpactDetector;
import org.dyn4j.collision.manifold.ClippingManifoldSolver;
import org.dyn4j.collision.manifold.Manifold;
import org.dyn4j.collision.manifold.ManifoldSolver;
import org.dyn4j.collision.narrowphase.Gjk;
import org.dyn4j.collision.narrowphase.NarrowphaseDetector;
import org.dyn4j.collision.narrowphase.Penetration;
import org.dyn4j.collision.narrowphase.Raycast;
import org.dyn4j.collision.narrowphase.RaycastDetector;
import org.dyn4j.dynamics.Settings.ContinuousDetectionMode;
import org.dyn4j.dynamics.contact.Contact;
import org.dyn4j.dynamics.contact.ContactAdapter;
import org.dyn4j.dynamics.contact.ContactConstraint;
import org.dyn4j.dynamics.contact.ContactEdge;
import org.dyn4j.dynamics.contact.ContactListener;
import org.dyn4j.dynamics.contact.ContactManager;
import org.dyn4j.dynamics.contact.ContactPoint;
import org.dyn4j.dynamics.contact.TimeOfImpactSolver;
import org.dyn4j.dynamics.joint.Joint;
import org.dyn4j.dynamics.joint.JointEdge;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

/**
 * Manages the logic of collision detection, resolution, and reporting.
 * <p>
 * Employs the same {@link Island} solving technique as <a href="http://www.box2d.org">Box2d</a>'s equivalent class.
 * @see <a href="http://www.box2d.org">Box2d</a>
 * @author William Bittle
 * @version 3.0.1
 * @since 1.0.0
 */
public class World {
	/** Earths gravity constant */
	public static final Vector2 EARTH_GRAVITY = new Vector2(0.0, -9.8);
	
	/** Zero gravity constant */
	public static final Vector2 ZERO_GRAVITY = new Vector2(0.0, 0.0);
		
	/** The {@link Step} used by the dynamics calculations */
	protected Step step;
	
	/** The world gravity vector */
	protected Vector2 gravity;
	
	/** The world {@link Bounds} */
	protected Bounds bounds;
	
	/** The {@link BroadphaseDetector} */
	protected BroadphaseDetector<Body> broadphaseDetector;
	
	/** The {@link NarrowphaseDetector} */
	protected NarrowphaseDetector narrowphaseDetector;
	
	/** The {@link ManifoldSolver} */
	protected ManifoldSolver manifoldSolver;
	
	/** The {@link TimeOfImpactDetector} */
	protected TimeOfImpactDetector timeOfImpactDetector;
	
	/** The {@link RaycastDetector} */
	protected RaycastDetector raycastDetector;
	
	/** The {@link CollisionListener} */
	protected CollisionListener collisionListener;
	
	/** The {@link ContactManager} */
	protected ContactManager contactManager;

	/** The {@link ContactListener} */
	protected ContactListener contactListener;
	
	/** The {@link TimeOfImpactListener} */
	protected TimeOfImpactListener timeOfImpactListener;
	
	/** The {@link RaycastListener} */
	protected RaycastListener raycastListener;
	
	/** The {@link BoundsListener} */
	protected BoundsListener boundsListener;
	
	/** The {@link DestructionListener} */
	protected DestructionListener destructionListener;
	
	/** The {@link StepListener} */
	protected StepListener stepListener;
	
	/** The {@link CoefficientMixer} */
	protected CoefficientMixer coefficientMixer;
	
	/** The {@link TimeOfImpactSolver} */
	protected TimeOfImpactSolver timeOfImpactSolver;
	
	/** The {@link Body} list */
	protected List<Body> bodies;
	
	/** The {@link Joint} list */
	protected List<Joint> joints;
	
	/** The reusable island */
	protected Island island;
	
	/** The accumulated time */
	protected double time;
	
	/** Flag to find new contacts */
	protected boolean updateRequired;
	
	/**
	 * Default constructor.
	 * <p>
	 * Builds a simulation {@link World} without bounds.
	 */
	public World() {
		this(null);
	}
	
	/**
	 * Full constructor.
	 * <p>
	 * Defaults to using {@link #EARTH_GRAVITY}, {@link SapIncremental} broad-phase,
	 * {@link Gjk} narrow-phase, and {@link ClippingManifoldSolver}.
	 * @param bounds the bounds of the {@link World}; can be null
	 */
	public World(Bounds bounds) {
		// initialize all the classes with default values
		this.step = new Step();
		this.gravity = World.EARTH_GRAVITY;
		this.bounds = bounds;
		this.broadphaseDetector = new DynamicAABBTree<Body>();
		this.narrowphaseDetector = new Gjk();
		this.manifoldSolver = new ClippingManifoldSolver();
		this.timeOfImpactDetector = new ConservativeAdvancement();
		this.raycastDetector = new Gjk();
		// create default listeners
		this.collisionListener = new CollisionAdapter();
		this.contactManager = new ContactManager();
		this.contactListener = new ContactAdapter();
		this.timeOfImpactListener = new TimeOfImpactAdapter();
		this.raycastListener = new RaycastAdapter();
		this.boundsListener = new BoundsAdapter();
		this.destructionListener = new DestructionAdapter();
		this.stepListener = new StepAdapter();
		this.coefficientMixer = CoefficientMixer.DEFAULT_MIXER;
		this.timeOfImpactSolver = new TimeOfImpactSolver();
		this.bodies = new ArrayList<Body>();
		this.joints = new ArrayList<Joint>();
		this.island = new Island();
		this.time = 0.0;
		this.updateRequired = true;
	}
	
	/**
	 * Updates the {@link World}.
	 * <p>
	 * This method will only update the world given the step frequency contained
	 * in the {@link Settings} object.  You can use the {@link StepListener} interface
	 * to listen for when a step is actually performed.  In addition, this method will
	 * return true if a step was performed.
	 * <p>
	 * Alternatively you can call the {@link #updatev(double)} method to use a variable
	 * time step.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @see #updatev(double)
	 * @param elapsedTime the elapsed time in seconds
	 * @return boolean true if the {@link World} performed a simulation step
	 */
	public boolean update(double elapsedTime) {
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return false;
		// update the time
		this.time += elapsedTime;
		// check the frequency in settings
		double invhz = Settings.getInstance().getStepFrequency();
		// see if we should update or not
		if (this.time >= invhz) {
			// update the step
			this.step.update(invhz);
			// reset the time
			this.time = this.time - invhz;
			// step the world
			this.step();
			// return true indicating we performed a simulation step
			return true;
		}
		return false;
	}
	
	/**
	 * Updates the {@link World}.
	 * <p>
	 * This method will update the world on every call.  Unlike the {@link #update(double)}
	 * method, this method uses the given elapsed time and does not attempt to update the world
	 * in a set interval.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @see #update(double)
	 * @param elapsedTime the elapsed time in seconds
	 */
	public void updatev(double elapsedTime) {
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return;
		// update the step
		this.step.update(elapsedTime);
		// step the world
		this.step();
	}
	
	/**
	 * Performs the given number of simulation steps using the step frequency in {@link Settings}.
	 * @param steps the number of simulation steps to perform
	 */
	public void step(int steps) {
		// get the frequency from settings
		double invhz = Settings.getInstance().getStepFrequency();
		// perform the steps
		this.step(steps, invhz);
	}
	
	/**
	 * Performs the given number of simulation steps using the given elapsed time for each step.
	 * <p>
	 * This method immediately returns if the given elapsedTime is less than or equal to
	 * zero.
	 * @param steps the number of simulation steps to perform
	 * @param elapsedTime the elapsed time for each step
	 */
	public void step(int steps, double elapsedTime) {
		// make sure the number of steps is greather than zero
		if (steps <= 0) return;
		// make sure the update time is greater than zero
		if (elapsedTime <= 0.0) return;
		// perform the steps
		for (int i = 0; i < steps; i++) {
			// update the step object
			this.step.update(elapsedTime);
			// step the world
			this.step();
		}
	}
	
	/**
	 * Steps the {@link World} using the current {@link Step}.
	 * <p>
	 * Performs collision detection and resolution.
	 * <p>
	 * Use the various listeners to listen for events during the execution of
	 * this method.
	 * <p>
	 * Take care when performing methods on the {@link World} object in any
	 * event listeners tied to this method.
	 */
	protected void step() {
		// notify the step listener
		this.stepListener.begin(this.step, this);
		
		// check if we need to update the contacts first
		if (this.updateRequired) {
			// if so then update the contacts
			this.detect();
			// set the update required flag to false
			this.updateRequired = false;
		}
		
		// notify of all the contacts that will be solved and all the sensed contacts
		this.contactManager.preSolveNotify(this.contactListener);
		
		// check for CCD
		ContinuousDetectionMode continuousDetectionMode = Settings.getInstance().getContinuousDetectionMode();
		
		// get the number of bodies
		int size = this.bodies.size();
		
		// test for out of bounds objects
		// clear the body contacts
		// clear the island flag
		// save the current transform for CCD
		for (int i = 0; i < size; i++) {
			Body body = this.bodies.get(i);
			// remove the island flag
			body.setOnIsland(false);
			// we only need to save the old transform for CCD so don't
			// bother if its completely disabled
			if (continuousDetectionMode != ContinuousDetectionMode.NONE) {
				// save the current transform into the previous transform
				body.transform0.set(body.transform);
			}
		}
		
		// clear the joint island flags
		int jSize = this.joints.size();
		for (int i = 0; i < jSize; i++) {
			// get the joint
			Joint joint = this.joints.get(i);
			// set the island flag to false
			joint.setOnIsland(false);
		}
		
		// perform a depth first search of the contact graph
		// to create islands for constraint solving
		Stack<Body> stack = new Stack<Body>();
		stack.ensureCapacity(size);
		// loop over the bodies and their contact edges to create the islands
		for (int i = 0; i < size; i++) {
			Body seed = this.bodies.get(i);
			// skip if asleep, in active, static, or already on an island
			if (seed.isOnIsland() || seed.isAsleep() || !seed.isActive() || seed.isStatic()) continue;
			
			// set the island to the reusable island
			Island island = this.island;
			
			island.clear();
			stack.clear();
			stack.push(seed);
			while (stack.size() > 0) {
				// get the next body
				Body body = stack.pop();
				// add it to the island
				island.add(body);
				// flag that it has been added
				body.setOnIsland(true);
				// make sure the body is awake
				body.setAsleep(false);
				// if its static then continue since we dont want the
				// island to span more than one static object
				// this keeps the size of the islands small
				if (body.isStatic()) continue;
				// loop over the contact edges of this body
				int ceSize = body.contacts.size();
				for (int j = 0; j < ceSize; j++) {
					ContactEdge contactEdge = body.contacts.get(j);
					// get the contact constraint
					ContactConstraint contactConstraint = contactEdge.getContactConstraint();
					// skip sensor contacts
					if (contactConstraint.isSensor()) continue;
					// get the other body
					Body other = contactEdge.getOther();
					// check if the contact constraint has already been added to an island
					if (contactConstraint.isOnIsland()) continue;
					// add the contact constraint to the island list
					island.add(contactConstraint);
					// set the island flag on the contact constraint
					contactConstraint.setOnIsland(true);
					// has the other body been added to an island yet?
					if (!other.isOnIsland()) {
						// if not then add this body to the stack
						stack.push(other);
						other.setOnIsland(true);
					}
				}
				// loop over the joint edges of this body
				int jeSize = body.joints.size();
				for (int j = 0; j < jeSize; j++) {
					// get the joint edge
					JointEdge jointEdge = body.joints.get(j);
					// get the joint
					Joint joint = jointEdge.getJoint();
					// check if the joint is inactive
					if (!joint.isActive()) continue;
					// get the other body
					Body other = jointEdge.getOther();
					// check if the joint has already been added to an island
					// or if the other body is not active
					if (joint.isOnIsland() || !other.isActive()) continue;
					// add the joint to the island
					island.add(joint);
					// set the island flag on the joint
					joint.setOnIsland(true);
					// check if the other body has been added to an island
					if (!other.isOnIsland()) {
						// if not then add the body to the stack
						stack.push(other);
						other.setOnIsland(true);
					}
				}
			}
			
			// solve the island
			island.solve(this.gravity, this.step);
			
			// allow static bodies to participate in other islands
			for (int j = 0; j < size; j++) {
				Body body = this.bodies.get(j);
				if (body.isStatic()) {
					body.setOnIsland(false);
				}
			}
		}
		
		// notify of the all solved contacts
		this.contactManager.postSolveNotify(this.contactListener);
		
		// make sure CCD is enabled
		if (continuousDetectionMode != ContinuousDetectionMode.NONE) {
			// solve time of impact
			this.solveTOI(continuousDetectionMode);
		}
		
		// after all has been updated find new contacts
		// this done so that the user has the latest contacts
		// the broadphase has the latest AABBs, etc.
		this.detect();
		
		// set the update required flag to false
		this.updateRequired = false;
		
		// notify the step listener
		this.stepListener.end(this.step, this);
	}
	
	/**
	 * Finds new contacts for all bodies in this world.
	 * <p>
	 * This method performs the following:
	 * <ol>
	 * 	<li>Checks for out of bound bodies</li>
	 * 	<li>Updates the broadphase using the current body positions</li>
	 * 	<li>Performs broadphase collision detection</li>
	 * 	<li>Performs narrowphase collision detection</li>
	 * 	<li>Performs manifold solving</li>
	 * 	<li>Adds contacts to the contact manager</li>
	 * 	<li>Warm starts the contacts</li>
	 * </ol>
	 * <p>
	 * @since 3.0.0
	 */
	protected void detect() {
		// clear the old contact list (does NOT clear the contact map
		// which is used to warm start)
		this.contactManager.clear();
		
		// get the number of bodies
		int size = this.bodies.size();
		
		// test for out of bounds objects
		// clear the body contacts
		// update the broadphase
		for (int i = 0; i < size; i++) {
			Body body = this.bodies.get(i);
			// skip if already not active
			if (!body.isActive()) continue;
			// clear all the old contacts
			body.contacts.clear();
			// check if bounds have been set
			if (this.bounds != null) {
				// check if the body is out of bounds
				if (this.bounds.isOutside(body)) {
					// set the body to inactive
					body.setActive(false);
					// if so, notify via the listener
					this.boundsListener.outside(body);
				}
			}
			// update the broadphase with the new position/orientation
			this.broadphaseDetector.update(body);
		}
		
		// make sure there are some bodies
		if (size > 0) {
			// test for collisions via the broad-phase
			List<BroadphasePair<Body>> pairs = this.broadphaseDetector.detect();
			int pSize = pairs.size();		
			
			// using the broad-phase results, test for narrow-phase
			for (int i = 0; i < pSize; i++) {
				BroadphasePair<Body> pair = pairs.get(i);
				
				// get the bodies
				Body body1 = pair.a;
				Body body2 = pair.b;
				
				// inactive objects don't have collision detection/response
				if (!body1.isActive() || !body2.isActive()) continue;
				// one body must be dynamic
				if (!body1.isDynamic() && !body2.isDynamic()) continue;
				// check for connected pairs who's collision is not allowed
				if (body1.isConnected(body2, false)) continue;
				
				// notify of the broadphase collision
				if (!this.collisionListener.collision(body1, body2)) {
					// if the collision listener returned false then skip this collision
					continue;
				}
	
				// get their transforms
				Transform transform1 = body1.transform;
				Transform transform2 = body2.transform;
				
				// create a reusable penetration object
				Penetration penetration = new Penetration();
				// create a reusable manifold object
				Manifold manifold = new Manifold();
				
				// loop through the fixtures of body 1
				int b1Size = body1.getFixtureCount();
				int b2Size = body2.getFixtureCount();
				for (int j = 0; j < b1Size; j++) {
					BodyFixture fixture1 = body1.getFixture(j);
					Filter filter1 = fixture1.getFilter();
					Convex convex1 = fixture1.getShape();
					// test against each fixture of body 2
					for (int k = 0; k < b2Size; k++) {
						BodyFixture fixture2 = body2.getFixture(k);
						Filter filter2 = fixture2.getFilter();
						Convex convex2 = fixture2.getShape();
						// test the filter
						if (!filter1.isAllowed(filter2)) {
							// if the collision is not allowed then continue
							continue;
						}
						// test the two convex shapes
						if (this.narrowphaseDetector.detect(convex1, transform1, convex2, transform2, penetration)) {
							// check for zero penetration
							if (penetration.getDepth() == 0.0) {
								// this should only happen if numerical error occurs
								continue;
							}
							// notify of the narrow-phase collision
							if (!this.collisionListener.collision(body1, fixture1, body2, fixture2, penetration)) {
								// if the collision listener returned false then skip this collision
								continue;
							}
							// if there is penetration then find a contact manifold
							// using the filled in penetration object
							if (this.manifoldSolver.getManifold(penetration, convex1, transform1, convex2, transform2, manifold)) {
								// check for zero points
								if (manifold.getPoints().size() == 0) {
									// this should only happen if numerical error occurs
									continue;
								}
								// notify of the manifold solving result
								if (!this.collisionListener.collision(body1, fixture1, body2, fixture2, manifold)) {
									// if the collision listener returned false then skip this collision
									continue;
								}
								// compute the friction and restitution
								double friction = this.coefficientMixer.mixFriction(fixture1.getFriction(), fixture2.getFriction());
								double restitution = this.coefficientMixer.mixRestitution(fixture1.getRestitution(), fixture2.getRestitution());
								// create a contact constraint
								ContactConstraint contactConstraint = new ContactConstraint(body1, fixture1, 
										                                                    body2, fixture2, 
										                                                    manifold, 
										                                                    friction, restitution);
								// add a contact edge to both bodies
								ContactEdge contactEdge1 = new ContactEdge(body2, contactConstraint);
								ContactEdge contactEdge2 = new ContactEdge(body1, contactConstraint);
								body1.contacts.add(contactEdge1);
								body2.contacts.add(contactEdge2);
								// add the contact constraint to the contact manager
								this.contactManager.add(contactConstraint);
							}
						}
					}
				}
			}
		}
		
		// warm start the contact constraints
		this.contactManager.updateContacts(this.contactListener);
	}
	
	/**
	 * Solves the time of impact for all the {@link Body}s in this {@link World}.
	 * <p>
	 * This method solves for the time of impact for each {@link Body} iteratively
	 * and pairwise.
	 * <p>
	 * The cases considered are dependent on the current collision detection mode.
	 * <p>
	 * Cases skipped (including the converse of the above):
	 * <ul>
	 * <li>Inactive, asleep, or non-moving bodies</li>
	 * <li>Bodies connected via a joint with the collision flag set to false</li>
	 * <li>Bodies already in contact</li>
	 * <li>Fixtures whose filters return false</li>
	 * <li>Sensor fixtures</li>
	 * </ul>
	 * @param mode the continuous collision detection mode
	 * @see Settings.ContinuousDetectionMode
	 * @since 1.2.0
	 */
	protected void solveTOI(ContinuousDetectionMode mode) {
		// get the number of bodies
		int size = this.bodies.size();
		
		// check the CCD mode
		boolean bulletsOnly = mode == ContinuousDetectionMode.BULLETS_ONLY;
		
		// loop over all the bodies and find the minimum TOI for each
		// dynamic body
		for (int i = 0; i < size; i++) {
			// get the body
			Body body = this.bodies.get(i);
			
			// if we are only doing CCD on bullets only, then check
			// to make sure that the current body is a bullet
			if (bulletsOnly && !body.isBullet()) continue;
			
			// otherwise we process all dynamic bodies
				
			// we don't process kinematic or static bodies except with
			// dynamic bodies (in other words b1 must always be a dynamic
			// body)
			if (body.isKinematic() || body.isStatic()) continue;
			
			// don't bother with bodies that did not have their
			// positions integrated, if they were not added to an island then
			// that means they didn't move
			
			// we can also check for sleeping bodies and skip those since
			// they will only be asleep after being stationary for a set
			// time period
			if (!body.isOnIsland() || body.isAsleep()) continue;

			// solve for time of impact
			this.solveTOI(body);
		}
	}
	
	/**
	 * Solves the time of impact for the given {@link Body}.
	 * <p>
	 * This method will find the first {@link Body} that the given {@link Body}
	 * collides with unless ignored via the {@link TimeOfImpactListener}.
	 * <p>
	 * After the first {@link Body} is found the two {@link Body}s are interpolated
	 * to the time of impact.
	 * <p>
	 * Then the {@link Body}s are position solved using the {@link TimeOfImpactSolver}
	 * to force the {@link Body}s into collision.  This causes the discrete collision
	 * detector to detect the collision on the next time step.
	 * @param body the {@link Body}
	 * @since 2.0.0
	 */
	protected void solveTOI(Body body) {
		int size = this.bodies.size();
		
		// setup the initial time bounds [0, 1]
		double t1 = 0.0;
		double t2 = 1.0;
		
		// save the minimum time of impact and body
		TimeOfImpact minToi = null;
		Body minBody = null;
		
		// loop over all the other bodies to find the minimum TOI
		for (int j = 0; j < size; j++) {
			// get the other body
			Body b2 = this.bodies.get(j);
			
			// make sure the other body is active
			if (!b2.isActive()) continue;
			
			// skip other dynamic bodies; we only do TOI for
			// dynamic vs. static/kinematic unless its a bullet
			if (b2.isDynamic() && !body.isBullet()) continue;
			
			// check for connected pairs who's collision is not allowed
			if (body.isConnected(b2, false)) continue;
			
			// check for bodies already in collision
			if (body.isInContact(b2)) continue;
			
			// skip this test if they are the same body
			if (body == b2) continue;
			
			// compute the time of impact between the two bodies
			TimeOfImpact toi = new TimeOfImpact();
			if (this.timeOfImpactDetector.getTimeOfImpact(body, b2, t1, t2, toi)) {
				// get the time of impact
				double t = toi.getToi();
				// check if the time of impact is less than
				// the current time of impact
				if (t < t2) {
					// if it is then ask the listener if we should use this collision
					if (this.timeOfImpactListener.collision(body, b2, toi)) {
						// set the new upper bound
						t2 = t;
						// save the minimum toi and body
						minToi = toi;
						minBody = b2;
					}
				}
			}
			// if the bodies are intersecting or do not intersect
			// within the range of motion then skip this body
			// and move to the next
		}
		
		// make sure the time of impact is not null
		if (minToi != null) {
			// get the time of impact info
			double t = minToi.getToi();
			
			// move the dynamic body to the time of impact
			body.transform0.lerp(body.transform, t, body.transform);
			// check if the other body is dynamic
			if (minBody.isDynamic()) {
				// if the other body is dynamic then interpolate its transform also
				minBody.transform0.lerp(minBody.transform, t, minBody.transform);
			}
			// this should bring the bodies within d distance from one another
			// we need to move the bodies more so that they are in collision
			// so that on the next time step they are solved by the discrete
			// collision detector
			
			// performs position correction on the body/bodies so that they are
			// in collision and will be detected in the next time step
			this.timeOfImpactSolver.solve(body, minBody, minToi);
			
			// this method does not conserve time
		}
	}
	
	/**
	 * Performs a raycast against all the {@link Body}s in the {@link World}.
	 * <p>
	 * The given {@link RaycastResult} list, results, will be filled with the raycast results
	 * if the given ray intersected any bodies.
	 * <p>
	 * The {@link RaycastResult} class implements the Comparable interface to allow sorting by
	 * distance from the ray's origin.
	 * <p>
	 * If the all flag is false, the results list will only contain the closest result (if any).
	 * @param start the start point
	 * @param end the end point
	 * @param ignoreSensors true if sensor {@link BodyFixture}s should be ignored
	 * @param all true if all intersected {@link Body}s should be returned; false if only the closest {@link Body} should be returned
	 * @param results a list to contain the results of the raycast
	 * @return boolean true if at least one {@link Body} was intersected by the {@link Ray}
	 * @throws NullPointerException if start, end, or results is null
	 * @see #raycast(Ray, double, boolean, boolean, List)
	 * @see RaycastListener#allow(Ray, Body)
	 * @see RaycastListener#allow(Ray, Body, BodyFixture)
	 * @since 2.0.0
	 */
	public boolean raycast(Vector2 start, Vector2 end, boolean ignoreSensors, boolean all, List<RaycastResult> results) {
		// create the ray and obtain the maximum length
		Vector2 d = start.to(end);
		double maxLength = d.normalize();
		Ray ray = new Ray(start, d);
		// call the raycast method
		return this.raycast(ray, maxLength, ignoreSensors, all, results);
	}
	
	/**
	 * Performs a raycast against all the {@link Body}s in the {@link World}.
	 * <p>
	 * The given {@link RaycastResult} list, results, will be filled with the raycast results
	 * if the given ray intersected any bodies.
	 * <p>
	 * The {@link RaycastResult} class implements the Comparable interface to allow sorting by
	 * distance from the ray's origin.
	 * <p>
	 * If the all flag is false, the results list will only contain the closest result (if any).
	 * <p>
	 * Pass 0 into the maxLength field to specify an infinite length {@link Ray}.
	 * @param ray the {@link Ray}
	 * @param maxLength the maximum length of the ray; 0 for infinite length
	 * @param ignoreSensors true if sensor {@link BodyFixture}s should be ignored
	 * @param all true if all intersected {@link Body}s should be returned; false if only the closest {@link Body} should be returned
	 * @param results a list to contain the results of the raycast
	 * @return boolean true if at least one {@link Body} was intersected by the given {@link Ray}
	 * @throws NullPointerException if ray or results is null
	 * @see #raycast(Vector2, Vector2, boolean, boolean, List)
	 * @see RaycastListener#allow(Ray, Body)
	 * @see RaycastListener#allow(Ray, Body, BodyFixture)
	 * @since 2.0.0
	 */
	public boolean raycast(Ray ray, double maxLength, boolean ignoreSensors, boolean all, List<RaycastResult> results) {
		boolean found = false;
		// check for the desired length
		double max = 0.0;
		if (maxLength > 0.0) {
			max = maxLength;
		}
		// create a raycast result
		RaycastResult result = new RaycastResult();
		// if we are only looking for the minimum then go ahead
		// and add the result to the list
		if (!all) {
			results.add(result);
		}
		// filter using the broadphase first
		List<Body> bodies = this.broadphaseDetector.raycast(ray, maxLength);
		// loop over the list of bodies testing each one
		int size = bodies.size();
		for (int i = 0; i < size; i++) {
			// get a body to test
			Body body = bodies.get(i);
			// see if we should test this body
			if (!this.raycastListener.allow(ray, body)) continue;
			// otherwise test the body
			// does the ray intersect the body?
			if (raycast(ray, body, max, ignoreSensors, result)) {
				// check if we are raycasting for all the objects
				// or only the closest
				if (!all) {
					// we are only looking for the closest so
					// set the new maximum
					max = result.raycast.getDistance();
				} else {
					// add this result to the results
					results.add(result);
					// create a new result for the next iteration
					result = new RaycastResult();
				}
				// set the found flag
				found = true;
			}
		}
		
		return found;
	}
	
	/**
	 * Performs a raycast against the given {@link Body} and returns true
	 * if the ray intersects the body.
	 * <p>
	 * The given {@link RaycastResult} object, result, will be filled with the raycast result
	 * if the given ray intersected the given body. 
	 * @param start the start point
	 * @param end the end point
	 * @param body the {@link Body} to test
	 * @param ignoreSensors whether or not to ignore sensor {@link BodyFixture}s
	 * @param result the raycast result
	 * @return boolean true if the {@link Ray} intersects the {@link Body}
	 * @throws NullPointerException if start, end, body, or result is null
	 * @see #raycast(Ray, Body, double, boolean, RaycastResult)
	 * @see RaycastListener#allow(Ray, Body, BodyFixture)
	 * @since 2.0.0
	 */
	public boolean raycast(Vector2 start, Vector2 end, Body body, boolean ignoreSensors, RaycastResult result) {
		// create the ray and obtain the maximum length
		Vector2 d = start.to(end);
		double maxLength = d.normalize();
		Ray ray = new Ray(start, d);
		// call the raycast method
		return this.raycast(ray, body, maxLength, ignoreSensors, result);
	}
	
	/**
	 * Performs a raycast against the given {@link Body} and returns true
	 * if the ray intersects the body.
	 * <p>
	 * The given {@link RaycastResult} object, result, will be filled with the raycast result
	 * if the given ray intersected the given body.
	 * <p>
	 * Pass 0 into the maxLength field to specify an infinite length {@link Ray}.
	 * @param ray the {@link Ray} to cast
	 * @param body the {@link Body} to test
	 * @param maxLength the maximum length of the ray; 0 for infinite length
	 * @param ignoreSensors whether or not to ignore sensor {@link BodyFixture}s
	 * @param result the raycast result
	 * @return boolean true if the {@link Ray} intersects the {@link Body}
	 * @throws NullPointerException if ray, body, or result is null
	 * @see #raycast(Vector2, Vector2, Body, boolean, RaycastResult)
	 * @see RaycastListener#allow(Ray, Body, BodyFixture)
	 * @since 2.0.0
	 */
	public boolean raycast(Ray ray, Body body, double maxLength, boolean ignoreSensors, RaycastResult result) {
		// get the number of fixtures
		int size = body.getFixtureCount();
		// get the body transform
		Transform transform = body.getTransform();
		// set the maximum length
		double max = 0.0;
		if (maxLength > 0.0) {
			max = maxLength;
		}
		// create a raycast object to store the result
		Raycast raycast = new Raycast();
		// loop over the fixtures finding the closest one
		boolean found = false;
		for (int i = 0; i < size; i++) {
			// get the fixture
			BodyFixture fixture = body.getFixture(i);
			// check for sensor
			if (ignoreSensors && fixture.isSensor()) {
				// skip this fixture
				continue;
			}
			// notify the listener to see if we should test this fixture
			if (!this.raycastListener.allow(ray, body, fixture)) continue;
			// get the convex shape
			Convex convex = fixture.getShape();
			// perform the raycast
			if (this.raycastDetector.raycast(ray, max, convex, transform, raycast)) {
				// if the raycast detected a collision then set the new
				// maximum distance
				max = raycast.getDistance();
				// assign the fixture
				result.fixture = fixture;
				// the last raycast will always be the minimum raycast
				// flag that we did get a successful raycast
				found = true;
			}
		}
		
		// we only want to populate the
		// result object if a result was found
		if (found) {
			result.body = body;
			result.raycast = raycast;
		}
		
		return found;
	}
	
	/**
	 * Adds a {@link Body} to the {@link World}.
	 * @param body the {@link Body} to add
	 * @throws NullPointerException if body is null
	 * @throws IllegalArgumentException if body has already been added to this world
	 */
	public void add(Body body) {
		// check for null body
		if (body == null) throw new NullPointerException("Cannot add a null body to the world.");
		// dont allow adding it twice
		if (this.bodies.contains(body)) throw new IllegalArgumentException("Cannot add the same body more than once.");
		// add it to the world
		this.bodies.add(body);
		// add it to the broadphase
		this.broadphaseDetector.add(body);
	}
	
	/**
	 * Adds a {@link Joint} to the {@link World}.
	 * @param joint the {@link Joint} to add
	 * @throws NullPointerException if joint is null
	 * @throws IllegalArgumentException if joint has already been added to this world
	 */
	public void add(Joint joint) {
		// check for null joint
		if (joint == null) throw new NullPointerException("Cannot add a null joint to the world.");
		// dont allow adding it twice
		if (this.joints.contains(joint)) throw new IllegalArgumentException("Cannot add the same joint more than once.");
		// add the joint to the joint list
		this.joints.add(joint);
		// get the associated bodies
		Body body1 = joint.getBody1();
		Body body2 = joint.getBody2();
		// create a joint edge from the first body to the second
		JointEdge jointEdge1 = new JointEdge(body2, joint);
		// add the edge to the body
		body1.joints.add(jointEdge1);
		// create a joint edge from the second body to the first
		JointEdge jointEdge2 = new JointEdge(body1, joint);
		// add the edge to the body
		body2.joints.add(jointEdge2);
	}
	
	/**
	 * Removes the given {@link Body} from the {@link World}.
	 * @param body the {@link Body} to remove
	 * @return boolean true if the body was removed
	 */
	public boolean remove(Body body) {
		// check for null body
		if (body == null) return false;
		// remove the body from the list
		boolean removed = this.bodies.remove(body);
		
		// remove the body from the broadphase
		this.broadphaseDetector.remove(body);
		
		// only remove joints and contacts if the body was removed
		if (removed) {
			// wake up any bodies connected to this body by a joint
			// and destroy the joints and remove the edges
			Iterator<JointEdge> aIterator = body.joints.iterator();
			while (aIterator.hasNext()) {
				// get the joint edge
				JointEdge jointEdge = aIterator.next();
				// remove the joint edge from the given body
				aIterator.remove();
				// get the joint
				Joint joint = jointEdge.getJoint();
				// get the other body
				Body other = jointEdge.getOther();
				// wake up the other body
				other.setAsleep(false);
				// remove the joint edge from the other body
				Iterator<JointEdge> bIterator = other.joints.iterator();
				while (bIterator.hasNext()) {
					// get the joint edge
					JointEdge otherJointEdge = bIterator.next();
					// get the joint
					Joint otherJoint = otherJointEdge.getJoint();
					// are the joints the same object reference
					if (otherJoint == joint) {
						// remove the joint edge
						bIterator.remove();
						// we can break from the loop since there should
						// not be more than one contact edge per joint per body
						break;
					}
				}
				// notify of the destroyed joint
				this.destructionListener.destroyed(joint);
				// remove the joint from the world
				this.joints.remove(joint);
			}
			
			// remove any contacts this body had with any other body
			Iterator<ContactEdge> acIterator = body.contacts.iterator();
			while (acIterator.hasNext()) {
				// get the contact edge
				ContactEdge contactEdge = acIterator.next();
				// remove the contact edge from the given body
				acIterator.remove();
				// get the contact constraint
				ContactConstraint contactConstraint = contactEdge.getContactConstraint();
				// get the other body
				Body other = contactEdge.getOther();
				// wake up the other body
				other.setAsleep(false);
				// remove the contact edge connected from the other body
				// to this body
				Iterator<ContactEdge> iterator = other.contacts.iterator();
				while (iterator.hasNext()) {
					ContactEdge otherContactEdge = iterator.next();
					// get the contact constraint
					ContactConstraint otherContactConstraint = otherContactEdge.getContactConstraint();
					// check if the contact constraint is the same reference
					if (otherContactConstraint == contactConstraint) {
						// remove the contact edge
						iterator.remove();
						// break from the loop since there should only be
						// one contact edge per body pair
						break;
					}
				}
				// remove the contact constraint from the contact manager
				this.contactManager.remove(contactConstraint);
				// loop over the contact points
				Contact[] contacts = contactConstraint.getContacts();
				int size = contacts.length;
				for (int j = 0; j < size; j++) {
					// get the contact
					Contact contact = contacts[j];
					// create a contact point for notification
					ContactPoint contactPoint = new ContactPoint(
													contactConstraint.getBody1(), 
													contactConstraint.getFixture1(), 
													contactConstraint.getBody2(), 
													contactConstraint.getFixture2(),
													contact.isEnabled(),
													contact.getPoint(), 
													contactConstraint.getNormal(), 
													contact.getDepth());
					// call the destruction listener
					this.destructionListener.destroyed(contactPoint);
				}
			}
		}
		
		return removed;
	}
	
	/**
	 * Removes the given {@link Joint} from the {@link World}.
	 * @param joint the {@link Joint} to remove
	 * @return boolean true if the {@link Joint} was removed
	 */
	public boolean remove(Joint joint) {
		// check for null joint
		if (joint == null) return false;
		// remove the joint from the joint list
		boolean removed = this.joints.remove(joint);
		
		// get the involved bodies
		Body body1 = joint.getBody1();
		Body body2 = joint.getBody2();
		
		// see if the given joint was removed
		if (removed) {
			// remove the joint edges from body1
			Iterator<JointEdge> iterator = body1.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			// remove the joint edges from body2
			iterator = body2.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			
			// finally wake both bodies
			body1.setAsleep(false);
			body2.setAsleep(false);
		}
		
		return removed;
	}
	
	/**
	 * Returns true if upon the next time step the contacts must be updated.
	 * @return boolean
	 */
	public boolean isUpdateRequired() {
		return this.updateRequired;
	}
	
	/**
	 * Sets the update required flag.
	 * <p>
	 * Set this flag to true if any of the following conditions have been met:
	 * <ul>
	 * 	<li>If a Body has been added or removed from the World</li>
	 * 	<li>If a Body has been translated or rotated</li>
	 * 	<li>If a Body's state has been manually changed via the Body.setActive(boolean) method</li>
	 * 	<li>If a BodyFixture has been added or removed from a Body</li>
	 * 	<li>If a BodyFixture's sensor flag has been manually changed via the BodyFixture.setSensor(boolean) method</li>
	 * 	<li>If a BodyFixture's filter has been manually changed via the BodyFixture.setFilter(boolean) method</li>
	 * 	<li>If a BodyFixture's restitution or friction coefficient has changed</li>
	 * 	<li>If a BodyFixture's Shape has been translated or rotated</li>
	 * 	<li>If a BodyFixture's Shape has been changed (vertices, radius, etc.)</li>
	 * 	<li>If a Body's type has changed to or from Static (this is caused by the using setMassType(Mass.INFINITE/Mass.NORMAL) method)</li>
	 * 	<li>If a Joint has been added or removed from the World in which the joined bodies should not be allowed to collide</li>
	 * 	<li>If the World's CoefficientMixer has been changed</li>
	 * </ul>
	 * @param flag the flag
	 */
	public void setUpdateRequired(boolean flag) {
		this.updateRequired = flag;
	}
	
	/**
	 * Sets the gravity.
	 * <p>
	 * Setting the gravity vector to the zero vector eliminates gravity.
	 * @param gravity the gravity in meters/second<sup>2</sup>
	 * @throws NullPointerException if gravity is null
	 */
	public void setGravity(Vector2 gravity) {
		if (gravity == null) throw new NullPointerException("The gravity vector cannot be null.");
		this.gravity = gravity;
	}
	
	/**
	 * Returns the gravity.
	 * @return {@link Vector2} the gravity in meters/second<sup>2</sup>
	 */
	public Vector2 getGravity() {
		return this.gravity;
	}

	/**
	 * Sets the bounds of the {@link World}.
	 * @param bounds the bounds; can be null
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}
	
	/**
	 * Returns the bounds of the world.
	 * @return {@link Bounds} the bounds
	 */
	public Bounds getBounds() {
		return this.bounds;
	}
	
	/**
	 * Sets the bounds listener.
	 * @param boundsListener the bounds listener
	 * @throws NullPointerException if boundsListener is null
	 */
	public void setBoundsListener(BoundsListener boundsListener) {
		if (boundsListener == null) throw new NullPointerException("The bounds listener cannot be null.  Create an instance of the BoundsAdapter class to set it to the default.");
		this.boundsListener = boundsListener;
	}
	
	/**
	 * Returns the bounds listener.
	 * @return {@link BoundsListener} the bounds listener
	 */
	public BoundsListener getBoundsListener() {
		return this.boundsListener;
	}
	
	/**
	 * Sets the {@link ContactListener}.
	 * @param contactListener the contact listener
	 * @throws NullPointerException if contactListener is null
	 */
	public void setContactListener(ContactListener contactListener) {
		if (contactListener == null) throw new NullPointerException("The contact listener cannot be null.  Create an instance of the ContactAdapter class to set it to the default.");
		this.contactListener = contactListener;
	}
	
	/**
	 * Returns the contact listener.
	 * @return {@link ContactListener} the contact listener
	 */
	public ContactListener getContactListener() {
		return this.contactListener;
	}
	
	/**
	 * Returns the time of impact listener.
	 * @return {@link TimeOfImpactListener} the time of impact listener
	 */
	public TimeOfImpactListener getTimeOfImpactListener() {
		return this.timeOfImpactListener;
	}
	
	/**
	 * Sets the {@link TimeOfImpactListener}.
	 * @param timeOfImpactListener the time of impact listener
	 * @throws NullPointerException if timeOfImpactListener is null
	 */
	public void setTimeOfImpactListener(TimeOfImpactListener timeOfImpactListener) {
		if (timeOfImpactListener == null) throw new NullPointerException("The time of impact listener cannot be null.  Create an instance of the TimeOfImpactAdapter class to set it to the default.");
		this.timeOfImpactListener = timeOfImpactListener;
	}
	
	/**
	 * Sets the raycast listener.
	 * @param raycastListener the raycast listener
	 * @throws NullPointerException if raycastListener is null
	 * @since 2.0.0
	 */
	public void setRaycastListener(RaycastListener raycastListener) {
		if (raycastListener == null) throw new NullPointerException("The raycast listener cannot be null.  Create an instance of the RaycastAdapter class to set it to the default.");
		this.raycastListener = raycastListener;
	}
	
	/**
	 * Returns the raycast listener.
	 * @return {@link RaycastListener}
	 * @since 2.0.0
	 */
	public RaycastListener getRaycastListener() {
		return this.raycastListener;
	}
	
	/**
	 * Sets the {@link DestructionListener}.
	 * @param destructionListener the {@link DestructionListener}
	 * @throws NullPointerException if destructionListener is null
	 */
	public void setDestructionListener(DestructionListener destructionListener) {
		if (destructionListener == null) throw new NullPointerException("The destruction listener cannot be null.  Create an instance of the DestructionAdapter class to set it to the default.");
		this.destructionListener = destructionListener;
	}
	
	/**
	 * Returns the {@link DestructionListener}.
	 * @return {@link DestructionListener} the destruction listener
	 */
	public DestructionListener getDestructionListener() {
		return this.destructionListener;
	}
	
	/**
	 * Sets the {@link StepListener}.
	 * @param stepListener the {@link StepListener}
	 * @throws NullPointerException if stepListener is null
	 */
	public void setStepListener(StepListener stepListener) {
		if (stepListener == null) throw new NullPointerException("The step listener cannot be null.  Create an instance of the StepAdapter class to set it to the default.");
		this.stepListener = stepListener;
	}
	
	/**
	 * Returns the {@link StepListener}
	 * @return {@link StepListener}
	 */
	public StepListener getStepListener() {
		return this.stepListener;
	}
	
	/**
	 * Sets the broad-phase collision detection algorithm.
	 * <p>
	 * If the given detector is null then the default {@link SapIncremental}
	 * {@link BroadphaseDetector} is set as the current broad phase.
	 * @param broadphaseDetector the broad-phase collision detection algorithm
	 * @throws NullPointerException if broadphaseDetector is null
	 */
	public void setBroadphaseDetector(BroadphaseDetector<Body> broadphaseDetector) {
		if (broadphaseDetector == null) throw new NullPointerException("The broadphase detector cannot be null.");
		// clear the broadphase
		this.broadphaseDetector.clear();
		// set the new broadphase
		this.broadphaseDetector = broadphaseDetector;
		// re-add all bodies to the broadphase
		int size = this.bodies.size();
		for (int i = 0; i < size; i++) {
			this.broadphaseDetector.add(this.bodies.get(i));
		}
	}
	
	/**
	 * Returns the broad-phase collision detection algorithm.
	 * @return {@link BroadphaseDetector} the broad-phase collision detection algorithm
	 */
	public BroadphaseDetector<Body> getBroadphaseDetector() {
		return this.broadphaseDetector;
	}
	
	/**
	 * Sets the narrow-phase collision detection algorithm.
	 * <p>
	 * If the given detector is null then the default {@link Gjk}
	 * {@link NarrowphaseDetector} is set as the current narrow phase.
	 * @param narrowphaseDetector the narrow-phase collision detection algorithm
	 * @throws NullPointerException if narrowphaseDetector is null
	 */
	public void setNarrowphaseDetector(NarrowphaseDetector narrowphaseDetector) {
		if (narrowphaseDetector == null) throw new NullPointerException("The narrowphase detector cannot be null.");
		this.narrowphaseDetector = narrowphaseDetector;
	}
	
	/**
	 * Returns the narrow-phase collision detection algorithm.
	 * @return {@link NarrowphaseDetector} the narrow-phase collision detection algorithm
	 */
	public NarrowphaseDetector getNarrowphaseDetector() {
		return this.narrowphaseDetector;
	}
	
	/**
	 * Sets the manifold solver.
	 * @param manifoldSolver the manifold solver
	 * @throws NullPointerException if manifoldSolver is null
	 */
	public void setManifoldSolver(ManifoldSolver manifoldSolver) {
		if (manifoldSolver == null) throw new NullPointerException("The manifold solver cannot be null.");
		this.manifoldSolver = manifoldSolver;
	}
	
	/**
	 * Returns the manifold solver.
	 * @return {@link ManifoldSolver} the manifold solver
	 */
	public ManifoldSolver getManifoldSolver() {
		return this.manifoldSolver;
	}
	
	/**
	 * Sets the time of impact detector.
	 * @param timeOfImpactDetector the time of impact detector
	 * @throws NullPointerException if timeOfImpactDetector is null
	 * @since 1.2.0
	 */
	public void setTimeOfImpactDetector(TimeOfImpactDetector timeOfImpactDetector) {
		if (timeOfImpactDetector == null) throw new NullPointerException("The time of impact solver cannot be null.");
		this.timeOfImpactDetector = timeOfImpactDetector;
	}
	
	/**
	 * Returns the time of impact detector.
	 * @return {@link TimeOfImpactDetector} the time of impact detector
	 * @since 1.2.0
	 */
	public TimeOfImpactDetector getTimeOfImpactDetector() {
		return this.timeOfImpactDetector;
	}
	
	/**
	 * Sets the raycast detector.
	 * @param raycastDetector the raycast detector
	 * @throws NullPointerException if raycastDetector is null
	 * @since 2.0.0
	 */
	public void setRaycastDetector(RaycastDetector raycastDetector) {
		if (raycastDetector == null) throw new NullPointerException("The raycast detector cannot be null.");
		this.raycastDetector = raycastDetector;
	}
	
	/**
	 * Returns the raycast detector.
	 * @return {@link RaycastDetector} the raycast detector
	 * @since 2.0.0
	 */
	public RaycastDetector getRaycastDetector() {
		return this.raycastDetector;
	}
	
	/**
	 * Sets the collision listener.
	 * @param collisionListener the collision listener
	 * @throws NullPointerException if collisionListener is null
	 */
	public void setCollisionListener(CollisionListener collisionListener) {
		if (collisionListener == null) throw new NullPointerException("The collision listener cannot be null.  Create an instance of the CollisionAdapter class to set it to the default.");
		this.collisionListener = collisionListener;
	}
	
	/**
	 * Returns the collision listener.
	 * @return {@link CollisionListener} the collision listener
	 */
	public CollisionListener getCollisionListener() {
		return this.collisionListener;
	}
	
	/**
	 * Returns the {@link CoefficientMixer}.
	 * @return {@link CoefficientMixer}
	 */
	public CoefficientMixer getCoefficientMixer() {
		return coefficientMixer;
	}
	
	/**
	 * Sets the {@link CoefficientMixer}.
	 * @param coefficientMixer the coefficient mixer
	 * @throws NullPointerException if coefficientMixer is null
	 */
	public void setCoefficientMixer(CoefficientMixer coefficientMixer) {
		if (coefficientMixer == null) throw new NullPointerException("The coefficient mixer cannot be null.  Set it to CoefficientMixer.DEFAULT_MIXER to set it to the defaut.");
		this.coefficientMixer = coefficientMixer;
	}
	
	/**
	 * Returns the {@link ContactManager}.
	 * @return {@link ContactManager}
	 * @since 1.0.2
	 */
	public ContactManager getContactManager() {
		return contactManager;
	}
	
	/**
	 * Sets the {@link ContactManager}.
	 * @param contactManager the contact manager
	 * @throws NullPointerException if contactManager is null
	 * @since 1.0.2
	 */
	public void setContactManager(ContactManager contactManager) {
		// make sure the contact manager is not null
		if (contactManager == null) throw new NullPointerException("The contact manager cannot be null.");
		this.contactManager = contactManager;
	}
	
	/**
	 * Returns the {@link TimeOfImpactSolver}.
	 * @return {@link TimeOfImpactSolver}
	 * @since 2.0.0
	 */
	public TimeOfImpactSolver getTimeOfImpactSolver() {
		return this.timeOfImpactSolver;
	}
	
	/**
	 * Sets the {@link TimeOfImpactSolver}.
	 * @param timeOfImpactSolver the time of impact solver
	 * @throws NullPointerException if timeOfImpactSolver is null
	 * @since 2.0.0
	 */
	public void setTimeOfImpactSolver(TimeOfImpactSolver timeOfImpactSolver) {
		if (timeOfImpactSolver == null) throw new NullPointerException("The time of impact solver cannot be null.");
		this.timeOfImpactSolver = timeOfImpactSolver;
	}
	
	/**
	 * Removes all the joints and bodies from the world.
	 * <p>
	 * This method does <b>not</b> notify of destroyed objects.
	 * <p>
	 * Renamed from clear (3.0.0 and below).
	 * @see #removeAll(boolean)
	 * @since 3.0.1
	 */
	public void removeAll() {
		this.removeAll(false);
	}
	
	/**
	 * Removes all the joints and bodies from the world.
	 * <p>
	 * This method will remove the joints and contacts from all {@link Body}s.
	 * <p>
	 * Renamed from clear (3.0.0 to 1.0.2).
	 * @param notify true if destruction of joints and contacts should be notified of by the {@link DestructionListener}
	 * @since 3.0.1
	 */
	public void removeAll(boolean notify) {
		// loop over the bodies and clear the
		// joints and contacts
		int bsize = this.bodies.size();
		for (int i = 0; i < bsize; i++) {
			// get the body
			Body body = this.bodies.get(i);
			// clear the joint edges
			body.joints.clear();
			// do we need to notify?
			if (notify) {
				// notify of all the destroyed contacts
				Iterator<ContactEdge> aIterator = body.contacts.iterator();
				while (aIterator.hasNext()) {
					// get the contact edge
					ContactEdge contactEdge = aIterator.next();
					// get the other body involved
					Body other = contactEdge.getOther();
					// get the contact constraint
					ContactConstraint contactConstraint = contactEdge.getContactConstraint();
					// find the other contact edge
					Iterator<ContactEdge> bIterator = other.contacts.iterator();
					while (bIterator.hasNext()) {
						// get the contact edge
						ContactEdge otherContactEdge = bIterator.next();
						// get the contact constraint on the edge
						ContactConstraint otherContactConstraint = otherContactEdge.getContactConstraint();
						// are the constraints the same object reference
						if (otherContactConstraint == contactConstraint) {
							// if so then remove it
							bIterator.remove();
							// there should only be one contact edge
							// for each body-body pair
							break;
						}
					}
					// notify of all the contacts on the contact constraint
					Contact[] contacts = contactConstraint.getContacts();
					int csize = contacts.length;
					for (int j = 0; j < csize; j++) {
						Contact contact = contacts[j];
						// create a contact point for notification
						ContactPoint contactPoint = new ContactPoint(
														contactConstraint.getBody1(), 
														contactConstraint.getFixture1(), 
														contactConstraint.getBody2(), 
														contactConstraint.getFixture2(),
														contact.isEnabled(),
														contact.getPoint(), 
														contactConstraint.getNormal(), 
														contact.getDepth());
						// call the destruction listener
						this.destructionListener.destroyed(contactPoint);
					}
				}
			}
			// clear all the contacts
			body.contacts.clear();
			// notify of the destroyed body
			this.destructionListener.destroyed(body);
		}
		// do we need to notify?
		if (notify) {
			// notify of all the destroyed joints
			int jsize = this.joints.size();
			for (int i = 0; i < jsize; i++) {
				// get the joint
				Joint joint = this.joints.get(i);
				// call the destruction listener
				this.destructionListener.destroyed(joint);
			}
		}
		// clear all the broadphase bodies
		this.broadphaseDetector.clear();
		// clear all the joints
		this.joints.clear();
		// clear all the bodies
		this.bodies.clear();
		// clear the contact manager of cached contacts
		this.contactManager.reset();
	}
	
	/**
	 * This is a convenience method for the {@link #removeAll()} method since all joints will be removed
	 * when all bodies are removed.
	 * <p>
	 * This method does not notify of the destroyed contacts, joints, etc.
	 * @see #removeAllBodies(boolean)
	 * @since 3.0.1
	 */
	public void removeAllBodies() {
		this.removeAll();
	}
	
	/**
	 * This is a convenience method for the {@link #removeAll(boolean)} method since all joints will be removed
	 * when all bodies are removed.
	 * @param notify true if destruction of joints and contacts should be notified of by the {@link DestructionListener}
	 * @since 3.0.1
	 */
	public void removeAllBodies(boolean notify) {
		this.removeAll(notify);
	}
	
	/**
	 * Removes all {@link Joint}s from this {@link World}.
	 * <p>
	 * This method does not notify of the joints removed.
	 * @see #removeAllJoints(boolean)
	 * @since 3.0.1
	 */
	public void removeAllJoints() {
		this.removeAllJoints(false);
	}
	
	/**
	 * Removes all {@link Joint}s from this {@link World}.
	 * @param notify true if destruction of joints should be notified of by the {@link DestructionListener}
	 * @since 3.0.1
	 */
	public void removeAllJoints(boolean notify) {
		// get the number of joints
		int jSize = this.joints.size();
		// remove all the joints
		for (int i = 0; i < jSize; i++) {
			// remove the joint from the joint list
			Joint joint = this.joints.get(i);
			
			// get the involved bodies
			Body body1 = joint.getBody1();
			Body body2 = joint.getBody2();
			
			// remove the joint edges from body1
			Iterator<JointEdge> iterator = body1.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			// remove the joint edges from body2
			iterator = body2.joints.iterator();
			while (iterator.hasNext()) {
				// see if this is the edge we want to remove
				JointEdge jointEdge = iterator.next();
				if (jointEdge.getJoint() == joint) {
					// then remove this joint edge
					iterator.remove();
					// joints should only have one joint edge
					// per body
					break;
				}
			}
			
			// finally wake both bodies
			body1.setAsleep(false);
			body2.setAsleep(false);
			
			// notify of the destruction if required
			if (notify) {
				this.destructionListener.destroyed(joint);
			}
		}
		
		// remove all the joints from the joint list
		this.joints.clear();
	}
	
	/**
	 * Returns the number of {@link Body} objects.
	 * @return int the number of bodies
	 */
	public int getBodyCount() {
		return this.bodies.size();
	}
	
	/**
	 * Returns the {@link Body} at the given index.
	 * @param index the index
	 * @return {@link Body}
	 */
	public Body getBody(int index) {
		return this.bodies.get(index);
	}
	
	/**
	 * Returns the number of {@link Joint} objects.
	 * @return int the number of joints
	 */
	public int getJointCount() {
		return this.joints.size();
	}
	
	/**
	 * Returns the {@link Joint} at the given index.
	 * @param index the index
	 * @return {@link Joint}
	 */
	public Joint getJoint(int index) {
		return this.joints.get(index);
	}
	
	/**
	 * Returns the {@link Step} object used to advance
	 * the simulation.
	 * @return {@link Step} the current step object
	 */
	public Step getStep() {
		return this.step;
	}
}
