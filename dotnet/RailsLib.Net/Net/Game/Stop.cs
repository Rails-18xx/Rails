using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;
using static GameLib.Net.Game.StopType;

/**
 * A Stop object represents any junction on the map that is relevant for
 * establishing train run length and revenue calculation. A Stop object is bound
 * to (1) a MapHex, (2) to a Station object on the current Tile laid on that
 * MapHex, and (3) any tokens laid on that tile and station. <p> Each Stop has a
 * unique ID, that is derived from the MapHex name and the Stop number. The
 * initial Stop numbers are derived from the Station numbers of the preprinted
 * tile of that hex. <p> Please note, that during upgrades the Stop numbers
 * related to a city on a multiple-city hex may change: city 1 on one tile may
 * be numbered 2 on its upgrade, depending on the rotation of the upgrading
 * tile. However, the Stop numbers will not change, unless cities are merged
 * during upgrades; but even then it is attempted to retain the old Stop numbers
 * as much as possible.
  */

namespace GameLib.Net.Game
{
    public class Stop : RailsAbstractItem, IRailsOwner, IComparable<Stop>
    {
        private PortfolioSet<BaseToken> tokens;
        private GenericState<Station> relatedStation;
        // FIXME: Only used for Rails1.x compatibility
        private IntegerState legacyNumber;
        // FIXME: Only used for Rails1.x compatibility
        private HashSetState<int> previousNumbers;

        private Stop(MapHex hex, string id, Station station) : base(hex, id)
        {
            tokens = PortfolioSet<BaseToken>.Create(this, "tokens");
            relatedStation = GenericState<Station>.Create(this, "station");
            legacyNumber = IntegerState.Create(this, "legacyNumber", 0);
            previousNumbers = HashSetState<int>.Create(this, "previousNumbers");

            relatedStation.Set(station);
            tokens.AddModel(hex);
            //if (station != null)
            //{
            //    legacyNumber.set(station.getNumber());
            //}
        }

        public static Stop Create(MapHex hex, Station station)
        {
            if (station == null)
            {
                return new Stop(hex, "0", null);
            }
            else
            {
                return new Stop(hex, station.Number.ToString(), station);
            }
        }

        new public MapHex Parent
        {
            get
            {
                return (MapHex)base.Parent;
            }
        }

        // This should not be used for identification reasons
        // It is better to use the getRelatedNumber()
        [Obsolete]
        public string GetSpecificId()
        {
            return Parent.Id + "/" + this.GetRelatedNumber();
        }

        public Station GetRelatedStation()
        {
            return relatedStation.Value;
        }

        public void SetRelatedStation(Station station)
        {
            relatedStation.Set(station);
        }

        // FIMXE: Due to Rails1.x compatibility use the legacy number 
        public int GetRelatedNumber()
        {
            // #Removed_Legacy
            return relatedStation.Value.Number;
            //return getLegacyNumber();
        }

        // FIMXE: Due to Rails1.x compatibility
        [Obsolete]
        public int GetLegacyNumber()
        {
            //return legacyNumber.value();
            return legacyNumber.Value;
        }

        // FIMXE: Due to Rails1.x compatibility
        [Obsolete]
        public bool CheckPreviousNumbers(int number)
        {
            //return previousNumbers.contains(number);
            return previousNumbers.Contains(number);
        }

        // FIMXE: Due to Rails1.x compatibility
        [Obsolete]
        public void AddPreviousNumbers(int number)
        {
            //previousNumbers.add(number);
            previousNumbers.Add(number);
        }

        public IReadOnlyCollection<BaseToken> GetBaseTokens()
        {
            return tokens.Items;
        }

        public bool HasTokens
        {
            get
            {
                return tokens.Count > 0;
            }
        }

        public int Slots
        {
            get
            {
                return relatedStation.Value.BaseSlots;
            }
        }

        public bool HasTokenSlotsLeft
        {
            get
            {
                return tokens.Count < Slots;
            }
        }

        public int TokenSlotsLeft
        {
            get
            {
                return Slots - tokens.Count;
            }
        }

        /**
         * @param company
         * @return true if this Stop already contains an instance of the specified
         * company's token. Do this by calling the hasTokenOf with Company Name.
         * Using a tokens.contains(company) fails since the tokens are a ArrayList
         * of Token not a ArrayList of PublicCompany.
         */
        public bool HasTokenOf(PublicCompany company)
        {
            foreach (BaseToken token in tokens)
            {
                if (token.Parent == company)
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if stop is tokenable, thus it has open token slots and no company token yet
         */
        public bool IsTokenableFor(PublicCompany company)
        {
            return HasTokenSlotsLeft && !HasTokenOf(company);
        }

        public RunTo RunToAllowed
        {
            get
            {
                RunTo? runTo = Parent.StopType.RunToAllowed;
                if (runTo == null) runTo = Parent.CurrentTile.StopType.RunToAllowed;
                if (runTo == null) runTo = GetRelatedStation().StopType.RunToAllowed;
                return runTo.Value;
            }
        }

        public RunThrough RunThroughAllowed
        {
            get
            {
                RunThrough? runThrough = Parent.StopType.RunThroughAllowed;
                if (runThrough == null) runThrough = Parent.CurrentTile.StopType.RunThroughAllowed;
                if (runThrough == null) runThrough = GetRelatedStation().StopType.RunThroughAllowed;
                return runThrough.Value;
            }
        }

        public Loop LoopAllowed
        {
            get
            {
                Loop? loopAllowed = Parent.StopType.LoopAllowed;
                if (loopAllowed == null) loopAllowed = Parent.CurrentTile.StopType.LoopAllowed;
                if (loopAllowed == null) loopAllowed = GetRelatedStation().StopType.LoopAllowed;
                return loopAllowed.Value;
            }
        }

        public Score ScoreType
        {
            get
            {
                Score? scoreType = Parent.StopType.ScoreType;
                if (scoreType == null) scoreType = Parent.CurrentTile.StopType.ScoreType;
                if (scoreType == null) scoreType = GetRelatedStation().StopType.ScoreType;
                return scoreType.Value;
            }
        }

        public bool IsRunToAllowedFor(PublicCompany company)
        {
            switch (RunToAllowed)
            {
                case RunTo.YES:
                    return true;
                case RunTo.NO:
                    return false;
                case RunTo.TOKENONLY:
                    return HasTokenOf(company);
                default:
                    // Dead code, only to satisfy the compiler
                    return true;
            }
        }

        public bool IsRunThroughAllowedFor(PublicCompany company)
        {
            switch (RunThroughAllowed)
            {
                case RunThrough.YES: // either it has no tokens at all, or it has a company tokens or empty token slots
                    return !HasTokens || HasTokenOf(company) || HasTokenSlotsLeft;
                case RunThrough.NO:
                    return false;
                case RunThrough.TOKENONLY:
                    return HasTokenOf(company);
                default:
                    // Dead code, only to satisfy the compiler
                    return true;
            }
        }

        public int GetValueForPhase(Phase phase)
        {
            if (Parent.HasValuesPerPhase)
            {
                return Parent.GetCurrentValueForPhase(phase);
            }
            else
            {
                return relatedStation.Value.Value;
            }
        }

        public int CompareTo(Stop o)
        {
            if (o == null) return 1;
            int result = o.GetRelatedStation().Value.CompareTo(GetRelatedStation().Value);
            if (result != 0) return result;

            result = o.TokenSlotsLeft.CompareTo(TokenSlotsLeft);
            if (result != 0) return result;

            return Id.CompareTo(o.Id);
            //return ComparisonChain.start()
            //        .compare(o.getRelatedStation().getValue(), this.getRelatedStation().getValue())
            //        .compare(o.getTokenSlotsLeft(), this.getTokenSlotsLeft())
            //        .compare(this.getId(), o.getId())
            //        .result()
            ;
        }

        override public string ToText()
        {
            StringBuilder b = new StringBuilder();
            b.Append("Hex ").Append(Parent.Id);
            string cityName = Parent.StopName;
            b.Append(" (");
            if (!string.IsNullOrEmpty(cityName))
            {
                b.Append(cityName);
            }
            if (Parent.Stops.Count > 1)
            {
                b.Append(" ").Append(Parent.GetConnectionString(relatedStation.Value));
            }
            b.Append(")");
            return b.ToString();
        }
    }
}
