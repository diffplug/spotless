class FormatAnnotationsTest {

  public @Nullable String s0 = null;

  @Deprecated
  public @Nullable String m0() {}

  @Nullable String s1 = null;

  @Deprecated
  @Nullable String m1() {}

  @Nullable @Deprecated
  String m2() {}

  @Nullable @Regex(2) @Interned String s2 = null;

  @Deprecated
  @Nullable @Regex(2) @Interned String m3() {}

  @Nullable @Deprecated
  @Regex(2) @Interned String m4() {}

  @Nullable
  // a comment
  @Regex(2) @Interned String s3 = null;

  @Nullable // a comment
  @Regex(2) @Interned String s4 = null;

  @Nullable @Regex(2) @Interned
  // a comment
  String s5 = null;

  @Deprecated
  // a comment
  @Nullable @Regex(2) @Interned String m5() {}

  @Deprecated
  @Nullable
  // a comment
  @Regex(2) @Interned String m6() {}

  @Empty
  String e;

  @NonEmpty
  String ne;

  @Localized String localized;
}

@Deprecated
@SuppressWarnings
public
@Interned @MustCall("close") class MyClass1 {
  // No body
}
