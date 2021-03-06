package fr.inria.jessy.store;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;

import fr.inria.jessy.ConstantPool;

public class ReadReply<E extends JessyEntity> implements Externalizable {

	private static final long serialVersionUID = ConstantPool.JESSY_MID;

	int readRequestId;
	Collection<E> entities;

	// FIXME use transaction handler instead.
	
	/**
	 * For externalizable interface
	 */
	@Deprecated
	public ReadReply() {

	}

	public ReadReply(E entity, int correspondingReadRequestId) {
		entities = new ArrayList<E>(1);
		this.entities.add(entity);
		this.readRequestId = correspondingReadRequestId;
	}

	public ReadReply(Collection<E> entities, int correspondingReadRequestId) {
		this.entities = entities;
		this.readRequestId = correspondingReadRequestId;
	}

	public Integer getReadRequestId() {
		return readRequestId;
	}

	public Collection<E> getEntity() {
		return entities;
	}
	
	@Override
	public String toString() {
		return "RReP"+getReadRequestId().toString();
	}

	public synchronized void mergeReply(ReadReply<E> readReply) {
		if (this.readRequestId!=readReply.getReadRequestId())
			throw new IllegalArgumentException("Invalid requestId");
		entities.addAll(readReply.getEntity());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(readRequestId);
		if (entities.size() == 1) {
			out.writeBoolean(true);
			out.writeObject(entities.iterator().next());
		} else {
			out.writeBoolean(false);
			out.writeObject(new ArrayList(entities));
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		readRequestId = in.readInt();
		if (in.readBoolean()) {
			entities = new ArrayList<E>(1);
			entities.add((E) in.readObject());
		} else {
			entities = (Collection<E>) in.readObject();
		}
	}

}
