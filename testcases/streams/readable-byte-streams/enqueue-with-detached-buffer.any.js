// META: global=window,worker,shadowrealm

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
promise_test(_async(function (t) {
  var error = new Error('cannot proceed');
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(controller => {
      var buffer = controller.byobRequest.view.buffer;
      // Detach the buffer.
      structuredClone(buffer, {
        transfer: [buffer]
      });

      // Try to enqueue with a new buffer.
      assert_throws_js(TypeError, () => controller.enqueue(new Uint8Array([42])));

      // If we got here the test passed.
      controller.error(error);
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_exactly(t, error, reader.read(new Uint8Array(1))));
}), 'enqueue after detaching byobRequest.view.buffer should throw');