package org.sbrubbles.conditio;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
  public <T, S extends T> Scope on(Class<T> restartType, Function<S, ?> body) {
    Objects.requireNonNull(restartType, "restartType");

    return on(restartType::isInstance, body);
  }

  public <T, S extends T> Scope on(Predicate<T> matcher, Function<S, ?> body) {
    return on(new Restart.Impl(matcher, body));
  }

  public Scope on(Restart restart) {
    Objects.requireNonNull(restart, "restart");

    this.restarts.add(restart);

    return this;
  }

  public Scope handle(Class<?> signalType, Function<Condition, Restart.Option> body) {
    Objects.requireNonNull(signalType, "signalType");

    return handle(signalType::isInstance, body);
  }

  public Scope handle(Predicate<?> matcher, Function<Condition, Restart.Option> body) {
    return handle(new Handler.Impl(matcher, body));
  }

  public Scope handle(Handler handler) {
    Objects.requireNonNull(handler, "handler");

    this.handlers.add(handler);

    return this;
  }

  public Object signal(Object signal) {
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
    return Collections.unmodifiableList(handlers);
  }

  public List<Restart> getRestarts() {
    return Collections.unmodifiableList(restarts);
  }

  private Restart.Option selectRestartFor(Condition c) throws HandlerNotFoundException {
    assert c != null;

    for (Handler h : getAllHandlers()) {
      if (!h.accepts(c.getSignal())) {
        continue;
      }

      Restart.Option restartOption = h.handle(c);
      if (restartOption == null) {
        continue; // TODO test handler skipping
      }

      return restartOption;
    }

    throw new HandlerNotFoundException(c.getSignal());
  }

  private Object runRestartWith(Object restartOption) throws RestartNotFoundException {
    assert restartOption != null;

    for (Restart r : getAllRestarts()) {
      if (r.matches(restartOption)) {
        return r.run(restartOption);
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