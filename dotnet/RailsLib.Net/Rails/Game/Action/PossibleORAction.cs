using GameLib.Net.Game;
using GameLib.Net.Game.GameRound;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * PossibleAction is the superclass of all classes that describe an allowed user
 * action (such as laying a tile or dropping a token on a specific hex, buying a
 * train etc.).
 * 
 * Rails 2.0: Added updated equals and toString methods 
 */

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    abstract public class PossibleORAction : PossibleAction
    {
        // Rails 2.0: This is a fix to be compatible with Rails 1.x
        new private const long serialVersionUID = -1656570654856705840L;

        // #transient
        [JsonIgnore]
        protected PublicCompany company;
        protected string companyName;

        /**
         *
         */
        public PossibleORAction() : base(null)
        {
            //super(null); // not defined by an activity yet
            // TODO: The company field should be set from outside and not inside the action classes themselves
            IRoundFacade round = GetRoot.GameManager.CurrentRound;
            if (round is OperatingRound)
            {
                company = ((OperatingRound)round).GetOperatingCompany();
                companyName = company.Id;
            }
        }

        virtual public PublicCompany Company
        {
            get
            {
                return company;
            }
            /** To be used in the client (to enable safety check in the server) */
            set
            {
                this.company = value;
                this.companyName = value.Id;
            }
        }

        public string CompanyName
        {
            get
            {
                return company.Id;
            }
        }

        /**
         * @return costs of executing the action, default for an ORAction is zero
         */
        virtual public int Cost
        {
            get
            {
                return 0;
            }
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            PossibleORAction action = (PossibleORAction)pa;
            return company.Equals(action.company); //Objects.equal(this.company, action.company);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                    .AddToString("company", company)
                    .ToString()
            ;
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (!string.IsNullOrEmpty(companyName))
                company = CompanyManager.GetPublicCompany(companyName);
        }
    }
}
