// META: global=window,worker,shadowrealm
// META: script=../resources/recording-streams.js
// META: script=../resources/test-utils.js
'use strict';

// Tests for the use of pipeTo with AbortSignal.
// There is some extra complexity to avoid timeouts in environments where abort is not implemented.
function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
}
var error1 = new Error('error1');
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
function _invoke(body, then) {
  var result = body();
  if (result && result.then) {
    return result.then(then);
  }
  return then(result);
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
function _invokeIgnored(body) {
  var result = body();
  if (result && result.then) {
    return result.then(_empty);
  }
}
error1.name = 'error1';
var error2 = new Error('error2');
error2.name = 'error2';
var errorOnPull = {
  pull(controller) {
    // This will cause the test to error if pipeTo abort is not implemented.
    controller.error('failed to abort');
  }
};

// To stop pull() being called immediately when the stream is created, we need to set highWaterMark to 0.
var hwm0 = {
  highWaterMark: 0
};
var _loop = function (invalidSignal) {
  promise_test(t => {
    var rs = recordingReadableStream(errorOnPull, hwm0);
    var ws = recordingWritableStream();
    return promise_rejects_js(t, TypeError, rs.pipeTo(ws, {
      signal: invalidSignal
    }), 'pipeTo should reject').then(() => {
      assert_equals(rs.events.length, 0, 'no ReadableStream methods should have been called');
      assert_equals(ws.events.length, 0, 'no WritableStream methods should have been called');
    });
  }, `a signal argument '${invalidSignal}' should cause pipeTo() to reject`);
};
for (var invalidSignal of [null, 'AbortSignal', true, -1, Object.create(AbortSignal.prototype)]) {
  _loop(invalidSignal);
}
promise_test(t => {
  var rs = recordingReadableStream(errorOnPull, hwm0);
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject').then(() => Promise.all([rs.getReader().closed, promise_rejects_dom(t, 'AbortError', ws.getWriter().closed, 'writer.closed should reject')])).then(() => {
    assert_equals(rs.events.length, 2, 'cancel should have been called');
    assert_equals(rs.events[0], 'cancel', 'first event should be cancel');
    assert_equals(rs.events[1].name, 'AbortError', 'the argument to cancel should be an AbortError');
    assert_equals(rs.events[1].constructor.name, 'DOMException', 'the argument to cancel should be a DOMException');
  });
}, 'an aborted signal should cause the writable stream to reject with an AbortError');
var _loop2 = function (reason) {
  promise_test(_async(function (t) {
    var rs = recordingReadableStream(errorOnPull, hwm0);
    var ws = new WritableStream();
    var abortController = new AbortController();
    var signal = abortController.signal;
    abortController.abort(reason);
    var pipeToPromise = rs.pipeTo(ws, {
      signal
    });
    return _invoke(function () {
      if (reason !== undefined) {
        return _awaitIgnored(promise_rejects_exactly(t, reason, pipeToPromise, 'pipeTo rejects with abort reason'));
      } else {
        return _awaitIgnored(promise_rejects_dom(t, 'AbortError', pipeToPromise, 'pipeTo rejects with AbortError'));
      }
    }, function () {
      return _await(pipeToPromise.catch(e => e), function (error) {
        return _await(rs.getReader().closed, function () {
          return _await(promise_rejects_exactly(t, error, ws.getWriter().closed, 'the writable should be errored with the same object'), function () {
            assert_equals(signal.reason, error, 'signal.reason should be error'), assert_equals(rs.events.length, 2, 'cancel should have been called');
            assert_equals(rs.events[0], 'cancel', 'first event should be cancel');
            assert_equals(rs.events[1], error, 'the readable should be canceled with the same object');
          });
        });
      });
    });
  }), `(reason: '${reason}') all the error objects should be the same object`);
};
for (var reason of [null, undefined, error1]) {
  _loop2(reason);
}
promise_test(t => {
  var rs = recordingReadableStream(errorOnPull, hwm0);
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal,
    preventCancel: true
  }), 'pipeTo should reject').then(() => assert_equals(rs.events.length, 0, 'cancel should not be called'));
}, 'preventCancel should prevent canceling the readable');
promise_test(t => {
  var rs = new ReadableStream(errorOnPull, hwm0);
  var ws = recordingWritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal,
    preventAbort: true
  }), 'pipeTo should reject').then(() => {
    assert_equals(ws.events.length, 0, 'writable should not have been aborted');
    return ws.getWriter().ready;
  });
}, 'preventAbort should prevent aborting the readable');
promise_test(t => {
  var rs = recordingReadableStream(errorOnPull, hwm0);
  var ws = recordingWritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal,
    preventCancel: true,
    preventAbort: true
  }), 'pipeTo should reject').then(() => {
    assert_equals(rs.events.length, 0, 'cancel should not be called');
    assert_equals(ws.events.length, 0, 'writable should not have been aborted');
    return ws.getWriter().ready;
  });
}, 'preventCancel and preventAbort should prevent canceling the readable and aborting the readable');
var _loop3 = function (_reason) {
  promise_test(_async(function (t) {
    var rs = new ReadableStream({
      start(controller) {
        controller.enqueue('a');
        controller.enqueue('b');
        controller.close();
      }
    });
    var abortController = new AbortController();
    var signal = abortController.signal;
    var ws = recordingWritableStream({
      write() {
        abortController.abort(_reason);
      }
    });
    var pipeToPromise = rs.pipeTo(ws, {
      signal
    });
    return _invoke(function () {
      if (_reason !== undefined) {
        return _awaitIgnored(promise_rejects_exactly(t, _reason, pipeToPromise, 'pipeTo rejects with abort reason'));
      } else {
        return _awaitIgnored(promise_rejects_dom(t, 'AbortError', pipeToPromise, 'pipeTo rejects with AbortError'));
      }
    }, function () {
      return _await(pipeToPromise.catch(e => e), function (error) {
        assert_equals(signal.reason, error, 'signal.reason should be error');
        assert_equals(ws.events.length, 4, 'only chunk "a" should have been written');
        assert_array_equals(ws.events.slice(0, 3), ['write', 'a', 'abort'], 'events should match');
        assert_equals(ws.events[3], error, 'abort reason should be error');
      });
    });
  }), `(reason: '${_reason}') abort should prevent further reads`);
};
for (var _reason of [null, undefined, error1]) {
  _loop3(_reason);
}
var _loop4 = function (_reason2) {
  promise_test(_async(function (t) {
    var readController;
    var rs = new ReadableStream({
      start(c) {
        readController = c;
        c.enqueue('a');
        c.enqueue('b');
      }
    });
    var abortController = new AbortController();
    var signal = abortController.signal;
    var resolveWrite;
    var writePromise = new Promise(resolve => {
      resolveWrite = resolve;
    });
    var ws = recordingWritableStream({
      write() {
        return writePromise;
      }
    }, new CountQueuingStrategy({
      highWaterMark: Infinity
    }));
    var pipeToPromise = rs.pipeTo(ws, {
      signal
    });
    return _await(delay(0), function () {
      return _await(abortController.abort(_reason2), function () {
        return _await(readController.close(), function () {
          // Make sure the test terminates when signal is not implemented.
          return _call(resolveWrite, function () {
            return _invoke(function () {
              if (_reason2 !== undefined) {
                return _awaitIgnored(promise_rejects_exactly(t, _reason2, pipeToPromise, 'pipeTo rejects with abort reason'));
              } else {
                return _awaitIgnored(promise_rejects_dom(t, 'AbortError', pipeToPromise, 'pipeTo rejects with AbortError'));
              }
            }, function () {
              return _await(pipeToPromise.catch(e => e), function (error) {
                assert_equals(signal.reason, error, 'signal.reason should be error');
                assert_equals(ws.events.length, 6, 'chunks "a" and "b" should have been written');
                assert_array_equals(ws.events.slice(0, 5), ['write', 'a', 'write', 'b', 'abort'], 'events should match');
                assert_equals(ws.events[5], error, 'abort reason should be error');
              });
            });
          });
        });
      });
    });
  }), `(reason: '${_reason2}') all pending writes should complete on abort`);
};
for (var _reason2 of [null, undefined, error1]) {
  _loop4(_reason2);
}
var _loop5 = function (_reason3) {
  promise_test(_async(function (t) {
    var rejectPull;
    var pullPromise = new Promise((_, reject) => {
      rejectPull = reject;
    });
    var rejectCancel;
    var cancelPromise = new Promise((_, reject) => {
      rejectCancel = reject;
    });
    var rs = recordingReadableStream({
      pull: _async(function () {
        return _awaitIgnored(Promise.race([pullPromise, cancelPromise]));
      }),
      cancel(reason) {
        rejectCancel(reason);
      }
    });
    var ws = new WritableStream();
    var abortController = new AbortController();
    var signal = abortController.signal;
    var pipeToPromise = rs.pipeTo(ws, {
      signal
    });
    pipeToPromise.catch(() => {}); // Prevent unhandled rejection.
    return _await(delay(0), function () {
      abortController.abort(_reason3);
      rejectPull('should not catch pull rejection');
      return _await(delay(0), function () {
        assert_equals(rs.eventsWithoutPulls.length, 2, 'cancel should have been called');
        assert_equals(rs.eventsWithoutPulls[0], 'cancel', 'first event should be cancel');
        return _invokeIgnored(function () {
          if (_reason3 !== undefined) {
            return _awaitIgnored(promise_rejects_exactly(t, _reason3, pipeToPromise, 'pipeTo rejects with abort reason'));
          } else {
            return _awaitIgnored(promise_rejects_dom(t, 'AbortError', pipeToPromise, 'pipeTo rejects with AbortError'));
          }
        });
      });
    });
  }), `(reason: '${_reason3}') underlyingSource.cancel() should called when abort, even with pending pull`);
};
for (var _reason3 of [null, undefined, error1]) {
  _loop5(_reason3);
}
promise_test(t => {
  var rs = new ReadableStream({
    pull(controller) {
      controller.error('failed to abort');
    },
    cancel() {
      return Promise.reject(error1);
    }
  }, hwm0);
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_exactly(t, error1, rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject');
}, 'a rejection from underlyingSource.cancel() should be returned by pipeTo()');
promise_test(t => {
  var rs = new ReadableStream(errorOnPull, hwm0);
  var ws = new WritableStream({
    abort() {
      return Promise.reject(error1);
    }
  });
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_exactly(t, error1, rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject');
}, 'a rejection from underlyingSink.abort() should be returned by pipeTo()');
promise_test(t => {
  var events = [];
  var rs = new ReadableStream({
    pull(controller) {
      controller.error('failed to abort');
    },
    cancel() {
      events.push('cancel');
      return Promise.reject(error1);
    }
  }, hwm0);
  var ws = new WritableStream({
    abort() {
      events.push('abort');
      return Promise.reject(error2);
    }
  });
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_exactly(t, error2, rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject').then(() => assert_array_equals(events, ['abort', 'cancel'], 'abort() should be called before cancel()'));
}, 'a rejection from underlyingSink.abort() should be preferred to one from underlyingSource.cancel()');
promise_test(t => {
  var rs = new ReadableStream({
    start(controller) {
      controller.close();
    }
  });
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject');
}, 'abort signal takes priority over closed readable');
promise_test(t => {
  var rs = new ReadableStream({
    start(controller) {
      controller.error(error1);
    }
  });
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject');
}, 'abort signal takes priority over errored readable');
promise_test(t => {
  var rs = new ReadableStream({
    pull(controller) {
      controller.error('failed to abort');
    }
  }, hwm0);
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  var writer = ws.getWriter();
  return writer.close().then(() => {
    writer.releaseLock();
    return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
      signal
    }), 'pipeTo should reject');
  });
}, 'abort signal takes priority over closed writable');
promise_test(t => {
  var rs = new ReadableStream({
    pull(controller) {
      controller.error('failed to abort');
    }
  }, hwm0);
  var ws = new WritableStream({
    start(controller) {
      controller.error(error1);
    }
  });
  var abortController = new AbortController();
  var signal = abortController.signal;
  abortController.abort();
  return promise_rejects_dom(t, 'AbortError', rs.pipeTo(ws, {
    signal
  }), 'pipeTo should reject');
}, 'abort signal takes priority over errored writable');
promise_test(() => {
  var readController;
  var rs = new ReadableStream({
    start(c) {
      readController = c;
    }
  });
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  var pipeToPromise = rs.pipeTo(ws, {
    signal,
    preventClose: true
  });
  readController.close();
  return Promise.resolve().then(() => {
    abortController.abort();
    return pipeToPromise;
  }).then(() => ws.getWriter().write('this should succeed'));
}, 'abort should do nothing after the readable is closed');
promise_test(t => {
  var readController;
  var rs = new ReadableStream({
    start(c) {
      readController = c;
    }
  });
  var ws = new WritableStream();
  var abortController = new AbortController();
  var signal = abortController.signal;
  var pipeToPromise = rs.pipeTo(ws, {
    signal,
    preventAbort: true
  });
  readController.error(error1);
  return Promise.resolve().then(() => {
    abortController.abort();
    return promise_rejects_exactly(t, error1, pipeToPromise, 'pipeTo should reject');
  }).then(() => ws.getWriter().write('this should succeed'));
}, 'abort should do nothing after the readable is errored');
promise_test(t => {
  var readController;
  var rs = new ReadableStream({
    start(c) {
      readController = c;
    }
  });
  var resolveWrite;
  var writePromise = new Promise(resolve => {
    resolveWrite = resolve;
  });
  var ws = new WritableStream({
    write() {
      readController.error(error1);
      return writePromise;
    }
  });
  var abortController = new AbortController();
  var signal = abortController.signal;
  var pipeToPromise = rs.pipeTo(ws, {
    signal,
    preventAbort: true
  });
  readController.enqueue('a');
  return delay(0).then(() => {
    abortController.abort();
    resolveWrite();
    return promise_rejects_exactly(t, error1, pipeToPromise, 'pipeTo should reject');
  }).then(() => ws.getWriter().write('this should succeed'));
}, 'abort should do nothing after the readable is errored, even with pending writes');
promise_test(t => {
  var rs = recordingReadableStream({
    pull(controller) {
      return delay(0).then(() => controller.close());
    }
  });
  var writeController;
  var ws = new WritableStream({
    start(c) {
      writeController = c;
    }
  });
  var abortController = new AbortController();
  var signal = abortController.signal;
  var pipeToPromise = rs.pipeTo(ws, {
    signal,
    preventCancel: true
  });
  return Promise.resolve().then(() => {
    writeController.error(error1);
    return Promise.resolve();
  }).then(() => {
    abortController.abort();
    return promise_rejects_exactly(t, error1, pipeToPromise, 'pipeTo should reject');
  }).then(() => {
    assert_array_equals(rs.events, ['pull'], 'cancel should not have been called');
  });
}, 'abort should do nothing after the writable is errored');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    pull(c) {
      c.enqueue(new Uint8Array([]));
    },
    type: "bytes"
  });
  var ws = new WritableStream();
  var [first, second] = rs.tee();
  var aborted = false;
  first.pipeTo(ws, {
    signal: AbortSignal.abort()
  }).catch(() => {
    aborted = true;
  });
  return _await(delay(0), function () {
    assert_true(!aborted, "pipeTo should not resolve yet");
    return _await(second.cancel(), function () {
      return _await(delay(0), function () {
        assert_true(aborted, "pipeTo should be aborted now");
      });
    });
  });
}), "pipeTo on a teed readable byte stream should only be aborted when both branches are aborted");