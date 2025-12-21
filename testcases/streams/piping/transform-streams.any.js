// META: global=window,worker,shadowrealm
'use strict';

promise_test(() => {
  var rs = new ReadableStream({
    start(c) {
      c.enqueue('a');
      c.enqueue('b');
      c.enqueue('c');
      c.close();
    }
  });
  var ts = new TransformStream();
  var ws = new WritableStream();
  return rs.pipeThrough(ts).pipeTo(ws).then(() => {
    var writer = ws.getWriter();
    return writer.closed;
  });
}, 'Piping through an identity transform stream should close the destination when the source closes');