using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Special private ability involving deductions in train buying. The deduction
 * can be absolute (an amount) or relative (a percentage)
 */

namespace GameLib.Net.Game.Special
{
    public class SpecialTrainBuy : SpecialProperty
    {
        string name = "SpecialTrainBuy";
        string trainTypeName = ""; // Default: all train types
        bool extra = false;
        string deductionString;
        bool relativeDeduction = false;
        bool absoluteDeduction = false;
        int deductionAmount; // Money or percentage

        /**
         * Used by Configure (via reflection) only
         */
        public SpecialTrainBuy(IRailsItem parent, string id) : base(parent, id)
        {
        }

        override public void ConfigureFromXML(Tag tag)
        {

            base.ConfigureFromXML(tag);

            Tag trainBuyTag = tag.GetChild("SpecialTrainBuy");
            if (trainBuyTag == null)
            {
                throw new ConfigurationException("<SpecialTrainBuy> tag missing");
            }

            trainTypeName = trainBuyTag.GetAttributeAsString("trainType", trainTypeName);
            if (trainTypeName.Equals("any", StringComparison.OrdinalIgnoreCase)) trainTypeName = "";

            deductionString = trainBuyTag.GetAttributeAsString("deduction");
            if (string.IsNullOrEmpty(deductionString))
            {
                throw new ConfigurationException(
                        "No deduction found in <SpecialTrainBuy> tag");
            }
            string deductionAmountString;
            if (deductionString.EndsWith("%"))
            {
                relativeDeduction = true;
                deductionAmountString = deductionString.Replace("%", "");
            }
            else
            {
                deductionAmountString = deductionString;
            }
            try
            {
                deductionAmount = int.Parse(deductionAmountString);
            }
            catch (FormatException e)
            {
                throw new ConfigurationException("Invalid deduction "
                                                 + deductionString, e);
            }
        }

        public int GetPrice(int standardPrice)
        {

            if (absoluteDeduction)
            {
                return standardPrice - deductionAmount;
            }
            else if (relativeDeduction)
            {
                return (int)(standardPrice * (0.01 * (100 - deductionAmount)));
            }
            else
            {
                return standardPrice;
            }
        }

        public bool IsValidForTrainType(string trainType)
        {
            return trainTypeName.Equals("")
                   || trainTypeName.Equals(trainType, StringComparison.OrdinalIgnoreCase);
        }

        override public bool IsExecutionable
        {
            get
            {
                return true;
            }
        }

        public bool IsExtra
        {
            get
            {
                return extra;
            }
        }

        public bool IsFree
        {
            get
            {
                return false;
            }
        }

        // #FIXME is this correct?
        new public string Id
        {
            get
            {
                return name;
            }
        }

        public bool IsAbsoluteDeduction
        {
            get
            {
                return absoluteDeduction;
            }
        }

        public int DeductionAmount
        {
            get
            {
                return deductionAmount;
            }
        }

        public string DeductionString
        {
            get
            {
                return deductionString;
            }
        }

        public bool IsRelativeDeduction
        {
            get
            {
                return relativeDeduction;
            }
        }

        public string TrainTypeName
        {
            get
            {
                return trainTypeName;
            }
        }

        override public string ToText()
        {
            return "SpecialTrainBuy comp=" + originalCompany.Id + " extra="
                   + extra + " deduction=" + deductionString;
        }

        override public string ToMenu()
        {
            if (trainTypeName.Equals(""))
            {
                return LocalText.GetText("SpecialTrainBuyAny",
                        deductionString,
                        originalCompany.Id);
            }
            else
            {
                return LocalText.GetText("SpecialTrainBuy",
                        trainTypeName,
                        deductionString,
                        originalCompany.Id);
            }
        }

        override public string GetInfo()
        {
            return ToMenu();
        }
    }
}
