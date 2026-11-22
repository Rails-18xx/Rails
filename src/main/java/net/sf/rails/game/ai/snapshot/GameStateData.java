package net.sf.rails.game.ai.snapshot;

import java.util.List;
import java.util.Map;

import net.sf.rails.game.ai.snapshot.GameStateData.CertificateData;
import net.sf.rails.game.ai.snapshot.GameStateData.PortfolioData;
import net.sf.rails.game.ai.snapshot.GameStateData.PrivateCompanyData;


/**
 * Root data structure for serializing the complete game state.
 * Contains no logic, only fields for JSON serialization.
 */
public class GameStateData {

    // Game-level info
    public GameManagerData gameManager;
    public PhaseData phase;
    public RoundData currentRound;
    public java.util.List<HexData> mapHexes = new java.util.ArrayList<>();
    public BankData bank;
    public MapData map;
    public List<TrainData> trains; // <-- ADD THIS MASTER LIST
    public java.util.Map<String, String> gameOptions;
    
    // Actor info
    public List<PlayerData> players;
    public List<CompanyData> publicCompanies;
    public List<PrivateCompanyData> privateCompanies;

    // ----- NESTED DATA CLASSES -----

    public static class GameManagerData {
        public String gameName;
        public int absoluteActionCounter;
        public int startRoundNumber;
        public int stockRoundNumber;
        public int absoluteORNumber;
        public int relativeORNumber;
        public String currentPlayerId;
        public String priorityPlayerId;
    }

    public static class PhaseData {
        public String currentPhaseName;
        public int offBoardRevenueStep;
    }

    public static class RoundData {
        public String type; // "StockRound", "OperatingRound", "StartRound"
        public String id;   // e.g., "OR_1.1"
        public String step; // e.g., "LAY_TILE", "BUY_TRAIN"
        public String operatingCompanyId; // Null if not OR
        public int numPasses;
        public java.util.List<String> operatingCompanyQueueIds = new java.util.ArrayList<>();
    }


    public static class MapData {
        public List<MapHexData> hexes;
    }

    public static class MapHexData {
        public String id; // "H12"
        public String tileId; // "57"
        public String rotationName; // e.g., "NORTH"
        // A list of tokens on this hex, e.g., ["id":"BY", "stationIndex":1]
        public List<TokenData> tokens;
    }

    public static class PlayerData {
        public String id;
        public int cash;
        public String fullName;
        public List<CertificateData> certificates;
        public List<String> privateCompanyIds; // IDs of privates owned
        public int timeBankSeconds;
        public java.util.List<String> soldCompanyIdsThisRound = new java.util.ArrayList<>();
    }

    public static class CompanyData {
        public String id;
        public int cash;
        public String presidentId;
        public boolean hasFloated;
        public boolean closed;

        public List<CertificateData> treasuryCertificates;

                public int stockPrice; // e.g., 100
        public int stockRow;
        public int stockCol;
        public int stockStackIndex = -1;
        public int totalTokens;
        public boolean hasOperated;
        public int bankLoan;

        public int lastRevenue;
        public int lastDividend;
        public String lastRevenueAllocation;
        public int lastDirectIncome;
        public int directIncomeRevenue;


    }

    public static class SpecialPropertyData {
        public String id;
        public boolean exercised;
        public int occurred;
    }

    public static class PrivateCompanyData {
        public String id;
        public String ownerId; // Player or Company ID
        public java.util.List<Integer> revenue;
        public boolean closed;
        public java.util.List<SpecialPropertyData> specialProperties = new java.util.ArrayList<>();
    }



    public static class CertificateData {
        public String companyId;
        public int percentage;
        public boolean isPresident;
    }


    public static class TrainData {
        public String id; // Unique ID, e.g., "3_1", "3_2"
        public String name; // "3", "4+4"
        public String ownerId; // ID of company, "IPO", "ScrapHeap"
        public boolean obsolete;
    }

    public static class TokenData {
        public String companyId;     // The ID of the company that owns the token (e.g., "BY")
        public int stationIndex; // The station number (1, 2, etc.) it's placed on
    }


    public static class PortfolioData {
        public java.util.List<CertificateData> certificates = new java.util.ArrayList<>();
        public java.util.List<PrivateCompanyData> privateCompanies = new java.util.ArrayList<>();
    }

    public static class BankData {
        public int cash;
        public boolean isBroken;
        public PortfolioData ipo;
        public PortfolioData pool;
        public PortfolioData unavailable;
        public PortfolioData scrapHeap;
        public PortfolioData osi;
    }
    // Represents a tile laid on a hex
    public static class HexData {
        public String id; // e.g., "D4"
        public String tileId; // e.g., "57"
        public int rotation; // 0-5
    }

}