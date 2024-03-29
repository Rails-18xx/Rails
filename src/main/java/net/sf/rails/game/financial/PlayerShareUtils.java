package net.sf.rails.game.financial;

import java.util.*;

import net.sf.rails.game.GameDef;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate.Combination;
import net.sf.rails.game.model.CertificatesModel;
import net.sf.rails.game.state.Portfolio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlayerShareUtils is a class with static methods around president changes etc.
 */
public class PlayerShareUtils {

    private static final Logger log = LoggerFactory.getLogger(PlayerShareUtils.class);

    /** Find which number of shares a player can sell in case splitting is allowed
     * (also implying that half a presidency can be dumped, as in 1830 and in most other games)
     * @param company Company to sell shares of
     * @param player Selling player
     * @return A list of share quantities that can be sold, given the current pool capacity
     */
    public static SortedSet<Integer> sharesToSell (PublicCompany company, Player player) {
        
        if (company.hasMultipleCertificates()) {
            if (player == company.getPresident()) {
                return presidentSellMultiple(company, player);
            } else {
                return otherSellMultiple(company, player);
            }
        } else {
            if (player == company.getPresident()) {
                return presidentSellStandard(company, player);
            } else {
                return otherSellStandard(company, player);
            }
        }
        
    }

    /** Find which certificates a player can sell in case splitting is not allowed.
     * This implies that only whole certificates may be sold,
     * possibly including the president certificate.
     *
     * So far, this concept only applies to 1835.
     *
     * Note: the existence of non-president certs of different sizes is assumed,
     * as is the case in 1835. Hopefully it will also work generally.
     *
     * @param company Company to sell shares of
     * @param player Selling player
     * @return A Map from share sizes to quantities of that share type.
     */
    public static SortedMap<Integer, Integer> certificatesToSell (PublicCompany company, Player player) {

        int allowedSharesToPool = poolAllowsShares(company);

        SortedMap<Integer, Integer> sellableCertificates = new TreeMap<>();
        List<PublicCertificate> ownedCerts = player.getPortfolioModel().getCertificates().asList();
        for (PublicCertificate cert : ownedCerts) {
            if (cert.getCompany() == company) {
                if (cert.isPresidentShare()) continue;
                int certSize = cert.getShares();
                int certsOfSize = (sellableCertificates.containsKey(certSize)
                        ? sellableCertificates.get(certSize)
                        : 0);
                if ((certsOfSize + 1) * certSize <= allowedSharesToPool) {
                     sellableCertificates.put(certSize, certsOfSize + 1);
                }
                log.debug("{} size={} toSell={}", company, certSize, sellableCertificates);
            }
        }

        return sellableCertificates;
    }
    
    public static int poolAllowsShares(PublicCompany company) {
        int poolShares = Bank.getPool(company).getPortfolioModel().getShares(company);
        int poolMax = (GameDef.getParmAsInt(company, GameDef.Parm.POOL_SHARE_LIMIT) / company.getShareUnit()
                - poolShares);
        return poolMax;
    }
    
    // President selling for companies WITHOUT multiple certificates
    private static SortedSet<Integer> presidentSellStandard(PublicCompany company, Player president) {
        int presidentShares = president.getPortfolioModel().getShares(company);
        int poolShares = poolAllowsShares(company);
        
        // check if there is a potential new president ...
        int presidentCertificateShares = company.getPresidentsShare().getShares();
        Player potential = company.findPlayerToDump();

        int maxShares;
        if (potential == null) {
            // ... if there is none, selling is only possible until the presidentCertificate or pool maximum
            maxShares = Math.min(presidentShares - presidentCertificateShares, poolShares);
        } else { 
            // otherwise until pool maximum only
            maxShares = Math.min(presidentShares, poolShares);
        }
         
        ImmutableSortedSet.Builder<Integer> sharesToSell = ImmutableSortedSet.naturalOrder();
        for (int s=1; s <= maxShares; s++) {
            sharesToSell.add(s);
        }
        return sharesToSell.build();
    }
    
    // Non-president selling for companies WITHOUT multiple certificates
    private static SortedSet<Integer> otherSellStandard(PublicCompany company, Player player) {
        int playerShares = player.getPortfolioModel().getShares(company);
        int poolShares = poolAllowsShares(company);
        
        ImmutableSortedSet.Builder<Integer> sharesToSell = ImmutableSortedSet.naturalOrder();
        for (int s=1; s <= Math.min(playerShares, poolShares); s++) {
            sharesToSell.add(s);
        }
        return sharesToSell.build();
    }
    
    
    // President selling for companies WITH multiple certificates
    private static SortedSet<Integer> presidentSellMultiple(PublicCompany company, Player president) {
        
        // first: check what number of shares have to be dumped
        int presidentShareNumber = president.getPortfolioModel().getShare(company);
        PublicCertificate presidentCert = company.getPresidentsShare();
        Player potential = company.findPlayerToDump();

        int shareNumberDumpDifference = 0;
        // For unknown reasons, this method previously relied on dumping always be possible.
        // As that is not the case, it's hoped that the following added extra condition works out rightly. (EV)
        if (potential != null) {
            int potentialShareNumber = potential.getPortfolioModel().getShare(company);
            shareNumberDumpDifference = presidentShareNumber - potentialShareNumber;
        }

        boolean presidentShareOnly = false;
        if (presidentCert.getShare() == presidentShareNumber)  { // Only President Share to be sold...
            presidentShareOnly = true;
        }
        
        // ... if this is less than what the pool allows => goes back to non-president selling
        int poolAllows = poolAllowsShares(company);
        if ((shareNumberDumpDifference <= poolAllows) && (!presidentShareOnly)) {
            return otherSellMultiple(company, president);
        }
        
        // second: separate the portfolio into other shares and president certificate
        ImmutableList.Builder<PublicCertificate> otherCerts = ImmutableList.builder();
        for (PublicCertificate c:president.getPortfolioModel().getCertificates(company)) {
            if (!c.isPresidentShare()) {
                otherCerts.add(c);
            }
        }
        
        // third: retrieve the share number combinations of the non-president certificates
        SortedSet<Integer> otherShareNumbers = CertificatesModel.shareNumberCombinations(otherCerts.build(),
                poolAllows, false);
        
        // fourth: combine pool and potential certificates, those are possible returns
        ImmutableList.Builder<PublicCertificate> returnCerts = ImmutableList.builder();
        returnCerts.addAll(Bank.getPool(company).getPortfolioModel().getCertificates(company));
        if (potential != null) {
            returnCerts.addAll(potential.getPortfolioModel().getCertificates(company));
        }
        SortedSet<Integer> returnShareNumbers = CertificatesModel.shareNumberCombinations(returnCerts.build(),
                presidentCert.getShares(), true);
        
        ImmutableSortedSet.Builder<Integer> sharesToSell = ImmutableSortedSet.naturalOrder();
        for (Integer s:otherShareNumbers) {
            if (s <= shareNumberDumpDifference){
                // shareNumber is below or equal the dump difference => add as possibility to sell without dump
                sharesToSell.add(s);
            }
            // now check if there are dumping possibilities
            for (int d=1; d <= presidentCert.getShares(); d++) {
                if (s+d <= poolAllows) { 
                    // d is the amount sold in addition to standard shares, returned has the remaining part of the president share
                    int remaining = presidentCert.getShares() - d;
                    if (returnShareNumbers.contains(remaining)) {
                        sharesToSell.add(s+d);
                    }
                } else {
                    break; // pool is full
                }
            }
        }
        return sharesToSell.build();
    }
    
    // Non-president selling for companies WITH multiple certificates
    private static SortedSet<Integer> otherSellMultiple(PublicCompany company, Player player) {
        
        // check if there is a multiple certificate inside the portfolio
        if (player.getPortfolioModel().containsMultipleCert(company)) {
            int poolAllows = poolAllowsShares(company);
            SortedSet<PublicCertificate> certificates = player.getPortfolioModel().getCertificates(company);
            return CertificatesModel.shareNumberCombinations(certificates, poolAllows, false);
        } else { // otherwise standard case
            return otherSellStandard(company, player);
        }
    }

    // FIXME: Rails 2.x This is a helper function as long as the sold certificates are not stored
    public static int presidentShareNumberToSell(PublicCompany company, Player president, Player dumpedPlayer,
                                                 int nbCertsToSell) {
        log.debug ("Dump {} {} to {} certsToSell={}", company, president, dumpedPlayer, nbCertsToSell);
        int dumpThreshold = president.getPortfolioModel().getShares(company) - dumpedPlayer.getPortfolioModel().getShares(company);
        if (nbCertsToSell > dumpThreshold) {
            // reduce the nbCertsToSell by the presidentShare (but it can be sold partially...)
            return Math.min(company.getPresidentsShare().getShares(), nbCertsToSell);
        } else {
            return 0;
        }
    }
    
    // FIXME: Rails 2.x This is a helper function as long as the sold certificates are not stored
    // EV: dumpPossible parameter added to prevent that in 1835 a president share is sold
    // instead of a non-pres 20% share, if no dumping is possible.
    public static List<PublicCertificate> findCertificatesToSell(PublicCompany company, Player player,
                                                                 int nbCertsToSell, int shareSize,
                                                                 boolean dumpAllowed) {
        log.debug ("FindCertsToSell: {} {} number={} units={} dump={}",
                company, player, nbCertsToSell, shareSize, dumpAllowed);
        PublicCertificate presCert = null;
        // check for <= 0 => empty list
        if (nbCertsToSell <= 0) {
            return ImmutableList.of();
        }

        ImmutableList.Builder<PublicCertificate> certsToSell = ImmutableList.builder();
        for (PublicCertificate cert:player.getPortfolioModel().getCertificates(company)) {
            log.debug("Found {} {}%{}", cert, cert.getShares()*company.getShareUnit(),
                    (cert.isPresidentShare() ? "P" : ""));
            if (!cert.isPresidentShare() && cert.getShares() == shareSize) {
                log.debug("Added {}", cert);
                certsToSell.add(cert);
                nbCertsToSell--;
                if (nbCertsToSell == 0) {
                    break;
                }
            } else if (dumpAllowed && cert.isPresidentShare() && cert.getShares()== shareSize) {
                // Pres.share must be added last, if needed at all
                presCert = cert;
                if (nbCertsToSell == 0) {
                    break;
                }
            }
        }
        if (nbCertsToSell > 0 && presCert != null) {
            log.debug("Added {}P", presCert);
            certsToSell.add(presCert);
            nbCertsToSell--;
        }
        
        return certsToSell.build();
    }

    public static void executePresidentTransferAfterDump(PublicCompany company, Player newPresident,
                                                         BankPortfolio bankTo, int presSharesToSell) {

        Player oldPresident = company.getPresident();
        log.debug("Company = {}, presSharesToSell={}", company, presSharesToSell);

        // 1. Move the swap certificates from new president to the pool
        PublicCertificate presidentCert = company.getPresidentsShare();

        // ... get all combinations for the presidentCert share numbers
        SortedSet<Combination> combinations = CertificatesModel.certificateCombinations(
                newPresident.getPortfolioModel().getCertificates(company), presidentCert.getShares());
        log.debug("newPres combinations={} (owned {})", combinations, newPresident.getPortfolioModel().getCertificates(company));
    
        // ... move them to the Bank
        // FIXME: this should be based on a selection of the new president, however it chooses the combination with most certificates
        Combination swapToBank = combinations.last();
        log.debug("swapToBank={} from newPres to pool", swapToBank);
        Portfolio.moveAll(swapToBank, bankTo);
        
        // 2. Move the replace certificates from the bank to the old president
        
        // What is the difference between the shares to sell and the president share number
        int replaceShares = presidentCert.getShares() - presSharesToSell;
        log.debug("presShares={} presSharesToSell={} replaceShares={}",
                presidentCert.getShares(), presSharesToSell, replaceShares);
        if (replaceShares > 0) {
            combinations = CertificatesModel.certificateCombinations(
                    bankTo.getPortfolioModel().getCertificates(company), replaceShares);
            log.debug("pool combinations={} (owned {})", combinations, bankTo.getPortfolioModel().getCertificates(company));
            // FIXME: this should be based on a selection of the previous president, however it chooses the combination with least certificates
            Combination swapFromBank = combinations.first();
            log.debug("swapFromBank={} from pool to old pres.", swapFromBank);
            // ... move to (old) president
            Portfolio.moveAll(swapFromBank, oldPresident);
        }
        
        // 3. Transfer the president certificate
        log.debug ("move pres.cert from {} to {}", oldPresident, newPresident);
        presidentCert.moveTo(newPresident);
        log.debug("newPresident ({}) now has {}", newPresident, newPresident.getPortfolioModel().getCertificates(company));
        log.debug("oldPresident ({}) now has {}", oldPresident, oldPresident.getPortfolioModel().getCertificates(company));
        log.debug("pool now has {}", bankTo.getPortfolioModel().getCertificates(company));
    }
    
}
