import express from 'express';
import fs from 'fs';
import shutdownServer from 'http-graceful-shutdown';

// this file will be glued to the top of the specific xy-serve.js file

const app = express();

app.use(express.json({ limit: "50mb" }));

const server = app.listen(
  0,
  "127.0.0.1",
  () =>
    streamOutput(
      server,
      process.argv.slice(2).find(arg => arg.startsWith("--node-server-instance-id="))?.split("=")[1]
      ||  (() => {  throw new Error("Missing --node-server-instance-id argument"); })()
    )
);

app.post("/shutdown", (req, res) => {
  res.status(200).send("Graceful shutdown.");

  setTimeout(async () => {
    try {
      await shutdownServer(server, {
        forceExit: false, // let the event loop clear
        finally: () => debugLog("graceful shutdown finished.")
      })();
    } catch (err) {
      console.error("Error during shutdown:", err);
    }
  }, 200);
});

function streamOutput(server, instanceId) {
  const port = server.address().port;

  debugLog(`Server running on port ${port} for instance ${instanceId}`);

  fs.writeFile("server.port.tmp", String(port), err => {
    if (err) {
      console.log(err);
      return;
    }

    fs.rename(`server.port.tmp`, `server-${instanceId}.port`, err => {
      if (err) {
        console.log(err);
      }
    });
  });
}

function debugLog() {
  if (false) { // set to true for debug log output in node process
    console.log.apply(this, arguments);
  }
}
