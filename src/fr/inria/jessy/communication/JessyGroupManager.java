package fr.inria.jessy.communication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.fractal.FractalManager;
import net.sourceforge.fractal.membership.Group;
import net.sourceforge.fractal.membership.Membership;

import org.apache.log4j.Logger;

import com.yahoo.ycsb.YCSBEntity;

import fr.inria.jessy.ConstantPool;
import fr.inria.jessy.partitioner.Partitioner;
import fr.inria.jessy.partitioner.PartitionerFactory;
import fr.inria.jessy.utils.Configuration;

/**
 * This class wrap up the complexity of {@code FractalManager} by simplifying
 * and uniforming the access for different groups needed inside Jessy.
 * <p>
 * Note: any need for any Fractal group should be made through this class.
 * 
 * @author Masoud Saeida Ardekani
 * 
 */
public class JessyGroupManager {

	private static Logger logger = Logger.getLogger(JessyGroupManager.class);

	private Partitioner partitioner;

	private int sourceId;

	/**
	 * Depending on whether this jessy instance is a proxy or a replica,
	 * myGroup is either myReplicaGroup or groupOfAllInstances
	 */
	private Group myGroup;

	/**
	 * The group of all running jessy instances in the system
	 */
	private Group everybodyGroup;

	/**
	 * A group that contains all the replicas.
	 */
	private Group allReplicaGroup;
	
	/**
	 * The collection of different groups that this jessy instance is member of.
	 * This collection is used in the {@code Partitioner#isLocal(String)} in
	 * order to check whether a group of a key isLocal or not.
	 */
	private Collection<Group> myGroups;

	/**
	 * The sorted list of all replica groups. I.e., Only jessy instances that
	 * replicate jessy entities are in this list.
	 */
	private List<Group> replicaGroups;

	/**
	 * Shows whether the Jessy instance is proxy or not. A jessy instance is
	 * proxy, if it is only being used for forwarding the transactions to the
	 * entity replicas
	 */
	private boolean isProxy;
	
	/**
	 * The number of processes dedicated to each group.
	 */
	private int groupSize=0;

	public FractalManager fractal;
	
	public JessyGroupManager() {

		try {

			String fractalFile = Configuration
					.readConfig(ConstantPool.FRACTAL_FILE);
			
			/*
			 * Initialize Fractal and create the replica groups
			 */
			fractal = new FractalManager();
			fractal.loadFile(fractalFile);
			fractal.membership
			.dispatchPeers(ConstantPool.JESSY_SERVER_GROUP,
					ConstantPool.JESSY_SERVER_PORT,
					getGroupSize());
			replicaGroups = new ArrayList<Group>(
					fractal.membership
							.allGroups(ConstantPool.JESSY_SERVER_GROUP));
			Collections.sort(replicaGroups);
			logger.debug("replicaGroups are : " + replicaGroups);

			/*
			 * Initialize a static group containing all the replicas.
			 */
			Collection<Integer> replicas = new HashSet<Integer>(
					fractal.membership.allNodes());
			allReplicaGroup = fractal.membership.getOrCreateNettyGroup(
					ConstantPool.JESSY_ALL_SERVERS_GROUP,
					ConstantPool.JESSY_ALL_SERVERS_PORT);
			allReplicaGroup.putNodes(replicas);

			/*
			 * Initialize my replica group and myself.
			 */
			fractal.membership.loadIdenitity(null);
			sourceId = fractal.membership.myId();
			myGroups = fractal.membership.myGroups();
			Group myReplicaGroup = null;
			isProxy = true;
			for(Group rgroup: replicaGroups){
				if(rgroup.contains(sourceId)){
					myReplicaGroup = rgroup;
					isProxy = false;
					break;
				}
			}
			
			/*
			 * Initialize a group that contains everybody.
			 */
			everybodyGroup = fractal.membership
					.getOrCreateNettyGroup(
							ConstantPool.JESSY_EVERYBODY_GROUP,
							ConstantPool.JESSY_EVERYBODY_PORT,
							true);
			everybodyGroup.putNodes(replicas); // to ensure only replicas are listening
			logger.debug("everybodyGroup is : " + everybodyGroup);
			
			/*
			 * Start fractal
			 */
			if (!isProxy) {
				logger.info("Server mode");
				myGroup = myReplicaGroup;
			} else {
				logger.info("proxy mode");
				myGroup = everybodyGroup;
			}
			
			logger.debug("myGroup is : " + myGroup);
			
			fractal.start();

			// TODO generalize this
			partitioner = PartitionerFactory
					.getPartitioner(this, YCSBEntity.keyspace);

		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);
		}
	}

	public Group getMyGroup() {
		return myGroup;
	}

	public Collection<Group> getMyGroups() {
		return myGroups;
	}

	public int getSourceId() {
		return sourceId;
	}

	/**
	 * @return a sorted list of the replica groups.
	 */
	public List<Group> getReplicaGroups() {
		return replicaGroups;
	}

	public Partitioner getPartitioner() {
		return partitioner;
	}

	public Group getEverybodyGroup() {
		return everybodyGroup;
	}
	
	public Group getAllReplicaGroup(){
		return allReplicaGroup;
	}

	public boolean isProxy() {
		return isProxy;
	}
	
	public Membership getMembership(){
		return fractal.membership;
	}
	
	public int getGroupSize(){
		if (groupSize>0)
			return groupSize;


		String strGroupSize=Configuration.readConfig(ConstantPool.GROUP_SIZE);
		System.out.println(">>> Group Size is " + strGroupSize + " <<<");
		if (strGroupSize==null || strGroupSize.equals(""))
			groupSize=1;
		else
			groupSize= Integer.parseInt(strGroupSize);

		return groupSize;

	}
}
