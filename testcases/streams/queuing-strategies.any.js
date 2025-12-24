// META: global=window,worker,shadowrealm
'use strict';

function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
function _callSuper(t, o, e) { return o = _getPrototypeOf(o), _possibleConstructorReturn(t, _isNativeReflectConstruct() ? Reflect.construct(o, e || [], _getPrototypeOf(t).constructor) : o.apply(t, e)); }
function _possibleConstructorReturn(t, e) { if (e && ("object" == typeof e || "function" == typeof e)) return e; if (void 0 !== e) throw new TypeError("Derived constructors may only return object or undefined"); return _assertThisInitialized(t); }
function _assertThisInitialized(e) { if (void 0 === e) throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); return e; }
function _isNativeReflectConstruct() { try { var t = !Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {})); } catch (t) {} return (_isNativeReflectConstruct = function () { return !!t; })(); }
function _getPrototypeOf(t) { return _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function (t) { return t.__proto__ || Object.getPrototypeOf(t); }, _getPrototypeOf(t); }
function _inherits(t, e) { if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function"); t.prototype = Object.create(e && e.prototype, { constructor: { value: t, writable: !0, configurable: !0 } }), Object.defineProperty(t, "prototype", { writable: !1 }), e && _setPrototypeOf(t, e); }
function _setPrototypeOf(t, e) { return _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function (t, e) { return t.__proto__ = e, t; }, _setPrototypeOf(t, e); }
var highWaterMarkConversions = new Map([[-Infinity, -Infinity], [-5, -5], [false, 0], [true, 1], [NaN, NaN], ['foo', NaN], ['0', 0], [{}, NaN], [() => {}, NaN]]);
var _loop = function (QueuingStrategy) {
  test(() => {
    new QueuingStrategy({
      highWaterMark: 4
    });
  }, `${QueuingStrategy.name}: Can construct a with a valid high water mark`);
  test(() => {
    var highWaterMark = 1;
    var highWaterMarkObjectGetter = {
      get highWaterMark() {
        return highWaterMark;
      }
    };
    var error = new Error('wow!');
    var highWaterMarkObjectGetterThrowing = {
      get highWaterMark() {
        throw error;
      }
    };
    assert_throws_js(TypeError, () => new QueuingStrategy(), 'construction fails with undefined');
    assert_throws_js(TypeError, () => new QueuingStrategy(null), 'construction fails with null');
    assert_throws_js(TypeError, () => new QueuingStrategy(true), 'construction fails with true');
    assert_throws_js(TypeError, () => new QueuingStrategy(5), 'construction fails with 5');
    assert_throws_js(TypeError, () => new QueuingStrategy({}), 'construction fails with {}');
    assert_throws_exactly(error, () => new QueuingStrategy(highWaterMarkObjectGetterThrowing), 'construction fails with an object with a throwing highWaterMark getter');
    assert_equals(new QueuingStrategy(highWaterMarkObjectGetter).highWaterMark, highWaterMark);
  }, `${QueuingStrategy.name}: Constructor behaves as expected with strange arguments`);
  test(() => {
    for (var [input, output] of highWaterMarkConversions.entries()) {
      var strategy = new QueuingStrategy({
        highWaterMark: input
      });
      assert_equals(strategy.highWaterMark, output, `${input} gets set correctly`);
    }
  }, `${QueuingStrategy.name}: highWaterMark constructor values are converted per the unrestricted double rules`);
  test(() => {
    var size1 = new QueuingStrategy({
      highWaterMark: 5
    }).size;
    var size2 = new QueuingStrategy({
      highWaterMark: 10
    }).size;
    assert_equals(size1, size2);
  }, `${QueuingStrategy.name}: size is the same function across all instances`);
  test(() => {
    var size = new QueuingStrategy({
      highWaterMark: 5
    }).size;
    assert_equals(size.name, 'size');
  }, `${QueuingStrategy.name}: size should have the right name`);
  test(() => {
    var SubClass = /*#__PURE__*/function (_QueuingStrategy) {
      function SubClass() {
        _classCallCheck(this, SubClass);
        return _callSuper(this, SubClass, arguments);
      }
      _inherits(SubClass, _QueuingStrategy);
      return _createClass(SubClass, [{
        key: "size",
        value: function size() {
          return 2;
        }
      }, {
        key: "subClassMethod",
        value: function subClassMethod() {
          return true;
        }
      }]);
    }(QueuingStrategy);
    var sc = new SubClass({
      highWaterMark: 77
    });
    assert_equals(sc.constructor.name, 'SubClass', 'constructor.name should be correct');
    assert_equals(sc.highWaterMark, 77, 'highWaterMark should come from the parent class');
    assert_equals(sc.size(), 2, 'size() on the subclass should override the parent');
    assert_true(sc.subClassMethod(), 'subClassMethod() should work');
  }, `${QueuingStrategy.name}: subclassing should work correctly`);
  test(() => {
    var size = new QueuingStrategy({
      highWaterMark: 5
    }).size;
    assert_false('prototype' in size);
  }, `${QueuingStrategy.name}: size should not have a prototype property`);
};
for (var QueuingStrategy of [CountQueuingStrategy, ByteLengthQueuingStrategy]) {
  _loop(QueuingStrategy);
}
test(() => {
  var size = new CountQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_throws_js(TypeError, () => new size());
}, `CountQueuingStrategy: size should not be a constructor`);
test(() => {
  var size = new ByteLengthQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_throws_js(TypeError, () => new size({
    byteLength: 1024
  }));
}, `ByteLengthQueuingStrategy: size should not be a constructor`);
test(() => {
  var size = new CountQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_equals(size.length, 0);
}, 'CountQueuingStrategy: size should have the right length');
test(() => {
  var size = new ByteLengthQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_equals(size.length, 1);
}, 'ByteLengthQueuingStrategy: size should have the right length');
test(() => {
  var size = 1024;
  var chunk = {
    byteLength: size
  };
  var chunkGetter = {
    get byteLength() {
      return size;
    }
  };
  var error = new Error('wow!');
  var chunkGetterThrowing = {
    get byteLength() {
      throw error;
    }
  };
  var sizeFunction = new CountQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_equals(sizeFunction(), 1, 'size returns 1 with undefined');
  assert_equals(sizeFunction(null), 1, 'size returns 1 with null');
  assert_equals(sizeFunction('potato'), 1, 'size returns 1 with non-object type');
  assert_equals(sizeFunction({}), 1, 'size returns 1 with empty object');
  assert_equals(sizeFunction(chunk), 1, 'size returns 1 with a chunk');
  assert_equals(sizeFunction(chunkGetter), 1, 'size returns 1 with chunk getter');
  assert_equals(sizeFunction(chunkGetterThrowing), 1, 'size returns 1 with chunk getter that throws');
}, 'CountQueuingStrategy: size behaves as expected with strange arguments');
test(() => {
  var size = 1024;
  var chunk = {
    byteLength: size
  };
  var chunkGetter = {
    get byteLength() {
      return size;
    }
  };
  var error = new Error('wow!');
  var chunkGetterThrowing = {
    get byteLength() {
      throw error;
    }
  };
  var sizeFunction = new ByteLengthQueuingStrategy({
    highWaterMark: 5
  }).size;
  assert_throws_js(TypeError, () => sizeFunction(), 'size fails with undefined');
  assert_throws_js(TypeError, () => sizeFunction(null), 'size fails with null');
  assert_equals(sizeFunction('potato'), undefined, 'size succeeds with undefined with a random non-object type');
  assert_equals(sizeFunction({}), undefined, 'size succeeds with undefined with an object without hwm property');
  assert_equals(sizeFunction(chunk), size, 'size succeeds with the right amount with an object with a hwm');
  assert_equals(sizeFunction(chunkGetter), size, 'size succeeds with the right amount with an object with a hwm getter');
  assert_throws_exactly(error, () => sizeFunction(chunkGetterThrowing), 'size fails with the error thrown by the getter');
}, 'ByteLengthQueuingStrategy: size behaves as expected with strange arguments');