/**
 * Holds an implementation of a simple condition system. The core abstractions are:
 * <ul>
 *   <li>{@linkplain org.sbrubbles.conditio.Scope Scopes}, that implement the main operations;</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Condition Conditions}, that represent unusual situations to handle;</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Handler Handlers}, that decide how to handle conditions; and</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Restart Restarts}, that define recovery strategies for handlers.</li>
 * </ul>
 * <p>
 * This package provides the core abstractions and machinery.
 * The {@link org.sbrubbles.conditio.restarts} package supplies some general use restarts and restart options.
 * The {@link org.sbrubbles.conditio.handlers} package offers some utility methods for handlers.
 * The {@link org.sbrubbles.conditio.policies} package has some policies for special cases.
 */
package org.sbrubbles.conditio;