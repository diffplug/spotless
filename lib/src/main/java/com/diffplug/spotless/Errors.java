package com.diffplug.spotless;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Copied from `com.diffplug.common.base.Errors`.
 *
 * Converts functions which throw exceptions into functions that don't by passing exceptions to an error policy.
 *
 * Let's say you have a method `void eat(Food food) throws Barf`, and you wanted to pass a `List<Food>` to this
 * method.
 *
 * ```java
 * List<Food> foodOnPlate = Arrays.asList(
 *     cook("salmon"),
 *     cook("asparagus"),
 *     cook("enterotoxin"));
 *
 * // without Errors, we have to write this
 * foodOnPlate.forEach(val -> {
 *     try {
 *         eat(val);
 *     } catch (Barf e) {
 *         // get out the baking soda
 *     }
 * });
 * ```
 *
 * Because the {@link Consumer} required by {@link Iterable#forEach(Consumer)} doesn't allow checked exceptions,
 * and `void eat(Food food) throws Barf` has a checked exception, we can't take advantage of method references.
 *
 * With `Errors`, we can do this succinctly:
 *
 * ```java
 * //                         sweep it under the rug
 * foodOnPlate.forEach(Errors.suppress().wrap(this::eat));
 * //                         save it for later
 * foodOnPlate.forEach(Errors.log().wrap(this::eat));
 * //                         make mom deal with it
 * foodOnPlate.forEach(Errors.rethrow().wrap(this::eat));
 * //                         ask the user deal with it
 * foodOnPlate.forEach(Errors.dialog().wrap(this::eat));
 * ```
 *
 * Errors comes with four built-in error handling policies: {@link #suppress()}, {@link #log()}, {@link #rethrow()}, and {@link #dialog()}.
 * If you don't like their default behaviors, you can change them using {@link Plugins} and {@link DurianPlugins}.
 *
 * You can also create your own error handling policies using {@link #createHandling(Consumer)} and {@link #createRethrowing(Function)}.
 *
 * For a deep-dive into how `Errors` works, see [ErrorsExample.java](https://github.com/diffplug/durian/blob/10631a3480e5491eb6eb6ee06e752d8596914232/test/com/diffplug/common/base/ErrorsExample.java).
 */
public abstract class Errors implements Consumer<Throwable> {
	protected final Consumer<Throwable> handler;

	protected Errors(Consumer<Throwable> error) {
		this.handler = requireNonNull(error);
	}

	/**
	 * Creates an Errors.Handling which passes any exceptions it receives
	 * to the given handler.
	 * <p>
	 * The handler is free to throw a RuntimeException if it wants to. If it always
	 * throws a RuntimeException, then you should instead create an Errors.Rethrowing
	 * using {@link #createRethrowing}.
	 */
	public static Handling createHandling(Consumer<Throwable> handler) {
		return new Handling(handler);
	}

	/**
	 * Creates an Errors.Rethrowing which transforms any exceptions it receives into a RuntimeException
	 * as specified by the given function, and then throws that RuntimeException.
	 * <p>
	 * If that function happens to throw an unchecked error itself, that'll work just fine too.
	 */
	public static Rethrowing createRethrowing(Function<Throwable, RuntimeException> transform) {
		return new Rethrowing(transform);
	}

	/** Suppresses errors entirely. */
	public static Handling suppress() {
		return suppress;
	}

	private static final Handling suppress = createHandling(val -> {});

	/** Rethrows any exceptions as runtime exceptions. */
	public static Rethrowing rethrow() {
		return rethrow;
	}

	private static final Rethrowing rethrow = createRethrowing(Errors::rethrowErrorAndWrapOthersAsRuntime);

	private static RuntimeException rethrowErrorAndWrapOthersAsRuntime(Throwable e) {
		if (e instanceof Error) {
			throw (Error) e;
		} else {
			return Errors.asRuntime(e);
		}
	}

	private static final Logger logger = Logger.getLogger(Errors.class.getName());

	/**
	 * Logs any exceptions.
	 * <p>
	 * By default, log() calls Throwable.printStackTrace(). To modify this behavior
	 * in your application, call DurianPlugins.set(Errors.Plugins.Log.class, error -> myCustomLog(error));
	 * 
	 * @see DurianPlugins
	 * @see Errors.Plugins.OnErrorThrowAssertion
	 */
	@SuppressFBWarnings(value = "LI_LAZY_INIT_STATIC", justification = "This race condition is fine, as explained in the comment below.")
	public static Handling log() {
		if (log == null) {
			// There is an acceptable race condition here - log might get set multiple times.
			// This would happen if multiple threads called log() at the same time
			// during initialization, and this is likely to actually happen in practice.
			// 
			// Because DurianPlugins guarantees that its methods will have the exact same
			// return value for the duration of the library's runtime existence, the only
			// adverse symptom of this race condition is that there will temporarily be
			// multiple instances of Errors which are wrapping the same Consumer<Throwable>.
			//
			// It is important for this method to be fast, so it's better to accept
			// that suppress() might return different Errors instances which are wrapping
			// the same actual Consumer<Throwable>, rather than to incur the cost of some
			// type of synchronization.
			log = createHandling(error -> logger.log(Level.WARNING, error.getMessage(), error));
		}
		return log;
	}

	private static Handling log;

	/** Passes the given error to this Errors. */
	@Override
	public void accept(Throwable error) {
		requireNonNull(error);
		handler.accept(error);
	}

	/** Converts this {@code Consumer<Throwable>} to a {@code Consumer<Optional<Throwable>>}. */
	public Consumer<Optional<Throwable>> asTerminal() {
		return errorOpt -> {
			if (errorOpt.isPresent()) {
				accept(errorOpt.get());
			}
		};
	}

	/** Attempts to run the given runnable. */
	public void run(Throwing.Runnable runnable) {
		wrap(runnable).run();
	}

	/** Returns a Runnable whose exceptions are handled by this Errors. */
	public Runnable wrap(Throwing.Runnable runnable) {
		requireNonNull(runnable);
		return () -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				handler.accept(e);
			}
		};
	}

	/** Returns a Consumer whose exceptions are handled by this Errors. */
	public <T> Consumer<T> wrap(Throwing.Consumer<T> consumer) {
		requireNonNull(consumer);
		return val -> {
			try {
				consumer.accept(val);
			} catch (Throwable e) {
				handler.accept(e);
			}
		};
	}

	/**
	 * An {@link Errors} which is free to rethrow the exception, but it might not.
	 * 
	 * If we want to wrap a method with a return value, since the handler might
	 * not throw an exception, we need a default value to return.
	 */
	public static class Handling extends Errors {
		protected Handling(Consumer<Throwable> error) {
			super(error);
		}

		/** Attempts to call {@code supplier} and returns {@code onFailure} if an exception is thrown. */
		public <T> T getWithDefault(Throwing.Supplier<T> supplier, @Nullable T onFailure) {
			return wrapWithDefault(supplier, onFailure).get();
		}

		/** Returns a Supplier which wraps {@code supplier} and returns {@code onFailure} if an exception is thrown. */
		public <T> Supplier<T> wrapWithDefault(Throwing.Supplier<T> supplier, @Nullable T onFailure) {
			requireNonNull(supplier);
			return () -> {
				try {
					return supplier.get();
				} catch (Throwable e) {
					handler.accept(e);
					return onFailure;
				}
			};
		}

		/**
		 * Returns a Function which wraps {@code function} and returns {@code onFailure} if an exception is thrown.
		 *
		 * If you are getting an error about {@code the method wrapWithDefault is ambiguous}, use
		 * {@link #wrapFunctionWithDefault(com.diffplug.common.base.Throwing.Function)} or
		 * {@link #wrapPredicateWithDefault(com.diffplug.common.base.Throwing.Predicate).
		 */
		public <T, R> Function<T, R> wrapWithDefault(Throwing.Function<T, R> function, @Nullable R onFailure) {
			return wrapFunctionWithDefault(function, onFailure);
		}

		/**
		 * Returns a Predicate which wraps {@code predicate} and returns {@code onFailure} if an exception is thrown.
		 *
		 * If you are getting an error about {@code the method wrapWithDefault is ambiguous}, use
		 * {@link #wrapFunctionWithDefault(com.diffplug.common.base.Throwing.Function)} or
		 * {@link #wrapPredicateWithDefault(com.diffplug.common.base.Throwing.Predicate).
		 */
		public <T> Predicate<T> wrapWithDefault(Throwing.Predicate<T> predicate, boolean onFailure) {
			return wrapPredicateWithDefault(predicate, onFailure);
		}

		/** Returns a Function which wraps {@code function} and returns {@code onFailure} if an exception is thrown. */
		public <T, R> Function<T, R> wrapFunctionWithDefault(Throwing.Function<T, R> function, @Nullable R onFailure) {
			requireNonNull(function);
			return input -> {
				try {
					return function.apply(input);
				} catch (Throwable e) {
					handler.accept(e);
					return onFailure;
				}
			};
		}

		/** Returns a Predicate which wraps {@code predicate} and returns {@code onFailure} if an exception is thrown. */
		public <T> Predicate<T> wrapPredicateWithDefault(Throwing.Predicate<T> predicate, boolean onFailure) {
			requireNonNull(predicate);
			return input -> {
				try {
					return predicate.test(input);
				} catch (Throwable e) {
					handler.accept(e);
					return onFailure;
				}
			};
		}
	}

	/**
	 * An {@link Errors} which is guaranteed to always throw a RuntimeException.
	 * 
	 * If we want to wrap a method with a return value, it's pointless to specify
	 * a default value because if the wrapped method fails, a RuntimeException is
	 * guaranteed to throw.
	 */
	public static class Rethrowing extends Errors {
		private final Function<Throwable, RuntimeException> transform;

		protected Rethrowing(Function<Throwable, RuntimeException> transform) {
			super(error -> {
				throw transform.apply(error);
			});
			this.transform = requireNonNull(transform);
		}

		/** Attempts to call {@code supplier} and rethrows any exceptions as unchecked exceptions. */
		public <T> T get(Throwing.Supplier<T> supplier) {
			return wrap(supplier).get();
		}

		/** Returns a Supplier which wraps {@code supplier} and rethrows any exceptions as unchecked exceptions. */
		public <T> Supplier<T> wrap(Throwing.Supplier<T> supplier) {
			requireNonNull(supplier);
			return () -> {
				try {
					return supplier.get();
				} catch (Throwable e) {
					throw transform.apply(e);
				}
			};
		}

		/**
		 * Returns a Function which wraps {@code function} and rethrows any exceptions as unchecked exceptions.
		 * <p>
		 * If you are getting an error about {@code the method wrap is ambiguous}, use
		 * {@link #wrapFunction(com.diffplug.common.base.Throwing.Function)} or
		 * {@link #wrapPredicate(com.diffplug.common.base.Throwing.Predicate).
		 * */
		public <T, R> Function<T, R> wrap(Throwing.Function<T, R> function) {
			return wrapFunction(function);
		}

		/**
		 * Returns a Predicate which wraps {@code predicate} and rethrows any exceptions as unchecked exceptions.
		 * <p>
		 * If you are getting an error about {@code the method wrap is ambiguous}, use
		 * {@link #wrapFunction(com.diffplug.common.base.Throwing.Function)} or
		 * {@link #wrapPredicate(com.diffplug.common.base.Throwing.Predicate).
		 * */
		public <T> Predicate<T> wrap(Throwing.Predicate<T> predicate) {
			return wrapPredicate(predicate);
		}

		/** Returns a Function which wraps {@code function} and rethrows any exceptions as unchecked exceptions. */
		public <T, R> Function<T, R> wrapFunction(Throwing.Function<T, R> function) {
			requireNonNull(function);
			return arg -> {
				try {
					return function.apply(arg);
				} catch (Throwable e) {
					throw transform.apply(e);
				}
			};
		}

		/** Returns a Predicate which wraps {@code predicate} and rethrows any exceptions as unchecked exceptions. */
		public <T> Predicate<T> wrapPredicate(Throwing.Predicate<T> predicate) {
			requireNonNull(predicate);
			return arg -> {
				try {
					return predicate.test(arg);
				} catch (Throwable e) {
					throw transform.apply(e); // 1 855 548 2505
				}
			};
		}
	}

	/**
	 * Casts or wraps the given exception to be a RuntimeException.
	 * 
	 * If the input exception is a RuntimeException, it is simply
	 * cast and returned.  Otherwise, it wrapped in a
	 * {@link WrappedAsRuntimeException} and returned.
	 */
	public static RuntimeException asRuntime(Throwable e) {
		requireNonNull(e);
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		} else {
			return new WrappedAsRuntimeException(e);
		}
	}

	/** A RuntimeException specifically for the purpose of wrapping non-runtime Throwables as RuntimeExceptions. */
	public static class WrappedAsRuntimeException extends RuntimeException {
		private static final long serialVersionUID = -912202209702586994L;

		public WrappedAsRuntimeException(Throwable e) {
			super(e);
		}
	}
}
