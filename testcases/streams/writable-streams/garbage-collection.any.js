// META: global=window,worker,shadowrealm
// META: script=/common/gc.js
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
promise_test(_async(function () {
  var written = false;
  var promise = (() => {
    var rs = new WritableStream({
      write() {
        written = true;
      }
    });
    var writer = rs.getWriter();
    return writer.write('something');
  })();
  return _call(garbageCollect, function () {
    return _await(promise, function () {
      assert_true(written);
    });
  });
}), 'A WritableStream and its writer should not be garbage collected while there is a write promise pending');