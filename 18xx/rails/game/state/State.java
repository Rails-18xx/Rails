package rails.game.state;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;
import rails.game.move.StateChange;
import rails.util.Util;


public class State extends ModelObject implements StateI {
	
    protected String name;
	protected Object object = null;
	protected Class clazz = null;
	
	protected static Logger log = Logger.getLogger(State.class.getPackage().getName());

	public State (String name, Class clazz) {
	    this.name = name;
		if (clazz == null) {
			new Exception ("NULL class not allowed in creating State wrapper")
				.printStackTrace();
		} else {
			this.clazz = clazz;
		}
	}
	
	public State (String name, Object object) {
	    this.name = name;
		if (object == null) {
			new Exception ("NULL object not allowed in creating State wrapper")
				.printStackTrace();
		} else if (clazz != null && Util.isInstanceOf(object, clazz)) {
			new Exception ("Object "+object+" must be instance of "+clazz)
				.printStackTrace();
		} else {
			this.object = object;
			if (clazz == null) clazz = object.getClass();
		}
	}
	
	public void set (Object object) {
		if (object == null) {
		    if (this.object != null) 
		        new StateChange (this, object);
		} else if (Util.isInstanceOf (object, clazz)) {
		    if (!object.equals(this.object)) 
		        new StateChange (this, object);
		} else {
		    log.error("Incompatible object type "+object.getClass().getName()
					+ "passed to " + getClassName() + " wrapper for object type "+clazz.getName()+" at:",
					new Exception (""));
		}
	}

	public Object getState() {
		return object;
	}

	/** Must only be called by the Move execute() and undo() methods */
	public void setState(Object object) {
		this.object = object;
		//log.debug (getClassName() + " "+name+" set to "+object);
		notifyViewObjects();
	}
	
	public String getName() {
	    return name;
	}
	
	public String getText() {
	    if (object != null) {
	        return object.toString();
	    } else {
	        return "";
	    }
	}
	
	public String toString () {
	    return name;
	}
	
	public String getClassName() {
	    return getClass().getName().replaceAll(".*\\.", "");
	}

}
