/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TreasuryShareRound.java,v 1.1 2008/01/21 22:57:29 evos Exp $
 * 
 * Created on 21-May-2006
 * Change Log:
 */
package rails.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rails.game.action.BuyCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.game.move.MoveSet;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.LocalText;


/**
 * @author Erik Vos
 */
public class TreasuryShareRound extends StockRound {
    
    Player sellingPlayer;
    PublicCompanyI operatingCompany;
    GameManager gameMgr;
    private BooleanState hasBought;
    private BooleanState hasSold;
    
    public TreasuryShareRound (PublicCompanyI operatingCompany) {
        
        this.operatingCompany = operatingCompany;
        sellingPlayer = operatingCompany.getPresident();
        log.debug("Creating TreasuryShareRound");
        hasBought = new BooleanState (operatingCompany.getName()+"_boughtShares", false);
        hasSold = new BooleanState (operatingCompany.getName()+"_soldShares", false);

        gameMgr = GameManager.getInstance();
        gameMgr.setRound(this);
		GameManager.setCurrentPlayerIndex(sellingPlayer.getIndex());
		
}
    
    public void start() {
        log.info ("Treasury share trading round started");
        currentPlayer = sellingPlayer;
    }
    
 	public boolean mayCurrentPlayerSellAnything() {
	    return false;
	}
	
	public boolean mayCurrentPlayerBuyAnything() {
	    return false;
	}

	public boolean setPossibleActions() {
		
		possibleActions.clear();
		
		if (operatingCompany.hasOperated()) return true;
		
		if (!hasSold.booleanValue()) setBuyableCerts();
		if (!hasBought.booleanValue()) setSellableCerts();
		
		for (PossibleAction pa : possibleActions.getList()) {
			log.debug(operatingCompany.getName()+ " may: "+pa.toString());
		}
		
		return true;
	}

    /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     * 
     * @return List of buyable certificates.
     */
    public void setBuyableCerts()
    {
        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        StockSpaceI stockSpace;
        Portfolio from;
        int price;
        int number;

        int cash = operatingCompany.getCash();

        /* Get the unique Pool certificates and check which ones can be bought */
        from = Bank.getPool();
        Map<String, List<PublicCertificateI>> map 
            = from.getCertsPerCompanyMap();

        for (String compName : map.keySet())
        {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty())
                continue;
            
            cert = certs.get(0);
            comp = cert.getCompany();
            
            // TODO For now, only consider own certificates.
            // This will have to be revisited with 1841.
            if (comp != operatingCompany) continue;

            // Shares already owned
            int ownedShare = operatingCompany.getPortfolio().getShare(operatingCompany);
            // Max share that may be owned
            int maxShare = gameMgr.getTreasuryShareLimit();
            // Max number of shares to add
            int maxBuyable = (maxShare - ownedShare) / operatingCompany.getShareUnit();
            // Max number of shares to buy
            number = Math.min(certs.size(), maxBuyable);
            if (number == 0) continue;

            stockSpace = comp.getCurrentPrice(); 
            price = stockSpace.getPrice();
            
            // Does the company have enough cash?
            while (number > 0 && cash < number * price) number--;

            if (number > 0) {
                possibleActions.add(new BuyCertificate (cert, from, price, number));
            }
        }
        
    }

	/**
	 * Create a list of certificates that the company may sell,
	 * taking all rules taken into account.
	 * <br>Note: old code that provides for ownership of presidencies
	 * of other companies has been retained, but not tested.
	 * This code will be needed for 1841.
	 * @return List of sellable certificates.
	 */
	public void setSellableCerts()
	{
		String compName;
		int price;
		int number;
		int share, maxShareToSell;
		boolean dumpAllowed;
		Portfolio companyPortfolio = operatingCompany.getPortfolio();
		
		/* First check of which companies the player owns stock,
		 * and what maximum percentage he is allowed to sell.
		 */ 
		for (PublicCompanyI company : companyMgr.getAllPublicCompanies()) {
			
			// Can't sell shares that have no price
			if (!company.hasStarted()) continue;
			
			share = maxShareToSell = companyPortfolio.getShare(company);
			if (maxShareToSell == 0) continue;

			/* May not sell more than the Pool can accept */
			maxShareToSell = Math.min(maxShareToSell, Bank.getPoolShareLimit() - pool.getShare(company));
			if (maxShareToSell == 0) continue;
			
			// If the current Player is president, check if he can dump
			// the presidency onto someone else
			/* DISABLED, companies cannot yet have another company as a President.
			 * We will need this code later for 1841, so it might make sense to retain it.
			if (company.getPresident() == currentPlayer) {
				int presidentShare = company.getCertificates().get(0).getShare();
				if (maxShareToSell > share - presidentShare) {
					dumpAllowed = false;
                    if (company != operatingCompany) {
     					int playerShare;
    					List<Player> players = GameManager.getPlayers();
    					for (Player player : players)
    					{
    						if (player == currentPlayer) continue;
    						playerShare = player.getPortfolio().getShare(company);
    						if (playerShare	>= presidentShare)
    						{
    							dumpAllowed = true;
    							break;
    						}
    					}
                    }
					if (!dumpAllowed) maxShareToSell = share - presidentShare;
				}
			}
			*/
			
			/* Check what share units the player actually owns.
			 * In some games (e.g. 1835) companies may have different 
			 * ordinary shares: 5% and 10%, or 10% and 20%.
			 * The president's share counts as a multiple of the lowest
			 * ordinary share unit type.
			 */
			// Take care for max. 4 share units per share
			int[] shareCountPerUnit = new int[5]; 
			compName = company.getName();
			for (PublicCertificateI c : companyPortfolio.getCertificatesPerCompany(compName)) {
				if (c.isPresidentShare()) {
					shareCountPerUnit[1] += c.getShares();
				} else {
					++shareCountPerUnit[c.getShares()];
				}
			}
			// TODO The above ignores that a dumped player must be
			// able to exchange the president's share.
			
			/* Check the price.
			 * If a cert was sold before this turn,
			 * the original price is still valid */
			if (sellPrices.containsKey(compName))
			{
				price = ((StockSpaceI) sellPrices.get(compName)).getPrice();
			} else {
				price = company.getCurrentPrice().getPrice();
			}

			
			for (int i=1; i<=4; i++) {
				number = shareCountPerUnit[i];
				if (number == 0) continue;
				number = Math.min (number, 
						maxShareToSell / (i * company.getShareUnit()));
				if (number == 0) continue;
				
				if (number > 0) {
                    possibleActions.add (new SellShares (compName,
						i, number, price));
                }
			}
		}
	}

	public boolean sellShares (SellShares action)
	{
		Portfolio portfolio = currentPlayer.getPortfolio();
		String playerName = currentPlayer.getName();
		String errMsg = null;
		String companyName = action.getCompanyName();
		PublicCompanyI company = companyMgr.getPublicCompany(action.getCompanyName());
		PublicCertificateI cert = null;
		PublicCertificateI presCert = null;
		List<PublicCertificateI> certsToSell 
			= new ArrayList<PublicCertificateI>();
		Player dumpedPlayer = null;
		int presSharesToSell = 0;
		int numberToSell = action.getNumberSold();
		int shareUnits = action.getShareUnits();
		int currentIndex = GameManager.getCurrentPlayerIndex();

		// Dummy loop to allow a quick jump out
		while (true)
		{

			// Check everything
			if (numberToSell <= 0)
			{
				errMsg = LocalText.getText("NoSellZero");
				break;
			}

			// Check company
			if (company == null)
			{
				errMsg = LocalText.getText("NoCompany");
				break;
			}

			// The player must have the share(s)
			if (portfolio.getShare(company) < numberToSell)
			{
				errMsg = LocalText.getText("NoShareOwned");
				break;
			}

			// The pool may not get over its limit.
			if (pool.getShare(company) + numberToSell * company.getShareUnit() > Bank.getPoolShareLimit())
			{
				errMsg = LocalText.getText("PoolOverHoldLimit");
				break;
			}

			// Find the certificates to sell
			Iterator<PublicCertificateI> it = portfolio.getCertificatesPerCompany(companyName)
					.iterator();
			while (numberToSell > 0 && it.hasNext())
			{
				cert = it.next();
				if (cert.isPresidentShare())
				{
					// Remember the president's certificate in case we need it
					if (cert.isPresidentShare())
						presCert = cert;
					continue;
				}
				else if (shareUnits != cert.getShares())
				{
					// Wrong number of share units
					continue;
				}
				// OK, we will sell this one
				certsToSell.add(cert);
				numberToSell--;
			}
			if (numberToSell == 0)
				presCert = null;

			if (numberToSell > 0 && presCert != null
					&& numberToSell <= presCert.getShares())
			{
				// Not allowed to dump the company that needs the train
				if (company == operatingCompany)
				{
				    errMsg = LocalText.getText("CannotDumpTrainBuyingPresidency");
				    break;
				}
				// More to sell and we are President: see if we can dump it.
				Player otherPlayer;
				for (int i = currentIndex + 1; i < currentIndex
						+ numberOfPlayers; i++)
				{
					otherPlayer = GameManager.getPlayer(i);
					if (otherPlayer.getPortfolio().getShare(company) >= presCert.getShare())
					{
						// Check if he has the right kind of share
						if (numberToSell > 1
								|| otherPlayer.getPortfolio()
										.ownsCertificates(company, 1, false) >= 1)
						{
							// The poor sod.
							dumpedPlayer = otherPlayer;
							presSharesToSell = numberToSell;
							numberToSell = 0;
							break;
						}
					}
				}
			}
			// Check if we could sell them all
			if (numberToSell > 0)
			{
				if (presCert != null)
				{
					errMsg = LocalText.getText("NoDumping");
				}
				else
				{
					errMsg = LocalText.getText("NotEnoughShares");
				}
				break;
			}

			break;
		}

		int numberSold = action.getNumberSold();
		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("CantSell", new String[] {
					playerName,
					String.valueOf(numberSold),
					companyName,
					errMsg
					}));
			return false;
		}

		// All seems OK, now do the selling.
		StockSpaceI sellPrice;
		int price;

		// Get the sell price (does not change within a turn)
		if (sellPrices.containsKey(companyName))
		{
			price = ((StockSpaceI) sellPrices.get(companyName)).getPrice();
		}
		else
		{
			sellPrice = company.getCurrentPrice();
			price = sellPrice.getPrice();
			sellPrices.put(companyName, sellPrice);
		}

		MoveSet.start(true);

		ReportBuffer.add (LocalText.getText("SELL_SHARES_LOG", new String[]{
		        playerName,
		        String.valueOf(numberSold),
		        String.valueOf((numberSold * company.getShareUnit())),
		        companyName,
		        Bank.format(numberSold * price)}));

		// Check if the presidency has changed
		if (presCert != null && dumpedPlayer != null && presSharesToSell > 0)
		{
			ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF", new String[] {
					        dumpedPlayer.getName(),
					        companyName
					}));
			// First swap the certificates
			Portfolio dumpedPortfolio = dumpedPlayer.getPortfolio();
			List<PublicCertificateI> swapped = portfolio.swapPresidentCertificate(company,
					dumpedPortfolio);
			for (int i = 0; i < presSharesToSell; i++)
			{
				certsToSell.add(swapped.get(i));
			}
		}

		// Transfer the sold certificates
		for (PublicCertificateI cert2 : certsToSell)
		{
			if (cert2 != null)
				pool.buyCertificate(cert2, portfolio, cert2.getShares() * price);
		}
		stockMarket.sell(company, numberSold);

		// Check if we still have the presidency
		if (currentPlayer == company.getPresident())
		{
			Player otherPlayer;
			for (int i = currentIndex + 1; i < currentIndex + numberOfPlayers; i++)
			{
				otherPlayer = GameManager.getPlayer(i);
				if (otherPlayer.getPortfolio().getShare(company) > portfolio.getShare(company))
				{
					portfolio.swapPresidentCertificate(company,
							otherPlayer.getPortfolio());
					ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF", new String[]{
									otherPlayer.getName(),
									company.getName()
							}));
					break;
				}
			}
		}

		return true;
	}

    public String toString() {
        return "TreasuryShareRound";
    }

}
