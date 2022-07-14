package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.Function;

public class Scope implements AutoCloseable {
  // the current scope in execution
  private static Scope current = null;

  // this scope's daddy
  private final Scope parent;

  private final List<Handler> handlers;
  private final List<Restart> restarts;

  // private to ensure creation only via #create(), so that current is updated accordingly
  private Scope(Scope parent) {
    this.parent = parent;

    this.handlers = new ArrayList<>();
    this.restarts = new ArrayList<>();
  }

  // === main operations ===
  public <T extends Restart.Option, S extends T> Scope on(Class<S> optionType, Function<T, ?> body) {
    this.restarts.add(new Restart.Impl(optionType, body, this));

    return this;
  }

  public Scope handle(Class<?> signalType, Function<Condition, Restart.Option> body) {
    this.handlers.add(new Handler.Impl(signalType, body, this));

    return this;
  }

  public Object signal(Object signal) throws HandlerNotFoundException, RestartNotFoundException {
    Condition c = new Condition(signal, this);
    Restart.Option restartOption = selectRestartFor(c);
    return runRestartWith(restartOption);
  }

  // === scope search ===
  public Iterable<Handler> getAllHandlers() {
    return () -> new FullSearchIterator<Handler>(this) {
      @Override
      Iterator<Handler> getNextIteratorFrom(Scope scope) {
        return scope.handlers.iterator();
      }
    };
  }

  public Iterable<Restart> getAllRestarts() {
    return () -> new FullSearchIterator<Restart>(this) {
      @Override
      Iterator<Restart> getNextIteratorFrom(Scope scope) {
        return scope.restarts.iterator();
      }
    };
  }

  public List<Handler> getHandlers() {
    return Collections.unmodifiableList(this.handlers);
  }

  public List<Restart> getRestarts() {
    return Collections.unmodifiableList(this.restarts);
  }

  private Restart.Option selectRestartFor(Condition c) throws HandlerNotFoundException {
    assert c != null;

    for (Handler h : getAllHandlers()) {
      if (!h.test(c.getSignal())) {
        continue;
      }

      // TODO Is null a valid restart option? It would work with runRestartWith... What would the semantics be?
      Restart.Option restartOption = h.apply(c);
      if (restartOption == Handler.SKIP) {
        continue;
      }

      return restartOption;
    }

    throw new HandlerNotFoundException(c.getSignal());
  }

  private Object runRestartWith(Restart.Option restartOption) throws RestartNotFoundException {
    for (Restart r : getAllRestarts()) {
      if (r.test(restartOption)) {
        return r.apply(restartOption);
      }
    }

    throw new RestartNotFoundException(restartOption);
  }

  // === scope management ===

  /**
   * Creates and returns a new {@link Scope} instance, keeping track of the current {@code scope} in... well, scope :)
   *
   * @return a new instance of {@link Scope}.
   */
  public static Scope create() {
    current = new Scope(current);

    return current;
  }

  /**
   * The {@link Scope} instance wrapping this one. May be {@code null} if this is the topmost {@code Scope}.
   *
   * @return the {@link Scope} instance wrapping this one, or {@code null} if this is a root scope.
   */
  public Scope getParent() {
    return parent;
  }

  /**
   * If this is the topmost scope in its execution.
   *
   * @return {@code true} if this is the topmost scope.
   */
  public boolean isRoot() {
    return getParent() == null;
  }

  /**
   * Updates the current scope when execution leaves the {@code try} block.
   */
  @Override
  public void close() {
    current = getParent();
  }
}

abstract class FullSearchIterator<T> implements Iterator<T> {
  private Iterator<T> currentIterator;
  private Scope currentScope;

  public FullSearchIterator(Scope currentScope) {
    this.currentScope = Objects.requireNonNull(currentScope, "currentScope");
    this.currentIterator = getNextIteratorFrom(currentScope);
  }

  abstract Iterator<T> getNextIteratorFrom(Scope scope);

  @Override
  public boolean hasNext() {
    if (this.currentIterator.hasNext()) {
      return true;
    }

    do {
      if (this.currentScope.getParent() == null) {
        return false;
      }

      this.currentScope = this.currentScope.getParent();
      this.currentIterator = getNextIteratorFrom(this.currentScope);
    } while (!this.currentIterator.hasNext());

    return true;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    return this.currentIterator.next();
  }
}