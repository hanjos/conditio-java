package org.sbrubbles.conditio;

/**
 * Represents an unusual situation, which the running code doesn't know how to deal with, but the code that called it
 * might. Conditions are meant to be
 * {@linkplain Scope#signal(Condition, java.util.Optional, Restart[]) signalled},
 * which is how lower-level code communicates what happened.
 * <p>
 * This class is the supertype of all conditions.
 */
public interface Condition {
}
