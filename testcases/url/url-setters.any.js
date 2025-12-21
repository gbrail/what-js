// META: script=/common/subset-tests-by-key.js
// META: variant=?include=file
// META: variant=?include=javascript
// META: variant=?include=mailto
// META: variant=?exclude=(file|javascript|mailto)

// Keep this file in sync with url-setters-a-area.window.js.

promise_test(() => fetch("resources/setters_tests.json").then(res => res.json()).then(runURLSettersTests), "Loading data…");
function runURLSettersTests(allTestCases) {
  var _loop = function (propertyToBeSet) {
    if (propertyToBeSet === "comment") {
      return 1; // continue
    }
    var _loop2 = function (test_case) {
      var name = `Setting <${test_case.href}>.${propertyToBeSet} = '${test_case.new_value}'${test_case.comment ? ` ${test_case.comment}` : ''}`;
      var key = test_case.href.split(":")[0];
      subsetTestByKey(key, test, () => {
        var url = new URL(test_case.href);
        url[propertyToBeSet] = test_case.new_value;
        for (var [property, expectedValue] of Object.entries(test_case.expected)) {
          assert_equals(url[property], expectedValue);
        }
      }, `URL: ${name}`);
    };
    for (var test_case of testCases) {
      _loop2(test_case);
    }
  };
  for (var [propertyToBeSet, testCases] of Object.entries(allTestCases)) {
    if (_loop(propertyToBeSet)) continue;
  }
}