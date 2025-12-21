"use strict";

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
function _callIgnored(body, direct) {
  return _call(body, _empty, direct);
}
function receiveEventOnce(target, name) {
  return new Promise(resolve => {
    target.addEventListener(name, ev => {
      resolve(ev);
    }, {
      once: true
    });
  });
}
var mixedTransferMessagePortWith = function () {
  return _call(mixedTransferMessagePortWithOrder1, function () {
    return _call(mixedTransferMessagePortWithOrder2, function () {
      return _callIgnored(mixedTransferMessagePortWithOrder3);
    });
  });
};
var mixedTransferMessagePortWithOrder3 = _async(function () {
  var channel = new MessageChannel();
  var readable1 = new ReadableStream();
  var readable2 = new ReadableStream();
  var writable1 = new WritableStream();
  var writable2 = new WritableStream();
  var transform1 = new TransformStream();
  var transform2 = new TransformStream();
  return _awaitIgnored(postAndTestMessageEvent({
    readable1,
    writable1,
    transform1,
    readable2,
    writable2,
    transform2
  }, [transform2, channel.port1, readable1, channel.port2, writable2, readable2, writable1, transform1], `when transferring [TransformStream, MessagePort, ReadableStream, MessagePort, WritableStream, ReadableStream, WritableStream, TransformStream] but with the data having different order`));
});
var mixedTransferMessagePortWithOrder2 = _async(function () {
  var channel = new MessageChannel();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  var transform = new TransformStream();
  return _awaitIgnored(postAndTestMessageEvent({
    readable,
    writable,
    transform
  }, [transform, channel.port1, readable, channel.port2, writable], `when transferring [TransformStream, MessagePort, ReadableStream, MessagePort, WritableStream]`));
});
var mixedTransferMessagePortWithOrder1 = _async(function () {
  var channel = new MessageChannel();
  var readable = new ReadableStream();
  var writable = new WritableStream();
  var transform = new TransformStream();
  return _awaitIgnored(postAndTestMessageEvent({
    readable,
    writable,
    transform,
    port1: channel.port1,
    port2: channel.port2
  }, [readable, writable, transform, channel.port1, channel.port2], `when transferring [ReadableStream, WritableStream, TransformStream, MessagePort, MessagePort]`));
});
var advancedTransferMessagePortWith = _async(function (constructor) {
  return _await(transferMessagePortWithOrder4(new constructor()), function () {
    return _await(transferMessagePortWithOrder5(new constructor()), function () {
      return _await(transferMessagePortWithOrder6(new constructor()), function () {
        return _awaitIgnored(transferMessagePortWithOrder7(new constructor()));
      });
    });
  });
});
var transferMessagePortWith = _async(function (constructor) {
  return _await(transferMessagePortWithOrder1(new constructor()), function () {
    return _await(transferMessagePortWithOrder2(new constructor()), function () {
      return _awaitIgnored(transferMessagePortWithOrder3(new constructor()));
    });
  });
});
var transferMessagePortWithOrder7 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    stream
  }, [channel.port1, stream, channel.port2], `when transferring [MessagePort, ${stream.constructor.name}, MessagePort] but with ports not being in the data`));
});
var transferMessagePortWithOrder6 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    port2: channel.port2,
    port1: channel.port1
  }, [channel.port1, stream, channel.port2], `when transferring [MessagePort, ${stream.constructor.name}, MessagePort] but with stream not being in the data`));
});
var transferMessagePortWithOrder5 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    port2: channel.port2,
    port1: channel.port1,
    stream
  }, [channel.port1, stream, channel.port2], `when transferring [MessagePort, ${stream.constructor.name}, MessagePort] but with data having different order`));
});
var transferMessagePortWithOrder4 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({}, [channel.port1, stream, channel.port2], `when transferring [MessagePort, ${stream.constructor.name}, MessagePort] but with empty data`));
});
var transferMessagePortWithOrder3 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    port1: channel.port1,
    stream,
    port2: channel.port2
  }, [channel.port1, stream, channel.port2], `when transferring [MessagePort, ${stream.constructor.name}, MessagePort]`));
});
var transferMessagePortWithOrder2 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    stream,
    port2: channel.port2
  }, [channel.port2, stream], `when transferring [MessagePort, ${stream.constructor.name}]`));
});
var transferMessagePortWithOrder1 = _async(function (stream) {
  var channel = new MessageChannel();
  return _awaitIgnored(postAndTestMessageEvent({
    stream,
    port2: channel.port2
  }, [stream, channel.port2], `when transferring [${stream.constructor.name}, MessagePort]`));
});
var postAndTestMessageEvent = _async(function (data, transfer, title) {
  postMessage(data, "*", transfer);
  var messagePortCount = transfer.filter(i => i instanceof MessagePort).length;
  return _await(receiveEventOnce(window, "message"), function (ev) {
    assert_equals(ev.ports.length, messagePortCount, `Correct number of ports ${title}`);
    for (var [i, port] of ev.ports.entries()) {
      assert_true(port instanceof MessagePort, `ports[${i}] include MessagePort ${title}`);
    }
    for (var [key, value] of Object.entries(data)) {
      assert_true(ev.data[key] instanceof value.constructor, `data.${key} has correct interface ${value.constructor.name} ${title}`);
    }
  });
});
promise_test(_async(function (t) {
  return _awaitIgnored(transferMessagePortWith(ReadableStream));
}), "Transferring a MessagePort with a ReadableStream should set `.ports`");
promise_test(_async(function (t) {
  return _awaitIgnored(transferMessagePortWith(WritableStream));
}), "Transferring a MessagePort with a WritableStream should set `.ports`");
promise_test(_async(function (t) {
  return _awaitIgnored(transferMessagePortWith(TransformStream));
}), "Transferring a MessagePort with a TransformStream should set `.ports`");
promise_test(_async(function (t) {
  return _awaitIgnored(advancedTransferMessagePortWith(ReadableStream));
}), "Transferring a MessagePort with a ReadableStream should set `.ports`, advanced");
promise_test(_async(function (t) {
  return _awaitIgnored(advancedTransferMessagePortWith(WritableStream));
}), "Transferring a MessagePort with a WritableStream should set `.ports`, advanced");
promise_test(_async(function (t) {
  return _awaitIgnored(advancedTransferMessagePortWith(TransformStream));
}), "Transferring a MessagePort with a TransformStream should set `.ports`, advanced");
promise_test(_async(function (t) {
  return _callIgnored(mixedTransferMessagePortWith);
}), "Transferring a MessagePort with multiple streams should set `.ports`");
test(() => {
  assert_throws_dom("DataCloneError", () => postMessage({
    stream: new ReadableStream()
  }, "*"));
}, "ReadableStream must not be serializable");
test(() => {
  assert_throws_dom("DataCloneError", () => postMessage({
    stream: new WritableStream()
  }, "*"));
}, "WritableStream must not be serializable");
test(() => {
  assert_throws_dom("DataCloneError", () => postMessage({
    stream: new TransformStream()
  }, "*"));
}, "TransformStream must not be serializable");