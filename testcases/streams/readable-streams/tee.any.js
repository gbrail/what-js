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
  var rs = new ReadableStream();
  var result = rs.tee();
  assert_true(Array.isArray(result), 'return value should be an array');
  assert_equals(result.length, 2, 'array should have length 2');
  assert_equals(result[0].constructor, ReadableStream, '0th element should be a ReadableStream');
  assert_equals(result[1].constructor, ReadableStream, '1st element should be a ReadableStream');
}, 'ReadableStream teeing: rs.tee() returns an array of two ReadableStreams');
promise_test(t => {
  var rs = new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.close();
    }
  });
  var branch = rs.tee();
  var branch1 = branch[0];
  var branch2 = branch[1];
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader();
  reader2.closed.then(t.unreached_func('branch2 should not be closed'));
  return Promise.all([reader1.closed, reader1.read().then(r => {
    assert_object_equals(r, {
      value: 'a',
      done: false
    }, 'first chunk from branch1 should be correct');
  }), reader1.read().then(r => {
    assert_object_equals(r, {
      value: 'b',
      done: false
    }, 'second chunk from branch1 should be correct');
  }), reader1.read().then(r => {
    assert_object_equals(r, {
      value: undefined,
      done: true
    }, 'third read() from branch1 should be done');
  }), reader2.read().then(r => {
    assert_object_equals(r, {
      value: 'a',
      done: false
    }, 'first chunk from branch2 should be correct');
  })]);
}, 'ReadableStream teeing: should be able to read one branch to the end without affecting the other');
promise_test(() => {
  var theObject = {
    the: 'test object'
  };
  var rs = new ReadableStream({
    start(c) {
      c.enqueue(theObject);
    }
  });
  var branch = rs.tee();
  var branch1 = branch[0];
  var branch2 = branch[1];
  var reader1 = branch1.getReader();
  var reader2 = branch2.getReader();
  return Promise.all([reader1.read(), reader2.read()]).then(values => {
    assert_object_equals(values[0], values[1], 'the values should be equal');
  });
}, 'ReadableStream teeing: values should be equal across each branch');
promise_test(t => {
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
    },
    pull() {
      throw theError;
    }
  });
  var branches = rs.tee();
  var reader1 = branches[0].getReader();
  var reader2 = branches[1].getReader();
  reader1.label = 'reader1';
  reader2.label = 'reader2';
  return Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed), reader1.read().then(r => {
    assert_object_equals(r, {
      value: 'a',
      done: false
    }, 'should be able to read the first chunk in branch1');
  }), reader1.read().then(r => {
    assert_object_equals(r, {
      value: 'b',
      done: false
    }, 'should be able to read the second chunk in branch1');
    return promise_rejects_exactly(t, theError, reader2.read());
  }).then(() => promise_rejects_exactly(t, theError, reader1.read()))]);
}, 'ReadableStream teeing: errors in the source should propagate to both branches');
promise_test(() => {
  var rs = new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.close();
    }
  });
  var branches = rs.tee();
  var branch1 = branches[0];
  var branch2 = branches[1];
  branch1.cancel();
  return Promise.all([readableStreamToArray(branch1).then(chunks => {
    assert_array_equals(chunks, [], 'branch1 should have no chunks');
  }), readableStreamToArray(branch2).then(chunks => {
    assert_array_equals(chunks, ['a', 'b'], 'branch2 should have two chunks');
  })]);
}, 'ReadableStream teeing: canceling branch1 should not impact branch2');
promise_test(() => {
  var rs = new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.close();
    }
  });
  var branches = rs.tee();
  var branch1 = branches[0];
  var branch2 = branches[1];
  branch2.cancel();
  return Promise.all([readableStreamToArray(branch1).then(chunks => {
    assert_array_equals(chunks, ['a', 'b'], 'branch1 should have two chunks');
  }), readableStreamToArray(branch2).then(chunks => {
    assert_array_equals(chunks, [], 'branch2 should have no chunks');
  })]);
}, 'ReadableStream teeing: canceling branch2 should not impact branch1');
templatedRSTeeCancel('ReadableStream teeing', extras => {
  return new ReadableStream({
    ...extras
  });
});
promise_test(t => {
  var controller;
  var stream = new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  var [branch1, branch2] = stream.tee();
  var error = new Error();
  error.name = 'distinctive';

  // Ensure neither branch is waiting in ReadableStreamDefaultReaderRead().
  controller.enqueue();
  controller.enqueue();
  return delay(0).then(() => {
    // This error will have to be detected via [[closedPromise]].
    controller.error(error);
    var reader1 = branch1.getReader();
    var reader2 = branch2.getReader();
    return Promise.all([promise_rejects_exactly(t, error, reader1.closed, 'reader1.closed should reject'), promise_rejects_exactly(t, error, reader2.closed, 'reader2.closed should reject')]);
  });
}, 'ReadableStream teeing: erroring a teed stream should error both branches');
promise_test(() => {
  var controller;
  var rs = new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  var branches = rs.tee();
  var reader1 = branches[0].getReader();
  var reader2 = branches[1].getReader();
  var promise = Promise.all([reader1.closed, reader2.closed]);
  controller.close();
  return promise;
}, 'ReadableStream teeing: closing the original should immediately close the branches');
promise_test(t => {
  var controller;
  var rs = new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  var branches = rs.tee();
  var reader1 = branches[0].getReader();
  var reader2 = branches[1].getReader();
  var theError = {
    name: 'boo!'
  };
  var promise = Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]);
  controller.error(theError);
  return promise;
}, 'ReadableStream teeing: erroring the original should immediately error the branches');
promise_test(_async(function (t) {
  var controller;
  var rs = new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  var cancelPromise = reader2.cancel();
  controller.enqueue('a');
  return _await(reader1.read(), function (read1) {
    assert_object_equals(read1, {
      value: 'a',
      done: false
    }, 'first read() from branch1 should fulfill with the chunk');
    controller.close();
    return _await(reader1.read(), function (read2) {
      assert_object_equals(read2, {
        value: undefined,
        done: true
      }, 'second read() from branch1 should be done');
      return _awaitIgnored(Promise.all([reader1.closed, cancelPromise]));
    });
  });
}), 'ReadableStream teeing: canceling branch1 should finish when branch2 reads until end of stream');
promise_test(_async(function (t) {
  var controller;
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    start(c) {
      controller = c;
    }
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  var cancelPromise = reader2.cancel();
  controller.error(theError);
  return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, reader1.read()), cancelPromise]));
}), 'ReadableStream teeing: canceling branch1 should finish when original stream errors');
promise_test(_async(function () {
  var rs = new ReadableStream({});
  var [branch1, branch2] = rs.tee();
  var cancel1 = branch1.cancel();
  return _call(flushAsyncEvents, function () {
    var cancel2 = branch2.cancel();
    return _awaitIgnored(Promise.all([cancel1, cancel2]));
  });
}), 'ReadableStream teeing: canceling both branches in sequence with delay');
promise_test(_async(function (t) {
  var theError = {
    name: 'boo!'
  };
  var rs = new ReadableStream({
    cancel() {
      throw theError;
    }
  });
  var [branch1, branch2] = rs.tee();
  var cancel1 = branch1.cancel();
  return _call(flushAsyncEvents, function () {
    var cancel2 = branch2.cancel();
    return _awaitIgnored(Promise.all([promise_rejects_exactly(t, theError, cancel1), promise_rejects_exactly(t, theError, cancel2)]));
  });
}), 'ReadableStream teeing: failing to cancel when canceling both branches in sequence with delay');
test(t => {
  // Copy original global.
  var oldReadableStream = ReadableStream;
  var getReader = ReadableStream.prototype.getReader;
  var origRS = new ReadableStream();

  // Replace the global ReadableStream constructor with one that doesn't work.
  ReadableStream = function () {
    throw new Error('global ReadableStream constructor called');
  };
  t.add_cleanup(() => {
    ReadableStream = oldReadableStream;
  });

  // This will probably fail if the global ReadableStream constructor was used.
  var [rs1, rs2] = origRS.tee();

  // These will definitely fail if the global ReadableStream constructor was used.
  assert_not_equals(getReader.call(rs1), undefined, 'getReader should work on rs1');
  assert_not_equals(getReader.call(rs2), undefined, 'getReader should work on rs2');
}, 'ReadableStreamTee should not use a modified ReadableStream constructor from the global object');
promise_test(t => {
  var rs = recordingReadableStream({}, {
    highWaterMark: 0
  });

  // Create two branches, each with a HWM of 1. This should result in one
  // chunk being pulled, not two.
  rs.tee();
  return flushAsyncEvents().then(() => {
    assert_array_equals(rs.events, ['pull'], 'pull should only be called once');
  });
}, 'ReadableStreamTee should not pull more chunks than can fit in the branch queue');
promise_test(t => {
  var rs = recordingReadableStream({
    pull(controller) {
      controller.enqueue('a');
    }
  }, {
    highWaterMark: 0
  });
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  return Promise.all([reader1.read(), reader2.read()]).then(() => {
    assert_array_equals(rs.events, ['pull', 'pull'], 'pull should be called twice');
  });
}, 'ReadableStreamTee should only pull enough to fill the emptiest queue');
promise_test(t => {
  var rs = recordingReadableStream({}, {
    highWaterMark: 0
  });
  var theError = {
    name: 'boo!'
  };
  rs.controller.error(theError);
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  return flushAsyncEvents().then(() => {
    assert_array_equals(rs.events, [], 'pull should not be called');
    return Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]);
  });
}, 'ReadableStreamTee should not pull when original is already errored');
var _loop = function (branch) {
  promise_test(t => {
    var rs = recordingReadableStream({}, {
      highWaterMark: 0
    });
    var theError = {
      name: 'boo!'
    };
    var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
    return flushAsyncEvents().then(() => {
      assert_array_equals(rs.events, ['pull'], 'pull should be called once');
      rs.controller.enqueue('a');
      var reader = branch === 1 ? reader1 : reader2;
      return reader.read();
    }).then(() => flushAsyncEvents()).then(() => {
      assert_array_equals(rs.events, ['pull', 'pull'], 'pull should be called twice');
      rs.controller.error(theError);
      return Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]);
    }).then(() => flushAsyncEvents()).then(() => {
      assert_array_equals(rs.events, ['pull', 'pull'], 'pull should be called twice');
    });
  }, `ReadableStreamTee stops pulling when original stream errors while branch ${branch} is reading`);
};
for (var branch of [1, 2]) {
  _loop(branch);
}
promise_test(t => {
  var rs = recordingReadableStream({}, {
    highWaterMark: 0
  });
  var theError = {
    name: 'boo!'
  };
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  return flushAsyncEvents().then(() => {
    assert_array_equals(rs.events, ['pull'], 'pull should be called once');
    rs.controller.enqueue('a');
    return Promise.all([reader1.read(), reader2.read()]);
  }).then(() => flushAsyncEvents()).then(() => {
    assert_array_equals(rs.events, ['pull', 'pull'], 'pull should be called twice');
    rs.controller.error(theError);
    return Promise.all([promise_rejects_exactly(t, theError, reader1.closed), promise_rejects_exactly(t, theError, reader2.closed)]);
  }).then(() => flushAsyncEvents()).then(() => {
    assert_array_equals(rs.events, ['pull', 'pull'], 'pull should be called twice');
  });
}, 'ReadableStreamTee stops pulling when original stream errors while both branches are reading');
promise_test(_async(function () {
  var rs = recordingReadableStream();
  var [reader1, reader2] = rs.tee().map(branch => branch.getReader());
  var branch1Reads = [reader1.read(), reader1.read()];
  var branch2Reads = [reader2.read(), reader2.read()];
  return _call(flushAsyncEvents, function () {
    rs.controller.enqueue('a');
    rs.controller.close();
    var _assert_object_equals = assert_object_equals;
    return _await(branch1Reads[0], function (_branch1Reads$) {
      _assert_object_equals(_branch1Reads$, {
        value: 'a',
        done: false
      }, 'first chunk from branch1 should be correct');
      var _assert_object_equals2 = assert_object_equals;
      return _await(branch2Reads[0], function (_branch2Reads$) {
        _assert_object_equals2(_branch2Reads$, {
          value: 'a',
          done: false
        }, 'first chunk from branch2 should be correct');
        var _assert_object_equals3 = assert_object_equals;
        return _await(branch1Reads[1], function (_branch1Reads$2) {
          _assert_object_equals3(_branch1Reads$2, {
            value: undefined,
            done: true
          }, 'second read() from branch1 should be done');
          var _assert_object_equals4 = assert_object_equals;
          return _await(branch2Reads[1], function (_branch2Reads$2) {
            _assert_object_equals4(_branch2Reads$2, {
              value: undefined,
              done: true
            }, 'second read() from branch2 should be done');
          });
        });
      });
    });
  });
}), 'ReadableStream teeing: enqueue() and close() while both branches are pulling');