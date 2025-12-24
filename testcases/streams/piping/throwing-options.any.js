// META: global=window,worker,shadowrealm
'use strict';

function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
var ThrowingOptions = /*#__PURE__*/function () {
  function ThrowingOptions(whatShouldThrow) {
    _classCallCheck(this, ThrowingOptions);
    this.whatShouldThrow = whatShouldThrow;
    this.touched = [];
  }
  return _createClass(ThrowingOptions, [{
    key: "preventClose",
    get: function () {
      this.maybeThrow('preventClose');
      return false;
    }
  }, {
    key: "preventAbort",
    get: function () {
      this.maybeThrow('preventAbort');
      return false;
    }
  }, {
    key: "preventCancel",
    get: function () {
      this.maybeThrow('preventCancel');
      return false;
    }
  }, {
    key: "signal",
    get: function () {
      this.maybeThrow('signal');
      return undefined;
    }
  }, {
    key: "maybeThrow",
    value: function maybeThrow(forWhat) {
      this.touched.push(forWhat);
      if (this.whatShouldThrow === forWhat) {
        throw new Error(this.whatShouldThrow);
      }
    }
  }]);
}();
var checkOrder = ['preventAbort', 'preventCancel', 'preventClose', 'signal'];
var _loop = function () {
  var whatShouldThrow = checkOrder[i];
  var whatShouldBeTouched = checkOrder.slice(0, i + 1);
  promise_test(t => {
    var options = new ThrowingOptions(whatShouldThrow);
    return promise_rejects_js(t, Error, new ReadableStream().pipeTo(new WritableStream(), options), 'pipeTo should reject').then(() => assert_array_equals(options.touched, whatShouldBeTouched, 'options should be touched in the right order'));
  }, `pipeTo should stop after getting ${whatShouldThrow} throws`);
  test(() => {
    var options = new ThrowingOptions(whatShouldThrow);
    assert_throws_js(Error, () => new ReadableStream().pipeThrough(new TransformStream(), options), 'pipeThrough should throw');
    assert_array_equals(options.touched, whatShouldBeTouched, 'options should be touched in the right order');
  }, `pipeThrough should stop after getting ${whatShouldThrow} throws`);
};
for (var i = 0; i < checkOrder.length; ++i) {
  _loop();
}