package net.sf.rails.game.ai.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

// Imports based on GameManager.java and compiler errors
import net.sf.rails.game.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.round.RoundFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles serializing the current GameManager state into a
 * JSON file using simple data-transfer objects (GameStateData).
 */
public class JsonStateSerializer {

    private static final Logger log = LoggerFactory.getLogger(JsonStateSerializer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT); // For human-readable JSON


    private static GameStateData.GameManagerData populateGameManagerData(GameManager gm) {
        GameStateData.GameManagerData data = new GameStateData.GameManagerData();
        data.gameName = gm.getGameName();
// FIX: We must use the 'actionCount' (IntegerState) which is incremented
        // during replay, not 'absoluteActionCounter' (int) which is not.
        data.absoluteActionCounter = gm.getActionCountModel().value();

        data.startRoundNumber = gm.getStartRoundNumber();
        data.stockRoundNumber = gm.getSRNumber();
        data.absoluteORNumber = gm.getAbsoluteORNumber();
        data.relativeORNumber = gm.getRelativeORNumber();
if (gm.getCurrentPlayer() != null) {
            data.currentPlayerId = gm.getCurrentPlayer().getId();
        } else {
            data.currentPlayerId = "System"; // Fallback value
        }
                data.priorityPlayerId = gm.getRoot().getPlayerManager().getPriorityPlayer().getId();
        return data;
    }

private static GameStateData.PhaseData populatePhaseData(PhaseManager pm) {
        GameStateData.PhaseData data = new GameStateData.PhaseData();
        // Get the actual, live Phase object
        Phase currentPhase = pm.getCurrentPhase();
        
        data.currentPhaseName = currentPhase.toText();
        
        // This is the critical fix: save the field the AI needs
        data.offBoardRevenueStep = currentPhase.getOffBoardRevenueStep();
        return data;
    }

    private static GameStateData.RoundData populateRoundData(RoundFacade round) {
        GameStateData.RoundData data = new GameStateData.RoundData();
        data.id = round.getId();

        if (round instanceof OperatingRound) {
            data.type = "OperatingRound";
            OperatingRound or = (OperatingRound) round;
            data.step = or.getStep().name();
            if (or.getOperatingCompany() != null) {
                data.operatingCompanyId = or.getOperatingCompany().getId();
            }
            if (or.getOperatingCompanies() != null) {
                data.operatingCompanyQueueIds = or.getOperatingCompanies().stream()
                        .map(net.sf.rails.game.PublicCompany::getId)
                        .collect(Collectors.toList());
            }
        } else if (round instanceof StockRound) {
            data.type = "StockRound";
            data.numPasses = ((StockRound) round).getNumPasses();
        } else {
            data.type = "Other";
        }
        return data;
    }

    
    private static GameStateData.MapData populateMapData(MapManager mm) {
        GameStateData.MapData data = new GameStateData.MapData();
        data.hexes = mm.getHexes().stream()
                .map(JsonStateSerializer::populateMapHexData)
                .collect(Collectors.toList());
        return data;
    }

    private static GameStateData.MapHexData populateMapHexData(MapHex hex) {
        GameStateData.MapHexData data = new GameStateData.MapHexData();
        data.id = hex.getId();
        if (hex.getCurrentTile() != null) {
            // Corrected method: toText()
            data.tileId = hex.getCurrentTile().toText();
            // Corrected method: getOrientationName()
            data.rotationName = hex.getOrientationName(hex.getCurrentTileRotation());
        }
        
// Capture token data
        data.tokens = new ArrayList<>();
        for (Stop stop : hex.getStops()) {
            for (BaseToken token : stop.getBaseTokens()) {
                GameStateData.TokenData tokenData = new GameStateData.TokenData();
                
                // FIX: The correct method to get the company is getParent()
                tokenData.companyId = ((PublicCompany) token.getParent()).getId();
                
                tokenData.stationIndex = stop.getRelatedStationNumber();
                data.tokens.add(tokenData);
            }
        }
        return data;
    }

    private static GameStateData.PlayerData populatePlayerData(Player player) {
        GameStateData.PlayerData data = new GameStateData.PlayerData();
        data.id = player.getId();
        data.cash = player.getCash();
        data.fullName = player.getFullName();
        // Corrected method: value()
        data.timeBankSeconds = player.getTimeBankModel().value();
// Record which companies the player has sold this round
        if (player.getParent() != null && player.getParent().getRoot() != null) {
            data.soldCompanyIdsThisRound = player.getParent().getRoot().getCompanyManager().getAllPublicCompanies().stream()
                    .filter(player::hasSoldThisRound)
                    .map(net.sf.rails.game.PublicCompany::getId)
                    .collect(Collectors.toList());
        }
        data.certificates = player.getPortfolioModel().getCertificates().stream()
                .map(JsonStateSerializer::populateCertificateData)
                .collect(Collectors.toList());

        data.privateCompanyIds = player.getPortfolioModel().getPrivateCompanies().stream()
                .map(PrivateCompany::getId)
                .collect(Collectors.toList());
        return data;
    }

    private static GameStateData.CompanyData populateCompanyData(PublicCompany co) {
        GameStateData.CompanyData data = new GameStateData.CompanyData();
        data.id = co.getId();
        data.cash = co.getCash();
        if (co.getPresident() != null) {
            data.presidentId = co.getPresident().getId();
        }
        data.hasFloated = co.hasStarted();
        data.closed = co.isClosed();
        if (co.getCurrentSpace() != null) {
            // Corrected method: getPrice()
            data.stockPrice = co.getCurrentSpace().getPrice();
            data.stockRow = co.getCurrentSpace().getRow();
            data.stockCol = co.getCurrentSpace().getColumn();
            data.stockStackIndex = co.getCurrentSpace().getStackPosition(co);
        }
        // Capture dynamic token count (crucial for merged companies like Prussia)
        data.totalTokens = co.getNumberOfBaseTokens();
        data.hasOperated = co.hasOperated();
        data.bankLoan = co.getBankLoan();
        data.lastRevenue = co.getLastRevenue();
        data.lastDividend = co.getLastDividend();
        data.lastRevenueAllocation = co.getlastRevenueAllocationText();
        data.lastDirectIncome = co.getLastDirectIncome();
        data.directIncomeRevenue = co.getDirectIncomeRevenue();

        // Removed train logic

        data.treasuryCertificates = co.getPortfolioModel().getCertificates().stream()
                .map(JsonStateSerializer::populateCertificateData)
                .collect(Collectors.toList());
        return data;
    }
    private static GameStateData.TrainData populateTrainData(Train t) {
        GameStateData.TrainData data = new GameStateData.TrainData();
        data.id = t.getId();
        data.name = t.toText();
        if (t.getOwner() != null) {
            data.ownerId = t.getOwner().getId();
        }
        data.obsolete = t.isObsolete();
        return data;
    }

    private static GameStateData.SpecialPropertyData populateSpecialPropertyData(net.sf.rails.game.special.SpecialProperty sp) {
        GameStateData.SpecialPropertyData data = new GameStateData.SpecialPropertyData();
        data.id = sp.getId();
        data.exercised = sp.isExercised();
        data.occurred = sp.getOccurred();
        return data;
    }

    private static GameStateData.PrivateCompanyData populatePrivateCompanyData(PrivateCompany pc) {
        GameStateData.PrivateCompanyData data = new GameStateData.PrivateCompanyData();
        data.id = pc.getId();
        data.revenue = pc.getRevenue();
        if (pc.getOwner() != null) {
            data.ownerId = pc.getOwner().getId();
        }
        data.closed = pc.isClosed();
        data.specialProperties = pc.getSpecialProperties().stream()
                .map(JsonStateSerializer::populateSpecialPropertyData)
                .collect(Collectors.toList());
        return data;
    }

    private static GameStateData.CertificateData populateCertificateData(PublicCertificate cert) {
        GameStateData.CertificateData data = new GameStateData.CertificateData();
        data.companyId = cert.getCompany().getId();
        data.percentage = cert.getShare();
        data.isPresident = cert.isPresidentShare();
        return data;
    }


    // ... (lines of unchanged context code) ...
    public static void serialize(GameManager gm, String filename) throws IOException {
        log.debug("Serializing state for action {} to {}", gm.getCurrentActionCount(), filename);

        // 1. Create and populate the root state object
        GameStateData state = createSnapshotData(gm); // <-- CALL THE NEW METHOD

        // 2. Write to file
        objectMapper.writeValue(new File(filename), state);
        log.debug("Serialization complete.");
    }

    /**
     * Creates and populates the root GameStateData object for the given GameManager.
     * This separates the computation from the I/O and is necessary for analysis tools.
     * * @param gm The current GameManager instance.
     * @return The fully populated GameStateData POJO.
     */
    public static GameStateData createSnapshotData(GameManager gm) {
        // 1. Create the root state object
        GameStateData state = new GameStateData();
        RailsRoot root = gm.getRoot();

        // 2. Populate all components
        state.gameManager = populateGameManagerData(gm);
        state.phase = populatePhaseData(root.getPhaseManager());
        
        // FIX: The Bank is needed but the BankData method was not complete/called correctly
        // We need to call a method that populates the BankData, even if it's currently simple.
        state.bank = populateBankData(root.getBank()); // Assuming a simple bank method exists.

        state.currentRound = populateRoundData(gm.getCurrentRound());
        state.map = populateMapData(root.getMapManager());

        state.players = root.getPlayerManager().getPlayers().stream()
                .map(JsonStateSerializer::populatePlayerData)
                .collect(Collectors.toList());

        state.publicCompanies = root.getCompanyManager().getAllPublicCompanies().stream()
                .map(JsonStateSerializer::populateCompanyData)
                .collect(Collectors.toList());

        state.privateCompanies = root.getCompanyManager().getAllPrivateCompanies().stream()
                .map(JsonStateSerializer::populatePrivateCompanyData)
                .collect(Collectors.toList());

        state.trains = root.getTrainManager().getAllTrains().stream()
                .map(JsonStateSerializer::populateTrainData)
                .collect(Collectors.toList());
                
                // Serialize Map Tiles (only those that differ from the printed board)
        if (root.getMapManager() != null && root.getMapManager().getHexes() != null) {
            state.mapHexes = root.getMapManager().getHexes().stream()
                    .filter(hex -> !hex.isPreprintedTileCurrent())
                    .map(JsonStateSerializer::populateHexData)
                    .collect(Collectors.toList());
        }
        if (root.getGameOptions() != null) {
            state.gameOptions = root.getGameOptions().getOptions();
        }
        return state;
    }

    private static GameStateData.HexData populateHexData(MapHex hex) {
        GameStateData.HexData data = new GameStateData.HexData();
        data.id = hex.getId();
        data.tileId = hex.getCurrentTile().getId();
        data.rotation = hex.getTileRotation();
        return data;
    }

    private static GameStateData.PortfolioData populatePortfolioData(net.sf.rails.game.financial.BankPortfolio bp) {
        GameStateData.PortfolioData data = new GameStateData.PortfolioData();
        data.certificates = bp.getPortfolioModel().getCertificates().stream()
                .map(JsonStateSerializer::populateCertificateData)
                .collect(Collectors.toList());
        data.privateCompanies = bp.getPortfolioModel().getPrivateCompanies().stream()
                .map(JsonStateSerializer::populatePrivateCompanyData)
                .collect(Collectors.toList());
        return data;
    }

    private static GameStateData.BankData populateBankData(Bank bank) {
        GameStateData.BankData data = new GameStateData.BankData();
        data.cash = bank.getCash();
        data.isBroken = bank.isBroken();

        data.ipo = populatePortfolioData(bank.getIpo());
        data.pool = populatePortfolioData(bank.getPool());
        data.unavailable = populatePortfolioData(bank.getUnavailable());
        data.scrapHeap = populatePortfolioData(bank.getScrapHeap());
        data.osi = populatePortfolioData(bank.getOSI());

        return data;
    }
    
}