// META: script=/common/subset-tests-by-key.js
// META: variant=?include=file
// META: variant=?include=javascript
// META: variant=?include=mailto
// META: variant=?exclude=(file|javascript|mailto)

// Keep this file in sync with url-setters.any.js.

promise_test(() => fetch("resources/setters_tests.json").then(res => res.json()).then(runURLSettersTests), "Loading data…");
function runURLSettersTests(allTestCases) {
  var _loop = function (propertyToBeSet) {
    if (propertyToBeSet === "comment") {
      return 1; // continue
    }
    var _loop2 = function (testCase) {
      var name = `Setting <${testCase.href}>.${propertyToBeSet} = '${testCase.new_value}'${testCase.comment ? ` ${testCase.comment}` : ''}`;
      var key = testCase.href.split(":")[0];
      subsetTestByKey(key, test, () => {
        var url = document.createElement("a");
        url.href = testCase.href;
        url[propertyToBeSet] = testCase.new_value;
        for (var [property, expectedValue] of Object.entries(testCase.expected)) {
          assert_equals(url[property], expectedValue);
        }
      }, `<a>: ${name}`);
      subsetTestByKey(key, test, () => {
        var url = document.createElement("area");
        url.href = testCase.href;
        url[propertyToBeSet] = testCase.new_value;
        for (var [property, expectedValue] of Object.entries(testCase.expected)) {
          assert_equals(url[property], expectedValue);
        }
      }, `<area>: ${name}`);
    };
    for (var testCase of testCases) {
      _loop2(testCase);
    }
  };
  for (var [propertyToBeSet, testCases] of Object.entries(allTestCases)) {
    if (_loop(propertyToBeSet)) continue;
  }
}