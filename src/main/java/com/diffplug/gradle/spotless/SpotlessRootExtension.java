package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.diffplug.gradle.spotless.java.JavaExtension;

public class SpotlessRootExtension {
	final Project project;

	public SpotlessRootExtension(Project project) {
		this.project = project;
	}

	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = lineEndings;
	}

	Map<String, CustomExtension> extensions = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Closure<JavaExtension> closure) {
		JavaExtension java = new JavaExtension(this);
		closure.setDelegate(java);
		closure.call();
	}

	/** Configures the special java-specific extension. */
	public void java(Action<JavaExtension> closure) {
		JavaExtension java = new JavaExtension(this);
		closure.execute(java);
	}

	/** Configures a custom extension. */
	public void custom(String name, Closure<CustomExtension> closure) {
		CustomExtension extension = new CustomExtension(name, this);
		closure.setDelegate(extension);
		closure.call();
	}

	/** Configures a custom extension. */
	public void custom(String name, Action<CustomExtension> closure) {
		CustomExtension extension = new CustomExtension(name, this);
		closure.execute(extension);
	}

	void addSubExtension(CustomExtension extension) {
		CustomExtension former = extensions.put(extension.name, extension);
		if (former != null) {
			throw new GradleException("Multiple spotless extensions with name '" + extension.name + "'");
		}
	}
}
