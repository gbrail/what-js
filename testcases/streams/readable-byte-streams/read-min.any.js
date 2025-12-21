// META: global=window,worker,shadowrealm
// META: script=../resources/rs-utils.js
// META: script=../resources/test-utils.js
'use strict';

// View buffers are detached after pull() returns, so record the information at the time that pull() was called.
function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
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
function extractViewInfo(view) {
  return {
    constructor: view.constructor,
    bufferByteLength: view.buffer.byteLength,
    byteOffset: view.byteOffset,
    byteLength: view.byteLength
  };
}
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.unreached_func('pull() should not be called')
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_js(t, TypeError, reader.read(new Uint8Array(1), {
    min: 0
  })));
}), 'ReadableStream with byte source: read({ min }) rejects if min is 0');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.unreached_func('pull() should not be called')
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_js(t, TypeError, reader.read(new Uint8Array(1), {
    min: -1
  })));
}), 'ReadableStream with byte source: read({ min }) rejects if min is negative');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.unreached_func('pull() should not be called')
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_js(t, RangeError, reader.read(new Uint8Array(1), {
    min: 2
  })));
}), 'ReadableStream with byte source: read({ min }) rejects if min is larger than view\'s length (Uint8Array)');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.unreached_func('pull() should not be called')
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_js(t, RangeError, reader.read(new Uint16Array(1), {
    min: 2
  })));
}), 'ReadableStream with byte source: read({ min }) rejects if min is larger than view\'s length (Uint16Array)');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.unreached_func('pull() should not be called')
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _awaitIgnored(promise_rejects_js(t, RangeError, reader.read(new DataView(new ArrayBuffer(1)), {
    min: 2
  })));
}), 'ReadableStream with byte source: read({ min }) rejects if min is larger than view\'s length (DataView)');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        view[0] = 0x01;
        view[1] = 0x02;
        byobRequest.respond(2);
      } else if (pullCount === 1) {
        view[0] = 0x03;
        byobRequest.respond(1);
      } else if (pullCount === 2) {
        view[0] = 0x04;
        byobRequest.respond(1);
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  var read1 = reader.read(new Uint8Array(3), {
    min: 3
  });
  var read2 = reader.read(new Uint8Array(1));
  return _await(read1, function (result1) {
    assert_false(result1.done, 'first result should not be done');
    assert_typed_array_equals(result1.value, new Uint8Array([0x01, 0x02, 0x03]), 'first result value');
    return _await(read2, function (result2) {
      assert_false(result2.done, 'second result should not be done');
      assert_typed_array_equals(result2.value, new Uint8Array([0x04]), 'second result value');
      assert_equals(pullCount, 3, 'pull() must have been called 3 times');
      {
        var byobRequest = byobRequests[0];
        assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
        assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
        var viewInfo = byobRequest.viewInfo;
        assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
        assert_equals(viewInfo.bufferByteLength, 3, 'first view.buffer.byteLength should be 3');
        assert_equals(viewInfo.byteOffset, 0, 'first view.byteOffset should be 0');
        assert_equals(viewInfo.byteLength, 3, 'first view.byteLength should be 3');
      }
      {
        var _byobRequest = byobRequests[1];
        assert_true(_byobRequest.nonNull, 'second byobRequest must not be null');
        assert_true(_byobRequest.viewNonNull, 'second byobRequest.view must not be null');
        var _viewInfo = _byobRequest.viewInfo;
        assert_equals(_viewInfo.constructor, Uint8Array, 'second view.constructor should be Uint8Array');
        assert_equals(_viewInfo.bufferByteLength, 3, 'second view.buffer.byteLength should be 3');
        assert_equals(_viewInfo.byteOffset, 2, 'second view.byteOffset should be 2');
        assert_equals(_viewInfo.byteLength, 1, 'second view.byteLength should be 1');
      }
      {
        var _byobRequest2 = byobRequests[2];
        assert_true(_byobRequest2.nonNull, 'third byobRequest must not be null');
        assert_true(_byobRequest2.viewNonNull, 'third byobRequest.view must not be null');
        var _viewInfo2 = _byobRequest2.viewInfo;
        assert_equals(_viewInfo2.constructor, Uint8Array, 'third view.constructor should be Uint8Array');
        assert_equals(_viewInfo2.bufferByteLength, 1, 'third view.buffer.byteLength should be 1');
        assert_equals(_viewInfo2.byteOffset, 0, 'third view.byteOffset should be 0');
        assert_equals(_viewInfo2.byteLength, 1, 'third view.byteLength should be 1');
      }
    });
  });
}), 'ReadableStream with byte source: read({ min }), then read()');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        view[0] = 0x01;
        view[1] = 0x02;
        byobRequest.respond(2);
      } else if (pullCount === 1) {
        view[0] = 0x03;
        byobRequest.respond(1);
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new DataView(new ArrayBuffer(3)), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'result should not be done');
    assert_equals(result.value.constructor, DataView, 'result.value must be a DataView');
    assert_equals(result.value.byteOffset, 0, 'result.value.byteOffset');
    assert_equals(result.value.byteLength, 3, 'result.value.byteLength');
    assert_equals(result.value.buffer.byteLength, 3, 'result.value.buffer.byteLength');
    assert_array_equals([...new Uint8Array(result.value.buffer)], [0x01, 0x02, 0x03], `result.value.buffer contents`);
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    {
      var byobRequest = byobRequests[0];
      assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
      assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
      var viewInfo = byobRequest.viewInfo;
      assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
      assert_equals(viewInfo.bufferByteLength, 3, 'first view.buffer.byteLength should be 3');
      assert_equals(viewInfo.byteOffset, 0, 'first view.byteOffset should be 0');
      assert_equals(viewInfo.byteLength, 3, 'first view.byteLength should be 3');
    }
    {
      var _byobRequest3 = byobRequests[1];
      assert_true(_byobRequest3.nonNull, 'second byobRequest must not be null');
      assert_true(_byobRequest3.viewNonNull, 'second byobRequest.view must not be null');
      var _viewInfo3 = _byobRequest3.viewInfo;
      assert_equals(_viewInfo3.constructor, Uint8Array, 'second view.constructor should be Uint8Array');
      assert_equals(_viewInfo3.bufferByteLength, 3, 'second view.buffer.byteLength should be 3');
      assert_equals(_viewInfo3.byteOffset, 2, 'second view.byteOffset should be 2');
      assert_equals(_viewInfo3.byteLength, 1, 'second view.byteLength should be 1');
    }
  });
}), 'ReadableStream with byte source: read({ min }) with a DataView');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    start: t.step_func(c => {
      c.enqueue(new Uint8Array([0x01]));
    }),
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        view[0] = 0x02;
        view[1] = 0x03;
        byobRequest.respond(2);
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(3), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'first result should not be done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0x02, 0x03]), 'first result value');
    assert_equals(pullCount, 1, 'pull() must have only been called once');
    var byobRequest = byobRequests[0];
    assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
    assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
    var viewInfo = byobRequest.viewInfo;
    assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
    assert_equals(viewInfo.bufferByteLength, 3, 'first view.buffer.byteLength should be 3');
    assert_equals(viewInfo.byteOffset, 1, 'first view.byteOffset should be 1');
    assert_equals(viewInfo.byteLength, 2, 'first view.byteLength should be 2');
  });
}), 'ReadableStream with byte source: enqueue(), then read({ min })');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        c.enqueue(new Uint8Array([0x01, 0x02]));
      } else if (pullCount === 1) {
        c.enqueue(new Uint8Array([0x03]));
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(3), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'first result should not be done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0x02, 0x03]), 'first result value');
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    {
      var byobRequest = byobRequests[0];
      assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
      assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
      var viewInfo = byobRequest.viewInfo;
      assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
      assert_equals(viewInfo.bufferByteLength, 3, 'first view.buffer.byteLength should be 3');
      assert_equals(viewInfo.byteOffset, 0, 'first view.byteOffset should be 0');
      assert_equals(viewInfo.byteLength, 3, 'first view.byteLength should be 3');
    }
    {
      var _byobRequest4 = byobRequests[1];
      assert_true(_byobRequest4.nonNull, 'second byobRequest must not be null');
      assert_true(_byobRequest4.viewNonNull, 'second byobRequest.view must not be null');
      var _viewInfo4 = _byobRequest4.viewInfo;
      assert_equals(_viewInfo4.constructor, Uint8Array, 'second view.constructor should be Uint8Array');
      assert_equals(_viewInfo4.bufferByteLength, 3, 'second view.buffer.byteLength should be 3');
      assert_equals(_viewInfo4.byteOffset, 2, 'second view.byteOffset should be 2');
      assert_equals(_viewInfo4.byteLength, 1, 'second view.byteLength should be 1');
    }
  });
}), 'ReadableStream with byte source: read({ min: 3 }) on a 3-byte Uint8Array, then multiple enqueue() up to 3 bytes');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        c.enqueue(new Uint8Array([0x01, 0x02]));
      } else if (pullCount === 1) {
        c.enqueue(new Uint8Array([0x03]));
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(5), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'first result should not be done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0x02, 0x03, 0, 0]).subarray(0, 3), 'first result value');
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    {
      var byobRequest = byobRequests[0];
      assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
      assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
      var viewInfo = byobRequest.viewInfo;
      assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
      assert_equals(viewInfo.bufferByteLength, 5, 'first view.buffer.byteLength should be 5');
      assert_equals(viewInfo.byteOffset, 0, 'first view.byteOffset should be 0');
      assert_equals(viewInfo.byteLength, 5, 'first view.byteLength should be 5');
    }
    {
      var _byobRequest5 = byobRequests[1];
      assert_true(_byobRequest5.nonNull, 'second byobRequest must not be null');
      assert_true(_byobRequest5.viewNonNull, 'second byobRequest.view must not be null');
      var _viewInfo5 = _byobRequest5.viewInfo;
      assert_equals(_viewInfo5.constructor, Uint8Array, 'second view.constructor should be Uint8Array');
      assert_equals(_viewInfo5.bufferByteLength, 5, 'second view.buffer.byteLength should be 5');
      assert_equals(_viewInfo5.byteOffset, 2, 'second view.byteOffset should be 2');
      assert_equals(_viewInfo5.byteLength, 3, 'second view.byteLength should be 3');
    }
  });
}), 'ReadableStream with byte source: read({ min: 3 }) on a 5-byte Uint8Array, then multiple enqueue() up to 3 bytes');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequests = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      var byobRequest = c.byobRequest;
      var view = byobRequest.view;
      byobRequests[pullCount] = {
        nonNull: byobRequest !== null,
        viewNonNull: view !== null,
        viewInfo: extractViewInfo(view)
      };
      if (pullCount === 0) {
        c.enqueue(new Uint8Array([0x01, 0x02]));
      } else if (pullCount === 1) {
        c.enqueue(new Uint8Array([0x03, 0x04]));
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(5), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'first result should not be done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0x02, 0x03, 0x04, 0]).subarray(0, 4), 'first result value');
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    {
      var byobRequest = byobRequests[0];
      assert_true(byobRequest.nonNull, 'first byobRequest must not be null');
      assert_true(byobRequest.viewNonNull, 'first byobRequest.view must not be null');
      var viewInfo = byobRequest.viewInfo;
      assert_equals(viewInfo.constructor, Uint8Array, 'first view.constructor should be Uint8Array');
      assert_equals(viewInfo.bufferByteLength, 5, 'first view.buffer.byteLength should be 5');
      assert_equals(viewInfo.byteOffset, 0, 'first view.byteOffset should be 0');
      assert_equals(viewInfo.byteLength, 5, 'first view.byteLength should be 5');
    }
    {
      var _byobRequest6 = byobRequests[1];
      assert_true(_byobRequest6.nonNull, 'second byobRequest must not be null');
      assert_true(_byobRequest6.viewNonNull, 'second byobRequest.view must not be null');
      var _viewInfo6 = _byobRequest6.viewInfo;
      assert_equals(_viewInfo6.constructor, Uint8Array, 'second view.constructor should be Uint8Array');
      assert_equals(_viewInfo6.bufferByteLength, 5, 'second view.buffer.byteLength should be 5');
      assert_equals(_viewInfo6.byteOffset, 2, 'second view.byteOffset should be 2');
      assert_equals(_viewInfo6.byteLength, 3, 'second view.byteLength should be 3');
    }
  });
}), 'ReadableStream with byte source: read({ min: 3 }) on a 5-byte Uint8Array, then multiple enqueue() up to 4 bytes');
promise_test(_async(function (t) {
  var stream = new ReadableStream({
    start(c) {
      var view = new Uint8Array(16);
      view[0] = 0x01;
      view[8] = 0x02;
      c.enqueue(view);
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var byobReader = stream.getReader({
    mode: 'byob'
  });
  return _await(byobReader.read(new Uint8Array(8), {
    min: 8
  }), function (result1) {
    assert_false(result1.done, 'result1.done');
    var view1 = result1.value;
    assert_equals(view1.constructor, Uint8Array, 'result1.value.constructor');
    assert_equals(view1.buffer.byteLength, 8, 'result1.value.buffer.byteLength');
    assert_equals(view1.byteOffset, 0, 'result1.value.byteOffset');
    assert_equals(view1.byteLength, 8, 'result1.value.byteLength');
    assert_equals(view1[0], 0x01, 'result1.value[0]');
    byobReader.releaseLock();
    var reader = stream.getReader();
    return _await(reader.read(), function (result2) {
      assert_false(result2.done, 'result2.done');
      var view2 = result2.value;
      assert_equals(view2.constructor, Uint8Array, 'result2.value.constructor');
      assert_equals(view2.buffer.byteLength, 16, 'result2.value.buffer.byteLength');
      assert_equals(view2.byteOffset, 8, 'result2.value.byteOffset');
      assert_equals(view2.byteLength, 8, 'result2.value.byteLength');
      assert_equals(view2[0], 0x02, 'result2.value[0]');
    });
  });
}), 'ReadableStream with byte source: enqueue(), read({ min }) partially, then read()');
promise_test(_async(function () {
  var pullCount = 0;
  var byobRequestDefined = [];
  var byobRequestViewDefined;
  var stream = new ReadableStream({
    pull: _async(function (c) {
      byobRequestDefined.push(c.byobRequest !== null);
      var initialByobRequest = c.byobRequest;
      return _await(transferArrayBufferView(c.byobRequest.view), function (transferredView) {
        transferredView[0] = 0x01;
        c.byobRequest.respondWithNewView(transferredView);
        byobRequestDefined.push(c.byobRequest !== null);
        byobRequestViewDefined = initialByobRequest.view !== null;
        ++pullCount;
      });
    }),
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(1), {
    min: 1
  }), function (result) {
    assert_false(result.done, 'result.done');
    assert_equals(result.value.byteLength, 1, 'result.value.byteLength');
    assert_equals(result.value[0], 0x01, 'result.value[0]');
    assert_equals(pullCount, 1, 'pull() should be called only once');
    assert_true(byobRequestDefined[0], 'byobRequest must not be null before respondWithNewView()');
    assert_false(byobRequestDefined[1], 'byobRequest must be null after respondWithNewView()');
    assert_false(byobRequestViewDefined, 'view of initial byobRequest must be null after respondWithNewView()');
  });
}), 'ReadableStream with byte source: read({ min }), then respondWithNewView() with a transferred ArrayBuffer');
promise_test(_async(function (t) {
  var stream = new ReadableStream({
    start(c) {
      c.close();
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array([0x01]), {
    min: 1
  }), function (result) {
    assert_true(result.done, 'result.done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01]).subarray(0, 0), 'result.value');
    return _awaitIgnored(reader.closed);
  });
}), 'ReadableStream with byte source: read({ min }) on a closed stream');
promise_test(_async(function (t) {
  var pullCount = 0;
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      if (pullCount === 0) {
        c.byobRequest.view[0] = 0x01;
        c.byobRequest.respond(1);
      } else if (pullCount === 1) {
        c.close();
        c.byobRequest.respond(0);
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(3), {
    min: 3
  }), function (result) {
    assert_true(result.done, 'result.done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0, 0]).subarray(0, 1), 'result.value');
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    return _awaitIgnored(reader.closed);
  });
}), 'ReadableStream with byte source: read({ min }) when closed before view is filled');
promise_test(_async(function (t) {
  var pullCount = 0;
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      if (pullCount === 0) {
        c.byobRequest.view[0] = 0x01;
        c.byobRequest.view[1] = 0x02;
        c.byobRequest.respond(2);
      } else if (pullCount === 1) {
        c.byobRequest.view[0] = 0x03;
        c.byobRequest.respond(1);
        c.close();
      }
      ++pullCount;
    })
  });
  var reader = rs.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(3), {
    min: 3
  }), function (result) {
    assert_false(result.done, 'result.done');
    assert_typed_array_equals(result.value, new Uint8Array([0x01, 0x02, 0x03]), 'result.value');
    assert_equals(pullCount, 2, 'pull() must have been called 2 times');
    return _awaitIgnored(reader.closed);
  });
}), 'ReadableStream with byte source: read({ min }) when closed immediately after view is filled');
promise_test(_async(function (t) {
  var error1 = new Error('error1');
  var stream = new ReadableStream({
    start(c) {
      c.error(error1);
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  var read = reader.read(new Uint8Array(1), {
    min: 1
  });
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, error1, read, 'read() must fail'), promise_rejects_exactly(t, error1, reader.closed, 'closed must fail')]));
}), 'ReadableStream with byte source: read({ min }) on an errored stream');
promise_test(_async(function (t) {
  var error1 = new Error('error1');
  var controller;
  var stream = new ReadableStream({
    start(c) {
      controller = c;
    },
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  var read = reader.read(new Uint8Array(1), {
    min: 1
  });
  controller.error(error1);
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, error1, read, 'read() must fail'), promise_rejects_exactly(t, error1, reader.closed, 'closed must fail')]));
}), 'ReadableStream with byte source: read({ min }), then error()');
promise_test(t => {
  var cancelCount = 0;
  var reason;
  var passedReason = new TypeError('foo');
  var stream = new ReadableStream({
    pull: t.unreached_func('pull() should not be called'),
    cancel(r) {
      if (cancelCount === 0) {
        reason = r;
      }
      ++cancelCount;
      return 'bar';
    },
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  var readPromise = reader.read(new Uint8Array(1), {
    min: 1
  }).then(result => {
    assert_true(result.done, 'result.done');
    assert_equals(result.value, undefined, 'result.value');
  });
  var cancelPromise = reader.cancel(passedReason).then(result => {
    assert_equals(result, undefined, 'cancel() return value should be fulfilled with undefined');
    assert_equals(cancelCount, 1, 'cancel() should be called only once');
    assert_equals(reason, passedReason, 'reason should equal the passed reason');
  });
  return Promise.all([readPromise, cancelPromise]);
}, 'ReadableStream with byte source: getReader(), read({ min }), then cancel()');
promise_test(_async(function (t) {
  var pullCount = 0;
  var byobRequest;
  var viewInfos = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull: t.step_func(c => {
      byobRequest = c.byobRequest;
      viewInfos.push(extractViewInfo(c.byobRequest.view));
      c.byobRequest.view[0] = 0x01;
      c.byobRequest.respond(1);
      viewInfos.push(extractViewInfo(c.byobRequest.view));
      ++pullCount;
    })
  });
  return _await(Promise.resolve(), function () {
    assert_equals(pullCount, 0, 'pull() must not have been called yet');
    var reader = rs.getReader({
      mode: 'byob'
    });
    var read = reader.read(new Uint8Array(3), {
      min: 3
    });
    assert_equals(pullCount, 1, 'pull() must have been called once');
    assert_not_equals(byobRequest, null, 'byobRequest should not be null');
    assert_equals(viewInfos[0].byteLength, 3, 'byteLength before respond() should be 3');
    assert_equals(viewInfos[1].byteLength, 2, 'byteLength after respond() should be 2');
    reader.cancel().catch(t.unreached_func('cancel() should not reject'));
    return _await(read, function (result) {
      assert_true(result.done, 'result.done');
      assert_equals(result.value, undefined, 'result.value');
      assert_equals(pullCount, 1, 'pull() must only be called once');
      return _awaitIgnored(reader.closed);
    });
  });
}), 'ReadableStream with byte source: cancel() with partially filled pending read({ min }) request');
promise_test(_async(function () {
  var pullCalled = false;
  var stream = new ReadableStream({
    start(c) {
      var view = new Uint8Array(16);
      view[7] = 0x01;
      view[15] = 0x02;
      c.enqueue(view);
    },
    pull() {
      pullCalled = true;
    },
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  return _await(reader.read(new Uint8Array(8), {
    min: 8
  }), function (result1) {
    assert_false(result1.done, 'result1.done');
    var view1 = result1.value;
    assert_equals(view1.byteOffset, 0, 'result1.value.byteOffset');
    assert_equals(view1.byteLength, 8, 'result1.value.byteLength');
    assert_equals(view1[7], 0x01, 'result1.value[7]');
    return _await(reader.read(new Uint8Array(8), {
      min: 8
    }), function (result2) {
      assert_false(pullCalled, 'pull() must not have been called');
      assert_false(result2.done, 'result2.done');
      var view2 = result2.value;
      assert_equals(view2.byteOffset, 0, 'result2.value.byteOffset');
      assert_equals(view2.byteLength, 8, 'result2.value.byteLength');
      assert_equals(view2[7], 0x02, 'result2.value[7]');
    });
  });
}), 'ReadableStream with byte source: enqueue(), then read({ min }) with smaller views');
promise_test(_async(function (t) {
  var stream = new ReadableStream({
    start(c) {
      c.enqueue(new Uint8Array([0xaa, 0xbb, 0xcc]));
      c.close();
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  return _await(promise_rejects_js(t, TypeError, reader.read(new Uint16Array(2), {
    min: 2
  }), 'read() must fail'), function () {
    return _awaitIgnored(promise_rejects_js(t, TypeError, reader.closed, 'reader.closed should reject'));
  });
}), 'ReadableStream with byte source: 3 byte enqueue(), then close(), then read({ min }) with 2-element Uint16Array must fail');
promise_test(_async(function (t) {
  var controller;
  var stream = new ReadableStream({
    start(c) {
      controller = c;
    },
    pull: t.unreached_func('pull() should not be called'),
    type: 'bytes'
  });
  var reader = stream.getReader({
    mode: 'byob'
  });
  var readPromise = reader.read(new Uint16Array(2), {
    min: 2
  });
  controller.enqueue(new Uint8Array([0xaa, 0xbb, 0xcc]));
  assert_throws_js(TypeError, () => controller.close(), 'controller.close() must throw');
  return _await(promise_rejects_js(t, TypeError, readPromise, 'read() must fail'), function () {
    return _awaitIgnored(promise_rejects_js(t, TypeError, reader.closed, 'reader.closed must reject'));
  });
}), 'ReadableStream with byte source: read({ min }) with 2-element Uint16Array, then 3 byte enqueue(), then close() must fail');
promise_test(_async(function (t) {
  var pullCount = 0;
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start: t.step_func(c => {
      controller = c;
    }),
    pull: t.step_func(c => {
      ++pullCount;
    })
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  return _await(Promise.resolve(), function () {
    assert_equals(pullCount, 0, 'pull() must not have been called yet');
    var read1 = reader1.read(new Uint8Array(3), {
      min: 3
    });
    var read2 = reader2.read(new Uint8Array(1));
    assert_equals(pullCount, 1, 'pull() must have been called once');
    var byobRequest1 = controller.byobRequest;
    assert_equals(byobRequest1.view.byteLength, 3, 'first byobRequest.view.byteLength should be 3');
    byobRequest1.view[0] = 0x01;
    byobRequest1.respond(1);
    return _await(read2, function (result2) {
      assert_false(result2.done, 'branch2 first read() should not be done');
      assert_typed_array_equals(result2.value, new Uint8Array([0x01]), 'branch2 first read() value');
      assert_equals(pullCount, 2, 'pull() must have been called 2 times');
      var byobRequest2 = controller.byobRequest;
      assert_equals(byobRequest2.view.byteLength, 2, 'second byobRequest.view.byteLength should be 2');
      byobRequest2.view[0] = 0x02;
      byobRequest2.view[1] = 0x03;
      byobRequest2.respond(2);
      return _await(read1, function (result1) {
        assert_false(result1.done, 'branch1 read() should not be done');
        assert_typed_array_equals(result1.value, new Uint8Array([0x01, 0x02, 0x03]), 'branch1 read() value');
        return _await(reader2.read(new Uint8Array(2)), function (result3) {
          assert_equals(pullCount, 2, 'pull() must only be called 2 times');
          assert_false(result3.done, 'branch2 second read() should not be done');
          assert_typed_array_equals(result3.value, new Uint8Array([0x02, 0x03]), 'branch2 second read() value');
        });
      });
    });
  });
}), 'ReadableStream with byte source: tee() with read({ min }) from branch1 and read() from branch2');