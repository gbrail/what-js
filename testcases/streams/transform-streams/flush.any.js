// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
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
promise_test(() => {
  var flushCalled = false;
  var ts = new TransformStream({
    transform() {},
    flush() {
      flushCalled = true;
    }
  });
  return ts.writable.getWriter().close().then(() => {
    return assert_true(flushCalled, 'closing the writable triggers the transform flush immediately');
  });
}, 'TransformStream flush is called immediately when the writable is closed, if no writes are queued');
promise_test(() => {
  var flushCalled = false;
  var resolveTransform;
  var ts = new TransformStream({
    transform() {
      return new Promise(resolve => {
        resolveTransform = resolve;
      });
    },
    flush() {
      flushCalled = true;
      return new Promise(() => {}); // never resolves
    }
  }, undefined, {
    highWaterMark: 1
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  assert_false(flushCalled, 'closing the writable does not immediately call flush if writes are not finished');
  var rsClosed = false;
  ts.readable.getReader().closed.then(() => {
    rsClosed = true;
  });
  return delay(0).then(() => {
    assert_false(flushCalled, 'closing the writable does not asynchronously call flush if writes are not finished');
    resolveTransform();
    return delay(0);
  }).then(() => {
    assert_true(flushCalled, 'flush is eventually called');
    assert_false(rsClosed, 'if flushPromise does not resolve, the readable does not become closed');
  });
}, 'TransformStream flush is called after all queued writes finish, once the writable is closed');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform() {},
    flush() {
      c.enqueue('x');
      c.enqueue('y');
    }
  });
  var reader = ts.readable.getReader();
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  return reader.read().then(result1 => {
    assert_equals(result1.value, 'x', 'the first chunk read is the first one enqueued in flush');
    assert_equals(result1.done, false, 'the first chunk read is the first one enqueued in flush');
    return reader.read().then(result2 => {
      assert_equals(result2.value, 'y', 'the second chunk read is the second one enqueued in flush');
      assert_equals(result2.done, false, 'the second chunk read is the second one enqueued in flush');
    });
  });
}, 'TransformStream flush gets a chance to enqueue more into the readable');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform() {},
    flush() {
      c.enqueue('x');
      c.enqueue('y');
      return delay(0);
    }
  });
  var reader = ts.readable.getReader();
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  return Promise.all([reader.read().then(result1 => {
    assert_equals(result1.value, 'x', 'the first chunk read is the first one enqueued in flush');
    assert_equals(result1.done, false, 'the first chunk read is the first one enqueued in flush');
    return reader.read().then(result2 => {
      assert_equals(result2.value, 'y', 'the second chunk read is the second one enqueued in flush');
      assert_equals(result2.done, false, 'the second chunk read is the second one enqueued in flush');
    });
  }), reader.closed.then(() => {
    assert_true(true, 'readable reader becomes closed');
  })]);
}, 'TransformStream flush gets a chance to enqueue more into the readable, and can then async close');
var error1 = new Error('error1');
error1.name = 'error1';
promise_test(t => {
  var ts = new TransformStream({
    flush(controller) {
      controller.error(error1);
    }
  });
  return promise_rejects_exactly(t, error1, ts.writable.getWriter().close(), 'close() should reject');
}, 'error() during flush should cause writer.close() to reject');
promise_test(_async(function (t) {
  var flushed = false;
  var ts = new TransformStream({
    flush() {
      flushed = true;
    },
    cancel: t.unreached_func('cancel should not be called')
  });
  var closePromise = ts.writable.close();
  return _await(delay(0), function () {
    var cancelPromise = ts.readable.cancel(error1);
    return _await(Promise.all([closePromise, cancelPromise]), function () {
      assert_equals(flushed, true, 'transformer.flush() should be called');
    });
  });
}), 'closing the writable side should call transformer.flush() and a parallel readable.cancel() should not reject');