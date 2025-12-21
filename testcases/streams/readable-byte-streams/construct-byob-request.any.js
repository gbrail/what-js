// META: global=window,worker,shadowrealm
// META: script=../resources/rs-utils.js
'use strict';

// Prior to whatwg/stream#870 it was possible to construct a ReadableStreamBYOBRequest directly. This made it possible
// to construct requests that were out-of-sync with the state of the ReadableStream. They could then be used to call
// internal operations, resulting in asserts or bad behaviour. This file contains regression tests for the change.
function getRealByteStreamController() {
  var controller;
  new ReadableStream({
    start(c) {
      controller = c;
    },
    type: 'bytes'
  });
  return controller;
}

// Create an object pretending to have prototype |prototype|, of type |type|. |type| is one of "undefined", "null",
// "fake", or "real". "real" will call the realObjectCreator function to get a real instance of the object.
function createDummyObject(prototype, type, realObjectCreator) {
  switch (type) {
    case 'undefined':
      return undefined;
    case 'null':
      return null;
    case 'fake':
      return Object.create(prototype);
    case 'real':
      return realObjectCreator();
  }
  throw new Error('not reached');
}
var dummyTypes = ['undefined', 'null', 'fake', 'real'];
var _loop = function () {
  var controller = createDummyObject(ReadableByteStreamController.prototype, controllerType, getRealByteStreamController);
  var _loop2 = function () {
    var view = createDummyObject(Uint8Array.prototype, viewType, () => new Uint8Array(16));
    test(() => {
      assert_throws_js(TypeError, () => new ReadableStreamBYOBRequest(controller, view), 'constructor should throw');
    }, `ReadableStreamBYOBRequest constructor should throw when passed a ${controllerType} ` + `ReadableByteStreamController and a ${viewType} view`);
  };
  for (var viewType of dummyTypes) {
    _loop2();
  }
};
for (var controllerType of dummyTypes) {
  _loop();
}