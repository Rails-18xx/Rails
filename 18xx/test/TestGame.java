package test;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import rails.game.Game;
import rails.game.ReportBuffer;
import rails.util.Config;

import junit.framework.TestCase;

public class TestGame extends TestCase {

    private String gamePath;
    private String gameName;
    
    private List<String> testReport = null;
    private List<String> expectedReport = null;
    
    protected static Logger log =
        Logger.getLogger(TestGame.class.getPackage().getName());

    public TestGame(String gameName, String gamePath) {
        super(gameName);
        this.gameName = gameName;
        this.gamePath = gamePath;
        log.debug("Creates TestGame gameName = " + gameName 
                + ", gamePath = " + gamePath);
    }
    
    protected void runTest() throws Throwable {
        gameReportTest();
    }
    
    /**
     * a method that test that the game report is identical to the one created before
     */
    private void gameReportTest() {
        // compares the two reports line by line
        int line = 0;
        while (true) {
            // test for size of reports
            if (line >= expectedReport.size()) 
                if (line >= testReport.size())
                    break; // test succesfull
                else
                    fail("Test report exceeeds expected report." +
                    		" Last line (" + line + "): " + testReport.get(line-1));
            else
                if (line >= testReport.size())
                    fail("Expected report exceeds test report." +
                    		" Last line (" + line + "): " + expectedReport.get(line-1));
                    
            assertEquals("Reports differ in line " + line+1, 
                    expectedReport.get(line), testReport.get(line));
            line = line + 1;
        }
    }

    protected void setUp() throws Exception {
        super.setUp();

        // tries to load the game report 
        String reportFilename = gamePath + File.separator + gameName + "." + Config.get("report.filename.extension"); 
        
        File reportFile = new File(reportFilename);
        
        if (reportFile.exists()) {
            log.debug("Found reportfile at " + reportFilename);
                Scanner reportScanner = new Scanner(new FileReader(reportFilename));
                expectedReport = new ArrayList<String>();
                while (reportScanner.hasNext())
                    expectedReport.add(reportScanner.nextLine());
                reportScanner.close();
        } else {
            log.debug("Did not find reportfile at " + reportFilename);
        }
        
        // tries to load the game and run
        String gameFilename = gamePath + File.separator + gameName + "." + Config.get("save.filename.extension"); 
        
        File gameFile = new File(gameFilename);
        
        if (gameFile.exists()) {
            log.debug("Found gamefile at " + gameFilename);
            Game testGame = Game.load(gameFilename);
            if (testGame != null)
                testReport = ReportBuffer.getAsList();
        } else {
            log.error("Did not find gamefile at " + gameFilename);
        }
   }

    protected void tearDown() throws Exception {
        super.tearDown();
        testReport.clear();
        expectedReport.clear();
        
        
    }

}
