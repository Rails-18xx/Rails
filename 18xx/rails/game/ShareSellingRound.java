/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ShareSellingRound.java,v 1.7 2007/09/30 12:55:18 evos Exp $
 * 
 * Created on 21-May-2006
 * Change Log:
 */
package rails.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.util.LocalText;


/**
 * @author Erik Vos
 */
public class ShareSellingRound extends StockRound {
    
    Player sellingPlayer;
    PublicCompanyI companyNeedingTrain;
    IntegerState cashToRaise;
    
    public ShareSellingRound (PublicCompanyI compNeedingTrain, int cashToRaise) {
        
        this.companyNeedingTrain = compNeedingTrain;
        this.cashToRaise = new IntegerState ("CashToRaise", cashToRaise);
        sellingPlayer = compNeedingTrain.getPresident();
        currentPlayer = sellingPlayer;
log.debug("Creating ShareSellingRound, cash to raise ="+cashToRaise);
		GameManager.getInstance().setRound(this);
		GameManager.setCurrentPlayerIndex(sellingPlayer.getIndex());
		
}
    
    public void start() {
        log.info ("Share selling round started");
        currentPlayer = sellingPlayer;
		//setPossibleActions();
    }
    
    /*
    public boolean isCompanyBuyable(String companyName, Portfolio source) {
        return false;
    }
    
    public boolean isCompanySellable(String companyName) {
        return true;
    }
    */
    
	public boolean mayCurrentPlayerSellAnything() {
	    return true;
	}
	
	public boolean mayCurrentPlayerBuyAnything() {
	    return false;
	}

	public boolean setPossibleActions() {
		
		possibleActions.clear();
		
		setSellableShares();
		
	    if (possibleActions.isEmpty() && cashToRaise.intValue() > 0) {
			DisplayBuffer.add (LocalText.getText("YouAreBankrupt", 
					Bank.format(cashToRaise.intValue())));

	        GameManager.getInstance().registerBankruptcy();
	        return false;
	    }
		
		for (PossibleAction pa : possibleActions.getList()) {
			log.debug(currentPlayer.getName()+ " may: "+pa.toString());
		}
		
		return true;
	}

	/**
	 * Create a list of certificates that a player may sell in a Stock Round,
	 * taking all rules taken into account.
	 * 
	 * @return List of sellable certificates.
	 */
	public void setSellableShares()
	{
		String compName;
		int price;
		int number;
		int share, maxShareToSell;
		boolean dumpAllowed;
		Portfolio playerPortfolio = currentPlayer.getPortfolio();
		
		/* First check of which companies the player owns stock,
		 * and what maximum percentage he is allowed to sell.
		 */ 
		for (PublicCompanyI company : companyMgr.getAllPublicCompanies()) {
			
			// Can't sell shares that have no price
			if (!company.hasStarted()) continue;
			
			share = maxShareToSell = playerPortfolio.getShare(company);
			if (maxShareToSell == 0) continue;

			/* May not sell more than the Pool can accept */
			maxShareToSell = Math.min(maxShareToSell, Bank.getPoolShareLimit() - pool.getShare(company));
			if (maxShareToSell == 0) continue;
			
			/* If the current Player is president, check if he can dump
			 * the presidency onto someone else */
			if (company.getPresident() == currentPlayer) {
				int presidentShare = company.getCertificates().get(0).getShare();
				if (maxShareToSell > share - presidentShare) {
					dumpAllowed = false;
                    if (company != companyNeedingTrain) {
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
			
			/* Check what share units the player actually owns.
			 * In some games (e.g. 1835) companies may have different 
			 * ordinary shares: 5% and 10%, or 10% and 20%.
			 * The president's share counts as a multiple of the lowest
			 * ordinary share unit type.
			 */
			// Take care for max. 4 share units per share
			int[] shareCountPerUnit = new int[5]; 
			compName = company.getName();
			for (PublicCertificateI c : playerPortfolio.getCertificatesPerCompany(compName)) {
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
				
				// May not sell more than is needed to buy the train
				while (number > 0 && ((number-1) * price) > cashToRaise.intValue()) number--;

				if (number > 0) {
                    possibleActions.add (new SellShares (compName,
						i, number, price));
                }
			}
		}
	}

	/**
	 * Get a list of certificates that can be sold.
	 * If none can be sold, register bankruptcy. 
	 * TODO NEEDS BE REPLACED
	 */
	/*
	public List<TradeableCertificate> getSellableCerts () {
	    
	    List<TradeableCertificate> sellableCertificates = super.getSellableCerts();
	    
	    if (sellableCertificates.isEmpty() && cashToRaise > 0) {
	        GameManager.getInstance().registerBankruptcy();
	    }
	    
	    return sellableCertificates;
	    
	}
	*/

	/**
	 * Sell one or more shares or certificates, to be specified in detail.
	 * Special version for emergency train buying.
	 * 
	 * @param player
	 *            Name of the selling player.
	 * @param company
	 *            Name of the company of which shares are sold.
	 * @param number
	 *            The number of shares (if unit=1) or certificates (if unit>1)
	 *            to sell.
	 * @param unit
	 *            The share unit size of the certificates to sell.
	 * @param president
	 *            Indicates if the sale may include (part of) a president's
	 *            certificate, subject to the rules that allow that. TODO Does
	 *            not yet cater for double shares (incl. president).
	 * @return False if an error is found.
	 */
	/*
	public boolean sellShares(String playerName, String companyName,
			int number, int unit, boolean president)
	{

		currentPlayer = GameManager.getCurrentPlayer();
		Portfolio portfolio = currentPlayer.getPortfolio();
		String errMsg = null;
		PublicCompanyI company = null;
		PublicCertificateI cert = null;
		PublicCertificateI presCert = null;
		List<PublicCertificateI> certsToSell 
			= new ArrayList<PublicCertificateI>();
		//List certsToSwap = new ArrayList();
		Player dumpedPlayer = null;
		int presSharesToSell = 0;
		int numberToSell = number;
		int currentIndex = GameManager.getCurrentPlayerIndex();

		// Dummy loop to allow a quick jump out
		while (true)
		{

			// Check everything
			if (number <= 0)
			{
				errMsg = LocalText.getText("NoSellZero");
				break;
			}
			if (!playerName.equals(currentPlayer.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer", playerName);
				break;
			}

			// May not sell in certain cases
			if (!mayCurrentPlayerSellAnything())
			{
				errMsg = LocalText.getText("SoldEnough");
				break;
			}

			// Check company
			company = companyMgr.getPublicCompany(companyName);
			if (company == null)
			{
				errMsg = LocalText.getText("CompanyDoesNotExist",companyName);
				break;
			}

			// The player must have the share(s)
			if (portfolio.getShare(company) < number)
			{
				errMsg = LocalText.getText("DoesNotHaveTheShares");
				break;
			}

			// The pool may not get over its limit.
			if (pool.getShare(company) + number * company.getShareUnit()
					> Bank.getPoolShareLimit())
			{
				errMsg = LocalText.getText("PoolWouldGetOverLimit",
						String.valueOf(Bank.getPoolShareLimit()));
				break;
			}

			// Find the certificates to sell
			Iterator it = portfolio.getCertificatesPerCompany(companyName)
					.iterator();
			while (numberToSell > 0 && it.hasNext())
			{
				cert = (PublicCertificateI) it.next();
				if (cert.isPresidentShare())
				{
					// Remember the president's certificate in case we need it
					if (cert.isPresidentShare())
						presCert = cert;
					continue;
				}
				else if (unit != cert.getShares())
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

			Player otherPlayer = null;
			if (numberToSell > 0 && presCert != null
					&& numberToSell <= presCert.getShares())
			{
				// More to sell and we are President: see if we can dump it.
				for (int i = currentIndex + 1; i < currentIndex
						+ numberOfPlayers; i++)
				{
					otherPlayer = GameManager.getPlayer(i);
					if (otherPlayer.getPortfolio().getShare(company) >= presCert.getShares())
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
					errMsg = LocalText.getText("CannotDumpPresidency");
				}
				else
				{
					errMsg = LocalText.getText("DoesNotHaveTheShares");
				}
				break;
			}
			
			// In emergency selling, we may not lose the presidency
			// of the company that needs the train.
			if (presCert != null && dumpedPlayer != null && presSharesToSell > 0
			        && company == companyNeedingTrain)
			{
			    errMsg = LocalText.getText("CannotDumpTrainBuyingPresidency");
			    break;
			}
			

			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(
					LocalText.getText("CantSell", new String[] {
							playerName,
							String.valueOf(number),
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

		//ReportBuffer.add(playerName + " sells " + number + " shares ("
		//		+ (number * company.getShareUnit()) + "%) of " + companyName
		//		+ " for " + Bank.format(number * price));
		ReportBuffer.add (LocalText.getText("SELL_SHARES_LOG", new String[]{
		        playerName,
		        String.valueOf(numberToSell),
		        String.valueOf((numberToSell * company.getShareUnit())),
		        companyName,
		        Bank.format(numberToSell * price)}));

		// Check if the presidency has changed
		if (presCert != null && dumpedPlayer != null && presSharesToSell > 0)
		{
			//ReportBuffer.add("Presidency of " + companyName + " is transferred to "
			//		+ dumpedPlayer.getName());
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
		Iterator it = certsToSell.iterator();
		while (it.hasNext())
		{
			cert = (PublicCertificateI) it.next();
			if (cert != null)
				pool.buyCertificate(cert, portfolio, cert.getShares() * price);
		}
		stockMarket.sell(company, number);

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
					ReportBuffer.add("Presidency of " + companyName
							+ " is transferred to " + otherPlayer.getName());
					break;
				}
			}
		}


		cashToRaise -= number * price;

		if (cashToRaise <= 0) {
			GameManager.getInstance().finishShareSellingRound();
		}
		return true;
	}
	*/

	public boolean sellShares (SellShares action)
	{

		//currentPlayer = GameManager.getCurrentPlayer();
		Portfolio portfolio = currentPlayer.getPortfolio();
		String playerName = currentPlayer.getName();
		String errMsg = null;
		String companyName = action.getCompanyName();
		PublicCompanyI company = companyMgr.getPublicCompany(action.getCompanyName());
		PublicCertificateI cert = null;
		PublicCertificateI presCert = null;
		List<PublicCertificateI> certsToSell 
			= new ArrayList<PublicCertificateI>();
		//List certsToSwap = new ArrayList();
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
			Iterator it = portfolio.getCertificatesPerCompany(companyName)
					.iterator();
			while (numberToSell > 0 && it.hasNext())
			{
				cert = (PublicCertificateI) it.next();
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
				if (company == companyNeedingTrain)
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
		Iterator it = certsToSell.iterator();
		while (it.hasNext())
		{
			cert = (PublicCertificateI) it.next();
			if (cert != null)
				pool.buyCertificate(cert, portfolio, cert.getShares() * price);
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

		cashToRaise.add(-numberSold * price);

		if (cashToRaise.intValue() <= 0) {
			GameManager.getInstance().finishShareSellingRound();
		}

		return true;
	}

	public int getRemainingCashToRaise () {
	    return cashToRaise.intValue();
	}

    public String toString() {
        return "ShareSellingRound";
    }

}
