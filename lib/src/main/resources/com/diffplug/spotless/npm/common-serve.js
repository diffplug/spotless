// this file will be glued to the top of the specific xy-serve.js file
const debug_serve = false; // set to true for debug log output in node process
const shutdownServer = require("http-graceful-shutdown");
const express = require("express");
const app = express();

app.use(express.json({limit: "50mb"}));

const fs = require("fs");

function debugLog() {
	if (debug_serve) {
		console.log.apply(this, arguments)
	}
}

function getInstanceId() {
	const args = process.argv.slice(2);

	// Look for the --node-server-instance-id option
	let instanceId;

	args.forEach(arg => {
		if (arg.startsWith('--node-server-instance-id=')) {
			instanceId = arg.split('=')[1];
		}
	});

	// throw if instanceId is not set
	if (!instanceId) {
		throw new Error("Missing --node-server-instance-id argument");
	}
	return instanceId;
}

var listener = app.listen(0, "127.0.0.1", () => {
	const instanceId = getInstanceId();
	debugLog("Server running on port " + listener.address().port + " for instance " + instanceId);
	fs.writeFile("server.port.tmp", "" + listener.address().port, function (err) {
		if (err) {
			return console.log(err);
		} else {
			fs.rename("server.port.tmp", `server-${instanceId}.port`, function (err) {
				if (err) {
					return console.log(err);
				}
			}); // try to be as atomic as possible
		}
	});
});
const shutdown = shutdownServer(listener, {
	forceExit: false, // let the event loop clear
	finally: () => debugLog("graceful shutdown finished."),
});

app.post("/shutdown", (req, res) => {
	res.status(200).send("Shutting down");
	setTimeout(async () => {
		try {
			await shutdown();
		} catch (err) {
			console.error("Error during shutdown:", err);
		}
	}, 200);
});
