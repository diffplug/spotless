const tsfmt = require("typescript-formatter");

app.post("/tsfmt/format", (req, res) => {
	tsfmt.processString("spotless-format-string.ts", req.body.file_content, req.body.config_options).then(resultMap => {
		if (resultMap.error !== undefined && resultMap.error) {
			res.status(400).send(resultMap.message);
			return;
		}
		res.set("Content-Type", "text/plain");
		res.send(resultMap.dest);
	}).catch(reason => {
		res.status(500).send(reason);
	});
});
