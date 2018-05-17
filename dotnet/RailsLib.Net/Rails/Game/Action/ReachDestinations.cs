using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;

/** This class is needed until we have a means to determine reaching
 * destinations automatically.
 *
 * Rails 2.0: updated equals and toString methods
 */

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class ReachDestinations : PossibleORAction
    {
        // Server-side settings
        /*transient*/
        [JsonIgnore]
        protected List<PublicCompany> possibleCompanies;
        protected string possibleCompanyNames = "";

        // Client-side settings
        /*transient*/
        [JsonIgnore]
        protected List<PublicCompany> reachedCompanies;
        protected string reachedCompanyNames = "";

        new public const long serialVersionUID = 1L;

        public ReachDestinations(List<PublicCompany> companies)
        {
            possibleCompanies = companies;
            StringBuilder b = new StringBuilder();
            foreach (PublicCompany company in companies)
            {
                if (b.Length > 0) b.Append(",");
                b.Append(company.Id);
            }
            possibleCompanyNames = b.ToString();
        }

        /** Required for deserialization */
        public ReachDestinations() { }

        public List<PublicCompany> PossibleCompanies
        {
            get
            {
                return possibleCompanies;
            }
        }

        public string PossibleCompanyNames
        {
            get
            {
                return possibleCompanyNames;
            }
        }

        public void AddReachedCompany(PublicCompany company)
        {
            if (reachedCompanies == null)
                reachedCompanies = new List<PublicCompany>();
            reachedCompanies.Add(company);
            if (reachedCompanyNames.Length > 0)
            {
                reachedCompanyNames += ",";
            }
            reachedCompanyNames += company.Id;
        }

        public List<PublicCompany> ReachedCompanies
        {
            get
            {
                return reachedCompanies;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            ReachDestinations action = (ReachDestinations)pa;
            // #FIXME sequence compare
            bool options = possibleCompanies.SequenceEqual(action.possibleCompanies);

            // finish if asOptions check
            if (asOption) return options;

            // check asAction attributes
            return options && reachedCompanies.SequenceEqual(action.reachedCompanies);
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("possibleCompanies", possibleCompanies)
                        .AddToStringOnlyActed("reachedCompanies", reachedCompanies)
                        .ToString();
        }

        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            CompanyManager cmgr = CompanyManager;

            possibleCompanies = new List<PublicCompany>();
            if (!string.IsNullOrEmpty(possibleCompanyNames))
            {
                foreach (string cname in possibleCompanyNames.Split(','))
                {
                    if (!string.IsNullOrEmpty(cname))
                    {
                        possibleCompanies.Add(cmgr.GetPublicCompany(cname));
                    }
                }
            }
            reachedCompanies = new List<PublicCompany>();
            if (!string.IsNullOrEmpty(reachedCompanyNames))
            {
                foreach (string cname in reachedCompanyNames.Split(','))
                {
                    if (!string.IsNullOrEmpty(cname))
                    {
                        reachedCompanies.Add(cmgr.GetPublicCompany(cname));
                    }
                }
            }
        }
    }
}
