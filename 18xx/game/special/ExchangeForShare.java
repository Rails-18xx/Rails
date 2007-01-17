package game.special;

import org.w3c.dom.*;

import util.LocalText;
import util.Util;
import util.XmlUtils;

import game.*;

public class ExchangeForShare extends SpecialSRProperty
{

	/** The public company of which a share can be obtained. */
	String publicCompanyName;
	PublicCompanyI publicCompany;
	/** The share size */
	int share;

	public void configureFromXML(Element element) throws ConfigurationException
	{

		NodeList nl = element.getElementsByTagName("ExchangeForShare");
		if (nl == null || nl.getLength() == 0)
		{
			throw new ConfigurationException("<ExchangeForShare> tag missing");
		}
		Element exchEl = (Element) nl.item(0);

		NamedNodeMap nnp = exchEl.getAttributes();
		publicCompanyName = XmlUtils.extractStringAttribute(nnp, "company");
		if (!Util.hasValue(publicCompanyName))
			throw new ConfigurationException("ExchangeForShare: company name missing");
		share = XmlUtils.extractIntegerAttribute(nnp, "share", 10);
	}

	public boolean isExecutionable()
	{

		return GameManager.getCurrentPhase().isPrivateSellingAllowed()
				&& privateCompany.getPortfolio().getOwner() instanceof Player;
	}

	public boolean execute()
	{

		publicCompany = Game.getCompanyManager()
				.getPublicCompany(publicCompanyName);

		Portfolio portfolio = privateCompany.getPortfolio();
		Player player = null;
		String errMsg = null;
		boolean ipoHasShare = Bank.getIpo().ownsShare(publicCompany) >= share;
		boolean poolHasShare = Bank.getPool().ownsShare(publicCompany) >= share;

		while (true)
		{

			/* Check if the private is owned by a player */
			if (!(portfolio.getOwner() instanceof Player))
			{
				errMsg = LocalText.getText("PrivateIsNotOwnedByAPlayer",
						privateCompany.getName());
				break;
			}

			player = (Player) portfolio.getOwner();

			/* Check if a share is available */
			if (!ipoHasShare && !poolHasShare)
			{
				errMsg = LocalText.getText("NoSharesAvailable", publicCompanyName);
				break;
			}
			/* Check if the player has room for a share of this company */
			if (!player.mayBuyCompanyShare(publicCompany, 1))
			{
				// TODO: Not nice to use '1' here, should be percentage.
				// TODO: below message should include hold limit percentage.
				errMsg = LocalText.getText("WouldExceedHoldLimit", new String[] {
						player.getName(),
						publicCompanyName
					});
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			DisplayBuffer.add(
				LocalText.getText("CannotSwapPrivateForCertificate", new String[] {
					player.getName(),
					privateCompany.getName(),
					String.valueOf(share),
					publicCompanyName,
					errMsg
				}));
			return false;
		}

		Certificate cert = ipoHasShare ? Bank.getIpo()
				.findCertificate(publicCompany, false) : Bank.getPool()
				.findCertificate(publicCompany, false);
		player.buy(cert, 0);
		ReportBuffer.add(LocalText.getText("SwapsPrivateForCertificate", new String[] {
				player.getName(),
				privateCompany.getName(),
				String.valueOf(share),
				publicCompanyName
		}));
		setExercised();
		privateCompany.setClosed();

		return true;
	}

	/**
	 * @return Returns the privateCompany.
	 */
	public PrivateCompanyI getPrivateCompany()
	{
		return privateCompany;
	}

	/**
	 * @return Returns the publicCompanyName.
	 */
	public String getPublicCompanyName()
	{
		return publicCompanyName;
	}

	/**
	 * @return Returns the share.
	 */
	public int getShare()
	{
		return share;
	}
}
