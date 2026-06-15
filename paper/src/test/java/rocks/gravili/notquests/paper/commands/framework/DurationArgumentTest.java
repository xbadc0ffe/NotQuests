package rocks.gravili.notquests.paper.commands.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationArgumentTest {
    @Test
    void parsesMillisecondsWithShortAndLongUnits() throws Exception {
        final NQArgumentType<Duration> argument = NQArguments.durationArgument();

        assertEquals(Duration.ofMillis(500), argument.convert("500"));
        assertEquals(Duration.ofMillis(500), argument.convert("500ms"));
        assertEquals(Duration.ofMillis(500), argument.convert("500milliseconds"));
        assertEquals(Duration.ofMillis(500), argument.convert("500millisecond"));
        assertEquals(Duration.ofMillis(500), argument.convert("500millis"));
        assertEquals(Duration.ofMillis(500), argument.convert("500msec"));
        assertEquals(Duration.ofMillis(500), argument.convert("500miliseconds"));
    }

    @Test
    void parsesLongUnitNamesCaseInsensitively() throws Exception {
        final NQArgumentType<Duration> argument = NQArguments.durationArgument();

        assertEquals(Duration.ofSeconds(30), argument.convert("30Seconds"));
        assertEquals(Duration.ofMinutes(5), argument.convert("5MINUTES"));
        assertEquals(Duration.ofHours(2), argument.convert("2hours"));
        assertEquals(Duration.ofDays(1), argument.convert("1Day"));
    }
}
