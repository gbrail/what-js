// META: global=window,worker,shadowrealm
// META: script=../resources/rs-utils.js
// META: script=../resources/test-utils.js
// META: script=../resources/recording-streams.js
// META: script=../resources/rs-test-templates.js
'use strict';

function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
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
function _call(body, then, direct) {
  if (direct) {
    return then ? then(body()) : body();
  }
  try {
    var result = Promise.resolve(body());
    return then ? result.then(then) : result;
  } catch (e) {
    return Promise.reject(e);
  }
}
test(() => {
  var rs = new ReadableStream({
    type: 'bytes'
  });
  var result = rs.tee();
  assert_true(Array.isArray(result), 'return value should be an array');
  assert_equals(result.length, 2, 'array should have length 2');
  assert_equals(result[0].constructor, ReadableStream, '0th element should be a ReadableStream');
  assert_equals(result[1].constructor, ReadableStream, '1st element should be a ReadableStream');
}, 'ReadableStream teeing with byte source: rs.tee() returns an array of two ReadableStreams');
promise_test(_async(function (t) {
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      c.enqueue(new Uint8Array([0x01]));
      c.enqueue(new Uint8Array([0x02]));
      c.close();
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  reader2.closed.then(t.unreached_func('branch2 should not be closed'));
  {
    return _await(reader1.read(new Uint8Array(1)), function (result) {
      assert_equals(result.done, false, 'done');
      assert_typed_array_equals(result.value, new Uint8Array([0x01]), 'value');
      {
        return _await(reader1.read(new Uint8Array(1)), function (result) {
          assert_equals(result.done, false, 'done');
          assert_typed_array_equals(result.value, new Uint8Array([0x02]), 'value');
          {
            return _await(reader1.read(new Uint8Array(1)), function (result) {
              assert_equals(result.done, true, 'done');
              assert_typed_array_equals(result.value, new Uint8Array([0]).subarray(0, 0), 'value');
              {
                return _await(reader2.read(new Uint8Array(1)), function (result) {
                  assert_equals(result.done, false, 'done');
                  assert_typed_array_equals(result.value, new Uint8Array([0x01]), 'value');
                  return _awaitIgnored(reader1.closed);
                });
              }
            });
          }
        });
      }
    });
  }
}), 'ReadableStream teeing with byte source: should be able to read one branch to the end without affecting the other');
promise_test(_async(function () {
  var pullCount = 0;
  var enqueuedChunk = new Uint8Array([0x01]);
  var rs = new ReadableStream({
    type: 'bytes',
    pull(c) {
      ++pullCount;
      if (pullCount === 1) {
        c.enqueue(enqueuedChunk);
      }
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader();
  return _await(Promise.all([reader1.read(), reader2.read()]), function ([result1, result2]) {
    assert_equals(result1.done, false, 'reader1 done');
    assert_equals(result2.done, false, 'reader2 done');
    var view1 = result1.value;
    var view2 = result2.value;
    assert_typed_array_equals(view1, new Uint8Array([0x01]), 'reader1 value');
    assert_typed_array_equals(view2, new Uint8Array([0x01]), 'reader2 value');
    assert_not_equals(view1.buffer, view2.buffer, 'chunks should have different buffers');
    assert_not_equals(enqueuedChunk.buffer, view1.buffer, 'enqueued chunk and branch1\'s chunk should have different buffers');
    assert_not_equals(enqueuedChunk.buffer, view2.buffer, 'enqueued chunk and branch2\'s chunk should have different buffers');
  });
}), 'ReadableStream teeing with byte source: chunks should be cloned for each branch');
promise_test(_async(function () {
  var pullCount = 0;
  var rs = new ReadableStream({
    type: 'bytes',
    pull(c) {
      ++pullCount;
      if (pullCount === 1) {
        c.byobRequest.view[0] = 0x01;
        c.byobRequest.respond(1);
      }
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader();
  var buffer = new Uint8Array([42, 42, 42]).buffer;
  {
    return _await(reader1.read(new Uint8Array(buffer, 0, 1)), function (result) {
      assert_equals(result.done, false, 'done');
      assert_typed_array_equals(result.value, new Uint8Array([0x01, 42, 42]).subarray(0, 1), 'value');
      {
        return _await(reader2.read(), function (result) {
          assert_equals(result.done, false, 'done');
          assert_typed_array_equals(result.value, new Uint8Array([0x01]), 'value');
        });
      }
    });
  }
}), 'ReadableStream teeing with byte source: chunks for BYOB requests from branch 1 should be cloned to branch 2');
promise_test(_async(function (t) {
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      c.enqueue(new Uint8Array([0x01]));
      c.enqueue(new Uint8Array([0x02]));
    },
    pull() {
      throw theError;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  {
    return _await(reader1.read(new Uint8Array(1)), function (result) {
      assert_equals(result.done, false, 'first read from branch1 should not be done');
      assert_typed_array_equals(result.value, new Uint8Array([0x01]), 'first read from branch1');
      {
        return _await(reader1.read(new Uint8Array(1)), function (result) {
          assert_equals(result.done, false, 'second read from branch1 should not be done');
          assert_typed_array_equals(result.value, new Uint8Array([0x02]), 'second read from branch1');
          return _await(promise_rejects_exactly(t, theError, reader1.read(new Uint8Array(1))), function () {
            return _await(promise_rejects_exactly(t, theError, reader2.read(new Uint8Array(1))), function () {
              return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]));
            });
          });
        });
      }
    });
  }
}), 'ReadableStream teeing with byte source: errors in the source should propagate to both branches');
promise_test(_async(function () {
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      c.enqueue(new Uint8Array([0x01]));
      c.enqueue(new Uint8Array([0x02]));
      c.close();
    }
  });
  var [branch1, branch2] = rs.tee();
  branch1.cancel();
  return _await(Promise.all([readableStreamToArray(branch1), readableStreamToArray(branch2)]), function ([chunks1, chunks2]) {
    assert_array_equals(chunks1, [], 'branch1 should have no chunks');
    assert_equals(chunks2.length, 2, 'branch2 should have two chunks');
    assert_typed_array_equals(chunks2[0], new Uint8Array([0x01]), 'first chunk from branch2');
    assert_typed_array_equals(chunks2[1], new Uint8Array([0x02]), 'second chunk from branch2');
  });
}), 'ReadableStream teeing with byte source: canceling branch1 should not impact branch2');
promise_test(_async(function () {
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      c.enqueue(new Uint8Array([0x01]));
      c.enqueue(new Uint8Array([0x02]));
      c.close();
    }
  });
  var [branch1, branch2] = rs.tee();
  branch2.cancel();
  return _await(Promise.all([readableStreamToArray(branch1), readableStreamToArray(branch2)]), function ([chunks1, chunks2]) {
    assert_equals(chunks1.length, 2, 'branch1 should have two chunks');
    assert_typed_array_equals(chunks1[0], new Uint8Array([0x01]), 'first chunk from branch1');
    assert_typed_array_equals(chunks1[1], new Uint8Array([0x02]), 'second chunk from branch1');
    assert_array_equals(chunks2, [], 'branch2 should have no chunks');
  });
}), 'ReadableStream teeing with byte source: canceling branch2 should not impact branch1');
templatedRSTeeCancel('ReadableStream teeing with byte source', extras => {
  return new ReadableStream({
    type: 'bytes',
    ...extras
  });
});
promise_test(_async(function () {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  var promise = Promise.all([reader1.closed, reader2.closed]);
  controller.close();

  // The branches are created with HWM 0, so we need to read from at least one of them
  // to observe the stream becoming closed.
  return _await(reader1.read(new Uint8Array(1)), function (read1) {
    assert_equals(read1.done, true, 'first read from branch1 should be done');
    return _awaitIgnored(promise);
  });
}), 'ReadableStream teeing with byte source: closing the original should close the branches');
promise_test(_async(function (t) {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  var theError = {
    name: 'boo!'
  };
  var promise = Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]);
  controller.error(theError);
  return _awaitIgnored(promise);
}), 'ReadableStream teeing with byte source: erroring the original should immediately error the branches');
promise_test(_async(function (t) {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader();
  var theError = {
    name: 'boo!'
  };
  var promise = Promise.all([promise_rejects_exactly(t, theError, reader1.read()), promise_rejects_exactly(t, theError, reader2.read())]);
  controller.error(theError);
  return _awaitIgnored(promise);
}), 'ReadableStream teeing with byte source: erroring the original should error pending reads from default reader');
promise_test(_async(function (t) {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  var theError = {
    name: 'boo!'
  };
  var promise = Promise.all([promise_rejects_exactly(t, theError, reader1.read(new Uint8Array(1))), promise_rejects_exactly(t, theError, reader2.read(new Uint8Array(1)))]);
  controller.error(theError);
  return _awaitIgnored(promise);
}), 'ReadableStream teeing with byte source: erroring the original should error pending reads from BYOB reader');
promise_test(_async(function () {
  var controller;
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  var cancelPromise = reader2.cancel();
  controller.enqueue(new Uint8Array([0x01]));
  return _await(reader1.read(new Uint8Array(1)), function (read1) {
    assert_equals(read1.done, false, 'first read() from branch1 should not be done');
    assert_typed_array_equals(read1.value, new Uint8Array([0x01]), 'first read() from branch1');
    controller.close();
    return _await(reader1.read(new Uint8Array(1)), function (read2) {
      assert_equals(read2.done, true, 'second read() from branch1 should be done');
      return _awaitIgnored(Promise.all([reader1.closed, cancelPromise]));
    });
  });
}), 'ReadableStream teeing with byte source: canceling branch1 should finish when branch2 reads until end of stream');
promise_test(_async(function (t) {
  var controller;
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    type: 'bytes',
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  var cancelPromise = reader2.cancel();
  controller.error(theError);
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader1.read(new Uint8Array(1))), cancelPromise]));
}), 'ReadableStream teeing with byte source: canceling branch1 should finish when original stream errors');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });

  // Create two branches, each with a HWM of 0. This should result in no chunks being pulled.
  rs.tee();
  return _call(flushAsyncEvents, function () {
    assert_array_equals(rs.events, [], 'pull should not be called');
  });
}), 'ReadableStream teeing with byte source: should not pull any chunks if no branches are reading');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes',
    pull(controller) {
      controller.enqueue(new Uint8Array([0x01]));
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  return _await(Promise.all([reader1.read(new Uint8Array(1)), reader2.read(new Uint8Array(1))]), function () {
    assert_array_equals(rs.events, ['pull'], 'pull should be called once');
  });
}), 'ReadableStream teeing with byte source: should only pull enough to fill the emptiest queue');
promise_test(_async(function (t) {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var theError = {
    name: 'boo!'
  };
  rs.controller.error(theError);
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  return _call(flushAsyncEvents, function () {
    assert_array_equals(rs.events, [], 'pull should not be called');
    return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]));
  });
}), 'ReadableStream teeing with byte source: should not pull when original is already errored');
var _loop = function (branch) {
  promise_test(_async(function (t) {
    var rs = recordingReadableStream({
      type: 'bytes'
    });
    var theError = {
      name: 'boo!'
    };
    var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
      mode: 'byob'
    }));
    return _call(flushAsyncEvents, function () {
      assert_array_equals(rs.events, [], 'pull should not be called');
      var reader = branch === 1 ? reader1 : reader2;
      var read1 = reader.read(new Uint8Array(1));
      return _call(flushAsyncEvents, function () {
        assert_array_equals(rs.events, ['pull'], 'pull should be called once');
        rs.controller.error(theError);
        return _await(Promise.all([promise_rejects_exactly(t, theError, read1), promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]), function () {
          return _call(flushAsyncEvents, function () {
            assert_array_equals(rs.events, ['pull'], 'pull should be called once');
          });
        });
      });
    });
  }), `ReadableStream teeing with byte source: stops pulling when original stream errors while branch ${branch} is reading`);
};
for (var branch of [1, 2]) {
  _loop(branch);
}
promise_test(_async(function (t) {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var theError = {
    name: 'boo!'
  };
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  return _call(flushAsyncEvents, function () {
    assert_array_equals(rs.events, [], 'pull should not be called');
    var read1 = reader1.read(new Uint8Array(1));
    var read2 = reader2.read(new Uint8Array(1));
    return _call(flushAsyncEvents, function () {
      assert_array_equals(rs.events, ['pull'], 'pull should be called once');
      rs.controller.error(theError);
      return _await(Promise.all([promise_rejects_exactly(t, theError, read1), promise_rejects_exactly(t, theError, read2), promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]), function () {
        return _call(flushAsyncEvents, function () {
          assert_array_equals(rs.events, ['pull'], 'pull should be called once');
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: stops pulling when original stream errors while both branches are reading');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  var read2 = reader2.read(new Uint8Array([0x22]));
  var cancel1 = reader1.cancel();
  return _call(flushAsyncEvents, function () {
    var cancel2 = reader2.cancel();
    return _await(read1, function (result1) {
      assert_object_equals(result1, {
        value: undefined,
        done: true
      });
      return _await(read2, function (result2) {
        assert_object_equals(result2, {
          value: undefined,
          done: true
        });
        return _awaitIgnored(Promise.all([cancel1, cancel2]));
      });
    });
  });
}), 'ReadableStream teeing with byte source: canceling both branches in sequence with delay');
promise_test(_async(function (t) {
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    type: 'bytes',
    cancel() {
      throw theError;
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  var read2 = reader2.read(new Uint8Array([0x22]));
  var cancel1 = reader1.cancel();
  return _call(flushAsyncEvents, function () {
    var cancel2 = reader2.cancel();
    return _await(read1, function (result1) {
      assert_object_equals(result1, {
        value: undefined,
        done: true
      });
      return _await(read2, function (result2) {
        assert_object_equals(result2, {
          value: undefined,
          done: true
        });
        return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, cancel1), promise_rejects_exactly(t, theError, cancel2)]));
      });
    });
  });
}), 'ReadableStream teeing with byte source: failing to cancel when canceling both branches in sequence with delay');
promise_test(_async(function () {
  var cancelResolve;
  var cancelCalled = new Promise(resolve => {
    cancelResolve = resolve;
  });
  var rs = recordingReadableStream({
    type: 'bytes',
    cancel() {
      cancelResolve();
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // We are reading into branch1's buffer.

      // Cancelling branch1 should not affect the BYOB request.

      // Cancelling branch1 should invalidate the BYOB request.
      var byobRequest1 = rs.controller.byobRequest;
      assert_not_equals(byobRequest1, null);
      assert_typed_array_equals(byobRequest1.view, new Uint8Array([0x11]), 'byobRequest1.view');
      var cancel1 = reader1.cancel();
      return _await(read1, function (result1) {
        assert_equals(result1.done, true);
        assert_equals(result1.value, undefined);
        return _call(flushAsyncEvents, function () {
          var byobRequest2 = rs.controller.byobRequest;
          assert_typed_array_equals(byobRequest2.view, new Uint8Array([0x11]), 'byobRequest2.view');
          var cancel2 = reader2.cancel();
          return _await(cancelCalled, function () {
            var byobRequest3 = rs.controller.byobRequest;
            assert_equals(byobRequest3, null);
            return _await(read2, function (result2) {
              assert_equals(result2.done, true);
              assert_equals(result2.value, undefined);
              return _awaitIgnored(Promise.all([cancel1, cancel2]));
            });
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch1 and branch2, cancel branch1, cancel branch2');
promise_test(_async(function () {
  var cancelResolve;
  var cancelCalled = new Promise(resolve => {
    cancelResolve = resolve;
  });
  var rs = recordingReadableStream({
    type: 'bytes',
    cancel() {
      cancelResolve();
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // We are reading into branch1's buffer.

      // Cancelling branch2 should not affect the BYOB request.

      // Cancelling branch1 should invalidate the BYOB request.
      var byobRequest1 = rs.controller.byobRequest;
      assert_not_equals(byobRequest1, null);
      assert_typed_array_equals(byobRequest1.view, new Uint8Array([0x11]), 'byobRequest1.view');
      var cancel2 = reader2.cancel();
      return _await(read2, function (result2) {
        assert_equals(result2.done, true);
        assert_equals(result2.value, undefined);
        return _call(flushAsyncEvents, function () {
          var byobRequest2 = rs.controller.byobRequest;
          assert_typed_array_equals(byobRequest2.view, new Uint8Array([0x11]), 'byobRequest2.view');
          var cancel1 = reader1.cancel();
          return _await(cancelCalled, function () {
            var byobRequest3 = rs.controller.byobRequest;
            assert_equals(byobRequest3, null);
            return _await(read1, function (result1) {
              assert_equals(result1.done, true);
              assert_equals(result1.value, undefined);
              return _awaitIgnored(Promise.all([cancel1, cancel2]));
            });
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch1 and branch2, cancel branch2, cancel branch1');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // We are reading into branch1's buffer.

      // Cancelling branch2 should not affect the BYOB request.

      // Respond to the BYOB request.

      // branch1 should receive the read chunk.
      assert_typed_array_equals(rs.controller.byobRequest.view, new Uint8Array([0x11]), 'first byobRequest.view');
      reader2.cancel();
      return _await(read2, function (result2) {
        assert_equals(result2.done, true);
        assert_equals(result2.value, undefined);
        return _call(flushAsyncEvents, function () {
          assert_typed_array_equals(rs.controller.byobRequest.view, new Uint8Array([0x11]), 'second byobRequest.view');
          rs.controller.byobRequest.view[0] = 0x33;
          rs.controller.byobRequest.respond(1);
          return _await(read1, function (result1) {
            assert_equals(result1.done, false);
            assert_typed_array_equals(result1.value, new Uint8Array([0x33]), 'first read() from branch1');
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch1 and branch2, cancel branch2, enqueue to branch1');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var read1 = reader1.read(new Uint8Array([0x11]));
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // We are reading into branch1's buffer.

      // Cancelling branch1 should not affect the BYOB request.

      // Respond to the BYOB request.

      // branch2 should receive the read chunk.
      assert_typed_array_equals(rs.controller.byobRequest.view, new Uint8Array([0x11]), 'first byobRequest.view');
      reader1.cancel();
      return _await(read1, function (result1) {
        assert_equals(result1.done, true);
        assert_equals(result1.value, undefined);
        return _call(flushAsyncEvents, function () {
          assert_typed_array_equals(rs.controller.byobRequest.view, new Uint8Array([0x11]), 'second byobRequest.view');
          rs.controller.byobRequest.view[0] = 0x33;
          rs.controller.byobRequest.respond(1);
          return _await(read2, function (result2) {
            assert_equals(result2.done, false);
            assert_typed_array_equals(result2.value, new Uint8Array([0x33]), 'first read() from branch2');
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch1 and branch2, cancel branch1, respond to branch2');
promise_test(_async(function () {
  var pullCount = 0;
  var byobRequestDefined = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull(c) {
      ++pullCount;
      byobRequestDefined.push(c.byobRequest !== null);
      c.enqueue(new Uint8Array([pullCount]));
    }
  });
  var [branch1, _] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  return _await(reader1.read(new Uint8Array([0x11])), function (result1) {
    assert_equals(result1.done, false, 'first read should not be done');
    assert_typed_array_equals(result1.value, new Uint8Array([0x1]), 'first read');
    assert_equals(pullCount, 1, 'pull() should be called once');
    assert_equals(byobRequestDefined[0], true, 'should have created a BYOB request for first read');
    reader1.releaseLock();
    var reader2 = branch1.getReader();
    return _await(reader2.read(), function (result2) {
      assert_equals(result2.done, false, 'second read should not be done');
      assert_typed_array_equals(result2.value, new Uint8Array([0x2]), 'second read');
      assert_equals(pullCount, 2, 'pull() should be called twice');
      assert_equals(byobRequestDefined[1], false, 'should not have created a BYOB request for second read');
    });
  });
}), 'ReadableStream teeing with byte source: pull with BYOB reader, then pull with default reader');
promise_test(_async(function () {
  var pullCount = 0;
  var byobRequestDefined = [];
  var rs = new ReadableStream({
    type: 'bytes',
    pull(c) {
      ++pullCount;
      byobRequestDefined.push(c.byobRequest !== null);
      c.enqueue(new Uint8Array([pullCount]));
    }
  });
  var [branch1, _] = rs.tee();
  var reader1 = branch1.getReader();
  return _await(reader1.read(), function (result1) {
    assert_equals(result1.done, false, 'first read should not be done');
    assert_typed_array_equals(result1.value, new Uint8Array([0x1]), 'first read');
    assert_equals(pullCount, 1, 'pull() should be called once');
    assert_equals(byobRequestDefined[0], false, 'should not have created a BYOB request for first read');
    reader1.releaseLock();
    var reader2 = branch1.getReader({
      mode: 'byob'
    });
    return _await(reader2.read(new Uint8Array([0x22])), function (result2) {
      assert_equals(result2.done, false, 'second read should not be done');
      assert_typed_array_equals(result2.value, new Uint8Array([0x2]), 'second read');
      assert_equals(pullCount, 2, 'pull() should be called twice');
      assert_equals(byobRequestDefined[1], true, 'should have created a BYOB request for second read');
    });
  });
}), 'ReadableStream teeing with byte source: pull with default reader, then pull with BYOB reader');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));

  // Wait for each branch's start() promise to resolve.
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read(new Uint8Array([0x22]));
    var read1 = reader1.read(new Uint8Array([0x11]));
    return _call(flushAsyncEvents, function () {
      // branch2 should provide the BYOB request.
      var byobRequest = rs.controller.byobRequest;
      assert_typed_array_equals(byobRequest.view, new Uint8Array([0x22]), 'first BYOB request');
      byobRequest.view[0] = 0x01;
      byobRequest.respond(1);
      return _await(read1, function (result1) {
        assert_equals(result1.done, false, 'first read should not be done');
        assert_typed_array_equals(result1.value, new Uint8Array([0x1]), 'first read');
        return _await(read2, function (result2) {
          assert_equals(result2.done, false, 'second read should not be done');
          assert_typed_array_equals(result2.value, new Uint8Array([0x1]), 'second read');
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch2, then read from branch1');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader({
    mode: 'byob'
  });
  return _call(flushAsyncEvents, function () {
    var read1 = reader1.read();
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // There should be no BYOB request.

      // Close the stream.

      // branch2 should get its buffer back.
      assert_equals(rs.controller.byobRequest, null, 'first BYOB request');
      rs.controller.close();
      return _await(read1, function (result1) {
        assert_equals(result1.done, true, 'read from branch1 should be done');
        assert_equals(result1.value, undefined, 'read from branch1');
        return _await(read2, function (result2) {
          assert_equals(result2.done, true, 'read from branch2 should be done');
          assert_typed_array_equals(result2.value, new Uint8Array([0x22]).subarray(0, 0), 'read from branch2');
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch1 with default reader, then close while branch2 has pending BYOB read');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader({
    mode: 'byob'
  });
  var reader2 = branch2.getReader();
  return _call(flushAsyncEvents, function () {
    var read2 = reader2.read();
    var read1 = reader1.read(new Uint8Array([0x11]));
    return _call(flushAsyncEvents, function () {
      // There should be no BYOB request.

      // Close the stream.

      // branch1 should get its buffer back.
      assert_equals(rs.controller.byobRequest, null, 'first BYOB request');
      rs.controller.close();
      return _await(read2, function (result2) {
        assert_equals(result2.done, true, 'read from branch2 should be done');
        assert_equals(result2.value, undefined, 'read from branch2');
        return _await(read1, function (result1) {
          assert_equals(result1.done, true, 'read from branch1 should be done');
          assert_typed_array_equals(result1.value, new Uint8Array([0x11]).subarray(0, 0), 'read from branch1');
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: read from branch2 with default reader, then close while branch1 has pending BYOB read');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  return _call(flushAsyncEvents, function () {
    var read1 = reader1.read(new Uint8Array([0x11]));
    var read2 = reader2.read(new Uint8Array([0x22]));
    return _call(flushAsyncEvents, function () {
      // branch1 should provide the BYOB request.

      // Close the stream.

      // Both branches should get their buffers back.
      var byobRequest = rs.controller.byobRequest;
      assert_typed_array_equals(byobRequest.view, new Uint8Array([0x11]), 'first BYOB request');
      rs.controller.close();
      byobRequest.respond(0);
      return _await(read1, function (result1) {
        assert_equals(result1.done, true, 'first read should be done');
        assert_typed_array_equals(result1.value, new Uint8Array([0x11]).subarray(0, 0), 'first read');
        return _await(read2, function (result2) {
          assert_equals(result2.done, true, 'second read should be done');
          assert_typed_array_equals(result2.value, new Uint8Array([0x22]).subarray(0, 0), 'second read');
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: close when both branches have pending BYOB reads');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  var branch1Reads = [reader1.read(), reader1.read()];
  var branch2Reads = [reader2.read(), reader2.read()];
  return _call(flushAsyncEvents, function () {
    rs.controller.enqueue(new Uint8Array([0x11]));
    rs.controller.close();
    return _await(branch1Reads[0], function (result1) {
      assert_equals(result1.done, false, 'first read() from branch1 should be not done');
      assert_typed_array_equals(result1.value, new Uint8Array([0x11]), 'first chunk from branch1 should be correct');
      return _await(branch2Reads[0], function (result2) {
        assert_equals(result2.done, false, 'first read() from branch2 should be not done');
        assert_typed_array_equals(result2.value, new Uint8Array([0x11]), 'first chunk from branch2 should be correct');
        var _assert_object_equals = assert_object_equals;
        return _await(branch1Reads[1], function (_branch1Reads$) {
          _assert_object_equals(_branch1Reads$, {
            value: undefined,
            done: true
          }, 'second read() from branch1 should be done');
          var _assert_object_equals2 = assert_object_equals;
          return _await(branch2Reads[1], function (_branch2Reads$) {
            _assert_object_equals2(_branch2Reads$, {
              value: undefined,
              done: true
            }, 'second read() from branch2 should be done');
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: enqueue() and close() while both branches are pulling');
promise_test(_async(function () {
  var rs = recordingReadableStream({
    type: 'bytes'
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader({
    mode: 'byob'
  }));
  var branch1Reads = [reader1.read(new Uint8Array(1)), reader1.read(new Uint8Array(1))];
  var branch2Reads = [reader2.read(new Uint8Array(1)), reader2.read(new Uint8Array(1))];
  return _call(flushAsyncEvents, function () {
    rs.controller.byobRequest.view[0] = 0x11;
    rs.controller.byobRequest.respond(1);
    rs.controller.close();
    return _await(branch1Reads[0], function (result1) {
      assert_equals(result1.done, false, 'first read() from branch1 should be not done');
      assert_typed_array_equals(result1.value, new Uint8Array([0x11]), 'first chunk from branch1 should be correct');
      return _await(branch2Reads[0], function (result2) {
        assert_equals(result2.done, false, 'first read() from branch2 should be not done');
        assert_typed_array_equals(result2.value, new Uint8Array([0x11]), 'first chunk from branch2 should be correct');
        return _await(branch1Reads[1], function (result3) {
          assert_equals(result3.done, true, 'second read() from branch1 should be done');
          assert_typed_array_equals(result3.value, new Uint8Array([0]).subarray(0, 0), 'second chunk from branch1 should be correct');
          return _await(branch2Reads[1], function (result4) {
            assert_equals(result4.done, true, 'second read() from branch2 should be done');
            assert_typed_array_equals(result4.value, new Uint8Array([0]).subarray(0, 0), 'second chunk from branch2 should be correct');
          });
        });
      });
    });
  });
}), 'ReadableStream teeing with byte source: respond() and close() while both branches are pulling');
promise_test(_async(function (t) {
  var pullCount = 0;
  var arrayBuffer = new Uint8Array([0x01, 0x02, 0x03]).buffer;
  var enqueuedChunk = new Uint8Array(arrayBuffer, 2);
  assert_equals(enqueuedChunk.length, 1);
  assert_equals(enqueuedChunk.byteOffset, 2);
  var rs = new ReadableStream({
    type: 'bytes',
    pull(c) {
      ++pullCount;
      if (pullCount === 1) {
        c.enqueue(enqueuedChunk);
      }
    }
  });
  var [branch1, branch2] = rs.tee();
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader();
  return _await(Promise.all([reader1.read(), reader2.read()]), function ([result1, result2]) {
    assert_equals(result1.done, false, 'reader1 done');
    assert_equals(result2.done, false, 'reader2 done');
    var view1 = result1.value;
    var view2 = result2.value;
    // The first stream has the transferred buffer, but the second stream has the
    // cloned buffer.
    var underlying = new Uint8Array([0x01, 0x02, 0x03]).buffer;
    assert_typed_array_equals(view1, new Uint8Array(underlying, 2), 'reader1 value');
    assert_typed_array_equals(view2, new Uint8Array([0x03]), 'reader2 value');
  });
}), 'ReadableStream teeing with byte source: reading an array with a byte offset should clone correctly');