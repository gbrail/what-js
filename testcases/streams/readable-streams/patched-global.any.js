// META: global=window,worker,shadowrealm
'use strict';

// Tests which patch the global environment are kept separate to avoid
// interfering with other tests.
var _asyncIteratorSymbol = /*#__PURE__*/typeof Symbol !== "undefined" ? Symbol.asyncIterator || (Symbol.asyncIterator = Symbol("Symbol.asyncIterator")) : "@@asyncIterator";
function _settle(pact, state, value) {
  if (!pact.s) {
    if (value instanceof _Pact) {
      if (value.s) {
        if (state & 1) {
          state = value.s;
        }
        value = value.v;
      } else {
        value.o = _settle.bind(null, pact, state);
        return;
      }
    }
    if (value && value.then) {
      value.then(_settle.bind(null, pact, state), _settle.bind(null, pact, 2));
      return;
    }
    pact.s = state;
    pact.v = value;
    var observer = pact.o;
    if (observer) {
      observer(pact);
    }
  }
}
var _Pact = /*#__PURE__*/function () {
    function _Pact() {}
    _Pact.prototype.then = function (onFulfilled, onRejected) {
      var result = new _Pact();
      var state = this.s;
      if (state) {
        var callback = state & 1 ? onFulfilled : onRejected;
        if (callback) {
          try {
            _settle(result, 1, callback(this.v));
          } catch (e) {
            _settle(result, 2, e);
          }
          return result;
        } else {
          return this;
        }
      }
      this.o = function (_this) {
        try {
          var value = _this.v;
          if (_this.s & 1) {
            _settle(result, 1, onFulfilled ? onFulfilled(value) : value);
          } else if (onRejected) {
            _settle(result, 1, onRejected(value));
          } else {
            _settle(result, 2, value);
          }
        } catch (e) {
          _settle(result, 2, e);
        }
      };
      return result;
    };
    return _Pact;
  }(),
  _iteratorSymbol = /*#__PURE__*/typeof Symbol !== "undefined" ? Symbol.iterator || (Symbol.iterator = Symbol("Symbol.iterator")) : "@@iterator";
function _isSettledPact(thenable) {
  return thenable instanceof _Pact && thenable.s & 1;
}
function _forTo(array, body, check) {
  var i = -1,
    pact,
    reject;
  function _cycle(result) {
    try {
      while (++i < array.length && (!check || !check())) {
        result = body(i);
        if (result && result.then) {
          if (_isSettledPact(result)) {
            result = result.v;
          } else {
            result.then(_cycle, reject || (reject = _settle.bind(null, pact = new _Pact(), 2)));
            return;
          }
        }
      }
      if (pact) {
        _settle(pact, 1, result);
      } else {
        pact = result;
      }
    } catch (e) {
      _settle(pact || (pact = new _Pact()), 2, e);
    }
  }
  _cycle();
  return pact;
}
var ReadableStream_prototype_locked_get = Object.getOwnPropertyDescriptor(ReadableStream.prototype, 'locked').get; // Verify that |rs| passes the brand check as a readable stream.
function _forOf(target, body, check) {
  if (typeof target[_iteratorSymbol] === "function") {
    var iterator = target[_iteratorSymbol](),
      step,
      pact,
      reject;
    function _cycle(result) {
      try {
        while (!(step = iterator.next()).done && (!check || !check())) {
          result = body(step.value);
          if (result && result.then) {
            if (_isSettledPact(result)) {
              result = result.v;
            } else {
              result.then(_cycle, reject || (reject = _settle.bind(null, pact = new _Pact(), 2)));
              return;
            }
          }
        }
        if (pact) {
          _settle(pact, 1, result);
        } else {
          pact = result;
        }
      } catch (e) {
        _settle(pact || (pact = new _Pact()), 2, e);
      }
    }
    _cycle();
    if (iterator.return) {
      var _fixup = function (value) {
        try {
          if (!step.done) {
            iterator.return();
          }
        } catch (e) {}
        return value;
      };
      if (pact && pact.then) {
        return pact.then(_fixup, function (e) {
          throw _fixup(e);
        });
      }
      _fixup();
    }
    return pact;
  }
  // No support for Symbol.iterator
  if (!("length" in target)) {
    throw new TypeError("Object is not iterable");
  }
  // Handle live collections properly
  var values = [];
  for (var i = 0; i < target.length; i++) {
    values.push(target[i]);
  }
  return _forTo(values, function (i) {
    return body(values[i]);
  }, check);
}
function _forAwaitOf(target, body, check) {
  if (typeof target[_asyncIteratorSymbol] === "function") {
    var pact = new _Pact();
    var iterator = target[_asyncIteratorSymbol]();
    iterator.next().then(_resumeAfterNext).then(void 0, _reject);
    return pact;
    function _resumeAfterBody(result) {
      if (check && check()) {
        return _settle(pact, 1, iterator.return ? iterator.return().then(function () {
          return result;
        }) : result);
      }
      iterator.next().then(_resumeAfterNext).then(void 0, _reject);
    }
    function _resumeAfterNext(step) {
      if (step.done) {
        _settle(pact, 1);
      } else {
        Promise.resolve(body(step.value)).then(_resumeAfterBody).then(void 0, _reject);
      }
    }
    function _reject(error) {
      _settle(pact, 2, iterator.return ? iterator.return().then(function () {
        return error;
      }) : error);
    }
  }
  return Promise.resolve(_forOf(target, function (value) {
    return Promise.resolve(value).then(body);
  }, check));
}
function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
function _continue(value, then) {
  return value && value.then ? value.then(then) : then(value);
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
function isReadableStream(rs) {
  try {
    ReadableStream_prototype_locked_get.call(rs);
    return true;
  } catch (e) {
    return false;
  }
}
test(t => {
  var rs = new ReadableStream();
  var trappedProperties = ['highWaterMark', 'size', 'start', 'type', 'mode'];
  var _loop = function (property) {
    // eslint-disable-next-line no-extend-native, accessor-pairs
    Object.defineProperty(Object.prototype, property, {
      get() {
        throw new Error(`${property} getter called`);
      },
      configurable: true
    });
  };
  for (var property of trappedProperties) {
    _loop(property);
  }
  t.add_cleanup(() => {
    for (var _property of trappedProperties) {
      delete Object.prototype[_property];
    }
  });
  var [branch1, branch2] = rs.tee();
  assert_true(isReadableStream(branch1), 'branch1 should be a ReadableStream');
  assert_true(isReadableStream(branch2), 'branch2 should be a ReadableStream');
}, 'ReadableStream tee() should not touch Object.prototype properties');
test(t => {
  var rs = new ReadableStream();
  var oldReadableStream = self.ReadableStream;
  self.ReadableStream = function () {
    throw new Error('ReadableStream called on global object');
  };
  t.add_cleanup(() => {
    self.ReadableStream = oldReadableStream;
  });
  var [branch1, branch2] = rs.tee();
  assert_true(isReadableStream(branch1), 'branch1 should be a ReadableStream');
  assert_true(isReadableStream(branch2), 'branch2 should be a ReadableStream');
}, 'ReadableStream tee() should not call the global ReadableStream');
promise_test(_async(function (t) {
  var _interrupt = false;
  var rs = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });
  var oldReadableStreamGetReader = ReadableStream.prototype.getReader;
  var ReadableStreamDefaultReader = new ReadableStream().getReader().constructor;
  var oldDefaultReaderRead = ReadableStreamDefaultReader.prototype.read;
  var oldDefaultReaderCancel = ReadableStreamDefaultReader.prototype.cancel;
  var oldDefaultReaderReleaseLock = ReadableStreamDefaultReader.prototype.releaseLock;
  self.ReadableStream.prototype.getReader = function () {
    throw new Error('patched getReader() called');
  };
  ReadableStreamDefaultReader.prototype.read = function () {
    throw new Error('patched read() called');
  };
  ReadableStreamDefaultReader.prototype.cancel = function () {
    throw new Error('patched cancel() called');
  };
  ReadableStreamDefaultReader.prototype.releaseLock = function () {
    throw new Error('patched releaseLock() called');
  };
  t.add_cleanup(() => {
    self.ReadableStream.prototype.getReader = oldReadableStreamGetReader;
    ReadableStreamDefaultReader.prototype.read = oldDefaultReaderRead;
    ReadableStreamDefaultReader.prototype.cancel = oldDefaultReaderCancel;
    ReadableStreamDefaultReader.prototype.releaseLock = oldDefaultReaderReleaseLock;
  });

  // read the first chunk, then cancel
  return _continue(_forAwaitOf(rs, function (chunk) {
    _interrupt = true;
  }, function () {
    return _interrupt;
  }), function () {
    // should be able to acquire a new reader
    var reader = oldReadableStreamGetReader.call(rs);
    // stream should be cancelled
    return _awaitIgnored(reader.closed);
  });
}), 'ReadableStream async iterator should use the original values of getReader() and ReadableStreamDefaultReader ' + 'methods');
test(t => {
  var oldPromiseThen = Promise.prototype.then;
  Promise.prototype.then = () => {
    throw new Error('patched then() called');
  };
  t.add_cleanup(() => {
    Promise.prototype.then = oldPromiseThen;
  });
  var [branch1, branch2] = new ReadableStream().tee();
  assert_true(isReadableStream(branch1), 'branch1 should be a ReadableStream');
  assert_true(isReadableStream(branch2), 'branch2 should be a ReadableStream');
}, 'tee() should not call Promise.prototype.then()');
test(t => {
  var oldPromiseThen = Promise.prototype.then;
  Promise.prototype.then = () => {
    throw new Error('patched then() called');
  };
  t.add_cleanup(() => {
    Promise.prototype.then = oldPromiseThen;
  });
  var readableController;
  var rs = new ReadableStream({
    start(c) {
      readableController = c;
    }
  });
  var ws = new WritableStream();
  rs.pipeTo(ws);
  readableController.close();
}, 'pipeTo() should not call Promise.prototype.then()');