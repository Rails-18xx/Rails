using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Special
{
    public sealed class ExchangeForShare : SpecialProperty
    {
        /** The public company of which a share can be obtained. */
        string publicCompanyName;

        /** The share size */
        int share;


        /**
         * Used by Configure (via reflection) only
         */
        public ExchangeForShare(IRailsItem parent, string id) : base(parent, id)
        {
        }

        
    override public void ConfigureFromXML(Tag tag)
        {
            base.ConfigureFromXML(tag);

        Tag swapTag = tag.GetChild("ExchangeForShare");
        if (swapTag == null)
            {
            throw new ConfigurationException("<ExchangeForShare> tag missing");
    }

    publicCompanyName = swapTag.GetAttributeAsString("company");
            if (string.IsNullOrEmpty(publicCompanyName))
            {
                throw new ConfigurationException(
                        "ExchangeForShare: company name missing");
            }
    share = swapTag.GetAttributeAsInteger("share", 10);
    }

override public bool IsExecutionable
{
            get
            {
                // FIXME: Check if this works correctly
                // IT is better to rewrite this check
                return ((PrivateCompany)originalCompany).Owner is Player;
            }
}

/**
 * @return Returns the publicCompanyName.
 */
public string PublicCompanyName
{
            get
            {
                return publicCompanyName;
            }
}

/**
 * @return Returns the share.
 */
public int Share
{
            get
            {
                return share;
            }
}

    override public string ToText()
{
    return "Swap " + originalCompany.Id + " for " + share
           + "% share of " + publicCompanyName;
}

    override public string ToMenu()
{
    return LocalText.GetText("SwapPrivateForCertificate",
            originalCompany.Id,
            share,
            publicCompanyName);
}

override public string GetInfo()
{
    return ToMenu();
}
    }
}
