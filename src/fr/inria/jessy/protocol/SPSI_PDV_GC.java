package fr.inria.jessy.protocol;

import static fr.inria.jessy.transaction.ExecutionHistory.TransactionType;
import static fr.inria.jessy.transaction.ExecutionHistory.TransactionType.BLIND_WRITE;
import static fr.inria.jessy.vector.Vector.CompatibleResult;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import fr.inria.jessy.ConstantPool;
import fr.inria.jessy.communication.JessyGroupManager;
import fr.inria.jessy.communication.message.TerminateTransactionRequestMessage;
import fr.inria.jessy.consistency.SPSI;
import fr.inria.jessy.store.DataStore;
import fr.inria.jessy.store.JessyEntity;
import fr.inria.jessy.store.ReadReply;
import fr.inria.jessy.store.ReadRequest;
import fr.inria.jessy.transaction.ExecutionHistory;
import fr.inria.jessy.transaction.TransactionHandler;
import fr.inria.jessy.transaction.termination.vote.Vote;
import fr.inria.jessy.transaction.termination.vote.VotePiggyback;
import fr.inria.jessy.transaction.termination.vote.VotingQuorum;
import fr.inria.jessy.vector.PartitionDependenceVector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SPSI_PDV_GC extends SPSI {
    private static ConcurrentHashMap<UUID, PartitionDependenceVector<String>> receivedVectors;

    static {
        votePiggybackRequired = true;
        receivedVectors = new ConcurrentHashMap<>();
        // TODO: I think this should be here, but NMSI_PDV_GC doesn't have it
        // TODO: ConstantPool.PROTOCOL_ATOMIC_COMMIT = ConstantPool.ATOMIC_COMMIT_TYPE.ATOMIC_MULTICAST;
    }

    public SPSI_PDV_GC(JessyGroupManager m, DataStore s) {
        super(m, s);
    }

    /**
     * Since we want the transaction in a single replica to be serializable, we don't want commutativity when applying
     * transaction, i.e. we apply one transaction at a time.
     *
     * @return false, since we want serializability in one replica's inner transaction.
     */
    @Override
    public boolean applyingTransactionCommute() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Readonly transaction always commit (and also INIT_TRANSACTION). For other transaction we check for read-write
     * and write-write conflicts with every transaction that committed before. If a conflict exists we reject
     * certification.
     *
     * @param history The {@link ExecutionHistory} of the transaction to certify.
     * @return {@code true} if the transaction passed the certification check, {@code false} otherwise.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean certify(ExecutionHistory history) {
        TransactionType type = history.getTransactionType();

        // If a transaction is an init transaction, we load the partitioned dependence vectors and always commit.
        if (type == TransactionType.INIT_TRANSACTION) {
            String groupName = manager.getMyGroup().name();

            // For each entity we load an initial only-zeros dependence vector.
            // TODO: Probably the best place where to do this initialization is in the method createEntity. See
            // TODO: NMSI_PDV_GC#createEntity.
            for (JessyEntity e : history.getCreateSet().getEntities())
                e.setLocalVector(new PartitionDependenceVector<>(groupName, 0));

            return true;
        }

        // If a transaction is readonly it should always commit.
        if (type == TransactionType.READONLY_TRANSACTION) {
            return true;
        }

        Set<JessyEntity> checkEntities = new HashSet<>();

        if (isMarkedSerializable(history)) {
            checkEntities.addAll(history.getReadSet().getEntities());
        } else {
            // If an operation is neither an init nor a read only operation we treat the create operations as update
            // operations. Then for each update operation we check its compatibility.
            checkEntities.addAll(history.getCreateSet().getEntities());
            checkEntities.addAll(history.getWriteSet().getEntities());
        }

        for (JessyEntity e : checkEntities) {
            // Check only for local entities. read-write conflicts and write-write conflicts should be checked
            // locally, according to SPSI consistency.
            if (manager.getPartitioner().isLocal(e.getKey())) {
                // The write set of all transaction Tj that committed before Ti (that is, this transaction) is the
                // set of entities in the data store.

                // Given a JessyEntity e from the set cs(Ti) U ws(Ti) we get the version of the same data item stored
                // in the database.
                ReadRequest<JessyEntity> request = new ReadRequest<>((Class<JessyEntity>) e.getClass(), "secondaryKey",
                        e.getKey(), null);
                ReadReply<JessyEntity> reply = store.get(request);

                // The entity we requested could not be in the data store (this transaction created the entity).
                if (reply.getEntity() != null) {
                    JessyEntity last = reply.getEntity().iterator().next();

                    if (isMarkedSerializable(history)) {
                        if (last.getLocalVector().getSelfValue() > e.getLocalVector().getSelfValue())
                            return false;
                    } else {
                        // We check if e and the value stored in the database are compatible. If not, we reject
                        // certification.
                        // TODO: Write down a comment to say when two operations are compatible.
                        if (last.getLocalVector().isCompatible(e.getLocalVector()) != CompatibleResult.COMPATIBLE)
                            return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The vote piggyback is the PDV vector.
     *
     * @param history The {@link ExecutionHistory} to certify.
     * @param object  The piggyback, that is the partitioned dependency vector.
     * @return A {@link Vote} instance representing the result of the certification process.
     */
    @Override
    public Vote createCertificationVote(ExecutionHistory history, Object object) {
        // This is almost a copy-paste of the super class' method. Here we add the piggyback (instead of null).
        boolean isCommitted = history.getTransactionType() == BLIND_WRITE || certify(history);

        // The object parameter is created in method transactionDeliveredForTermination(), in the line
        // msg.setComputedObjectUponDelivery(vector.clone()). It is the partitioned dependency vector.
        return new Vote(history.getTransactionHandler(), isCommitted, manager.getMyGroup().name(),
                new VotePiggyback(object));
    }

    /**
     * Free the PDV vectors we were collecting.
     *
     * @param history The {@link ExecutionHistory} whose vectors we want to free.
     */
    private void freeVectors(ExecutionHistory history) {
        // Garbage collect the received vectors.
        if (receivedVectors.containsKey(history.getTransactionHandler().getId()))
            receivedVectors.remove(history.getTransactionHandler().getId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * When aborting we only need to free the PDV vectors we were collecting.
     *
     * @param msg  The termination message.
     * @param Vote The vote used during certification.
     */
    @Override
    public void postAbort(TerminateTransactionRequestMessage msg, Vote Vote) {
        freeVectors(msg.getExecutionHistory());
    }

    @Override
    public void postCommit(ExecutionHistory history) {
        if (history.getTransactionType() != TransactionType.INIT_TRANSACTION) {
            PartitionDependenceVector<String> comVec = receivedVectors.get(history.getTransactionHandler().getId());
            PartitionDependenceVector.lastCommit = comVec.clone();
        }

        freeVectors(history);
    }

    @Override
    public void prepareToCommit(TerminateTransactionRequestMessage msg) {
        ExecutionHistory history = msg.getExecutionHistory();
        PartitionDependenceVector<String> pdv = receivedVectors.get(history.getTransactionHandler().getId());

        // Assigning pdv to the entities.
        for (JessyEntity e : history.getWriteSet().getEntities()) {
            e.setLocalVector(pdv.clone());
            e.temporaryObject = null;
            e.getLocalVector().setSelfKey(manager.getMyGroup().name());
        }
    }

    @Override
    public boolean transactionDeliveredForTermination(ConcurrentLinkedHashMap<UUID, Object> terminatedTransactions,
                                                      ConcurrentHashMap<TransactionHandler, VotingQuorum> quorums,
                                                      TerminateTransactionRequestMessage msg) {
        try {
            if (msg.getExecutionHistory().getTransactionType() != TransactionType.INIT_TRANSACTION) {
                int seqNo = PartitionDependenceVector.lastCommitSeqNo.incrementAndGet();
                PartitionDependenceVector<String> vector = new PartitionDependenceVector<>();

                for (JessyEntity entity : msg.getExecutionHistory().getReadSet().getEntities())
                    vector.update(entity.getLocalVector());

                vector.update(PartitionDependenceVector.lastCommit.clone());
                vector.setSelfKey(manager.getMyGroup().name());
                vector.setValue(vector.getSelfKey(), seqNo);
                msg.setComputedObjectUponDelivery(vector.clone());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void voteReceived(Vote vote) {
        // Init (or read only) transaction don't have piggybacks.
        if (vote.getVotePiggyBack().getPiggyback() == null)
            return;

        try {
            PartitionDependenceVector<String> comVec = (PartitionDependenceVector<String>) vote
                    .getVotePiggyBack().getPiggyback();
            PartitionDependenceVector<String> recVec = receivedVectors.putIfAbsent(vote.getTransactionHandler().getId(),
                    comVec);

            if (recVec != null)
                recVec.update(comVec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}