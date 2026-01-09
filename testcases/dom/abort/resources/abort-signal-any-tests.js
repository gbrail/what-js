// Tests for AbortSignal.any() and subclasses that don't use a controller.
function abortSignalAnySignalOnlyTests(signalInterface) {
  var desc = `${signalInterface.name}.any()`;
  test(t => {
    var signal = signalInterface.any([]);
    assert_false(signal.aborted);
  }, `${desc} works with an empty array of signals`);
}

// Tests for AbortSignal.any() and subclasses that use a controller.
function abortSignalAnyTests(signalInterface, controllerInterface) {
  var suffix = `(using ${controllerInterface.name})`;
  var desc = `${signalInterface.name}.any()`;
  test(t => {
    var controller = new controllerInterface();
    var signal = controller.signal;
    var cloneSignal = signalInterface.any([signal]);
    assert_false(cloneSignal.aborted);
    assert_true("reason" in cloneSignal, "cloneSignal has reason property");
    assert_equals(cloneSignal.reason, undefined, "cloneSignal.reason is initially undefined");
    assert_not_equals(signal, cloneSignal, `${desc} returns a new signal.`);
    var eventFired = false;
    cloneSignal.onabort = t.step_func(e => {
      assert_equals(e.target, cloneSignal, `The event target is the signal returned by ${desc}`);
      eventFired = true;
    });
    controller.abort("reason string");
    assert_true(signal.aborted);
    assert_true(cloneSignal.aborted);
    assert_true(eventFired);
    assert_equals(cloneSignal.reason, "reason string", `${desc} propagates the abort reason`);
  }, `${desc} follows a single signal ${suffix}`);
  test(t => {
    var _loop = function () {
      var controllers = [];
      for (var j = 0; j < 3; ++j) {
        controllers.push(new controllerInterface());
      }
      var combinedSignal = signalInterface.any(controllers.map(c => c.signal));
      var eventFired = false;
      combinedSignal.onabort = t.step_func(e => {
        assert_equals(e.target, combinedSignal, `The event target is the signal returned by ${desc}`);
        eventFired = true;
      });
      controllers[i].abort();
      assert_true(eventFired);
      assert_true(combinedSignal.aborted);
      assert_true(combinedSignal.reason instanceof DOMException, "signal.reason is a DOMException");
      assert_equals(combinedSignal.reason.name, "AbortError", "signal.reason is a AbortError");
    };
    for (var i = 0; i < 3; ++i) {
      _loop();
    }
  }, `${desc} follows multiple signals ${suffix}`);
  test(t => {
    var controllers = [];
    for (var i = 0; i < 3; ++i) {
      controllers.push(new controllerInterface());
    }
    controllers[1].abort("reason 1");
    controllers[2].abort("reason 2");
    var signal = signalInterface.any(controllers.map(c => c.signal));
    assert_true(signal.aborted);
    assert_equals(signal.reason, "reason 1", "The signal should be aborted with the first reason");
  }, `${desc} returns an aborted signal if passed an aborted signal ${suffix}`);
  test(t => {
    var controller = new controllerInterface();
    var signal = signalInterface.any([controller.signal, controller.signal]);
    assert_false(signal.aborted);
    controller.abort("reason");
    assert_true(signal.aborted);
    assert_equals(signal.reason, "reason");
  }, `${desc} can be passed the same signal more than once ${suffix}`);
  test(t => {
    var controller1 = new controllerInterface();
    controller1.abort("reason 1");
    var controller2 = new controllerInterface();
    controller2.abort("reason 2");
    var signal = signalInterface.any([controller1.signal, controller2.signal, controller1.signal]);
    assert_true(signal.aborted);
    assert_equals(signal.reason, "reason 1");
  }, `${desc} uses the first instance of a duplicate signal ${suffix}`);
  test(t => {
    var _loop2 = function () {
      var controllers = [];
      for (var j = 0; j < 3; ++j) {
        controllers.push(new controllerInterface());
      }
      var combinedSignal1 = signalInterface.any([controllers[0].signal, controllers[1].signal]);
      var combinedSignal2 = signalInterface.any([combinedSignal1, controllers[2].signal]);
      var eventFired = false;
      combinedSignal2.onabort = t.step_func(e => {
        eventFired = true;
      });
      controllers[i].abort();
      assert_true(eventFired);
      assert_true(combinedSignal2.aborted);
      assert_true(combinedSignal2.reason instanceof DOMException, "signal.reason is a DOMException");
      assert_equals(combinedSignal2.reason.name, "AbortError", "signal.reason is a AbortError");
    };
    for (var i = 0; i < 3; ++i) {
      _loop2();
    }
  }, `${desc} signals are composable ${suffix}`);
  async_test(t => {
    var controller = new controllerInterface();
    var timeoutSignal = AbortSignal.timeout(5);
    var combinedSignal = signalInterface.any([controller.signal, timeoutSignal]);
    combinedSignal.onabort = t.step_func_done(() => {
      assert_true(combinedSignal.aborted);
      assert_true(combinedSignal.reason instanceof DOMException, "combinedSignal.reason is a DOMException");
      assert_equals(combinedSignal.reason.name, "TimeoutError", "combinedSignal.reason is a TimeoutError");
    });
  }, `${desc} works with signals returned by AbortSignal.timeout() ${suffix}`);
  test(t => {
    var controller = new controllerInterface();
    var combined = signalInterface.any([controller.signal]);
    combined = signalInterface.any([combined]);
    combined = signalInterface.any([combined]);
    combined = signalInterface.any([combined]);
    var eventFired = false;
    combined.onabort = () => {
      eventFired = true;
    };
    assert_false(eventFired);
    assert_false(combined.aborted);
    controller.abort("the reason");
    assert_true(eventFired);
    assert_true(combined.aborted);
    assert_equals(combined.reason, "the reason");
  }, `${desc} works with intermediate signals ${suffix}`);
  test(t => {
    var controller = new controllerInterface();
    var signals = [];
    // The first event should be dispatched on the originating signal.
    signals.push(controller.signal);
    // All dependents are linked to `controller.signal` (never to another
    // composite signal), so this is the order events should fire.
    signals.push(signalInterface.any([controller.signal]));
    signals.push(signalInterface.any([controller.signal]));
    signals.push(signalInterface.any([signals[0]]));
    signals.push(signalInterface.any([signals[1]]));
    var result = "";
    var _loop3 = function (i) {
      signals[i].addEventListener('abort', () => {
        result += i;
      });
    };
    for (var i = 0; i < signals.length; i++) {
      _loop3(i);
    }
    controller.abort();
    assert_equals(result, "01234");
  }, `Abort events for ${desc} signals fire in the right order ${suffix}`);
  test(t => {
    var controller = new controllerInterface();
    var signal1 = signalInterface.any([controller.signal]);
    var signal2 = signalInterface.any([signal1]);
    var eventFired = false;
    controller.signal.addEventListener('abort', () => {
      var signal3 = signalInterface.any([signal2]);
      assert_true(controller.signal.aborted);
      assert_true(signal1.aborted);
      assert_true(signal2.aborted);
      assert_true(signal3.aborted);
      eventFired = true;
    });
    controller.abort();
    assert_true(eventFired, "event fired");
  }, `Dependent signals for ${desc} are marked aborted before abort events fire ${suffix}`);
  test(t => {
    var controller1 = new controllerInterface();
    var controller2 = new controllerInterface();
    var signal = signalInterface.any([controller1.signal, controller2.signal]);
    var count = 0;
    controller1.signal.addEventListener('abort', () => {
      controller2.abort("reason 2");
    });
    signal.addEventListener('abort', () => {
      count++;
    });
    controller1.abort("reason 1");
    assert_equals(count, 1);
    assert_true(signal.aborted);
    assert_equals(signal.reason, "reason 1");
  }, `Dependent signals for ${desc} are aborted correctly for reentrant aborts ${suffix}`);
  test(t => {
    var source = signalInterface.abort();
    var dependent = signalInterface.any([source]);
    assert_true(source.reason instanceof DOMException);
    assert_equals(source.reason, dependent.reason);
  }, `Dependent signals for ${desc} should use the same DOMException instance from the already aborted source signal ${suffix}`);
  test(t => {
    var controller = new controllerInterface();
    var source = controller.signal;
    var dependent = signalInterface.any([source]);
    controller.abort();
    assert_true(source.reason instanceof DOMException);
    assert_equals(source.reason, dependent.reason);
  }, `Dependent signals for ${desc} should use the same DOMException instance from the source signal being aborted later ${suffix}`);
}