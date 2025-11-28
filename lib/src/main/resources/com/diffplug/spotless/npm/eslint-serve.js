import { ESLint } from 'eslint';

app.post("/eslint/format", async (req, res) => {
	try {
		const format_data = req.body;

		const ESLintOverrideConfig = format_data.eslint_override_config;

		const ESLintOverrideConfigFile = format_data.eslint_override_config_file;

		if (!ESLintOverrideConfig && !ESLintOverrideConfigFile) {
			res.status(400).send("Error while formatting: No config provided");
			return;
		}

		const filePath = format_data.file_path;

		if (!filePath) {
			res.status(400).send("Error while formatting: No file path provided");
			return;
		}

		const ESLintOptions = {
			fix: true,
			useEslintrc: false, // would result in (gradle) cache issues
		};

		if (format_data.ts_config_root_dir) {
			ESLintOptions.baseConfig = {
				parserOptions: {
					tsconfigRootDir: format_data.ts_config_root_dir
				}
			};
		}


		if (ESLintOverrideConfigFile) {
			ESLintOptions.overrideConfigFile = ESLintOverrideConfigFile;
		}
		if (ESLintOverrideConfig) {
			eval("ESLintOptions.overrideConfig = " + ESLintOverrideConfig);
		}

		debugLog("using options: " + JSON.stringify(ESLintOptions));
		debugLog("format input: ", format_data.file_content);

		const eslint = new ESLint(ESLintOptions);


		const lintTextOptions = {
			filePath: filePath,
		}
		debugLog("lintTextOptions", lintTextOptions);

		// LintResult[] // https://eslint.org/docs/latest/developer-guide/nodejs-api#-lintresult-type
		const results = await eslint.lintText(format_data.file_content, lintTextOptions);
		if (results.length !== 1) {
			res.status(500).send("Error while formatting: Unexpected number of results: " + JSON.stringify(results));
			return;
		}
		const result = results[0];
		debugLog("result: " + JSON.stringify(result));
		if (result.fatalErrorCount && result.fatalErrorCount > 0) {
			res.status(500).send("Fatal error while formatting: " + JSON.stringify(result.messages));
			return;
		}
		const formatted = result.output || result.source || format_data.file_content;
		res.set("Content-Type", "text/plain");
		res.send(formatted);
	} catch (err) {
		console.log("error", err);
		res.status(500).send("Error while formatting: " + err);
	}
});
