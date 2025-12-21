// META: global=window,dedicatedworker,shadowrealm

var combinations = [(t => [t, t.readable])(new TransformStream()), (t => [t.readable, t])(new TransformStream()), (t => [t, t.writable])(new TransformStream()), (t => [t.writable, t])(new TransformStream())];
var _loop = function (combination) {
  test(() => {
    assert_throws_dom("DataCloneError", () => structuredClone(combination, {
      transfer: combination
    }), "structuredClone should throw");
  }, `Transferring ${combination} should fail`);
};
for (var combination of combinations) {
  _loop(combination);
}