/*
 * This was an early example UI. Commented out because it uses a very old
 * version of the API and is no longer maintained.
 */
/*
 * package test;
 * 
 * import game.*;
 * 
 * import javax.servlet.*; import javax.servlet.http.*;
 * 
 * import java.io.IOException; import java.util.*;
 */

/**
 * @author Erik
 */

/*
 * public class GameTestServlet extends HttpServlet {
 * 
 * private static final int SELECTGAME = 0;
 * 
 * private static final int SELECTVARIANT = 1;
 * 
 * private static final int SELECTPLAYERS = 2;
 * 
 * private static final int BUYPRIVATES = 3;
 * 
 * private static final int SR = 4;
 * 
 * private static final int OR = 5;
 * 
 * private static final String servletName = "test.GameTestServlet";
 * 
 * private StockMarketI stockMarket = null;
 * 
 * private Game game = null;
 * 
 * private Bank bank = null;
 * 
 * private int process = SELECTGAME;
 * 
 * private boolean orStarted = false;
 * 
 * private String error;
 * 
 * private ArrayList playerList = new ArrayList();
 * 
 * private Player[] players = null;
 * 
 * private PlayerManager playerManager = null;
 * 
 * private CompanyManagerI companyManager = null;
 * 
 * private Round currentRound = null;
 * 
 * private StockRound stockRound = null;
 * 
 * private StartRoundI startRound = null;
 * 
 * private OperatingRound operatingRound = null;
 * 
 * private GameManager gameMgr = null;
 * 
 * public void init(ServletConfig config) throws ServletException {
 * super.init(config); }
 */
/*
 * (non-Javadoc)
 * 
 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
 * javax.servlet.http.HttpServletResponse)
 */

/*
 * protected void doGet(HttpServletRequest request, HttpServletResponse
 * response) throws ServletException, IOException { doWork(request, response); }
 */
/*
 * (non-Javadoc)
 * 
 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
 * javax.servlet.http.HttpServletResponse)
 */

/*
 * protected void doPost(HttpServletRequest request, HttpServletResponse
 * response) throws ServletException, IOException { doWork(request, response); }
 * 
 * protected void doWork(HttpServletRequest request, HttpServletResponse
 * response) throws ServletException, IOException {
 * 
 * int row, col; StockSpaceI square; CompanyI comp; PublicCompanyI company;
 * String companyName; PublicCertificateI cert; int companyNumber; Iterator
 * iterator, iterator2; int i; List startSpaces; int price;
 * 
 * Portfolio ipo = Bank.getIpo(); Portfolio pool = Bank.getPool();
 * 
 * if (process == SELECTGAME) {
 */
/* Initialise */
/*
 * String gameName = request.getParameter("Game"); if (gameName != null &&
 * !gameName.equals("")) { game = Game.getInstance(); Game.initialise(gameName);
 * stockMarket = Game.getStockMarket(); bank = Game.getBank(); companyManager =
 * Game.getCompanyManager();
 * 
 * if (GameManager.getVariants().size() > 1) { process = SELECTVARIANT; } else {
 * process = SELECTPLAYERS; } } else { System.out.println("No game selected!"); } }
 * else if (process == SELECTVARIANT) {
 * 
 * String varName = request.getParameter("Variant"); if
 * (GameManager.existVariant(varName)) { GameManager.setVariant(varName);
 * process = SELECTPLAYERS; } else { System.out.println ("No variant
 * selected!"); } } else if (process == SELECTPLAYERS) {
 * 
 * if (hasValue(request.getParameter("AddPlayer"))) { String newPlayer =
 * request.getParameter("Player"); if (hasValue(newPlayer)) {
 * playerList.add(newPlayer); } } else if
 * (hasValue(request.getParameter("StartGame"))) { // Give players their start
 * cash playerManager = Game.getPlayerManager(playerList); players =
 * playerManager.getPlayersArray();
 * 
 * GameManager.getInstance().startGame();
 * 
 * process = BUYPRIVATES; // From here on we will not use process // anymore.
 * gameMgr = GameManager.getInstance(); currentRound =
 * gameMgr.getCurrentRound(); System.out.println("First round is " +
 * currentRound.getClass().getName()); } } else {
 * 
 * currentRound = gameMgr.getCurrentRound();
 * 
 * if (currentRound instanceof StartRoundI) {
 * 
 * startRound = (StartRoundI) currentRound;
 * 
 * String playerName = request.getParameter("Player");
 * 
 * if (hasValue(request.getParameter("Buy"))) {
 * 
 * String itemName = request.getParameter("BuyItem"); startRound.buy(playerName,
 * itemName); } else if (hasValue(request.getParameter("Bid5")) ||
 * hasValue(request.getParameter("Bid"))) {
 * 
 * String itemName = request.getParameter("BidItem"); int bidAmount; if
 * (hasValue(request.getParameter("Bid5"))) { startRound.bid5(playerName,
 * itemName); } else if (hasValue(request.getParameter("Bid"))) { int amount =
 * Integer.parseInt(request .getParameter("Price")); startRound.bid(playerName,
 * itemName, amount); } } else if (hasValue(request.getParameter("Pass"))) {
 * 
 * startRound.pass(playerName); } else if
 * (hasValue(request.getParameter("SetPrice"))) {
 * 
 * String compName = request.getParameter("Company"); price = Integer
 * .parseInt(request.getParameter("StartPrice"));
 * 
 * startRound.setPrice(playerName, compName, price); } } else if (currentRound
 * instanceof StockRound) {
 * 
 * stockRound = (StockRound) currentRound;
 * 
 * String playerName = request.getParameter("Player"); Player player =
 * playerManager.getPlayerByName(playerName); String cmpy; Portfolio from =
 * null, to = null; String snumber = ""; int number = 0; String msg; String
 * sprice; // Starting a company if (hasValue(request.getParameter("Start"))) {
 * cmpy = request.getParameter("StartCompany"); price = hasValue(sprice =
 * request .getParameter("StartPrice")) ? Integer .parseInt(sprice) : 0;
 * stockRound.startCompany(playerName, cmpy, price);
 */
/*
 * Buying shares from the IPO (shortcuts: (1) price is not always Par price, (2)
 * sometimes initial shares are bought from company treasury)
 */
/*
 * } else if (hasValue(request.getParameter("BuyIPO"))) {
 * 
 * cmpy = request.getParameter("BuyIPOCompany"); stockRound.buyShare(playerName,
 * ipo, cmpy, 1); // Buying shares from the Pool } else if
 * (hasValue(request.getParameter("BuyPool"))) { cmpy =
 * request.getParameter("BuyPoolCompany"); stockRound.buyShare(playerName, pool,
 * cmpy, 1); // Selling shares to the Pool } else if
 * (hasValue(request.getParameter("Sell"))) { cmpy =
 * request.getParameter("SellCompany"); snumber =
 * request.getParameter("Number"); number = hasValue(snumber) ?
 * Integer.parseInt(snumber) : 1; stockRound.sellShares(playerName, cmpy,
 * number); } else if (hasValue(request.getParameter("Done"))) {
 * 
 * stockRound.done(playerName); } } else if (currentRound instanceof
 * OperatingRound) {
 * 
 * operatingRound = (OperatingRound) currentRound;
 * 
 * String compName = request.getParameter("Company"); company =
 * Game.getCompanyManager().getPublicCompany(compName);
 * 
 * String samount = request.getParameter("Amount"); int amount =
 * hasValue(samount) ? Integer.parseInt(samount) : 0;
 * 
 * if (hasValue(request.getParameter("LayTrack"))) {
 * operatingRound.layTrack(compName, amount); } else if
 * (hasValue(request.getParameter("LayToken"))) {
 * operatingRound.layToken(compName, amount); } else if
 * (hasValue(request.getParameter("BuyTrain"))) {
 * operatingRound.buyTrain(compName, amount); } else if
 * (hasValue(request.getParameter("SetRevenue"))) {
 * operatingRound.setRevenue(compName, amount); } else if
 * (hasValue(request.getParameter("Payout"))) {
 * operatingRound.fullPayout(compName); } else if
 * (hasValue(request.getParameter("Withhold"))) {
 * operatingRound.withholdPayout(compName); } else if
 * (hasValue(request.getParameter("Split"))) {
 * operatingRound.splitPayout(compName); } else if
 * (hasValue(request.getParameter("Done"))) { operatingRound.done(compName); }
 * else if (hasValue(request.getParameter("BuyPrivate"))) { String privName =
 * request.getParameter("Private"); price =
 * Integer.parseInt(request.getParameter("Price"));
 * operatingRound.buyPrivate(compName, privName, price); } else if
 * (hasValue(request.getParameter("ClosePrivate"))) { String privName =
 * request.getParameter("Private"); operatingRound.closePrivate(privName); } } }
 */
/* Create the new Stock Chart HTML page */
// response.setContentType("text/html");
/* Read some properties */
/*
 * Properties prop = new Properties();
 * prop.load(this.getClass().getClassLoader().getResourceAsStream(
 * "testservlet.properties")); //String styleSheetPrefix =
 * prop.getProperty("StyleSheetPrefix"); String servletPrefix =
 * prop.getProperty("ServletPrefix");
 * 
 * StringBuffer out = new StringBuffer();
 * 
 * out.append("<html><head>"); //out.append ("<link REL=\"STYLESHEET\" //
 * HREF=\"http://localhost:8080/18xx/stockmarket.css\" //
 * TYPE=\"text/css\">\n"); out.append("<STYLE TYPE=\"text/css\">\n");
 * out.append(".stockmarket0 { background-color: white; border: 1px solid black;
 * text-align:center; vertical-align:top;}\n"); out.append(".stockmarket1 {
 * background-color: yellow; border: 1px solid black; text-align:center;
 * vertical-align:top;}\n"); out.append(".stockmarket2 { background-color:
 * orange; border: 1px solid black; text-align:center; vertical-align:top;}\n");
 * out.append(".stockmarket3 { color:white; background-color: brown; border: 1px
 * solid black; text-align:center; vertical-align:top;}\n");
 * out.append(".stockmarket_start { background-color: white; border: 2px solid
 * red; text-align:center; vertical-align:top;}\n"); out.append(".bordertable
 * td,th { background-color: white; border: 1px solid black; text-align:center;
 * vertical-align:top;}\n"); out.append(".bigtable { border: 2px solid black;
 * text-align:left; vertical-align:top;}\n"); out.append("</STYLE>\n");
 * 
 * out.append("</head><body>\n");
 * 
 * if (process == SELECTGAME) {
 * 
 * out.append("<form method=\"POST\" action=\"" + servletPrefix + servletName +
 * "\">\n"); out.append("Select a game: "); String[] games = Game.getGames();
 * out.append("<select name=\"Game\"
 * onChange=\"javascript:this.form.submit()\">\n"); out.append("<option
 * value=\"\">-- select a game --"); for (i = 0; i < games.length; i++) {
 * out.append("<option value=\"").append(games[i]).append("\">")
 * .append(games[i]).append("\n"); } out.append("</select>\n");
 * 
 * out.append("</form>\n"); } else if (process == SELECTVARIANT) {
 * 
 * out.append("<form method=\"POST\" action=\"" + servletPrefix + servletName +
 * "\">\n"); out.append("Select a variant: "); List variants =
 * GameManager.getVariants(); out.append("<select name=\"Variant\"
 * onChange=\"javascript:this.form.submit()\">\n"); out.append("<option
 * value=\"\">-- select a variant --"); for (i = 0; i < variants.size(); i++) {
 * out.append("<option value=\"") .append(variants.get(i)).append("\">")
 * .append(variants.get(i)).append("\n"); } out.append("</select>\n");
 * 
 * out.append("</form>\n"); } else { // Left upper part: the stockmarket
 * out.append("<table><tr><td rowspan=3 colspan=2 valign=\"top\" class=\"bigtable\">\n<h3>Stock
 * Market</h3>\n");
 * 
 * out.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
 * out.append("<tr><td></td>"); for (col = 0; col <
 * stockMarket.getNumberOfColumns(); col++) { out.append("<td align=\"center\">" +
 * Character.toString((char) ('A' + col)) + "</td>"); } out.append("</tr>\n");
 * 
 * for (row = 0; row < stockMarket.getNumberOfRows(); row++) { out.append("<tr><td>" +
 * (row + 1) + "</td>\n"); for (col = 0; col <
 * stockMarket.getNumberOfColumns(); col++) { square =
 * stockMarket.getStockSpace(row, col); if (square != null) { out.append("<td
 * width=\"30\" height=\"40\" class=\"stockmarket"); if (square.isStart())
 * out.append("_start"); else if (square.isNoBuyLimit()) out.append("3"); else
 * if (square.isNoHoldLimit()) out.append("2"); else if (square.isNoCertLimit())
 * out.append("1"); else out.append("0"); out.append("\"");
 * 
 * if (square.isBelowLedge()) out.append(" style=\"border-top: 3px solid
 * red\""); else if (square.isLeftOfLedge()) out.append(" style=\"border-right:
 * 3px solid red\"");
 * 
 * out.append(">"); if (square.closesCompany()) { out.append("<small>Closes</small>"); }
 * else { out.append(square.getPrice()); } if (square.endsGame()) out.append("
 * <small>Game Over</small>"); iterator = square.getTokens().iterator(); while
 * (iterator.hasNext()) { company = (PublicCompany) iterator.next();
 * out.append("<br><span style=\"color:" + company.getHexFgColour() +
 * ";background-color:" + company.getHexBgColour() + "\">" + company.getName() + "</span>"); }
 * iterator = square.getFixedStartPrices().iterator(); while
 * (iterator.hasNext()) { company = (PublicCompany) iterator.next(); if
 * (!company.hasStarted()) out.append("<br><small>")
 * .append(company.getName()) .append("</small>"); } out.append("</td>\n"); }
 * else { out.append("<td></td>\n"); } } out.append("</tr>\n"); }
 * out.append("</table>"); // Right upper part 1: actions out .append("</td><td valign=\"top\" class=\"bigtable\">\n<h3>Actions</h3>\n");
 * 
 * if (gameMgr != null) { currentRound = gameMgr.getCurrentRound(); process =
 * -1; }
 * 
 * if (process == SELECTPLAYERS) {
 * 
 * out.append("<h4>Selecting players</h4>"); out.append("<table
 * class=\"bordertable\" cellspacing=0>"); for (int j = 0; j <
 * playerList.size(); j++) { out.append("<tr><td>" + (j + 1) + "</td><td>" +
 * ((String) playerList.get(j)) + "</td></tr>\n"); } out.append("</table><p>");
 * out.append("<form method=\"POST\" action=\"" + servletPrefix + servletName +
 * "\">\n"); out.append("<input type=\"text\" name=\"Player\" size=\"16\">")
 * .append("&nbsp;&nbsp;") .append("<input type=\"submit\" name=\"AddPlayer\"
 * value=\"Add Player\">"); out.append(" <input type=\"submit\"
 * name=\"StartGame\" value=\"Start Game\">"); out.append("</form>"); } else if
 * (currentRound instanceof StartRoundI) {
 * 
 * startRound = (StartRoundI) currentRound; int step = startRound.nextStep();
 * 
 * if (step == StartRoundI.SET_PRICE) {
 * 
 * PublicCompanyI currCo = startRound.getCompanyNeedingPrice(); Player player =
 * startRound.getCurrentPlayer(); out.append("<h4>").append(player.getName()).append( ":
 * set par price for company ").append( currCo.getName()).append("</h4><p>");
 * out.append("<form method=\"POST\" action=\"" + servletPrefix + servletName +
 * "\">\n"); out.append("<input type=hidden name=Player value=\"")
 * .append(startRound.getCurrentPlayer().getName()) .append("\">\n");
 * out.append("<input type=hidden name=Company value=\"")
 * .append(currCo.getName()).append("\">\n"); out.append("<select
 * name=StartPrice>\n"); Iterator it = Game.getStockMarket().getStartSpaces()
 * .iterator(); while (it.hasNext()) { price = ((StockSpaceI)
 * it.next()).getPrice(); out.append("<option value=" + price + ">" + price +
 * "\n"); } out.append("</select> <input type=submit name=SetPrice value=Set></form>\n"); }
 * else {
 * 
 * out.append("<h4>Buying Privates - ").append(
 * startRound.getCurrentPlayer().getName()).append( "'s turn</h4>");
 * out.append("<form method=\"POST\" action=\"" + servletPrefix + servletName +
 * "\">\n"); out.append("<input type=hidden name=Player value=\"")
 * .append(startRound.getCurrentPlayer().getName()) .append("\">\n");
 * out.append("<table>");
 * 
 * StartItem[] items = startRound.getBuyableItems();
 * 
 * if (items.length > 0) { out.append("<tr><td align=right><input type=submit
 * name=Buy value=Buy>") .append("</td><td>");
 * 
 * if (items.length == 1) { out.append("<input type=hidden name=BuyItem
 * value=\"") .append(startRound.getBuyableItems()[0].getName())
 * .append("\">\n") .append(startRound.getBuyableItems()[0].getName()) .append("
 * for ") .append(Bank.format(startRound.getBuyableItems()[0] .getBasePrice())); }
 * else { out.append("<select name=BuyItem>"); for (i=0; i<items.length; i++) {
 * out.append("<option value=\"") .append(items[i].getName()) .append("\">")
 * .append(items[i].getName()) .append(" for ")
 * .append(items[i].getBasePrice()); } } out.append("</td></tr>\n");
 * 
 * out.append("<tr><td colspan=2><hr></td></tr>\n"); }
 * 
 * items = startRound.getBiddableItems(); if (items.length > 0) { if
 * (items.length > 1) {
 * 
 * out.append("<tr><td align=right>Select for bid</td><td><select
 * name=BidItem>\n");
 * 
 * for (int j = 0; j < items.length; j++) { StartItem item = items[j];
 * out.append("<option value=\"" + item.getName() + "\">" + item.getName() + "
 * (min. " + (item.getMinimumBid()) + ")\n"); } out.append("</select></td></tr>\n");
 *  } else if (items.length == 1) { out.append("<input type=hidden name=BidItem
 * value=") .append(items[0].getName()) .append("><tr><td align=right>Bid on ")
 * .append(items[0].getName()) .append("</td></tr>\n"); } out.append("<tr><td align=right>\n")
 * .append("<input type=submit name=Bid5 value=\"Bid +5\"></td><td>")
 * .append("<input type=submit name=Bid value=Bid>") .append("<input type=text
 * name=Price size=6>") .append("</td></tr>\n");
 * 
 * out.append("<tr><td colspan=2><hr></td></tr>\n"); }
 * 
 * if (step != StartRound.BUY) { out.append("<tr><td align=right>") .append("<input
 * type=submit name=Pass value=\"Pass\"></td></tr>\n"); }
 * 
 * out.append("</table>"); } } else if (currentRound instanceof StockRound) {
 * 
 * stockRound = (StockRound) currentRound;
 * 
 * out.append("<h4>Stock Round ") .append(stockRound.getStockRoundNumber())
 * .append(", ") .append(stockRound.getCurrentPlayer().getName()) .append("'s
 * turn</h4><p>\n"); out.append("<form method=\"POST\" action=\"").append(
 * servletPrefix).append(servletName).append("\">\n");
 * 
 * out.append("<input type=hidden name=Player value=\"").append(
 * stockRound.getCurrentPlayer().getName()).append( "\"><table>\n");
 * 
 * List companies = Game.getCompanyManager() .getAllPublicCompanies();
 * 
 * boolean askStartPrice = true; if (stockRound.mayCurrentPlayerBuyAtAll()) {
 * out.append("<tr><td align=right><input type=submit name=Start
 * value=\"Start Company\"></td>") .append("<td><select
 * name=StartCompany>\n"); for (int j = 0; j < companies.size(); j++) { company =
 * (PublicCompanyI) companies.get(j); if
 * (stockRound.isCompanyStartable(company.getName())) { out.append("<option
 * value=\"") .append(company.getName()) .append("\">")
 * .append(company.getName()); if (company.getParPrice() != null) { out.append("
 * at ") .append(Bank.format(company.getParPrice().getPrice())); askStartPrice =
 * false; } out.append("\n"); } } out.append("</select>\n</td>"); if
 * (askStartPrice) { out.append("<td> at <select name=StartPrice>\n"); Iterator
 * it = Game.getStockMarket().getStartSpaces() .iterator(); while (it.hasNext()) {
 * price = ((StockSpaceI) it.next()).getPrice(); out.append("<option value=" +
 * price + ">" + price + "\n"); } out.append("</select></td>"); }
 * out.append("</tr>\n");
 * 
 * out.append("<tr><td colspan=2><hr></td></tr>\n");
 * 
 * out.append("<tr><td align=right><input type=submit name=BuyIPO value=\"Buy
 * from IPO\"></td>") .append("<td><select name=BuyIPOCompany>\n"); for (int
 * j = 0; j < companies.size(); j++) { company = (PublicCompanyI)
 * companies.get(j); if (stockRound.isCompanyBuyable(company.getName(), ipo)) {
 * out.append("<option value=\"") .append(company.getName()) .append("\">")
 * .append(company.getName()) .append("\n"); } } out.append("</select></td></tr>\n");
 * 
 * out.append("</td><td align=right><input type=submit name=BuyPool
 * value=\"Buy from Pool\"></td>") .append("<td><select
 * name=BuyPoolCompany>\n"); for (int j = 0; j < companies.size(); j++) {
 * company = (PublicCompanyI) companies.get(j); if (stockRound
 * .isCompanyBuyable(company.getName(), pool)) { out.append("<option value=\"")
 * .append(company.getName()) .append("\">") .append(company.getName())
 * .append("\n"); } } out.append("</select></td></tr>\n");
 * 
 * out.append("<tr><td colspan=2><hr></td></tr>\n"); }
 * 
 * if (stockRound.mayCurrentPlayerSellAtAll()) { out.append("<tr><td align=right><input
 * type=submit name=Sell value=\"Sell\">"); out.append("<select
 * name=Number>\n"); for (int k = 1; k <= 5; k++) out.append("<option value=" +
 * k + ">" + k + "\n"); out.append("</select></td>\n").append( "<td><select
 * name=SellCompany>\n"); for (int j = 0; j < companies.size(); j++) { company =
 * (PublicCompanyI) companies.get(j); if
 * (stockRound.isCompanySellable(company.getName())) { out.append("<option
 * value=\"").append( company.getName()).append("\">").append(
 * company.getName()).append("\n"); } } out.append("</select></td></tr>\n");
 * 
 * out.append("<tr><td colspan=2><hr></td></tr>\n"); }
 * 
 * out.append("<tr><td align=right><input type=submit name=Done
 * value=\"Pass/Done\"></td></tr></table></form>"); } else if (currentRound
 * instanceof OperatingRound) {
 * 
 * operatingRound = (OperatingRound) currentRound;
 * 
 * PublicCompanyI currCo = operatingRound.getOperatingCompany(); Player player =
 * currCo.getPresident(); out.append("<h4>Operating Round ").append(
 * operatingRound.getCompositeORNumber()).append(" - ")
 * .append(currCo.getName()).append(" turn, ").append(
 * player.getName()).append(" is President</h4>"); out.append("<form
 * method=\"POST\" action=\"" + servletPrefix + servletName + "\">\n");
 * out.append("<input type=hidden name=Company value=\"").append(
 * operatingRound.getOperatingCompany().getName()).append( "\"><table>\n");
 * 
 * int step = operatingRound.getStep(); if (step ==
 * OperatingRound.STEP_LAY_TRACK) { out.append("<tr><td align=right>Track
 * Laying Cost</td>") .append("<td><input type=text size=6 name=Amount
 * value=0>") .append(" <input type=submit name=LayTrack value=Go></td></tr>\n"); }
 * else if (step == OperatingRound.STEP_LAY_TOKEN) { out.append("<tr><td align=right>Token
 * Laying Cost</td>") .append("<td><input type=text size=6 name=Amount
 * value=0>") .append(" <input type=submit name=LayToken value=Go></td></tr>\n"); }
 * else if (step == OperatingRound.STEP_CALC_REVENUE) { out.append("<tr><td align=right>Revenue
 * is</td>") .append("<td><input type=text size=6 name=Amount value=0>")
 * .append(" <input type=submit name=SetRevenue value=Go></td></tr>\n"); }
 * else if (step == OperatingRound.STEP_PAYOUT) { out.append("<tr><td>Revenue
 * allocation <input type=submit name=Payout value=\"Pay Out\"></td><td align=left>");
 * if (currCo.isSplitAllowed()) { out.append("<input type=submit name=Split
 * value=\"Split\">"); } out.append("<input type=submit name=Withhold
 * value=\"Withhold\"></td></tr>"); } else if (step ==
 * OperatingRound.STEP_BUY_TRAIN) { out.append("<tr><td align=right>Train
 * Buying Cost</td>") .append("<td><input type=text size=6 name=Amount
 * value=0>") .append(" <input type=submit name=BuyTrain value=Go></td></tr>\n"); }
 * 
 * if (currCo.canBuyPrivates()) { out.append("<tr><td colspan=2><hr></td></tr>\n");
 * 
 * out.append("<tr><td align=right>Private <select name=Private>\n");
 * Iterator it = Game.getCompanyManager().getAllPrivateCompanies() .iterator();
 * while (it.hasNext()) { PrivateCompanyI priv = (PrivateCompanyI) it.next(); if
 * (priv.getPortfolio().getOwner() instanceof Player) { out.append("<option
 * value=\"" + priv.getName() + "\">" + priv.getName() + "\n"); } } out.append("</select></td><td>
 * for <input type=text size=6 name=Price>") .append(" <input type=submit
 * name=BuyPrivate value=\"Buy\">") .append(" <input type=submit
 * name=ClosePrivate value=\"Close\"></td></tr>\n"); }
 * 
 * out.append("<tr><td align=right>"); if (step >=
 * OperatingRound.STEP_BUY_TRAIN) { out.append("<input type=submit name=Done
 * value=\"Done\">"); } out.append("</td></tr></table></form>"); } // Right
 * upper part 2: log out .append("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Messages</h3>\n");
 * String errMsg = Log.getErrorBuffer(); if (hasValue(errMsg)) { out.append("<p><font
 * color=red><b>").append( errMsg.replaceAll(":", ":<br>&nbsp;&nbsp;")).append( "</b></font>"); }
 * 
 * out.append("<p>").append( Log.getMessageBuffer().replaceAll("\n", "<br>")); //
 * Right upper part 3: bank out .append("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Bank</h3>\n");
 * out.append("<p><b>Cash: " + bank.getFormattedCash() + "</b>"); // Lower
 * left part: company status out .append("</td></tr><tr><td valign=\"top\" class=\"bigtable\">\n<h3>Companies</h3>\n");
 * 
 * if (stockMarket.isGameOver()) { out.append("<b>Game over !</b>"); } else {
 * 
 * out.append("<table class=bordertable cellspacing=0 cellpadding=0>\n");
 * out.append("<tr><th>Company</th><th>Par</th><th>Price</th><th>Cash</th><th>Revenue</th>")
 * .append("<th>Privates</th><th>IPO</th><th>Pool</th></tr>\n");
 * CompanyManagerI compMgr = Game.getCompanyManager(); iterator =
 * compMgr.getAllPublicCompanies().iterator(); while (iterator.hasNext()) {
 * company = (PublicCompanyI) iterator.next(); companyName = company.getName();
 * companyNumber = company.getCompanyNumber(); out.append("<tr><td>" +
 * companyName + "</td>");
 * 
 * if (company.isClosed()) {
 * 
 * out.append("<td colspan=5>is closed</td>"); } else {
 * 
 * if (!company.hasStockPrice()) { out.append("<td colspan=2>&nbsp;</td>"); }
 * else if (company.getParPrice() != null) { out.append("<td>").append(
 * company.hasParPrice() ? Bank.format(company .getParPrice().getPrice()) :
 * "&nbsp;").append("</td><td>") .append( Bank.format(company
 * .getCurrentPrice() .getPrice())).append( "</td>"); } else { out.append("<td colspan=2>Not
 * started</td>"); } if (company.hasFloated()) { out.append("<td>" +
 * company.getFormattedCash() + "</td><td>" +
 * Bank.format(company.getLastRevenue()) + "</td><td>&nbsp;"); Iterator it =
 * company.getPortfolio() .getPrivateCompanies().iterator(); while
 * (it.hasNext()) { out.append(((PrivateCompanyI) it.next()) .getName() + " "); } }
 * else { out.append("<td colspan=3>Not floated"); } out.append("</td><td>" +
 * Bank.getIpo().ownsShare(company)); out.append("%</td><td>" +
 * Bank.getPool().ownsShare(company)); out.append("%</td>\n"); } out.append("</tr>\n"); }
 * out.append("</table>");
 * 
 * out.append("</form>\n"); } // Lower right part: Player status
 * 
 * boolean includeMinors = companyManager.getCompaniesByType("Minor")!= null;
 * 
 * out.append("</td><td colspan=2 valign=\"top\" class=\"bigtable\">\n<h3>Players</h3>\n");
 * 
 * out.append("<table border=0 cellspacing=0 class=bordertable><tr><th>Player</th><th>Cash</th><th>Privates</th>"); //
 * NOTE: The below code has hardcoded behaviour for company type // "Minor". if
 * (includeMinors) out.append("<th>Minors</th>"); List allCompanies =
 * Game.getCompanyManager() .getAllPublicCompanies(); Iterator it =
 * allCompanies.iterator(); while (it.hasNext()) { company = (PublicCompanyI)
 * it.next(); if (!company.getType().getName().equals("Minor")) out.append("<th>" +
 * company.getName() + "</th>"); } out.append("<th>Worth</th></tr>\n");
 * for (int j = 0; j < (players != null ? players.length : -1); j++) { Player
 * player = players[j]; out.append("<tr><td>" + player.getName()); if (player ==
 * GameManager.getPriorityPlayer()) out.append(" *P*"); out.append("</td><td>" +
 * player.getFormattedCash()); if (currentRound instanceof StartRoundI &&
 * player.getCash() > player.getUnblockedCash()) { out.append(" (").append(
 * Bank.format(player.getUnblockedCash())).append(")"); } out.append("</td><td>"); //
 * Private companies it =
 * player.getPortfolio().getPrivateCompanies().iterator(); while (it.hasNext()) {
 * out.append(" " + ((PrivateCompanyI) it.next()).getName()); } // Bids if
 * (currentRound instanceof StartRoundI) { startRound = (StartRoundI)
 * currentRound; it = startRound.getStartPacket().getItems().iterator();
 * StartItem item; StartItem.Bid bid; while (it.hasNext()) { item = (StartItem)
 * it.next(); if (!item.isSold() && (bid =
 * item.getBidForPlayer(player.getName())) != null) { out.append("
 * (").append(item.getName()) .append(":&nbsp;").append(bid.getAmount())
 * .append(")"); } } } out.append("&nbsp;</td>\n"); // Minors (1835-style) if
 * (includeMinors) { out.append("<td>"); it =
 * player.getPortfolio().getCertificates().iterator(); while (it.hasNext()) {
 * company = ((PublicCertificate) it.next()).getCompany(); if
 * (company.getType().getName().equals("Minor")) out.append(" " +
 * company.getName()); } out.append("&nbsp;</td>"); } // Public companies it =
 * allCompanies.iterator(); while (it.hasNext()) { company = (PublicCompanyI)
 * it.next(); if (company.getType().getName().equals("Minor")) continue; String
 * compName = company.getName(); List certs; int share = 0; boolean president =
 * false; if ((certs = player.getPortfolio()
 * .getCertificatesPerCompany(compName)) != null) { Iterator it2 =
 * certs.iterator(); while (it2.hasNext()) { cert = (PublicCertificateI)
 * it2.next(); share += cert.getShare(); if (cert.isPresidentShare()) president =
 * true; } } out.append("<td>" + (president ? "P" : "") + share + "%</td>"); }
 * out.append("<td>").append(player.getFormattedWorth()).append( "</td></tr>"); }
 * out.append("</table>"); // End out .append("</td></tr><tr><td></td><td width=\"10%\"></td><td></td></tr></table>\n"); }
 * 
 * out.append("</body></html>\n");
 * 
 * ServletOutputStream output = response.getOutputStream();
 * output.println(out.toString()); output.close(); }
 * 
 * private boolean hasValue(String s) { return s != null && !s.equals(""); } }
 */
