using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * This is MoneyModel that derives it value from a calculation method.
 * TODO: Rewrite all methods implementing the interface
 */

namespace GameLib.Net.Game.Model
{
    public interface ICalculationMethod
    {
        int Calculate();
        bool Initialized { get; }
    }

    abstract public class CalculationMethodBase : ICalculationMethod
    {
        abstract public int Calculate();
        abstract public bool Initialized { get; }
    }

    public class CalculatedMoneyModel : MoneyModel
    {

        private ICalculationMethod method;

        private CalculatedMoneyModel(IRailsItem parent, string id, ICalculationMethod method, Currency currency) :
                base(parent, id, currency)
        {
            this.method = method;
        }

        public static CalculatedMoneyModel Create(IRailsItem parent, string id, ICalculationMethod method)
        {
            Currency currency = parent.GetRoot.Bank.Currency;
            return new CalculatedMoneyModel(parent, id, method, currency);
        }

        override public int Value
        {
            get
            {
                return method.Calculate();
            }
        }

        override public bool Initialized
        {
            get
            {
                return method.Initialized;
            }
        }
    }
}
