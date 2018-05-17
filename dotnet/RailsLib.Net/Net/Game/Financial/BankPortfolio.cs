using GameLib.Net.Game.Model;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * BankPortfolios act as Owner of their owns
 * Used for implementation of the separate Bank identities (IPO, POOL, SCRAPHEAP)

 */
namespace GameLib.Net.Game.Financial
{
    public sealed class BankPortfolio : RailsAbstractItem, IPortfolioOwner
    {
        private PortfolioModel portfolio;


    private BankPortfolio(Bank parent, string id) : base(parent, id)
        {
            portfolio = PortfolioModel.Create(this);
        }

        /**
         * @param parent restricted to bank
         */
        public static BankPortfolio Create(Bank parent, string id)
        {
            return new BankPortfolio(parent, id);
        }

        public void FinishConfiguration()
        {
            portfolio.FinishConfiguration();
        }

    new public Bank Parent
        {
            get
            {
                return (Bank)base.Parent;
            }
        }

        // Owner methods
        public PortfolioModel PortfolioModel
        {
            get
            {
                return portfolio;
            }
        }

    }
}
