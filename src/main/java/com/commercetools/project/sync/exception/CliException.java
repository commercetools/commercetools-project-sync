package com.commercetools.project.sync.exception;

/** Custom Exception class to return IllegalArgumentException without Stacktrace. */
public class CliException extends IllegalArgumentException {

  public CliException(String message) {
    super(message);
  }

  // Overriding this method from RuntimeException class to exclude writing stacktrace into the
  // Exception.
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
