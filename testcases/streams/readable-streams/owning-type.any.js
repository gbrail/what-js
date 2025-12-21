// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
// META: script=../resources/rs-utils.js
'use strict';

function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
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
test(() => {
  new ReadableStream({
    type: 'owning'
  }); // ReadableStream constructed with 'owning' type
}, 'ReadableStream can be constructed with owning type');
test(() => {
  var startCalled = false;
  var source = {
    start(controller) {
      assert_equals(this, source, 'source is this during start');
      assert_true(controller instanceof ReadableStreamDefaultController, 'default controller');
      startCalled = true;
    },
    type: 'owning'
  };
  new ReadableStream(source);
  assert_true(startCalled);
}, 'ReadableStream of type owning should call start with a ReadableStreamDefaultController');
test(() => {
  var startCalled = false;
  var source = {
    start(controller) {
      controller.enqueue("a", {
        transfer: []
      });
      controller.enqueue("a", {
        transfer: undefined
      });
      startCalled = true;
    },
    type: 'owning'
  };
  new ReadableStream(source);
  assert_true(startCalled);
}, 'ReadableStream should be able to call enqueue with an empty transfer list');
test(() => {
  var startCalled = false;
  var uint8Array = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8]);
  var buffer = uint8Array.buffer;
  var source = {
    start(controller) {
      startCalled = true;
      assert_throws_js(TypeError, () => {
        controller.enqueue(buffer, {
          transfer: [buffer]
        });
      }, "transfer list is not empty");
    }
  };
  new ReadableStream(source);
  assert_true(startCalled);
  startCalled = false;
  source = {
    start(controller) {
      startCalled = true;
      assert_throws_js(TypeError, () => {
        controller.enqueue(buffer, {
          get transfer() {
            throw new TypeError();
          }
        });
      }, "getter throws");
    }
  };
  new ReadableStream(source);
  assert_true(startCalled);
}, 'ReadableStream should check transfer parameter');
promise_test(_async(function () {
  var uint8Array = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8]);
  var buffer = uint8Array.buffer;
  buffer.test = 1;
  var source = {
    start(controller) {
      assert_equals(buffer.byteLength, 8);
      controller.enqueue(buffer, {
        transfer: [buffer]
      });
      assert_equals(buffer.byteLength, 0);
      assert_equals(buffer.test, 1);
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var reader = stream.getReader();
  return _await(reader.read(), function (chunk) {
    assert_not_equals(chunk.value, buffer);
    assert_equals(chunk.value.byteLength, 8);
    assert_equals(chunk.value.test, undefined);
  });
}), 'ReadableStream of type owning should transfer enqueued chunks');