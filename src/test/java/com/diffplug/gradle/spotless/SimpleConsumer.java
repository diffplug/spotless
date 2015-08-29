package com.diffplug.gradle.spotless;

public abstract class SimpleConsumer<T> {

	abstract public void accept(T extension);
}
