// META: global=window,worker,shadowrealm
'use strict';

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
function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  writer.releaseLock();
  assert_throws_js(TypeError, () => writer.desiredSize, 'desiredSize should throw a TypeError');
}, 'desiredSize on a released writer');
test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  assert_equals(writer.desiredSize, 1, 'desiredSize should be 1');
}, 'desiredSize initial value');
promise_test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  writer.close();
  return writer.closed.then(() => {
    assert_equals(writer.desiredSize, 0, 'desiredSize should be 0');
  });
}, 'desiredSize on a writer for a closed stream');
test(() => {
  var ws = new WritableStream({
    start(c) {
      c.error();
    }
  });
  var writer = ws.getWriter();
  assert_equals(writer.desiredSize, null, 'desiredSize should be null');
}, 'desiredSize on a writer for an errored stream');
test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  writer.close();
  writer.releaseLock();
  ws.getWriter();
}, 'ws.getWriter() on a closing WritableStream');
promise_test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  return writer.close().then(() => {
    writer.releaseLock();
    ws.getWriter();
  });
}, 'ws.getWriter() on a closed WritableStream');
test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  writer.abort();
  writer.releaseLock();
  ws.getWriter();
}, 'ws.getWriter() on an aborted WritableStream');
promise_test(() => {
  var ws = new WritableStream({
    start(c) {
      c.error();
    }
  });
  var writer = ws.getWriter();
  return writer.closed.then(v => assert_unreached('writer.closed fulfilled unexpectedly with: ' + v), () => {
    writer.releaseLock();
    ws.getWriter();
  });
}, 'ws.getWriter() on an errored WritableStream');
promise_test(() => {
  var ws = new WritableStream({});
  var writer = ws.getWriter();
  writer.releaseLock();
  return writer.closed.then(v => assert_unreached('writer.closed fulfilled unexpectedly with: ' + v), closedRejection => {
    assert_equals(closedRejection.name, 'TypeError', 'closed promise should reject with a TypeError');
    return writer.ready.then(v => assert_unreached('writer.ready fulfilled unexpectedly with: ' + v), readyRejection => assert_equals(readyRejection, closedRejection, 'ready promise should reject with the same error'));
  });
}, 'closed and ready on a released writer');
promise_test(t => {
  var thisObject = null;
  // Calls to Sink methods after the first are implicitly ignored. Only the first value that is passed to the resolver
  // is used.
  var Sink = /*#__PURE__*/function () {
    function Sink() {
      _classCallCheck(this, Sink);
    }
    return _createClass(Sink, [{
      key: "start",
      value: function start() {
        // Called twice
        t.step(() => {
          assert_equals(this, thisObject, 'start should be called as a method');
        });
      }
    }, {
      key: "write",
      value: function write() {
        t.step(() => {
          assert_equals(this, thisObject, 'write should be called as a method');
        });
      }
    }, {
      key: "close",
      value: function close() {
        t.step(() => {
          assert_equals(this, thisObject, 'close should be called as a method');
        });
      }
    }, {
      key: "abort",
      value: function abort() {
        t.step(() => {
          assert_equals(this, thisObject, 'abort should be called as a method');
        });
      }
    }]);
  }();
  var theSink = new Sink();
  thisObject = theSink;
  var ws = new WritableStream(theSink);
  var writer = ws.getWriter();
  writer.write('a');
  var closePromise = writer.close();
  var ws2 = new WritableStream(theSink);
  var writer2 = ws2.getWriter();
  var abortPromise = writer2.abort();
  return Promise.all([closePromise, abortPromise]);
}, 'WritableStream should call underlying sink methods as methods');
promise_test(t => {
  function functionWithOverloads() {}
  functionWithOverloads.apply = t.unreached_func('apply() should not be called');
  functionWithOverloads.call = t.unreached_func('call() should not be called');
  var underlyingSink = {
    start: functionWithOverloads,
    write: functionWithOverloads,
    close: functionWithOverloads,
    abort: functionWithOverloads
  };
  // Test start(), write(), close().
  var ws1 = new WritableStream(underlyingSink);
  var writer1 = ws1.getWriter();
  writer1.write('a');
  writer1.close();

  // Test abort().
  var abortError = new Error();
  abortError.name = 'abort error';
  var ws2 = new WritableStream(underlyingSink);
  var writer2 = ws2.getWriter();
  writer2.abort(abortError);

  // Test abort() with a close underlying sink method present. (Historical; see
  // https://github.com/whatwg/streams/issues/620#issuecomment-263483953 for what used to be
  // tested here. But more coverage can't hurt.)
  var ws3 = new WritableStream({
    start: functionWithOverloads,
    write: functionWithOverloads,
    close: functionWithOverloads
  });
  var writer3 = ws3.getWriter();
  writer3.abort(abortError);
  return writer1.closed.then(() => promise_rejects_exactly(t, abortError, writer2.closed, 'writer2.closed should be rejected')).then(() => promise_rejects_exactly(t, abortError, writer3.closed, 'writer3.closed should be rejected'));
}, 'methods should not not have .apply() or .call() called');
promise_test(() => {
  var strategy = {
    size() {
      if (this !== undefined) {
        throw new Error('size called as a method');
      }
      return 1;
    }
  };
  var ws = new WritableStream({}, strategy);
  var writer = ws.getWriter();
  return writer.write('a');
}, 'WritableStream\'s strategy.size should not be called as a method');
promise_test(() => {
  var ws = new WritableStream();
  var writer1 = ws.getWriter();
  assert_equals(undefined, writer1.releaseLock(), 'releaseLock() should return undefined');
  var writer2 = ws.getWriter();
  assert_equals(undefined, writer1.releaseLock(), 'no-op releaseLock() should return undefined');
  // Calling releaseLock() on writer1 should not interfere with writer2. If it did, then the ready promise would be
  // rejected.
  return writer2.ready;
}, 'redundant releaseLock() is no-op');
promise_test(() => {
  var events = [];
  var ws = new WritableStream();
  var writer = ws.getWriter();
  return writer.ready.then(() => {
    // Force the ready promise back to a pending state.
    var writerPromise = writer.write('dummy');
    var readyPromise = writer.ready.catch(() => events.push('ready'));
    var closedPromise = writer.closed.catch(() => events.push('closed'));
    writer.releaseLock();
    return Promise.all([readyPromise, closedPromise]).then(() => {
      assert_array_equals(events, ['ready', 'closed'], 'ready promise should fire before closed promise');
      // Stop the writer promise hanging around after the test has finished.
      return Promise.all([writerPromise, ws.abort()]);
    });
  });
}, 'ready promise should fire before closed on releaseLock');
test(() => {
  var Subclass = /*#__PURE__*/function (_WritableStream) {
    function Subclass() {
      _classCallCheck(this, Subclass);
      return _callSuper(this, Subclass, arguments);
    }
    _inherits(Subclass, _WritableStream);
    return _createClass(Subclass, [{
      key: "extraFunction",
      value: function extraFunction() {
        return true;
      }
    }]);
  }(/*#__PURE__*/_wrapNativeSuper(WritableStream));
  assert_equals(Object.getPrototypeOf(Subclass.prototype), WritableStream.prototype, 'Subclass.prototype\'s prototype should be WritableStream.prototype');
  assert_equals(Object.getPrototypeOf(Subclass), WritableStream, 'Subclass\'s prototype should be WritableStream');
  var sub = new Subclass();
  assert_true(sub instanceof WritableStream, 'Subclass object should be an instance of WritableStream');
  assert_true(sub instanceof Subclass, 'Subclass object should be an instance of Subclass');
  var lockedGetter = Object.getOwnPropertyDescriptor(WritableStream.prototype, 'locked').get;
  assert_equals(lockedGetter.call(sub), sub.locked, 'Subclass object should pass brand check');
  assert_true(sub.extraFunction(), 'extraFunction() should be present on Subclass object');
}, 'Subclassing WritableStream should work');
test(() => {
  var ws = new WritableStream();
  assert_false(ws.locked, 'stream should not be locked');
  ws.getWriter();
  assert_true(ws.locked, 'stream should be locked');
}, 'the locked getter should return true if the stream has a writer');