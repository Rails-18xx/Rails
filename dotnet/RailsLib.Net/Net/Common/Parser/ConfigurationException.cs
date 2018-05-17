using System;


namespace GameLib.Net.Common.Parser
{
    public class ConfigurationException : Exception
    {
        public ConfigurationException() : base()
        {

        }

        public ConfigurationException(string msg) : base(msg)
        {

        }

        public ConfigurationException(string msg, Exception e) : base(msg, e)
        {

        }

        public ConfigurationException(Exception e) : base(e.Message, e)
        {

        }
    }
}
