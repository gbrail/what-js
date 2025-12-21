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
function _await(value, then, direct) {
  if (direct) {
    return then ? then(value) : value;
  }
  if (!value || !value.then) {
    value = Promise.resolve(value);
  }
  return then ? value.then(then) : value;
}
// There aren't many cloneable types that will cause an error on
// deserialization. WASM modules have the property that it's an error to
// deserialize them cross-site, which works for our purposes.
var createWasmModule = _async(function () {
  // It doesn't matter what the module is, so we use one from another
  // test.
  return _await(fetch("/wasm/serialization/module/resources/incrementer.wasm"), function (response) {
    return _await(response.arrayBuffer(), function (ab) {
      return WebAssembly.compile(ab);
    });
  });
});