module org.brail.jwhat.core {
  requires transitive org.mozilla.rhino;

  exports org.brail.jwhat.console;
  exports org.brail.jwhat.url;
  exports org.brail.jwhat.stream;

  opens org.brail.jwhat.stream;

  exports org.brail.jwhat.core.impl to
      org.brail.jwhat.core.tests;
}
