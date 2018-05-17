using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class CompanyManager : RailsManager, IConfigurable
    {
        /**
         * This is the name by which the CompanyManager should be registered with
         * the ComponentManager.
         */
        public const string COMPONENT_NAME = "CompanyManager";

        /** A List with all private companies */
        private List<PrivateCompany> lPrivateCompanies = new List<PrivateCompany>();

        /** A List with all public companies */
        private List<PublicCompany> lPublicCompanies = new List<PublicCompany>();

        /** A map with all private companies by name */
        private Dictionary<string, PrivateCompany> mPrivateCompanies =
                new Dictionary<string, PrivateCompany>();

        /** A map with all public (i.e. non-private) companies by name */
        private Dictionary<string, PublicCompany> mPublicCompanies =
                new Dictionary<string, PublicCompany>();

        /** A map of all type names to maps of companies of that type by name */
        // TODO Redundant, current usage can be replaced.
        private Dictionary<string, Dictionary<string, ICompany>> mCompaniesByTypeAndName =
                new Dictionary<string, Dictionary<string, ICompany>>();

        /** A list of all company types */
        private List<CompanyType> lCompanyTypes = new List<CompanyType>();

        /** A list of all start packets (usually one) */
        protected List<StartPacket> startPackets = new List<StartPacket>();
        /** A map of all start packets, keyed by name. Default name is "Initial" */
        private Dictionary<string, StartPacket> startPacketMap = new Dictionary<string, StartPacket>();

        /** A map to enable translating aliases to names */
        protected Dictionary<string, string> aliases = null;

        private int numberOfPublicCompanies = 0;

        protected static Logger<CompanyManager> log = new Logger<CompanyManager>();

        protected GameManager gameManager;

        /**
         * Used by Configure (via reflection) only
         */
        public CompanyManager(RailsRoot parent, string id) : base(parent, id)
        {
        }

        /*
         * NOTES: 1. we don't have a map over all companies, because some games have
         * duplicate names, e.g. B&O in 1830. 2. we have both a map and a list of
         * private/public companies to preserve configuration sequence while
         * allowing direct access.
         */

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {
            gameManager = GetRoot.GameManager;

            /** A map with all company types, by type name */
            // Localized here as it has no permanent use
            Dictionary<string, CompanyType> mCompanyTypes = new Dictionary<string, CompanyType>();

            //NEW//
            Dictionary<string, Tag> typeTags = new Dictionary<string, Tag>();

            foreach (Tag compTypeTag in tag.GetChildren(CompanyType.ELEMENT_ID))
            {
                // Extract the attributes of the Component
                string name = compTypeTag.GetAttributeAsString(CompanyType.NAME_TAG);
                if (name == null)
                {
                    throw new ConfigurationException(
                            LocalText.GetText("UnnamedCompanyType"));
                }
                string className = compTypeTag.GetAttributeAsString(CompanyType.CLASS_TAG);
                if (className == null)
                {
                    throw new ConfigurationException(LocalText.GetText("CompanyTypeHasNoClass", name));
                }
                if (mCompanyTypes.ContainsKey(name))
                {
                    throw new ConfigurationException(LocalText.GetText(
                            "CompanyTypeConfiguredTwice", name));
                }

                CompanyType companyType = CompanyType.Create(this, name, className);
                mCompanyTypes[name] = companyType;
                lCompanyTypes.Add(companyType);

                // Further parsing is done within CompanyType
                companyType.ConfigureFromXML(compTypeTag);

                //NEW//
                typeTags[name] = compTypeTag;
            }

            /* Read and configure the companies */
            foreach (Tag companyTag in tag.GetChildren(ICompanyConsts.COMPANY_ELEMENT_ID))
            {
                // Extract the attributes of the Component
                string name = companyTag.GetAttributeAsString(ICompanyConsts.COMPANY_NAME_TAG);
                if (name == null)
                {
                    throw new ConfigurationException(
                            LocalText.GetText("UnnamedCompany"));
                }
                string type =
                        companyTag.GetAttributeAsString(ICompanyConsts.COMPANY_TYPE_TAG);
                if (type == null)
                {
                    throw new ConfigurationException(LocalText.GetText(
                            "CompanyHasNoType", name));
                }
                CompanyType cType;
                mCompanyTypes.TryGetValue(type, out cType);
                if (cType == null)
                {
                    throw new ConfigurationException(LocalText.GetText(
                            "CompanyHasUnknownType", name, type));
                }
                try
                {

                    //NEW//Company company = cType.createCompany(name, companyTag);
                    Tag typeTag = typeTags[type];
                    ICompany company = cType.CreateCompany(name, typeTag, companyTag);

                    /* Private or public */
                    if (company is PrivateCompany)
                    {
                        mPrivateCompanies[name] = (PrivateCompany)company;
                        lPrivateCompanies.Add((PrivateCompany)company);

                    }
                    else if (company is PublicCompany)
                    {
                        ((PublicCompany)company).SetIndex(numberOfPublicCompanies++);
                        mPublicCompanies[name] = (PublicCompany)company;
                        lPublicCompanies.Add((PublicCompany)company);
                    }
                    /* By type and name */
                    if (!mCompaniesByTypeAndName.ContainsKey(type))
                    {
                        mCompaniesByTypeAndName[type] = new Dictionary<string, ICompany>();
                    }
                    (mCompaniesByTypeAndName[type])[name] = company;

                    string alias = company.Alias;
                    if (alias != null) CreateAlias(alias, name);

                }
                catch (Exception)
                {
                    throw new ConfigurationException(LocalText.GetText(
                            "ClassCannotBeInstantiated", cType.ClassName));
                }

            }

            /* Read and configure the start packets */
            List<Tag> packetTags = tag.GetChildren("StartPacket");

            if (packetTags != null)
            {
                foreach (Tag packetTag in tag.GetChildren("StartPacket"))
                {
                    // Extract the attributes of the Component
                    string name = packetTag.GetAttributeAsString("name", StartPacket.DEFAULT_ID);
                    string roundClass = packetTag.GetAttributeAsString("roundClass");
                    if (roundClass == null)
                    {
                        throw new ConfigurationException(LocalText.GetText(
                                "StartPacketHasNoClass", name));
                    }

                    StartPacket sp = StartPacket.Create(this, name, roundClass);
                    startPackets.Add(sp);
                    startPacketMap[name] = sp;

                    sp.ConfigureFromXML(packetTag);
                }
            }
        }

        // Post XML parsing initializations
        public void FinishConfiguration(RailsRoot root)
        {
            foreach (PublicCompany comp in lPublicCompanies)
            {
                comp.FinishConfiguration(root);
            }
            foreach (PrivateCompany comp in lPrivateCompanies)
            {
                comp.FinishConfiguration(root);
            }

        }

        public void InitStartPackets(GameManager gameManager)
        {
            // initialize startPackets
            // TODO: Check if this still works in 2.0
            foreach (StartPacket packet in startPackets)
            {
                packet.Init(gameManager);
            }
        }

        private void CreateAlias(string alias, string name)
        {
            if (aliases == null)
            {
                aliases = new Dictionary<string, string>();
            }
            aliases[alias] = name;
        }

        public string CheckAlias(string alias)
        {
            if (aliases != null && aliases.ContainsKey(alias))
            {
                return aliases[alias];
            }
            else
            {
                return alias;
            }
        }

        public string CheckAliasInCertId(string certId)
        {
            string[] parts = certId.Split('-');
            string realName = CheckAlias(parts[0]);
            if (!parts[0].Equals(realName))
            {
                return realName + "-" + parts[1];
            }
            else
            {
                return certId;
            }
        }
        /**
         * @see net.sf.rails.game.CompanyManager#getCompany(java.lang.string)
         *
         */
        public PrivateCompany GetPrivateCompany(string name)
        {
            if (mPrivateCompanies.ContainsKey(name))
                return mPrivateCompanies[name];
            else
                return null;
        }

        public PublicCompany GetPublicCompany(string name)
        {
            string alias = CheckAlias(name);
            if (alias != null && mPublicCompanies.ContainsKey(alias))
            {
                return mPublicCompanies[alias];
            }
            else
            {
                return null;
            }
        }

        public List<PrivateCompany> GetAllPrivateCompanies()
        {
            return lPrivateCompanies;
        }

        public List<PublicCompany> GetAllPublicCompanies()
        {
            return lPublicCompanies;
        }

        public List<CompanyType> GetCompanyTypes()
        {
            return lCompanyTypes;
        }

        public ICompany GetCompany(string type, string name)
        {

            if (mCompaniesByTypeAndName.ContainsKey(type))
            {
                return (mCompaniesByTypeAndName[type])[CheckAlias(name)];
            }
            else
            {
                return null;
            }
        }

        public void CloseAllPrivates()
        {
            if (lPrivateCompanies == null) return;
            foreach (PrivateCompany priv in lPrivateCompanies)
            {
                if (priv.IsCloseable()) // check if private is closeable
                    priv.SetClosed();
            }
        }

        public List<PrivateCompany> GetPrivatesOwnedByPlayers()
        {
            List<PrivateCompany> privatesOwnedByPlayers = new List<PrivateCompany>();

            foreach (PrivateCompany priv in GetAllPrivateCompanies())
            {
                if (priv.Owner is Player)
                {
                    privatesOwnedByPlayers.Add(priv);
                }
            }
            return privatesOwnedByPlayers;
        }

        public StartPacket GetStartPacket(int index)
        {
            return startPackets[index];
        }

        public StartPacket GetStartPacket(string name)
        {
            return startPacketMap[name];
        }

        /** Pass number of turns for which a certain company type can lay extra tiles of a certain colour. */
        // NOTE: Called by phase.finishConfiguration().
        // This implies, that the CompanyManager configuration must finished be BEFORE PhaseManager.
        // (We shouldn't have such dependencies...)
        // TODO: Resolve the issues mentioned above
        public void AddExtraTileLayTurnsInfo(Dictionary<string, int> extraTileTurns)
        {
            foreach (string typeAndColour in extraTileTurns.Keys)
            {
                string[] keys = typeAndColour.Split('~');
                Dictionary<string, ICompany> companies = mCompaniesByTypeAndName[keys[0]];
                if (companies != null)
                {
                    foreach (ICompany company in companies.Values)
                    {
                        ((PublicCompany)company).AddExtraTileLayTurnsInfo(keys[1], extraTileTurns[typeAndColour]);
                    }
                }
            }
        }

        public StartPacket GetNextUnfinishedStartPacket()
        {
            foreach (StartPacket packet in startPackets)
            {
                if (packet.AreAllSold() == false)
                {
                    return packet;

                }
            }
            return null;
        }

        /**
         * @param id of the startItem
         * @return the startItem with that id
         */
        public StartItem GetStartItemById(string id)
        {
            foreach (StartPacket packet in startPackets)
            {
                foreach (StartItem item in packet.Items)
                {
                    if (item.Id.Equals(id))
                    {
                        return item;
                    }
                }
            }
            return null;
        }

    }
}
