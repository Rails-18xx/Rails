using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Game.Special
{
    abstract public class SpecialProperty : RailsOwnableItem<SpecialProperty>, IConfigurable, ICreatable
    {
        protected static Logger<SpecialProperty> log = new Logger<SpecialProperty>();

        protected const string STORAGE_NAME = "SpecialProperty";

        protected BooleanState exercised;// = BooleanState.Create(this, "exercised");
        protected ICompany originalCompany;

        /* Usability conditions. Not all of these are already being used. */
        protected bool usableIfOwnedByPlayer = false;
        protected bool usableIfOwnedByCompany = false;
        protected bool usableDuringSR = false;
        protected bool usableDuringOR = false;
        protected bool usableDuringTileLayingStep = false;
        protected bool usableDuringTokenLayingStep = false;

        protected string conditionText = "";
        protected string whenText = "";
        protected string transferText = "";
        protected bool permanent = false;
        // if exercising contributes to closing, if private has the closing conditions set, thus default is true
        // allows to exclude special properties that do not close privates that are closeable
        protected bool closesPrivate = true;


        protected bool isORProperty = false;
        protected bool isSRProperty = false;

        /** Optional descriptive text, for display in menus and info text.
         * Subclasses may put real text in it.
         */
        protected string description = "";

        protected int uniqueId;

        protected SpecialProperty(IRailsItem parent, string id) : base(parent, ConvertId(id))
        {
            exercised = BooleanState.Create(this, "exercised");
            //super(parent, convertId(id), SpecialProperty.class);
            uniqueId = int.Parse(id);
            GetRoot.GameManager.StoreObject(STORAGE_NAME, this);
        }

        virtual public void ConfigureFromXML(Tag tag)
        {
            conditionText = tag.GetAttributeAsString("condition");
            if (string.IsNullOrEmpty(conditionText))
                throw new ConfigurationException("Missing condition in private special property");

            IsUsableIfOwnedByPlayer = (new Regex("(?i).*ifOwnedByPlayer.*").IsMatch(conditionText));  //conditionText.matches("(?i).*ifOwnedByPlayer.*"));
            IsUsableIfOwnedByCompany = (new Regex("(?i).*ifOwnedByCompany.*").IsMatch(conditionText)); //conditionText.matches("(?i).*ifOwnedByCompany.*"));

            whenText = tag.GetAttributeAsString("when");
            if (string.IsNullOrEmpty(whenText))
                throw new ConfigurationException("Missing condition in private special property");

            IsUsableDuringSR = (whenText.Equals("anyTurn", StringComparison.OrdinalIgnoreCase)
                        || whenText.Equals("srTurn", StringComparison.OrdinalIgnoreCase));
            SetUsableDuringOR(whenText.Equals("anyTurn", StringComparison.OrdinalIgnoreCase)
                    || whenText.Equals("orTurn", StringComparison.OrdinalIgnoreCase));

            IsUsableDuringTileLayingStep = (whenText.Equals("tileLayingStep", StringComparison.OrdinalIgnoreCase));
            IsUsableDuringTileLayingStep = (whenText.Equals("tileAndTokenLayingStep", StringComparison.OrdinalIgnoreCase));
            IsUsableDuringTokenLayingStep = (whenText.Equals("tokenLayingStep", StringComparison.OrdinalIgnoreCase));
            IsUsableDuringTokenLayingStep = (whenText.Equals("tileAndTokenLayingStep", StringComparison.OrdinalIgnoreCase));

            transferText = tag.GetAttributeAsString("transfer", "");

            permanent = tag.GetAttributeAsBoolean("permanent", permanent);

            closesPrivate = tag.GetAttributeAsBoolean("closesPrivate", closesPrivate);
        }

        virtual public void FinishConfiguration(RailsRoot root)
        {
            // do nothing specific
        }

        public int UniqueId
        {
            get
            {
                return uniqueId;
            }
        }

        public ICompany OriginalCompany
        {
            get
            {
                return originalCompany;
            }
            // Sets the first (time) owner
            set
            {
                Precondition.CheckState(originalCompany == null, "OriginalCompany can only set once");
                originalCompany = value;
            }
        }

        /**
         * @return Returns the usableIfOwnedByCompany.
         */
        public bool IsUsableIfOwnedByCompany
        {
            get
            {
                return usableIfOwnedByCompany;
            }
            /**
            * @param usableIfOwnedByCompany The usableIfOwnedByCompany to set.
            */
            set
            {
                this.usableIfOwnedByCompany = value;
            }
        }

        /**
         * @return Returns the usableIfOwnedByPlayer.
         */
        public bool IsUsableIfOwnedByPlayer
        {
            get
            {
                return usableIfOwnedByPlayer;
            }
            /**
            * @param usableIfOwnedByPlayer The usableIfOwnedByPlayer to set.
            */
            set
            {
                usableIfOwnedByPlayer = value;
            }
        }

        public bool IsUsableDuringOR(GameDef.OrStep step)
        {
            if (usableDuringOR) return true;

            switch (step)
            {
                case GameDef.OrStep.LAY_TRACK:
                    return usableDuringTileLayingStep;
                case GameDef.OrStep.LAY_TOKEN:
                    return usableDuringTokenLayingStep;
                default:
                    return false;
            }
        }

        public void SetUsableDuringOR(bool usableDuringOR)
        {
            this.usableDuringOR = usableDuringOR;
        }

        public bool IsUsableDuringSR
        {
            get
            {
                return usableDuringSR;
            }
            set
            {
                usableDuringSR = value;
            }
        }

        public bool IsUsableDuringTileLayingStep
        {
            get
            {
                return usableDuringTileLayingStep;
            }
            set
            {
                usableDuringTileLayingStep = value;
            }
        }

        public bool IsUsableDuringTokenLayingStep
        {
            get
            {
                return usableDuringTokenLayingStep;
            }
            set
            {
                usableDuringTokenLayingStep = value;
            }
        }

        virtual public void SetExercised()
        {
            SetExercised(true);
        }

        virtual public void SetExercised(bool value)
        {
            if (permanent) return; // sfy 1889 
            exercised.Set(value);
            if (value && closesPrivate && originalCompany is PrivateCompany)
            {
                ((PrivateCompany)originalCompany).CheckClosingIfExercised(false);
            }
        }

        virtual public bool IsExercised()
        {
            return exercised.Value;
        }

        public abstract bool IsExecutionable { get; }


        public bool IsSRProperty
        {
            get
            {
                return isSRProperty;
            }
        }

        public bool IsORProperty
        {
            get
            {
                return isORProperty;
            }
        }

        public string TransferText
        {
            get
            {
                return transferText;
            }
        }

        /**
         * Default menu item text, should be by all special properties that can
         * appear as a menu item
         */
        virtual public string ToMenu()
        {
            return ToString();
        }

        /** Default Info text. To be overridden where useful. */
        virtual public string GetInfo()
        {
            return ToString();
        }

        /** Default Help text: "You can " + the menu description */
        public string GetHelp()
        {
            return LocalText.GetText("YouCan", Util.Util.LowerCaseFirst(ToMenu()));
        }

        // TODO: Rails 2.0: Move this to a new SpecialPropertyManager

        // convert to the full id used 
        private static string ConvertId(string id)
        {
            return STORAGE_NAME + "_" + id;
        }

        // return new storage id
        private static string CreateUniqueId(IRailsItem item)
        {
            return (item.GetRoot.GameManager.GetStorageId(STORAGE_NAME) + 1).ToString();
            // increase unique id to allow loading old save files (which increase by 1)
            // TODO: remove that legacy issue
        }

        // return special property by unique id
        public static SpecialProperty GetByUniqueId(IRailsItem item, int id)
        {
            id -= 1;
            // decrease retrieval id to allow loading old save files (which increase by 1)
            // TODO: remove that legacy issue
            return (SpecialProperty)item.GetRoot.GameManager.RetrieveObject(STORAGE_NAME, id);
        }

        /**
         * @param company the company that owns the SpecialProperties
         * @param tag with XML to create SpecialProperties
         * @return additional InfoText
         * @throws ConfigurationException
         */
        public static string Configure(ICompany company, Tag tag)
        {

            StringBuilder text = new StringBuilder();

            // Special properties
            Tag spsTag = tag.GetChild("SpecialProperties");
            if (spsTag != null)
            {

                List<Tag> spTags = spsTag.GetChildren("SpecialProperty");
                string className;
                foreach (Tag spTag in spTags)
                {
                    className = spTag.GetAttributeAsString("class");
                    if (string.IsNullOrEmpty(className))
                        throw new ConfigurationException("Missing class in private special property");

                    string uniqueId = SpecialProperty.CreateUniqueId(company);
                    SpecialProperty sp = (SpecialProperty)Common.Parser.Configure.Create(className, company, uniqueId);
                    sp.OriginalCompany = company;
                    sp.ConfigureFromXML(spTag);
                    sp.MoveTo(company);
                    text.Append("<br>" + sp.GetInfo());
                }
            }
            return text.ToString();
        }
    }
}
