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

/**
 * Sub package of the Collision package handling contact manifold generation.
 * <p>
 * Once a penetration vector and depth have been found between two {@link Collidable}s,
 * the next step is to find where the collision occurred.
 * <p>
 * A contact {@link Manifold} is an object representing the surface where the two 
 * {@link Collidable}s are colliding.
 * <p>
 * {@link Manifold}s have been designed to represent two types of surfaces: edge and point.
 * <p>
 * A {@link Manifold} contains a number of {@link ManifoldPoint}s who represent the surface.
 * For 2D this will either be 1 or 2 points.
 * <p>
 * A {@link ManifoldSolver} is used to obtain a collision {@link Manifold} from a 
 * {@link Penetration}.
 * <p>
 * Only one implementation of the {@link ManifoldSolver} is provided: {@link ClippingManifoldSolver}.
 */
package org.dyn4j.game2d.collision.manifold;

import org.dyn4j.game2d.collision.Collidable;
import org.dyn4j.game2d.collision.narrowphase.Penetration;