package net.sf.rails.game;

import java.util.SortedSet;


import net.sf.rails.game.model.CertificatesModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;

/**
 * PlayerShareUtils is a class with static methods around president changes etc.
 */
public class PlayerShareUtils {
    
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
    
    public static int poolAllowsShareNumbers(PublicCompany company) {
        int poolShares = Bank.getPool(company).getPortfolioModel().getShareNumber(company);
        int poolMax = (GameDef.getGameParameterAsInt(company, GameDef.Parm.POOL_SHARE_LIMIT) - poolShares);
        return poolMax;
    }
    
    // President selling for companies WITHOUT multiple certificates
    private static SortedSet<Integer> presidentSellStandard(PublicCompany company, Player president) {
        int presidentShares = president.getPortfolioModel().getShareNumber(company);
        int poolShares = poolAllowsShareNumbers(company);
        
        // check if there is a potential new president ...
        int presidentCertificateShares = company.getPresidentsShare().getShares();
        Player potential = company.findPlayerToDump();

        int maxShares;
        if (potential == null) {
            // ... if there is none, selling is only possible until the presidentCerificate or pool maximum
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
        int playerShares = player.getPortfolioModel().getShareNumber(company);
        int poolShares = poolAllowsShareNumbers(company);
        
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
        int potentialShareNumber = potential.getPortfolioModel().getShare(company);
        int shareNumberDumpDifference = presidentShareNumber - potentialShareNumber;
        
        // ... if this is less than what the pool allows => goes back to non-president selling
        int poolAllows = poolAllowsShareNumbers(company);
        if (shareNumberDumpDifference <= poolAllows) {
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
        SortedSet<Integer> otherShareNumbers = CertificatesModel.shareNumberCombinations(otherCerts.build(), poolAllows);
        
        // fourth: combine pool and potential certificates, those are possible returns
        ImmutableList.Builder<PublicCertificate> returnCerts = ImmutableList.builder();
        returnCerts.addAll(Bank.getPool(company).getPortfolioModel().getCertificates(company));
        returnCerts.addAll(potential.getPortfolioModel().getCertificates(company));
        SortedSet<Integer> returnShareNumbers = CertificatesModel.shareNumberCombinations(returnCerts.build(), presidentCert.getShares());
        
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
                        sharesToSell.add(s);
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
            int poolAllows = poolAllowsShareNumbers(company);
            SortedSet<PublicCertificate> certificates = player.getPortfolioModel().getCertificates(company);
            return CertificatesModel.shareNumberCombinations(certificates, poolAllows);
        } else { // otherwise standard case
            return otherSellStandard(company, player);
        }
    }
    
}
