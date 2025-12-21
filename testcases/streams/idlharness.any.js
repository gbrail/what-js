// META: global=window,worker,shadowrealm-in-window
// META: script=/resources/WebIDLParser.js
// META: script=/resources/idlharness.js
// META: timeout=long

function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
function _catch(body, recover) {
  try {
    var result = body();
  } catch (e) {
    return recover(e);
  }
  if (result && result.then) {
    return result.then(void 0, recover);
  }
  return result;
}
function _continue(value, then) {
  return value && value.then ? value.then(then) : then(value);
}
function _async(f) {
  return function () {
    for (var args = [], i = 0; i < arguments.length; i++) {
      args[i] = arguments[i];
    }
    try {
      return Promise.resolve(f.apply(this, args));
    } catch (e) {
      return Promise.reject(e);
    }
  };
}
idl_test(['streams'], ['dom'], // for AbortSignal
_async(function (idl_array) {
  // Empty try/catches ensure that if something isn't implemented (e.g., readable byte streams, or writable streams)
  // the harness still sets things up correctly. Note that the corresponding interface tests will still fail.

  try {
    new ReadableStream({
      start(c) {
        self.readableStreamDefaultController = c;
      }
    });
  } catch {}
  try {
    new ReadableStream({
      start(c) {
        self.readableByteStreamController = c;
      },
      type: 'bytes'
    });
  } catch {}
  return _continue(_catch(function () {
    var resolvePullCalledPromise;
    var pullCalledPromise = new Promise(resolve => {
      resolvePullCalledPromise = resolve;
    });
    var stream = new ReadableStream({
      pull(c) {
        self.readableStreamByobRequest = c.byobRequest;
        resolvePullCalledPromise();
      },
      type: 'bytes'
    });
    var reader = stream.getReader({
      mode: 'byob'
    });
    reader.read(new Uint8Array(1));
    return _awaitIgnored(pullCalledPromise);
  }, _empty), function () {
    try {
      new WritableStream({
        start(c) {
          self.writableStreamDefaultController = c;
        }
      });
    } catch {}
    try {
      new TransformStream({
        start(c) {
          self.transformStreamDefaultController = c;
        }
      });
    } catch {}
    idl_array.add_objects({
      ReadableStream: ["new ReadableStream()"],
      ReadableStreamDefaultReader: ["(new ReadableStream()).getReader()"],
      ReadableStreamBYOBReader: ["(new ReadableStream({ type: 'bytes' })).getReader({ mode: 'byob' })"],
      ReadableStreamDefaultController: ["self.readableStreamDefaultController"],
      ReadableByteStreamController: ["self.readableByteStreamController"],
      ReadableStreamBYOBRequest: ["self.readableStreamByobRequest"],
      WritableStream: ["new WritableStream()"],
      WritableStreamDefaultWriter: ["(new WritableStream()).getWriter()"],
      WritableStreamDefaultController: ["self.writableStreamDefaultController"],
      TransformStream: ["new TransformStream()"],
      TransformStreamDefaultController: ["self.transformStreamDefaultController"],
      ByteLengthQueuingStrategy: ["new ByteLengthQueuingStrategy({ highWaterMark: 5 })"],
      CountQueuingStrategy: ["new CountQueuingStrategy({ highWaterMark: 5 })"]
    });
  });
}));