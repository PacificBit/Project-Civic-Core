package com.pacificbytestudios.projectciviccore.utils;

import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class Logger {
  private static final StackWalker STACK_WALKER = StackWalker.getInstance();
  private static final String LOG_PATTERN = "[%s] %s";

  /**
   * Used to log errors.
   * Prints the caller's class and the method name
   * @param message message to print
   */
  public static void logSevere(String message) {
    var log = ProjectCivicCore.LOGGER.atSevere();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerName(), message);
  }

  /**
   * Used to log info at a finer level (more verbose).
   * Prints the caller's class and the method name
   * @param message message to print
   */
  public static void logFiner(String message) {
    var log = ProjectCivicCore.LOGGER.atFiner();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerName(), message);
  }

  /**
   * Used to log as much info as possible (extremely verbose).
   * Prints the caller's class and the method name
   * @param message message to print
   */
  public static void logFinest(String message) {
    var log = ProjectCivicCore.LOGGER.atFinest();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerName(), message);
  }

  /**
   * Used to log errors that are recoverable
   * @param message message to print
   */
  public static void logFine(String message) {
    var log = ProjectCivicCore.LOGGER.atFine();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerClassName(), message);
  }

  /**
   * Used to log generic info
   * @param message message to print
   */
  public static void logInfo(String message) {
    var log = ProjectCivicCore.LOGGER.atInfo();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerClassName(), message);
  }

  /**
   * Used to log a potential bug or problematic state of the program
   * @param message message to print
   */
  public static void logWarning(String message) {
    var log = ProjectCivicCore.LOGGER.atWarning();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerClassName(), message);
  }

  /**
   * Used to log config info
   * @param message message to print
   */
  public static void logConfig(String message) {
    var log = ProjectCivicCore.LOGGER.atConfig();
    if (!log.isEnabled()) {
      return;
    }

    log.log(LOG_PATTERN, callerClassName(), message);
  }

  private static String callerName() {
    StackWalker.StackFrame frame = callerFrame();
    if (frame == null) {
      return "Unknown - unknown";
    }

    return "%s - %s".formatted(simpleClassName(frame.getClassName()), frame.getMethodName());
  }

  private static String callerClassName() {
    StackWalker.StackFrame frame = callerFrame();
    return frame == null ? "Unknown" : simpleClassName(frame.getClassName());
  }

  private static StackWalker.StackFrame callerFrame() {
    return STACK_WALKER.walk(frames -> frames
        .filter(frame -> !frame.getClassName().equals(Logger.class.getName()))
        .findFirst()
        .orElse(null));
  }

  private static String simpleClassName(String className) {
    int packageSeparator = className.lastIndexOf('.');
    return packageSeparator >= 0 ? className.substring(packageSeparator + 1) : className;
  }
}
