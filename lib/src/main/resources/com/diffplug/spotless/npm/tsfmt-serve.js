const GracefulShutdownManager = require("@moebius/http-graceful-shutdown").GracefulShutdownManager;
const express = require("express");
const app = express();
app.use(express.json({ limit: "50mb" }));

const tsfmt = require("typescript-formatter");

const fs = require("fs");

var listener = app.listen(0, "127.0.0.1", () => {
	console.log("Server running on port " + listener.address().port);
	fs.writeFile("server.port.tmp", "" + listener.address().port, function(err) {
		if (err) {
			return console.log(err);
		} else {
			fs.rename("server.port.tmp", "server.port", function(err) {
				if (err) {
					return console.log(err);
				}
			}); // try to be as atomic as possible
		}
	});
});

const shutdownManager = new GracefulShutdownManager(listener);

app.post("/shutdown", (req, res) => {
	res.status(200).send("Shutting down");
	setTimeout(function() {
		shutdownManager.terminate(() => console.log("graceful shutdown finished."));
	}, 200);
});

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
	});
});
