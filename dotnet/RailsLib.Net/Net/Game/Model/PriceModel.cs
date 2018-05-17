using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class PriceModel : ColorModel
    {
        // FIXME: Remove duplication of company and parent
        private PublicCompany company;

        private GenericState<StockSpace> stockPrice;
        private bool showCoordinates = true;

        private PriceModel(PublicCompany parent, string id, bool showCoordinates) : base(parent, id)
        {
            stockPrice = GenericState<StockSpace>.Create(this, "stockPrice");
            company = parent;
            this.showCoordinates = showCoordinates;
        }

        public static PriceModel Create(PublicCompany parent, string id, bool showCoordinates)
        {
            return new PriceModel(parent, id, showCoordinates);
        }

        new public PublicCompany Parent
        {
            get
            {
                return (PublicCompany)base.Parent;
            }
        }

        public void SetPrice(StockSpace price)
        {
            stockPrice.Set(price);
        }

        public StockSpace GetPrice()
        {
            return stockPrice.Value;
        }

        public PublicCompany Company
        {
            get
            {
                return company;
            }
        }

        override public Color Background
        {
            get
            {
                if (stockPrice.Value != null)
                {
                    return stockPrice.Value.Color;
                }
                else
                {
                    return default(Color);
                }
            }
        }

        override public Color Foreground
        {
            get
            {
                return default(Color);
            }
        }

        
    override public string ToText()
        {
            string text = "";
            if (stockPrice.Value != null)
            {
                text = Bank.Format(Parent, stockPrice.Value.Price);
                if (showCoordinates)
                {
                    text += " (" + stockPrice.Value.Id + ")";
                }
            }
            return text;
        }

    }
}
