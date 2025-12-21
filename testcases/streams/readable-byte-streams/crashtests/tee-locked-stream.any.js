// META: global=window,worker
'use strict';

test(() => {
  var byteReadable = new ReadableStream({
    type: 'bytes'
  });
  byteReadable.getReader();
  assert_throws_js(TypeError, () => byteReadable.tee(), 'byteReadable.tee() must throw');
}, 'tee() on a locked byte stream does not crash');