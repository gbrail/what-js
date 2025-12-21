// META: global=window,worker,shadowrealm
'use strict';

var transformerMethods = {
  start: {
    length: 1,
    trigger: () => Promise.resolve()
  },
  transform: {
    length: 2,
    trigger: ts => ts.writable.getWriter().write()
  },
  flush: {
    length: 1,
    trigger: ts => ts.writable.getWriter().close()
  }
};
var _loop = function (method) {
  var {
    length,
    trigger
  } = transformerMethods[method];

  // Some semantic tests of how transformer methods are called can be found in general.js, as well as in the test files
  // specific to each method.
  promise_test(() => {
    var argCount;
    var ts = new TransformStream({
      [method](...args) {
        argCount = args.length;
      }
    }, undefined, {
      highWaterMark: Infinity
    });
    return Promise.resolve(trigger(ts)).then(() => {
      assert_equals(argCount, length, `${method} should be called with ${length} arguments`);
    });
  }, `transformer method ${method} should be called with the right number of arguments`);
  promise_test(() => {
    var methodWasCalled = false;
    function Transformer() {}
    Transformer.prototype = {
      [method]() {
        methodWasCalled = true;
      }
    };
    var ts = new TransformStream(new Transformer(), undefined, {
      highWaterMark: Infinity
    });
    return Promise.resolve(trigger(ts)).then(() => {
      assert_true(methodWasCalled, `${method} should be called`);
    });
  }, `transformer method ${method} should be called even when it's located on the prototype chain`);
};
for (var method in transformerMethods) {
  _loop(method);
}