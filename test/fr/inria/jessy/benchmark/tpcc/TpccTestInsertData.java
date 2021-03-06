package fr.inria.jessy.benchmark.tpcc;

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.yahoo.ycsb.measurements.Measurements;

import fr.inria.jessy.ConstantPool;
import fr.inria.jessy.Jessy;
import fr.inria.jessy.JessyFactory;
import fr.inria.jessy.benchmark.tpcc.entities.Customer;
import fr.inria.jessy.benchmark.tpcc.entities.District;
import fr.inria.jessy.benchmark.tpcc.entities.History;
import fr.inria.jessy.benchmark.tpcc.entities.Item;
import fr.inria.jessy.benchmark.tpcc.entities.New_order;
import fr.inria.jessy.benchmark.tpcc.entities.Order;
import fr.inria.jessy.benchmark.tpcc.entities.Order_line;
import fr.inria.jessy.benchmark.tpcc.entities.Stock;
import fr.inria.jessy.benchmark.tpcc.entities.Warehouse;
import fr.inria.jessy.transaction.ExecutionHistory;
import fr.inria.jessy.transaction.TransactionState;
import fr.inria.jessy.utils.Configuration;

/**
 * @author WANG Haiyun & ZHAO Guang
 * 
 */
public class TpccTestInsertData extends TestCase{

	Jessy jessy;
	InsertData id;
	Warehouse wh;
	District di;
	Item it;
	Customer cu;

	static int _warehauses;

	static {
		final String warehouses = Configuration
				.readConfig(ConstantPool.WAREHOUSES_NUMBER);
		_warehauses= Integer.parseInt(warehouses);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		PropertyConfigurator.configure("log4j.properties");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		Properties props = new Properties();
		//		TODO ADD consistency, move to wrapper
		Measurements.setProperties(props);

		jessy =JessyFactory.getLocalJessy();
		jessy.registerClient(this);

		jessy.addEntity(Warehouse.class);
		jessy.addEntity(District.class);
		jessy.addEntity(Customer.class);
		jessy.addEntity(Item.class);
		jessy.addEntity(Stock.class);
		jessy.addEntity(History.class);
		jessy.addEntity(Order.class);
		jessy.addEntity(New_order.class);
		jessy.addEntity(Order_line.class);

	}

	/**
	 * Test method for {@link fr.inria.jessy.BenchmarkTpcc.InsertData}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInsertData() throws Exception {

		ExecutionHistory result;

		for(int i=1;i<=_warehauses;i++){

			id = new InsertData(jessy,i);


			result = id.execute();
			/* test execution */
			assertEquals("Result", TransactionState.COMMITTED,
					result.getTransactionState());
		}
		jessy.close(this);
	}

}
