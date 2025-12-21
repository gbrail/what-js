// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
'use strict';

function _empty() {}
var iterableFactories = [['an array of values', () => {
  return ['a', 'b'];
}], ['an array of promises', () => {
  return [Promise.resolve('a'), Promise.resolve('b')];
}], ['an array iterator', () => {
  return ['a', 'b'][Symbol.iterator]();
}], ['a string', () => {
  // This iterates over the code points of the string.
  return 'ab';
}], ['a Set', () => {
  return new Set(['a', 'b']);
}], ['a Set iterator', () => {
  return new Set(['a', 'b'])[Symbol.iterator]();
}], ['a sync generator', () => {
  function* syncGenerator() {
    yield 'a';
    yield 'b';
  }
  return syncGenerator();
}], ['an async generator', () => {
  var asyncGenerator = function () {
    return new _AsyncGenerator(function (_generator) {
      return _generator._yield('a').then(function () {
        return _generator._yield('b').then(_empty);
      });
    });
  };
  return asyncGenerator();
}], ['a sync iterable of values', () => {
  var chunks = ['a', 'b'];
  var iterator = {
    next() {
      return {
        done: chunks.length === 0,
        value: chunks.shift()
      };
    }
  };
  var iterable = {
    [Symbol.iterator]: () => iterator
  };
  return iterable;
}], ['a sync iterable of promises', () => {
  var chunks = ['a', 'b'];
  var iterator = {
    next() {
      return chunks.length === 0 ? {
        done: true
      } : {
        done: false,
        value: Promise.resolve(chunks.shift())
      };
    }
  };
  var iterable = {
    [Symbol.iterator]: () => iterator
  };
  return iterable;
}], ['an async iterable', () => {
  var chunks = ['a', 'b'];
  var asyncIterator = {
    next() {
      return Promise.resolve({
        done: chunks.length === 0,
        value: chunks.shift()
      });
    }
  };
  var asyncIterable = {
    [Symbol.asyncIterator]: () => asyncIterator
  };
  return asyncIterable;
}], ['a ReadableStream', () => {
  return new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.close();
    }
  });
}], ['a ReadableStream async iterator', () => {
  return new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.close();
    }
  })[Symbol.asyncIterator]();
}]];
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
var _loop = function (factory) {
  promise_test(_async(function () {
    var iterable = factory();
    var rs = ReadableStream.from(iterable);
    assert_equals(rs.constructor, ReadableStream, 'from() should return a ReadableStream');
    var reader = rs.getReader();
    var _assert_object_equals = assert_object_equals;
    return _await(reader.read(), function (_reader$read) {
      _assert_object_equals(_reader$read, {
        value: 'a',
        done: false
      }, 'first read should be correct');
      var _assert_object_equals2 = assert_object_equals;
      return _await(reader.read(), function (_reader$read2) {
        _assert_object_equals2(_reader$read2, {
          value: 'b',
          done: false
        }, 'second read should be correct');
        var _assert_object_equals3 = assert_object_equals;
        return _await(reader.read(), function (_reader$read3) {
          _assert_object_equals3(_reader$read3, {
            value: undefined,
            done: true
          }, 'third read should be done');
          return _awaitIgnored(reader.closed);
        });
      });
    });
  }), `ReadableStream.from accepts ${label}`);
};
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
  }();
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
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
for (var [label, factory] of iterableFactories) {
  _loop(factory);
}
var badIterables = [['null', null], ['undefined', undefined], ['0', 0], ['NaN', NaN], ['true', true], ['{}', {}], ['Object.create(null)', Object.create(null)], ['a function', () => 42], ['a symbol', Symbol()], ['an object with a non-callable @@iterator method', {
  [Symbol.iterator]: 42
}], ['an object with a non-callable @@asyncIterator method', {
  [Symbol.asyncIterator]: 42
}], ['an object with an @@iterator method returning a non-object', {
  [Symbol.iterator]: () => 42
}], ['an object with an @@asyncIterator method returning a non-object', {
  [Symbol.asyncIterator]: () => 42
}]];
var _loop2 = function (iterable) {
  test(() => {
    assert_throws_js(TypeError, () => ReadableStream.from(iterable), 'from() should throw a TypeError');
  }, `ReadableStream.from throws on invalid iterables; specifically ${_label}`);
};
for (var [_label, iterable] of badIterables) {
  _loop2(iterable);
}
test(() => {
  var theError = new Error('a unique string');
  var iterable = {
    [Symbol.iterator]() {
      throw theError;
    }
  };
  assert_throws_exactly(theError, () => ReadableStream.from(iterable), 'from() should re-throw the error');
}, `ReadableStream.from re-throws errors from calling the @@iterator method`);
test(() => {
  var theError = new Error('a unique string');
  var iterable = {
    [Symbol.asyncIterator]() {
      throw theError;
    }
  };
  assert_throws_exactly(theError, () => ReadableStream.from(iterable), 'from() should re-throw the error');
}, `ReadableStream.from re-throws errors from calling the @@asyncIterator method`);
test(t => {
  var theError = new Error('a unique string');
  var iterable = {
    [Symbol.iterator]: t.unreached_func('@@iterator should not be called'),
    [Symbol.asyncIterator]() {
      throw theError;
    }
  };
  assert_throws_exactly(theError, () => ReadableStream.from(iterable), 'from() should re-throw the error');
}, `ReadableStream.from ignores @@iterator if @@asyncIterator exists`);
test(() => {
  var theError = new Error('a unique string');
  var iterable = {
    [Symbol.asyncIterator]: null,
    [Symbol.iterator]() {
      throw theError;
    }
  };
  assert_throws_exactly(theError, () => ReadableStream.from(iterable), 'from() should re-throw the error');
}, `ReadableStream.from ignores a null @@asyncIterator`);
promise_test(_async(function () {
  var iterable = {
    next: _async(function () {
      return {
        value: undefined,
        done: true
      };
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(reader.read(), function (read) {
    assert_object_equals(read, {
      value: undefined,
      done: true
    }, 'first read should be done');
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from accepts an empty iterable`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var iterable = {
    next: _async(function () {
      throw theError;
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader.read()), promise_rejects_exactly(t, theError, reader.closed)]));
}), `ReadableStream.from: stream errors when next() rejects`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var iterable = {
    next() {
      throw theError;
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader.read()), promise_rejects_exactly(t, theError, reader.closed)]));
}), 'ReadableStream.from: stream errors when next() throws synchronously');
promise_test(_async(function (t) {
  var iterable = {
    next() {
      return 42; // not a promise or an iterator result
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.all([promise_rejects_js(t, TypeError, reader.read()), promise_rejects_js(t, TypeError, reader.closed)]));
}), 'ReadableStream.from: stream errors when next() returns a non-object');
promise_test(_async(function (t) {
  var iterable = {
    next() {
      return Promise.resolve(42); // not an iterator result
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.all([promise_rejects_js(t, TypeError, reader.read()), promise_rejects_js(t, TypeError, reader.closed)]));
}), 'ReadableStream.from: stream errors when next() fulfills with a non-object');
promise_test(_async(function (t) {
  var iterable = {
    next() {
      return new Promise(() => {});
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.race([reader.read().then(t.unreached_func('read() should not resolve'), t.unreached_func('read() should not reject')), reader.closed.then(t.unreached_func('closed should not resolve'), t.unreached_func('closed should not reject')), flushAsyncEvents()]));
}), 'ReadableStream.from: stream stalls when next() never settles');
promise_test(_async(function () {
  var nextCalls = 0;
  var nextArgs;
  var iterable = {
    next: _async(function (...args) {
      nextCalls += 1;
      nextArgs = args;
      return {
        value: 'a',
        done: false
      };
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _call(flushAsyncEvents, function () {
    assert_equals(nextCalls, 0, 'next() should not be called yet');
    return _await(reader.read(), function (read) {
      assert_object_equals(read, {
        value: 'a',
        done: false
      }, 'first read should be correct');
      assert_equals(nextCalls, 1, 'next() should be called after first read()');
      assert_array_equals(nextArgs, [], 'next() should be called with no arguments');
    });
  });
}), `ReadableStream.from: calls next() after first read()`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var returnCalls = 0;
  var returnArgs;
  var resolveReturn;
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    return: _async(function (...args) {
      returnCalls += 1;
      returnArgs = args;
      return _await(new Promise(r => resolveReturn = r), function () {
        return {
          done: true
        };
      });
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  assert_equals(returnCalls, 0, 'return() should not be called yet');
  var cancelResolved = false;
  var cancelPromise = reader.cancel(theError).then(() => {
    cancelResolved = true;
  });
  return _call(flushAsyncEvents, function () {
    assert_equals(returnCalls, 1, 'return() should be called');
    assert_array_equals(returnArgs, [theError], 'return() should be called with cancel reason');
    assert_false(cancelResolved, 'cancel() should not resolve while promise from return() is pending');
    resolveReturn();
    return _awaitIgnored(Promise.all([cancelPromise, reader.closed]));
  });
}), `ReadableStream.from: cancelling the returned stream calls and awaits return()`);
promise_test(_async(function (t) {
  var nextCalls = 0;
  var returnCalls = 0;
  var iterable = {
    next: _async(function () {
      nextCalls += 1;
      return {
        value: undefined,
        done: true
      };
    }),
    throw: t.unreached_func('throw() should not be called'),
    return: function () {
      returnCalls += 1;
      return _await();
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(reader.read(), function (read) {
    assert_object_equals(read, {
      value: undefined,
      done: true
    }, 'first read should be done');
    assert_equals(nextCalls, 1, 'next() should be called once');
    return _await(reader.closed, function () {
      assert_equals(returnCalls, 0, 'return() should not be called');
    });
  });
}), `ReadableStream.from: return() is not called when iterator completes normally`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    // no return method
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _awaitIgnored(Promise.all([reader.cancel(theError), reader.closed]));
}), `ReadableStream.from: cancel() resolves when return() method is missing`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    return: 42,
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(promise_rejects_js(t, TypeError, reader.cancel(theError), 'cancel() should reject with a TypeError'), function () {
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: cancel() rejects when return() is not a method`);
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var rejectError = new Error('reject error');
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    return: _async(function () {
      throw rejectError;
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(promise_rejects_exactly(t, rejectError, reader.cancel(cancelReason), 'cancel() should reject with error from return()'), function () {
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: cancel() rejects when return() rejects`);
promise_test(_async(function (t) {
  var cancelReason = new Error('cancel reason');
  var rejectError = new Error('reject error');
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    return() {
      throw rejectError;
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(promise_rejects_exactly(t, rejectError, reader.cancel(cancelReason), 'cancel() should reject with error from return()'), function () {
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: cancel() rejects when return() throws synchronously`);
promise_test(_async(function (t) {
  var theError = new Error('a unique string');
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    throw: t.unreached_func('throw() should not be called'),
    return: function () {
      return _await(42);
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  var reader = rs.getReader();
  return _await(promise_rejects_js(t, TypeError, reader.cancel(theError), 'cancel() should reject with a TypeError'), function () {
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: cancel() rejects when return() fulfills with a non-object`);
promise_test(_async(function () {
  var nextCalls = 0;
  var reader;
  var values = ['a', 'b', 'c'];
  var iterable = {
    next: _async(function () {
      nextCalls += 1;
      if (nextCalls === 1) {
        reader.read();
      }
      return {
        value: values.shift(),
        done: false
      };
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  reader = rs.getReader();
  return _await(reader.read(), function (read1) {
    assert_object_equals(read1, {
      value: 'a',
      done: false
    }, 'first read should be correct');
    return _call(flushAsyncEvents, function () {
      assert_equals(nextCalls, 2, 'next() should be called two times');
      return _await(reader.read(), function (read2) {
        assert_object_equals(read2, {
          value: 'c',
          done: false
        }, 'second read should be correct');
        assert_equals(nextCalls, 3, 'next() should be called three times');
      });
    });
  });
}), `ReadableStream.from: reader.read() inside next()`);
promise_test(_async(function () {
  var nextCalls = 0;
  var returnCalls = 0;
  var reader;
  var iterable = {
    next: _async(function () {
      nextCalls++;
      return _await(reader.cancel(), function () {
        assert_equals(returnCalls, 1, 'return() should be called once');
        return {
          value: 'something else',
          done: false
        };
      });
    }),
    return: function () {
      returnCalls++;
      return _await();
    },
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  reader = rs.getReader();
  return _await(reader.read(), function (read) {
    assert_object_equals(read, {
      value: undefined,
      done: true
    }, 'first read should be done');
    assert_equals(nextCalls, 1, 'next() should be called once');
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: reader.cancel() inside next()`);
promise_test(_async(function (t) {
  var returnCalls = 0;
  var reader;
  var iterable = {
    next: t.unreached_func('next() should not be called'),
    return: _async(function () {
      returnCalls++;
      return _await(reader.cancel(), function () {
        return {
          done: true
        };
      });
    }),
    [Symbol.asyncIterator]: () => iterable
  };
  var rs = ReadableStream.from(iterable);
  reader = rs.getReader();
  return _await(reader.cancel(), function () {
    assert_equals(returnCalls, 1, 'return() should be called once');
    return _awaitIgnored(reader.closed);
  });
}), `ReadableStream.from: reader.cancel() inside return()`);
promise_test(_async(function (t) {
  var array = ['a', 'b'];
  var rs = ReadableStream.from(array);
  var reader = rs.getReader();
  return _await(reader.read(), function (read1) {
    assert_object_equals(read1, {
      value: 'a',
      done: false
    }, 'first read should be correct');
    return _await(reader.read(), function (read2) {
      assert_object_equals(read2, {
        value: 'b',
        done: false
      }, 'second read should be correct');
      array.push('c');
      return _await(reader.read(), function (read3) {
        assert_object_equals(read3, {
          value: 'c',
          done: false
        }, 'third read after push() should be correct');
        return _await(reader.read(), function (read4) {
          assert_object_equals(read4, {
            value: undefined,
            done: true
          }, 'fourth read should be done');
          return _awaitIgnored(reader.closed);
        });
      });
    });
  });
}), `ReadableStream.from(array), push() to array while reading`);