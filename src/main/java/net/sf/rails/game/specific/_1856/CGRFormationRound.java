package net.sf.rails.game.specific._1856;

import java.util.*;

import com.google.common.collect.Lists;

import rails.game.action.*;
import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.state.ArrayListMultimapState;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Portfolio;

// Rails 2.0 refactoring

public class CGRFormationRound extends SwitchableUIRound {

    private static final int[][] certLimitsTable = {
        {14, 19, 21, 26, 29, 31, 36, 40},
        {10, 13, 15, 18, 20, 22, 25, 28},
        {8, 10, 12, 14, 16, 18, 20, 22},
        {7, 8, 10, 11, 13, 15, 16, 18},
        {6, 7, 8, 10, 11, 12, 14, 15}
    };

    // static variables
    private final PublicCompany_CGR cgr;

    // initialized in start() method only
    private Player startingPlayer;
    private int maxLoansToRepayByPresident = 0;

    /*
     * effects from the merger, processed at the end
     * thus no need for state variables
     */
    private Set<Train> trainsToDiscardFrom = null;
    private boolean forcedTrainDiscard = true;
    private List<ExchangeableToken> tokensToExchangeFrom = null;
    private List<BaseToken> nonHomeTokens = null;

    
    // dynamic variables
    private final GenericState<Steps> step = 
            GenericState.create(this, "step");

    private final ArrayListMultimapState<Player, PublicCompany> companiesToRepayLoans = 
            ArrayListMultimapState.create(this, "companiesToRepayLoans");

    private final GenericState<PublicCompany> currentCompany =
            GenericState.create(this, "currentCompany");

    private ArrayListState<PublicCompany> mergingCompanies = 
            ArrayListState.create(this, "mergingCompanies");

    private final BooleanState cgrHasDiscardedTrains = 
            BooleanState.create(this, "cgrHasDiscardedTrains");


    /**
     * Constructed via Configure
     */
    public CGRFormationRound(GameManager parent, String id) {
        super(parent, id);
        
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        
        cgr = (PublicCompany_CGR) getRoot().getCompanyManager().getPublicCompany(PublicCompany_CGR.NAME);
    }
    
    public void start (Player startingPlayer) {

        // store starting player
        this.startingPlayer = startingPlayer;
        
        ReportBuffer.add(this, LocalText.getText("StartFormationRound",
                PublicCompany_CGR.NAME));
        ReportBuffer.add(this, LocalText.getText("StartingPlayer", 
                startingPlayer.getId()));

        guiHints.setCurrentRoundType(getClass());

        // Collect companies having loans
        for (PublicCompany company : setOperatingCompanies()) {
            if (company.getCurrentNumberOfLoans() > 0) {
                companiesToRepayLoans.put(company.getPresident(), company);
            }
        }

        if (companiesToRepayLoans.isEmpty()) {
            ReportBuffer.add(this, LocalText.getText("DoesNotForm", cgr.toText()));
            finishRound();
            return;
        }

        step.set(Steps.STEP_REPAY_LOANS);
        playerManager.setCurrentPlayer(startingPlayer);

        process (null);
    }

    private boolean setNextCompanyNeedingPresidentIntervention () {

        while (true) {

            while (!companiesToRepayLoans.containsKey(playerManager.getCurrentPlayer())) {
                playerManager.setCurrentToNextPlayer();
                if (playerManager.getCurrentPlayer().equals(startingPlayer)) {
                    return false;
                }
            }
            // select player and company to act and remove them from the list
            Player player = playerManager.getCurrentPlayer();
            PublicCompany company = companiesToRepayLoans.get(player).get(0);
            companiesToRepayLoans.remove(player, company);
            // set current company for further actions 
            currentCompany.set(company);

            int numberOfLoans = company.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) continue;

            int compCash = company.getCash();
            int presCash = player.getCash();
            int valuePerLoan = company.getValuePerLoan();

            String message = LocalText.getText("CompanyHasLoans",
                    currentCompany.value().getId(),
                    player.getId(),
                    numberOfLoans,
                    Bank.format(this, valuePerLoan),
                    Bank.format(this, numberOfLoans * valuePerLoan));
            ReportBuffer.add(this, " ");
            DisplayBuffer.add(this, " ", false);
            ReportBuffer.add(this, message);
            DisplayBuffer.add(this, message, false);

            // Let company repay all loans for which it has the cash
            int numberToRepay = Math.min(numberOfLoans,
                    compCash / valuePerLoan);
            if (numberToRepay > 0) {
                int payment = numberToRepay * valuePerLoan;
                String paymentText = Currency.toBank(company, payment);
                company.addLoans(-numberToRepay);

                message = LocalText.getText("CompanyRepaysLoans",
                        currentCompany.value().getId(),
                        paymentText,
                        Bank.format(this, numberOfLoans * valuePerLoan), 
                        numberToRepay,
                        Bank.format(this, valuePerLoan));
                ReportBuffer.add(this, message);
                DisplayBuffer.add(this, message, false);
            }

            // If that was all, we're done with this company
            numberOfLoans = company.getCurrentNumberOfLoans();
            if (numberOfLoans == 0) {
                continue;
            }

            // Check the president's cash
            // He should be involved if at least one extra loan could be repaid
            compCash = company.getCash();
            if ((compCash + presCash) / valuePerLoan > 0) {
                int maxNumber = Math.min((compCash + presCash)/valuePerLoan, numberOfLoans);
                if (maxNumber == numberOfLoans) {
                    DisplayBuffer.add(this, LocalText.getText("YouCanRepayAllLoans",
                            player.getId(),
                            maxNumber,
                            company.getId()),
                            false);
                } else {
                    DisplayBuffer.add(this, LocalText.getText("YouCannotRepayAllLoans",
                            player.getId(),
                            maxNumber,
                            numberOfLoans,
                            company.getId()),
                            false);
                    // FIXME: Rails 2.0 adapt LoanValue to be able to store this information
                    //                    currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
                }
                maxLoansToRepayByPresident = maxNumber;
                break;
            } else {
                // President cannot help, this company will merge into CGR anyway
                mergingCompanies.add(company);
                // FIXME: see above
                // currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
                message = LocalText.getText("WillMergeInto",
                        company.getId(),
                        PublicCompany_CGR.NAME);
                DisplayBuffer.add(this, message, false);
                ReportBuffer.add(this, message);
                continue;
            }
        }
        return true;
    }

    @Override
    public boolean setPossibleActions() {

        if (step.value() == Steps.STEP_REPAY_LOANS) {
            RepayLoans action = new RepayLoans (currentCompany.value(), 0,
                    maxLoansToRepayByPresident,
                    currentCompany.value().getValuePerLoan());
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        } else if (step.value() == Steps.STEP_EXCHANGE_TOKENS) {
            int numberToExchange = cgr.getNumberOfFreeBaseTokens();
            ExchangeTokens action = new ExchangeTokens (tokensToExchangeFrom,
                    numberToExchange, numberToExchange);
            action.setCompany(cgr);
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        } else if (step.value() == Steps.STEP_DISCARD_TRAINS) {
            DiscardTrain action = new DiscardTrain (cgr,
                    trainsToDiscardFrom, forcedTrainDiscard);
            possibleActions.add(action);
            guiHints.setActivePanel(GuiDef.Panel.STATUS);
        }
        return true;

    }

    protected boolean repayLoans (RepayLoans action) {
        // TODO Validation skipped for now...

        PublicCompany company = action.getCompany();
        int numberRepaid = action.getNumberRepaid();
        int repayment = numberRepaid * company.getValuePerLoan();

        if (repayment > 0) {

            int repaymentByCompany = Math.min (repayment, company.getCash());
            int repaymentByPresident = repayment - repaymentByCompany;

            company.addLoans(-numberRepaid);
            if (repaymentByCompany > 0) {
                String repayCompanyText = Currency.toBank(company, repaymentByCompany);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoans",
                        company.getId(),
                    repayCompanyText,
                        numberRepaid,
                    Bank.format(this, company.getValuePerLoan()))); // TODO: Make this nicer
            }
            if (repaymentByPresident > 0) {
                Player president = company.getPresident();
                String repayPresidentText =  Currency.toBank(president, repaymentByPresident);
                ReportBuffer.add(this, LocalText.getText("CompanyRepaysLoansWithPresCash",
                        company.getId(),
                        repayPresidentText,
                        Bank.format(this, repayment), 
                        numberRepaid,
                        Bank.format(this, company.getValuePerLoan()), 
                        president.getId()));
            }
        }

        if (company.getCurrentNumberOfLoans() > 0) {
            mergingCompanies.add(company);
            // FIXME: see above
            //            currentCompany.getLoanValueModel().setText(LocalText.getText("MERGE"));
            String message = LocalText.getText("WillMergeInto",
                    company.getId(),
                    PublicCompany_CGR.NAME);
            DisplayBuffer.add(this, message, true);
            ReportBuffer.add(this, message);

        }

        return true;

    }

    private void formCGR () {
        ReportBuffer.add(this, "");

        Player temporaryPresident = null;
        Player newPresident = null;
        Player firstCGRowner = null;
        int maxShares = 0;
        int cgrSharesUsed = 0;

        // Exchange the player shares
        for (Player player:playerManager.getNextPlayersAfter(startingPlayer, true, false)) {
            int oldShares = 0, newShares = 0;
            List<PublicCertificate> certs = Lists.newArrayList();
            PublicCertificate poolCert = null;

            // count number of shares for the players in oldShares
            log.debug(player.getPortfolioModel().getCertificates().toString());
            for (PublicCertificate cert : player.getPortfolioModel().getCertificates()) {
                if (mergingCompanies.contains(cert.getCompany())) {
                    log.debug("merge cert= " + cert);
                    certs.add((cert));
                    oldShares++;
                    if (cert.isPresidentShare()) {
                        oldShares++;
                    }
                }
            }

            if (oldShares > 0) {

                int count = oldShares;
                // no president assigned so far, assign president if there are enough oldShares
                if (count >= 4 && temporaryPresident == null && cgrSharesUsed <= 18) {
                    PublicCertificate cgrCert = cgr.getPresidentsShare();
                    cgrCert.moveTo(player);
                    count -= 4;
                    cgrSharesUsed += 2;
                    newShares += 2;
                    temporaryPresident = player;
                }
                
                // now convert the remaining shares
                while (count >= 2 && cgrSharesUsed <= 19) {
                    PublicCertificate cgrCert = unavailable.findCertificate(cgr, false);
                    cgrCert.moveTo(player);
                    count -= 2;
                    cgrSharesUsed++;
                    newShares++;
                }

                String message = LocalText.getText("HasMergedShares",
                        player.getId(),
                        oldShares,
                        newShares,
                        PublicCompany_CGR.NAME);
                DisplayBuffer.add(this, message, false);
                ReportBuffer.add(this, message);

                if (count == 1) {
                    // Should work OK even if this is a president's share.
                    // In the pool we will treat all certs equally.
                    poolCert = certs.get(certs.size()-1);
                    poolCert.moveTo(pool.getParent());
                    certs.remove(poolCert);

                    message = LocalText.getText("HasPutShareInPool",
                            player.getId());
                    DisplayBuffer.add(this, message, false);
                    ReportBuffer.add(this, message);

                }
                // Note: old shares are removed when company is closed

                if (firstCGRowner == null) firstCGRowner = player;

                // Check for presidency
                if (newShares > maxShares) {
                    maxShares = newShares;
                    newPresident = player;
                }
            }
        }

        // Exchange the pool shares
        int oldShares = 0, newShares = 0;
        List<PublicCertificate> certs = Lists.newArrayList();
        for (PublicCertificate cert : pool.getCertificates()) {
            if (mergingCompanies.contains(cert.getCompany())) {
                certs.add((cert));
                oldShares++;
            }
        }
        int count = oldShares;
        while (count >= 2 && cgrSharesUsed <= 19) {
            PublicCertificate cgrCert = unavailable.findCertificate(cgr, false);
            cgrCert.moveTo(pool.getParent());
            count -= 2;
            cgrSharesUsed++;
            newShares++;
        }

        String message = LocalText.getText("HasMergedShares",
                LocalText.getText("POOL"),
                oldShares,
                newShares,
                PublicCompany_CGR.NAME);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        Portfolio.moveAll(certs, scrapHeap.getParent());
        log.info(cgrSharesUsed+" CGR shares are now in play");

        // If no more than 10 shares are in play, the CGR share
        // unit becomes 10%; otherwise it stays 5%.
        if (cgrSharesUsed <=10) {
            cgr.setShareUnit (10);
            // All superfluous shares have been removed
        }
        message = LocalText.getText("CompanyHasShares",
                cgr.toText(), 100/cgr.getShareUnit(), cgr.getShareUnit());
        DisplayBuffer.add(this, " ");
        ReportBuffer.add(this, " ");
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        // Move the remaining CGR shares to the ipo.
        Portfolio.moveAll(unavailable.getCertificates(cgr), ipo.getParent());

        // Assign the new president
        if (newPresident.getPortfolioModel().getShare(cgr) == cgr.getShareUnit()) {
            // Nobody has 2 shares, then takes the first player who has got one share
            log.debug("Nobody has two shares, creating a temp.pres.: "+firstCGRowner.getId());
            cgr.setTemporaryPresident(firstCGRowner);
            newPresident = firstCGRowner;
        } else if (temporaryPresident != null && temporaryPresident != newPresident) {
            log.debug("Moving pres.share from "+temporaryPresident.getId()
                    +" to "+newPresident.getId());
            temporaryPresident.getPortfolioModel().swapPresidentCertificate(cgr,
                        newPresident.getPortfolioModel(), 1);
        }

        // TODO: What does the following command do? I assume only trigger an update, so I uncommented
        // newPresident.getPortfolio().getShareModel(cgr).setShare();
        message = LocalText.getText("IS_NOW_PRES_OF",
                newPresident.getId(), cgr.toText());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);

        // Determine the CGR starting price,
        // and close the absorbed companies.
        int lowestPrice = 999;
        int totalPrice = 0;
        int price;
        int numberMerged = mergingCompanies.size();
        for (PublicCompany comp : mergingCompanies) {
            price = comp.getMarketPrice();
            totalPrice += price;
            if (price < lowestPrice) lowestPrice = price;
        }
        if (numberMerged >= 3) {
            totalPrice -= lowestPrice;
            numberMerged--;
        }
        int cgrPrice = Math.max(100, (((totalPrice/numberMerged)/5))*5);

        // Find the correct start space and start the CGR
        if (cgrPrice == 100) {
            cgr.start(100);
        } else {
            int prevColPrice = 100;
            int colPrice;
            StockSpace startSpace;
            for (int col=6; col <= stockMarket.getNumberOfColumns(); col++) {
                colPrice = stockMarket.getStockSpace(0, col).getPrice();
                if (cgrPrice > colPrice) continue;
                if (cgrPrice - prevColPrice < colPrice - cgrPrice) {
                    startSpace = stockMarket.getStockSpace(0, col-1);
                } else {
                    startSpace = stockMarket.getStockSpace(0, col);
                }
                cgr.start(startSpace);
                message = LocalText.getText("START_MERGED_COMPANY",
                        PublicCompany_CGR.NAME,
                        Bank.format(this, startSpace.getPrice()),
                        startSpace.getId());
                DisplayBuffer.add(this, message);
                ReportBuffer.add(this, message);
                break;
            }
        }
        cgr.setFloated();
        ReportBuffer.add(this, LocalText.getText("Floats", PublicCompany_CGR.NAME));

        // Determine the new certificate limit.
        // The number of available companies is 11,
        // or 12 minus the number of closed companies, whichever is lower.
        //Make sure that only available companies are counted
        int validCompanies= 12; //including the CGR
        //Need to find out if a company is already closed, if yes
        //decrease the validCompany value by 1
        for(PublicCompany c : getRoot().getGameManager().getAllPublicCompanies()) {
            if (c.isClosed()) {
                validCompanies--;
            }
        }
        int numCompanies = Math.min(11, validCompanies-mergingCompanies.size());
        int numPlayers = playerManager.getNumberOfPlayers();
        // Need some checks here...
        int newCertLimit = certLimitsTable[numPlayers-2][numCompanies-4];
        getRoot().getPlayerManager().setPlayerCertificateLimit(newCertLimit);
        message = LocalText.getText("CertificateLimit",
                newCertLimit,
                numPlayers,
                numCompanies);
        DisplayBuffer.add(this, message);
        ReportBuffer.add(this, message);

        // Collect the old token spots, and move cash and trains
        List<BaseToken> homeTokens = new ArrayList<BaseToken>();
        nonHomeTokens = new ArrayList<BaseToken>();
        BaseToken bt;
        MapHex hex;
        Stop stop;
        for (PublicCompany comp : mergingCompanies) {

            // Exchange home tokens and collect non-home tokens
            List<MapHex> homeHexes = comp.getHomeHexes();
            for (BaseToken token :comp.getAllBaseTokens()) {
                bt = token;
                if (!bt.isPlaced()) continue;
                stop = (Stop) bt.getOwner();
                hex = stop.getParent();
                if (homeHexes != null && homeHexes.contains(hex)) {
                    homeTokens.add(bt);
                } else {
                    nonHomeTokens.add(bt);
                }
            }

            // Move any remaining cash
            if (comp.getCash() > 0) {
                Currency.wireAll(comp, cgr);
            }

            // Move any remaining trains
            Set<Train> trains = comp.getPortfolioModel().getTrainList();
            for (Train train : trains) {
                cgr.getPortfolioModel().addTrain(train);
                if (train.isPermanent()) cgr.setHadPermanentTrain(true);
            }

            // Move any still valid bonuses
            if (comp.getBonuses() != null) {
                List<Bonus> bonuses = new ArrayList<Bonus> (comp.getBonuses());
                bonuses:        for (Bonus bonus : bonuses) {
                    comp.removeBonus(bonus);
                    // Only add if the CGR does not already have the same bonus
                    if (cgr.getBonuses() != null) {
                        for (Bonus b : cgr.getBonuses()) {
                            if (b.equals(bonus)) {
                                // Remove this duplicate bonus token.
                                // Check if it should be made available again.
                                List<SellBonusToken> commonSP = gameManager.getSpecialProperties(SellBonusToken.class, true);
                                if (commonSP != null) {
                                    for (SellBonusToken sp : commonSP) {
                                        if (sp.getId().equalsIgnoreCase(b.getName())) {
                                            sp.makeResellable();
                                            log.debug("BonusToken "+b.getName()+" made sellable again");
                                            break;
                                        }
                                    }
                                }
                                log.debug("Duplicate BonusToken "+b.getName()+" not added to "+ cgr.getId());
                                continue bonuses;
                            }
                        }
                    }
                    cgr.addBonus(new Bonus(cgr, bonus.getName(), bonus.getValue(),
                            bonus.getLocations()));
                }
            }
        }

        // Replace the home tokens
        ReportBuffer.add(this, "");
        for (BaseToken token : homeTokens) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to home
            token.moveTo(token.getParent());
            if (hex.layBaseToken(cgr, stop)) {
                /* TODO: the false return value must be impossible. */
                ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                        cgr.toText(), token.getParent().getId(),
                        stop.getSpecificId()));
                cgr.layBaseToken(hex, 0);
            }
        }

        // Clean up any non-home tokens on cities now having a CGR token
        for (BaseToken token : new ArrayList<BaseToken>(nonHomeTokens)) {
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            Set<BaseToken> otherTokens = hex.getBaseTokens();
            if (otherTokens != null) {
                for (BaseToken token2 : otherTokens) {
                    if (token2.getParent() == cgr
                            || nonHomeTokens.contains(token2) && token2 != token) {
                        ReportBuffer.add(this, LocalText.getText("DiscardsBaseToken",
                                cgr.toText(), token.getParent().getId(),
                                stop.getSpecificId()));
                        // return token to home
                        token.moveTo(token.getParent());
                        nonHomeTokens.remove(token);
                        break;
                    }
                }
            }
        }

        // Prepare replacing the other tokens, if possible
        if (homeTokens.size() + nonHomeTokens.size() > cgr.getNumberOfBaseTokens()) {
            // CGR cannot replace all tokens, must choose
            // First collect old names per city
            Map<String, String> oldTokens = new HashMap<String, String>();
            String cityName;
            for (BaseToken token : nonHomeTokens) {
                if (token.getOwner() instanceof Stop) {
                    cityName = ((Stop)token.getOwner()).getSpecificId();
                    if (oldTokens.containsKey(cityName)) {
                        oldTokens.put(cityName,
                                oldTokens.get(cityName)+","+token.getParent().getId());
                    } else {
                        oldTokens.put(cityName, token.getParent().getId());
                    }
                }
            }
            // Then create list of exchange spots. Sort it on hexname/city number
            tokensToExchangeFrom = new ArrayList<ExchangeableToken>();
            for (String key : new TreeSet<String> (oldTokens.keySet())) {
                tokensToExchangeFrom.add(new ExchangeableToken(
                        key, oldTokens.get(key)));
            }
        } else {
            executeExchangeTokens(nonHomeTokens);
        }

        // Close the merged companies
        for (PublicCompany comp : mergingCompanies) {
            comp.setClosed();
        }

        // Check the trains, autodiscard any excess non-permanent trains
        //        int trainLimit = cgr.getTrainLimit(gameManager.getCurrentPlayerIndex());
        int trainLimit = cgr.getCurrentTrainLimit();
        Set<Train> trains = cgr.getPortfolioModel().getTrainList();
        if (cgr.getNumberOfTrains() > trainLimit) {
            ReportBuffer.add(this, "");
            int numberToDiscard = cgr.getNumberOfTrains() - trainLimit;
            List<Train> trainsToDiscard = new ArrayList<Train>(4);
            for (Train train : trains) {
                if (!train.isPermanent()) {
                    trainsToDiscard.add(train);
                    if (--numberToDiscard == 0) break;
                }
            }
            for (Train train : trainsToDiscard) {
                train.discard();
            }
        }

    }

    private void executeExchangeTokens (List<BaseToken> exchangedTokens) {
        Stop stop;
        MapHex hex;
        ReportBuffer.add(this, "");
        for (BaseToken token : exchangedTokens) {
            // Remove old token
            stop = (Stop) token.getOwner();
            hex = stop.getParent();
            // return token to Company
            token.moveTo(token.getParent());
            // Replace it with a CGR token
            if (hex.layBaseToken(cgr, stop)) {
                cgr.layBaseToken(hex, 0);
            } else {
                log.error("Error in laying CGR token on "+hex.getId()+" "+hex.getStopName());
            }
        }
    }

    @Override
    public boolean process (PossibleAction action) {

        boolean result = true;

        if (action instanceof RepayLoans) {
            result = repayLoans((RepayLoans)action);
        } else if (action instanceof DiscardTrain) {
            result = discardTrain((DiscardTrain)action);
        } else if (action instanceof ExchangeTokens) {
            result = exchangeTokens((ExchangeTokens)action, true); // 2nd parameter: linked moveset
        }
        if (!result) return false;

        if (step.value() == Steps.STEP_REPAY_LOANS) {

            if (setNextCompanyNeedingPresidentIntervention()) {
                return true;
            }

            if (!mergingCompanies.isEmpty()) {
                formCGR();
                step.set(Steps.STEP_EXCHANGE_TOKENS);
            } else {
                finishRound();
            }
        }

        if (step.value() == Steps.STEP_EXCHANGE_TOKENS) {

            if (action instanceof ExchangeTokens) {
                tokensToExchangeFrom = null;
            } else if (tokensToExchangeFrom != null
                    && !tokensToExchangeFrom.isEmpty()) {
                return true;
            }
            step.set(Steps.STEP_DISCARD_TRAINS);
        }

        if (step.value() == Steps.STEP_DISCARD_TRAINS) {

            if (checkForTrainsToDiscard()) return true;
            finishRound();
        }

        return true;
    }


    private boolean checkForTrainsToDiscard () {

        // Check if CGR must discard trains
        if (cgr.getNumberOfTrains() > cgr.getCurrentTrainLimit()) {
            log.debug("CGR must discard trains");
            trainsToDiscardFrom = cgr.getPortfolioModel().getTrainList();
            forcedTrainDiscard = true;
            return true;
        } else if (!this.cgrHasDiscardedTrains.value()) {
            // Check if CGR still has non-permanent trains
            // these may be discarded voluntarily
            trainsToDiscardFrom = new HashSet<Train>();
            for (Train train : cgr.getPortfolioModel().getTrainList()) {
                if (!train.isPermanent()) {
                    trainsToDiscardFrom.add(train);
                }
            }
            if (!trainsToDiscardFrom.isEmpty()) {
                forcedTrainDiscard = false;
                return true;
            }
        }
        return false;
    }

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be CGR
            if (company != cgr) {
                errMsg = LocalText.getText("WrongCompany",
                        company.getId(),
                        cgr.toText());
                break;
            }
            // Must be correct step
            if (step.value() != Steps.STEP_DISCARD_TRAINS) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (train != null && !company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                    LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.toText() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.toText(),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // new: link always, see below commented
        
        if (train != null) {
            train.discard();
        } else {
            cgrHasDiscardedTrains.set(true);
        }

        return true;
    }

    protected boolean exchangeTokens(ExchangeTokens action, boolean linkedMoveSet) {

        String errMsg = null;

        List<ExchangeableToken> tokens = action.getTokensToExchange();
        int min = action.getMinNumberToExchange();
        int max = action.getMaxNumberToExchange();
        int exchanged = 0;

        checks: {

            for (ExchangeableToken token : tokens) {
                if (token.isSelected()) exchanged++;
            }
            if (exchanged < min || exchanged > max) {
                errMsg = LocalText.getText("WrongNumberOfTokensExchanged",
                        action.getCompany(),
                        min, max, exchanged);
                break checks;
            }
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotExchangeTokens",
                    action.getCompany(),
                    action.toString(),
                    errMsg));

            return false;
        }

        
        // FIMXE: if (linkedMoveSet) changeStack.linkToPreviousMoveSet();

        if (exchanged > 0) {
            MapHex hex;
            Stop stop;
            String stopName, hexName;
            int stationNumber;
            String[] ct;
            PublicCompany comp = action.getCompany();

            ReportBuffer.add(this, "");

            for (ExchangeableToken token : tokens) {
                stopName = token.getCityName();
                ct = stopName.split("/");
                hexName = ct[0];
                try {
                    stationNumber = Integer.parseInt(ct[1]);
                } catch (NumberFormatException e) {
                    stationNumber = 1;
                }
                hex = mapManager.getHex(hexName);
                stop = hex.getRelatedStop(stationNumber);

                if (token.isSelected()) {

                    // For now we'll assume that the old token(s) have already been removed.
                    // This is true in the 1856 CGR formation.
                    if (hex.layBaseToken(comp, stop)) {
                        /* TODO: the false return value must be impossible. */
                        ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                                comp.getId(),
                                token.getOldCompanyName(),
                                stop.getSpecificId()));
                        comp.layBaseToken(hex, 0);
                    }
                } else {
                    ReportBuffer.add(this, LocalText.getText("NoBaseTokenExchange",
                            comp.getId(),
                            token.getOldCompanyName(),
                            stop.getSpecificId()));
                }
            }
        }

        return true;
    }

    
    public List<PublicCompany> getMergingCompanies() {
        return mergingCompanies.view();
    }

    @Override
    public String toString() {
        return "1856 CGRFormationRound";
    }

    // Step Objects to control progress
    private static enum Steps {STEP_REPAY_LOANS, STEP_DISCARD_TRAINS, STEP_EXCHANGE_TOKENS };  
    
}
