package net.sf.rails.game.ai;

import java.util.List;

/**
 * Contains a set of "Plain Old Java Objects" (POJOs) that mirror the
 * exact structure of the state_NNN.json file. The Gson library uses these
 * classes as a template to parse the JSON data.
 */
public class JsonStatePojos {

    // Note: The class names and field names MUST match the JSON keys

    public static class JsonState {
        public GameManager gameManager;
        public Phase phase;
        public CurrentRound currentRound;
        public Bank bank;
        public Map map;
        public List<Player> players;
        public List<PublicCompany> publicCompanies;
        public List<PrivateCompany> privateCompanies;
    }

    public static class GameManager {
        public int absoluteActionCounter;
        public int startRoundNumber;
        public int stockRoundNumber;
        public int absoluteORNumber;
        public int relativeORNumber;
        public String currentPlayerId;
        public String priorityPlayerId;
    }

    public static class Phase {
        public String currentPhaseName;
    }

    public static class CurrentRound {
        public String type;
        public String id;
        public String step;
        public String operatingCompanyId;
    }

    public static class Bank {
        public int cash;
        public List<Certificate> poolCertificates;
        public List<Train> ipoTrains;
    }

    public static class Map {
        public List<Hex> hexes;
    }

    public static class Hex {
        public String id;
        public String tileId;
        public String rotationName;
    }

    public static class Player {
        public String id;
        public int cash;
        public List<Certificate> certificates;
        public List<String> privateCompanyIds;
        public int timeBankSeconds;
    }

    public static class PublicCompany {
        public String id;
        public int cash;
        public String presidentId;
        public boolean hasFloated;
        public int stockPrice;
        public int stockRow;
        public int stockCol;
        public List<Certificate> treasuryCertificates;
    }

    public static class PrivateCompany {
        public String id;
        public String ownerId;
        public List<Integer> revenue;
    }

    public static class Certificate {
        public String companyId;
        public int percentage;
        public boolean isPresident;
    }

    public static class Train {
        public String name;
        public String ownerId;
    }
}
