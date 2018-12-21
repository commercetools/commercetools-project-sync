package com.commercetools.project.sync;

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

final class CliRunner {
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

  static final String APPLICATION_DEFAULT_NAME = "commercetools-project-sync";
  static final String APPLICATION_DEFAULT_VERSION = "1.0.0";

  private static final Logger LOGGER = LoggerFactory.getLogger(CliRunner.class);

  public static CliRunner of() {
    return new CliRunner();
  }

  void run(@Nonnull final String[] arguments, @Nonnull final SyncerFactory syncerFactory) {

    final Options cliOptions = buildCliOptions();
    final CommandLineParser parser = new DefaultParser();

    try {
      final CommandLine commandLine = parser.parse(cliOptions, arguments);
      processCliArguments(commandLine, cliOptions, syncerFactory);
    } catch (final ParseException | IllegalArgumentException exception) {
      handleIllegalArgumentException(
          format("Parse error:%n%s", exception.getMessage()), cliOptions);
    }
  }

  private static Options buildCliOptions() {
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

  private static void processCliArguments(
      @Nonnull final CommandLine commandLine,
      @Nonnull final Options cliOptions,
      @Nonnull final SyncerFactory syncerFactory) {

    final Option[] options = commandLine.getOptions();
    if (options.length == 0) {
      handleIllegalArgumentException("Please pass at least 1 option to the CLI.", cliOptions);
    } else {
      final Option option = options[0];
      final String optionName = option.getOpt();
      switch (optionName) {
        case SYNC_MODULE_OPTION_SHORT:
          processSyncOptionAndExecute(commandLine, syncerFactory).toCompletableFuture().join();
          break;
        case HELP_OPTION_SHORT:
          printHelpToStdOut(cliOptions);
          break;
        case VERSION_OPTION_SHORT:
          logApplicationVersion();
          break;
        default:
          // Unreachable code since this case is already handled by parser.parse(options,
          // arguments);
          // in the CliRunner#run method.
          throw new IllegalStateException(format("Unrecognized option: -%s", optionName));
      }
    }
  }

  private static void handleIllegalArgumentException(
      @Nonnull final String errorMessage, @Nonnull final Options cliOptions) {
    LOGGER.error(errorMessage);
    printHelpToStdOut(cliOptions);
  }

  @Nonnull
  private static CompletionStage processSyncOptionAndExecute(
      @Nonnull final CommandLine commandLine, @Nonnull final SyncerFactory syncerFactory) {

    final String syncOptionValue = commandLine.getOptionValue(SYNC_MODULE_OPTION_SHORT);
    return syncerFactory.buildSyncer(syncOptionValue).sync();
  }

  private static void printHelpToStdOut(@Nonnull final Options cliOptions) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(getApplicationName(), cliOptions);
  }

  @Nonnull
  private static String getApplicationName() {
    final String implementationTitle =
        SyncerApplication.class.getPackage().getImplementationTitle();
    return isBlank(implementationTitle) ? APPLICATION_DEFAULT_NAME : implementationTitle;
  }

  private static void logApplicationVersion() {
    final String implementationVersion = getApplicationVersion();
    LOGGER.info(implementationVersion);
  }

  @Nonnull
  private static String getApplicationVersion() {
    final String implementationVersion =
        SyncerApplication.class.getPackage().getImplementationVersion();
    return isBlank(implementationVersion) ? APPLICATION_DEFAULT_VERSION : implementationVersion;
  }

  private CliRunner() {}
}
