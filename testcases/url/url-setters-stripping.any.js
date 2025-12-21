function urlString({
  scheme = "https",
  username = "username",
  password = "password",
  host = "host",
  port = "8000",
  pathname = "path",
  search = "query",
  hash = "fragment"
}) {
  return `${scheme}://${username}:${password}@${host}:${port}/${pathname}?${search}#${hash}`;
}
function urlRecord(scheme) {
  return new URL(urlString({
    scheme
  }));
}
var _loop = function (scheme) {
  var _loop2 = function (i) {
    var stripped = i === 0x09 || i === 0x0A || i === 0x0D;

    // It turns out that user agents are surprisingly similar for these ranges so generate fewer
    // tests. If this is changed also change the logic for host below.
    if (i !== 0 && i !== 0x1F && !stripped) {
      return 1; // continue
    }
    var cpString = String.fromCodePoint(i);
    var cpReference = "U+" + i.toString(16).toUpperCase().padStart(4, "0");
    test(() => {
      var expected = scheme === "https" ? stripped ? "http" : "https" : stripped ? "wpt--" : "wpt++";
      var url = urlRecord(scheme);
      url.protocol = String.fromCodePoint(i) + (scheme === "https" ? "http" : "wpt--");
      assert_equals(url.protocol, expected + ":", "property");
      assert_equals(url.href, urlString({
        scheme: expected
      }), "href");
    }, `Setting protocol with leading ${cpReference} (${scheme}:)`);
    test(() => {
      var expected = scheme === "https" ? stripped ? "http" : "https" : stripped ? "wpt--" : "wpt++";
      var url = urlRecord(scheme);
      url.protocol = (scheme === "https" ? "http" : "wpt--") + String.fromCodePoint(i);
      assert_equals(url.protocol, expected + ":", "property");
      assert_equals(url.href, urlString({
        scheme: expected
      }), "href");
    }, `Setting protocol with ${cpReference} before inserted colon (${scheme}:)`);

    // Cannot test protocol with trailing as the algorithm inserts a colon before proceeding

    // These do no stripping
    var _loop3 = function (property) {
      var _loop6 = function (expected, _input) {
        test(() => {
          var url = urlRecord(scheme);
          url[property] = _input;
          assert_equals(url[property], expected, "property");
          assert_equals(url.href, urlString({
            scheme,
            [property]: expected
          }), "href");
        }, `Setting ${property} with ${_type} ${cpReference} (${scheme}:)`);
      };
      for (var [_type, expected, _input] of [["leading", encodeURIComponent(cpString) + "test", String.fromCodePoint(i) + "test"], ["middle", "te" + encodeURIComponent(cpString) + "st", "te" + String.fromCodePoint(i) + "st"], ["trailing", "test" + encodeURIComponent(cpString), "test" + String.fromCodePoint(i)]]) {
        _loop6(expected, _input);
      }
    };
    for (var property of ["username", "password"]) {
      _loop3(property);
    }
    var _loop4 = function (expectedPart, input) {
      test(() => {
        var expected = i === 0x00 || scheme === "https" && i === 0x1F ? "host" : stripped ? "test" : expectedPart;
        var url = urlRecord(scheme);
        url.host = input;
        assert_equals(url.host, expected + ":8000", "property");
        assert_equals(url.href, urlString({
          scheme,
          host: expected
        }), "href");
      }, `Setting host with ${type} ${cpReference} (${scheme}:)`);
      test(() => {
        var expected = i === 0x00 || scheme === "https" && i === 0x1F ? "host" : stripped ? "test" : expectedPart;
        var url = urlRecord(scheme);
        url.hostname = input;
        assert_equals(url.hostname, expected, "property");
        assert_equals(url.href, urlString({
          scheme,
          host: expected
        }), "href");
      }, `Setting hostname with ${type} ${cpReference} (${scheme}:)`);
    };
    for (var [type, expectedPart, input] of [["leading", (scheme === "https" ? cpString : encodeURIComponent(cpString)) + "test", String.fromCodePoint(i) + "test"], ["middle", "te" + (scheme === "https" ? cpString : encodeURIComponent(cpString)) + "st", "te" + String.fromCodePoint(i) + "st"], ["trailing", "test" + (scheme === "https" ? cpString : encodeURIComponent(cpString)), "test" + String.fromCodePoint(i)]]) {
      _loop4(expectedPart, input);
    }
    test(() => {
      var expected = stripped ? "9000" : "8000";
      var url = urlRecord(scheme);
      url.port = String.fromCodePoint(i) + "9000";
      assert_equals(url.port, expected, "property");
      assert_equals(url.href, urlString({
        scheme,
        port: expected
      }), "href");
    }, `Setting port with leading ${cpReference} (${scheme}:)`);
    test(() => {
      var expected = stripped ? "9000" : "90";
      var url = urlRecord(scheme);
      url.port = "90" + String.fromCodePoint(i) + "00";
      assert_equals(url.port, expected, "property");
      assert_equals(url.href, urlString({
        scheme,
        port: expected
      }), "href");
    }, `Setting port with middle ${cpReference} (${scheme}:)`);
    test(() => {
      var expected = "9000";
      var url = urlRecord(scheme);
      url.port = "9000" + String.fromCodePoint(i);
      assert_equals(url.port, expected, "property");
      assert_equals(url.href, urlString({
        scheme,
        port: expected
      }), "href");
    }, `Setting port with trailing ${cpReference} (${scheme}:)`);
    var _loop5 = function (_property, separator) {
      var _loop7 = function (_expectedPart, _input2) {
        test(() => {
          var expected = stripped ? "test" : _expectedPart;
          var url = urlRecord(scheme);
          url[_property] = _input2;
          assert_equals(url[_property], separator + expected, "property");
          assert_equals(url.href, urlString({
            scheme,
            [_property]: expected
          }), "href");
        }, `Setting ${_property} with ${_type2} ${cpReference} (${scheme}:)`);
      };
      for (var [_type2, _expectedPart, _input2] of [["leading", encodeURIComponent(cpString) + "test", String.fromCodePoint(i) + "test"], ["middle", "te" + encodeURIComponent(cpString) + "st", "te" + String.fromCodePoint(i) + "st"], ["trailing", "test" + encodeURIComponent(cpString), "test" + String.fromCodePoint(i)]]) {
        _loop7(_expectedPart, _input2);
      }
    };
    for (var [_property, separator] of [["pathname", "/"], ["search", "?"], ["hash", "#"]]) {
      _loop5(_property, separator);
    }
  };
  for (var i = 0; i < 0x20; i++) {
    if (_loop2(i)) continue;
  }
};
for (var scheme of ["https", "wpt++"]) {
  _loop(scheme);
}