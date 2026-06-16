package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.state.Currency;

/**
 * Helper class for 1837-specific merger logic (Sd, KK, Ug).
 * Encapsulates robust asset transfer, token placement, and share exchange
 * logic.
 */
public class Merger1837 {

    private static final Logger log = LoggerFactory.getLogger(Merger1837.class);


    /**
     * Centralized logic to determine if a company merges into another.
     * STRICTLY checks IDs to avoid false positives (e.g. SB -> Sd).
     */
    public static PublicCompany getMergeTarget(GameManager gameManager, PublicCompany source) {
        if (source == null)
            return null;

        String id = source.getId();
        String targetId = null;

        // 1. Explicit Coal Mappings (Exact Match)
        switch (id) {
            case "EPP":
            case "RGTE":
                targetId = "BK";
                break;

            case "EOD":
            case "EKT":
                targetId = "MS";
                break;

            case "MLB":
                targetId = "CL";
                break;

            case "ZKB":
            case "SPB":
                targetId = "SB";
                break;

            case "LRB":
            case "EHS":
                targetId = "TH";
                break;

            case "BB":
                targetId = "BH";
                break;
        }

        // 2. National Minor Mappings (Strict Regex)
        // Only matches S1-S5, K1-K3, U1-U3.
        // Ignores "SB", "Sd", "KK", "Ug", "KA", etc.
        if (targetId == null) {
            if (id.matches("S[1-5]")) {
                targetId = "Sd";
            } else if (id.matches("K[1-3]")) {
                targetId = "KK";
            } else if (id.matches("U[1-3]")) {
                targetId = "Ug";
            }
        }

        if (targetId != null) {
            return gameManager.getRoot().getCompanyManager().getPublicCompany(targetId);
        }
        return null;
    }

    public static void fixDirectorship(GameManager gm, PublicCompany major) {
        fixDirectorship(gm, major, null, null, null);
    }

    /**
     * Overloaded to handle complex tie-breakers for 1837 national companies (e.g.,
     * Ug).
     */
    public static void fixDirectorship(GameManager gm, PublicCompany major, Map<Player, Integer> directorHistory,
            Map<Player, Integer> ownerHistory, Player formerU1Owner) {

        // 1. Calculate Holdings deterministically based on seat order
        Map<Player, Integer> shareCounts = new java.util.LinkedHashMap<>();
        for (Player p : gm.getPlayers()) {
            shareCounts.put(p, 0);
        }
        for (PublicCertificate cert : major.getCertificates()) {
            if (cert.getOwner() instanceof Player) {
                Player p = (Player) cert.getOwner();
                shareCounts.put(p, shareCounts.get(p) + cert.getShare());
            }
        }

        Player currentPrez = major.getPresident();
        // If currentPrez is null (e.g., just formed), identify who actually holds the
        // president certificate
        if (currentPrez == null) {
            for (PublicCertificate cert : major.getCertificates()) {
                if (cert.isPresidentShare() && cert.getOwner() instanceof Player) {
                    currentPrez = (Player) cert.getOwner();
                    break;
                }
            }
        }

        // If NO ONE holds the President's share, the company has not floated and has no
        // director.
        // We must prevent 10% coal exchange holders from stealing the 20% President's
        // share from the IPO.
        if (currentPrez == null) {
            return;
        }

        // 2. Find Leader
        Player newPrez = null;
        int maxShare = -1;

        List<Player> tiedPlayers = new ArrayList<>();

        for (Map.Entry<Player, Integer> entry : shareCounts.entrySet()) {
            int share = entry.getValue();
            if (share > maxShare) {
                maxShare = share;
                tiedPlayers.clear();
                tiedPlayers.add(entry.getKey());
            } else if (share == maxShare) {
                tiedPlayers.add(entry.getKey());
            }
        }

        if (tiedPlayers.size() == 1) {
            newPrez = tiedPlayers.get(0);
            ReportBuffer.add(gm,
                    "DIRECTORSHIP RESOLVED: " + newPrez.getName() + " holds strict majority (" + maxShare + "%).");

        } else if (tiedPlayers.size() > 1) {
            // TIE BREAKER LOGIC
            ReportBuffer.add(gm,
                    "DIRECTORSHIP TIE: Multiple players tied at " + maxShare + "%. Executing 1837 tie-breaker rules.");

            if ("Ug".equals(major.getId()) && directorHistory != null && ownerHistory != null
                    && formerU1Owner != null) {
                // Rule 1: Director of lowest-numbered minor
                int lowestDir = 999;
                for (Player p : tiedPlayers) {
                    if (directorHistory.containsKey(p) && directorHistory.get(p) < lowestDir) {
                        lowestDir = directorHistory.get(p);
                        newPrez = p;
                    }
                }
                if (newPrez != null) {
                    ReportBuffer.add(gm, "Tie resolved via Rule 1 (Director of lowest-numbered minor). Winner: "
                            + newPrez.getName());
                } else {
                    // Rule 2: Owner of lowest-numbered minor
                    int lowestOwn = 999;
                    for (Player p : tiedPlayers) {
                        if (ownerHistory.containsKey(p) && ownerHistory.get(p) < lowestOwn) {
                            lowestOwn = ownerHistory.get(p);
                            newPrez = p;
                        }
                    }
                    if (newPrez != null) {
                        ReportBuffer.add(gm, "Tie resolved via Rule 2 (Owner of lowest-numbered minor). Winner: "
                                + newPrez.getName());
                    }
                }
                if (newPrez == null) {
                    // Rule 3: Closest to left of former U1 owner
                    List<Player> players = gm.getPlayers();
                    int u1Index = players.indexOf(formerU1Owner);
                    int minDistance = 999;
                    for (Player p : tiedPlayers) {
                        int pIndex = players.indexOf(p);
                        int dist = (pIndex > u1Index) ? (pIndex - u1Index) : (pIndex + players.size() - u1Index);
                        if (dist < minDistance) {
                            minDistance = dist;
                            newPrez = p;
                        }
                    }
                    if (newPrez != null) {
                        ReportBuffer.add(gm,
                                "Tie resolved via Rule 3 (Closest to left of U1 owner). Winner: " + newPrez.getName());
                    }
                }
            } else {
                // Standard tie-breaker: Incumbent wins
                if (currentPrez != null && tiedPlayers.contains(currentPrez)) {
                    newPrez = currentPrez;
                    ReportBuffer.add(gm, "Tie resolved: Incumbent director " + newPrez.getName() + " retains control.");
                } else {
                    newPrez = tiedPlayers.get(0); // Fallback
                    ReportBuffer.add(gm, "Tie resolved via default fallback. Winner: " + newPrez.getName());
                }
            }
        }

        // 3. Execute Swap
        if (newPrez != null && !newPrez.equals(currentPrez)) {

            PublicCertificate presCert = null;
            for (PublicCertificate c : major.getCertificates()) {
                if (c.isPresidentShare()) {
                    presCert = c;
                    break;
                }
            }

            if (presCert != null) {
                // Move Pres Cert to New Prez
                presCert.moveTo(newPrez);

                // Return 2x 10% shares
                int valueToReturn = presCert.getShare(); // 20
                int valueReturned = 0;

                // Fetch New Prez's certs again to find return candidates
                List<PublicCertificate> returnCandidates = new ArrayList<>();
                for (PublicCertificate c : major.getCertificates()) {
                    if (c.getOwner().equals(newPrez) && !c.isPresidentShare()) {
                        returnCandidates.add(c);
                    }
                }

                for (PublicCertificate c : returnCandidates) {
                    if (valueReturned < valueToReturn) {
                        if (currentPrez != null) {
                            c.moveTo(currentPrez);
                        } else {
                            c.moveTo(Bank.getUnavailable(gm.getRoot()));
                        }
                        valueReturned += c.getShare();
                    }
                }

                major.setPresident(newPrez);

                // String msg = "Director of " + major.getId() + " changes to " +
                // newPrez.getName();
                // ReportBuffer.add(gm, msg);
                // DisplayBuffer.add(gm, msg);
            }
        }
    }


    // ... (lines of unchanged context code) ...
    public static String build1837StateReport(GameManager gm, PublicCompany comp) {
        // --- START FIX ---
        if (comp == null)
            return "None";

        StringBuilder report = new StringBuilder();
        String type = comp.getType().getId();
        String presName = comp.getPresident() != null ? comp.getPresident().getName() : "Bank/IPO";

        report.append(comp.getId()).append(" (Type: ").append(type)
                .append(", President: ").append(presName).append(")\n");
        report.append("  Cash: ").append(Bank.format(gm, comp.getCash()));

        // Trains
        java.util.List<String> trains = new java.util.ArrayList<>();
        for (net.sf.rails.game.Train t : comp.getTrains()) {
            trains.add(t.getName());
        }
        report.append(" | Trains: ").append(trains.isEmpty() ? "None" : String.join(", ", trains));

        // Tokens
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (net.sf.rails.game.BaseToken t : comp.getLaidBaseTokens()) {
            if (t.getOwner() instanceof net.sf.rails.game.Stop) {
                tokens.add(((net.sf.rails.game.Stop) t.getOwner()).getHex().getId());
            }
        }
        report.append(" | Tokens: ").append(tokens.isEmpty() ? "None" : String.join(", ", tokens));

        // Share Distribution (For Majors/Nationals only)
        if (!type.equals("Minor") && !type.equals("Coal")) {
            Map<String, Integer> ownership = new java.util.LinkedHashMap<>();
            int ipo = 0;
            for (net.sf.rails.game.financial.PublicCertificate cert : comp.getCertificates()) {
                if (cert.getOwner() instanceof Player) {
                    String pName = ((Player) cert.getOwner()).getName();
                    ownership.put(pName, ownership.getOrDefault(pName, 0) + cert.getShare());
                } else if (cert.getOwner() == comp || cert.getOwner() == gm.getRoot().getBank().getIpo()) {
                    ipo += cert.getShare();
                }
            }

            java.util.List<String> ownStrings = new java.util.ArrayList<>();
            for (Map.Entry<String, Integer> e : ownership.entrySet()) {
                ownStrings.add(e.getKey() + ": " + e.getValue() + "%");
            }
            if (ipo > 0)
                ownStrings.add("IPO: " + ipo + "%");

            report.append("\n  Ownership: ").append(String.join(", ", ownStrings));
        }

        return report.toString();
        // --- END FIX ---
    }

    /**
     * Merges a single minor company into a major company.
     * Handles: Assets, Trains, Closing, Token Swap, and Share Exchange.
     */
public static void mergeMinor(GameManager gm, PublicCompany minor, PublicCompany major) {
        String id = minor.getId();
        Player owner = minor.getPresident();
        log.info("Merging Minor: " + id + " into " + major.getId() + " (Owner: "
                + (owner != null ? owner.getName() : "Bank/IPO") + ")");

        ReportBuffer.add(gm, "--- MINOR MERGER EXECUTED: " + id + " folds into " + major.getId() + " ---");
        ReportBuffer.add(gm, "Pre-Merger State (" + id + "):\n" + build1837StateReport(gm, minor));
        ReportBuffer.add(gm, "Pre-Merger State (" + major.getId() + "):\n" + build1837StateReport(gm, major));

        // Assets MUST be moved before the minor is modified or closed
        log.info("1837_MERGER: Commencing asset transfer for " + id);
        major.transferAssetsFrom(minor);
        log.info("1837_MERGER: Assets (Trains, Cash, Privates) transferred via transferAssetsFrom().");

        // --- 1. LOCATE HOME STOP (For Token Placement) ---
        MapHex homeHex = null;
        Stop targetStop = null;
        if (minor.getHomeHexes() != null && !minor.getHomeHexes().isEmpty()) {
            homeHex = minor.getHomeHexes().get(0);
            if (homeHex != null) {
                targetStop = homeHex.getRelatedStop(minor.getHomeCityNumber());
            }
        }

        // --- 2. ASSET TRANSFER ---
        int cash = minor.getCash();
        if (cash > 0) {
            Currency.toBank(minor, cash);
            Currency.fromBank(cash, major);
            log.info("Transferred " + cash + " from " + id);
        }

        List<net.sf.rails.game.PrivateCompany> privates = new ArrayList<>(minor.getPrivates());
        for (net.sf.rails.game.PrivateCompany p : privates) {
            p.moveTo(major);
        }

       
        // --- 3. SHARE EXCHANGE ---
        boolean isPriorityMinor = id.equals("S1") || id.equals("K1"); // U1 excluded to allow tie-breaker resolution
        net.sf.rails.game.financial.BankPortfolio unavailable = net.sf.rails.game.financial.Bank.getUnavailable(gm.getRoot());
        
        List<PublicCertificate> minorCerts = new ArrayList<>(minor.getCertificates());
        log.info("1837_MERGER: Commencing share exchange for " + id + ". Found " + minorCerts.size() + " certificates.");

        for (PublicCertificate mCert : minorCerts) {
            if (mCert.getOwner() instanceof Player) {
                Player p = (Player) mCert.getOwner();
                int minorSharePercentage = mCert.getShare();
                
                mCert.moveTo(unavailable);
                PublicCertificate shareToGive = null;
                boolean givePresident = isPriorityMinor && mCert.isPresidentShare();

                if (givePresident) {
                    for (PublicCertificate cert : major.getCertificates()) {
                        if (cert.isPresidentShare() && !(cert.getOwner() instanceof Player)) {
                            shareToGive = cert;
                            break;
                        }
                    }
                }

// 1. Prioritize reserved exchange shares which reside in the 'Unavailable' portfolio.
                if (shareToGive == null) {
                    for (PublicCertificate cert : major.getCertificates()) {
                        if (!cert.isPresidentShare() && cert.getOwner() == unavailable) {
                            shareToGive = cert;
                            break;
                        }
                    }
                }

                // Fallback: If no non-president shares remain, assign the President's share.
                // In 1837, National President shares are 10%, identical in equity to regular shares.
                if (shareToGive == null) {
                    for (PublicCertificate cert : major.getCertificates()) {
                        if (!(cert.getOwner() instanceof Player)) {
                            shareToGive = cert;
                            log.info("1837_MERGER: Distributing 10% President's share as standard exchange fallback.");
                            break;
                        }
                    }
                }

                if (shareToGive != null) {
                    shareToGive.moveTo(p);
                    String msg = "EXCHANGE: " + p.getName() + " exchanges " + id + " (" + minorSharePercentage + "%) for " + major.getId() + " " + shareToGive.getShare() + "% Exchange Share.";
                    ReportBuffer.add(gm, msg);
                    log.info("1837_MERGER: " + msg);
                } else {
                    log.error("1837_MERGER CRITICAL: No share available in " + major.getId() + " for " + p.getName());
                }
            }
        }

        // --- 4. CLOSE MINOR ---
        log.info("1837_MERGER: Closing minor " + id);
        minor.setClosed();

        // --- 5. PLACE MAJOR TOKEN ---
        boolean isCoal = "Coal".equals(minor.getType().getId());
        if (!isCoal && targetStop != null) {
            boolean alreadyHasToken = false;
            if (targetStop.getTokens() != null) {
                for (BaseToken t : targetStop.getTokens()) {
                    if (t.getOwner() != null && t.getOwner().equals(major)) {
                        alreadyHasToken = true;
                        break;
                    }
                }
            }

            boolean isS5 = id.equals("S5");
            if (!alreadyHasToken && !isS5) {
                BaseToken tokenToPlace = null;
                for (BaseToken t : major.getAllBaseTokens()) {
                    if (!t.isPlaced()) {
                        tokenToPlace = t;
                        break;
                    }
                }
                if (tokenToPlace != null) {
                    tokenToPlace.moveTo(targetStop);
                    String location = (homeHex != null) ? homeHex.getId() : "Unknown";
                    log.info("1837_MERGER: Placed " + major.getId() + " token on hex " + location);
                }
            }
        }
        
        ReportBuffer.add(gm, "Post-Merger State (" + major.getId() + "):\n" + build1837StateReport(gm, major));
    }
}