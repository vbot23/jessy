package fr.inria.jessy.vector;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;

import com.sleepycat.persist.model.Persistent;

import fr.inria.jessy.ConstantPool;
import fr.inria.jessy.communication.JessyGroupManager;
import fr.inria.jessy.store.DataStore;
import fr.inria.jessy.store.JessyEntity;
import fr.inria.jessy.store.ReadRequest;

@Persistent
public abstract class Vector<K> extends ValueVector<K, Integer> implements Cloneable,
		Externalizable {

	private static final long serialVersionUID = ConstantPool.JESSY_MID;

	public enum CompatibleResult {
		/**
		 * The two vectors are compatibles.
		 */
		COMPATIBLE,
		/**
		 * The two vectors are not compatibles.
		 */
		NOT_COMPATIBLE_TRY_NEXT,
		/**
		 * The two vectors are not compatibles and any version of this entity
		 * can be compatible with this vector
		 */
		NEVER_COMPATIBLE,
	};

	K selfKey;

	private static final Integer _bydefault = -1;

	/**
	 * needed by BerkleyDB
	 */
	@Deprecated
	public Vector() {
		super(_bydefault);
	}

	public Vector(K selfKey) {
		super(_bydefault);
		this.selfKey = selfKey;
	}

	/**
	 * This method is called inside {@code Consistency#certify} method. It
	 * returns true if the modified entity with the {@link other} Vector is
	 * compatible with this vector (i.e., the last committed entity).
	 * 
	 * @param other
	 * @return
	 * @throws NullPointerException
	 */
	public abstract CompatibleResult isCompatible(Vector<K> other)
			throws NullPointerException;

	/**
	 * This method is called in
	 * {@code DataStore#get(fr.inria.jessy.store.ReadRequest)} upon receiving a
	 * read request. An entity will be returned such that its {@code Vector} is
	 * compatible with the received {@code CompactVector}
	 * 
	 * @param other
	 *            the compactVector containing all previously read entities.
	 * @return true if the entity can be read, otherwise false.
	 * @throws NullPointerException
	 */
	public abstract CompatibleResult isCompatible(CompactVector<K> other)
			throws NullPointerException;

	public void update(CompactVector<K> readSet, CompactVector<K> writeSet) {
		return;
	}

	public void setSelfKey(K selfKey) {
		this.selfKey = selfKey;
	}

	public K getSelfKey() {
		return selfKey;
	}

	public Integer getSelfValue() {
		return super.getValue(selfKey);
	}

	@SuppressWarnings("unchecked")
	public Vector<K> clone() {
		Vector<K> result = (Vector<K>) super.clone();
		result.selfKey = selfKey;

		return result;
	}

	/**
	 * Increament the value of selfKey;
	 */
	public void increment() {
		setValue(selfKey, (getSelfValue() + 1));
	}

	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		map = (HashMap<K, Integer>) in.readObject();
		super.setBydefault(_bydefault);
		selfKey = (K) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(map);
		out.writeObject(selfKey);
	}

	/**
	 * A server sometimes need to send some additional information to the proxy.
	 * These informations are put either in entityLocalVector or entityTemproryObject.
	 * Ultimately, they must put in the compact vector temprory object.
	 * 
	 * @param entityLocalVector
	 * @param entityTemproryObject
	 * @param compactVectorExtraObjectContainer
	 */
	public void updateExtraObjectInCompactVector(Vector<K> entityLocalVector, Object entityTemproryObject, ExtraObjectContainer compactVectorExtraObjectContainer) {
		return;
	}

	/**
	 * Implements this method if the vector can be loaded from persistent storage.
	 * This method is called only once during system initialization to initialize the vector's static members.
	 * 
	 * @param m
	 */
	public synchronized void init(JessyGroupManager m){
		return;
	}
	
	/**
	 * Implements this method if the vector needs to be persistent.
	 * This method is called once {@link DataStore} is being closed.
	 */
	public void makePersistent(){
		return;
	}
	
	public boolean prepareRead(ReadRequest rr){
		return true;
	}
	
	public void postRead(ReadRequest rr, JessyEntity entity){
		return;
	}
}
