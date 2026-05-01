package rocks.gravili.notquests.paper.managers;

import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FlagParser {
    // Flag values must be allowed to contain underscores, hyphens, and dots
    // because quest/category/profile identifiers commonly contain them.
    // The previous pattern truncated "--quest Trader_Tier1_Wheat" to "Trader",
    // which caused OpenGuiAction to receive a null Quest and broke %QUESTNAME%
    // substitution on the quest-preview GUI.
    private static final String FLAG_PATTERN = "--[a-zA-Z]+ [\\w%.\\-]+";

    // Flag names are canonicalised to lowercase so that callers don't have to
    // worry about whether the action string was written `--targetPlayer` or
    // `--targetplayer`. Use Locale.ROOT to avoid Turkish-i edge cases.
    public static Map<String, String> parseFlags(String stringToParse) {
        return Pattern.compile(FLAG_PATTERN)
                .matcher(stringToParse)
                .results()
                .map(MatchResult::group)
                .toList()
                .stream()
                .collect(
                        Collectors.toMap(flag -> flag.split(" ")[0].substring(2).toLowerCase(Locale.ROOT),
                        flag -> flag.split(" ")[1])
                );
    }
}
