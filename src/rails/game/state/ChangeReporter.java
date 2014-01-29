package rails.game.state;

public interface ChangeReporter {

    void setChangeStack(ChangeStack changeStack);
    
    void addMessage(String message);

    void close(ChangeSet set);
    
    void update();
    
}
