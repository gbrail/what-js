(function() {
  var subTestKeyPattern = null;
  var match;
  var collectKeys = false;
  var collectCounts = false;
  var keys = {};
  var exclude = false;

  /**
   * Check if `key` is in the subset specified in the URL.
   * @param {string} key
   * @returns {boolean}
   */
  function shouldRunSubTest(key) {
    if (key && subTestKeyPattern) {
      var found = subTestKeyPattern.test(key);
      if (exclude) {
        return !found;
      }
      return found;
    }
    return true;
  }
  /**
   * Only test a subset of tests with `?include=Foo` or `?exclude=Foo` in the URL.
   * Can be used together with `<meta name="variant" content="...">`
   * Sample usage:
   * for (const test of tests) {
   *   subsetTestByKey("Foo", async_test, test.fn, test.name);
   * }
   */
   function subsetTestByKey(key, testFunc, arg1, arg2, arg3) {
    if (collectKeys) {
      if (collectCounts && key in keys) {
        keys[key]++;
      } else {
        keys[key] = 1;
      }
    }
    if (shouldRunSubTest(key)) {
      return testFunc(arg1, arg2, arg3);
    }
    return null;
  }
  self.shouldRunSubTest = shouldRunSubTest;
  self.subsetTestByKey = subsetTestByKey;
})();
