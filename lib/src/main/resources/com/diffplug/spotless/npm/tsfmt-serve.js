const tsfmt = require("typescript-formatter");

app.post("/tsfmt/format", (req, res) => {
	var format_data = req.body;
	tsfmt.processString("spotless-format-string.ts", format_data.file_content, format_data.config_options).then(resultMap => {
		/*
        export interface ResultMap {
            [fileName: string]: Result;
        }

        export interface Result {
            fileName: string;
            settings: ts.FormatCodeSettings | null;
            message: string;
            error: boolean;
            src: string;
            dest: string;
        }
        */
		// result contains 'message' (String), 'error' (boolean), 'dest' (String) => formatted
		if (resultMap.error !== undefined && resultMap.error) {
			res.status(400).send(resultmap.message);
			return;
		}
		res.set("Content-Type", "text/plain");
		res.send(resultMap.dest);
	}).catch(reason => {
		res.status(500).send(reason);
	});
});
