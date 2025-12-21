'use strict';

importScripts('helpers.js');
onconnect = msg => {
  var port = msg.source;
  var orig = createOriginalReadableStream();
  try {
    port.postMessage(orig, [orig]);
  } catch (e) {
    port.postMessage(e.message);
  }
};