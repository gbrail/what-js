'use strict';

importScripts('helpers.js');
var orig = createOriginalReadableStream();
postMessage(orig, [orig]);