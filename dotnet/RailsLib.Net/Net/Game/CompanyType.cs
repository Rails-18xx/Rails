using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Objects of this class represent a particular type of company, of which
 * typically multiple instances exist in a rails.game. Examples: "Private",
 * "Minor", "Major", "Mountain" etc. <p> This class contains common properties
 * of the companies of one type, and aids in configuring the companies by
 * reducing the need to repeatedly specify common properties with different
 * companies.
 */

namespace GameLib.Net.Game
{
    public class CompanyType : RailsAbstractItem
    {
        /*--- Class attributes ---*/

        /*--- Constants ---*/
        /** The name of the XML tag used to configure a company type. */
        public const string ELEMENT_ID = "CompanyType";

        /** The name of the XML attribute for the company type's name. */
        public const string NAME_TAG = "name";

        /** The name of the XML attribute for the company type's class name. */
        public const string CLASS_TAG = "class";

        /** The name of the XML tag for the "NoCertLimit" property. */
        public const string AUCTION_TAG = "Auction";

        /** The name of the XML tag for the "AllClose" tag. */
        public const string ALL_CLOSE_TAG = "AllClose";

        /*--- Instance attributes ---*/
        protected string className;
        protected int capitalization = PublicCompany.CAPITALIZE_FULL;

        protected List<ICompany> companies = new List<ICompany>();

        protected CompanyType(IRailsItem parent, string id, string className) : base(parent, id)
        {
            this.className = className;
        }

        /**
        * @param id Company type name ("Private", "Public", "Minor" etc.).
        * @param className Name of the class that will instantiate this type of
        * company.
        */
        public static CompanyType Create(CompanyManager parent, string id, string className)
        {
            return new CompanyType(parent, id, className);
        }

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {
            //No longer needed.
        }

        public void FinishConfiguration(GameManager gameManager)
        {

        }

        public ICompany CreateCompany(string id, Tag typeTag, Tag tag)
        {
            ICompany newCompany = null;
            try
            {
                newCompany = Configure.Create<ICompany>(className, this, id);
            }
            catch (Exception e)
            {
                throw new ConfigurationException(LocalText.GetText("ClassCannotBeInstantiated", className), e);
            }
            newCompany.InitType(this);
            newCompany.ConfigureFromXML(typeTag);
            newCompany.ConfigureFromXML(tag);
            companies.Add(newCompany);
            return newCompany;
        }

        /*--- Getters and setters ---*/
        /**
         * Get the name of the class that will implement this type of company.
         *
         * @return The full class name.
         */
        public string ClassName
        {
            get
            {
                return className;
            }
        }

        public List<ICompany> Companies
        {
            get
            {
                return companies;
            }
        }

        public void SetCapitalization(int mode)
        {
            this.capitalization = mode;
        }

        public void SetCapitalisation(string mode)
        {
            if (mode.Equals("full", StringComparison.OrdinalIgnoreCase))
            {
                this.capitalization = PublicCompany.CAPITALIZE_FULL;
            }
            else if (mode.Equals("incremental", StringComparison.OrdinalIgnoreCase))
            {
                this.capitalization = PublicCompany.CAPITALIZE_INCREMENTAL;
            }
        }

        public int GetCapitalisation()
        {
            return capitalization;
        }

    }
}
