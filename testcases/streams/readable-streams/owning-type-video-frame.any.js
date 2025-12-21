// META: global=window,worker
// META: script=../resources/test-utils.js
// META: script=../resources/rs-utils.js
'use strict';

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
function _empty() {}
function _awaitIgnored(value, direct) {
  if (!direct) {
    return value && value.then ? value.then(_empty) : Promise.resolve();
  }
}
function createVideoFrame() {
  var init = {
    format: 'I420',
    timestamp: 1234,
    codedWidth: 4,
    codedHeight: 2
  };
  var data = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8,
  // y
  1, 2,
  // u
  1, 2 // v
  ]);
  return new VideoFrame(data, init);
}
promise_test(_async(function () {
  var videoFrame = createVideoFrame();
  videoFrame.test = 1;
  var source = {
    start(controller) {
      assert_equals(videoFrame.format, 'I420');
      controller.enqueue(videoFrame, {
        transfer: [videoFrame]
      });
      assert_equals(videoFrame.format, null);
      assert_equals(videoFrame.test, 1);
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  // Cancelling the stream should close all video frames, thus no console messages of GCing VideoFrames should happen.
  stream.cancel();
  return _await();
}), 'ReadableStream of type owning should close serialized chunks');
promise_test(_async(function () {
  var videoFrame = createVideoFrame();
  videoFrame.test = 1;
  var source = {
    start(controller) {
      assert_equals(videoFrame.format, 'I420');
      controller.enqueue({
        videoFrame
      }, {
        transfer: [videoFrame]
      });
      assert_equals(videoFrame.format, null);
      assert_equals(videoFrame.test, 1);
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var reader = stream.getReader();
  return _await(reader.read(), function (chunk) {
    assert_equals(chunk.value.videoFrame.format, 'I420');
    assert_equals(chunk.value.videoFrame.test, undefined);
    chunk.value.videoFrame.close();
  });
}), 'ReadableStream of type owning should transfer JS chunks with transferred values');
promise_test(_async(function (t) {
  var videoFrame = createVideoFrame();
  videoFrame.close();
  var source = {
    start(controller) {
      assert_throws_dom("DataCloneError", () => controller.enqueue(videoFrame, {
        transfer: [videoFrame]
      }));
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var reader = stream.getReader();
  return _awaitIgnored(promise_rejects_dom(t, "DataCloneError", reader.read()));
}), 'ReadableStream of type owning should error when trying to enqueue not serializable values');
promise_test(_async(function () {
  var videoFrame = createVideoFrame();
  var source = {
    start(controller) {
      controller.enqueue(videoFrame, {
        transfer: [videoFrame]
      });
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var [clone1, clone2] = stream.tee();
  return _await(clone1.getReader().read(), function (chunk1) {
    return _await(clone2.getReader().read(), function (chunk2) {
      assert_equals(videoFrame.format, null);
      assert_equals(chunk1.value.format, 'I420');
      assert_equals(chunk2.value.format, 'I420');
      chunk1.value.close();
      chunk2.value.close();
    });
  });
}), 'ReadableStream of type owning should clone serializable objects when teeing');
promise_test(_async(function () {
  var videoFrame = createVideoFrame();
  videoFrame.test = 1;
  var source = {
    start(controller) {
      assert_equals(videoFrame.format, 'I420');
      controller.enqueue({
        videoFrame
      }, {
        transfer: [videoFrame]
      });
      assert_equals(videoFrame.format, null);
      assert_equals(videoFrame.test, 1);
    },
    type: 'owning'
  };
  var stream = new ReadableStream(source);
  var [clone1, clone2] = stream.tee();
  return _await(clone1.getReader().read(), function (chunk1) {
    return _await(clone2.getReader().read(), function (chunk2) {
      assert_equals(videoFrame.format, null);
      assert_equals(chunk1.value.videoFrame.format, 'I420');
      assert_equals(chunk2.value.videoFrame.format, 'I420');
      chunk1.value.videoFrame.close();
      chunk2.value.videoFrame.close();
    });
  });
}), 'ReadableStream of type owning should clone JS Objects with serializables when teeing');