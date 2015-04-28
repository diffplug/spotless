package com.diffplug.gradle.spotless.java;

import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.gradle.spotless.CustomExtension;
import com.diffplug.gradle.spotless.FormatTask;
import com.diffplug.gradle.spotless.SpotlessRootExtension;

public class JavaExtension extends CustomExtension {
	public static final String NAME = "java";

	public JavaExtension(SpotlessRootExtension rootExtension) {
		super(NAME, rootExtension);
	}

	public void licenseHeader(String licenseHeader) {
		stepLazy(LicenseHeaderStep.NAME, () -> new LicenseHeaderStep(licenseHeader)::format);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		stepLazy(LicenseHeaderStep.NAME, () -> new LicenseHeaderStep(getProject().file(licenseHeaderFile))::format);
	}

	public void importOrder(List<String> importOrder) {
		stepLazy(ImportSorterStep.NAME, () -> new ImportSorterStep(importOrder)::format);
	}

	public void importOrderFile(Object importOrderFile) {
		stepLazy(ImportSorterStep.NAME, () -> new ImportSorterStep(getProject().file(importOrderFile))::format);
	}

	public void eclipseFormatFile(Object eclipseFormatFile) {
		stepLazy(EclipseFormatterStep.NAME, () -> EclipseFormatterStep.load(getProject().file(eclipseFormatFile))::format);
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(FormatTask task) throws Exception {
		if (toFormat == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("Must apply the java plugin before you apply the spotless plugin.");
			}
			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union.add(sourceSet.getJava());
			}
			toFormat = union;
		}
		super.setupTask(task);
	}
}
