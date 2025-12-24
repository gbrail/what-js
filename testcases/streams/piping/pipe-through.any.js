// META: global=window,worker,shadowrealm
// META: script=../resources/rs-utils.js
// META: script=../resources/test-utils.js
// META: script=../resources/recording-streams.js
'use strict';

function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
function _callSuper(t, o, e) { return o = _getPrototypeOf(o), _possibleConstructorReturn(t, _isNativeReflectConstruct() ? Reflect.construct(o, e || [], _getPrototypeOf(t).constructor) : o.apply(t, e)); }
function _possibleConstructorReturn(t, e) { if (e && ("object" == typeof e || "function" == typeof e)) return e; if (void 0 !== e) throw new TypeError("Derived constructors may only return object or undefined"); return _assertThisInitialized(t); }
function _assertThisInitialized(e) { if (void 0 === e) throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); return e; }
function _inherits(t, e) { if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function"); t.prototype = Object.create(e && e.prototype, { constructor: { value: t, writable: !0, configurable: !0 } }), Object.defineProperty(t, "prototype", { writable: !1 }), e && _setPrototypeOf(t, e); }
function _wrapNativeSuper(t) { var r = "function" == typeof Map ? new Map() : void 0; return _wrapNativeSuper = function (t) { if (null === t || !_isNativeFunction(t)) return t; if ("function" != typeof t) throw new TypeError("Super expression must either be null or a function"); if (void 0 !== r) { if (r.has(t)) return r.get(t); r.set(t, Wrapper); } function Wrapper() { return _construct(t, arguments, _getPrototypeOf(this).constructor); } return Wrapper.prototype = Object.create(t.prototype, { constructor: { value: Wrapper, enumerable: !1, writable: !0, configurable: !0 } }), _setPrototypeOf(Wrapper, t); }, _wrapNativeSuper(t); }
function _construct(t, e, r) { if (_isNativeReflectConstruct()) return Reflect.construct.apply(null, arguments); var o = [null]; o.push.apply(o, e); var p = new (t.bind.apply(t, o))(); return r && _setPrototypeOf(p, r.prototype), p; }
function _isNativeReflectConstruct() { try { var t = !Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {})); } catch (t) {} return (_isNativeReflectConstruct = function () { return !!t; })(); }
function _isNativeFunction(t) { try { return -1 !== Function.toString.call(t).indexOf("[native code]"); } catch (n) { return "function" == typeof t; } }
function _setPrototypeOf(t, e) { return _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function (t, e) { return t.__proto__ = e, t; }, _setPrototypeOf(t, e); }
function _getPrototypeOf(t) { return _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function (t) { return t.__proto__ || Object.getPrototypeOf(t); }, _getPrototypeOf(t); }
function duckTypedPassThroughTransform() {
  var enqueueInReadable;
  var closeReadable;
  return {
    writable: new WritableStream({
      write(chunk) {
        enqueueInReadable(chunk);
      },
      close() {
        closeReadable();
      }
    }),
    readable: new ReadableStream({
      start(c) {
        enqueueInReadable = c.enqueue.bind(c);
        closeReadable = c.close.bind(c);
      }
    })
  };
}
function uninterestingReadableWritablePair() {
  return {
    writable: new WritableStream(),
    readable: new ReadableStream()
  };
}
promise_test(() => {
  var readableEnd = sequentialReadableStream(5).pipeThrough(duckTypedPassThroughTransform());
  return readableStreamToArray(readableEnd).then(chunks => assert_array_equals(chunks, [1, 2, 3, 4, 5]), 'chunks should match');
}, 'Piping through a duck-typed pass-through transform stream should work');
promise_test(() => {
  var transform = {
    writable: new WritableStream({
      start(c) {
        c.error(new Error('this rejection should not be reported as unhandled'));
      }
    }),
    readable: new ReadableStream()
  };
  sequentialReadableStream(5).pipeThrough(transform);

  // The test harness should complain about unhandled rejections by then.
  return flushAsyncEvents();
}, 'Piping through a transform errored on the writable end does not cause an unhandled promise rejection');
test(() => {
  var calledPipeTo = false;
  var BadReadableStream = /*#__PURE__*/function (_ReadableStream) {
    function BadReadableStream() {
      _classCallCheck(this, BadReadableStream);
      return _callSuper(this, BadReadableStream, arguments);
    }
    _inherits(BadReadableStream, _ReadableStream);
    return _createClass(BadReadableStream, [{
      key: "pipeTo",
      value: function pipeTo() {
        calledPipeTo = true;
      }
    }]);
  }(/*#__PURE__*/_wrapNativeSuper(ReadableStream));
  var brs = new BadReadableStream({
    start(controller) {
      controller.close();
    }
  });
  var readable = new ReadableStream();
  var writable = new WritableStream();
  var result = brs.pipeThrough({
    readable,
    writable
  });
  assert_false(calledPipeTo, 'the overridden pipeTo should not have been called');
  assert_equals(result, readable, 'return value should be the passed readable property');
}, 'pipeThrough should not call pipeTo on this');
test(t => {
  var calledFakePipeTo = false;
  var realPipeTo = ReadableStream.prototype.pipeTo;
  t.add_cleanup(() => {
    ReadableStream.prototype.pipeTo = realPipeTo;
  });
  ReadableStream.prototype.pipeTo = () => {
    calledFakePipeTo = true;
  };
  var rs = new ReadableStream();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  var result = rs.pipeThrough({
    readable,
    writable
  });
  assert_false(calledFakePipeTo, 'the monkey-patched pipeTo should not have been called');
  assert_equals(result, readable, 'return value should be the passed readable property');
}, 'pipeThrough should not call pipeTo on the ReadableStream prototype');
var badReadables = [null, undefined, 0, NaN, true, 'ReadableStream', Object.create(ReadableStream.prototype)];
var _loop = function (readable) {
  test(() => {
    assert_throws_js(TypeError, ReadableStream.prototype.pipeThrough.bind(readable, uninterestingReadableWritablePair()), 'pipeThrough should throw');
  }, `pipeThrough should brand-check this and not allow '${readable}'`);
  test(() => {
    var rs = new ReadableStream();
    var writableGetterCalled = false;
    assert_throws_js(TypeError, () => rs.pipeThrough({
      get writable() {
        writableGetterCalled = true;
        return new WritableStream();
      },
      readable
    }), 'pipeThrough should brand-check readable');
    assert_false(writableGetterCalled, 'writable should not have been accessed');
  }, `pipeThrough should brand-check readable and not allow '${readable}'`);
};
for (var readable of badReadables) {
  _loop(readable);
}
var badWritables = [null, undefined, 0, NaN, true, 'WritableStream', Object.create(WritableStream.prototype)];
var _loop2 = function (writable) {
  test(() => {
    var rs = new ReadableStream({
      start(c) {
        c.close();
      }
    });
    var readableGetterCalled = false;
    assert_throws_js(TypeError, () => rs.pipeThrough({
      get readable() {
        readableGetterCalled = true;
        return new ReadableStream();
      },
      writable
    }), 'pipeThrough should brand-check writable');
    assert_true(readableGetterCalled, 'readable should have been accessed');
  }, `pipeThrough should brand-check writable and not allow '${writable}'`);
};
for (var writable of badWritables) {
  _loop2(writable);
}
test(t => {
  var error = new Error();
  error.name = 'custom';
  var rs = new ReadableStream({
    pull: t.unreached_func('pull should not be called')
  }, {
    highWaterMark: 0
  });
  var throwingWritable = {
    readable: rs,
    get writable() {
      throw error;
    }
  };
  assert_throws_exactly(error, () => ReadableStream.prototype.pipeThrough.call(rs, throwingWritable, {}), 'pipeThrough should rethrow the error thrown by the writable getter');
  var throwingReadable = {
    get readable() {
      throw error;
    },
    writable: {}
  };
  assert_throws_exactly(error, () => ReadableStream.prototype.pipeThrough.call(rs, throwingReadable, {}), 'pipeThrough should rethrow the error thrown by the readable getter');
}, 'pipeThrough should rethrow errors from accessing readable or writable');
var badSignals = [null, 0, NaN, true, 'AbortSignal', Object.create(AbortSignal.prototype)];
var _loop3 = function (signal) {
  test(() => {
    var rs = new ReadableStream();
    assert_throws_js(TypeError, () => rs.pipeThrough(uninterestingReadableWritablePair(), {
      signal
    }), 'pipeThrough should throw');
  }, `invalid values of signal should throw; specifically '${signal}'`);
};
for (var signal of badSignals) {
  _loop3(signal);
}
test(() => {
  var rs = new ReadableStream();
  var controller = new AbortController();
  var signal = controller.signal;
  rs.pipeThrough(uninterestingReadableWritablePair(), {
    signal
  });
}, 'pipeThrough should accept a real AbortSignal');
test(() => {
  var rs = new ReadableStream();
  rs.getReader();
  assert_throws_js(TypeError, () => rs.pipeThrough(uninterestingReadableWritablePair()), 'pipeThrough should throw');
}, 'pipeThrough should throw if this is locked');
test(() => {
  var rs = new ReadableStream();
  var writable = new WritableStream();
  var readable = new ReadableStream();
  writable.getWriter();
  assert_throws_js(TypeError, () => rs.pipeThrough({
    writable,
    readable
  }), 'pipeThrough should throw');
}, 'pipeThrough should throw if writable is locked');
test(() => {
  var rs = new ReadableStream();
  var writable = new WritableStream();
  var readable = new ReadableStream();
  readable.getReader();
  assert_equals(rs.pipeThrough({
    writable,
    readable
  }), readable, 'pipeThrough should not throw');
}, 'pipeThrough should not care if readable is locked');
promise_test(() => {
  var rs = recordingReadableStream();
  var writable = new WritableStream({
    start(controller) {
      controller.error();
    }
  });
  var readable = new ReadableStream();
  rs.pipeThrough({
    writable,
    readable
  }, {
    preventCancel: true
  });
  return flushAsyncEvents(0).then(() => {
    assert_array_equals(rs.events, ['pull'], 'cancel should not have been called');
  });
}, 'preventCancel should work');
promise_test(() => {
  var rs = new ReadableStream({
    start(controller) {
      controller.close();
    }
  });
  var writable = recordingWritableStream();
  var readable = new ReadableStream();
  rs.pipeThrough({
    writable,
    readable
  }, {
    preventClose: true
  });
  return flushAsyncEvents(0).then(() => {
    assert_array_equals(writable.events, [], 'writable should not be closed');
  });
}, 'preventClose should work');
promise_test(() => {
  var rs = new ReadableStream({
    start(controller) {
      controller.error();
    }
  });
  var writable = recordingWritableStream();
  var readable = new ReadableStream();
  rs.pipeThrough({
    writable,
    readable
  }, {
    preventAbort: true
  });
  return flushAsyncEvents(0).then(() => {
    assert_array_equals(writable.events, [], 'writable should not be aborted');
  });
}, 'preventAbort should work');
test(() => {
  var rs = new ReadableStream();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  assert_throws_js(TypeError, () => rs.pipeThrough({
    readable,
    writable
  }, {
    get preventAbort() {
      writable.getWriter();
    }
  }), 'pipeThrough should throw');
}, 'pipeThrough() should throw if an option getter grabs a writer');
test(() => {
  var rs = new ReadableStream();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  rs.pipeThrough({
    readable,
    writable
  }, null);
}, 'pipeThrough() should not throw if option is null');
test(() => {
  var rs = new ReadableStream();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  rs.pipeThrough({
    readable,
    writable
  }, {
    signal: undefined
  });
}, 'pipeThrough() should not throw if signal is undefined');
function tryPipeThrough(pair, options) {
  var rs = new ReadableStream();
  if (!pair) pair = {
    readable: new ReadableStream(),
    writable: new WritableStream()
  };
  try {
    rs.pipeThrough(pair, options);
  } catch (e) {
    return e;
  }
}
test(() => {
  var result = tryPipeThrough({
    get readable() {
      return new ReadableStream();
    },
    get writable() {
      throw "writable threw";
    }
  }, {});
  assert_equals(result, "writable threw");
  result = tryPipeThrough({
    get readable() {
      throw "readable threw";
    },
    get writable() {
      throw "writable threw";
    }
  }, {});
  assert_equals(result, "readable threw");
  result = tryPipeThrough({
    get readable() {
      throw "readable threw";
    },
    get writable() {
      throw "writable threw";
    }
  }, {
    get preventAbort() {
      throw "preventAbort threw";
    }
  });
  assert_equals(result, "readable threw");
}, 'pipeThrough() should throw if readable/writable getters throw');