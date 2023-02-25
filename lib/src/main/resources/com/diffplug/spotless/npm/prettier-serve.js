const prettier = require("prettier");

app.post("/prettier/config-options", (req, res) => {
	var config_data = req.body;
	var prettier_config_path = config_data.prettier_config_path;
	var prettier_config_options = config_data.prettier_config_options || {};

	if (prettier_config_path) {
		prettier
			.resolveConfig(undefined, { config: prettier_config_path })
			.then(options => {
				var mergedConfigOptions = mergeConfigOptions(options, prettier_config_options);
				res.set("Content-Type", "application/json")
				res.json(mergedConfigOptions);
			})
			.catch(reason => res.status(501).send("Exception while resolving config_file_path: " + reason));
		return;
	}
	res.set("Content-Type", "application/json")
	res.json(prettier_config_options);
});

app.post("/prettier/format", (req, res) => {
	var format_data = req.body;

	var formatted_file_content = "";
	try {
		formatted_file_content = prettier.format(format_data.file_content, format_data.config_options);
	} catch(err) {
		res.status(500).send("Error while formatting: " + err);
		return;
	}
	res.set("Content-Type", "text/plain");
	res.send(formatted_file_content);
});

var mergeConfigOptions = function(resolved_config_options, config_options) {
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

var extend = function() {
	// Variables
	var extended = {};
	var i = 0;
	var length = arguments.length;

	// Merge the object into the extended object
	var merge = function(obj) {
		for (var prop in obj) {
			if (Object.prototype.hasOwnProperty.call(obj, prop)) {
				extended[prop] = obj[prop];
			}
		}
	};

	// Loop through each object and conduct a merge
	for (; i < length; i++) {
		var obj = arguments[i];
		merge(obj);
	}

	return extended;
};
