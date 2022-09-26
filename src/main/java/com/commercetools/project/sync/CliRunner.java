package com.commercetools.project.sync;

import static com.commercetools.project.sync.model.ProductSyncCustomRequest.parseProductQueryParametersOption;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationVersion;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;

import com.commercetools.project.sync.exception.CliException;
import com.commercetools.project.sync.model.ProductSyncCustomRequest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CliRunner {
  static final String SYNC_MODULE_OPTION_SHORT = "s";
  static final String RUNNER_NAME_OPTION_SHORT = "r";
  static final String FULL_SYNC_OPTION_SHORT = "f";
  static final String HELP_OPTION_SHORT = "h";
  static final String VERSION_OPTION_SHORT = "v";
  static final String PRODUCT_QUERY_PARAMETERS_OPTION = "productQueryParameters";

  static final String SYNC_MODULE_OPTION_LONG = "sync";
  static final String RUNNER_NAME_OPTION_LONG = "runnerName";
  static final String FULL_SYNC_OPTION_LONG = "full";
  static final String HELP_OPTION_LONG = "help";
  static final String VERSION_OPTION_LONG = "version";
  static final String SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG = "syncProjectSyncCustomObjects";

  static final String SYNC_MODULE_OPTION_ALL = "all";

  static final String SYNC_MODULE_OPTION_DESCRIPTION =
      format(
          "Choose one or more of the following modules to run: \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\" or \"%s\".",
          (Object[])
              ArrayUtils.add(SyncModuleOption.getSyncOptionValues(), SYNC_MODULE_OPTION_ALL));
  static final String RUNNER_NAME_OPTION_DESCRIPTION =
      "Choose a name for the running sync instance. Please make sure the name is unique, otherwise running more than 1 sync "
          + "instance with the same name would lead to an unexpected behaviour. This option must be added after `-s` option."
          + "(optional parameter) default: 'runnerName'.";
  static final String FULL_SYNC_OPTION_DESCRIPTION =
      "By default, a delta sync runs using last-sync-timestamp logic. Use this flag to run a full sync. i.e. sync the "
          + "entire data set. This option must be added after `-s` option.";
  static final String HELP_OPTION_DESCRIPTION = "Print help information.";
  static final String VERSION_OPTION_DESCRIPTION = "Print the version of the application.";
  static final String SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_DESCRIPTION =
      "Sync custom objects that were created with project sync (this application). "
          + "This option must be added after `-s` option.";
  static final String PRODUCT_QUERY_PARAMETERS_OPTION_DESCRIPTION =
      "Pass your customized product fetch limit and a product projection predicate to filter product resources to sync in the JSON format. "
          + "Example: {\"limit\": 100, \"where\": \"published=true\"} could be used to fetch only published "
          + "products to sync and limit max 100 elements in one page. This option must be added after `-s` option.";

  private static final Logger LOGGER = LoggerFactory.getLogger(CliRunner.class);

  @Nonnull
  public static CliRunner of() {
    return new CliRunner();
  }

  void run(@Nonnull final String[] arguments, @Nonnull final SyncerFactory syncerFactory) {

    final Options cliOptions = buildCliOptions();

    parseAndProcess(arguments, syncerFactory, cliOptions, new DefaultParser())
        .exceptionally(
            exception -> {
              LOGGER.error("Failed to run sync process.", exception);
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Nonnull
  private CompletionStage<Void> parseAndProcess(
      @Nonnull final String[] arguments,
      @Nonnull final SyncerFactory syncerFactory,
      @Nonnull final Options cliOptions,
      @Nonnull final CommandLineParser parser) {
    CommandLine commandLine;
    try {
      commandLine = parser.parse(cliOptions, arguments);
    } catch (final ParseException | IllegalArgumentException exception) {
      return exceptionallyCompletedFuture(new CliException(exception.getMessage()));
    }

    return processCliArguments(commandLine, cliOptions, syncerFactory);
  }

  @Nonnull
  private static Options buildCliOptions() {
    final Options options = new Options();

    final Option syncOption =
        Option.builder(SYNC_MODULE_OPTION_SHORT)
            .longOpt(SYNC_MODULE_OPTION_LONG)
            .desc(SYNC_MODULE_OPTION_DESCRIPTION)
            .hasArg()
            .build();
    syncOption.setArgs(Option.UNLIMITED_VALUES);

    final Option runnerOption =
        Option.builder(RUNNER_NAME_OPTION_SHORT)
            .longOpt(RUNNER_NAME_OPTION_LONG)
            .desc(RUNNER_NAME_OPTION_DESCRIPTION)
            .hasArg()
            .build();

    final Option fullSyncOption =
        Option.builder(FULL_SYNC_OPTION_SHORT)
            .longOpt(FULL_SYNC_OPTION_LONG)
            .desc(FULL_SYNC_OPTION_DESCRIPTION)
            .build();

    final Option productQueryParametersOption =
        Option.builder()
            .longOpt(PRODUCT_QUERY_PARAMETERS_OPTION)
            .desc(PRODUCT_QUERY_PARAMETERS_OPTION_DESCRIPTION)
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

    final Option syncProjectSyncCustomObjectsOption =
        Option.builder()
            .longOpt(SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG)
            .desc(SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_DESCRIPTION)
            .build();

    options.addOption(syncOption);
    options.addOption(fullSyncOption);
    options.addOption(runnerOption);
    options.addOption(helpOption);
    options.addOption(versionOption);
    options.addOption(syncProjectSyncCustomObjectsOption);
    options.addOption(productQueryParametersOption);

    return options;
  }

  private static CompletionStage<Void> processCliArguments(
      @Nonnull final CommandLine commandLine,
      @Nonnull final Options cliOptions,
      @Nonnull final SyncerFactory syncerFactory) {

    final Option[] options = commandLine.getOptions();

    if (options.length == 0) {

      return exceptionallyCompletedFuture(
          new CliException("Please pass at least 1 option to the CLI."));

    } else {
      final Option option =
          Arrays.stream(options)
              .filter(o -> !SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG.equals(o.getLongOpt()))
              .findAny()
              .orElse(null);
      if (option == null) {
        return exceptionallyCompletedFuture(
            new CliException(
                format(
                    "Please pass at least 1 more option other than %s to the CLI.",
                    SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG)));
      }
      final String optionName = option.getOpt();
      CompletionStage<Void> resultCompletionStage = CompletableFuture.completedFuture(null);
      switch (optionName) {
        case SYNC_MODULE_OPTION_SHORT:
          resultCompletionStage = processSyncOptionAndExecute(commandLine, syncerFactory);
          break;
        case HELP_OPTION_SHORT:
          printHelpToStdOut(cliOptions);
          break;
        case VERSION_OPTION_SHORT:
          printApplicationVersion();
          break;
        default:
          resultCompletionStage =
              exceptionallyCompletedFuture(
                  new CliException(
                      format(
                          "Please check that the first sync option is either -%s, -%s or -%s.",
                          SYNC_MODULE_OPTION_SHORT, HELP_OPTION_SHORT, VERSION_OPTION_SHORT)));
          break;
      }

      return resultCompletionStage;
    }
  }

  @Nonnull
  private static CompletionStage<Void> processSyncOptionAndExecute(
      @Nonnull final CommandLine commandLine, @Nonnull final SyncerFactory syncerFactory) {

    final String[] syncOptionValues = commandLine.getOptionValues(SYNC_MODULE_OPTION_SHORT);
    final String runnerNameValue = commandLine.getOptionValue(RUNNER_NAME_OPTION_SHORT);
    final boolean isFullSync = commandLine.hasOption(FULL_SYNC_OPTION_SHORT);
    final boolean isSyncProjectSyncCustomObjects =
        commandLine.hasOption(SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG);
    final boolean isProductQueryParametersOptionPresent =
        commandLine.hasOption(PRODUCT_QUERY_PARAMETERS_OPTION);

    final ProductSyncCustomRequest productSyncCustomRequest;
    try {
      productSyncCustomRequest =
          isProductQueryParametersOptionPresent
              ? parseProductQueryParametersOption(
                  commandLine.getOptionValue(PRODUCT_QUERY_PARAMETERS_OPTION))
              : null;

    } catch (CliException e) {
      return exceptionallyCompletedFuture(e);
    }
    return syncerFactory.sync(
        syncOptionValues,
        runnerNameValue,
        isFullSync,
        isSyncProjectSyncCustomObjects,
        productSyncCustomRequest);
  }

  private static void printHelpToStdOut(@Nonnull final Options cliOptions) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(getApplicationName(), cliOptions);
  }

  private static void printApplicationVersion() {
    final String implementationVersion = getApplicationVersion();
    System.out.println(implementationVersion); // NOPMD
  }

  private CliRunner() {}
}
