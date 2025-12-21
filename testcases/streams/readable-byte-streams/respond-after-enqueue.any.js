// META: global=window,worker,shadowrealm

'use strict';

// Repro for Blink bug https://crbug.com/1255762.
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
promise_test(_async(function () {
  var rs = new ReadableStream({
    type: 'bytes',
    autoAllocateChunkSize: 10,
    pull(controller) {
      controller.enqueue(new Uint8Array([1, 2, 3]));
      controller.byobRequest.respond(10);
    }
  });
  var reader = rs.getReader();
  return _await(reader.read(), function ({
    value,
    done
  }) {
    assert_false(done, 'done should not be true');
    assert_array_equals(value, [1, 2, 3], 'value should be 3 bytes');
  });
}), 'byobRequest.respond() after enqueue() should not crash');
promise_test(_async(function () {
  var rs = new ReadableStream({
    type: 'bytes',
    autoAllocateChunkSize: 10,
    pull(controller) {
      var byobRequest = controller.byobRequest;
      controller.enqueue(new Uint8Array([1, 2, 3]));
      byobRequest.respond(10);
    }
  });
  var reader = rs.getReader();
  return _await(reader.read(), function ({
    value,
    done
  }) {
    assert_false(done, 'done should not be true');
    assert_array_equals(value, [1, 2, 3], 'value should be 3 bytes');
  });
}), 'byobRequest.respond() with cached byobRequest after enqueue() should not crash');
promise_test(_async(function () {
  var rs = new ReadableStream({
    type: 'bytes',
    autoAllocateChunkSize: 10,
    pull(controller) {
      controller.enqueue(new Uint8Array([1, 2, 3]));
      controller.byobRequest.respond(2);
    }
  });
  var reader = rs.getReader();
  return _await(Promise.all([reader.read(), reader.read()]), function ([read1, read2]) {
    assert_false(read1.done, 'read1.done should not be true');
    assert_array_equals(read1.value, [1, 2, 3], 'read1.value should be 3 bytes');
    assert_false(read2.done, 'read2.done should not be true');
    assert_array_equals(read2.value, [0, 0], 'read2.value should be 2 bytes');
  });
}), 'byobRequest.respond() after enqueue() with double read should not crash');