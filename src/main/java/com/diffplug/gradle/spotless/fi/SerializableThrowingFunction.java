package com.diffplug.gradle.spotless.fi;

import com.diffplug.common.base.Throwing;

import java.io.Serializable;

public interface SerializableThrowingFunction<T, R> extends Throwing.Function<T, R>, Serializable {}
