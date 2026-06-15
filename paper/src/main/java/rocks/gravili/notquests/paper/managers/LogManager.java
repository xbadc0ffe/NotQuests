package rocks.gravili.notquests.paper.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import rocks.gravili.notquests.common.managers.LogCategory;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.logging.Level;

public class LogManager {
  private final NotQuests main;
  private Component prefix;
  private final Component prefixDownsampled, prefixNoColor;
  private final ArrayList<String> severeLogs, warnLogs;
  private final ConsoleCommandSender consoleSender;

  public LogManager(final NotQuests main) {
    this.main = main;
    severeLogs = new ArrayList<>();
    warnLogs = new ArrayList<>();

    consoleSender = Bukkit.getConsoleSender();

    prefix = main.parse("<#393e46>[<gradient:#E0EAFC:#CFDEF3>NotQuests<#393e46>]<#636c73>: ");
    prefixDownsampled = main.parse("<gray>[<aqua>NotQuests</aqua>]</gray><dark_gray>: ");
    prefixNoColor = Component.text("[NotQuests]: ");
  }

  public void lateInit() {
    prefix =
        main.parse(
            (main.getConfiguration() != null ? main.getConfiguration().getColorsConsolePrefixPrefix() : "")
            + "NotQuests"
            + (main.getConfiguration() != null ? main.getConfiguration().getColorsConsolePrefixSuffix() : "")
        );
  }

  public final ArrayList<String> getErrorLogs() {
    return severeLogs;
  }

  public final ArrayList<String> getWarnLogs() {
    return warnLogs;
  }

  private void log(final Level level, final String color, final String message, final Object... interpolatedStrings) {
    log(level, LogCategory.DEFAULT, color, message, interpolatedStrings);
  }

  private void log(
      final Level level, final LogCategory logCategory, final String color, String message, final Object... interpolatedStrings) {

    if(interpolatedStrings.length > 0){
      message = message.formatted((Object[]) interpolatedStrings);
    }

    if (main.getConfiguration() == null || main.getConfiguration().isConsoleColorsEnabled()) {
      if (main.getConfiguration() != null && !main.getConfiguration().isConsoleColorsDownsampleColors()) {
        consoleSender.sendMessage( prefix.append(main.parse(color + message)));
      } else {
        final Component component = main.parse(message);
        consoleSender.sendMessage(
            prefixDownsampled.append(main.parse(color)).append(
            GsonComponentSerializer.gson()
                .deserializeFromTree( // Convert back to component
                    GsonComponentSerializer.builder()
                        .downsampleColors()
                        .build()
                        .serializeToTree( // Convert to text
                            component))));
      }
    } else {
      consoleSender.sendMessage(
          prefixNoColor.append(
          main.parse(
              main.getMiniMessage()
                  .stripTags(message, main.getMessageManager().getTagResolver()))));
    }

    if(level == Level.SEVERE){
      severeLogs.add(message);
    } else if(level == Level.WARNING){
      warnLogs.add(message);
    }
  }

  public void info(final LogCategory logCategory, final String message, final Object... interpolatedStrings) {
    if (logCategory == LogCategory.DEFAULT) {
      log(Level.INFO, logCategory, main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleInfoDefaultDownsampled() : main.getConfiguration().getColorsConsoleInfoDefault(), message, interpolatedStrings);
    } else if (logCategory == LogCategory.DATA) {
      log(Level.INFO, logCategory, main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleInfoDataDownsampled() : main.getConfiguration().getColorsConsoleInfoData(), message, interpolatedStrings);
    } else if (logCategory == LogCategory.LANGUAGE) {
      log(Level.INFO, logCategory, main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleInfoLanguageDownsampled() : main.getConfiguration().getColorsConsoleInfoLanguage(), message, interpolatedStrings);
    }
  }

  public void info(final String message, final Object... interpolatedStrings) {
    info(LogCategory.DEFAULT, message, interpolatedStrings);
  }

  public void warn(final LogCategory logCategory, final String message, final Object... interpolatedStrings) {
    log(Level.WARNING, logCategory, main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleWarnDefaultDownsampled() : main.getConfiguration().getColorsConsoleWarnDefault(), message, interpolatedStrings);
  }

  public void warn(final String message, final Object... interpolatedStrings) {
    warn(LogCategory.DEFAULT, message, interpolatedStrings);
  }

  public void severe(final LogCategory logCategory, final String message, final Object... interpolatedStrings) {
    log(
        Level.SEVERE,
        logCategory,
        main.getConfiguration() != null ? (main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleSevereDefaultDownsampled() : main.getConfiguration().getColorsConsoleSevereDefault()) : "",
        message, interpolatedStrings);
  }

  public void severe(final String message, final Object... interpolatedStrings) {
    severe(LogCategory.DEFAULT, message, interpolatedStrings);
  }

  public void debug(final LogCategory logCategory, final String message, final Object... interpolatedStrings) {
    if (main.getConfiguration().debug) {
      log(Level.FINE, logCategory, main.getConfiguration().isConsoleColorsDownsampleColors() ? main.getConfiguration().getColorsConsoleDebugDownsampled() : main.getConfiguration().getColorsConsoleDebugDefault(), message, interpolatedStrings);
    }
  }

  public void debug(final String message, final Object... interpolatedStrings) {
    debug(LogCategory.DEFAULT, message, interpolatedStrings);
  }
}
