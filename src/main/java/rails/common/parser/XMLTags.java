package rails.common.parser;

public final class XMLTags {
    /* TAGS */
	public static final String GAME_TAG = "Game";
	public static final String DESCR_TAG = "Description";
	public static final String CREDITS_TAG = "Credits";
	public static final String PLAYERS_TAG = "Players";
	public static final String OPTION_TAG = "GameOption";
	public static final String GAMES_LIST_TAG = "GamesList";
    public static final String NOTE_TAG = "Note";
        
	/* ATTRIBUTES */
	public static final String NAME_ATTR = "name";
	public static final String TEXT_ATTR = "text";
	public static final String MIN_ATTR = "minimum";
	public static final String MAX_ATTR = "maximum";
	public static final String PARM_ATTR = "parm";
	public static final String TYPE_ATTR = "type";
	public static final String DEFAULT_ATTR = "default";
	public static final String VALUES_ATTR = "values";
	public static final String CLASS_ATTR = "class";
    public static final String FILE_ATTR = "file";
    
	public static final String VALUES_DELIM = ",";
	
    /* Used by ComponentManager. */
    public static final String COMPONENT_MANAGER_ELEMENT_ID = "ComponentManager";
    public static final String COMPONENT_ELEMENT_ID = "Component";
}
