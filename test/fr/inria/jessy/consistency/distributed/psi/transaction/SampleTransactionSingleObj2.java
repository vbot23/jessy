package fr.inria.jessy.consistency.distributed.psi.transaction;

import fr.inria.jessy.Jessy;
import fr.inria.jessy.entity.SampleEntityClass;
import fr.inria.jessy.transaction.ExecutionHistory;
import fr.inria.jessy.transaction.Transaction;

public class SampleTransactionSingleObj2 extends Transaction {

	public SampleTransactionSingleObj2(Jessy jessy) throws Exception{
		super(jessy);
	}

	@Override
	public ExecutionHistory execute() {
		try {
	
			Thread.sleep(1000);
			
			SampleEntityClass se=read(SampleEntityClass.class, "1");			
			se.setData("Second Trans");
			write(se);

			Thread.sleep(5000);
			
			return commitTransaction();			
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}		
	}

}
