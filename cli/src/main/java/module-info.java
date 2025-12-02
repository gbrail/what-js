module org.brail.jwhat.cli {
  requires org.mozilla.rhino;
  requires org.jline;
  requires org.brail.jwhat.core;

  exports org.brail.jwhat.cli;

  opens org.brail.jwhat.cli;
}
