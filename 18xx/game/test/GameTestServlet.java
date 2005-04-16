/*
 * Created on 26-Feb-2005
 */
package game.test;

import game.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Erik
 */
public class GameTestServlet extends HttpServlet {

	private StockMarketI stockMarket = null;
	private Game game = null;
	private Bank bank = null;
	private int phase = 0;
	private boolean orStarted = false;
	private String error;
	
	private static final int SELECTPLAYERS = 0;
	private static final int BUYPRIVATES = 1;
	private static final int SR = 2;
	private static final int OR = 3;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);

	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		doWork(request, response);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		doWork(request, response);
	}

	protected void doWork(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		int row, col;
		StockSpaceI square;
		CompanyI comp;
		PublicCompanyI company;
		String companyName;
		CertificateI cert;
		int companyNumber;
		Iterator iterator, iterator2;
		int i;
		List startSpaces;
		int price;
		
		if (stockMarket == null) {
			/* Initialise */
			String gameName = request.getParameter("Game");
			if (gameName != null && !gameName.equals("")) {
				game = Game.getInstance();
				game.initialise (gameName);
				stockMarket = game.getStockMarket();
				bank = game.getBank();
			}
		} else if (phase == SELECTPLAYERS) {
			
			if (hasValue(request.getParameter ("AddPlayer"))) {
				String newPlayer = request.getParameter ("Player");
				if(hasValue(newPlayer)) {
					Player.addPlayer (newPlayer);
				}
			} else if (hasValue (request.getParameter ("StartGame"))) {
				List players = Player.getPlayers();
				int startCash = 2400/players.size(); // To be made configurable
				Iterator it = players.iterator();
				Player player;
				while (it.hasNext()) {
					player = (Player)it.next();
					Bank.transferCash (null, player, startCash);
					Log.write ("Player "+player.getName()+" receives "+startCash);
				}
				phase = BUYPRIVATES;
			}
			
		} else if (phase == BUYPRIVATES) {
			
			if (hasValue (request.getParameter ("BuyPrivate"))) {
				int player = Integer.parseInt(request.getParameter("Player"));
				int priv = Integer.parseInt(request.getParameter("Private"));
				price = Integer.parseInt(request.getParameter("Price"));
				PrivateCompanyI privco = 
					(PrivateCompanyI)game.getCompanyManager().getAllPrivateCompanies().get(priv);
				Player.getPlayer(player).getPortfolio().buyPrivate(
					privco,
					game.getBank().getIpo(), price);
				if (Integer.parseInt(request.getParameter("Left")) == 1) {
					phase = SR;
					orStarted = false;
				} 
			}
			
		} else if (phase == SR) {
			
			if (hasValue(request.getParameter("GotoOR"))) {
				// Check for sold-out companies
				Iterator it = game.getCompanyManager().getAllPublicCompanies().iterator();
				boolean soldOut;
				while (it.hasNext()) {
					company = (PublicCompanyI)it.next();
					if (company.isSoldOut()) {
						Log.write(company.getName()+" is sold out");
						stockMarket.soldOut(company);} 
				}
				phase = OR;
			} else {
				int pl = Integer.parseInt(request.getParameter("Player"));
				Player player = Player.getPlayer(pl);
				String cmpy = request.getParameter("Company");
				company = game.getCompanyManager().getPublicCompany(cmpy);
				Portfolio from = null, to = null;
				boolean buyIpo = hasValue(request.getParameter("BuyIPO"));
				boolean buyPool = hasValue(request.getParameter("BuyPool"));
				boolean sell = hasValue(request.getParameter("Sell"));
				String snumber = request.getParameter("Number");
				int number = hasValue(snumber) ? Integer.parseInt(snumber) : 0;
				price = 0;
				boolean president = false;
				
				if (buyIpo) {
					from = bank.getIpo();
					to = player.getPortfolio();
					number = 1;
					if (company.getParPrice() != null) {
						price = company.getParPrice().getPrice();
					} else {
						price = Integer.parseInt(request.getParameter("StartPrice"));
						company.setParPrice(stockMarket.getStartSpace(price));
						president = true;
						Log.write(player.getName()+" starts "+company.getName()+" at "+price);
					}
//System.out.println("Player "+player.getName()+" buys "+company.getName()+" from IPO for "+price);
				} else if (buyPool) {
					from = bank.getPool();
					to = player.getPortfolio();
					number = 1;
					price = company.getCurrentPrice().getPrice();
//System.out.println("Player "+player.getName()+" buys "+company.getName()+" from Pool for "+price);
				} else if (sell) {
					from = player.getPortfolio();
					to = bank.getPool();
					if (number < 1) number = 1;
					else if (number > 5) number = 5;
					price = company.getCurrentPrice().getPrice();
//System.out.println("Player "+player.getName()+" sells "+company.getName()+" to pool for "+price);
				}
				for (int k=0; k<number; k++) {
					cert = from.findCertificate(company, president);
					if (cert == null) break;
					price *= cert.getShare() / 10;
					// Get the certificate and pay the price
					to.buyCertificate(cert, from, price);
					
				}
				
				if (buyIpo && !company.hasFloated() && from.countShares(company) <= 40) {
					// Float company (limit and capitalisation to be made configurable)
					company.setFloated(10*price);
				} else if (sell) {
					stockMarket.sell(company, number);
				}

			}
			
		} 
		
		if (phase == OR) {
			
			if (hasValue(request.getParameter("StartSR"))) {
				phase = SR;
			} else if (hasValue(request.getParameter("NextOR"))) {
				orStarted = false;
			} 
			if (!orStarted) {
				// Privates pay out
				Iterator it = game.getCompanyManager().getAllPrivateCompanies().iterator();
				while (it.hasNext()) {
					((PrivateCompanyI)it.next()).payOut();
				}
				orStarted = true;
			}

			String compName = request.getParameter("Company");
			company = game.getCompanyManager().getPublicCompany(compName);

			String samount = request.getParameter("Amount");
			int amount = hasValue(samount) ? Integer.parseInt(samount) : 0;
			
			if (hasValue (request.getParameter("Spend"))) {
				Bank.transferCash((CashHolder)company, null, amount);
				Log.write (company.getName()+" spends "+amount);
			} else if (hasValue (request.getParameter("PayOut"))) {
				company.payOut (amount);
			} else if (hasValue (request.getParameter("Withhold"))) {
				company.withhold (amount);
			} else if (hasValue (request.getParameter("BuyPrivate"))) {
				String privName = request.getParameter("Private");
				PrivateCompanyI priv = game.getCompanyManager().getPrivateCompany(privName);
				if (hasValue(privName)) {
					company.getPortfolio().buyPrivate(priv, priv.getHolder(), amount);
				}
			}
		}

		/* Create the new Stock Chart HTML page */
		response.setContentType("text/html");

		/* Read some properties */
		Properties prop = new Properties();
		prop.load(this.getClass().getClassLoader().getResourceAsStream("testservlet.properties"));
		String styleSheetPrefix = prop.getProperty("StyleSheetPrefix");
		String servletPrefix = prop.getProperty("ServletPrefix");

		StringBuffer out = new StringBuffer();

		out.append("<html><head>");
		//out.append ("<link REL=\"STYLESHEET\" HREF=\"http://localhost:8080/18xx/stockmarket.css\" TYPE=\"text/css\">\n");
		out.append("<STYLE TYPE=\"text/css\">\n");
		out.append(
			".stockmarket0 { background-color: white; border: 1px solid black; text-align:center; vertical-align:top;}\n");
		out.append(
			".stockmarket1 { background-color: yellow; border: 1px solid black; text-align:center; vertical-align:top;}\n");
		out.append(
			".stockmarket2 { background-color: orange; border: 1px solid black; text-align:center; vertical-align:top;}\n");
		out.append(
			".stockmarket3 { background-color: brown; border: 1px solid black; text-align:center; vertical-align:top;}\n");
		out.append(
			".stockmarket_start { background-color: white; border: 2px solid red; text-align:center; vertical-align:top;}\n");
		out.append(".bordertable td,th { background-color: white; border: 1px solid black; text-align:center; vertical-align:top;}\n");
		out.append(".bigtable { border: 2px solid black; text-align:left; vertical-align:top;}\n");
		out.append("</STYLE>\n");

		out.append("</head><body>\n");

		if (stockMarket != null) {
			
			// Left upper part: the stockmarket
			out.append ("<table><tr><td rowspan=3 colspan=2 valign=\"top\" class=\"bigtable\">\n<h3>Stock Market</h3>\n");

			out.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
			out.append("<tr><td></td>");
			for (col = 0; col < stockMarket.getNumberOfColumns(); col++) {
				out.append(
					"<td align=\"center\">" + Character.toString((char) ('A' + col)) + "</td>");
			}
			out.append("</tr>\n");

			for (row = 0; row < stockMarket.getNumberOfRows(); row++) {
				out.append("<tr><td>" + (row + 1) + "</td>\n");
				for (col = 0; col < stockMarket.getNumberOfColumns(); col++) {
					square = stockMarket.getStockSpace(row, col);
					if (square != null) {
						out.append(
							"<td width=\"30\" height=\"40\" class=\"stockmarket");
						if (square.isStart()) out.append ("_start");
						else if (square.isNoBuyLimit()) out.append("3");
						else if (square.isNoHoldLimit()) out.append("2");
						else if (square.isNoCertLimit()) out.append ("1");
						else out.append ("0");
						out.append ("\"");
						
						if (square.isBelowLedge())
							out.append(" style=\"border-top: 3px solid red\"");
						else if (square.isLeftOfLedge())
							out.append(" style=\"border-right: 3px solid red\"");

						out.append(">");
						if (square.closesCompany()) {
							out.append("<small>Closes</small>");
						} else {
							out.append(square.getPrice());
						}
						if (square.endsGame())
							out.append(" <small>Game Over</small>");
						iterator = square.getTokens().iterator();
						while (iterator.hasNext()) {
							company = (PublicCompany) iterator.next();
							out.append(
								"<br><span style=\"color:"
									+ company.getFgColour()
									+ ";background-color:"
									+ company.getBgColour()
									+ "\">"
									+ company.getName()
									+ "</span>");
						}
						out.append("</td>\n");
					} else {
						out.append("<td></td>\n");
					}
				}
				out.append("</tr>\n");

			}
			out.append("</table>");
			
			// Right upper part 1: actions
			out.append ("</td><td valign=\"top\" class=\"bigtable\">\n<h3>Actions</h3>\n");
			
			if (phase == SELECTPLAYERS) {
				
				out.append ("<h4>Selecting players</h4>");
				out.append("<table class=\"bordertable\" cellspacing=0>");
				for (int j=0; j<Player.numberOfPlayers(); j++) {
					out.append("<tr><td>"+(j+1)+"</td><td>"+Player.getPlayer(j).getName()
						+"</td></tr>\n");
				}
				out.append("</table><p>");
				out.append(
					"<form method=\"POST\" action=\""
						+ servletPrefix
						+ "game.test.GameTestServlet\">\n");
				out.append ("<input type=\"text\" name=\"Player\" size=\"16\">")
					.append("&nbsp;&nbsp;") 					.append("<input type=\"submit\" name=\"AddPlayer\" value=\"Add Player\">");
				out.append (" <input type=\"submit\" name=\"StartGame\" value=\"Start Game\">");
				out.append("</form>");				
				
			} else if (phase == BUYPRIVATES) {
				
				out.append ("<h4>Buying Privates</h4>");
				out.append("<form method=\"POST\" action=\""
					+ servletPrefix
					+ "game.test.GameTestServlet\">\n");
				out.append("<table><tr><td>Select Player</td><td><select name=Player>\n");
				for (int j=0; j<Player.numberOfPlayers(); j++) {
					out.append("<option value=\""+j+"\">"+Player.getPlayer(j).getName()+"\n");
				}
				out.append("</select></td></tr>");
				
				out.append("<tr><td>Select Private</td><td><select name=Private>\n");
				List privates = bank.getIpo().getPrivateCompanies();
				int freePrivates = 0;
				for (int j=0; j<privates.size(); j++) {
					PrivateCompanyI priv = (PrivateCompanyI) privates.get(j);
					out.append("<option value=\""+priv.getPrivateNumber()+"\">"+priv.getName()+"\n");
					freePrivates++;
				}
				out.append("</select></td></tr><tr><td>Price</td><td>")
					.append("<input type=text name=Price size=8></td></tr></table>\n");
				
				out.append ("<input type=submit name=BuyPrivate value=\"Buy Private\">");
				out.append("<input type=hidden name=Left value="+freePrivates+"></form>\n");
			
			} else if (phase == SR) {
				
				out.append ("<h4>Stock Round</h4>");
				out.append("<form method=\"POST\" action=\""
					+ servletPrefix
					+ "game.test.GameTestServlet\">\n");
				out.append("<table><tr><td align=right>Select Player</td><td><select name=Player>\n");
				for (int j=0; j<Player.numberOfPlayers(); j++) {
					out.append("<option value=\""+j+"\">"+Player.getPlayer(j).getName()+"\n");
				}
				out.append("</select></td></tr>");
				
				out.append("<tr><td align=right>Select Company</td><td><select name=Company>\n");
				List companies = game.getCompanyManager().getAllPublicCompanies();
				for (int j=0; j<companies.size(); j++) {
					company = (PublicCompanyI) companies.get(j);
					out.append("<option value=\""+company.getName()+"\">"+company.getName()+"\n");
				}
				out.append("</select>\n</td></tr><tr><td align=right>Start Price</td><td>");
				out.append("<select name=StartPrice>\n");
				Iterator it = game.getStockMarket().getStartSpaces().iterator();
				while (it.hasNext()) {
					price = ((StockSpaceI)it.next()).getPrice();
					out.append("<option value="+price+">"+price+"\n");
				}
				out.append("</select></td></tr>\n");
				
				out.append ("<tr><td align=right><input type=submit name=BuyIPO value=\"Buy from IPO\">");
				out.append ("</td><td><input type=submit name=BuyPool value=\"Buy from Pool\"><br>");
				out.append ("</td></tr><tr><td align=right><input type=submit name=Sell value=\"Sell\">");
				out.append("</td><td><select name=Number>\n");
				for (int k=1; k<=5; k++) out.append("<option value="+k+">"+k+"\n");
				out.append("</select></td></tr>\n");
				
				out.append ("<tr><td align=right><input type=submit name=GotoOR value=\"Start OR\"></tr></table></form>");

			} else if (phase == OR) {
				
				out.append ("<h4>Operating Round</h4>");
				out.append("<form method=\"POST\" action=\""
					+ servletPrefix
					+ "game.test.GameTestServlet\">\n");
				out.append("<table><tr><td align=right>Select Company</td><td><select name=Company>\n");
				List companies = game.getCompanyManager().getAllPublicCompanies();
				for (int j=0; j<companies.size(); j++) {
					company = (PublicCompanyI) companies.get(j);
					out.append("<option value=\""+company.getName()+"\">"+company.getName()+"\n");
				}
				out.append("</select>\n</td></tr><tr><td align=right>Amount</td><td>");
				out.append("<input type=text size=6 name=Amount></td></tr>\n");
				
				out.append ("<tr><td></td><td><input type=submit name=Spend value=\"Spend from Treasury\">");
				out.append ("</td></tr>\n<tr><td align=right><input type=submit name=PayOut value=\"Pay Out Revenue\"></td>\n");
				out.append ("<td><input type=submit name=Withhold value=\"Withhold revenue\">");
				out.append("</td></tr>\n");
				
				out.append("<tr><td align=right><select name=Private>\n");
				Iterator it = game.getCompanyManager().getAllPrivateCompanies().iterator();
				while (it.hasNext()) {
					PrivateCompanyI priv = (PrivateCompanyI) it.next();
					if (priv.getHolder().getOwner() instanceof Player) {
						out.append("<option value=\""+priv.getName()+"\">"+priv.getName()+"\n");
					}
				}
				out.append("</select></td><td><input type=submit name=BuyPrivate value=\"Buy Private\"></td></tr>\n");
				
				out.append ("<tr><td align=right><input type=submit name=NextOR value=\"Next OR\"></td>");
				out.append ("<td><input type=submit name=StartSR value=\"Next SR\"></td></tr></table></form>");
				
			}
			// Right upper part 2: log
			out.append ("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Log</h3>\n");
			out.append ("<p>"+Log.getBuffer().replaceAll("\n", "<br>"));
		
			
			// Right upper part 3: bank
			out.append ("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Bank</h3>\n");
			out.append ("<p><b>Cash: "+bank.getCash()+"</b>");
			
			// Lower left part: company status
			out.append ("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Companies</h3>\n");

			if (stockMarket.isGameOver()) {
				out.append("<b>Game over !</b>");
			} else {

				out.append("<table class=bordertable cellspacing=0 cellpadding=0>\n");
				out.append("<tr><th>Company</th><th>Par</th><th>Price</th><th>Cash</th><th>Revenue</th>")
					.append ("<th>Privates</th><th>IPO</th><th>Pool</th></tr>\n");
				CompanyManagerI compMgr = game.getCompanyManager();
				iterator = compMgr.getAllPublicCompanies().iterator();
				while (iterator.hasNext()) {
					company = (PublicCompanyI) iterator.next();
					companyName = company.getName();
					companyNumber = company.getCompanyNumber();
					out.append("<tr><td>" + companyName + "</td>");
					
					
					if (company.isClosed()) {

						out.append("<td colspan=5>is closed</td>");

					} else {
						
						if (company.getParPrice() != null) {
							out.append("<td>" + company.getParPrice().getPrice()
								+"</td><td>" + company.getCurrentPrice().getPrice()
								+"</td>");
						} else {
							out.append("<td colspan=2>Not started</td>");
						}
						if (company.hasFloated()) {
							out.append("<td>" + company.getCash()
								+"</td><td>"+company.getLastRevenue()+"</td><td>&nbsp;");
							Iterator it = company.getPortfolio().getPrivateCompanies().iterator();
							while (it.hasNext()) {
								out.append(((PrivateCompanyI)it.next()).getName()+" ");
							}
						} else {
							out.append("<td colspan=3>Not floated");
						}
						out.append("</td><td>" + bank.getIpo().countShares(company));
						out.append("</td><td>" + bank.getPool().countShares(company));
						out.append("</td>\n");
					}
					out.append ("</tr>\n");
				}
				out.append("</table>");
				
				out.append("</form>\n");
			}
			// Lower right part: Player status
			out.append ("</td><td colspan=2 valign=\"top\" class=\"bigtable\">\n<h3>Players</h3>\n");
			
			out.append ("<table border=0 cellspacing=0 class=bordertable><tr><th>Player</th><th>Cash</th><th>Privates</th>");
			List allCompanies = game.getCompanyManager().getAllPublicCompanies();
			Iterator it = allCompanies.iterator();
			int nComp = 0;
			while (it.hasNext()) {
				out.append("<th>"+((CompanyI)it.next()).getName()+"</th>");
				nComp++;
			}
			out.append ("</tr>\n");
			for (int j=0; j<Player.numberOfPlayers(); j++) {
				Player player = Player.getPlayer(j);
				out.append("<tr><td>" + player.getName())
					.append("</td><td>" + player.getCash())
					.append("</td><td>");
					
				// Private companies
				it = player.getPortfolio().getPrivateCompanies().iterator();
				while (it.hasNext()) {
					out.append(" "+((PrivateCompanyI)it.next()).getName());
				}
				out.append("&nbsp;</td>\n");
				
				// Public companies
				it = allCompanies.iterator();
				while (it.hasNext()) {
					String compName = ((PublicCompanyI)it.next()).getName();
					List certs;
					int share = 0;
					boolean president = false;
					if ((certs = player.getPortfolio().getCertificatesPerCompany(compName)) != null) {
						Iterator it2 = certs.iterator();
						while (it2.hasNext()) {
							cert = (CertificateI)it2.next();
							share += cert.getShare();
							if (cert.isPresident()) president = true;
						}
					}
					out.append("<td>"+(president ? "P" : "")+ share+"</td>"); 
				}
				out.append("</tr>");
				
			}
			out.append ("</table>");
				
				
			// End
			out.append ("</td></tr></table>\n");
				
		} else {
			out.append(
				"<form method=\"POST\" action=\""
					+ servletPrefix
					+ "game.test.GameTestServlet\">\n");
			out.append("Select a game: ");
			out.append("<select name=\"Game\" onChange=\"javascript:this.form.submit()\">\n");
			out.append("<option value=\"\">");
			out.append("<option value=\"1830\">1830\n");
			out.append("<option value=\"1856\">1856\n");
			out.append("<option value=\"1870\">1870\n");
			out.append("<option value=\"18AL\">18AL\n");
			out.append("</select>\n");
			
			out.append("</form>\n");

		}

		out.append("</body></html>\n");

		ServletOutputStream output = response.getOutputStream();
		output.println(out.toString());
		output.close();

	}

	private boolean hasValue(String s) {
		return s != null && !s.equals("");
	}

}
