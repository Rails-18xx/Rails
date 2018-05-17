using GameLib.Net.Game.Financial;
using System;


namespace GameLib.Net.Game.Model
{
    public class StockMarketModel : RailsModel
    {
        public static readonly string ID = "StockMarketModel";

        private StockMarketModel(StockMarket parent, String id) : base(parent, id)
        {

        }

        public static StockMarketModel Create(StockMarket parent)
        {
            return new StockMarketModel(parent, ID);
        }
    }
}
