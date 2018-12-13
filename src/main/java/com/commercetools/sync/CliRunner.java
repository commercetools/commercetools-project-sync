package com.commercetools.sync;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliRunner {
  static final String SYNC_MODULE_OPTION_SHORT = "s";
  static final String HELP_OPTION_SHORT = "h";
  static final String VERSION_OPTION_SHORT = "v";

  static final String SYNC_MODULE_OPTION_LONG = "sync";
  static final String HELP_OPTION_LONG = "help";
  static final String VERSION_OPTION_LONG = "version";

  static final String SYNC_MODULE_OPTION_TYPE_SYNC = "types";
  static final String SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC = "productTypes";
  static final String SYNC_MODULE_OPTION_CATEGORY_SYNC = "categories";
  static final String SYNC_MODULE_OPTION_PRODUCT_SYNC = "products";
  static final String SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC = "inventoryEntries";

  static final String SYNC_MODULE_OPTION_DESCRIPTION =
      format(
          "Choose which sync module to run: \"%s\", \"%s\", \"%s\", \"%s\" or \"%s\".",
          SYNC_MODULE_OPTION_TYPE_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC,
          SYNC_MODULE_OPTION_CATEGORY_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_SYNC,
          SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC);
  static final String HELP_OPTION_DESCRIPTION = "Print help information to System.out.";
  static final String VERSION_OPTION_DESCRIPTION = "Print the version of the application.";

  static final String APPLICATION_DEFAULT_NAME = "COMMERCETOOLS-SYNC";
  static final String APPLICATION_DEFAULT_VERSION = "1.0-dev";

  private static final Logger LOGGER = LoggerFactory.getLogger(CliRunner.class);

  private Options options;

  public CliRunner() {
    options = buildCliOptions();
  }

  static Options buildCliOptions() {
    final Options options = new Options();

    final Option syncOption =
        Option.builder(SYNC_MODULE_OPTION_SHORT)
            .longOpt(SYNC_MODULE_OPTION_LONG)
            .desc(SYNC_MODULE_OPTION_DESCRIPTION)
            .hasArg()
            .build();

    final Option helpOption =
        Option.builder(HELP_OPTION_SHORT)
            .longOpt(HELP_OPTION_LONG)
            .desc(HELP_OPTION_DESCRIPTION)
            .build();

    final Option versionOption =
        Option.builder(VERSION_OPTION_SHORT)
            .longOpt(VERSION_OPTION_LONG)
            .desc(VERSION_OPTION_DESCRIPTION)
            .build();
    options.addOption(syncOption);
    options.addOption(helpOption);
    options.addOption(versionOption);
    return options;
  }

  void run(@Nonnull final String[] arguments) {
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine commandLine = parser.parse(getOptions(), arguments);
      processCliArguments(commandLine);
    } catch (final ParseException | IllegalArgumentException exception) {
      handleIllegalArgumentException(format("Parse error:%n%s", exception.getMessage()));
    }
  }

  private void processCliArguments(@Nonnull final CommandLine commandLine) {
    final Option[] options = commandLine.getOptions();
    if (options.length == 0) {
      handleIllegalArgumentException("Please pass at least 1 option to the CLI.");
    } else {
      final Option option = options[0];
      final String optionName = option.getOpt();
      switch (optionName) {
        case SYNC_MODULE_OPTION_SHORT:
          processSyncOption(commandLine).toCompletableFuture().join();
          break;
        case HELP_OPTION_SHORT:
          printHelpToStdOut();
          break;
        case VERSION_OPTION_SHORT:
          logApplicationVersion();
          break;
        default:
          // Unreachable code since this case is already handled by parser.parse(options,
          // arguments);
          // in the constructor.
          throw new IllegalStateException(format("Unrecognized option: -%s", optionName));
      }
    }
  }

  private void handleIllegalArgumentException(@Nonnull final String errorMessage) {
    LOGGER.error(errorMessage);
    printHelpToStdOut();
  }

  CompletionStage processSyncOption(@Nonnull final CommandLine commandLine) {
    final String syncOptionValue = commandLine.getOptionValue(SYNC_MODULE_OPTION_SHORT);
    return SyncerFactory.getSyncer(syncOptionValue).sync();
  }

  private void printHelpToStdOut() {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(getApplicationName(), getOptions());
  }

  private static String getApplicationName() {
    final String implementationTitle = Main.class.getPackage().getImplementationTitle();
    return isBlank(implementationTitle) ? APPLICATION_DEFAULT_NAME : implementationTitle;
  }

  private static void logApplicationVersion() {
    final String implementationVersion = getApplicationVersion();
    LOGGER.info(implementationVersion);
  }

  private static String getApplicationVersion() {
    final String implementationVersion = Main.class.getPackage().getImplementationVersion();
    return isBlank(implementationVersion) ? APPLICATION_DEFAULT_VERSION : implementationVersion;
  }

  Options getOptions() {
    return options;
  }
}
