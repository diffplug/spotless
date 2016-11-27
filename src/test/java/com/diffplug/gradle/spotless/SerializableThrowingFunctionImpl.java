package com.diffplug.gradle.spotless;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.base.Throwing;

import java.util.function.Supplier;
import java.util.regex.Pattern;

class SerializableThrowingFunctionImpl {
  enum Identity implements SerializableThrowingFunction<Object, Object> {
    INSTANCE {
      @Override
      public byte[] toBytes() {
        return LazyForwardingEquality.toBytes(this);
      }

      @Override
      public Object apply(Object o) throws Throwable {
        return o;
      }

      private static final long serialVersionUID = 1L;
    }
  }

  static class Constant<T, R>
      implements SerializableThrowingFunction<T, R> {
    private final R constant;

    Constant(R constant) {
      this.constant = constant;
    }

    @Override
    public R apply(T input) throws Throwable {
      return constant;
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }

    private static final long serialVersionUID = 1L;
  }

  static class Replacing implements SerializableThrowingFunction<String, String> {
    private final String substringToReplace;
    private final String substringToReplaceWith;

    Replacing(String substringToReplace, String substringToReplaceWith) {
      this.substringToReplace = substringToReplace;
      this.substringToReplaceWith = substringToReplaceWith;
    }

    @Override
    public String apply(String input) throws Throwable {
      return input.replace(substringToReplace, substringToReplaceWith);
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }
  }

  static class RegexMatching implements SerializableThrowingFunction<String, String> {
    private final Pattern regex;
    private final String substringToReplaceWith;

    RegexMatching(Pattern regex, String substringToReplaceWith) {
      this.regex = regex;
      this.substringToReplaceWith = substringToReplaceWith;
    }

    @Override
    public String apply(String input) throws Throwable {
      return regex.matcher(input).replaceAll("");
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }
  }

  static class Cycle2 implements SerializableThrowingFunction<String, String> {
    private final String a;
    private final String b;

    Cycle2(String a, String b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String apply(String input) throws Throwable {
      return input.equals(a) ? b : a;
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }
  }

  static class Cycle4 implements SerializableThrowingFunction<String, String> {
    private final String a;
    private final String b;
    private final String c;
    private final String d;

    Cycle4(String a, String b, String c, String d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
    }

    @Override
    public String apply(String input) throws Throwable {
      if (a.equals(input)) {
        return b;
      } else if (b.equals(input)) {
        return c;
      } else if (c.equals(input)) {
        return d;
      } else {
        return a;
      }
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }
  }

  enum ConvergeToEmptyString implements SerializableThrowingFunction<String, String> {
    INSTANCE {
      @Override
      public String apply(String input) throws Throwable {
        return input.isEmpty() ? input : input.substring(0, input.length() - 1);
      }

      @Override
      public byte[] toBytes() {
        return LazyForwardingEquality.toBytes(this);
      }
    }
  }

  enum DivergeToEndlessString implements SerializableThrowingFunction<String, String> {
    INSTANCE {
      @Override
      public String apply(String input) throws Throwable {
        return input + " ";
      }

      @Override
      public byte[] toBytes() {
        return LazyForwardingEquality.toBytes(this);
      }
    }
  }

  static class Lazy<T, R> implements SerializableThrowingFunction<T, R> {
    private final transient Supplier<Throwing.Function<T, R>> memoized;

    Lazy(Throwing.Supplier<Throwing.Function<T, R>> formatterSupplier) {
      // wrap the supplier as a regular Supplier (not a Throwing.Supplier)
      Supplier<Throwing.Function<T, R>> rethrowFormatterSupplier = Errors.rethrow().wrap(formatterSupplier);
      // memoize its result
      this.memoized = Suppliers.memoize(rethrowFormatterSupplier);
    }

    @Override
    public R apply(T input) throws Throwable {
      return memoized.get().apply(input);
    }

    @Override
    public byte[] toBytes() {
      return LazyForwardingEquality.toBytes(this);
    }
  }
}
