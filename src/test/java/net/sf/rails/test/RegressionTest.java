package net.sf.rails.test;

import net.sf.rails.util.GameLoader;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

public class RegressionTest {

    public static void main(String[] args) {
        // 1. FORCE CONSOLE LOGGING
        // This ensures we see WHY the loader fails
        configureConsoleLogging();

        System.out.println("!!! DIAGNOSTIC MODE ACTIVE (DEBUG LOGS ON) !!!");
        
        // 2. Set Environment
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("java.awt.headless", "true"); 

        if (args.length < 1) {
            System.err.println("Usage: java RegressionTest <path_to_rails_file>");
            System.exit(1);
        }

        File railsFile = new File(args[0]);
        System.out.println("Testing: " + railsFile.getName());

        try {
            // 3. Execute Loader
            GameLoader loader = new GameLoader();
            boolean success = loader.createFromFile(railsFile);

            if (!success) {
                System.out.println("FAILURE: Loader returned false.");
                System.exit(1);
            } else {
                System.out.println("SUCCESS");
                System.exit(0);
            }

        } catch (Throwable e) {
            System.out.println("CRITICAL EXCEPTION:");
            e.printStackTrace();
            System.exit(255);
        }
    }

    /**
     * Manually resets Logback and forces a ConsoleAppender at DEBUG level.
     */
    private static void configureConsoleLogging() {
        try {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            lc.reset(); // Drop all existing configurations

            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            ple.setContext(lc);
            ple.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            ple.start();

            ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
            consoleAppender.setContext(lc);
            consoleAppender.setEncoder(ple);
            consoleAppender.start();

            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(consoleAppender);
            rootLogger.setLevel(Level.DEBUG); // Force DEBUG level
            
            System.out.println(">> Logback configured for Console output.");
        } catch (Exception e) {
            System.err.println(">> Failed to configure Logback: " + e.getMessage());
        }
    }
}