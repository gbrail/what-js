promise_test(() => fetch("resources/percent-encoding.json").then(res => res.json()).then(runTests), "Loading data…");
function runTests(testUnits) {
  var _loop = function (testUnit) {
    // Ignore comments
    if (typeof testUnit === "string") {
      return 1; // continue
    }
    var _loop2 = function (encoding) {
      async_test(t => {
        var frame = document.body.appendChild(document.createElement("iframe"));
        t.add_cleanup(() => frame.remove());
        frame.onload = t.step_func_done(() => {
          var output = frame.contentDocument.querySelector("a");
          // Test that the fragment is always UTF-8 encoded
          assert_equals(output.hash, `#${testUnit.output["utf-8"]}`, "fragment");
          assert_equals(output.search, `?${testUnit.output[encoding]}`, "query");
        });
        frame.src = `resources/percent-encoding.py?encoding=${encoding}&value=${toBase64(testUnit.input)}`;
      }, `Input ${testUnit.input} with encoding ${encoding}`);
    };
    for (var encoding of Object.keys(testUnit.output)) {
      _loop2(encoding);
    }
  };
  for (var testUnit of testUnits) {
    if (_loop(testUnit)) continue;
  }
}

// Use base64 to avoid relying on the URL parser to get UTF-8 percent-encoding correctly. This does
// not use btoa directly as that only works with code points in the range U+0000 to U+00FF,
// inclusive.
function toBase64(input) {
  var bytes = new TextEncoder().encode(input);
  var byteString = Array.from(bytes, byte => String.fromCharCode(byte)).join("");
  var encoded = self.btoa(byteString);
  return encoded;
}