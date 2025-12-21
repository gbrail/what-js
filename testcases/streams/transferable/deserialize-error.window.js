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
// META: script=/common/get-host-info.sub.js
// META: script=resources/create-wasm-module.js
// META: timeout=long

var {
  HTTPS_NOTSAMESITE_ORIGIN
} = get_host_info();
function _empty() {}
var iframe = document.createElement('iframe');
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
    const observer = pact.o;
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
function _switch(discriminant, cases) {
  var dispatchIndex = -1;
  var awaitBody;
  outer: {
    for (var i = 0; i < cases.length; i++) {
      var test = cases[i][0];
      if (test) {
        var testValue = test();
        if (testValue && testValue.then) {
          break outer;
        }
        if (testValue === discriminant) {
          dispatchIndex = i;
          break;
        }
      } else {
        // Found the default case, set it as the pending dispatch case
        dispatchIndex = i;
      }
    }
    if (dispatchIndex !== -1) {
      do {
        var body = cases[dispatchIndex][1];
        while (!body) {
          dispatchIndex++;
          body = cases[dispatchIndex][1];
        }
        var result = body();
        if (result && result.then) {
          awaitBody = true;
          break outer;
        }
        var fallthroughCheck = cases[dispatchIndex][2];
        dispatchIndex++;
      } while (fallthroughCheck && !fallthroughCheck());
      return result;
    }
  }
  var pact = new _Pact();
  var reject = _settle.bind(null, pact, 2);
  (awaitBody ? result.then(_resumeAfterBody) : testValue.then(_resumeAfterTest)).then(void 0, reject);
  return pact;
  function _resumeAfterTest(value) {
    for (;;) {
      if (value === discriminant) {
        dispatchIndex = i;
        break;
      }
      if (++i === cases.length) {
        if (dispatchIndex !== -1) {
          break;
        } else {
          _settle(pact, 1, result);
          return;
        }
      }
      test = cases[i][0];
      if (test) {
        value = test();
        if (value && value.then) {
          value.then(_resumeAfterTest).then(void 0, reject);
          return;
        }
      } else {
        dispatchIndex = i;
      }
    }
    do {
      var body = cases[dispatchIndex][1];
      while (!body) {
        dispatchIndex++;
        body = cases[dispatchIndex][1];
      }
      var result = body();
      if (result && result.then) {
        result.then(_resumeAfterBody).then(void 0, reject);
        return;
      }
      var fallthroughCheck = cases[dispatchIndex][2];
      dispatchIndex++;
    } while (fallthroughCheck && !fallthroughCheck());
    _settle(pact, 1, result);
  }
  function _resumeAfterBody(result) {
    for (;;) {
      var fallthroughCheck = cases[dispatchIndex][2];
      if (!fallthroughCheck || fallthroughCheck()) {
        break;
      }
      dispatchIndex++;
      var body = cases[dispatchIndex][1];
      while (!body) {
        dispatchIndex++;
        body = cases[dispatchIndex][1];
      }
      result = body();
      if (result && result.then) {
        result.then(_resumeAfterBody).then(void 0, reject);
        return;
      }
    }
    _settle(pact, 1, result);
  }
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
iframe.src = `${HTTPS_NOTSAMESITE_ORIGIN}/streams/transferable/resources/deserialize-error-frame.html`;
window.addEventListener('message', _async(function (evt) {
  // Tests are serialized to make the results deterministic.
  return _switch(evt.data, [[function () {
    return 'init done';
  }, function () {
    {
      var ws = new WritableStream();
      iframe.contentWindow.postMessage(ws, '*', [ws]);
      return;
    }
  }], [function () {
    return 'ws done';
  }, function () {
    {
      return _call(createWasmModule, function (module) {
        var rs = new ReadableStream({
          start(controller) {
            controller.enqueue(module);
          }
        });
        iframe.contentWindow.postMessage(rs, '*', [rs]);
      });
    }
  }], [function () {
    return 'rs done';
  }, function () {
    {
      iframe.remove();
    }
  }, _empty]]);
}));

// Need to do this after adding the listener to ensure we catch the first
// message.
document.body.appendChild(iframe);
fetch_tests_from_window(iframe.contentWindow);