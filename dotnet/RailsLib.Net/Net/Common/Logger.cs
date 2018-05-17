using System;
using System.Diagnostics;


namespace GameLib.Net.Common
{
    public class LoggerBase
    {
        static private TraceSwitch appSwitch = new TraceSwitch("gameLibSwitch", "Switch in config file");
        static private LoggerBase instance;
        //private LoggingChannel channel;

        // we'll make this do something later
        static public LoggerBase Instance
        {
            get
            {
                // #fix_threading
                if (instance == null)
                {
                    instance = new LoggerBase();
                    appSwitch.Level = TraceLevel.Verbose;
                }
                return instance;
            }
        }

        public void Log(string msg, TraceLevel level)
        {
            if (level <= appSwitch.Level)
            {
                Trace.WriteLine(msg);
            }
        }

        public static TraceLevel Level
        {
            get
            {
                return appSwitch.Level;
            }
            set
            {
                appSwitch.Level = value;
            }
        }
    }

    public class Logger<T>
    {
        private string name;

        public Logger()
        {
            name = typeof(T).Name;
        }

        public void Log(string msg, TraceLevel level)
        {
            LoggerBase.Instance.Log($"{name}: {msg}", level);
        }

        // These are from highest to lowest value
        //public void Critical(string msg)
        //{
        //    Log(msg, TraceLevel.Critical);
        //}

        public void Error(string msg)
        {
            Log(msg, TraceLevel.Error);
        }

        public void Error(string msg, Exception e)
        {
            Log($"{msg} : {e.Message}", TraceLevel.Error);
        }

        public void Warn(string msg)
        {
            Log(msg, TraceLevel.Warning);
        }

        public void Info(string msg)
        {
            Log(msg, TraceLevel.Info);
        }

        public void Debug(string msg)
        {
            Log(msg, TraceLevel.Verbose);
        }

    }
}
