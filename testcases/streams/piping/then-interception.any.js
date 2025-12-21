// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
// META: script=../resources/recording-streams.js
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
function interceptThen() {
  var intercepted = [];
  var callCount = 0;
  Object.prototype.then = function (resolver) {
    if (!this.done) {
      intercepted.push(this.value);
    }
    var retval = Object.create(null);
    retval.done = ++callCount === 3;
    retval.value = callCount;
    resolver(retval);
    if (retval.done) {
      delete Object.prototype.then;
    }
  };
  return intercepted;
}
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    start(controller) {
      controller.enqueue('a');
      controller.close();
    }
  });
  var ws = recordingWritableStream();
  var intercepted = interceptThen();
  t.add_cleanup(() => {
    delete Object.prototype.then;
  });
  return _await(rs.pipeTo(ws), function () {
    delete Object.prototype.then;
    assert_array_equals(intercepted, [], 'nothing should have been intercepted');
    assert_array_equals(ws.events, ['write', 'a', 'close'], 'written chunk should be "a"');
  });
}), 'piping should not be observable');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    start(controller) {
      controller.enqueue('a');
      controller.close();
    }
  });
  var ws = recordingWritableStream();
  var [branch1, branch2] = rs.tee();
  var intercepted = interceptThen();
  t.add_cleanup(() => {
    delete Object.prototype.then;
  });
  return _await(branch1.pipeTo(ws), function () {
    delete Object.prototype.then;
    branch2.cancel();
    assert_array_equals(intercepted, [], 'nothing should have been intercepted');
    assert_array_equals(ws.events, ['write', 'a', 'close'], 'written chunk should be "a"');
  });
}), 'tee should not be observable');