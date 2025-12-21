// TransformStream should still work even if the realm is detached.

// Adds an iframe to the document and returns it.

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
function addIframe() {
  var iframe = document.createElement('iframe');
  document.body.appendChild(iframe);
  return iframe;
}
promise_test(_async(function (t) {
  var iframe = addIframe();
  var stream = new iframe.contentWindow.TransformStream();
  var readPromise = stream.readable.getReader().read();
  var writer = stream.writable.getWriter();
  iframe.remove();
  return Promise.all([writer.write('A'), readPromise]);
}), 'TransformStream: write in detached realm should succeed');