// this file will be glued to the top of the specific xy-server.js file
const GracefulShutdownManager = require("@moebius/http-graceful-shutdown").GracefulShutdownManager;
const express = require("express");
const app = express();

app.use(express.json({ limit: "50mb" }));

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

