package org.brail.jwhat.stream;

public class BYOBReader extends AbstractReader {
  @Override
  public String getClassName() {
    return "ReadableStreamBYOBReader";
  }

  int getNumReadIntoRequests() {
    throw new AssertionError("Not implemented");
  }
}

