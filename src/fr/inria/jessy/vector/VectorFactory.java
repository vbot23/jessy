package fr.inria.jessy.vector;

import fr.inria.jessy.ConstantPool;
import fr.inria.jessy.communication.JessyGroupManager;
import fr.inria.jessy.utils.Configuration;

public class VectorFactory {

	private static String consType = Configuration.readConfig(ConstantPool.CONSISTENCY_TYPE);
	
	private JessyGroupManager manager;
	
	public VectorFactory(JessyGroupManager m) {
		manager = m;
		if (consType.equals("psi")) {
			VersionVector.init(manager);
		}
		if (consType.equals("nmsi2") || consType.equals("us2")) {
			GMUVector.init(manager);
		}		
	}	
	
	public <K> Vector<K> getVector(K selfKey) {
		if (consType.equals("nmsi") || consType.equals("us")) {
			return new DependenceVector<K>(selfKey);
		}
		if (consType.equals("rc")) {
			return new NullVector<K>(selfKey);
		}
		if (consType.equals("ser")) {
			return new LightScalarVector<K>(selfKey);
		}
		if (consType.equals("si") || consType.equals("si2")) {
			return new ScalarVector<K>();
		}
		if (consType.equals("psi")) {
			return new VersionVector(manager.getMyGroup().name(), 0);
		}
		if (consType.equals("nmsi2") || consType.equals("us2")) {
			return new GMUVector<K>();
		}
		return null;
	}

	public boolean needExtraObject() {

		if (consType.equals("nmsi2") || consType.equals("us2")) {
			return true;
		}

		return false;
	}

}
