package org.brail.jwhat.url;

public class URLFormatException extends Exception {
  public URLFormatException(String msg) {
    super(msg);
  }

  public URLFormatException(String msg, Throwable t) {
    super(msg, t);
  }
}
