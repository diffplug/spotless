package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.GradleException;

import com.diffplug.gradle.spotless.java.JavaExtension;

public class SpotlessRootExtension {
	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = lineEndings;
	}

	Map<String, SpotlessExtension> extensions = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Closure<JavaExtension> closure) {
		JavaExtension java = new JavaExtension();
		closure.call(java);
		addExtension(java);
	}

	/** Configures a custom extension. */
	public void custom(String name, Closure<SpotlessExtension> closure) {
		SpotlessExtension extension = new SpotlessExtension(name);
		closure.call(extension);
		addExtension(extension);
	}

	private void addExtension(SpotlessExtension extension) {
		SpotlessExtension former = extensions.put(extension.name, extension);
		if (former != null) {
			throw new GradleException("Multiple spotless extensions with name '" + extension.name + "'");
		}
	}
}
