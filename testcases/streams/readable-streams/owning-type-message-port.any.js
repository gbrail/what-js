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
function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
promise_test(_async(function () {
  var channel = new MessageChannel();
  var port1 = channel.port1;
  var port2 = channel.port2;
  var source = {
    start(controller) {
      controller.enqueue(port1, {
        transfer: [port1]
      });
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  return _await(stream.getReader().read(), function (chunk) {
    assert_not_equals(chunk.value, port1);
    var promise = new Promise(resolve => port2.onmessage = e => resolve(e.data));
    chunk.value.postMessage("toPort2");
    var _assert_equals = assert_equals;
    return _await(promise, function (_promise) {
      _assert_equals(_promise, "toPort2");
      promise = new Promise(resolve => chunk.value.onmessage = e => resolve(e.data));
      port2.postMessage("toPort1");
      var _assert_equals2 = assert_equals;
      return _await(promise, function (_promise2) {
        _assert_equals2(_promise2, "toPort1");
      });
    });
  });
}), 'Transferred MessageChannel works as expected');
promise_test(_async(function (t) {
  var channel = new MessageChannel();
  var port1 = channel.port1;
  var port2 = channel.port2;
  var source = {
    start(controller) {
      controller.enqueue({
        port1
      }, {
        transfer: [port1]
      });
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var [clone1, clone2] = stream.tee();
  return _awaitIgnored(promise_rejects_dom(t, "DataCloneError", clone2.getReader().read()));
}), 'Second branch of owning ReadableStream tee should end up into errors with transfer only values');