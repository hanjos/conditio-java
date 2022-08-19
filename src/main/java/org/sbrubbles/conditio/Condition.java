package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with, but the code that called it
 * might. Conditions are meant to be
 * {@linkplain Scope#signal(Condition, org.sbrubbles.conditio.policies.Policies, Restart[]) signalled},
 * which is how lower-level code communicates what happened.
 * <p>
 * This class is the superclass of all conditions in this library.
 *
 * @see Scope#signal(Condition, org.sbrubbles.conditio.policies.Policies, Restart[])
 */
public class Condition { }
