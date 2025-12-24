// META: global=window,worker,shadowrealm
'use strict';

function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, r) { if ("object" != typeof t || !t) return t; var e = t[Symbol.toPrimitive]; if (void 0 !== e) { var i = e.call(t, r || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === r ? String : Number)(t); }
var LipFuzzTransformer = /*#__PURE__*/function () {
  function LipFuzzTransformer(substitutions) {
    _classCallCheck(this, LipFuzzTransformer);
    this.substitutions = substitutions;
    this.partialChunk = '';
    this.lastIndex = undefined;
  }
  return _createClass(LipFuzzTransformer, [{
    key: "transform",
    value: function transform(chunk, controller) {
      chunk = this.partialChunk + chunk;
      this.partialChunk = '';
      // lastIndex is the index of the first character after the last substitution.
      this.lastIndex = 0;
      chunk = chunk.replace(/\{\{([a-zA-Z0-9_-]+)\}\}/g, this.replaceTag.bind(this));
      // Regular expression for an incomplete template at the end of a string.
      var partialAtEndRegexp = /\{(\{([a-zA-Z0-9_-]+(\})?)?)?$/g;
      // Avoid looking at any characters that have already been substituted.
      partialAtEndRegexp.lastIndex = this.lastIndex;
      this.lastIndex = undefined;
      var match = partialAtEndRegexp.exec(chunk);
      if (match) {
        this.partialChunk = chunk.substring(match.index);
        chunk = chunk.substring(0, match.index);
      }
      controller.enqueue(chunk);
    }
  }, {
    key: "flush",
    value: function flush(controller) {
      if (this.partialChunk.length > 0) {
        controller.enqueue(this.partialChunk);
      }
    }
  }, {
    key: "replaceTag",
    value: function replaceTag(match, p1, offset) {
      var replacement = this.substitutions[p1];
      if (replacement === undefined) {
        replacement = '';
      }
      this.lastIndex = offset + replacement.length;
      return replacement;
    }
  }]);
}();
var substitutions = {
  in1: 'out1',
  in2: 'out2',
  quine: '{{quine}}',
  bogusPartial: '{{incompleteResult}'
};
var cases = [{
  input: [''],
  output: ['']
}, {
  input: [],
  output: []
}, {
  input: ['{{in1}}'],
  output: ['out1']
}, {
  input: ['z{{in1}}'],
  output: ['zout1']
}, {
  input: ['{{in1}}q'],
  output: ['out1q']
}, {
  input: ['{{in1}}{{in1}'],
  output: ['out1', '{{in1}']
}, {
  input: ['{{in1}}{{in1}', '}'],
  output: ['out1', 'out1']
}, {
  input: ['{{in1', '}}'],
  output: ['', 'out1']
}, {
  input: ['{{', 'in1}}'],
  output: ['', 'out1']
}, {
  input: ['{', '{in1}}'],
  output: ['', 'out1']
}, {
  input: ['{{', 'in1}'],
  output: ['', '', '{{in1}']
}, {
  input: ['{'],
  output: ['', '{']
}, {
  input: ['{', ''],
  output: ['', '', '{']
}, {
  input: ['{', '{', 'i', 'n', '1', '}', '}'],
  output: ['', '', '', '', '', '', 'out1']
}, {
  input: ['{{in1}}{{in2}}{{in1}}'],
  output: ['out1out2out1']
}, {
  input: ['{{wrong}}'],
  output: ['']
}, {
  input: ['{{wron', 'g}}'],
  output: ['', '']
}, {
  input: ['{{quine}}'],
  output: ['{{quine}}']
}, {
  input: ['{{bogusPartial}}'],
  output: ['{{incompleteResult}']
}, {
  input: ['{{bogusPartial}}}'],
  output: ['{{incompleteResult}}']
}];
var _loop = function () {
  var inputChunks = testCase.input;
  var outputChunks = testCase.output;
  promise_test(() => {
    var lft = new TransformStream(new LipFuzzTransformer(substitutions));
    var writer = lft.writable.getWriter();
    var promises = [];
    for (var inputChunk of inputChunks) {
      promises.push(writer.write(inputChunk));
    }
    promises.push(writer.close());
    var reader = lft.readable.getReader();
    var readerChain = Promise.resolve();
    var _loop2 = function (outputChunk) {
      readerChain = readerChain.then(() => {
        return reader.read().then(({
          value,
          done
        }) => {
          assert_false(done, `done should be false when reading ${outputChunk}`);
          assert_equals(value, outputChunk, `value should match outputChunk`);
        });
      });
    };
    for (var outputChunk of outputChunks) {
      _loop2(outputChunk);
    }
    readerChain = readerChain.then(() => {
      return reader.read().then(({
        done
      }) => assert_true(done, `done should be true`));
    });
    promises.push(readerChain);
    return Promise.all(promises);
  }, `testing "${inputChunks}" (length ${inputChunks.length})`);
};
for (var testCase of cases) {
  _loop();
}