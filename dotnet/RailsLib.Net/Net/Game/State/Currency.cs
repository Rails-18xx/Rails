using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

/**
 * The class that represents a Currency
 */

namespace GameLib.Net.Game.State
{
    public class Currency : CountableItem<Currency>
    {
        /**
           * The money format template. '@' is replaced by the numeric amount, the
           * rest is copied.
           */
        private string format;

        private Currency(ICurrencyOwner parent, string id) : base(parent, id)
        {
        }

        public static Currency Create(ICurrencyOwner parent, string id)
        {
            return new Currency(parent, id);
        }

        new public ICurrencyOwner Parent
        {
            get
            {
                return (ICurrencyOwner)base.Parent;
            }
        }

        public string Format(int amount)
        {
            // Replace @ with the amount
            int i = format.IndexOf('@');
            string result;
            if (i != -1)
            {
                result = format.Substring(0, i) + amount.ToString() + format.Substring(i + 1);
            }
            else
            {
                throw new InvalidOperationException("No @ in format");
            }
            //string result = format.  ReplaceFirst("@", String.valueOf(amount));
            // Move any minus to the front

            if (amount < 0) //result = result.replaceFirst("(.+)-", "-$1");
            {
                Regex regex = new Regex("(.+)-");
                result = regex.Replace(result, "-$1", 1);
            }
            return result;
        }

        public string Format(IEnumerable<int> amountList)
        {
            StringBuilder result = new StringBuilder();
            bool init = true;
            foreach (int amount in amountList)
            {
                if (init)
                {
                    init = false;
                }
                else
                {
                    result.Append(",");
                }
                result.Append(Format(amount));
            }
            return result.ToString();
        }

        public void SetFormat(string format)
        {
            this.format = format;
        }

        public static string Wire(IMoneyOwner from, int amount, IMoneyOwner to)
        {
            Currency currency = from.Purse.Currency;
            currency.Move(from, amount, to);
            return currency.Format(amount);
        }

        public static string WireAll(IMoneyOwner from, IMoneyOwner to)
        {
            return Wire(from, from.Cash, to);
        }

        public static string ToBank(IMoneyOwner from, int amount)
        {
            Currency currency = from.Purse.Currency;
            currency.Move(from, amount, currency.Parent);
            return currency.Format(amount);
        }

        public static string ToBankAll(IMoneyOwner from)
        {
            return ToBank(from, from.Cash);
        }

        public static string FromBank(int amount, IMoneyOwner to)
        {
            Currency currency = to.Purse.Currency;
            currency.Move(currency.Parent, amount, to);
            return currency.Format(amount);
        }
    }
}
