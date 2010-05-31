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
package org.dyn4j.game2d.dynamics.joint;

import org.dyn4j.game2d.dynamics.Body;
import org.dyn4j.game2d.dynamics.Constraint;
import org.dyn4j.game2d.dynamics.Step;

/**
 * Represents constrained motion between two {@link Body}s.
 * @author William Bittle
 */
public abstract class Joint extends Constraint {
	/** Whether the pair of bodies joined together can collide with each other */
	protected boolean collisionAllowed;
	
	/** The user data */
	protected Object userData;
	
	/**
	 * Optional constructor.
	 * <p>
	 * Assumes that the joined bodies do not participate 
	 * in collision detection and resolution.
	 * @param b1 the first {@link Body}
	 * @param b2 the second {@link Body}
	 */
	public Joint(Body b1, Body b2) {
		this(b1, b2, false);
	}
	
	/**
	 * Full constructor.
	 * @param b1 the first {@link Body}
	 * @param b2 the second {@link Body}
	 * @param collisionAllowed true if the joined {@link Body}s can take part in collision detection
	 */
	public Joint(Body b1, Body b2, boolean collisionAllowed) {
		super(b1, b2);
		this.collisionAllowed = collisionAllowed;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append("|")
		.append(this.collisionAllowed);
		return sb.toString();
	}
	
	/**
	 * Performs any initialization of the velocity and position constraints.
	 * @param step the current step
	 */
	public abstract void initializeConstraints(Step step);
	
	/**
	 * Solves the velocity constraints.
	 * @param step the current step
	 */
	public abstract void solveVelocityConstraints(Step step);
	
	/**
	 * Solves the position constraints.
	 * <p>
	 * Returns true if the position constraints were solved.
	 * @return boolean
	 */
	public abstract boolean solvePositionConstraints();
	
	/**
	 * Returns the user data for this {@link Joint}.
	 * @return Object
	 */
	public Object getUserData() {
		return this.userData;
	}
	
	/**
	 * Sets the user data for this {@link Joint}.
	 * @param userData the user data
	 */
	public void setUserData(Object userData) {
		this.userData = userData;
	}
	
	/**
	 * Returns true if collision between the joined {@link Body}s is allowed.
	 * @return boolean
	 */
	public boolean isCollisionAllowed() {
		return this.collisionAllowed;
	}
}
