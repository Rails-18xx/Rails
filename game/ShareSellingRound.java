/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/ShareSellingRound.java,v 1.3 2007/01/07 19:25:26 evos Exp $
 * 
 * Created on 21-May-2006
 * Change Log:
 */
package game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Erik Vos
 */
public class ShareSellingRound extends StockRound {
    
    Player sellingPlayer;
    PublicCompanyI companyNeedingTrain;
    int cashToRaise;
    
    public ShareSellingRound (PublicCompanyI compNeedingTrain, int cashToRaise) {
        
        this.companyNeedingTrain = compNeedingTrain;
        this.cashToRaise = cashToRaise;
        sellingPlayer = compNeedingTrain.getPresident();

		GameManager.getInstance().setRound(this);
		GameManager.setCurrentPlayerIndex(sellingPlayer.getIndex());
}
    
    public void start() {
        System.out.println("Share selling round started");
        currentPlayer = sellingPlayer;
    }
    
    public boolean isCompanyBuyable(String companyName, Portfolio source) {
        return false;
    }
    
    public boolean isCompanySellable(String companyName) {
        return true;
    }
    
	public boolean mayCurrentPlayerSellAtAll() {
	    return true;
	}
	
	public boolean mayCurrentPlayerBuyAtAll() {
	    return false;
	}

	/**
	 * Get a list of certificates that can be sold.
	 * If none can be sold, register bankruptcy. 
	 */
	public List getSellableCerts () {
	    
	    List sellableCertificates = super.getSellableCerts();
	    
	    if (sellableCertificates.isEmpty() && cashToRaise > 0) {
	        GameManager.getInstance().registerBankruptcy();
	    }
	    
	    return sellableCertificates;
	    
	}

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
	public boolean sellShares(String playerName, String companyName,
			int number, int unit, boolean president)
	{

		currentPlayer = GameManager.getCurrentPlayer();
		Portfolio portfolio = currentPlayer.getPortfolio();
		String errMsg = null;
		PublicCompanyI company = null;
		PublicCertificateI cert = null;
		PublicCertificateI presCert = null;
		List certsToSell = new ArrayList();
		List certsToSwap = new ArrayList();
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
				errMsg = "Cannot sell less that one share";
				break;
			}
			if (!playerName.equals(currentPlayer.getName()))
			{
				errMsg = "Wrong player " + playerName;
				break;
			}

			// May not sell in certain cases
			if (!mayCurrentPlayerSellAtAll())
			{
				errMsg = "May not sell anymore in this turn";
				break;
			}

			// Check company
			company = companyMgr.getPublicCompany(companyName);
			if (company == null)
			{
				errMsg = "Company does not exist";
				break;
			}

			// The player must have the share(s)
			if (portfolio.ownsShare(company) < number)
			{
				errMsg = "Does not have the share(s)";
				break;
			}

			// The pool may not get over its limit.
			if (pool.ownsShare(company) + number * company.getShareUnit() > Bank.getPoolShareLimit())
			{
				errMsg = "Pool would get over its share holding limit";
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

			if (numberToSell > 0 && presCert != null
					&& numberToSell <= presCert.getShares())
			{
				// More to sell and we are President: see if we can dump it.
				Player otherPlayer;
				for (int i = currentIndex + 1; i < currentIndex
						+ numberOfPlayers; i++)
				{
					otherPlayer = GameManager.getPlayer(i);
					if (otherPlayer.getPortfolio().ownsShare(company) >= presCert.getShares())
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
					errMsg = "Cannot dump presidency";
				}
				else
				{
					errMsg = "Does not have that many shares";
				}
				break;
			}
			
			// In emergency selling, we may not lose the presidency
			// of the company that needs the train.
			if (presCert != null && dumpedPlayer != null && presSharesToSell > 0
			        && company == companyNeedingTrain)
			{
			    errMsg = "Cannot dump presidency of train buying company";
			    break;
			}
			

			break;
		}

		if (errMsg != null)
		{
			MessageBuffer.add(playerName + " cannot sell " + number + " share(s) of "
					+ companyName + ": " + errMsg);
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

		LogBuffer.add(playerName + " sells " + number + " shares ("
				+ (number * company.getShareUnit()) + "%) of " + companyName
				+ " for " + Bank.format(number * price));

		// Check if the presidency has changed
		if (presCert != null && dumpedPlayer != null && presSharesToSell > 0)
		{
			LogBuffer.add("Presidency of " + companyName + " is transferred to "
					+ dumpedPlayer.getName());
			// First swap the certificates
			Portfolio dumpedPortfolio = dumpedPlayer.getPortfolio();
			List swapped = portfolio.swapPresidentCertificate(company,
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
				if (otherPlayer.getPortfolio().ownsShare(company) > portfolio.ownsShare(company))
				{
					portfolio.swapPresidentCertificate(company,
							otherPlayer.getPortfolio());
					LogBuffer.add("Presidency of " + companyName
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

	public int getRemainingCashToRaise () {
	    return cashToRaise;
	}


}
