package rails.algorithms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rails.game.BaseToken;
import rails.game.City;
import rails.game.MapHex;
import rails.game.PublicCompanyI;
import rails.game.Station;
import rails.game.TokenI;

public final class NetworkVertex implements Comparable<NetworkVertex> {

    private static enum VertexType {
        STATION,
        SIDE,
        HQ
    }
    
    private final VertexType type;
    
    private final MapHex hex;
    
    private final Station station;
    private final int side;
    
    private boolean tokenable;
    private Set<PublicCompanyI> companiesHaveToken;
    private int tokenSlots;

    public NetworkVertex(MapHex hex, Station station) {
        this.type = VertexType.STATION;
        this.hex = hex;
        this.station = station;
        this.side = 0;
        
        if (station.getBaseSlots() == 0){
            this.tokenable = false;
        } else {
            this.tokenable = true;
            // find tokens
            List<TokenI> tokens = null;
            this.tokenSlots = 0;
            List<City> cities = hex.getCities();
            for (City city:cities) {
                if (station == city.getRelatedStation()) {
                    tokens = city.getTokens();
                    this.tokenSlots = city.getSlots();
                    break;
                }
            }
            if (tokens == null)
                this.companiesHaveToken = null;
            else {
                this.companiesHaveToken = new HashSet<PublicCompanyI>();
                for (TokenI token:tokens) {
                    if (token instanceof BaseToken) {
                        BaseToken baseToken = (BaseToken)token;
                        this.companiesHaveToken.add(baseToken.getCompany());
                    }
                }
            }
        }
    }
    
    public NetworkVertex(MapHex hex, int side) {
        this.type = VertexType.SIDE;
        this.hex = hex;
        this.station = null;
        this.side = (side % 6);
        this.tokenable = false;
        this.companiesHaveToken = null;
        this.tokenSlots = 0;
    }
    
    public NetworkVertex(PublicCompanyI company) {
        this.type = VertexType.HQ;
        this.hex = null;
        this.station = null;
        this.side = 0;
        this.tokenable = false;
        this.companiesHaveToken = null;
        this.tokenSlots = 0;
    }
    
    public boolean isStation(){
        return type == VertexType.STATION;
    }
    
    public boolean isSide(){
        return type == VertexType.SIDE;
    }
    
    public boolean isHQ(){
        return type == VertexType.HQ;
    }

    public MapHex getHex(){
        return hex;
    }
    
    public Station getStation(){
        return station;
    }
    
    public int getSide(){
        return side;
    }
    
    public boolean isFullyTokened(){
        return tokenable && companiesHaveToken.size() == tokenSlots;
    }

    public boolean hasCompanyToken(PublicCompanyI company) {
        return !(company == null) && companiesHaveToken.contains(company);
    }
    
    public String printTokens(){
        if (!tokenable) return "Not tokenable";
        
        StringBuffer result = new StringBuffer("Tokens:");
        for (PublicCompanyI company:companiesHaveToken)
            result.append(" " + company.getName());
        if (isFullyTokened()) result.append(", fully tokened"); 
        return result.toString();
    }
    
    public String getIdentifier(){
        if (isStation())
            return hex.getName() + "." + -station.getNumber();
        else if (isSide())
            return hex.getName() + "." + side; 
        else
            return "HQ";
    }
    
    @Override
    public String toString(){
        StringBuffer message = new StringBuffer();
        if (isStation()) 
            message.append( hex.getName() + "." + station.getNumber());
        else if (isSide())
            message.append(hex.getName() + "." + hex.getOrientationName(side));
        else 
            message.append("HQ");
        if (isFullyTokened())
            message.append("/*");
        return message.toString();
    }
    
    public int compareTo(NetworkVertex otherVertex) {
        return this.getIdentifier().compareTo(otherVertex.getIdentifier());
    }
    
}
