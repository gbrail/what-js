// META: global=window,worker,shadowrealm
'use strict';

class ThrowingOptions {
  constructor(whatShouldThrow) {
    this.whatShouldThrow = whatShouldThrow;
    this.touched = [];
  }
  get preventClose() {
    this.maybeThrow('preventClose');
    return false;
  }
  get preventAbort() {
    this.maybeThrow('preventAbort');
    return false;
  }
  get preventCancel() {
    this.maybeThrow('preventCancel');
    return false;
  }
  get signal() {
    this.maybeThrow('signal');
    return undefined;
  }
  maybeThrow(forWhat) {
    this.touched.push(forWhat);
    if (this.whatShouldThrow === forWhat) {
      throw new Error(this.whatShouldThrow);
    }
  }
}
var checkOrder = ['preventAbort', 'preventCancel', 'preventClose', 'signal'];
var _loop = function () {
  var whatShouldThrow = checkOrder[i];
  var whatShouldBeTouched = checkOrder.slice(0, i + 1);
  promise_test(t => {
    var options = new ThrowingOptions(whatShouldThrow);
    return promise_rejects_js(t, Error, new ReadableStream().pipeTo(new WritableStream(), options), 'pipeTo should reject').then(() => assert_array_equals(options.touched, whatShouldBeTouched, 'options should be touched in the right order'));
  }, `pipeTo should stop after getting ${whatShouldThrow} throws`);
  test(() => {
    var options = new ThrowingOptions(whatShouldThrow);
    assert_throws_js(Error, () => new ReadableStream().pipeThrough(new TransformStream(), options), 'pipeThrough should throw');
    assert_array_equals(options.touched, whatShouldBeTouched, 'options should be touched in the right order');
  }, `pipeThrough should stop after getting ${whatShouldThrow} throws`);
};
for (var i = 0; i < checkOrder.length; ++i) {
  _loop();
}