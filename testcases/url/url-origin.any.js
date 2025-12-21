promise_test(() => Promise.all([fetch("resources/urltestdata.json").then(res => res.json()), fetch("resources/urltestdata-javascript-only.json").then(res => res.json())]).then(tests => tests.flat()).then(runURLTests), "Loading data…");
function runURLTests(urlTests) {
  var _loop = function (expected) {
    // Skip comments and tests without "origin" expectation
    if (typeof expected === "string" || !("origin" in expected)) return 1; // continue
    var base = expected.base !== null ? expected.base : undefined;
    test(() => {
      var url = new URL(expected.input, base);
      assert_equals(url.origin, expected.origin, "origin");
    }, `Origin parsing: <${expected.input}> ${base ? "against <" + base + ">" : "without base"}`);
  };
  for (var expected of urlTests) {
    if (_loop(expected)) continue;
  }
}