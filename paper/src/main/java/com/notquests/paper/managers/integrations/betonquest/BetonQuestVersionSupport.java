package com.notquests.paper.managers.integrations.betonquest;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Version gate kept free of BetonQuest API imports so 1.x/2.x jars can be rejected safely. */
public final class BetonQuestVersionSupport {
  private static final Pattern LEADING_VERSION =
      Pattern.compile("^[^0-9]*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

  private BetonQuestVersionSupport() {}

  public static boolean isSupported(final String version) {
    final Optional<int[]> parsed = parse(version);
    if (parsed.isEmpty()) {
      return false;
    }
    final int[] parts = parsed.get();
    if (parts[0] != 3) {
      return parts[0] > 3;
    }
    if (parts[1] != 0) {
      return parts[1] > 0;
    }
    return parts[2] >= 0;
  }

  static Optional<int[]> parse(final String version) {
    if (version == null || version.isBlank()) {
      return Optional.empty();
    }
    final Matcher matcher = LEADING_VERSION.matcher(version.trim());
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(new int[] {
      Integer.parseInt(matcher.group(1)),
      matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
      matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
    });
  }
}
