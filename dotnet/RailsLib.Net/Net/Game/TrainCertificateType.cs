using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * TrainCertificateType indicates the type of a TrainCertficate
 * TrainCertficates can be multi-sided (thus provide several TrainType options)

 */
namespace GameLib.Net.Game
{
    public class TrainCertificateType : RailsAbstractItem, IConfigurable, IComparable<TrainCertificateType>
    {
        // FIXME: Rails 2.0, move this to some default .xml!
        // #FIXME_UPDATE_CREATORCLASS
        private const string DEFAULT_TRAIN_CLASS = "GameLib.Net.Game.Train";

        // Static definitions
        private int index; // for sorting

        private int quantity = 0;
        private bool infiniteQuantity = false;

        private List<TrainType> potentialTrainTypes = new List<TrainType>(2);

        private Dictionary<int, string> newPhaseNames;

        private bool permanent = true;
        private bool obsoleting = false;

        private bool canBeExchanged = false;
#pragma warning disable 649
        private int cost;
#pragma warning restore 649
        private int exchangeCost;

        // store the trainClassName to allow dual configuration
        private string trainClassName = DEFAULT_TRAIN_CLASS;
        private Type trainClass;

        /** In some cases, trains start their life in the Pool, default is IPO */
        private string initialPortfolio = "IPO";

        // Dynamic state variables
        private IntegerState numberBoughtFromIPO;
        private BooleanState available;
        private BooleanState rusted;

        protected static Logger<TrainCertificateType> log = new Logger<TrainCertificateType>();

        private TrainCertificateType(TrainManager parent, string id, int index) : base(parent, id)
        {
            numberBoughtFromIPO = IntegerState.Create(this, "numberBoughtFromIPO");
            available = BooleanState.Create(this, "available");
            rusted = BooleanState.Create(this, "rusted");

            this.index = index;
        }

        public static TrainCertificateType Create(TrainManager parent, string id, int index)
        {
            return new TrainCertificateType(parent, id, index);
        }


        new public TrainManager Parent
        {
            get
            {
                return (TrainManager)base.Parent;
            }
        }

        public void ConfigureFromXML(Tag tag)
        {
            trainClassName = tag.GetAttributeAsString("class", trainClassName);
            trainClass = Configure.GetTypeForName<Train>(trainClassName);

            // Quantity
            quantity = tag.GetAttributeAsInteger("quantity", quantity);
            quantity += tag.GetAttributeAsInteger("quantityIncrement", 0);

            // From where is this type initially available
            initialPortfolio = tag.GetAttributeAsString("initialPortfolio", initialPortfolio);

            // New style phase changes (to replace 'startPhase' attribute and <Sub> tag)
            List<Tag> newPhaseTags = tag.GetChildren("NewPhase");
            if (newPhaseTags != null)
            {
                int index;
                string phaseName;
                newPhaseNames = new Dictionary<int, string>();
                foreach (Tag newPhaseTag in newPhaseTags)
                {
                    phaseName = newPhaseTag.GetAttributeAsString("phaseName");
                    if (string.IsNullOrEmpty(phaseName))
                    {
                        throw new ConfigurationException("TrainType " + Id + " has NewPhase without phase name");
                    }
                    index = newPhaseTag.GetAttributeAsInteger("trainIndex", 1);
                    newPhaseNames[index] = phaseName;
                }
            }

            // Exchangeable
            Tag swapTag = tag.GetChild("Exchange");
            if (swapTag != null)
            {
                exchangeCost = swapTag.GetAttributeAsInteger("cost", 0);
                canBeExchanged = (exchangeCost > 0);
            }

            // Can run as obsolete train
            obsoleting = tag.GetAttributeAsBoolean("obsoleting");
        }

        public void FinishConfiguration(RailsRoot root)
        {

            if (quantity == -1)
            {
                infiniteQuantity = true;
            }
            else if (quantity <= 0)
            {
                throw new ConfigurationException("Invalid quantity " + quantity + " for train cert type " + this);
            }
        }

        public Dictionary<int, string> GetNewPhaseNames()
        {
                return newPhaseNames;
        }

        public Train CreateTrain(IRailsItem parent, string id, int sortingId)
        {
            Train train = Configure.Create<Train>(trainClass, parent, id);
            train.SetSortingId(sortingId);
            return train;
        }

        public List<TrainType> GetPotentialTrainTypes()
        {
            return potentialTrainTypes;
        }

        public void AddPotentialTrainType(TrainType type)
        {
            potentialTrainTypes.Add(type);
        }

        /**
         * @return Returns the available.
         */
        public bool IsAvailable()
        {
            return available.Value;
        }

        /**
         * Make a train type available for buying by public companies.
         */
        public void SetAvailable()
        {
            available.Set(true);
        }

        public void SetRusted()
        {
            rusted.Set(true);
        }

        public bool HasRusted()
        {
            return rusted.Value;
        }

        public bool IsPermanent
        {
            get
            {
                return permanent;
            }
            set
            {
                permanent = value;
            }
        }

        public bool IsObsoleting
        {
            get
            {
                return obsoleting;
            }
        }

        public int Quantity
        {
            get
            {
                return quantity;
            }
        }

        public bool HasInfiniteQuantity
        {
            get
            {
                return infiniteQuantity;
            }
        }

        public bool NextCanBeExchanged
        {
            get
            {
                return canBeExchanged;
            }
        }

        public void AddToBoughtFromIPO()
        {
            numberBoughtFromIPO.Add(1);
        }

        public int GetNumberBoughtFromIPO()
        {
            return numberBoughtFromIPO.Value;
        }

        public int Cost
        {
            get
            {
                return cost;
            }
        }

        public int ExchangeCost
        {
            get
            {
                return exchangeCost;
            }
        }

        public string InitialPortfolio
        {
            get
            {
                return initialPortfolio;
            }
        }

        public string GetInfo()
        {
            StringBuilder b = new StringBuilder("<html>");
            b.Append(LocalText.GetText("TrainInfo", Id, Bank.Format(this, cost), quantity));
            if (b.Length == 6) b.Append(LocalText.GetText("None"));

            return b.ToString();
        }

        public int Index
        {
            get
            {
                return index;
            }
        }

        // Comparable interface
        public int CompareTo(TrainCertificateType o)
        {
            return index.CompareTo(o.Index);
        }
    }
}
