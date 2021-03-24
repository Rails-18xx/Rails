package net.sf.rails.game.specific._18Chesapeake;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartPacket;

public class CompanyManager_18Chesapeake extends CompanyManager {

	public CompanyManager_18Chesapeake(RailsRoot parent, String id) {
		super(parent, id);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void configureStartPacket(Tag packetTag, String name, String roundClass) throws ConfigurationException {
		StartPacket_18Chesapeake sp =  StartPacket_18Chesapeake.create(this, name, roundClass);
		startPackets.add(sp);
		startPacketMap.put(name, sp);

		sp.configureFromXML(packetTag);
	}

}
