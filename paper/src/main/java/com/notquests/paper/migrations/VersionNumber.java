package com.notquests.paper.migrations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record VersionNumber(int major, int minor, int patch) implements Comparable<VersionNumber> {
  private static final Pattern LEADING_VERSION = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

  static VersionNumber parse(final String rawVersion) {
    if (rawVersion == null || rawVersion.isBlank()) {
      return new VersionNumber(0, 0, 0);
    }
    final Matcher matcher = LEADING_VERSION.matcher(rawVersion.trim());
    if (!matcher.matches()) {
      return new VersionNumber(0, 0, 0);
    }
    return new VersionNumber(
        parsePart(matcher.group(1)),
        parsePart(matcher.group(2)),
        parsePart(matcher.group(3)));
  }

  boolean isBefore(final String otherVersion) {
    return compareTo(parse(otherVersion)) < 0;
  }

  @Override
  public int compareTo(final VersionNumber other) {
    int result = Integer.compare(major, other.major);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(minor, other.minor);
    if (result != 0) {
      return result;
    }
    return Integer.compare(patch, other.patch);
  }

  private static int parsePart(final String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException ignored) {
      return 0;
    }
  }
}
