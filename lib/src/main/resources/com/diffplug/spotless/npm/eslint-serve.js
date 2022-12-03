const {ESLint} = require("eslint");

app.post("/eslint/format", async (req, res) => {

	const format_data = req.body;

	const ESLintOverrideConfig = format_data.eslint_override_config;

	const ESLintOverrideConfigFile = format_data.eslint_override_config_file;

	if (!ESLintOverrideConfig && !ESLintOverrideConfigFile) {
		res.status(501).send("Error while formatting: No config provided");
		return;
	}

	const filePath = format_data.file_path;

	if (!filePath) {
		res.status(501).send("Error while formatting: No file path provided");
		return;
	}

	const ESLintOptions = {
		fix: true,
		useEslintrc: false, // would result in (gradle) cache issues
		resolvePluginsRelativeTo: format_data.node_modules_dir
	};


	if (ESLintOverrideConfigFile) {
		ESLintOptions.overrideConfigFile = ESLintOverrideConfigFile;
	}
	if (ESLintOverrideConfig) {
		ESLintOptions.overrideConfig = ESLintOverrideConfig;
	}

	const eslint = new ESLint(ESLintOptions);


	try {
		console.log("using options: " + JSON.stringify(ESLintOptions));
		console.log("format input", format_data.file_content);
		const lintTextOptions = {
			filePath: filePath,
		}
		console.log("lintTextOptions", lintTextOptions);
		// LintResult[] // https://eslint.org/docs/latest/developer-guide/nodejs-api#-lintresult-type
		const results = await eslint.lintText(format_data.file_content, lintTextOptions);
		if (results.length !== 1) {
			res.status(501).send("Error while formatting: Unexpected number of results");
			return;
		}
		const result = results[0];
		console.log("result: " + JSON.stringify(result));
		if (result.fatalErrorCount && result.fatalErrorCount > 0) {
			res.status(501).send("Fatal error while formatting: " + JSON.stringify(result.messages));
			return;
		}
		const formatted = result.output || result.source || format_data.file_content;
		res.set("Content-Type", "text/plain");
		res.send(formatted);
	} catch (err) {
		console.log("error", err);
		res.status(501).send("Error while formatting: " + err);
	}
});
