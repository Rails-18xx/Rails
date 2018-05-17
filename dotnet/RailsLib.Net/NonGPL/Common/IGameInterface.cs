using System;


namespace GameLib.Net.Common
{
    public interface IGameInterface
    {
        IXmlLoader XmlLoader { get; set; }
        IGameFileInterface GameFileInterface { get; set; }
    }
}
