// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
// META: script=/common/gc.js
'use strict';

function _call(body, then, direct) {
  if (direct) {
    return then ? then(body()) : body();
  }
  try {
    var result = Promise.resolve(body());
    return then ? result.then(then) : result;
  } catch (e) {
    return Promise.reject(e);
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
promise_test(_async(function () {
  var controller;
  new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  return _call(garbageCollect, function () {
    return delay(50).then(() => {
      controller.close();
      assert_throws_js(TypeError, () => controller.close(), 'close should throw a TypeError the second time');
      controller.error();
    });
  });
}), 'ReadableStreamController methods should continue working properly when scripts lose their reference to the ' + 'readable stream');
promise_test(_async(function () {
  var controller;
  var closedPromise = new ReadableStream({
    start(c) {
      controller = c;
    }
  }).getReader().closed;
  return _call(garbageCollect, function () {
    return delay(50).then(() => controller.close()).then(() => closedPromise);
  });
}), 'ReadableStream closed promise should fulfill even if the stream and reader JS references are lost');
promise_test(_async(function (t) {
  var theError = new Error('boo');
  var controller;
  var closedPromise = new ReadableStream({
    start(c) {
      controller = c;
    }
  }).getReader().closed;
  return _call(garbageCollect, function () {
    return delay(50).then(() => controller.error(theError)).then(() => promise_rejects_exactly(t, theError, closedPromise));
  });
}), 'ReadableStream closed promise should reject even if stream and reader JS references are lost');
promise_test(_async(function () {
  var rs = new ReadableStream({});
  rs.getReader();
  return _call(garbageCollect, function () {
    return delay(50).then(() => assert_throws_js(TypeError, () => rs.getReader(), 'old reader should still be locking the stream even after garbage collection'));
  });
}), 'Garbage-collecting a ReadableStreamDefaultReader should not unlock its stream');
promise_test(_async(function () {
  var promise = (() => {
    var rs = new ReadableStream({
      pull(controller) {
        controller.enqueue('words');
      }
    });
    var reader = rs.getReader();
    return reader.read();
  })();
  return _call(garbageCollect, function () {
    return _await(promise, function ({
      value,
      done
    }) {
      // If we get here, the test passed.
      assert_equals(value, 'words', 'value should be words');
      assert_false(done, 'we should not be done');
    });
  });
}), 'A ReadableStream and its reader should not be garbage collected while there is a read promise pending');