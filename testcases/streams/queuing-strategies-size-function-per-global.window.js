var iframe = document.createElement('iframe');
document.body.appendChild(iframe);
var _loop = function (type) {
  test(() => {
    var myQs = new window[type]({
      highWaterMark: 1
    });
    var yourQs = new iframe.contentWindow[type]({
      highWaterMark: 1
    });
    assert_not_equals(myQs.size, yourQs.size, 'size should not be the same object');
  }, `${type} size should be different for objects in different realms`);
};
for (var type of ['CountQueuingStrategy', 'ByteLengthQueuingStrategy']) {
  _loop(type);
}

// Cleanup the document to avoid messing up the result page.
iframe.remove();