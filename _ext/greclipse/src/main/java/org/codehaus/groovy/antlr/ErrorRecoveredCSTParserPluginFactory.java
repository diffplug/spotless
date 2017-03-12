/*
 * Copyright 2016 DiffPlug
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
package org.codehaus.groovy.antlr;

import java.util.List;

import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.eclipse.core.GroovyCoreActivator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

/**
 * Overrides original class allowing spotless formatter to handle parser errors.
 * <p>
 * Compiler Parser errors in GrEclipse are treated as warnings, directed to the "standard"
 * error output stream, if less than 'groovy.errors.tolerance' are detected.
 * The 'groovy.errors.tolerance' is set to the fixed value 10 by the GrEclipse patch of
 * the original codehaus groovy, which is convenient within Eclipse editor on 'unfinished' code.
 * For Spotless is can be assumed that the plugin is applied after successful compilation.
 * Hence for all compiler errors, it can be assumed that the eclipse-groovy parser is not
 * able to cope with the code and therefore should not alter it.
 * Furthermore, in the default GrEclipse warnings, the source file name is replaced by
 * "Hello.groovy" (see GroovySnippetParser), which is not helpful for a spotless error report.
 * <p>
 * This patch is used as the least intrusive work-around.
 */
public class ErrorRecoveredCSTParserPluginFactory extends ParserPluginFactory {

	@Override
	public ParserPlugin createParserPlugin() {
		return new ErrorRecoveredCSTParserPlugin(new SpotlessCstReporter());
	}

	private static class SpotlessCstReporter implements ICSTReporter {

		@Override
		public void generatedCST(String fileName, GroovySourceAST ast) {
			//Always called when an AST has been generated. Nothing to do.
		}

		@Override
		public void reportErrors(String fileName, @SuppressWarnings("rawtypes") List errors) {
			ILog log = GroovyCoreActivator.getDefault().getLog();
			for (Object error : errors) {
				log.log(new Status(Status.ERROR, GroovyCoreActivator.PLUGIN_ID, error.toString()));
			}
		}

	};
}
