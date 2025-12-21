// META: global=window,worker,shadowrealm
'use strict';

function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
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
function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
}
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  var memory = new WebAssembly.Memory({
    initial: 1
  });
  var view = new Uint8Array(memory.buffer, 0, 1);
  return _awaitIgnored(promise_rejects_js(t, TypeError, reader.read(view)));
}), 'ReadableStream with byte source: read() with a non-transferable buffer');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  var memory = new WebAssembly.Memory({
    initial: 1
  });
  var view = new Uint8Array(memory.buffer, 0, 1);
  return _awaitIgnored(promise_rejects_js(t, TypeError, reader.read(view, {
    min: 1
  })));
}), 'ReadableStream with byte source: fill() with a non-transferable buffer');
test(t => {
  var controller;
  var rs = new ReadableStream({
    start(c) {
      controller = c;
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var memory = new WebAssembly.Memory({
    initial: 1
  });
  var view = new Uint8Array(memory.buffer, 0, 1);
  assert_throws_js(TypeError, () => controller.enqueue(view));
}, 'ReadableStream with byte source: enqueue() with a non-transferable buffer');
promise_test(_async(function (t) {
  var byobRequest;
  var resolvePullCalledPromise;
  var pullCalledPromise = new Promise(resolve => {
    resolvePullCalledPromise = resolve;
  });
  var rs = new ReadableStream({
    pull(controller) {
      byobRequest = controller.byobRequest;
      resolvePullCalledPromise();
    },
    type: 'bytes'
  });
  var memory = new WebAssembly.Memory({
    initial: 1
  });
  // Make sure the backing buffers of both views have the same length
  var byobView = new Uint8Array(new ArrayBuffer(memory.buffer.byteLength), 0, 1);
  var newView = new Uint8Array(memory.buffer, byobView.byteOffset, byobView.byteLength);
  var reader = rs.getReader({
    mode: 'byob'
  });
  reader.read(byobView).then(t.unreached_func('read() should not resolve'), t.unreached_func('read() should not reject'));
  return _await(pullCalledPromise, function () {
    assert_throws_js(TypeError, () => byobRequest.respondWithNewView(newView));
  });
}), 'ReadableStream with byte source: respondWithNewView() with a non-transferable buffer');