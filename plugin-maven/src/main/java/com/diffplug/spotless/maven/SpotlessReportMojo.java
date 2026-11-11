/*
 * Copyright 2026 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenMultiPageReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;
import com.diffplug.spotless.maven.report.SpotlessReport;

/**
 * Adds a Spotless page to the Maven site: list the plugin under {@code <reporting>} and run {@code mvn site}.
 * Running {@code mvn spotless:report} writes the same pages without building the whole site.
 * <p>
 * The report covers the same files as {@code spotless:check} and shows the changes {@code spotless:apply} would
 * make, but it never changes a file, never fails the build because of violations, and ignores the up-to-date index.
 * <p>
 * {@code mvn site} configures report goals only from {@code <reporting>} and {@code <build><pluginManagement>}, not
 * from {@code <build><plugins>}, where most projects configure Spotless. So the report copies the plugin-level
 * configuration from there, except for parameters that {@code <reporting>} sets itself. When you run
 * {@code mvn spotless:report} directly, Maven configures it from {@code <build>} only, never from
 * {@code <reporting>}.
 * <p>
 * The report is a {@link MavenMultiPageReport} built against the Doxia 2 API, using only calls that Doxia 1 has
 * too, so it works with {@code maven-site-plugin} 3.12.x (Maven 3.9's default) and 3.20 or later: both call
 * {@link #generate(Sink, SinkFactory, Locale)}, whose signature did not change between the two.
 */
@Mojo(name = AbstractSpotlessMojo.GOAL_REPORT, threadSafe = true)
public class SpotlessReportMojo extends AbstractSpotlessMojo implements MavenMultiPageReport {
	private static final String OUTPUT_NAME = "spotless";

	/** Skips the report. {@code spotless.skip} skips it too. */
	@Parameter(property = "spotless.report.skip", defaultValue = "false")
	private boolean reportSkip;

	/**
	 * Where {@code mvn spotless:report} writes the report. {@code mvn site} always writes it to the site's output
	 * directory instead.
	 */
	@Parameter(defaultValue = "${project.reporting.outputDirectory}", required = true)
	private File outputDirectory;

	/**
	 * The most lines of diff shown for one file. Longer diffs are cut off, with a note saying how many lines are
	 * hidden. {@code 0} or less shows whole diffs.
	 */
	@Parameter(property = "spotless.report.maxDiffLines", defaultValue = "1000")
	private int maxDiffLines;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
	private MojoExecution mojoExecution;

	@Inject
	@Named("basic")
	private ComponentConfigurator configurator;

	/** Filled by {@link #process}; {@code null} until the first format is processed. */
	private SpotlessReport report;
	/** Set by {@code mvn site}, which renders the pages itself, through {@link #generate}. */
	private boolean renderingForSite;
	private boolean usedBuildConfiguration;
	private MavenReportException buildConfigurationError;

	@Override
	protected boolean isGoalSpecificSkip() {
		return reportSkip;
	}

	@Override
	protected boolean usesUpToDateIndex() {
		return false;
	}

	@Override
	protected void process(String name, Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker) {
		if (report == null) {
			report = new SpotlessReport(maxDiffLines);
		}
		SpotlessReport.Format format = report.addFormat(name, formatter);
		for (File file : files) {
			String path = relativePath(file);
			try {
				LintState lintState = calculateLintState(formatter, file);
				byte[] rawBytes = Files.readAllBytes(file.toPath());
				format.addFile(path, lintState, rawBytes, formatter);
			} catch (IOException | RuntimeException e) {
				getLog().warn("Spotless could not check " + path + ": " + e.getMessage());
				getLog().debug(e);
				format.addError(path, e);
			}
		}
	}

	private String relativePath(File file) {
		try {
			return baseDir.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString().replace(File.separatorChar, '/');
		} catch (IllegalArgumentException e) {
			// on Windows, a file on another drive has no relative path
			return file.getAbsolutePath().replace(File.separatorChar, '/');
		}
	}

	@Override
	protected void afterProcessing() throws MojoExecutionException {
		if (report == null) {
			// no formats are configured
			report = new SpotlessReport(maxDiffLines);
		}
		getLog().info(report.summary());
		if (renderingForSite) {
			return;
		}
		try {
			File page = report.writeStandalone(outputDirectory, OUTPUT_NAME);
			getLog().info("Spotless report written to " + page);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to write the Spotless report to " + outputDirectory, e);
		}
	}

	@Override
	public void generate(Sink sink, Locale locale) throws MavenReportException {
		generate(sink, null, locale);
	}

	@Override
	public void generate(Sink sink, SinkFactory sinkFactory, Locale locale) throws MavenReportException {
		renderingForSite = true;
		useBuildConfiguration();
		if (buildConfigurationError != null) {
			throw buildConfigurationError;
		}
		// analyze on every call, because mvn site:run renders the same report instance again for each request
		report = null;
		try {
			execute();
		} catch (MojoExecutionException e) {
			throw new MavenReportException(e.getMessage(), e);
		}
		if (report == null) {
			report = new SpotlessReport(maxDiffLines);
		}
		try {
			report.render(sink, sinkFactory, getReportOutputDirectory(), OUTPUT_NAME);
		} catch (IOException e) {
			throw new MavenReportException("Unable to render the Spotless report", e);
		}
	}

	@Override
	public boolean canGenerateReport() {
		// before deciding, so that a <skip> in the <build> configuration counts too
		useBuildConfiguration();
		return buildConfigurationError != null || !shouldSkip();
	}

	/**
	 * Copies the plugin-level configuration of this plugin in {@code <build><plugins>} onto this goal, except for
	 * parameters that {@code <reporting>} sets itself. {@code mvn site} only applies the configuration from
	 * {@code <reporting>} and {@code <build><pluginManagement>}. Runs once; a failure is kept for {@link #generate}.
	 */
	private void useBuildConfiguration() {
		if (usedBuildConfiguration) {
			return;
		}
		usedBuildConfiguration = true;
		PluginDescriptor plugin = mojoExecution.getMojoDescriptor().getPluginDescriptor();
		String key = plugin.getGroupId() + ":" + plugin.getArtifactId();
		MavenProject project = session.getCurrentProject();
		Plugin buildPlugin = project.getPlugin(key);
		if (buildPlugin == null || !(buildPlugin.getConfiguration() instanceof Xpp3Dom buildConfiguration)) {
			return;
		}
		Set<String> setInReporting = new HashSet<>();
		// maven-site-plugin sets the output directory to the site directory just before canGenerateReport(), keep that
		setInReporting.add("outputDirectory");
		Reporting reporting = project.getModel().getReporting();
		if (reporting != null) {
			for (ReportPlugin reportPlugin : reporting.getPlugins()) {
				if (key.equals(reportPlugin.getGroupId() + ":" + reportPlugin.getArtifactId())) {
					addChildNames(reportPlugin.getConfiguration(), setInReporting);
					for (ReportSet reportSet : reportPlugin.getReportSets()) {
						addChildNames(reportSet.getConfiguration(), setInReporting);
					}
				}
			}
		}
		Set<String> parameters = mojoExecution.getMojoDescriptor().getParameterMap().keySet();
		Xpp3Dom inherited = new Xpp3Dom("configuration");
		for (Xpp3Dom child : buildConfiguration.getChildren()) {
			if (parameters.contains(child.getName()) && !setInReporting.contains(child.getName())) {
				inherited.addChild(new Xpp3Dom(child));
			}
		}
		if (inherited.getChildCount() == 0) {
			return;
		}
		try {
			configurator.configureComponent(this, new XmlPlexusConfiguration(inherited), new PluginParameterExpressionEvaluator(session, mojoExecution), plugin.getClassRealm());
		} catch (ComponentConfigurationException e) {
			buildConfigurationError = new MavenReportException("Unable to use the Spotless configuration from <build> for the report: " + e.getMessage(), e);
		}
	}

	private static void addChildNames(Object configuration, Set<String> names) {
		if (configuration instanceof Xpp3Dom dom) {
			for (Xpp3Dom child : dom.getChildren()) {
				names.add(child.getName());
			}
		}
	}

	/** Deprecated in maven-reporting-api 4 in favor of {@code getOutputPath()}, but it's the only one maven-site-plugin 3.12.x calls. */
	@Override
	@SuppressWarnings("deprecation")
	public String getOutputName() {
		return OUTPUT_NAME;
	}

	@Override
	public String getCategoryName() {
		return MavenReport.CATEGORY_PROJECT_REPORTS;
	}

	@Override
	public String getName(Locale locale) {
		return "Spotless";
	}

	@Override
	public String getDescription(Locale locale) {
		return "Files that do not match the Spotless formatting rules, the changes spotless:apply would make to them, and lint problems.";
	}

	@Override
	public void setReportOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@Override
	public File getReportOutputDirectory() {
		return outputDirectory;
	}

	@Override
	public boolean isExternalReport() {
		return false;
	}
}
