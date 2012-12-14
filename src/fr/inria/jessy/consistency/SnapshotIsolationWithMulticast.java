package fr.inria.jessy.consistency;

import net.sourceforge.fractal.Learner;
import net.sourceforge.fractal.membership.Group;
import fr.inria.jessy.communication.GenuineTerminationCommunication;
import fr.inria.jessy.communication.TerminationCommunication;
import fr.inria.jessy.store.DataStore;

public class SnapshotIsolationWithMulticast extends SnapshotIsolation {

	public SnapshotIsolationWithMulticast(DataStore store) {
		super(store);
	}

	@Override
	public TerminationCommunication getOrCreateTerminationCommunication(
			Group group, Learner learner) {
		if (terminationCommunication == null) {
			terminationCommunication = new GenuineTerminationCommunication(
					group, learner);

		}
		return terminationCommunication;
	}


}
