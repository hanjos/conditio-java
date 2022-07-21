package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with. Conditions are meant to
 * be {@linkplain Scope#signal(Condition) signalled}.
 * <p>
 * This is a marker interface, with no fields or methods of its own. Implementations typically hold extra data.
 *
 * @see Scope#signal(Condition)
 */
public interface Condition { /**/ }
