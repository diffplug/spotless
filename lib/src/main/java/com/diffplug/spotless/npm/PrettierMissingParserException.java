/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

class PrettierMissingParserException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private static final Map<String, String> EXTENSIONS_TO_PLUGINS;

	static {
		Map<String, String> plugins = new HashMap<>();
		// ---- official plugins
		plugins.put(".php", "@prettier/plugin-php");
		plugins.put(".pug", "@prettier/plugin-pug");
		plugins.put(".rb", "@prettier/plugin-ruby");
		plugins.put(".xml", "@prettier/plugin-xml");

		// ---- community plugins
		// default namings: astro, elm, java, jsonata, prisma, properties, sh, sql, svelte, toml
		plugins.put(".trigger", "prettier-plugin-apex");
		plugins.put(".cls", "prettier-plugin-apex");
		plugins.put(".html.erb", "prettier-plugin-erb");
		Arrays.asList(".glsl",
				".fp",
				".frag",
				".frg",
				".fs",
				".fsh",
				".fshader",
				".geo",
				".geom",
				".glslf",
				".glslv",
				".gs",
				".gshader",
				".rchit",
				".rmiss",
				".shader",
				".tesc",
				".tese",
				".vert",
				".vrx",
				".vsh",
				".vshader").forEach(ext -> plugins.put(ext, "prettier-plugin-glsl"));
		Arrays.asList(".go.html",
				".gohtml",
				".gotmpl",
				".go.tmpl",
				".tmpl",
				".tpl",
				".html.tmpl",
				".html.tpl").forEach(ext -> plugins.put(ext, "prettier-plugin-go-template"));
		plugins.put(".kt", "kotlin");
		plugins.put(".mo", "motoko");
		Arrays.asList(".nginx", ".nginxconf").forEach(ext -> plugins.put(ext, "prettier-plugin-nginx"));
		plugins.put(".sol", "prettier-plugin-solidity");

		EXTENSIONS_TO_PLUGINS = Collections.unmodifiableMap(plugins);
	}

	private final File file;

	public PrettierMissingParserException(@Nonnull File file, Exception cause) {
		super("Prettier could not infer a parser for file '" + file + "'. Maybe you need to include a prettier plugin in devDependencies?\n\n" + recommendPlugin(file), cause);
		this.file = Objects.requireNonNull(file);
	}

	private static String recommendPlugin(File file) {
		String pluginName = guessPlugin(file);
		return "A good candidate for file '" + file + "' is '" + pluginName + "\n"
				+ "See if you can find it on <https://prettier.io/docs/en/plugins.html#official-plugins>\n"
				+ "or search on npmjs.com for a plugin matching that name: "
				+ String.format("<https://www.npmjs.com/search?ranking=popularity&q=%s>", pluginName)
				+ "\n\n"
				+ "For instructions on how to include plugins for prettier in spotless see our documentation:\n"
				+ "- for gradle <https://github.com/diffplug/spotless/tree/main/plugin-gradle#prettier-plugins>\n"
				+ "- for maven <https://github.com/diffplug/spotless/tree/main/plugin-maven#prettier-plugins>";
	}

	private static String guessPlugin(File file) {
		return EXTENSIONS_TO_PLUGINS.entrySet().stream()
				.filter(entry -> file.getName().endsWith(entry.getKey()))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElse("prettier-plugin-" + extension(file));
	}

	public String fileType() {
		return extension(file);
	}

	private static String extension(File file) {
		return file.getName().substring(file.getName().lastIndexOf('.') + 1);
	}
}
