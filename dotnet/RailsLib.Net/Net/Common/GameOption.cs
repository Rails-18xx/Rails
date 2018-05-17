using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;

namespace GameLib.Net.Common
{
    public class GameOption : IComparable<GameOption>, IEquatable<GameOption>
    {
        // Strings that define yes or no options
        public const string OPTION_VALUE_YES = "yes";
        public const string OPTION_VALUE_NO = "no";

        // Strings that define types
        public const string OPTION_TYPE_SELECTION = "selection";
        public const string OPTION_TYPE_TOGGLE = "toggle";

        // A default option that will always be set
        public const string NUMBER_OF_PLAYERS = "NumberOfPlayers";
        // Some other common game options
        public const string VARIANT = "Variant";

        private string name;
        private string localisedName;
        private bool isBoolean;
        private string defaultValue;
        private List<string> allowedValues;
        private int ordering;

        // dynamic values
        private string selectedValue;

        public GameOption(string name, string localisedName, bool isBoolean,
                string defaultValue, List<string> allowedValues, int ordering)
        {
            this.name = name;
            this.localisedName = localisedName;
            this.isBoolean = isBoolean;
            this.defaultValue = defaultValue;
            this.allowedValues = allowedValues;
            this.ordering = ordering;
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        public string LocalisedName
        {
            get
            {
                return localisedName;
            }
        }

        public bool IsBoolean
        {
            get
            {
                return isBoolean;
            }
        }

        public List<string> AllowedValues
        {
            get
            {
                return allowedValues;
            }
        }

        public bool IsValueAllowed(string value)
        {
            return allowedValues.Contains(value);
        }

        public string DefaultValue
        {
            get
            {
                return defaultValue;
            }
        }

        public string SelectedValue
        {
            get
            {
                if (selectedValue == null)
                {
                    return defaultValue;
                }
                else
                {
                    return selectedValue;
                }
            }
            set
            {
                selectedValue = value;
            }
        }

        override public int GetHashCode()
        {
            return name.GetHashCode();
        }

        override public bool Equals(object obj)
        {
            if (obj == null) return false;
            if (!(obj is GameOption)) return false;
            GameOption other = (GameOption)obj;
            return name.Equals(other.name);
        }

        public bool Equals(GameOption obj)
        {
            if (obj == null) return false;
            return name.Equals(obj.name);
        }

        override public string ToString()
        {
            return name;
        }

        public int CompareTo(GameOption other)
        {
            return name.CompareTo(other.name);
        }

        public static Builder CreateBuilder(string name)
        {
            return new Builder(name);
        }

        public class Builder
        {
            private string name;
            private string type = OPTION_TYPE_SELECTION;
            private string defaultValue = null;
            private ReadOnlyCollection<string> allowedValues;
            private ReadOnlyCollection<string> parameters;
            private int ordering = 0;

            public Builder(string name)
            {
                this.name = name;
            }

            public void SetType(string type)
            {
                this.type = type;
            }

            public void SetDefaultValue(string defaultValue)
            {
                this.defaultValue = defaultValue;
            }

            public void SetAllowedValues(IEnumerable<string> values)
            {
                allowedValues = new ReadOnlyCollection<string>(new List<string>(values));
                // ImmutableList.ToImmutableList(new List<string>(values));// new List<string>(values).AsReadOnly();// ImmutableList.copyOf(values);
            }

            public void SetParameters(IEnumerable<string> parameters)
            {
                this.parameters = new ReadOnlyCollection<string>(new List<string>(parameters));
                //this.parameters = ImmutableList.ToImmutableList(new List<string>(parameters)); // new List<string>(parameters).AsReadOnly();// ImmutableList.copyOf(parameters);
            }

            public void SetOrdering(int ordering)
            {
                this.ordering = ordering;
            }

            private string GetLocalizedName()
            {
                if (parameters == null || parameters.Count == 0)
                {
                    return LocalText.GetText(name);
                }

                // #localized_text_needs_fixed
                List<string> localTextPars = new List<string>();
                foreach (string par in parameters)
                {
                    localTextPars.Add(LocalText.GetText(par));
                }
                // TODO (Rails2.0): Change method signature in LocalText
                return LocalText.GetText(name, localTextPars.ToArray());
            }

            private string GetFinalDefaultValue(bool isBoolean, List<string> finalAllowedValues)
            {
                if (defaultValue != null)
                {
                    return defaultValue;
                }
                else if (isBoolean)
                {
                    return OPTION_VALUE_NO;
                }
                else if (allowedValues.Count != 0)
                {
                    return allowedValues[0];
                }
                else
                {
                    return null;
                }
            }

            public GameOption Build()
            {

                // use type information
                Boolean isBoolean = false;
                List<string> finalAllowedValues = new List<string>();
                if (type.Equals(OPTION_TYPE_TOGGLE, StringComparison.OrdinalIgnoreCase))//  equalsIgnoreCase(OPTION_TYPE_TOGGLE))
                {
                    isBoolean = true;
                    finalAllowedValues = new List<string>() { OPTION_VALUE_YES, OPTION_VALUE_NO };// ImmutableList.of(OPTION_VALUE_YES, OPTION_VALUE_NO);
                }
                else if (type.Equals(OPTION_TYPE_SELECTION, StringComparison.OrdinalIgnoreCase))// equalsIgnoreCase(OPTION_TYPE_SELECTION))
                {
                    if (allowedValues == null)
                    {
                        finalAllowedValues = new List<string>(); // ImmutableList.of();
                    }
                    else
                    {
                        finalAllowedValues = new List<string>(allowedValues);
                    }
                }

                string parameterisedName = ConstructParameterizedName(name, parameters);
                string localisedName = GetLocalizedName();
                string finalDefaultValue = GetFinalDefaultValue(isBoolean, finalAllowedValues);

                return new GameOption(parameterisedName, localisedName, isBoolean,
                        finalDefaultValue, finalAllowedValues, ordering);
            }
        }

        /**
         * Returns parameterized Name
         */
        public static string ConstructParameterizedName(string name, ReadOnlyCollection<string> parameters)
        {
            if (parameters != null && parameters.Count != 0)
            {
                return name + "_" + string.Join("_", parameters);
            }
            else
            {
                return name;
            }
        }
        /**
         * Returns the value of the gameOption in a game which contains the RailItem
         */
        public static string GetValue(IRailsItem item, string gameOption)
        {
            // check the System properties for overwrites first
            // #SystemProperty
            //    if (!string.IsNullOrEmpty(System.GetProperty(gameOption)))
            //    {
            //        return System.getProperty(gameOption);
            //    }
            //    else
            //    {
            //        return item.getRoot().getGameOptions().get(gameOption);
            //    }
            return item.GetRoot.GameOptions.Get(gameOption);
        }

        /**
         * Returns the boolean value of the gameOption in a game which contains the
         * RailItem If not defined as in OPTION_VALUE_YES, it returns false
         */
        public static bool GetAsBoolean(IRailsItem item, string gameOption)
        {
            String value = GetValue(item, gameOption);
            return value != null && OPTION_VALUE_YES.Equals(value, StringComparison.OrdinalIgnoreCase);// equalsIgnoreCase(value);
        }
    }
}
