'use strict';

importScripts('/resources/testharness.js', 'helpers.js');
var promise = testMessageEvent(self);
promise.then(() => postMessage('OK')).catch(err => postMessage(`BAD: ${err}`));