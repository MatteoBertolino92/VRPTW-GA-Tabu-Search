package com.TabuSearch;

import java.util.List;

import org.coinor.opents.*;

import com.mdvrp.Customer;
import com.mdvrp.Depot;
import com.mdvrp.Instance;
import com.mdvrp.Route;

@SuppressWarnings("serial")
public class MyMoveManager implements MoveManager {
	private static final double INFINITY = 999999999;
	private static Instance instance;
    private MovesType movesType;
    private double d1 =0;
    private double d2 =0;
    private double min=INFINITY;
    private int index=0;

	public MyMoveManager(Instance instance) 
	{
    	MyMoveManager.setInstance(instance);
    }
    
    public Move[] getAllMoves( Solution solution ) 
    { 
    	MySolution sol = ((MySolution)solution); 
    	switch (movesType) 
    	{
			case SWAP:
				return getSwapMoves(sol);
			default:
				return getSwapMoves(sol);
		}
    }   // end getAllMoves
    
    /**
     * Generate moves that move each customer from one route to all routes that are different
     * @param solution
     * @return
     */
    public Move[] getSwapMoves(MySolution solution)
    {	 
    	 Route[][] routes = solution.getRoutes();
         Move[] buffer = new Move[ getInstance().getCustomersNr() * getInstance().getVehiclesNr() * getInstance().getDepotsNr()];
         int nextBufferPos = 0;
              
         // iterates depots
         for (int i = 0; i < routes.length; ++i) 
         {
         	// iterates routes
         	for (int j = 0; j < routes[i].length; ++j) 
         	{
         		// iterates customers in the route
         		for (int k = 0; k < routes[i][j].getCustomersLength(); ++k) 
         		{
         			index = routes[i][j].getCustomer(k).getNumber();
         			/**Per il customer attuale, metti in d1 la distanza con il precedente**/
         			if (k!=0)
         			{
         				Customer prec = routes[i][j].getCustomer(k-1);
         				d1 = instance.restituisciDistance(prec.getNumber(), index);
         			}
         			
         			for(int l = 0; l < routes.length; ++l)
         			{
	         			// iterate each route for that deposit and generate move to it if is different from the actual route
	         			for (int r = 0; r < routes[l].length; ++r) 
	         			{
	         				
	         			/**Ora analizzo le altre route: per ognuna di questa, prendo i clienti.
	         			 * Itero su essi.
	         			 * Trovo la distanza minima di questi dal mio attuale.
	         			 * Se questa distanza minima è < di d1, significa che la route considerata è plausibile, entro in IF.
	         			 * Altrimenti, non entro in IF**/
	         					
	         				List <Customer> list = routes[l][r].getCustomers();
	         				for ( Customer gg:list )
	         				{
	         					double d_tmp = instance.restituisciDistance(index, gg.getNumber());
	         					if (d_tmp < min)
	         						min = d_tmp;
	         				}
	         				
	         				if (!(r==j && i == l) ) 
	         				{		
	         					if (min<1.9*d1)
	         					{
	         						//i==l per verificare se è lo stesso deposito		         						
	         						Customer customer = routes[i][j].getCustomer(k);
	         						buffer[nextBufferPos++] = new MySwapMove(getInstance(), customer, i, j, k, l, r);
	         					}
	         				}
	         			}
         			} //tutte le route 	         		
         			min=INFINITY;
         		} //customers
         	} //route costumer
         } //depot 
         
         // Trim buffer
         Move[] moves = new Move[ nextBufferPos];
         System.arraycopy( buffer, 0, moves, 0, nextBufferPos );
        // System.out.println(moves.length);
         return moves; //in quali altri route lo posso mettere? 100 cust, 25 route, lo sposta.
    }
    
	/**
	 * @return the movesType
	 */
	public MovesType getMovesType() {
		return movesType;
	}
	
	/**
	 * @param movesType the movesType to set
	 */
	public void setMovesType(MovesType movesType) {
		this.movesType = movesType;
	}
	
	/**
	 * @return the instance
	 */
	public static Instance getInstance() {
		return instance;
	}
	
	/**
	 * @param instance the instance to set
	 */
	public static void setInstance(Instance instance) {
		MyMoveManager.instance = instance;
	}
    
}   // end class MyMoveManager