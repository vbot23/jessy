package fr.inria.jessy.communication;

import java.util.Collection;

import net.sourceforge.fractal.FractalManager;
import net.sourceforge.fractal.Learner;
import net.sourceforge.fractal.membership.Group;
import net.sourceforge.fractal.wanamcast.WanAMCastStream;
import fr.inria.jessy.transaction.termination.message.TerminateTransactionRequestMessage;


public class GenuineTerminationCommincation extends TerminationCommunication{

	/**
	 * Stream used for atomic multicast messages
	 */
	protected WanAMCastStream aMCastStream;
	
	public GenuineTerminationCommincation(Group group, Group all, Learner learner) {
		super(group, all, learner);
		
		aMCastStream = FractalManager.getInstance()
				.getOrCreateWanAMCastStream(group.name(), group.name());
		aMCastStream.registerLearner("TerminateTransactionRequestMessage", learner);
		aMCastStream.start();
	}

	@Override
	public void sendTerminateTransactionRequestMessage(
			TerminateTransactionRequestMessage msg, Collection<String> dest) {
		aMCastStream.atomicMulticast(msg);
	}

}
