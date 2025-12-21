'use strict';

importScripts('/resources/testharness.js', 'helpers.js');
onconnect = evt => {
  var port = evt.source;
  var promise = testMessageEvent(port);
  port.start();
  promise.then(() => port.postMessage('OK')).catch(err => port.postMessage(`BAD: ${err}`));
};