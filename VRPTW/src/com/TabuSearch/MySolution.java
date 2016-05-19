package com.TabuSearch;

import java.util.*;

import org.coinor.opents.*;

import com.mdvrp.Cost;
import com.mdvrp.Customer;
import com.mdvrp.Instance;
import com.mdvrp.Route;
import com.mdvrp.Vehicle;

@SuppressWarnings("serial")
public class MySolution extends SolutionAdapter{
	private static Instance instance;
	private static int iterationsDone;
	private Route[][] routes; // stores the routes to be modified on
	private Cost cost;		  // stores the total cost of the routes
	private double alpha;		// α
	private double beta;		// β
	private double gamma;		// γ
	private double delta;		// δ
	private double upLimit;
	private double resetValue;
	private int feasibleIndex;
	private int[][][] Bs;
	private List<MySwapMove> moves = new ArrayList<MySwapMove>();
	private List<Cost> costs = new ArrayList<Cost>();
	
	
	public MySolution() {} // Appease clone()

	public MySolution(Instance instance) {
		MySolution.setInstance(instance);
		cost = new Cost();
		initializeRoutes(instance);
		buildInitialRoutes1(instance);
		// used for input routes from file
		alpha 	= 1;
    	beta 	= 1;
    	gamma	= 1;
    	delta	= 0.005;
    	upLimit = 10000000;
    	resetValue = 0.1;
    	feasibleIndex = 0;
    	MySolution.setIterationsDone(0);
    	Bs = new int[instance.getCustomersNr()][instance.getVehiclesNr()][instance.getDepotsNr()];		
	}
	
	public MySolution(Instance instance, int value) {
		MySolution.setInstance(instance);
		cost = new Cost();
		initializeRoutes(instance);
		//buildInitialRoutes1(instance);
		buildRoutesFromInstance(instance);
		// used for input routes from file
		alpha 	= 1;
    	beta 	= 1;
    	gamma	= 1;
    	delta	= 0.005;
    	upLimit = 10000000;
    	resetValue = 0.1;
    	feasibleIndex = 0;
    	MySolution.setIterationsDone(0);
    	Bs = new int[instance.getCustomersNr()][instance.getVehiclesNr()][instance.getDepotsNr()];		
	}

	public Object clone()
    {   
        MySolution copy = (MySolution)super.clone();
        Route[][] routescopy = new Route[this.routes.length][];
        for (int i = 0; i < this.routes.length; ++i) {
        	routescopy[i] = new Route[this.routes[i].length];
        	for (int j = 0; j < this.routes[i].length; ++j)
        		routescopy[i][j] = new Route(this.routes[i][j]);
        }
        copy.routes        = routescopy;
        copy.cost          = new Cost(this.cost);
        copy.alpha         = this.alpha;
        copy.beta          = this.beta;
        copy.gamma         = this.gamma;
        copy.delta         = this.delta;
        copy.feasibleIndex = this.feasibleIndex;
        copy.Bs            = this.Bs;
        copy.moves         = this.moves;
        copy.costs         = this.costs;
        
        return copy;
    }   // end clone
		
	public void setParameters(double delta, double upLimit, double resetValue){
		this.delta =delta;
		this.upLimit = upLimit;
		this.resetValue = resetValue;
	}
	public void incrementBs(MySwapMove move){
		Bs[move.getCustomerNr()][move.getDeleteRouteNr()][move.getDeleteDepotNr()]++;
	}
	
	public int getBs(MySwapMove move){
		return Bs[move.getCustomerNr()][move.getDeleteRouteNr()][move.getDeleteDepotNr()];
	}
	
	public void addMove(MySwapMove move){
		moves.add(move);
	}
	
	public Route getRoute(int depot, int vehicle){
		return routes[depot][vehicle];
	}
	
	public Cost getCost(){
		return cost;
	}
	
	public int getBsOfMove(MySwapMove move) {
		return Bs[move.getCustomerNr()][move.getInsertRouteNr()][move.getInsertDepotNr()];
	}

	public void updateParameters(double a, double b, double g) {
    	// capacity violation test
    	if (a == 0) {
    		alpha = alpha / (1 + delta);
    	} else {
    		alpha = alpha * (1 + delta);
    		if(alpha > upLimit){
    			alpha = resetValue;
    		}
    	}
    	
    	// duration violation test    	
    	if (b == 0) {
    		beta = beta / (1 + delta);
    	} else {
    		beta = beta * (1 + delta);
    		if(beta > upLimit){
    			beta = resetValue;
    		}
    	}
    	
    	// time window violation test
    	if (g == 0) {
    		gamma = gamma / (1 + delta);
    	} else {
    		gamma = gamma * (1 + delta);
    		if(gamma > upLimit){
    			gamma = resetValue;
    		}
    	}
    	
    }
	
	/**
	 * Initialize the routes, assign vehicle and set the depot
	 * @param instance
	 */
	public void initializeRoutes(Instance instance) {
		routes = new Route[instance.getDepotsNr()][instance.getVehiclesNr()];
		// Creation of the routes; each route starts at the depot
		for (int i = 0; i < instance.getDepotsNr(); ++i)
			for (int j = 0; j < instance.getVehiclesNr(); ++j){
					// initialization of routes
					routes[i][j] = new Route();
					routes[i][j].setIndex(i*(instance.getVehiclesNr()) + j);
					
					// add the depot as the first node to the route
					routes[i][j].setDepot(instance.getDepot(i));
					
					// set the cost of the route
					Cost cost = new Cost();
					routes[i][j].setCost(cost);
					
					// assign vehicle
					Vehicle vehicle = new Vehicle();
					vehicle.setCapacity(instance.getCapacity(i, 0));
					vehicle.setDuration(instance.getDuration(i, 0));
					routes[i][j].setAssignedVehicle(vehicle);
					
				}
	}
	
	/**
	 * Build the initial routes
	 */
	public void buildInitialRoutes1(Instance instance) {
		Route route; // stores the pointer to the current route
		Customer customerChosenPtr; // stores the pointer to the customer chosen from depots list assigned customers
		StringBuffer debug = new StringBuffer();
		int assignedCustomersNr;
		int startCustomer;
		int customerChosen; // serve to cycle j, j+1, ... assignedcustomersnr, 0, ... j-1
		int[] vettore;
		vettore=new int[instance.getCustomersNr()];

		// cycle the list of depots
		for( int i = 0; i < instance.getDepotsNr(); ++i) {
			debug.append("\n");
			assignedCustomersNr = instance.getDepot(i).getAssignedCustomersNr();
			if(instance.getParameters().getStartClient() != -1) {
				startCustomer = instance.getParameters().getStartClient();
			}else{
				startCustomer = instance.getRandom().nextInt(assignedCustomersNr);
				instance.getParameters().setStartClient(startCustomer);
			}
			// cycle the entire list of customers starting from the randomly chosen one
			for (int j = startCustomer; j < assignedCustomersNr + startCustomer; ++j) {
				// serve to cycle j, j+1, ... assignedcustomersnr, 0, ... j-1
				customerChosen = j % assignedCustomersNr;

				// stores the pointer to the customer chosen from depots list assigned customers
				customerChosenPtr = instance.getDepot(i).getAssignedCustomer(customerChosen);//
				
				// cycle the routes until the last one
				int k;
				for(k= 0; k < instance.getVehiclesNr() - 1; ++k){
					// stores the pointer to the current route
					route = routes[i][k];
					
					
                   //number of customer to add
					int customerIndex=customerChosenPtr.getNumber();
					//number of the last customer in the route
					int prevCustomerIndex = 0;
					prevCustomerIndex = route.getCustomers().size();
					double dist=0.0;//distanza tra il last customer e quello da aggiungere
					double dist2=0.0;//distanza tra il last customer e il deposito
					double dist3=0.0;//distanza tra quelle da aggiungere e il deposito
					double totTime=0.0;
					double twViolation=0.0;
					dist3=instance.distances[customerIndex][instance.getCustomersNr()];
					if(prevCustomerIndex != 0){
						prevCustomerIndex=route.getCustomers().get(prevCustomerIndex-1).getNumber();
						dist=instance.distances[customerIndex][prevCustomerIndex];
						dist2=instance.distances[prevCustomerIndex][instance.getCustomersNr()];
						totTime=route.getReturnToDepotTime();
						totTime=totTime-dist2;
						totTime=totTime+dist;
						twViolation=Math.max(0,totTime-customerChosenPtr.getEndTw());
					}
					if(prevCustomerIndex == 0){
						prevCustomerIndex=customerChosenPtr.getNumber(); //=customerIndex
						dist=instance.distances[prevCustomerIndex][instance.getCustomersNr()];
						dist2=0;
						totTime=dist;
						twViolation=Math.max(0,totTime-customerChosenPtr.getEndTw());
						
					}
					
					// accept on the route only if satisfy the load and duration
					// which satisfies too the endTw
					if (customerChosenPtr.getCapacity() + route.getCost().load <= route.getLoadAdmited()
							
					&& route.getCost().travelTime - dist2 + dist <= customerChosenPtr.getEndTw()
					&& route.getCost().travelTime - dist2 + dist + dist3 <= instance.getDepot(0).getEndTw()
					&& twViolation == 0
					&& vettore[customerChosen]!=1
					 && customerChosenPtr.getServiceDuration() + route.getDuration()  <= route.getDurationAdmited()){
						insertBestTravel(instance, route, customerChosenPtr);						
						evaluateRoute(route);
						vettore[customerChosen]=1;
						break;
					}
				} // end for routes
				// if the customer was not inserted and we reach the last route
				// insert it anyway
				if(k == instance.getVehiclesNr() - 1 && vettore[customerChosen]!=1){
					insertBestTravel(instance, routes[i][k], customerChosenPtr);
					evaluateRoute(routes[i][k]);
					vettore[customerChosen]=1;
				}
			} // end for customer list
		}
	}
	
	private void buildRoutesFromInstance(Instance inst) 
	{
		// TODO Auto-generated method stub
		Route route; // stores the pointer to the current route
		Customer customerChosenPtr; // stores the pointer to the customer chosen from depots list assigned customers
		StringBuffer debug = new StringBuffer();
		int assignedCustomersNr;
		int startCustomer;
		int customerChosen; // serve to cycle j, j+1, ... assignedcustomersnr, 0, ... j-1
		int nRoutes=0;
		int[] vettore;
		vettore=new int[instance.getCustomersNr()];

		// cycle the list of depots
		for( int i = 0; i < instance.getDepotsNr(); ++i) {
			debug.append("\n");
			assignedCustomersNr = instance.getDepot(i).getAssignedCustomersNr();

			// cycle the entire list of customers starting from the randomly chosen one
//			for (int j = startCustomer; j < assignedCustomersNr + startCustomer; ++j) {
	for (Customer c:instance.getListCustomer()) {
			// serve to cycle j, j+1, ... assignedcustomersnr, 0, ... j-1
				customerChosen = c.getNumber();

				// stores the pointer to the customer chosen from depots list assigned customers
				customerChosenPtr = c;//
				
				// cycle the routes until the last one
				int k;
				for(k= 0; k < instance.getVehiclesNr() - 1; ++k){
					// stores the pointer to the current route
					route = routes[i][k];
					
					
                   //number of customer to add
					int customerIndex=customerChosenPtr.getNumber();
					//number of the last customer in the route
					int prevCustomerIndex = 0;
					prevCustomerIndex = route.getCustomers().size();
					double dist=0.0;//distanza tra il last customer e quello da aggiungere
					double dist2=0.0;//distanza tra il last customer e il deposito
					double dist3=0.0;//distanza tra quelle da aggiungere e il deposito
					double totTime=0.0;
					double twViolation=0.0;
					dist3=instance.distances[customerIndex][instance.getCustomersNr()];
					if(prevCustomerIndex != 0){
						prevCustomerIndex=route.getCustomers().get(prevCustomerIndex-1).getNumber();
						dist=instance.distances[customerIndex][prevCustomerIndex];
						dist2=instance.distances[prevCustomerIndex][instance.getCustomersNr()];
						totTime=route.getReturnToDepotTime();
						totTime=totTime-dist2;
						totTime=totTime+dist;
						twViolation=Math.max(0,totTime-customerChosenPtr.getEndTw());
					}
					if(prevCustomerIndex == 0){
						prevCustomerIndex=customerChosenPtr.getNumber(); //=customerIndex
						dist=instance.distances[prevCustomerIndex][instance.getCustomersNr()];
						dist2=0;
						totTime=dist;
						twViolation=Math.max(0,totTime-customerChosenPtr.getEndTw());
						
					}
					
					// accept on the route only if satisfy the load and duration
					// which satisfies too the endTw
					if (customerChosenPtr.getCapacity() + route.getCost().load <= route.getLoadAdmited()
							
					&& route.getCost().travelTime - dist2 + dist <= customerChosenPtr.getEndTw()
					&& route.getCost().travelTime - dist2 + dist + dist3 <= instance.getDepot(0).getEndTw()
					&& twViolation == 0
					&& vettore[customerChosen]!=1
					 && customerChosenPtr.getServiceDuration() + route.getDuration()  <= route.getDurationAdmited()){
						insertBestTravel(instance, route, customerChosenPtr);
						evaluateRoute(route);
						vettore[customerChosen]=1;
						break;
					}
				} // end for routes
				// if the customer was not inserted and we reach the last route
				// insert it anyway
				if(k == instance.getVehiclesNr() - 1 && vettore[customerChosen]!=1){
					insertBestTravel(instance, routes[i][k], customerChosenPtr);
					evaluateRoute(routes[i][k]);
					vettore[customerChosen]=1;
				}
			} // end for customer list
		}
		
		nRoutes=0;
		for (Route r:routes[0] )
		{
			if (r!=null)
				nRoutes++;
		}
		//System.out.println("Routes: " +nRoutes);
//		for (int i=0; i<nRoutes; i++)
//			System.out.println(routes[0][i].getCustomers());
	}
	
	
	void insertBestTravel(Instance instance, Route route, Customer customerChosenPtr) {
		double minCost = Double.MAX_VALUE;
		double tempMinCost = Double.MAX_VALUE;
		int position = 0;
		if(route.isEmpty()){
			// add on first position
			position = 0;
		}else {
			// first position
			if(customerChosenPtr.getEndTw() <= route.getCustomer(0).getEndTw()) {
				tempMinCost = instance.getTravelTime(route.getDepotNr(), customerChosenPtr.getNumber()) 
						+ instance.getTravelTime(customerChosenPtr.getNumber(), route.getFirstCustomerNr()) 
						- instance.getTravelTime(route.getDepotNr(), route.getFirstCustomerNr());
				if(minCost > tempMinCost) {
					minCost = tempMinCost;
					position = 0;
				}
			}
			
			// at the end
			if(route.getCustomer(route.getCustomersLength() - 1).getEndTw() <= customerChosenPtr.getEndTw()){
				tempMinCost = instance.getTravelTime(route.getLastCustomerNr(), customerChosenPtr.getNumber()) 
						+ instance.getTravelTime(customerChosenPtr.getNumber(), route.getDepotNr()) 
						- instance.getTravelTime(route.getLastCustomerNr(), route.getDepotNr());
				if(minCost > tempMinCost) {
					minCost = tempMinCost;
					position = route.getCustomersLength();
				}
			}
			
			// try between each customer
			for(int i = 0; i < route.getCustomersLength() - 1; ++i) {
				if(route.getCustomer(i).getEndTw() <= customerChosenPtr.getEndTw() && customerChosenPtr.getEndTw() <= route.getCustomer(i + 1).getEndTw()) {
					tempMinCost = instance.getTravelTime(route.getCustomerNr(i), customerChosenPtr.getNumber()) 
							+ instance.getTravelTime(customerChosenPtr.getNumber(), route.getCustomerNr(i + 1)) 
							- instance.getTravelTime(route.getCustomerNr(i), route.getCustomerNr(i + 1));
					if(minCost > tempMinCost) {
						minCost = tempMinCost;
						position = i + 1;
					}
				}
			}
//			return position;
		}
		
		route.addCustomer(customerChosenPtr, position);
		
	}

    /**
	 * this function calculates the cost of a route from scratch
	 * @param route
	 */
	public void evaluateRoute(Route route) {
    	double totalTime = 0;
    	double waitingTime = 0;
    	double twViol = 0;
    	Customer customerK;
    	route.initializeTimes();
    	// do the math only if the route is not empty
		if(!route.isEmpty()){
			//System.out.println("PRIMA: " +this.getCost().total);

	    	// sum distances between each node in the route
			for (int k = 0; k < route.getCustomersLength(); ++k){
				// get the actual customer
				customerK = route.getCustomer(k);
				// add travel time to the route
				if(k == 0){
					route.getCost().travelTime += getInstance().getTravelTime(route.getDepotNr(), customerK.getNumber());
					totalTime += getInstance().getTravelTime(route.getDepotNr(), customerK.getNumber());
				}else{
					route.getCost().travelTime += getInstance().getTravelTime(route.getCustomerNr(k -1), customerK.getNumber());
					totalTime += getInstance().getTravelTime(route.getCustomerNr(k -1), customerK.getNumber());
				} // end if else
				
				customerK.setArriveTime(totalTime);
				// add waiting time if any
				//waitingTime = Math.max(0, customerK.getStartTw() - totalTime);
				waitingTime=0;
				
				route.getCost().waitingTime += waitingTime;
				// update customer timings information
				customerK.setWaitingTime(waitingTime);
				
				
				totalTime = Math.max(customerK.getStartTw(), totalTime);

				// add time window violation if any
				twViol = Math.max(0, totalTime - customerK.getEndTw());
				route.getCost().addTWViol(twViol);
				customerK.setTwViol(twViol);
				
				//System.out.println("Time violation "+ twViol);
				
				// add the service time to the total
				totalTime += customerK.getServiceDuration();
				// add service time to the route
				route.getCost().serviceTime += customerK.getServiceDuration();
				// add capacity to the route
				route.getCost().load += customerK.getCapacity();
				
			} // end for customers
			
			// add the distance to return to depot: from last node to depot
			totalTime += getInstance().getTravelTime(route.getLastCustomerNr(), route.getDepotNr());
			route.getCost().travelTime += getInstance().getTravelTime(route.getLastCustomerNr(), route.getDepotNr());
			// add the depot time window violation if any
			twViol = Math.max(0, totalTime - route.getDepot().getEndTw());
			route.getCost().addTWViol(twViol);
			// update route with timings of the depot
			route.setDepotTwViol(twViol);
			route.setReturnToDepotTime(totalTime);
			route.getCost().setLoadViol(Math.max(0, route.getCost().load - route.getLoadAdmited()));
			route.getCost().setDurationViol(Math.max(0, route.getDuration() - route.getDurationAdmited()));
			
			// update total violation
			route.getCost().calculateTotalCostViol();
			//System.out.println("DOPO: " +this.getCost().getDuration());

		} // end if route not empty
		
    } // end method evaluate route
	

	
	public String printMovesAndCosts(){
		StringBuffer print = new StringBuffer();
		print.append("------------------------<<--Solution Moves And Costs-----------------------");
		for (int i = 0; i < moves.size(); ++i){
			MySwapMove move = moves.get(i);
			Cost cost = costs.get(i);
			print.append("\n" + "Move: " + move.getDeleteRouteNr() + ", " + move.getCustomerNr() + ", " + move.getInsertRouteNr() + "\n");
			print.append(String.format("Cost: %7.2f, %7.2f, %7.2f, %7.2f, %7.2f\n", cost.travelTime, cost.loadViol, cost.durationViol, cost.waitingTime,  cost.twViol ));
			print.append("\n-------");
		}
		print.append("--------------------------Solution-->>-----------------------");
		return print.toString();

	}
	
	/**
	 * Print all the routes for each depots, day and vehicle
	 */
	public String printTimeWindows() {
		
		StringBuffer print = new StringBuffer();
		print.append("------------------------<<--Solution-----------------------");
		for (int i = 0; i < routes.length; ++i){
			print.append("\n" + "Depot: " + i + " ");
			for (int j = 0; j < routes[i].length; ++j){
				for(int k = 0; k < routes[i][j].getCustomersLength(); ++k){
					print.append(routes[i][j].getCustomerNr(k) + " " + routes[i][j].getCustomer(k).getEndTw() + " " + routes[i][j].getCustomer(k).getArriveTime() + " " + routes[i][j].getCustomer(k).getWaitingTime() + "\n");
				}
				print.append("D" + i + ": " + routes[i][j].getDepot().getEndTw()+ " " + routes[i][j].getReturnToDepotTime() + " " + routes[i][j].getDepotTwViol() + "\n");
				print.append("--------\n");
			}
			print.append("\n");
		}
		
		print.append("--------------------------Solution-->>-----------------------");
		return print.toString();
	}
	
	/**
	 * Print all the routes for each depots, day and vehicle
	 */
	public String toString() {
		
		StringBuffer print = new StringBuffer();
		print.append("------------------------<<--Solution-----------------------");
		for (int i = 0; i < routes.length; ++i){
			print.append("\n" + "Depot: " + i + "\n");
			for (int j = 0; j < routes[i].length; ++j){
				print.append(routes[i][j].printRoute());
			}
			print.append("\n");
		}
		
		print.append("Total Cost\n" + cost );
		for (int i = 0; i < routes.length; ++i){
			for (int j = 0; j < routes[i].length; ++j){
				print.append(routes[i][j].printRouteCost());
			}
			print.append("\n");
		}
		print.append("--------------------------Solution-->>-----------------------");
		return print.toString();
	}
	
	
	public void printParameters() {
		System.out.println("alpha=" + alpha + " beta=" + beta + " gamma=" + gamma);
	}

	public double getAlpha() {
		return alpha;
	}

	public double getBeta() {
		return beta;
	}

	public double getGamma() {
		return gamma;
	}
	
	public void addTravelTime(double travelTime){
		cost.travelTime += travelTime;
	}

	public void addLoad(double load) {
		cost.load += load;
		
	}

	public void addServiceTime(double serviceTime) {
		cost.serviceTime += serviceTime;
		
	}

	public void addWaitingTime(double waitingTime) {
		cost.waitingTime += waitingTime;
		
	}

	public int getDepotsNr() {
		return routes.length;
	}

	public int getDepotVehiclesNr(int depot) {
		return routes[depot].length;
	}

	public Route[][] getRoutes() {
		return routes;
	}

	public void setCost(Cost cost) {
		this.cost = cost;
		
	}

	public void setRoutes(Route[][] routes) {
		this.routes = routes;
		
	}

	public void setFeasibleIndex(int feasibleIndex) {
		this.feasibleIndex = feasibleIndex;
		
	}

	public int getFeasibleIndex() {
		return feasibleIndex;
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
		MySolution.instance = instance;
	}

	/**
	 * @return the iterationsDone
	 */
	public static int getIterationsDone() {
		return iterationsDone;
	}
	
	/**
	 * @param iterationsDone the iterationsDone to set
	 */
	public static void setIterationsDone(int iterationsDone) {
		MySolution.iterationsDone = iterationsDone;
	}
	
	public void evR (Route r)
	{
		evaluateRoute(r);
	}
	
}