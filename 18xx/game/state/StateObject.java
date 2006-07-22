package game.state;

import util.Util;

public class StateObject implements StateI {
	
    private String name;
	private Object object = null;
	private Class clazz = null;
	
	public StateObject (String name, Class clazz) {
	    this.name = name;
		if (clazz == null) {
			new Exception ("NULL class not allowed in creating State wrapper")
				.printStackTrace();
		} else {
			this.clazz = clazz;
		}
	}
	
	public StateObject (String name, Object object) {
	    this.name = name;
		if (object == null) {
			new Exception ("NULL object not allowed in creating State wrapper")
				.printStackTrace();
		} else {
			this.object = object;
			this.clazz = object.getClass();
		}
	}

	public Object getState() {
		return object;
	}

	public void setState(Object object) {
		if (object == null || Util.isInstanceOf (object, clazz)) {
			this.object = object;
		} else {
			new Exception ("Incompatible object type "+object.getClass().getName()
					+ "passed to State wrapper for object type "+this.object.getClass().getName()+" at:")
				.printStackTrace();
		}
	}
	
	public String toString() {
	    return name;
	}

}
