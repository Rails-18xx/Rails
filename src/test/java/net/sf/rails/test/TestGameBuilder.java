package net.sf.rails.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.GameLoader;

import net.sf.rails.util.XmlGameLoader;
import org.junit.runners.AllTests;
import org.junit.runner.RunWith;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;



@RunWith(AllTests.class)
public final class TestGameBuilder extends TestCase {

    static void saveGameReport(List<String> report, String reportFilename, boolean failed) {
        PrintWriter reportFile = null;
        try{
            reportFile = new PrintWriter(reportFilename);
        } catch (IOException e)
            {
            System.err.print("Error: cannot open file " + reportFilename + " for report writing");
            }
        if (reportFile != null) {
            for (String msg:report){
                reportFile.println(msg);
            }
            reportFile.close();
            if (failed) {
                System.out.println("Created failed report at " + reportFilename);
            } else {
                System.out.println("Created base line report file at " + reportFilename);
            }
        }
    }


    private static void prepareGameReport(File gameFile, String reportFilename) {

        RailsRoot root = null;
        if (gameFile.exists()) {
            System.out.println("Found game at " + gameFile.getAbsolutePath());
            XmlGameLoader gameLoader = new XmlGameLoader();
            if (gameLoader.createFromFile(gameFile)) {
                root = gameLoader.getRoot();
            }
        }
        if (root != null) {
            List<String> report = root.getReportManager().getReportBuffer().getAsList();
            saveGameReport(report, reportFilename, false);
        }
    }


    // returns gameName if prepararion was successfull
    private static String prepareTestGame(File gameFile, boolean overrideReport){

        // check preconditions
        if (!gameFile.exists() || !gameFile.isFile()) return null;

        // check if it is a Rails savefile
        String fileName = gameFile.getName();
        char extensionSeparator = '.';
        int dot = fileName.lastIndexOf(extensionSeparator);

        String gameName = null;
        if (dot != -1 &&  fileName.substring(dot+1).equals(
                Config.get("save.filename.extension"))) {
            gameName = fileName.substring(0,dot);
            String gamePath = gameFile.getParent();

            // check if there is a reportfile
            String reportFilename = gamePath + File.separator + gameName
                    + "." + Config.get("report.filename.extension");
            File reportFile = new File(reportFilename);

            if (!reportFile.exists() || overrideReport) {
                prepareGameReport(gameFile, reportFilename);
            }
        }

        return gameName;
    }

    private static TestSuite recursiveTestSuite(String rootPath, String dirPath, int level, boolean overrideReport){

        // completeDirPath
        String combinedDirPath = rootPath + File.separator + dirPath;

        // assign directory
        File directory = new File(combinedDirPath);

        // check if directory exists otherwise return null
        if (!directory.exists() || !directory.isDirectory()) return null;

        // create new testsuite
        TestSuite suite;

        if (level == 0)
            suite = new TestSuite("Rails Tests");
        else
            suite = new TestSuite(directory.getName());

        // use filelist to sort
        List<String> filenameList = Arrays.asList(directory.list());
        Collections.sort(filenameList);

        // add deeper directories
        for (String fn:filenameList) {
            File f = new File(combinedDirPath + File.separator + fn);
            String nextDirPath;
            if (dirPath.equals(""))
                nextDirPath = f.getName();
            else
                nextDirPath = dirPath + File.separator + f.getName();
            int maxRecursionLevel = 5;
            if (f.isDirectory() && level <= maxRecursionLevel ) {
                TestSuite newSuite = recursiveTestSuite(rootPath, nextDirPath, level+1, overrideReport);
                if (newSuite != null) suite.addTest(newSuite);
            }
        }

        // add files of directory
        for (String fn:filenameList) {
            File f = new File(combinedDirPath + File.separator + fn);
            String gameName = prepareTestGame(f, overrideReport);
            if (gameName != null) {
                String extendedGameName;
                // true = optimal for ant html reports, false = optimal for test runner
                boolean extendedTestNames = true;
                if ( extendedTestNames )
                    extendedGameName = dirPath + File.separator + gameName;
                else
                    extendedGameName = gameName;
                suite.addTest(new TestGame(extendedGameName, rootPath));
                System.out.println("Added TestGame "+ extendedGameName);
            }
        }

        return suite;
    }

    /**
     * Builds test suite of all test games below the main test directory
     * @return created test suite for junit
     */

    public static Test suite() {

        ConfigManager.initConfiguration(true);

        // Main test directory
        File testDir = new File(Config.get("save.directory"));

        // Create tests
        TestSuite suite = null;
        if (testDir.exists() && testDir.isDirectory()) {
            System.out.println("Test directory = " + testDir.getAbsolutePath());
            suite = recursiveTestSuite(testDir.getAbsolutePath(), "",  0, false);
        }

        return suite;
    }


    /**
     * Run main to rebuild the report files.
     * Only use this if you know what you are doing
     *
     * @param args a list of directories below the main test directory
     */
    public static void main(String[] args) {

        ConfigManager.initConfiguration(true);

        // Main test directory
        String rootPath = Config.get("save.directory");

        if (args != null && args.length > 0) {
            // commandline argument: only directories are possible
            System.out.println("Number of args: "+ args.length);
            for (String arg : args)
                // discard testsuite, only override the report files
                recursiveTestSuite(rootPath, arg, 0, true);
        } else {
            // ask for directories to ovrerride
            JPanel panel = new JPanel();
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select directories and/or files to reset game reports");
            chooser.setCurrentDirectory(new File(rootPath));
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setMultiSelectionEnabled(true);
// Java 6:  chooser.setFileFilter(new FileNameExtensionFilter("Rails save files (*.rails)", "rails"));
            chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith("." + Config.get("save.filename.extension"));
                  }
                public String getDescription()  {
                    return "Rails save files (*."+ Config.get("save.filename.extension") + ")" ;
                }
            });
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.showDialog(panel, "Select");
            File[] files = chooser.getSelectedFiles();
            for (File f : files)
                if (f.isDirectory())
                    // discard testsuite, only override the report files
                    recursiveTestSuite(f.getAbsolutePath(), "", 0, true);
                else if (f.isFile())
                    prepareTestGame(f, true);
        }
    }
}
