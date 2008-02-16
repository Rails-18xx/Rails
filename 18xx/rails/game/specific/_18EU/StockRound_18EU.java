package rails.game.specific._18EU;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rails.game.Bank;
import rails.game.DisplayBuffer;
import rails.game.GameManager;
import rails.game.PhaseManager;
import rails.game.PhaseManagerI;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.StockRound;
import rails.game.StockSpaceI;
import rails.game.action.BuyCertificate;
import rails.game.action.StartCompany;
import rails.game.move.MoveSet;
import rails.util.LocalText;
import rails.util.Util;

/**
 * Implements a basic Stock Round.
 * <p>
 * A new instance must be created for each new Stock Round. At the end of a
 * round, the current instance should be discarded.
 * <p>
 * Permanent memory is formed by static attributes (like who has the Priority
 * Deal).
 */
public class StockRound_18EU extends StockRound
{

   /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     *
     * @return List of buyable certificates.
     */
    @Override
    public void setBuyableCerts()
    {
        if (!mayCurrentPlayerBuyAnything())
            return;

        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        StockSpaceI stockSpace;
        Portfolio from;
        int price;
        int number;

        int playerCash = currentPlayer.getCash();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompanyI companyBoughtThisTurn =
            (PublicCompanyI) companyBoughtThisTurnWrapper.getObject();
        if (companyBoughtThisTurn == null)
        {
            from = Bank.getIpo();
            Map<String, List<PublicCertificateI>> map
                = from.getCertsPerCompanyMap();
            int shares;

            for (String compName : map.keySet())
            {
                certs = map.get(compName);
                if (certs == null || certs.isEmpty())
                    continue;
                /* Only the top certificate is buyable from the IPO */
                cert = certs.get(0);
                comp = cert.getCompany();
                if (isSaleRecorded(currentPlayer, comp))
                    continue;
                if (currentPlayer.maxAllowedNumberOfSharesToBuy
                        (comp, cert.getShare()) < 1)
                    continue;
                shares = cert.getShares();

                if (!comp.hasStarted())
                {
                    // 18EU special: until phase 5, we can only
                    // start a company by trading in a Minor

                    // Check for phase to be added
                    PhaseManagerI pmgr = PhaseManager.getInstance();
                    boolean mustMergeMinor = pmgr.getCurrentPhase().getIndex()
                            < PhaseManager.getPhaseNyName("5").getIndex();

                    List<PublicCompanyI> minors = new ArrayList<PublicCompanyI>();
                    if (mustMergeMinor) {
                        for (PublicCertificateI c : getCurrentPlayer().getPortfolio().getCertificates()) {
                            if (c.getCompany().getTypeName().equalsIgnoreCase("Minor")) {
                                minors.add (c.getCompany());
                            }
                        }
                        if (minors.isEmpty()) continue;
                    }

                    List<Integer> startPrices = new ArrayList<Integer>();
                    for (int startPrice : stockMarket.getStartPrices())
                    {
                        if (startPrice * shares <= playerCash)
                        {
                            startPrices.add(startPrice);
                        }
                    }
                    if (startPrices.size() > 0) {
                        int[] prices = new int[startPrices.size()];
                        Arrays.sort (prices);
                        for (int i=0; i<prices.length; i++) {
                            prices[i] = startPrices.get(i);
                        }
                        possibleActions.add(new StartCompany_18EU (cert, prices, minors));
                    }
                }
                else if (comp.hasParPrice())
                {
                    price = comp.getParPrice().getPrice() * cert.getShares();
                    if (price <= playerCash) {
                        possibleActions.add (new BuyCertificate (cert, from, price));
                    }
                }
                else if (cert.getCertificatePrice() <= playerCash) {
                        possibleActions.add(new BuyCertificate (cert, from));
                }

            }
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = Bank.getPool();
        Map<String, List<PublicCertificateI>> map
            = from.getCertsPerCompanyMap();

        for (String compName : map.keySet())
        {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty())
                continue;
            number = certs.size();
            cert = certs.get(0);
            comp = cert.getCompany();
            if (isSaleRecorded(currentPlayer, comp))
                continue;
            if (currentPlayer.maxAllowedNumberOfSharesToBuy
                    (comp, cert.getShare()) < 1)
                continue;
            stockSpace = comp.getCurrentPrice();
            price = stockSpace.getPrice();

            if (companyBoughtThisTurn != null)
            {
                // If a cert was bought before, only brown zone ones can be
                // bought again in the same turn
                if (comp != companyBoughtThisTurn)
                    continue;
                if (!stockSpace.isNoBuyLimit())
                    continue;
            }
            /* Only certs in the brown zone may be bought all at once */
            if (!stockSpace.isNoBuyLimit()) {
                number = 1;
                /* Would the player exceed the per-company share hold limit? */
                if (!currentPlayer.mayBuyCompanyShare(comp, number))
                    continue;

                /* Would the player exceed the total certificate limit? */
                if (!stockSpace.isNoCertLimit()
                        && !currentPlayer.mayBuyCertificate(comp, number))
                continue;
            }

            // Does the player have enough cash?
            while (number > 0 && playerCash < number * price) number--;

            if (number > 0) {
                possibleActions.add(new BuyCertificate (cert, from, price, number));
            }
        }

        // Get any shares in company treasuries that can be bought
        if (GameManager.canAnyCompanyHoldShares()) {

            for (PublicCompanyI company : companyMgr.getAllPublicCompanies()) {
                certs = company.getPortfolio().getCertificatesPerCompany(company.getName());
                if (certs == null || certs.isEmpty()) continue;
                cert = certs.get(0);
                if (isSaleRecorded(currentPlayer, company))
                    continue;
                if (!currentPlayer.mayBuyCompanyShare(company, 1))
                    continue;
                if (currentPlayer.maxAllowedNumberOfSharesToBuy
                        (company, certs.get(0).getShare()) < 1)
                    continue;
                stockSpace = company.getCurrentPrice();
                if (!stockSpace.isNoCertLimit()
                        && !currentPlayer.mayBuyCertificate(company, 1))
                continue;
                if (cert.getCertificatePrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate (cert, company.getPortfolio()));
                }
            }
        }
    }


	/**
	 * Start a company by buying one or more shares (more applies to e.g. 1841)
	 *
	 * @param player
	 *            The player that wants to start a company.
	 * @param company
	 *            The company to start.
	 * @param price
	 *            The start (par) price (ignored if the price is fixed).
	 * @param shares
	 *            The number of shares to buy (can be more than 1 in e.g. 1841).
	 * @return True if the company could be started. False indicates an error.
	 */
	@Override
    public boolean startCompany(String playerName,
            StartCompany action)
	{
        PublicCompanyI company = action.getCertificate().getCompany();
        int price = action.getPrice();
        int shares = action.getNumberBought();

		String errMsg = null;
		StockSpaceI startSpace = null;
		int numberOfCertsToBuy = 0;
		PublicCertificateI cert = null;
		String companyName = company.getName();
		PublicCompanyI minor = null;
		StartCompany_18EU startAction = null;

		currentPlayer = GameManager.getCurrentPlayer();

		// Dummy loop to allow a quick jump out
		while (true)
		{
		    if (!(action instanceof StartCompany_18EU)) {
		        errMsg = LocalText.getText("InvalidAction");
		        break;
		    }
		    startAction = (StartCompany_18EU) action;

			// The player may not have bought this turn.
			if (companyBoughtThisTurnWrapper.getObject() != null)
			{
				errMsg = LocalText.getText("AlreadyBought", playerName);
				break;
			}

			// Check company
			company = companyMgr.getPublicCompany(companyName);
			if (company == null)
			{
				errMsg = LocalText.getText("CompanyDoesNotExist", companyName);
				break;
			}
			// The company may not have started yet.
			if (company.hasStarted())
			{
				errMsg = LocalText.getText("CompanyAlreadyStarted", companyName);
				break;
			}

			// Find the President's certificate
			cert = ipo.findCertificate(company, true);
			// Make sure that we buy at least one!
			if (shares < cert.getShares())
				shares = cert.getShares();

			// Determine the number of Certificates to buy
			// (shortcut: assume that any additional certs are one share each)
			numberOfCertsToBuy = shares - (cert.getShares() - 1);
			// Check if the player may buy that many certificates.
			if (!currentPlayer.mayBuyCertificate(company, numberOfCertsToBuy))
			{
				errMsg = LocalText.getText("CantBuyMoreCerts");
				break;
			}

			// Check if the company has a fixed par price (1835).
			startSpace = company.getParPrice();
			if (startSpace != null)
			{
				// If so, it overrides whatever is given.
				price = startSpace.getPrice();
			}
			else
			{
				// Else the given price must be a valid start price
				if ((startSpace = stockMarket.getStartSpace(price)) == null)
				{
					errMsg = LocalText.getText("InvalidStartPrice", new String[] {
							Bank.format(price),
							company.getName()
					});
					break;
				}
			}

			// Check if the Player has the money.
			if (currentPlayer.getCash() < shares * price)
			{
				errMsg = LocalText.getText("NoMoney");
				break;
			}

			// Check if the player owns the merged minor
			minor = startAction.getChosenMinor();
			if (currentPlayer.getPortfolio().getCertificatesPerCompany(minor.getName())== null) {
			    errMsg = LocalText.getText("PlayerDoesNotOwn",
			            new String[] {
			                currentPlayer.getName(),
			                minor.getName()
			    });
			    break;
			}
			numberOfCertsToBuy++;

			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("CantStart", new String[] {
			        playerName,
			        companyName,
			        Bank.format(price),
			        errMsg}));
			return false;
		}

		MoveSet.start(true);

		// All is OK, now start the company
		company.setHomeHex(minor.getHomeHex());
		company.start(startSpace);

        // TODO must get obtained from XML
        int tokensCost = 100;

        // Transfer the President's certificate
		//currentPlayer.getPortfolio().buyCertificate(cert,
		//		ipo,
		//		cert.getCertificatePrice());
		cert.moveTo(currentPlayer.getPortfolio());

		Bank.transferCash(currentPlayer, company, shares * price);
		Bank.transferCash(company, null, tokensCost);

		// Get the extra certificate for the minor, for free
		PublicCertificateI cert2 = ipo.findCertificate(company, false);
		cert2.moveTo(currentPlayer.getPortfolio());

		// Move the remaining certificates to the company treasury
		//for (PublicCertificateI cert3 : ipo.getCertificatesPerCompany(company.getName())) {
		//    cert3.moveTo(company.getPortfolio());
		//}
		Util.moveObjects(ipo.getCertificatesPerCompany(company.getName()),
		        company.getPortfolio());

		// Transfer the minor assets into the started company
        int minorCash = minor.getCash();
        int minorTrains = minor.getPortfolio().getTrainList().size();
		company.transferAssetsFrom (minor);

		minor.setClosed();

		// TODO: Must check for excess trains (though impossible here)

		ReportBuffer.add(LocalText.getText ("START_COMPANY_LOG", new String[] {
		        playerName,
		        companyName,
		        Bank.format(price),
                Bank.format (shares * price),
		        String.valueOf(shares),
		        String.valueOf(cert.getShare()),
		        company.getName()
		        }));
		ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG",
		        new String[] {
		            currentPlayer.getName(),
		            minor.getName(),
		            company.getName(),
		            Bank.format(minorCash),
		            String.valueOf(minorTrains)
		        }));
		ReportBuffer.add(LocalText.getText("GetShareForMinor",
		        new String[] {
		            currentPlayer.getName(),
		            String.valueOf(cert2.getShare()),
		            company.getName(),
		            minor.getName()
		        }));
		ReportBuffer.add(LocalText.getText("SharesPutInTreasury",
		        new String[] {
		            String.valueOf(company.getPortfolio().getShare(company)),
		            company.getName()
		        }));
		ReportBuffer.add(LocalText.getText("PaysForTokens",
		        new String[] {
		            company.getName(),
		            Bank.format(100),
		            String.valueOf(company.getNumberOfBaseTokens())
		        }));

		companyBoughtThisTurnWrapper.set(company);
		hasActed.set (true);
		setPriority();

		return true;
	}
	
	protected void checkFlotation (PublicCompanyI company) {
		company.checkFlotation(false);
	}

    @Override
    public String toString () {
        return "StockRound_18EU "+getStockRoundNumber();
    }
}
