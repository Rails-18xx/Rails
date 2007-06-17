package rails.game.model;

import java.util.List;

import rails.game.PrivateCompanyI;


public class PrivatesModel extends ModelObject
{

	private List<PrivateCompanyI> privatesList;

	public static final int SPACE = 0;
	public static final int BREAK = 1;

	public PrivatesModel(List<PrivateCompanyI> privatesList)
	{
		this.privatesList = privatesList;
	}

	public String getText()
	{

		StringBuffer buf = new StringBuffer("<html>");
		//Iterator it = privatesList.iterator();
		//PrivateCompanyI priv;
		//while (it.hasNext())
		for (PrivateCompanyI priv : privatesList)
		{
			//priv = (PrivateCompanyI) it.next();
			if (buf.length() > 6)
				buf.append(option == BREAK ? "<br>" : "&nbsp;");
			buf.append(priv.getName());
		}
		if (buf.length() > 6)
		{
			buf.append("</html>");
		}
		return buf.toString();

	}

}
