package fr.inria.jessy.vector;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sleepycat.persist.model.Persistent;

import fr.inria.jessy.ConstantPool;

/**
 * This is a simple ScalarVector implementation. It will be used for
 * implementing p-store like data store. I.e., during execution, read the most
 * recent committed versions. During termination, check to see if the already
 * read values have been modified or not.
 * <p>
 * The main difference between this class and {@code ScalarVector} is that this
 * implementation is so light. It does not consider reading consistent snapshots
 * during reads. Thus, it may end up taking an inconsistent snapshot from the
 * database.
 * 
 * 
 * @author Masoud Saeida Ardekani
 * 
 * @param <K>
 */
@Persistent
public class LightScalarVector<K> extends Vector<K> implements Externalizable {

	private static final long serialVersionUID = -ConstantPool.JESSY_MID;
	
	/**
	 * Needed for BerkeleyDB
	 */
	@Deprecated
	public LightScalarVector() {
	}

	public LightScalarVector(K selfKey) {
		super(selfKey);
		super.setValue(selfKey, 0);
	}

	@Override
	public CompatibleResult isCompatible(Vector<K> other) throws NullPointerException {

		if (getSelfValue().equals(other.getSelfValue()))
			return Vector.CompatibleResult.COMPATIBLE;
		else
			return Vector.CompatibleResult.NOT_COMPATIBLE_TRY_NEXT;

	}

	/**
	 * This method always returns true. This leads to reading the very last
	 * committed entity.
	 */
	@Override
	public CompatibleResult isCompatible(CompactVector<K> other)
			throws NullPointerException {
		return Vector.CompatibleResult.COMPATIBLE;
	}

	@Override
	public void update(CompactVector<K> readSet, CompactVector<K> writeSet) {
		setValue(getSelfKey(), getSelfValue() + 1);
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
	}

	@Override
	public String toString() {
		return selfKey + " : " + getSelfValue();
	}

}
