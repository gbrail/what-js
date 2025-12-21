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
var thrownError = new Error('bad things are happening!');
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
function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
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
thrownError.name = 'error1';
var originalReason = new Error('original reason');
originalReason.name = 'error2';
promise_test(_async(function (t) {
  var cancelled = undefined;
  var ts = new TransformStream({
    cancel(reason) {
      cancelled = reason;
    }
  });
  return _await(ts.readable.cancel(thrownError), function (res) {
    assert_equals(res, undefined, 'readable.cancel() should return undefined');
    assert_equals(cancelled, thrownError, 'transformer.cancel() should be called with the passed reason');
  });
}), 'cancelling the readable side should call transformer.cancel()');
promise_test(_async(function (t) {
  var ts = new TransformStream({
    cancel(reason) {
      assert_equals(reason, originalReason, 'transformer.cancel() should be called with the passed reason');
      throw thrownError;
    }
  });
  var writer = ts.writable.getWriter();
  var cancelPromise = ts.readable.cancel(originalReason);
  return _await(promise_rejects_exactly(t, thrownError, cancelPromise, 'readable.cancel() should reject with thrownError'), function () {
    return _awaitIgnored(promise_rejects_exactly(t, thrownError, writer.closed, 'writer.closed should reject with thrownError'));
  });
}), 'cancelling the readable side should reject if transformer.cancel() throws');
promise_test(_async(function (t) {
  var aborted = undefined;
  var ts = new TransformStream({
    cancel(reason) {
      aborted = reason;
    },
    flush: t.unreached_func('flush should not be called')
  });
  return _await(ts.writable.abort(thrownError), function (res) {
    assert_equals(res, undefined, 'writable.abort() should return undefined');
    assert_equals(aborted, thrownError, 'transformer.abort() should be called with the passed reason');
  });
}), 'aborting the writable side should call transformer.abort()');
promise_test(_async(function (t) {
  var ts = new TransformStream({
    cancel(reason) {
      assert_equals(reason, originalReason, 'transformer.cancel() should be called with the passed reason');
      throw thrownError;
    },
    flush: t.unreached_func('flush should not be called')
  });
  var reader = ts.readable.getReader();
  var abortPromise = ts.writable.abort(originalReason);
  return _await(promise_rejects_exactly(t, thrownError, abortPromise, 'writable.abort() should reject with thrownError'), function () {
    return _awaitIgnored(promise_rejects_exactly(t, thrownError, reader.closed, 'reader.closed should reject with thrownError'));
  });
}), 'aborting the writable side should reject if transformer.cancel() throws');
promise_test(_async(function (t) {
  var ts = new TransformStream({
    cancel: _async(function (reason) {
      assert_equals(reason, originalReason, 'transformer.cancel() should be called with the passed reason');
      throw thrownError;
    }),
    flush: t.unreached_func('flush should not be called')
  });
  var cancelPromise = ts.readable.cancel(originalReason);
  var closePromise = ts.writable.close();
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, thrownError, cancelPromise, 'cancelPromise should reject with thrownError'), promise_rejects_exactly(t, thrownError, closePromise, 'closePromise should reject with thrownError')]));
}), 'closing the writable side should reject if a parallel transformer.cancel() throws');
promise_test(_async(function (t) {
  var controller;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    cancel: _async(function (reason) {
      assert_equals(reason, originalReason, 'transformer.cancel() should be called with the passed reason');
      controller.error(thrownError);
      return _await();
    }),
    flush: t.unreached_func('flush should not be called')
  });
  var cancelPromise = ts.readable.cancel(originalReason);
  var closePromise = ts.writable.close();
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, thrownError, cancelPromise, 'cancelPromise should reject with thrownError'), promise_rejects_exactly(t, thrownError, closePromise, 'closePromise should reject with thrownError')]));
}), 'readable.cancel() and a parallel writable.close() should reject if a transformer.cancel() calls controller.error()');
promise_test(_async(function (t) {
  var controller;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    cancel: _async(function (reason) {
      assert_equals(reason, originalReason, 'transformer.cancel() should be called with the passed reason');
      controller.error(thrownError);
      return _await();
    }),
    flush: t.unreached_func('flush should not be called')
  });
  var cancelPromise = ts.writable.abort(originalReason);
  return _await(promise_rejects_exactly(t, thrownError, cancelPromise, 'cancelPromise should reject with thrownError'), function () {
    var closePromise = ts.readable.cancel(1);
    return _awaitIgnored(promise_rejects_exactly(t, thrownError, closePromise, 'closePromise should reject with thrownError'));
  });
}), 'writable.abort() and readable.cancel() should reject if a transformer.cancel() calls controller.error()');
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var controller;
  var cancelPromise;
  var flushCalled = false;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    flush() {
      flushCalled = true;
      cancelPromise = ts.readable.cancel(cancelReason);
    },
    cancel: t.unreached_func('cancel should not be called')
  });
  return _call(flushAsyncEvents, function () {
    // ensure stream is started
    return _await(ts.writable.close(), function () {
      assert_true(flushCalled, 'flush() was called');
      return _awaitIgnored(cancelPromise);
    });
  });
}), 'readable.cancel() should not call cancel() when flush() is already called from writable.close()');
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var abortReason = new Error('abort reason');
  var cancelCalls = 0;
  var controller;
  var cancelPromise;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    cancel() {
      if (++cancelCalls === 1) {
        cancelPromise = ts.readable.cancel(cancelReason);
      }
    },
    flush: t.unreached_func('flush should not be called')
  });
  return _call(flushAsyncEvents, function () {
    // ensure stream is started
    return _await(ts.writable.abort(abortReason), function () {
      assert_equals(cancelCalls, 1);
      return _await(cancelPromise, function () {
        assert_equals(cancelCalls, 1);
      });
    });
  });
}), 'readable.cancel() should not call cancel() again when already called from writable.abort()');
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var controller;
  var closePromise;
  var cancelCalled = false;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    cancel() {
      cancelCalled = true;
      closePromise = ts.writable.close();
    },
    flush: t.unreached_func('flush should not be called')
  });
  return _call(flushAsyncEvents, function () {
    // ensure stream is started
    return _await(ts.readable.cancel(cancelReason), function () {
      assert_true(cancelCalled, 'cancel() was called');
      return _awaitIgnored(closePromise);
    });
  });
}), 'writable.close() should not call flush() when cancel() is already called from readable.cancel()');
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var abortReason = new Error('abort reason');
  var cancelCalls = 0;
  var controller;
  var abortPromise;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    },
    cancel() {
      if (++cancelCalls === 1) {
        abortPromise = ts.writable.abort(abortReason);
      }
    },
    flush: t.unreached_func('flush should not be called')
  });
  return _call(flushAsyncEvents, function () {
    // ensure stream is started
    return _await(promise_rejects_exactly(t, abortReason, ts.readable.cancel(cancelReason)), function () {
      assert_equals(cancelCalls, 1);
      return _await(promise_rejects_exactly(t, abortReason, abortPromise), function () {
        assert_equals(cancelCalls, 1);
      });
    });
  });
}), 'writable.abort() should not call cancel() again when already called from readable.cancel()');