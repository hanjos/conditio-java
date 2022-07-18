package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition) signalled}.
 * <p>
 * Since signalling doesn't unwind the stack (unless that was the recovery strategy selected), a condition is more
 * general than an exception, and enables strategies and protocols for things other than error handling.
 * <p>
 * This is merely a marker interface, with no fields or methods of its own. Implementations typically hold extra
 * fields and data.
 *
 * @see Scope#signal(Condition)
 */
public interface Condition { /**/ }
