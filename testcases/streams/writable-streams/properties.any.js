// META: global=window,worker,shadowrealm
'use strict';

var sinkMethods = {
  start: {
    length: 1,
    trigger: () => Promise.resolve()
  },
  write: {
    length: 2,
    trigger: writer => writer.write()
  },
  close: {
    length: 0,
    trigger: writer => writer.close()
  },
  abort: {
    length: 1,
    trigger: writer => writer.abort()
  }
};
var _loop = function (method) {
  var {
    length,
    trigger
  } = sinkMethods[method];

  // Some semantic tests of how sink methods are called can be found in general.js, as well as in the test files
  // specific to each method.
  promise_test(() => {
    var argCount;
    var ws = new WritableStream({
      [method](...args) {
        argCount = args.length;
      }
    });
    return Promise.resolve(trigger(ws.getWriter())).then(() => {
      assert_equals(argCount, length, `${method} should be called with ${length} arguments`);
    });
  }, `sink method ${method} should be called with the right number of arguments`);
  promise_test(() => {
    var methodWasCalled = false;
    function Sink() {}
    Sink.prototype = {
      [method]() {
        methodWasCalled = true;
      }
    };
    var ws = new WritableStream(new Sink());
    return Promise.resolve(trigger(ws.getWriter())).then(() => {
      assert_true(methodWasCalled, `${method} should be called`);
    });
  }, `sink method ${method} should be called even when it's located on the prototype chain`);
};
for (var method in sinkMethods) {
  _loop(method);
}