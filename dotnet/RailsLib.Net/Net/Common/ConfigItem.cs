using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

/**
 * Defines an item used for the configuration of rails
 * T represents the value type
 */
namespace GameLib.Net.Common
{
    public sealed class ConfigItem
    {
        private static Logger<ConfigItem> log = new Logger<ConfigItem>();

        /**
         * Defines possible types (Java classes used as types in ConfigItem below
         */
        public enum ConfigType
        {
            BOOLEAN, INTEGER, PERCENT, STRING, LIST, FONT, DIRECTORY, FILE, COLOR
        }

        // static attributes
        public readonly string name;
        public readonly ConfigType type;
        public readonly List<string> allowedValues;
        public readonly string formatMask;

        // method call attributes
        private readonly string initClass;
        private readonly string initMethod;
        private readonly bool alwaysCallInit;
        private readonly bool initParameter;

        // dynamic attributes
        private string newValue;
        private string currentValue;

        public ConfigItem(Tag tag)
        {
            // check name and type (required)
            string name = tag.GetAttributeAsString("name");
            if (!string.IsNullOrEmpty(name))
            {
                this.name = name;
            }
            else
            {
                throw new ConfigurationException("Missing name for configuration item");
            }
            // optional: list of allowed values
            string valueString = tag.GetAttributeAsString("values");
            if (!string.IsNullOrEmpty(valueString))
            {
                allowedValues = valueString.Split(',').ToList();
                this.type = ConfigType.LIST;
            }
            else
            {
                allowedValues = null;
                string type = tag.GetAttributeAsString("type");
                if (!string.IsNullOrEmpty(type))
                {
                    try
                    {
                        this.type = (ConfigType)Enum.Parse(typeof(ConfigType), type.ToUpper());// ConfigType.valueOf(type.toUpperCase());
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException("Missing or invalid type for configuration item, exception = " + e);
                    }
                }
                else
                {
                    throw new ConfigurationException("Missing or invalid type for configuration item");
                }
                if (this.type == ConfigType.LIST)
                {
                    throw new ConfigurationException("No values defined for LIST config item");
                }
            }

            // optional: formatMask
            formatMask = tag.GetAttributeAsString("formatMask");

            // optional: init method attributes
            initClass = tag.GetAttributeAsString("initClass");
            initMethod = tag.GetAttributeAsString("initMethod");
            alwaysCallInit = tag.GetAttributeAsBoolean("alwaysCallInit", false);
            initParameter = tag.GetAttributeAsBoolean("initParameter", false);

            // initialize values
            currentValue = null;
            newValue = null;
        }


        public bool HasChanged
        {
            get
            {
                if (newValue == null) return false;
                return !CurrentValue.Equals(newValue);
            }
        }

        public string Value
        {
            get
            {
                if (HasChanged)
                {
                    return NewValue;
                }
                else
                {
                    return CurrentValue;
                }
            }
        }

        public string CurrentValue
        {
            get
            {
                if (currentValue == null) return "";
                return currentValue;
            }
            set
            {
                currentValue = value;
                newValue = null;
            }
        }

        //@Deprecated
        [Obsolete]
        public bool HasNewValue
        {
            get
            {
                return (newValue != null);
            }
        }

        public string NewValue
        {
            get
            {
                if (newValue == null) return "";
                return newValue;
            }
            set
            {
                if (value == null || value.Equals("") || value.Equals(currentValue))
                {
                    newValue = null;
                }
                else
                {
                    newValue = value;
                }
                log.Debug("ConfigItem " + name + " set to new value " + newValue);
            }
        }
    
public void ResetValue()
{
    if (HasChanged)
    {
        currentValue = newValue;
        newValue = null;
    }
}

        /**
         * @param applyInitMethod Specifies whether init should be called. Can be overruled
         * by an additional tag alwaysCallInit
         */
        public void CallInitMethod(bool applyInitMethod)
        {
            if (!applyInitMethod && !alwaysCallInit) return;
            if (initClass == null || initMethod == null) return;

            // call without parameter
            try
            {
                Type clazz = Type.GetType(initClass);
                //Class <?> clazz = Class.forName(initClass);

                if (initParameter)
                {
                    //clazz.getMethod(initMethod, string.class).invoke(null, newValue);
                    clazz.GetMethod(initMethod).Invoke(null, new object[] { newValue });

                }
                else
                {
                    clazz.GetMethod(initMethod).Invoke(null, null);
                }
            }
            catch (Exception e)
            {
                log.Error("Config profile: cannot call initMethod, Exception = " + e.Message);
            }
        }
    
    
    override public string ToString()
{
    StringBuilder s = new StringBuilder();
    s.Append("Configuration Item: name = " + name + ", type = " + type);
    s.Append(", current value = " + CurrentValue);
    s.Append(", new value = " + NewValue);
    if (allowedValues != null)
    {
        s.Append(", allowedValues = " + allowedValues);
    }
    if (formatMask != null)
    {
        s.Append(", formatMask = " + formatMask);
    }

    return s.ToString();
}
    
    
    
    }
}
