package game;

import java.util.*;
import ui.HelpWindow;

/**
 * Implements a basic Stock Round.
 * <p>
 * A new instance must be created for each new Stock Round. At the end of a
 * round, the current instance should be discarded.
 * <p>
 * Permanent memory is formed by static attributes (like who has the Priority
 * Deal).
 */
public class StockRound implements Round
{

	/* Transient memory (per round only) */
	protected static int numberOfPlayers;
	protected Player currentPlayer;

	protected PublicCompanyI companyBoughtThisTurn = null;
	protected boolean hasSoldThisTurnBeforeBuying = false;
	protected boolean hasPassed = true; // Is set false on any player action
	protected int numPasses = 0;

	protected Map sellPrices = new HashMap();
	
	protected List currentSpecialProperties = null;

	/* Transient data needed for rule enforcing */
	/** HashMap per player containing a HashMap per company */
	protected HashMap playersThatSoldThisRound = new HashMap();
	/** HashMap per player */ // Not used (yet?)
	protected HashMap playersThatBoughtThisRound = new HashMap();

	/* Rule constants */
	static protected final int SELL_BUY_SELL = 0;
	static protected final int SELL_BUY = 1;
	static protected final int SELL_BUY_OR_BUY_SELL = 2;

	/* Permanent memory */
	static protected int stockRoundNumber = 0;
	static protected StockMarketI stockMarket;
	static protected Portfolio ipo;
	static protected Portfolio pool;
	static protected CompanyManagerI companyMgr;
	static protected GameManager gameMgr;

	/* Rules */
	static protected int sequenceRule = SELL_BUY_SELL; // Currently fixed
	static protected boolean buySellInSameRound = true;
	static protected boolean noSaleInFirstSR = false;
	static protected boolean noSaleIfNotOperated = false;

	/**
	 * The constructor.
	 */
	public StockRound()
	{

		if (numberOfPlayers == 0)
			numberOfPlayers = GameManager.getPlayers().length;
		if (gameMgr == null)
			gameMgr = GameManager.getInstance();
		if (stockMarket == null)
			stockMarket = StockMarket.getInstance();
		if (ipo == null)
			ipo = Bank.getIpo();
		if (pool == null)
			pool = Bank.getPool();
		if (companyMgr == null)
			companyMgr = Game.getCompanyManager();
		GameManager.getInstance().setRound(this);
	}

	public void start()
	{
		stockRoundNumber++;

		Log.write("\nStart of Stock Round " + stockRoundNumber);

		GameManager.setCurrentPlayerIndex(GameManager.priorityPlayerIndex);
		
		initPlayer();
		Log.write(currentPlayer.getName() + " has the Priority Deal");
	}

	/*----- General methods -----*/

	public int getStockRoundNumber()
	{
		return stockRoundNumber;
	}

	public static int getLastStockRoundNumber()
	{
		return stockRoundNumber;
	}
	
	/**
	 * Create a list of certificates that a player may buy in a Stock Round,
	 * taking all rules into account.
	 * @return List of buyable certificates.
	 */
	public List getBuyableCerts () {
	    
	    List buyableCerts = new ArrayList();
	    
	    List certs;
	    PublicCertificateI cert;
	    TradeableCertificate tCert;
	    PublicCompanyI comp;
	    int price;
	    
	    int playerCash = currentPlayer.getCash();
	    
	    /* Get the next available IPO certificates */
	    // Never buy more than one from the IPO
	    if (this.companyBoughtThisTurn == null) {
		    String compName;
		    Map map = Bank.getIpo().getCertsPerCompanyMap();
		    int lowestStartPrice = 999;
		    int highestStartPrice = 0;
		    int shares;
		    int[] startPrices = stockMarket.getStartPrices();
	
		    for (Iterator it = map.keySet().iterator(); it.hasNext(); ) {
		        compName = (String) it.next();
		        certs = (List) map.get(compName);
		        if (certs == null || certs.isEmpty()) continue;
		        /* Only the top certificate is buyable from the IPO */
		        cert = (PublicCertificateI) certs.get(0);
		        comp = cert.getCompany();
		        if (isSaleRecorded (currentPlayer, comp)) continue;
		        if (!currentPlayer.mayBuyCompanyShare(comp, 1)) continue;
		        shares = cert.getShares(); 
		        
		        if (!comp.hasStarted()) {
	                for (int i=0; i<startPrices.length; i++) {
	                    if (startPrices[i] * shares <= playerCash) {
	                        buyableCerts.add(new TradeableCertificate (cert, startPrices[i]));
	                    }
	                }
		        } else if (comp.hasParPrice()) {
		            price = comp.getParPrice().getPrice() * cert.getShares();
			        if (playerCash < price) continue;
		            buyableCerts.add(new TradeableCertificate (cert, price));
		        } else {
		            price = comp.getCurrentPrice().getPrice() * cert.getShares();
			        if (playerCash < price) continue;
		            buyableCerts.add(new TradeableCertificate (cert, price));
		        }
	
		    }
	    }
	    
	    /* Get the unique Pool certificates and check which ones can be bought */
	    for (Iterator it = Bank.getPool().getUniqueTradeableCertificates().iterator();
	    		it.hasNext(); ) {
	        tCert = (TradeableCertificate) it.next();
	        if (playerCash < tCert.getPrice()) continue;
	        comp = tCert.getCert().getCompany();
	        if (isSaleRecorded (currentPlayer, comp)) continue;
	        if (companyBoughtThisTurn != null) {
	            // If a cert was bought before, only brown zone ones can be bought again
	            if (comp != companyBoughtThisTurn) continue;
	            if (!comp.getCurrentPrice().isNoBuyLimit()) continue;
	        }
	        if (!currentPlayer.mayBuyCompanyShare(comp, 1)) continue;
	        if (!currentPlayer.mayBuyCertificate(comp, 1)) continue;
	        buyableCerts.add (tCert);
	    }
	    
	    return buyableCerts;
	}

	/**
	 * Create a list of certificates that a player may sell in a Stock Round,
	 * taking all rules taken into account.
	 * @return List of sellable certificates.
	 */
	public List getSellableCerts () {
	    
	    List sellableCerts = new ArrayList();
	    
	    if (!mayCurrentPlayerSellAtAll()) return sellableCerts;
	    
	    List certs;
	    PublicCertificateI cert;
	    TradeableCertificate tCert;
	    PublicCompanyI comp;
	    String compName;
	    int price;

	    /* Get the unique Player certificates and check which ones can be sold */
	    //System.out.println("player="+currentPlayer);
	    //System.out.println("portfolio="+currentPlayer.getPortfolio());
	    //System.out.println("certs="+currentPlayer.getPortfolio().getUniqueTradeableCertificates());
	    for (Iterator it = currentPlayer.getPortfolio().getUniqueTradeableCertificates().iterator();
	    		it.hasNext(); ) {
	        tCert = (TradeableCertificate) it.next();
	        cert = tCert.getCert();
            comp = cert.getCompany();
            compName = comp.getName();
	        
	        if (cert.isPresidentShare()) {
	            /* Would even selling one unit give more that 50% in the Pool? */
	            if (comp.getShareUnit() + pool.ownsShare(comp) > Bank.getPoolShareLimit()) {
	                continue;
	            }
		        /* Can the presidency be dumped? */
	            boolean victimFound = false;
	            Player[] players = GameManager.getPlayers();
	            for (int i=0; i<numberOfPlayers; i++) {
	                if (players[i] == currentPlayer) continue;
	                if (players[i].getPortfolio().ownsShare(comp) >= cert.getShare()) {
	                    victimFound = true;
	                    break;
	                }
	            }
	            if (!victimFound) continue;
	        } else {
		        /* Would there be more than 50% in the Pool? */
		        if (cert.getShare() + pool.ownsShare(comp) > Bank.getPoolShareLimit()) {
		            continue;
		        }
	        }
	        
	        /* If a cert was sold before this turn, correct the price */
	        if (sellPrices.containsKey(compName)) {
	            price = cert.getShares() * ((StockSpaceI) sellPrices.get(compName)).getPrice();
	            tCert.setPrice(price);
	        }
	        sellableCerts.add (tCert);
	    }
	    return sellableCerts;
	}

	/*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/

	/**
	 * Start a company by buying the President's share only
	 * 
	 * @param company
	 *            The company to start.
	 * @return True if the company could be started.
	 */
	public boolean startCompany(String playerName, String companyName, int price)
	{
		return startCompany(playerName, companyName, price, 1);
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
	public boolean startCompany(String playerName, String companyName,
			int price, int shares)
	{

		String errMsg = null;
		StockSpaceI startSpace = null;
		int numberOfCertsToBuy = 0;
		PublicCertificateI cert = null;
		PublicCompanyI company = null;

		currentPlayer = GameManager.getCurrentPlayer();

		// Dummy loop to allow a quick jump out
		while (true)
		{

			// Check everything
			// Only the player that has the turn may buy
			if (!playerName.equals(currentPlayer.getName()))
			{
				errMsg = "Wrong player " + playerName;
				break;
			}

			// The player may not have bought this turn.
			if (companyBoughtThisTurn != null)
			{
				errMsg = "Already bought this turn";
				break;
			}

			// Check company
			company = companyMgr.getPublicCompany(companyName);
			if (company == null)
			{
				errMsg = "Company does not exist";
				break;
			}
			// The company may not have started yet.
			if (company.hasStarted())
			{
				errMsg = company.getName() + " was started before";
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
				errMsg = "Cannot buy more certificates";
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
					errMsg = "Invalid start price: " + price;
					break;
				}
			}

			// Check if the Player has the money.
			if (currentPlayer.getCash() < shares * price)
			{
				errMsg = "Not enough money";
				break;
			}

			break;
		}

		if (errMsg != null)
		{
			Log.error(playerName + " cannot start " + companyName + ": "
					+ errMsg);
			return false;
		}

		// All is OK, now start the company
		company.start(startSpace);

		// Transfer the President's certificate
		currentPlayer.getPortfolio().buyCertificate(cert,
				ipo,
				cert.getCertificatePrice());

		// If more than one certificate is bought at the same time, transfer
		// these too.
		for (int i = 1; i < numberOfCertsToBuy; i++)
		{
			cert = ipo.findCertificate(company, false);
			currentPlayer.getPortfolio().buyCertificate(cert,
					ipo,
					cert.getCertificatePrice());
		}
		Log.write(playerName + " starts " + companyName + " at " + price
				+ " and buys " + shares + " share(s) (" + cert.getShare()
				+ "%) for " + Bank.format(shares * price) + ".");

		company.checkFlotation();

		companyBoughtThisTurn = company;
		hasPassed = false;
		setPriority();

		return true;
	}

	/**
	 * Buy one or more single-share certificates (more is sometimes possible)
	 * 
	 * @param player
	 *            The player buying shares.
	 * @param portfolio
	 *            The portfolio from which to buy shares.
	 * @param company
	 *            The company of which to buy shares.
	 * @param shares
	 *            The number of shares to buy.
	 * @return True if the certificates bould be bought. False indicates an
	 *         error.
	 */
	public boolean buyShare(String playerName, Portfolio from,
			String companyName, int shares)
	{
		return buyShare(playerName, from, companyName, shares, 1);
	}

	/**
	 * Buying one or more single or double-share certificates (more is sometimes
	 * possible)
	 * 
	 * @param player
	 *            The player that wants to buy shares.
	 * @param portfolio
	 *            The portfolio from which to buy shares.
	 * @param company
	 *            The company of which to buy shares.
	 * @param shares
	 *            The number of shares to buy.
	 * @param unit
	 *            The number of share units in each certificate to buy (e.g.
	 *            value is 2 for 20% Badische or 10% Preussische non-president
	 *            certificates in 1835).
	 * @return True if the certificates could be bought. False indicates an
	 *         error. TODO Usage of 'unit' argument.
	 */
	public boolean buyShare(String playerName, Portfolio from,
			String companyName, int shares, int unit)
	{

		String errMsg = null;
		int price = 0;
		PublicCompanyI company = null;

		currentPlayer = GameManager.getCurrentPlayer();

		// Dummy loop to allow a quick jump out
		while (true)
		{

			// Check everything
			// Only the player that has the turn may buy
			if (!playerName.equals(currentPlayer.getName()))
			{
				errMsg = "Wrong player " + playerName;
				break;
			}

			// Check company
			company = companyMgr.getPublicCompany(companyName);
			if (company == null)
			{
				errMsg = "Company does not exist";
				break;
			}
			
			// The player may not have sold the company this round.
			if (isSaleRecorded (currentPlayer, company))
			{
				errMsg = currentPlayer.getName() + " already sold "
						+ companyName + " this turn";
				break;
			}

			// The company must have started before
			if (!company.hasStarted())
			{
				errMsg = "Company " + companyName + " is not yet started";
				break;
			}

			// The player may not have bought this turn, unless the company
			// bought before and now is in the brown area.
			if (companyBoughtThisTurn != null
					&& (companyBoughtThisTurn != company || !company.getCurrentPrice()
							.isNoBuyLimit()))
			{
				errMsg = currentPlayer.getName() + " already bought "
						+ companyBoughtThisTurn.getName() + " this turn";
				break;
			}

			// Check if that many shares are available
			if (shares > from.ownsShare(company))
			{
				errMsg = companyName + " share(s) not available";
				break;
			}

			StockSpaceI currentSpace;
			if (from == ipo && company.hasParPrice()) {
			    currentSpace = company.getParPrice();
			} else {
			    currentSpace = company.getCurrentPrice();
			}

			// Check if it is allowed to buy more than one certificate (if
			// requested)
			if (shares > 1 && !currentSpace.isNoBuyLimit())
			{
				errMsg = "Cannot buy more than 1 " + companyName + " share";
				break;
			}

			// Check if player would not exceed the certificate limit.
			// (shortcut: assume 1 cert == 1 certificate)
			if (!currentSpace.isNoCertLimit()
					&& !currentPlayer.mayBuyCertificate(company, shares))
			{
				errMsg = currentPlayer.getName()
						+ " would exceed certificate limit";
				break;
			}

			// Check if player would exceed the per-company share limit
			if (!currentSpace.isNoHoldLimit()
					&& !currentPlayer.mayBuyCompanyShare(company, shares))
			{
				errMsg = currentPlayer.getName()
						+ " would exceed holding limit";
				break;
			}

			price = currentSpace.getPrice();

			// Check if the Player has the money.
			if (currentPlayer.getCash() < shares * price)
			{
				errMsg = currentPlayer.getName()
						+ " does not have enough money";
				break;
			}

			break;
		}

		if (errMsg != null)
		{
			Log.error(playerName + " cannot buy " + shares + " share(s) of "
					+ companyName + " from " + from.getName() + ": " + errMsg);
			return false;
		}

		// All seems OK, now buy the shares.
		PublicCertificateI cert;
		for (int i = 0; i < shares; i++)
		{
			cert = from.findCertificate(company, false);
			Log.write(playerName + " buys " + shares + " share(s) ("
					+ cert.getShare() + "%) of " + companyName + " from "
					+ from.getName() + " for " + Bank.format(shares * price)
					+ ".");
			currentPlayer.buy(cert, price * cert.getShares());
		}

		companyBoughtThisTurn = company;
		hasPassed = false;
		setPriority();

		// Check if the company has floated
		if (from == ipo)
			company.checkFlotation();

		return true;
	}
	
	private void recordSale (Player player, PublicCompanyI company) {
	    if (!playersThatSoldThisRound.containsKey(player)) {
	        playersThatSoldThisRound.put (player, new HashMap());
	    }
	    ((Map)playersThatSoldThisRound.get (player)).put(company, null);
	}
	
	private boolean isSaleRecorded (Player player, PublicCompanyI company) {
	    return playersThatSoldThisRound.containsKey(currentPlayer)
			&& ((HashMap) playersThatSoldThisRound.get(currentPlayer)).containsKey(company);
	}

	/**
	 * Sell a one-share certificate (i.e. one share unit, normally 10%). This
	 * could involve partial sale of a President's certificate.
	 * 
	 * @see sellShare (String playerName, String companyName)
	 * @param playerName
	 *            Name of the selling player.
	 * @param companyName
	 *            Name of the company of which one share is sold.
	 * @return False if an error is found.
	 */
	public boolean sellShare(String playerName, String companyName)
	{
		return sellShares(playerName, companyName, 1, 1, true);

	}

	/**
	 * Sell a two-share certificate, NOT being a President's share (normally
	 * 20%). Such certificates exist in 1835 and other games.
	 * 
	 * @see sellShare (String playerName, String companyName)
	 * @param playerName
	 *            Name of the selling player.
	 * @param companyName
	 *            Name of the company of which a double share is sold.
	 * @return False if an error is found.
	 */
	public boolean sellDoubleShare(String playerName, String companyName)
	{
		return sellShares(playerName, companyName, 1, 2, false);

	}

	/**
	 * Sell one or more shares (one or multiple share units, normally 10% each).
	 * This could involve selling part of a President's share, but not a
	 * non-president double share.
	 * 
	 * @param player
	 *            Name of the selling player.
	 * @param company
	 *            Name of the company of which shares are sold.
	 * @param number
	 *            The number of shares (in fact: share units) to sell. TODO Does
	 *            not yet cater for double shares (incl. president).
	 * @return False if an error is found.
	 */
	public boolean sellShares(String playerName, String companyName, int number)
	{
		return sellShares(playerName, companyName, number, 1, true);
	}

	/**
	 * Sell one or more shares or certificates, to be specified in detail.
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
			if (stockRoundNumber == 1 && noSaleInFirstSR)
			{
				errMsg = "Cannot sell in first Stock Round";
				break;
			}
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

			break;
		}

		if (errMsg != null)
		{
			Log.error(playerName + " cannot sell " + number + " share(s) of "
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

		Log.write(playerName + " sells " + number + " shares ("
				+ (number * company.getShareUnit()) + "%) of " + companyName
				+ " for " + Bank.format(number * price));

		// Check if the presidency has changed
		if (presCert != null && dumpedPlayer != null && presSharesToSell > 0)
		{
			Log.write("Presidency of " + companyName + " is transferred to "
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
					Log.write("Presidency of " + companyName
							+ " is transferred to " + otherPlayer.getName());
					break;
				}
			}
		}

		// Remember that the player has sold this company this round.
		recordSale (currentPlayer, company);

		if (companyBoughtThisTurn == null)
			hasSoldThisTurnBeforeBuying = true;
		hasPassed = false;
		setPriority();

		return true;
	}

	/**
	 * The current Player passes or is done.
	 * 
	 * @param player
	 *            Name of the passing player.
	 * @return False if an error is found.
	 */
	public boolean done(String playerName)
	{

		String errMsg = null;

		currentPlayer = GameManager.getCurrentPlayer();

		if (!playerName.equals(currentPlayer.getName()))
		{
			errMsg = "Wrong player " + playerName;
			return false;
		}

		if (hasPassed)
		{
			numPasses++;
			Log.write(currentPlayer.getName() + " passes.");
		}
		else
		{
			numPasses = 0;
		}

		if (numPasses >= numberOfPlayers)
		{

			Log.write("All players have passed, end of SR " + stockRoundNumber);

			// Check if any companies are sold out.
			Iterator it = companyMgr.getAllPublicCompanies().iterator();
			boolean soldOut;
			PublicCompanyI company;
			while (it.hasNext())
			{
				company = (PublicCompanyI) it.next();
				if (company.hasStockPrice() && company.isSoldOut())
				{
					Log.write(company.getName() + " is sold out");
					stockMarket.soldOut(company);
				}
			}

			// Inform GameManager
			GameManager.getInstance().nextRound(this);

		}
		else
		{

			setNextPlayer();
			sellPrices = new HashMap();

		}

		return true;
	}

	/**
	 * Internal method: pass the turn to the next player.
	 */
	protected void setNextPlayer()
	{

		GameManager.setNextPlayer();
		initPlayer();
	}
	protected void initPlayer() {
		
		currentPlayer = GameManager.getCurrentPlayer();
		companyBoughtThisTurn = null;
		hasSoldThisTurnBeforeBuying = false;
		hasPassed = true;

		currentSpecialProperties = currentPlayer.getPortfolio()
			.getSpecialProperties(game.special.SpecialSRProperty.class);
		//System.out.println("Player "+currentPlayer.getName()+", spec#="+currentSpecialProperties.size());
	}

	/**
	 * Remember the player that has the Priority Deal. <b>Must be called BEFORE
	 * setNextPlayer()!</b>
	 */
	protected void setPriority()
	{
		GameManager.setPriorityPlayer();
	}

	/*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

	/**
	 * @return The player that has the Priority Deal.
	 */
	public static Player getPriorityPlayer()
	{
		return GameManager.priorityPlayer;
	}

	/**
	 * @return The index of the player that has the Priority Deal.
	 */
	public static int getPriorityPlayerIndex()
	{
		return GameManager.priorityPlayerIndex;
	}

	/**
	 * @return The player that has the turn.
	 */
	public Player getCurrentPlayer()
	{
		return GameManager.currentPlayer;
	}

	/**
	 * @return The index of the player that has the turn.
	 */
	public int getCurrentPlayerIndex()
	{
		return GameManager.currentPlayerIndex;
	}
	
	public List getSpecialProperties() {
	    return currentSpecialProperties;
	}

	/**
	 * Check if a public company can be started by the player that has the turn.
	 * 
	 * @param companyName
	 *            Name of the company to be checked.
	 * @return True of false. TODO Check for unstarted companies that may not
	 *         yet be started. TODO Check if current player has enough money to
	 *         start at the lowest price.
	 */
	public boolean isCompanyStartable(String companyName)
	{

		return !companyMgr.getPublicCompany(companyName).hasStarted();
	}

	/**
	 * Check if a company can be bought by the current player from a given
	 * Portfolio.
	 * 
	 * @param companyName
	 *            Name of the company to be checked.
	 * @param source
	 *            The portfolio that is checked for presence of company shares.
	 *            TODO Buying from company treasuries if just IPO is specified.
	 *            TODO Add checks that the current player may buy and has the
	 *            money. TODO Presidencies in the Pool (rare!)
	 */
	public boolean isCompanyBuyable(String companyName, Portfolio source)
	{

		PublicCompanyI company = companyMgr.getPublicCompany(companyName);
		if (!company.hasStarted())
			return false;
		if (source.findCertificate(company, false) == null)
			return false;
		return true;
	}

	/**
	 * Check if the current player can sell shares of a company.
	 * 
	 * @param companyName
	 *            Name of the company to be checked
	 * @return True if the company can be sold. TODO Make Bank Pool share limit
	 *         configurable.
	 */
	public boolean isCompanySellable(String companyName)
	{

		if (stockRoundNumber == 1 && noSaleInFirstSR)
			return false;
		PublicCompanyI company = companyMgr.getPublicCompany(companyName);
		if (!company.hasStockPrice())
			return false;
		if (noSaleIfNotOperated && !company.hasOperated())
			return false;
		if (currentPlayer.getPortfolio().ownsShare(company) == 0)
			return false;
		if (pool.ownsShare(company) >= Bank.getPoolShareLimit())
			return false;
		return true;
	}

	/**
	 * Can the current player do any selling?
	 * 
	 * @return True if any selling is allowed.
	 */
	public boolean mayCurrentPlayerSellAtAll()
	{
		if (stockRoundNumber == 1 && noSaleInFirstSR)
			return false;
		if (sequenceRule == SELL_BUY_OR_BUY_SELL
				&& companyBoughtThisTurn != null && hasSoldThisTurnBeforeBuying
				|| sequenceRule == SELL_BUY && companyBoughtThisTurn != null)
			return false;
		return true;
	}

	/**
	 * Can the current player do any buying?
	 * 
	 * @return True if any buying is allowed.
	 */
	/*
	public boolean mayCurrentPlayerBuyAtAll()
	{
		return companyBoughtThisTurn == null;
	}
	*/

	public static void setNoSaleInFirstSR()
	{
		noSaleInFirstSR = true;
	}

	public static void setNoSaleIfNotOperated()
	{
		noSaleIfNotOperated = true;
	}
	
	public String getHelp() {
	     return "Stock round help text";
	}
}
