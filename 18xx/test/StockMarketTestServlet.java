/*
 * This was an early example UI. Commented out because it uses a very old
 * version of the API and is no longer maintained.
 * 
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
 * public class StockMarketTestServlet extends HttpServlet {
 * 
 * private StockMarketI stockMarket = null; private Game game = null;
 * 
 * public void init(ServletConfig config) throws ServletException {
 * super.init(config); }
 * 
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
 * String companyName; int companyNumber; Iterator iterator, iterator2; int i;
 * List startSpaces; int price;
 * 
 * if (stockMarket == null) {
 */
/* Initialise */
/*
 * String gameName = request.getParameter("Game"); if (gameName != null &&
 * !gameName.equals("")) { game = Game.getInstance(); Game.initialise
 * (gameName); stockMarket = Game.getStockMarket(); } } else if
 * (hasValue(request.getParameter("ChangeGame"))) { game = null; stockMarket =
 * null; } else {
 */
/* Process the action performed. Note: companies not mentioned yet. */
// iterator = game.getCompanyManager().getAllCompanies().iterator();
/*
 * CompanyManagerI compMgr = Game.getCompanyManager(); iterator =
 * compMgr.getAllPublicNames().iterator(); while (iterator.hasNext()) {
 * //company = (CompanyI) iterator.next(); comp = (CompanyI)
 * compMgr.getPublicCompany((String) iterator.next()); if (!(comp instanceof
 * PublicCompanyI)) continue; company = (PublicCompanyI) comp; companyName =
 * company.getName(); companyNumber = company.getPublicNumber();
 * 
 * if (hasValue(request.getParameter("Sell_" + companyNumber))) { int number =
 * Integer.parseInt(request.getParameter("SellAmount_" + companyNumber));
 * System.out.println("Sold " + number + " of " + companyName);
 * stockMarket.sell(company, number); } else if
 * (hasValue(request.getParameter("SoldOut_" + companyNumber))) {
 * System.out.println(companyName + " is sold out");
 * stockMarket.soldOut(company); } else if
 * (hasValue(request.getParameter("PayOut_" + companyNumber))) {
 * System.out.println(companyName + " pays out full dividend");
 * stockMarket.payOut(company); } else if
 * (hasValue(request.getParameter("Withhold_" + companyNumber))) {
 * System.out.println(companyName + " withheld dividend");
 * stockMarket.withhold(company); } else if
 * (hasValue(request.getParameter("Start_" + companyNumber))) { price =
 * Integer.parseInt(request.getParameter("StartPrice_" + companyNumber));
 * startSpaces = stockMarket.getStartSpaces(); for (i = 0; i <
 * startSpaces.size(); i++) { if ((square = (StockSpace)
 * (startSpaces.get(i))).getPrice() == price) { company.setParPrice(square); } }
 * 
 * System.out.println( companyName + " is started at " +
 * company.getParPrice().getName() + ", par price " +
 * company.getParPrice().getPrice()); } } }
 */
/* Create the new Stock Chart HTML page */
// response.setContentType("text/html");
/* Read some properties */
/*
 * Properties prop = new Properties();
 * prop.load(this.getClass().getClassLoader().getResourceAsStream("testservlet.properties"));
 * String styleSheetPrefix = prop.getProperty("StyleSheetPrefix"); String
 * servletPrefix = prop.getProperty("ServletPrefix");
 * 
 * StringBuffer out = new StringBuffer();
 * 
 * out.append("<html><head>"); //out.append ("<link REL=\"STYLESHEET\"
 * HREF=\"http://localhost:8080/18xx/stockmarket.css\" TYPE=\"text/css\">\n");
 * out.append("<STYLE TYPE=\"text/css\">\n"); out.append( ".stockmarket0 {
 * background-color: white; border: 1px solid black; text-align:center;
 * vertical-align:top;}\n"); out.append( ".stockmarket1 { background-color:
 * yellow; border: 1px solid black; text-align:center; vertical-align:top;}\n");
 * out.append( ".stockmarket2 { background-color: orange; border: 1px solid
 * black; text-align:center; vertical-align:top;}\n"); out.append(
 * ".stockmarket3 { background-color: brown; border: 1px solid black;
 * text-align:center; vertical-align:top;}\n"); out.append( ".stockmarket_start {
 * background-color: white; border: 2px solid red; text-align:center;
 * vertical-align:top;}\n"); out.append("</STYLE>\n");
 * 
 * out.append("</head><body>\n");
 * 
 * if (stockMarket != null) {
 * 
 * out.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
 * out.append("<tr><td></td>"); for (col = 0; col <
 * stockMarket.getNumberOfColumns(); col++) { out.append( "<td align=\"center\">" +
 * Character.toString((char) ('A' + col)) + "</td>"); } out.append("</tr>\n");
 * 
 * for (row = 0; row < stockMarket.getNumberOfRows(); row++) { out.append("<tr><td>" +
 * (row + 1) + "</td>\n"); for (col = 0; col <
 * stockMarket.getNumberOfColumns(); col++) { square =
 * stockMarket.getStockSpace(row, col); if (square != null) { out.append( "<td
 * width=\"30\" height=\"40\" class=\"stockmarket"); if (square.isStart())
 * out.append ("_start"); else if (square.isNoBuyLimit()) out.append("3"); else
 * if (square.isNoHoldLimit()) out.append("2"); else if (square.isNoCertLimit())
 * out.append ("1"); else out.append ("0"); out.append ("\"");
 * 
 * if (square.isBelowLedge()) out.append(" style=\"border-top: 3px solid
 * red\""); else if (square.isLeftOfLedge()) out.append(" style=\"border-right:
 * 3px solid red\"");
 * 
 * out.append(">"); if (square.closesCompany()) { out.append("<small>Closes</small>"); }
 * else { out.append(square.getPrice()); } if (square.endsGame()) out.append("
 * <small>Game Over</small>"); iterator = square.getTokens().iterator(); while
 * (iterator.hasNext()) { company = (PublicCompany) iterator.next(); out.append( "<br><span
 * style=\"color:" + company.getFgColour() + ";background-color:" +
 * company.getBgColour() + "\">" + company.getName() + "</span>"); }
 * out.append("</td>\n"); } else { out.append("<td></td>\n"); } }
 * out.append("</tr>\n"); } out.append("</table><hr>");
 * 
 * if (stockMarket.isGameOver()) { out.append("<b>Game over !</b>"); } else {
 * 
 * //out.append ("<form method=\"POST\"
 * action=\"http://localhost:8080/18xx/servlet/game.test.StockMarketTestServlet\">\n");
 * out.append( "<form method=\"POST\" action=\"" + servletPrefix +
 * "game.test.StockMarketTestServlet\">\n"); out.append("<table
 * cellspacing=\"0\" cellpadding=\"0\">\n"); CompanyManagerI compMgr =
 * Game.getCompanyManager(); iterator =
 * compMgr.getAllPublicCompanies().iterator(); while (iterator.hasNext()) {
 * company = (PublicCompanyI) iterator.next(); companyName = company.getName();
 * companyNumber = company.getPublicNumber(); out.append("<tr><td>" +
 * companyName + ":&nbsp;&nbsp;</td><td>");
 * 
 * if (company.isClosed()) {
 * 
 * out.append("is closed"); } else if (company.hasFloated()) {
 * 
 * out.append( "<input type=\"submit\" name=\"Sell_" + companyNumber + "\"
 * value=\"Sell:\">\n"); out.append("<select name=\"SellAmount_" +
 * companyNumber + "\">\n"); out.append("<option value=\"1\" selected>1\n");
 * out.append("<option value=\"2\">2\n"); out.append("<option
 * value=\"3\">3\n"); out.append("<option value=\"4\">4\n"); out.append("<option
 * value=\"5\">5\n"); out.append("</select>\n");
 * 
 * out.append( " <input type=\"submit\" name=\"SoldOut_" + companyNumber + "\"
 * value=\"SoldOut\">\n"); out.append( " <input type=\"submit\" name=\"PayOut_" +
 * companyNumber + "\" value=\"PayOut\">\n"); out.append( " <input
 * type=\"submit\" name=\"Withhold_" + companyNumber + "\"
 * value=\"Withhold\">\n"); } else {
 * 
 * out.append( " <input type=\"submit\" name=\"Start_" + companyNumber + "\"
 * value=\"Start\"> at "); out.append("<select name=\"StartPrice_" +
 * companyNumber + "\">\n"); iterator2 =
 * stockMarket.getStartSpaces().iterator(); while (iterator2.hasNext()) { price =
 * ((StockSpaceI) iterator2.next()).getPrice(); out.append("<option value=\"" +
 * price + "\">" + price + "\n"); } out.append("</select>\n"); } out.append("</td></tr>\n"); }
 * out.append("</table>");
 * 
 * out.append("</form>\n"); } } else { out.append( "<form method=\"POST\"
 * action=\"" + servletPrefix + "game.test.StockMarketTestServlet\">\n");
 * out.append("Select a game: "); out.append("<select name=\"Game\"
 * onChange=\"javascript:this.form.submit()\">\n"); out.append("<option
 * value=\"\">"); out.append("<option value=\"1830\">1830\n"); out.append("<option
 * value=\"1856\">1856\n"); out.append("<option value=\"1870\">1870\n");
 * out.append("<option value=\"18AL\">18AL\n"); out.append("</select>\n");
 * 
 * out.append("</form>\n"); }
 * 
 * out.append("</body></html>\n");
 * 
 * ServletOutputStream output = response.getOutputStream();
 * output.println(out.toString()); output.close(); }
 * 
 * private boolean hasValue(String s) { return s != null && !s.equals(""); } }
 */