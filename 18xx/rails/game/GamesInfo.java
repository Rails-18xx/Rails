/**
 * This is a singleton class that encapsulates the static game info
 * contained in data/Games.xml.
 * This information is available through a number of getters.
 */
package rails.game;

import java.util.*;

import rails.util.Tag;
import rails.util.Util;

/**
 * @author Erik Vos
 *
 */
public class GamesInfo {

    private static final String GAMES_XML = "GamesList.xml";
    private static final String DATA_DIR = "data";
    private static final String GAMES_TAG = "GamesList";
    private static final String GAME_TAG = "Game";
    private static final String NOTE_TAG = "Note";
    private static final String DESCR_TAG = "Description";
    private static final String OPTION_TAG = "Option";
    private static final String PLAYERS_TAG = "Players";
    private static final String CREDITS_TAG = "Credits";

    private static final String GAME_NAME_ATTR = "name";
    private static final String MIN_ATTR = "minimum";
    private static final String MAX_ATTR = "maximum";
    private static final String OPTION_NAME_ATTR = "name";
    private static final String OPTION_TYPE_ATTR = "type";
    private static final String OPTION_DEFAULT_ATTR = "default";
    private static final String OPTION_VALUES_ATTR = "values";

    private List<String> gameNames = new ArrayList<String>();
    private Map<String, String> notes = new HashMap<String, String>();
    private Map<String, String> descriptions = new HashMap<String, String>();
    private Map<String, Integer> minPlayers = new HashMap<String, Integer>();
    private Map<String, Integer> maxPlayers = new HashMap<String, Integer>();
    private Map<String, List<GameOption>> options =
            new HashMap<String, List<GameOption>>();

    private String credits;

    private static GamesInfo instance = new GamesInfo();

    private GamesInfo() {

        readXML();
    }

    private void readXML() {

        String myGamesXmlFile = System.getProperty("gamesxmlfile");

        /* If not, use the default configuration file name */
        if (!Util.hasValue(myGamesXmlFile)) {
            myGamesXmlFile = GAMES_XML;
        }
        System.out.println("Loading games list from "+myGamesXmlFile);

        List<String> directories = new ArrayList<String>();
        directories.add(DATA_DIR);
        try {
            // Find the <Games> tag
            Tag gamesTag =
                    Tag.findTopTagInFile(myGamesXmlFile, directories, GAMES_TAG);

            // Get all <Game> tags
            List<Tag> gameList = gamesTag.getChildren(GAME_TAG);

            for (Tag gameTag : gameList) {

                fillGameInfo(gameTag);
            }

            // Credits
            Tag creditsTag = gamesTag.getChild(CREDITS_TAG);
            if (creditsTag != null) {
                credits = creditsTag.getText();
            }

        } catch (ConfigurationException e) {
            System.out.println("Cannot open " + GAMES_XML);
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void fillGameInfo(Tag gameTag) throws ConfigurationException {

        // Get the game name
        String gameName = gameTag.getAttributeAsString(GAME_NAME_ATTR);
        if (!Util.hasValue(gameName)) {
            throw new ConfigurationException("Game name missing in "
                                             + GAMES_XML);
        }
        gameNames.add(gameName);

        // Get the Note
        Tag noteTag = gameTag.getChild(NOTE_TAG);
        if (noteTag != null) {
            String text = noteTag.getText();
            if (Util.hasValue(text)) notes.put(gameName, text);
        }

        // Get the Description
        Tag descrTag = gameTag.getChild(DESCR_TAG);
        if (descrTag != null) {
            String text = descrTag.getText();
            if (Util.hasValue(text)) descriptions.put(gameName, text);
        }

        // Get the minimum and maximum number of players
        Tag playersTag = gameTag.getChild(PLAYERS_TAG);
        if (playersTag != null) {
            Integer minimum = playersTag.getAttributeAsInteger(MIN_ATTR);
            Integer maximum = playersTag.getAttributeAsInteger(MAX_ATTR);
            if (minimum == 0 || maximum == 0) {
                throw new ConfigurationException(
                        "Min/max number of players missing in " + GAMES_XML);
            }
            minPlayers.put(gameName, minimum);
            maxPlayers.put(gameName, maximum);
        }

        // Get the options
        List<Tag> optionTagList = gameTag.getChildren(OPTION_TAG);
        List<GameOption> gameOptions = new ArrayList<GameOption>();

        if (optionTagList != null) {
            for (Tag optionTag : optionTagList) {

                // Option name (required)
                String optionName =
                        optionTag.getAttributeAsString(OPTION_NAME_ATTR);
                if (!Util.hasValue(optionName)) {
                    throw new ConfigurationException("Option name missing in "
                                                     + GAMES_XML);
                }

                // Option name parameters (optional)
                String[] optionParameters = null;
                String optionNameParameters =
                    optionTag.getAttributeAsString("parm");
	            if (optionNameParameters != null) {
	            	optionParameters = optionNameParameters.split(",");
	            }
                GameOption option = new GameOption(optionName, optionParameters);
                gameOptions.add(option);

                // Option type (optional).
                // "toggle" means this is a boolean option,
                // and it sets the allowed values to "yes" and "no".
                // Other values currently have no specific effect.
                String optionType =
                        optionTag.getAttributeAsString(OPTION_TYPE_ATTR);
                if (Util.hasValue(optionType)) option.setType(optionType);

                // Allowed values (redundant is type = "toggle")
                String optionValues =
                        optionTag.getAttributeAsString(OPTION_VALUES_ATTR);
                if (Util.hasValue(optionValues)) {
                    option.setAllowedValues(optionValues.split(","));
                }

                // Default value (optional, default default is the first value)
                String defaultValue =
                        optionTag.getAttributeAsString(OPTION_DEFAULT_ATTR);
                if (Util.hasValue(defaultValue)) {
                    option.setDefaultValue(defaultValue);
                }
            }
        }
        options.put(gameName, gameOptions);

    }

    public static List<String> getGameNames() {
        return instance.gameNames;
    }

    public static String getNote(String name) {
        return instance.notes.get(name);
    }

    public static String getDescription(String name) {
        return instance.descriptions.get(name);
    }

    public static int getMinPlayers(String name) {
        return instance.minPlayers.get(name);
    }

    public static int getMaxPlayers(String name) {
        return instance.maxPlayers.get(name);
    }

    public static List<GameOption> getOptions(String name) {
        return instance.options.get(name);
    }

    public static String getCredits() {
        return instance.credits;
    }

}
