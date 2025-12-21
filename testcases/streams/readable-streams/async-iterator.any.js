// META: global=window,worker,shadowrealm
// META: script=../resources/rs-utils.js
// META: script=../resources/test-utils.js
// META: script=../resources/recording-streams.js
'use strict';

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
var error1 = new Error('error1');
const _Pact = /*#__PURE__*/function () {
    function _Pact() {}
    _Pact.prototype.then = function (onFulfilled, onRejected) {
      const result = new _Pact();
      const state = this.s;
      if (state) {
        const callback = state & 1 ? onFulfilled : onRejected;
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
          const value = _this.v;
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
  _earlyReturn = /*#__PURE__*/{},
  _asyncIteratorSymbol = /*#__PURE__*/typeof Symbol !== "undefined" ? Symbol.asyncIterator || (Symbol.asyncIterator = Symbol("Symbol.asyncIterator")) : "@@asyncIterator",
  _AsyncGenerator = /*#__PURE__*/function () {
    function _AsyncGenerator(entry) {
      this._entry = entry;
      this._pact = null;
      this._resolve = null;
      this._return = null;
      this._promise = null;
    }
    function _wrapReturnedValue(value) {
      return {
        value: value,
        done: true
      };
    }
    function _wrapYieldedValue(value) {
      return {
        value: value,
        done: false
      };
    }
    _AsyncGenerator.prototype._yield = function (value) {
      // Yield the value to the pending next call
      this._resolve(value && value.then ? value.then(_wrapYieldedValue) : _wrapYieldedValue(value));
      // Return a pact for an upcoming next/return/throw call
      return this._pact = new _Pact();
    };
    _AsyncGenerator.prototype.next = function (value) {
      // Advance the generator, starting it if it has yet to be started
      var _this = this;
      return _this._promise = new Promise(function (resolve) {
        var _pact = _this._pact;
        if (_pact === null) {
          var _entry = _this._entry;
          if (_entry === null) {
            // Generator is started, but not awaiting a yield expression
            // Abandon the next call!
            return resolve(_this._promise);
          }
          // Start the generator
          _this._entry = null;
          _this._resolve = resolve;
          function returnValue(value) {
            _this._resolve(value && value.then ? value.then(_wrapReturnedValue) : _wrapReturnedValue(value));
            _this._pact = null;
            _this._resolve = null;
          }
          var result = _entry(_this);
          if (result && result.then) {
            result.then(returnValue, function (error) {
              if (error === _earlyReturn) {
                returnValue(_this._return);
              } else {
                var pact = new _Pact();
                _this._resolve(pact);
                _this._pact = null;
                _this._resolve = null;
                _resolve(pact, 2, error);
              }
            });
          } else {
            returnValue(result);
          }
        } else {
          // Generator is started and a yield expression is pending, settle it
          _this._pact = null;
          _this._resolve = resolve;
          _settle(_pact, 1, value);
        }
      });
    };
    _AsyncGenerator.prototype.return = function (value) {
      // Early return from the generator if started, otherwise abandons the generator
      var _this = this;
      return _this._promise = new Promise(function (resolve) {
        var _pact = _this._pact;
        if (_pact === null) {
          if (_this._entry === null) {
            // Generator is started, but not awaiting a yield expression
            // Abandon the return call!
            return resolve(_this._promise);
          }
          // Generator is not started, abandon it and return the specified value
          _this._entry = null;
          return resolve(value && value.then ? value.then(_wrapReturnedValue) : _wrapReturnedValue(value));
        }
        // Settle the yield expression with a rejected "early return" value
        _this._return = value;
        _this._resolve = resolve;
        _this._pact = null;
        _settle(_pact, 2, _earlyReturn);
      });
    };
    _AsyncGenerator.prototype.throw = function (error) {
      // Inject an exception into the pending yield expression
      var _this = this;
      return _this._promise = new Promise(function (resolve, reject) {
        var _pact = _this._pact;
        if (_pact === null) {
          if (_this._entry === null) {
            // Generator is started, but not awaiting a yield expression
            // Abandon the throw call!
            return resolve(_this._promise);
          }
          // Generator is not started, abandon it and return a rejected Promise containing the error
          _this._entry = null;
          return reject(error);
        }
        // Settle the yield expression with the value as a rejection
        _this._resolve = resolve;
        _this._pact = null;
        _settle(_pact, 2, error);
      });
    };
    _AsyncGenerator.prototype[_asyncIteratorSymbol] = function () {
      return this;
    };
    return _AsyncGenerator;
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
function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
}
function _empty() {}
function _catch(body, recover) {
  try {
    var result = body();
  } catch (e) {
    return recover(e);
  }
  if (result && result.then) {
    return result.then(void 0, recover);
  }
  return result;
}
function _continueIgnored(value) {
  if (value && value.then) {
    return value.then(_empty);
  }
}
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
function _callIgnored(body, direct) {
  return _call(body, _empty, direct);
}
function assert_iter_result(iterResult, value, done, message) {
  var prefix = message === undefined ? '' : `${message} `;
  assert_equals(typeof iterResult, 'object', `${prefix}type is object`);
  assert_equals(Object.getPrototypeOf(iterResult), Object.prototype, `${prefix}[[Prototype]]`);
  assert_array_equals(Object.getOwnPropertyNames(iterResult).sort(), ['done', 'value'], `${prefix}property names`);
  assert_equals(iterResult.value, value, `${prefix}value`);
  assert_equals(iterResult.done, done, `${prefix}done`);
}
test(() => {
  var s = new ReadableStream();
  var it = s.values();
  var proto = Object.getPrototypeOf(it);
  var AsyncIteratorPrototype = Object.getPrototypeOf(Object.getPrototypeOf(function () {
    return new _AsyncGenerator(function (_generator) {});
  }).prototype);
  assert_equals(Object.getPrototypeOf(proto), AsyncIteratorPrototype, 'prototype should extend AsyncIteratorPrototype');
  var methods = ['next', 'return'].sort();
  assert_array_equals(Object.getOwnPropertyNames(proto).sort(), methods, 'should have all the correct methods');
  for (var m of methods) {
    var propDesc = Object.getOwnPropertyDescriptor(proto, m);
    assert_true(propDesc.enumerable, 'method should be enumerable');
    assert_true(propDesc.configurable, 'method should be configurable');
    assert_true(propDesc.writable, 'method should be writable');
    assert_equals(typeof it[m], 'function', 'method should be a function');
    assert_equals(it[m].name, m, 'method should have the correct name');
  }
  assert_equals(it.next.length, 0, 'next should have no parameters');
  assert_equals(it.return.length, 1, 'return should have 1 parameter');
  assert_equals(typeof it.throw, 'undefined', 'throw should not exist');
}, 'Async iterator instances should have the correct list of properties');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, [1, 2, 3]);
  });
}), 'Async-iterating a push source');
promise_test(_async(function () {
  var i = 1;
  var s = new ReadableStream({
    pull(c) {
      c.enqueue(i);
      if (i >= 3) {
        c.close();
      }
      i += 1;
    }
  });
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, [1, 2, 3]);
  });
}), 'Async-iterating a pull source');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.enqueue(undefined);
      c.enqueue(undefined);
      c.enqueue(undefined);
      c.close();
    }
  });
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, [undefined, undefined, undefined]);
  });
}), 'Async-iterating a push source with undefined values');
promise_test(_async(function () {
  var i = 1;
  var s = new ReadableStream({
    pull(c) {
      c.enqueue(undefined);
      if (i >= 3) {
        c.close();
      }
      i += 1;
    }
  });
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, [undefined, undefined, undefined]);
  });
}), 'Async-iterating a pull source with undefined values');
promise_test(_async(function () {
  var i = 1;
  var s = recordingReadableStream({
    pull(c) {
      c.enqueue(i);
      if (i >= 3) {
        c.close();
      }
      i += 1;
    }
  }, new CountQueuingStrategy({
    highWaterMark: 0
  }));
  var it = s.values();
  assert_array_equals(s.events, []);
  return _await(it.next(), function (read1) {
    assert_iter_result(read1, 1, false);
    assert_array_equals(s.events, ['pull']);
    return _await(it.next(), function (read2) {
      assert_iter_result(read2, 2, false);
      assert_array_equals(s.events, ['pull', 'pull']);
      return _await(it.next(), function (read3) {
        assert_iter_result(read3, 3, false);
        assert_array_equals(s.events, ['pull', 'pull', 'pull']);
        return _await(it.next(), function (read4) {
          assert_iter_result(read4, undefined, true);
          assert_array_equals(s.events, ['pull', 'pull', 'pull']);
        });
      });
    });
  });
}), 'Async-iterating a pull source manually');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.error('e');
    }
  });
  return _continueIgnored(_catch(function () {
    return _continue(_forAwaitOf(s, _empty), function () {
      assert_unreached();
    });
  }, function (e) {
    assert_equals(e, 'e');
  }));
}), 'Async-iterating an errored stream throws');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.close();
    }
  });
  return _continueIgnored(_forAwaitOf(s, function (chunk) {
    assert_unreached();
  }));
}), 'Async-iterating a closed stream never executes the loop body, but works fine');
promise_test(_async(function () {
  var s = new ReadableStream();
  var loop = _async(function () {
    return _continue(_forAwaitOf(s, function (chunk) {
      assert_unreached();
    }), function () {
      assert_unreached();
    });
  });
  return _awaitIgnored(Promise.race([loop(), flushAsyncEvents()]));
}), 'Async-iterating an empty but not closed/errored stream never executes the loop body and stalls the async function');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });
  var reader = s.getReader();
  return _await(reader.read(), function (readResult) {
    assert_iter_result(readResult, 1, false);
    reader.releaseLock();
    var chunks = [];
    return _continue(_forAwaitOf(s, function (chunk) {
      chunks.push(chunk);
    }), function () {
      assert_array_equals(chunks, [2, 3]);
    });
  });
}), 'Async-iterating a partially consumed stream');
var _loop = function (type) {
  var _loop4 = function (_preventCancel2) {
    promise_test(_async(function () {
      var s = recordingReadableStream({
        start(c) {
          c.enqueue(0);
        }
      });

      // use a separate function for the loop body so return does not stop the test
      var loop = _async(function () {
        var _exit = false,
          _interrupt3 = false;
        return _forAwaitOf(s.values({
          preventCancel: _preventCancel2
        }), function (c) {
          if (type === 'throw') {
            throw new Error();
          } else if (type === 'break') {
            _interrupt3 = true;
            return;
          } else if (type === 'return') {
            _exit = true;
            return;
          }
        }, function () {
          return _interrupt3 || _exit;
        });
      });
      return _continue(_catch(function () {
        return _callIgnored(loop);
      }, _empty), function () {
        if (_preventCancel2) {
          assert_array_equals(s.events, ['pull'], `cancel() should not be called`);
        } else {
          assert_array_equals(s.events, ['pull', 'cancel', undefined], `cancel() should be called`);
        }
      });
    }), `Cancellation behavior when ${type}ing inside loop body; preventCancel = ${_preventCancel2}`);
  };
  for (var _preventCancel2 of [false, true]) {
    _loop4(_preventCancel2);
  }
};
for (var type of ['throw', 'break', 'return']) {
  _loop(type);
}
var _loop2 = function (preventCancel) {
  promise_test(_async(function () {
    var s = recordingReadableStream({
      start(c) {
        c.enqueue(0);
      }
    });
    var it = s.values({
      preventCancel
    });
    return _await(it.return(), function () {
      if (preventCancel) {
        assert_array_equals(s.events, [], `cancel() should not be called`);
      } else {
        assert_array_equals(s.events, ['cancel', undefined], `cancel() should be called`);
      }
    });
  }), `Cancellation behavior when manually calling return(); preventCancel = ${preventCancel}`);
};
for (var preventCancel of [false, true]) {
  _loop2(preventCancel);
}
promise_test(_async(function (t) {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(it.next(), function (iterResult1) {
    assert_iter_result(iterResult1, 0, false, '1st next()');
    return _awaitIgnored(promise_rejects_exactly(t, error1, it.next(), '2nd next()'));
  });
}), 'next() rejects if the stream errors');
promise_test(_async(function () {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(it.return('return value'), function (iterResult) {
    assert_iter_result(iterResult, 'return value', true);
  });
}), 'return() does not rejects if the stream has not errored yet');
promise_test(_async(function (t) {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      // Do not error in start() because doing so would prevent acquiring a reader/async iterator.
      c.error(error1);
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _call(flushAsyncEvents, function () {
    return _awaitIgnored(promise_rejects_exactly(t, error1, it.return('return value')));
  });
}), 'return() rejects if the stream has errored');
promise_test(_async(function (t) {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(it.next(), function (iterResult1) {
    assert_iter_result(iterResult1, 0, false, '1st next()');
    return _await(promise_rejects_exactly(t, error1, it.next(), '2nd next()'), function () {
      return _await(it.next(), function (iterResult3) {
        assert_iter_result(iterResult3, undefined, true, '3rd next()');
      });
    });
  });
}), 'next() that succeeds; next() that reports an error; next()');
promise_test(_async(function () {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(Promise.allSettled([it.next(), it.next(), it.next()]), function (iterResults) {
    assert_equals(iterResults[0].status, 'fulfilled', '1st next() promise status');
    assert_iter_result(iterResults[0].value, 0, false, '1st next()');
    assert_equals(iterResults[1].status, 'rejected', '2nd next() promise status');
    assert_equals(iterResults[1].reason, error1, '2nd next() rejection reason');
    assert_equals(iterResults[2].status, 'fulfilled', '3rd next() promise status');
    assert_iter_result(iterResults[2].value, undefined, true, '3rd next()');
  });
}), 'next() that succeeds; next() that reports an error(); next() [no awaiting]');
promise_test(_async(function (t) {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(it.next(), function (iterResult1) {
    assert_iter_result(iterResult1, 0, false, '1st next()');
    return _await(promise_rejects_exactly(t, error1, it.next(), '2nd next()'), function () {
      return _await(it.return('return value'), function (iterResult3) {
        assert_iter_result(iterResult3, 'return value', true, 'return()');
      });
    });
  });
}), 'next() that succeeds; next() that reports an error(); return()');
promise_test(_async(function () {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(Promise.allSettled([it.next(), it.next(), it.return('return value')]), function (iterResults) {
    assert_equals(iterResults[0].status, 'fulfilled', '1st next() promise status');
    assert_iter_result(iterResults[0].value, 0, false, '1st next()');
    assert_equals(iterResults[1].status, 'rejected', '2nd next() promise status');
    assert_equals(iterResults[1].reason, error1, '2nd next() rejection reason');
    assert_equals(iterResults[2].status, 'fulfilled', 'return() promise status');
    assert_iter_result(iterResults[2].value, 'return value', true, 'return()');
  });
}), 'next() that succeeds; next() that reports an error(); return() [no awaiting]');
promise_test(_async(function () {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      c.enqueue(timesPulled);
      ++timesPulled;
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(it.next(), function (iterResult1) {
    assert_iter_result(iterResult1, 0, false, 'next()');
    return _await(it.return('return value'), function (iterResult2) {
      assert_iter_result(iterResult2, 'return value', true, 'return()');
      assert_equals(timesPulled, 2);
    });
  });
}), 'next() that succeeds; return()');
promise_test(_async(function () {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      c.enqueue(timesPulled);
      ++timesPulled;
    }
  });
  var it = s[Symbol.asyncIterator]();
  return _await(Promise.allSettled([it.next(), it.return('return value')]), function (iterResults) {
    assert_equals(iterResults[0].status, 'fulfilled', 'next() promise status');
    assert_iter_result(iterResults[0].value, 0, false, 'next()');
    assert_equals(iterResults[1].status, 'fulfilled', 'return() promise status');
    assert_iter_result(iterResults[1].value, 'return value', true, 'return()');
    assert_equals(timesPulled, 2);
  });
}), 'next() that succeeds; return() [no awaiting]');
promise_test(_async(function () {
  var rs = new ReadableStream();
  var it = rs.values();
  return _await(it.return('return value'), function (iterResult1) {
    assert_iter_result(iterResult1, 'return value', true, 'return()');
    return _await(it.next(), function (iterResult2) {
      assert_iter_result(iterResult2, undefined, true, 'next()');
    });
  });
}), 'return(); next()');
promise_test(_async(function () {
  var rs = new ReadableStream();
  var it = rs.values();
  var resolveOrder = [];
  return _await(Promise.allSettled([it.return('return value').then(result => {
    resolveOrder.push('return');
    return result;
  }), it.next().then(result => {
    resolveOrder.push('next');
    return result;
  })]), function (iterResults) {
    assert_equals(iterResults[0].status, 'fulfilled', 'return() promise status');
    assert_iter_result(iterResults[0].value, 'return value', true, 'return()');
    assert_equals(iterResults[1].status, 'fulfilled', 'next() promise status');
    assert_iter_result(iterResults[1].value, undefined, true, 'next()');
    assert_array_equals(resolveOrder, ['return', 'next'], 'next() resolves after return()');
  });
}), 'return(); next() [no awaiting]');
promise_test(_async(function () {
  var resolveCancelPromise;
  var rs = recordingReadableStream({
    cancel(reason) {
      return new Promise(r => resolveCancelPromise = r);
    }
  });
  var it = rs.values();
  var returnResolved = false;
  var returnPromise = it.return('return value').then(result => {
    returnResolved = true;
    return result;
  });
  return _call(flushAsyncEvents, function () {
    assert_false(returnResolved, 'return() should not resolve while cancel() promise is pending');
    resolveCancelPromise();
    return _await(returnPromise, function (iterResult1) {
      assert_iter_result(iterResult1, 'return value', true, 'return()');
      return _await(it.next(), function (iterResult2) {
        assert_iter_result(iterResult2, undefined, true, 'next()');
      });
    });
  });
}), 'return(); next() with delayed cancel()');
promise_test(_async(function () {
  var resolveCancelPromise;
  var rs = recordingReadableStream({
    cancel(reason) {
      return new Promise(r => resolveCancelPromise = r);
    }
  });
  var it = rs.values();
  var resolveOrder = [];
  var returnPromise = it.return('return value').then(result => {
    resolveOrder.push('return');
    return result;
  });
  var nextPromise = it.next().then(result => {
    resolveOrder.push('next');
    return result;
  });
  assert_array_equals(rs.events, ['cancel', 'return value'], 'return() should call cancel()');
  assert_array_equals(resolveOrder, [], 'return() should not resolve before cancel() resolves');
  resolveCancelPromise();
  return _await(returnPromise, function (iterResult1) {
    assert_iter_result(iterResult1, 'return value', true, 'return() should resolve with original reason');
    return _await(nextPromise, function (iterResult2) {
      assert_iter_result(iterResult2, undefined, true, 'next() should resolve with done result');
      assert_array_equals(rs.events, ['cancel', 'return value'], 'no pull() after cancel()');
      assert_array_equals(resolveOrder, ['return', 'next'], 'next() should resolve after return() resolves');
    });
  });
}), 'return(); next() with delayed cancel() [no awaiting]');
promise_test(_async(function () {
  var rs = new ReadableStream();
  var it = rs.values();
  return _await(it.return('return value 1'), function (iterResult1) {
    assert_iter_result(iterResult1, 'return value 1', true, '1st return()');
    return _await(it.return('return value 2'), function (iterResult2) {
      assert_iter_result(iterResult2, 'return value 2', true, '1st return()');
    });
  });
}), 'return(); return()');
promise_test(_async(function () {
  var rs = new ReadableStream();
  var it = rs.values();
  var resolveOrder = [];
  return _await(Promise.allSettled([it.return('return value 1').then(result => {
    resolveOrder.push('return 1');
    return result;
  }), it.return('return value 2').then(result => {
    resolveOrder.push('return 2');
    return result;
  })]), function (iterResults) {
    assert_equals(iterResults[0].status, 'fulfilled', '1st return() promise status');
    assert_iter_result(iterResults[0].value, 'return value 1', true, '1st return()');
    assert_equals(iterResults[1].status, 'fulfilled', '2nd return() promise status');
    assert_iter_result(iterResults[1].value, 'return value 2', true, '1st return()');
    assert_array_equals(resolveOrder, ['return 1', 'return 2'], '2nd return() resolves after 1st return()');
  });
}), 'return(); return() [no awaiting]');
test(() => {
  var s = new ReadableStream({
    start(c) {
      c.enqueue(0);
      c.close();
    }
  });
  s.values();
  assert_throws_js(TypeError, () => s.values(), 'values() should throw');
}, 'values() throws if there\'s already a lock');
promise_test(_async(function () {
  var s = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, [1, 2, 3]);
    var reader = s.getReader();
    return _awaitIgnored(reader.closed);
  });
}), 'Acquiring a reader after exhaustively async-iterating a stream');
promise_test(_async(function (t) {
  var timesPulled = 0;
  var s = new ReadableStream({
    pull(c) {
      if (timesPulled === 0) {
        c.enqueue(0);
        ++timesPulled;
      } else {
        c.error(error1);
      }
    }
  });
  var it = s[Symbol.asyncIterator]({
    preventCancel: true
  });
  return _await(it.next(), function (iterResult1) {
    assert_iter_result(iterResult1, 0, false, '1st next()');
    return _await(promise_rejects_exactly(t, error1, it.next(), '2nd next()'), function () {
      return _await(it.return('return value'), function (iterResult2) {
        assert_iter_result(iterResult2, 'return value', true, 'return()');

        // i.e. it should not reject with a generic "this stream is locked" TypeError.
        var reader = s.getReader();
        return _awaitIgnored(promise_rejects_exactly(t, error1, reader.closed, 'closed on the new reader should reject with the error'));
      });
    });
  });
}), 'Acquiring a reader after return()ing from a stream that errors');
promise_test(_async(function () {
  var _interrupt = false;
  var s = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });

  // read the first two chunks, then cancel
  var chunks = [];
  return _continue(_forAwaitOf(s, function (chunk) {
    chunks.push(chunk);
    if (chunk >= 2) {
      _interrupt = true;
    }
  }, function () {
    return _interrupt;
  }), function () {
    assert_array_equals(chunks, [1, 2]);
    var reader = s.getReader();
    return _awaitIgnored(reader.closed);
  });
}), 'Acquiring a reader after partially async-iterating a stream');
promise_test(_async(function () {
  var _interrupt2 = false;
  var s = new ReadableStream({
    start(c) {
      c.enqueue(1);
      c.enqueue(2);
      c.enqueue(3);
      c.close();
    }
  });

  // read the first two chunks, then release lock
  var chunks = [];
  return _continue(_forAwaitOf(s.values({
    preventCancel: true
  }), function (chunk) {
    chunks.push(chunk);
    if (chunk >= 2) {
      _interrupt2 = true;
    }
  }, function () {
    return _interrupt2;
  }), function () {
    assert_array_equals(chunks, [1, 2]);
    var reader = s.getReader();
    return _await(reader.read(), function (readResult) {
      assert_iter_result(readResult, 3, false);
      return _awaitIgnored(reader.closed);
    });
  });
}), 'Acquiring a reader and reading the remaining chunks after partially async-iterating a stream with preventCancel = true');
var _loop3 = function (_preventCancel) {
  test(() => {
    var rs = new ReadableStream();
    rs.values({
      preventCancel: _preventCancel
    }).return();
    // The test passes if this line doesn't throw.
    rs.getReader();
  }, `return() should unlock the stream synchronously when preventCancel = ${_preventCancel}`);
};
for (var _preventCancel of [false, true]) {
  _loop3(_preventCancel);
}
promise_test(_async(function () {
  var rs = new ReadableStream({
    start: _async(function (c) {
      c.enqueue('a');
      c.enqueue('b');
      c.enqueue('c');
      return _call(flushAsyncEvents, function () {
        // At this point, the async iterator has a read request in the stream's queue for its pending next() promise.
        // Closing the stream now causes two things to happen *synchronously*:
        //  1. ReadableStreamClose resolves reader.[[closedPromise]] with undefined.
        //  2. ReadableStreamClose calls the read request's close steps, which calls ReadableStreamReaderGenericRelease,
        //     which replaces reader.[[closedPromise]] with a rejected promise.
        c.close();
      });
    })
  });
  var chunks = [];
  return _continue(_forAwaitOf(rs, function (chunk) {
    chunks.push(chunk);
  }), function () {
    assert_array_equals(chunks, ['a', 'b', 'c']);
  });
}), 'close() while next() is pending');