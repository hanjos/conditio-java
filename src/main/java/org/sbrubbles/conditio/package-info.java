/**
 * Provides an implementation of a simple condition system.
 * <p>
 * The central abstractions are:
 * <ul>
 *   <li>{@linkplain org.sbrubbles.conditio.Scope Scopes}, which hold the main operations;</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Condition Conditions}, which represent unusual situations to handle;</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Handler Handlers}, which decide how to handle conditions; and</li>
 *   <li>{@linkplain org.sbrubbles.conditio.Restart Restarts}, which provide recovery strategies for handlers.</li>
 * </ul>
 * <p>
 * The {@code org.sbrubbles.conditio} package provides the main abstractions and machinery of the condition system.
 * The {@link org.sbrubbles.conditio.restarts} package provides some general use restarts.
 * The {@link org.sbrubbles.conditio.conditions} package provides some general use conditions, which enable
 * alternative handling protocols.
 *
 * @see <a href='https://gigamonkeys.com/book/beyond-exception-handling-conditions-and-restarts.html'>Beyond Exception Handling: Conditions and Restarts</a>
 */
package org.sbrubbles.conditio;