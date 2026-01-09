'use strict';

function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
}
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
}();
function _isSettledPact(thenable) {
  return thenable instanceof _Pact && thenable.s & 1;
}
function _for(test, update, body) {
  var stage;
  for (;;) {
    var shouldContinue = test();
    if (_isSettledPact(shouldContinue)) {
      shouldContinue = shouldContinue.v;
    }
    if (!shouldContinue) {
      return result;
    }
    if (shouldContinue.then) {
      stage = 0;
      break;
    }
    var result = body();
    if (result && result.then) {
      if (_isSettledPact(result)) {
        result = result.s;
      } else {
        stage = 1;
        break;
      }
    }
    if (update) {
      var updateValue = update();
      if (updateValue && updateValue.then && !_isSettledPact(updateValue)) {
        stage = 2;
        break;
      }
    }
  }
  var pact = new _Pact();
  var reject = _settle.bind(null, pact, 2);
  (stage === 0 ? shouldContinue.then(_resumeAfterTest) : stage === 1 ? result.then(_resumeAfterBody) : updateValue.then(_resumeAfterUpdate)).then(void 0, reject);
  return pact;
  function _resumeAfterBody(value) {
    result = value;
    do {
      if (update) {
        updateValue = update();
        if (updateValue && updateValue.then && !_isSettledPact(updateValue)) {
          updateValue.then(_resumeAfterUpdate).then(void 0, reject);
          return;
        }
      }
      shouldContinue = test();
      if (!shouldContinue || _isSettledPact(shouldContinue) && !shouldContinue.v) {
        _settle(pact, 1, result);
        return;
      }
      if (shouldContinue.then) {
        shouldContinue.then(_resumeAfterTest).then(void 0, reject);
        return;
      }
      result = body();
      if (_isSettledPact(result)) {
        result = result.v;
      }
    } while (!result || !result.then);
    result.then(_resumeAfterBody).then(void 0, reject);
  }
  function _resumeAfterTest(shouldContinue) {
    if (shouldContinue) {
      result = body();
      if (result && result.then) {
        result.then(_resumeAfterBody).then(void 0, reject);
      } else {
        _resumeAfterBody(result);
      }
    } else {
      _settle(pact, 1, result);
    }
  }
  function _resumeAfterUpdate() {
    if (shouldContinue = test()) {
      if (shouldContinue.then) {
        shouldContinue.then(_resumeAfterTest).then(void 0, reject);
      } else {
        _resumeAfterTest(shouldContinue);
      }
    } else {
      _settle(pact, 1, result);
    }
  }
}
function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
(function () {
  // Fake setInterval-like functionality in environments that don't have it
  var IntervalHandle = /*#__PURE__*/function () {
    function IntervalHandle(callback, delayMs) {
      _classCallCheck(this, IntervalHandle);
      this.callback = callback;
      this.delayMs = delayMs;
      this.cancelled = false;
      Promise.resolve().then(() => this.check());
    }
    return _createClass(IntervalHandle, [{
      key: "check",
      value: function check() {
        try {
          var _exit = false;
          var _this = this;
          return _await(_for(function () {
            return !_exit;
          }, void 0, function () {
            return _await(new Promise(resolve => step_timeout(resolve, _this.delayMs)), function () {
              if (_this.cancelled) {
                _exit = true;
                return;
              }
              _this.callback();
            });
          }));
        } catch (e) {
          return Promise.reject(e);
        }
      }
    }, {
      key: "cancel",
      value: function cancel() {
        this.cancelled = true;
      }
    }]);
  }();
  var localSetInterval, localClearInterval;
  if (typeof globalThis.setInterval !== "undefined" && typeof globalThis.clearInterval !== "undefined") {
    localSetInterval = globalThis.setInterval;
    localClearInterval = globalThis.clearInterval;
  } else {
    localSetInterval = function setInterval(callback, delayMs) {
      return new IntervalHandle(callback, delayMs);
    };
    localClearInterval = function clearInterval(handle) {
      handle.cancel();
    };
  }
  var RandomPushSource = /*#__PURE__*/function () {
    function RandomPushSource(toPush) {
      _classCallCheck(this, RandomPushSource);
      this.pushed = 0;
      this.toPush = toPush;
      this.started = false;
      this.paused = false;
      this.closed = false;
      this._intervalHandle = null;
    }
    return _createClass(RandomPushSource, [{
      key: "readStart",
      value: function readStart() {
        if (this.closed) {
          return;
        }
        if (!this.started) {
          this._intervalHandle = localSetInterval(writeChunk, 2);
          this.started = true;
        }
        if (this.paused) {
          this._intervalHandle = localSetInterval(writeChunk, 2);
          this.paused = false;
        }
        var source = this;
        function writeChunk() {
          if (source.paused) {
            return;
          }
          source.pushed++;
          if (source.toPush > 0 && source.pushed > source.toPush) {
            if (source._intervalHandle) {
              localClearInterval(source._intervalHandle);
              source._intervalHandle = undefined;
            }
            source.closed = true;
            source.onend();
          } else {
            source.ondata(randomChunk(128));
          }
        }
      }
    }, {
      key: "readStop",
      value: function readStop() {
        if (this.paused) {
          return;
        }
        if (this.started) {
          this.paused = true;
          localClearInterval(this._intervalHandle);
          this._intervalHandle = undefined;
        } else {
          throw new Error('Can\'t pause reading an unstarted source.');
        }
      }
    }]);
  }();
  function randomChunk(size) {
    var chunk = '';
    for (var i = 0; i < size; ++i) {
      // Add a random character from the basic printable ASCII set.
      chunk += String.fromCharCode(Math.round(Math.random() * 84) + 32);
    }
    return chunk;
  }
  function readableStreamToArray(readable, reader) {
    if (reader === undefined) {
      reader = readable.getReader();
    }
    var chunks = [];
    return pump();
    function pump() {
      return reader.read().then(result => {
        if (result.done) {
          return chunks;
        }
        chunks.push(result.value);
        return pump();
      });
    }
  }
  var SequentialPullSource = /*#__PURE__*/function () {
    function SequentialPullSource(limit, options) {
      _classCallCheck(this, SequentialPullSource);
      var async = options && options.async;
      this.current = 0;
      this.limit = limit;
      this.opened = false;
      this.closed = false;
      this._exec = f => f();
      if (async) {
        this._exec = f => step_timeout(f, 0);
      }
    }
    return _createClass(SequentialPullSource, [{
      key: "open",
      value: function open(cb) {
        this._exec(() => {
          this.opened = true;
          cb();
        });
      }
    }, {
      key: "read",
      value: function read(cb) {
        this._exec(() => {
          if (++this.current <= this.limit) {
            cb(null, false, this.current);
          } else {
            cb(null, true, null);
          }
        });
      }
    }, {
      key: "close",
      value: function close(cb) {
        this._exec(() => {
          this.closed = true;
          cb();
        });
      }
    }]);
  }();
  function sequentialReadableStream(limit, options) {
    var sequentialSource = new SequentialPullSource(limit, options);
    var stream = new ReadableStream({
      start() {
        return new Promise((resolve, reject) => {
          sequentialSource.open(err => {
            if (err) {
              reject(err);
            }
            resolve();
          });
        });
      },
      pull(c) {
        return new Promise((resolve, reject) => {
          sequentialSource.read((err, done, chunk) => {
            if (err) {
              reject(err);
            } else if (done) {
              sequentialSource.close(err2 => {
                if (err2) {
                  reject(err2);
                }
                c.close();
                resolve();
              });
            } else {
              c.enqueue(chunk);
              resolve();
            }
          });
        });
      }
    });
    stream.source = sequentialSource;
    return stream;
  }
  function transferArrayBufferView(view) {
    return structuredClone(view, {
      transfer: [view.buffer]
    });
  }
  self.RandomPushSource = RandomPushSource;
  self.readableStreamToArray = readableStreamToArray;
  self.sequentialReadableStream = sequentialReadableStream;
  self.transferArrayBufferView = transferArrayBufferView;
})();