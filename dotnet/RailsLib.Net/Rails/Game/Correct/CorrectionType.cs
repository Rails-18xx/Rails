using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

/**
  * Enumerates the possible Corrections
*/

namespace GameLib.Rails.Game.Correct
{
    public class CorrectionType : IEquatable<CorrectionType>
    {
        public static CorrectionType CORRECT_CASH = new CorrectionType("CORRECT_CASH", CreateCashCorrectionManager);
        public static CorrectionType CORRECT_MAP = new CorrectionType("CORRECT_MAP", CreateMapCorrectionManager);

        public CorrectionType(string name, Func<GameManager, CorrectionManager> fn)
        {
            Name = name;
            mgrCreator = fn;
        }

        public string Name { set; get; }
        public CorrectionManager CorrectionManager { get; set; }
        private Func<GameManager, CorrectionManager> mgrCreator;

        public CorrectionManager NewCorrectionManager(GameManager gm)
        {
            return mgrCreator(gm);
        }

        private static CorrectionManager CreateCashCorrectionManager(GameManager gm)
        {
            return CashCorrectionManager.Create(gm);
        }

        private static CorrectionManager CreateMapCorrectionManager(GameManager gm)
        {
            //return MapCorrectionManager.Create(gm);
            throw new NotImplementedException(); // replaced, see MapCorrectionAction
        }

        public bool Equals(CorrectionType other)
        {
            if (other == null) return false;
            return Name == other.Name;
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode();
        }

        private static List<CorrectionType> items = new List<CorrectionType>()
        {
            CORRECT_CASH
            // CORRECT_MAP removed because obsolete
        };
        public static List<CorrectionType> AllOf
        {
            get
            {
                return items;
            }
        }

        public static CorrectionType ValueOf(string name)
        {
            switch (name)
            {
                case "CORRECT_CASH":
                    return CORRECT_CASH;

                case "CORRECT_MAP":
                    throw new NotImplementedException();
            }

            throw new ArgumentException();
        }
    }
}
