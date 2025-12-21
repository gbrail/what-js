// META: global=window,worker,shadowrealm
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
promise_test(_async(function (t) {
  /** @type {ReadableStreamDefaultController} */
  var con;
  var synchronous = false;
  new ReadableStream({
    start(c) {
      con = c;
    }
  }, {
    highWaterMark: 0
  }).pipeTo(new WritableStream({
    write() {
      synchronous = true;
    }
  }));
  // wait until start algorithm finishes
  return _await(Promise.resolve(), function () {
    con.enqueue();
    assert_false(synchronous, 'write algorithm must not run synchronously');
  });
}), "enqueue() must not synchronously call write algorithm");