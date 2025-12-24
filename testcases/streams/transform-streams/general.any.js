// META: global=window,worker,shadowrealm
// META: script=../resources/test-utils.js
// META: script=../resources/rs-utils.js
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
test(() => {
  new TransformStream({
    transform() {}
  });
}, 'TransformStream can be constructed with a transform function');
test(() => {
  new TransformStream();
  new TransformStream({});
}, 'TransformStream can be constructed with no transform function');
test(() => {
  var ts = new TransformStream({
    transform() {}
  });
  var writer = ts.writable.getWriter();
  assert_equals(writer.desiredSize, 1, 'writer.desiredSize should be 1');
}, 'TransformStream writable starts in the writable state');
promise_test(() => {
  var ts = new TransformStream();
  var writer = ts.writable.getWriter();
  writer.write('a');
  assert_equals(writer.desiredSize, 0, 'writer.desiredSize should be 0 after write()');
  return ts.readable.getReader().read().then(result => {
    assert_equals(result.value, 'a', 'result from reading the readable is the same as was written to writable');
    assert_false(result.done, 'stream should not be done');
    return delay(0).then(() => assert_equals(writer.desiredSize, 1, 'desiredSize should be 1 again'));
  });
}, 'Identity TransformStream: can read from readable what is put into writable');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform(chunk) {
      c.enqueue(chunk.toUpperCase());
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  return ts.readable.getReader().read().then(result => {
    assert_equals(result.value, 'A', 'result from reading the readable is the transformation of what was written to writable');
    assert_false(result.done, 'stream should not be done');
  });
}, 'Uppercaser sync TransformStream: can read from readable transformed version of what is put into writable');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform(chunk) {
      c.enqueue(chunk.toUpperCase());
      c.enqueue(chunk.toUpperCase());
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  var reader = ts.readable.getReader();
  return reader.read().then(result1 => {
    assert_equals(result1.value, 'A', 'the first chunk read is the transformation of the single chunk written');
    assert_false(result1.done, 'stream should not be done');
    return reader.read().then(result2 => {
      assert_equals(result2.value, 'A', 'the second chunk read is also the transformation of the single chunk written');
      assert_false(result2.done, 'stream should not be done');
    });
  });
}, 'Uppercaser-doubler sync TransformStream: can read both chunks put into the readable');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform(chunk) {
      return delay(0).then(() => c.enqueue(chunk.toUpperCase()));
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  return ts.readable.getReader().read().then(result => {
    assert_equals(result.value, 'A', 'result from reading the readable is the transformation of what was written to writable');
    assert_false(result.done, 'stream should not be done');
  });
}, 'Uppercaser async TransformStream: can read from readable transformed version of what is put into writable');
promise_test(() => {
  var doSecondEnqueue;
  var returnFromTransform;
  var ts = new TransformStream({
    transform(chunk, controller) {
      delay(0).then(() => controller.enqueue(chunk.toUpperCase()));
      doSecondEnqueue = () => controller.enqueue(chunk.toUpperCase());
      return new Promise(resolve => {
        returnFromTransform = resolve;
      });
    }
  });
  var reader = ts.readable.getReader();
  var writer = ts.writable.getWriter();
  writer.write('a');
  return reader.read().then(result1 => {
    assert_equals(result1.value, 'A', 'the first chunk read is the transformation of the single chunk written');
    assert_false(result1.done, 'stream should not be done');
    doSecondEnqueue();
    return reader.read().then(result2 => {
      assert_equals(result2.value, 'A', 'the second chunk read is also the transformation of the single chunk written');
      assert_false(result2.done, 'stream should not be done');
      returnFromTransform();
    });
  });
}, 'Uppercaser-doubler async TransformStream: can read both chunks put into the readable');
promise_test(() => {
  var ts = new TransformStream({
    transform() {}
  });
  var writer = ts.writable.getWriter();
  writer.close();
  return Promise.all([writer.closed, ts.readable.getReader().closed]);
}, 'TransformStream: by default, closing the writable closes the readable (when there are no queued writes)');
promise_test(() => {
  var transformResolve;
  var transformPromise = new Promise(resolve => {
    transformResolve = resolve;
  });
  var ts = new TransformStream({
    transform() {
      return transformPromise;
    }
  }, undefined, {
    highWaterMark: 1
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  var rsClosed = false;
  ts.readable.getReader().closed.then(() => {
    rsClosed = true;
  });
  return delay(0).then(() => {
    assert_equals(rsClosed, false, 'readable is not closed after a tick');
    transformResolve();
    return writer.closed.then(() => {
      // TODO: Is this expectation correct?
      assert_equals(rsClosed, true, 'readable is closed at that point');
    });
  });
}, 'TransformStream: by default, closing the writable waits for transforms to finish before closing both');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform() {
      c.enqueue('x');
      c.enqueue('y');
      return delay(0);
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  var readableChunks = readableStreamToArray(ts.readable);
  return writer.closed.then(() => {
    return readableChunks.then(chunks => {
      assert_array_equals(chunks, ['x', 'y'], 'both enqueued chunks can be read from the readable');
    });
  });
}, 'TransformStream: by default, closing the writable closes the readable after sync enqueues and async done');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    start(controller) {
      c = controller;
    },
    transform() {
      return delay(0).then(() => c.enqueue('x')).then(() => c.enqueue('y')).then(() => delay(0));
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  var readableChunks = readableStreamToArray(ts.readable);
  return writer.closed.then(() => {
    return readableChunks.then(chunks => {
      assert_array_equals(chunks, ['x', 'y'], 'both enqueued chunks can be read from the readable');
    });
  });
}, 'TransformStream: by default, closing the writable closes the readable after async enqueues and async done');
promise_test(() => {
  var c;
  var ts = new TransformStream({
    suffix: '-suffix',
    start(controller) {
      c = controller;
      c.enqueue('start' + this.suffix);
    },
    transform(chunk) {
      c.enqueue(chunk + this.suffix);
    },
    flush() {
      c.enqueue('flushed' + this.suffix);
    }
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  var readableChunks = readableStreamToArray(ts.readable);
  return writer.closed.then(() => {
    return readableChunks.then(chunks => {
      assert_array_equals(chunks, ['start-suffix', 'a-suffix', 'flushed-suffix'], 'all enqueued chunks have suffixes');
    });
  });
}, 'Transform stream should call transformer methods as methods');
promise_test(() => {
  function functionWithOverloads() {}
  functionWithOverloads.apply = () => assert_unreached('apply() should not be called');
  functionWithOverloads.call = () => assert_unreached('call() should not be called');
  var ts = new TransformStream({
    start: functionWithOverloads,
    transform: functionWithOverloads,
    flush: functionWithOverloads
  });
  var writer = ts.writable.getWriter();
  writer.write('a');
  writer.close();
  return readableStreamToArray(ts.readable);
}, 'methods should not not have .apply() or .call() called');
promise_test(t => {
  var startCalled = false;
  var startDone = false;
  var transformDone = false;
  var flushDone = false;
  var ts = new TransformStream({
    start() {
      startCalled = true;
      return flushAsyncEvents().then(() => {
        startDone = true;
      });
    },
    transform() {
      return t.step(() => {
        assert_true(startDone, 'transform() should not be called until the promise returned from start() has resolved');
        return flushAsyncEvents().then(() => {
          transformDone = true;
        });
      });
    },
    flush() {
      return t.step(() => {
        assert_true(transformDone, 'flush() should not be called until the promise returned from transform() has resolved');
        return flushAsyncEvents().then(() => {
          flushDone = true;
        });
      });
    }
  }, undefined, {
    highWaterMark: 1
  });
  assert_true(startCalled, 'start() should be called synchronously');
  var writer = ts.writable.getWriter();
  var writePromise = writer.write('a');
  return writer.close().then(() => {
    assert_true(flushDone, 'promise returned from flush() should have resolved');
    return writePromise;
  });
}, 'TransformStream start, transform, and flush should be strictly ordered');
promise_test(() => {
  var transformCalled = false;
  var ts = new TransformStream({
    transform() {
      transformCalled = true;
    }
  }, undefined, {
    highWaterMark: Infinity
  });
  // transform() is only called synchronously when there is no backpressure and all microtasks have run.
  return delay(0).then(() => {
    var writePromise = ts.writable.getWriter().write();
    assert_true(transformCalled, 'transform() should have been called');
    return writePromise;
  });
}, 'it should be possible to call transform() synchronously');
promise_test(() => {
  var ts = new TransformStream({}, undefined, {
    highWaterMark: 0
  });
  var writer = ts.writable.getWriter();
  writer.close();
  return Promise.all([writer.closed, ts.readable.getReader().closed]);
}, 'closing the writable should close the readable when there are no queued chunks, even with backpressure');
test(() => {
  new TransformStream({
    start(controller) {
      controller.terminate();
      assert_throws_js(TypeError, () => controller.enqueue(), 'enqueue should throw');
    }
  });
}, 'enqueue() should throw after controller.terminate()');
promise_test(() => {
  var controller;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    }
  });
  var cancelPromise = ts.readable.cancel();
  assert_throws_js(TypeError, () => controller.enqueue(), 'enqueue should throw');
  return cancelPromise;
}, 'enqueue() should throw after readable.cancel()');
test(() => {
  new TransformStream({
    start(controller) {
      controller.terminate();
      controller.terminate();
    }
  });
}, 'controller.terminate() should do nothing the second time it is called');
promise_test(t => {
  var controller;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    }
  });
  var cancelReason = {
    name: 'cancelReason'
  };
  var cancelPromise = ts.readable.cancel(cancelReason);
  controller.terminate();
  return Promise.all([cancelPromise, promise_rejects_js(t, TypeError, ts.writable.getWriter().closed, 'closed should reject with TypeError')]);
}, 'terminate() should abort writable immediately after readable.cancel()');
promise_test(t => {
  var controller;
  var ts = new TransformStream({
    start(c) {
      controller = c;
    }
  });
  var cancelReason = {
    name: 'cancelReason'
  };
  return ts.readable.cancel(cancelReason).then(() => {
    controller.terminate();
    return promise_rejects_exactly(t, cancelReason, ts.writable.getWriter().closed, 'closed should reject with TypeError');
  });
}, 'terminate() should do nothing after readable.cancel() resolves');
promise_test(() => {
  var calls = 0;
  new TransformStream({
    start() {
      ++calls;
    }
  });
  return flushAsyncEvents().then(() => {
    assert_equals(calls, 1, 'start() should have been called exactly once');
  });
}, 'start() should not be called twice');
test(() => {
  assert_throws_js(RangeError, () => new TransformStream({
    readableType: 'bytes'
  }), 'constructor should throw');
}, 'specifying a defined readableType should throw');
test(() => {
  assert_throws_js(RangeError, () => new TransformStream({
    writableType: 'bytes'
  }), 'constructor should throw');
}, 'specifying a defined writableType should throw');
test(() => {
  var Subclass = /*#__PURE__*/function (_TransformStream) {
    function Subclass() {
      _classCallCheck(this, Subclass);
      return _callSuper(this, Subclass, arguments);
    }
    _inherits(Subclass, _TransformStream);
    return _createClass(Subclass, [{
      key: "extraFunction",
      value: function extraFunction() {
        return true;
      }
    }]);
  }(/*#__PURE__*/_wrapNativeSuper(TransformStream));
  assert_equals(Object.getPrototypeOf(Subclass.prototype), TransformStream.prototype, 'Subclass.prototype\'s prototype should be TransformStream.prototype');
  assert_equals(Object.getPrototypeOf(Subclass), TransformStream, 'Subclass\'s prototype should be TransformStream');
  var sub = new Subclass();
  assert_true(sub instanceof TransformStream, 'Subclass object should be an instance of TransformStream');
  assert_true(sub instanceof Subclass, 'Subclass object should be an instance of Subclass');
  var readableGetter = Object.getOwnPropertyDescriptor(TransformStream.prototype, 'readable').get;
  assert_equals(readableGetter.call(sub), sub.readable, 'Subclass object should pass brand check');
  assert_true(sub.extraFunction(), 'extraFunction() should be present on Subclass object');
}, 'Subclassing TransformStream should work');