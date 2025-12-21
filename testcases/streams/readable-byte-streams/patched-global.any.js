// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
'use strict';

// Tests which patch the global environment are kept separate to avoid
// interfering with other tests.
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
promise_test(_async(function (t) {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  var length = 0x4000;
  var buffer = new ArrayBuffer(length);
  var bigArray = new BigUint64Array(buffer, length - 8, 1);
  var read1 = reader.read(new Uint8Array(new ArrayBuffer(0x100)));
  var read2 = reader.read(bigArray);
  var flag = false;
  Object.defineProperty(Object.prototype, 'then', {
    get: t.step_func(() => {
      if (!flag) {
        flag = true;
        assert_equals(controller.byobRequest, null, 'byobRequest should be null after filling both views');
      }
    }),
    configurable: true
  });
  t.add_cleanup(() => {
    delete Object.prototype.then;
  });
  controller.enqueue(new Uint8Array(0x110).fill(0x42));
  assert_true(flag, 'patched then() should be called');

  // The first read() is filled entirely with 0x100 bytes
  return _await(read1, function (result1) {
    assert_false(result1.done, 'result1.done');
    assert_typed_array_equals(result1.value, new Uint8Array(0x100).fill(0x42), 'result1.value');

    // The second read() is filled with the remaining 0x10 bytes
    return _await(read2, function (result2) {
      assert_false(result2.done, 'result2.done');
      assert_equals(result2.value.constructor, BigUint64Array, 'result2.value constructor');
      assert_equals(result2.value.byteOffset, length - 8, 'result2.value byteOffset');
      assert_equals(result2.value.length, 1, 'result2.value length');
      assert_array_equals([...result2.value], [0x42424242_42424242n], 'result2.value contents');
    });
  });
}), 'Patched then() sees byobRequest after filling all pending pull-into descriptors');