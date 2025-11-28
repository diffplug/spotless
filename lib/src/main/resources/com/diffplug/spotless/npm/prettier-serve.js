import prettier from 'prettier';

app.post("/prettier/config-options", (req, res) => {
	const config_data = req.body;
	const prettier_config_path = config_data.prettier_config_path;
	const prettier_config_options = config_data.prettier_config_options || {};

	if (prettier_config_path) {
		prettier
			.resolveConfig(undefined, { config: prettier_config_path })
			.then(options => {
				const mergedConfigOptions = mergeConfigOptions(options, prettier_config_options);
				res.set("Content-Type", "application/json")
				res.json(mergedConfigOptions);
			})
			.catch(reason => res.status(501).send("Exception while resolving config_file_path: " + reason));
		return;
	}
	res.set("Content-Type", "application/json")
	res.json(prettier_config_options);
});

app.post("/prettier/format", async (req, res) => {
	const format_data = req.body;

	let formatted_file_content = "";
	try {
		formatted_file_content = await prettierFormat(format_data.file_content, format_data.config_options);
	} catch(err) {
		res.status(500).send("Error while formatting: " + err);
		return;
	}
	res.set("Content-Type", "text/plain");
	res.send(formatted_file_content);
});

const prettierFormat = async function(file_content, config_options) {
	const result = prettier.format(file_content, config_options);

	// Check if result is a Promise (version 3.0.0 and above)
	if (typeof result.then === 'function') {
		return result;
	}

	// If it's not a Promise (meaning it's a string), wrap it in a Promise (< 3.0.0)
	return Promise.resolve(result);
}


const mergeConfigOptions = function(resolved_config_options, config_options) {
	if (resolved_config_options !== undefined && config_options !== undefined) {
		return extend(resolved_config_options, config_options);
	}
	if (resolved_config_options === undefined) {
		return config_options;
	}
	if (config_options === undefined) {
		return resolved_config_options;
	}
};

const extend = function() {
	// Variables
	const extended = {};
	let i = 0;
	const length = arguments.length;

	// Merge the object into the extended object
	const merge = function (obj) {
		for (const prop in obj) {
			if (Object.prototype.hasOwnProperty.call(obj, prop)) {
				extended[prop] = obj[prop];
			}
		}
	};

	// Loop through each object and conduct a merge
	for (; i < length; i++) {
		const obj = arguments[i];
		merge(obj);
	}
	return extended;
};
