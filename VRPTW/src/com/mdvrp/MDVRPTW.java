package com.mdvrp;

import java.io.FileWriter;
import java.io.PrintStream;
import java.util.*;

import org.coinor.opents.TabuList;

import com.TabuSearch.GaOnRoutes;
import com.TabuSearch.MyGa;
import com.TabuSearch.MyMoveManager;
import com.TabuSearch.MyObjectiveFunction;
import com.TabuSearch.MySearchProgram;
import com.TabuSearch.MySolution;
import com.TabuSearch.MyTabuList;
import com.softtechdesign.ga.ChromStrings;
import com.softtechdesign.ga.Chromosome;
import com.softtechdesign.ga.Crossover;

public class MDVRPTW {
		
	public int populationNr = 10;
	public static long timer_start=0;
	public static long timer_end  =0;
	public static long timer_tmp  =0;
	public static long timer_best =0;
	public static long tempo_prova =0;
	public static int  flag=0;
	public static int count=0;
	public static double previous_cost=0;
	public static int numberSolutionFind=0;
	public static MySearchProgram bestGlobalSolution;
	public static boolean isFirstIteration=true;

	
	public static void convert(MySolution initialSol,String[] possibleGenes,String[] initialSolGa,List<Customer> customerList,Instance instance)
	{	
		//possible genes generation
		for(int i=0;i<instance.getCustomersNr();i++)
		{
			String tmp = new String();
			tmp=String.valueOf(i); ///da modificare
			possibleGenes[i]=tmp;
		} //end
		
		//conversione initial solution
		int j=0;
		for(int i=0; i<instance.getVehiclesNr(); i++)
		{
			Route r=initialSol.getRoute(0, i);
			if (r.getCustomers().size() == 0)  //MAT: Risparmiamo tempo
			{
				//System.out.println("DEVO USCIRE");
				break;
			}
			
			customerList.addAll(r.getCustomers()); //MAT: Sostituisco = con addAll
			//System.out.println("Veicolo: " +i +" " +r.getCustomers());
			
			for(Customer c : r.getCustomers()) //MAT: customerList <- r.getCustomers(); Sennò j non era più tra 0 e customerNr ma tra 0 e customerNr*veiclesNr
			{
				//System.out.println(j);
				String tmp = String.valueOf(c.getNumber());
				initialSolGa[j]=tmp;
				j++;
			}
		}
		
	}
	public static void main(String[] args) 
	{
		final int initialSolutionNr=50;   //number of chromosome in the initial popuolation
		MySearchProgram     search;
		MySolution []         initialSol;
		MyObjectiveFunction[] objFunc;	
		String[] possibleGenes;
		String[][] initialSolGa;
		List<Customer> customerList =new ArrayList<Customer>(); //conterà la lista ricostruita
		List<Customer> listInInstance = new ArrayList<Customer>();
		MyMoveManager[]       moveManager;
		TabuList            tabuList;
		Parameters          parameters 		= new Parameters(); 	// holds all the parameters passed from the input line
		Instance            instance; 								// holds all the problem data extracted from the input file
		Duration            duration 		= new Duration(); 		// used to calculate the elapsed time
		PrintStream         outPrintSream 	= null;					// used to redirect the output
		Route[][] r_try;
		int maxRoutes=0;
		
		try {			
			// check to see if an input file was specified
			timer_start=new Date().getTime();
			//System.out.println("Start: " +timer_start);
			parameters.updateParameters(args);
			if(parameters.getInputFileName() == null){
				System.out.println("You must specify an input file name");
				return;
			}
			
			duration.start();

			// get the instance from the file			
			instance = new Instance(parameters); 
			instance.populateFromHombergFile(parameters.getInputFileName());
								
			// Init memory for Tabu Search
			initialSol= new MySolution[initialSolutionNr]; 
			objFunc =new MyObjectiveFunction[initialSolutionNr];
			moveManager=new MyMoveManager[initialSolutionNr];
			possibleGenes =new String[instance.getCustomersNr()];
			
			//initialization
			for(int i=0;i<initialSolutionNr;i++){
			      initialSol[i] 		= new MySolution(instance);
			      objFunc[i] 		    = new MyObjectiveFunction(instance);
			      moveManager[i] 	    = new MyMoveManager(instance);
			      moveManager[i].setMovesType(parameters.getMovesType());
			}
			
			initialSolGa = new String[initialSolutionNr][instance.getCustomersNr()];
			listInInstance=instance.getListCustomer(); //MAT
	
			convert( initialSol[0], possibleGenes, initialSolGa[0], customerList, instance);
			
			MyGa provaGa = new MyGa (
					instance.getCustomersNr(), //size of chromosome
	                50, //population has N chromosomes
	                1, //crossover probability 0.7
	                10, //random selection chance % (regardless of fitness) 10
	                400, //max generations 2000
	                0, //num prelim runs (to build good breeding stock for final/full run)0
	                25, //max generations per prelim run
	                0, //chromosome mutation prob.0.06
	                0, //number of decimal places in chrom
	                possibleGenes, //gene space (possible gene values)
	                Crossover.ctTwoPoint, //crossover type
	                true,
	                initialSolGa[0],
	                customerList,
	                instance
	                );
			
			Chromosome[] solutionGa = new Chromosome[provaGa.getPopulationDim()]; 
			solutionGa = provaGa.evolve();
			
			/*Rimuovo i duplicati*/
			for (int j=0; j<provaGa.getPopulationDim(); j++)
			{
		        String c1 = new String(solutionGa[j].getGenesAsStr());
				String[] solc1 = new String[instance.getCustomersNr()];;
		    	StringTokenizer stx = new StringTokenizer(c1, "|", false);
		    	int h=0;
	        	while (stx.hasMoreTokens())
	        	{
	        		solc1[h] = stx.nextToken();
	        		h++;
	        	}
	        	String sChrom1 = getChromWithoutDuplicates(solc1, instance.getCustomersNr(), possibleGenes);
	        	((ChromStrings)solutionGa[j]).replaceChrom(sChrom1);
			}
		
//			System.out.println("POPOLAZIONE DATA DAL GENETICO IN INGRESSO A TS");
//			for (int j=0; j<provaGa.getPopulationDim(); j++)
//			{
//				System.out.println(solutionGa[j].getGenesAsStr());
//			}
		    System.out.println("Tabù Search Started");

	for (int jj=0; jj<provaGa.getPopulationDim(); jj++)
			{			
				String bestGa = new String (solutionGa[jj].getGenesAsStr());
			
				//devo suddividere bestGa in vettore di stringhe
				String[] v =new String[instance.getCustomersNr()];
				StringTokenizer st =new StringTokenizer(bestGa,"|",false);
				int r=0;
				while(st.hasMoreTokens()){
				v[r]=st.nextToken();
				r++;
				}
			
				//construct the new customer list
				customerList.clear();
				for(int i=0;i<v.length;i++)
				{
					int tmp=Integer.parseInt(v[i]);
					customerList.add(listInInstance.get(tmp));
				}	
			
			// cambio la lista di customer in instance			
			instance.setListCustomer(customerList);
			
			
			//utilizzo la nuova instanza
 			initialSol[jj] 		= new MySolution(instance, 1); //Un secondo costruttore, che non genera + route random
		    objFunc[jj] 		    = new MyObjectiveFunction(instance);
		    moveManager[jj] 	    = new MyMoveManager(instance);
		    moveManager[jj].setMovesType(parameters.getMovesType());
		    
		    
		    r_try = new Route[instance.getDepotsNr()][instance.getVehiclesNr()];// = initialSol[0].getRoutes();
		    r_try = initialSol[jj].getRoutes();
		  
		   
//		    int t=0;
//		    for (Route yy:r_try[0])
//		    {
//		    	System.out.print("Route " +t +": ");
//		    	List <Customer> k = yy.getCustomers();
//		    	for (Customer ff:k)
//		    	{
//		    		System.out.print(ff.getNumber() +" ");
//		    	}
//		    	t++;		    
//				System.out.println();  }
		    //Ora in teoria dai initialSol[jj] in pasto alla TS, ma prima
		    //Fai un genetico su initialSol[jj] che mescoli le routes.
		 
		    if (jj>0)
		    {
		    	for (int k=0; k<100; k++)
		    	{
		    		GaOnRoutes h = new GaOnRoutes(initialSol, instance, jj);  
		    		initialSol = h.shuffle();
		    	}
//		        t=0;
//			    r_try = initialSol[jj].getRoutes();
//			    for (Route yy:r_try[0])
//			    {
//			    	System.out.print("Route POST " +t +": ");
//			    	List <Customer> k = yy.getCustomers();
//			    	for (Customer ff:k)
//			    	{
//			    		System.out.print(ff.getNumber() +" ");
//			    	}
//			    	System.out.println();
//			    	t++;
//			    }
			   // System.out.println();
		    }	
			   
		     //Tabu list
		      int dimension[] = {instance.getDepotsNr(), instance.getVehiclesNr(), instance.getCustomersNr(), 1, 1};
	        tabuList 		= new MyTabuList(parameters.getTabuTenure(), dimension);
	        
	        // Create Tabu Search object **
	       search 			= new MySearchProgram(instance, initialSol[jj], moveManager[jj], objFunc[jj], tabuList, false,  outPrintSream);
	        // Start solving        
	        search.tabuSearch.setIterationsToGo(parameters.getIterations());
	        
	        search.tabuSearch.startSolving();
	        
	        
	        // wait for the search thread to finish
	        try {
	        	// in order to apply wait on an object synchronization must be done
	        	synchronized(instance){
	        		instance.wait();
	        	}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
	        
	        duration.stop();
	        
	        // Count routes
	        int routesNr = 0;
	        for(int i =0; i < search.feasibleRoutes.length; ++i)
	        	for(int j=0; j < search.feasibleRoutes[i].length; ++j)
	        		if(search.feasibleRoutes[i][j].getCustomersLength() > 0)
	        			routesNr++;

	        //Verifica se il costo è migliore del mio best. No se alla prima iterazione
	        if (flag==1)
	        {
	        	if (bestGlobalSolution.feasibleCost.total >= search.feasibleCost.total)
	        	{
		        	bestGlobalSolution = search;
		        	//mean_cost+=search.feasibleCost.total;
	        		numberSolutionFind++;
	        		tempo_prova=duration.getMinutes()*60+duration.getSeconds();
	        		String outSol = String.format("%s; %f; %d; %4d\r\n" ,
	    	        		instance.getParameters().getInputFileName(), search.feasibleCost.total,
	    	        		duration.getMinutes()*60+duration.getSeconds(), routesNr);
	        		maxRoutes=routesNr;
	    	        System.out.println(outSol);/*
	    	        FileWriter fw = new FileWriter(parameters.getOutputFileName(),true);
	    	        fw.write(outSol);
	    	        fw.close();*/
	        	}
	        	break;
	        }
	        
	        if (isFirstIteration==true)
	        {
	        	bestGlobalSolution = search;
        		timer_best=duration.getMinutes()*60+duration.getSeconds();
	        	isFirstIteration=false;
	        	previous_cost=search.feasibleCost.total;
	        }
	        else
	        {
    			//System.out.println((int)previous_cost + " " + (int)search.feasibleCost.total);
    			//System.out.println(count);

	        	if ((int)bestGlobalSolution.feasibleCost.total >= (int)search.feasibleCost.total)
	        	{
	        		if ((int)bestGlobalSolution.feasibleCost.total != (int)search.feasibleCost.total)
	        		{
		        		tempo_prova=duration.getMinutes()*60+duration.getSeconds();
	        		}
	        		
	        		bestGlobalSolution = search;

	        		if ((int)previous_cost==(int)search.feasibleCost.total)
	        		{
	        			count++;
	        			if (count==2)
	        				flag=1;
	        		}
	        		else
	        		{
	        			count = 0;
	        			previous_cost=search.feasibleCost.total;
	        		}
	        		
	        	}
	        }
	        
        	if (flag!=1)
        	{
        		//mean_cost+=search.feasibleCost.total;
        		numberSolutionFind++;
        		
	        // Print results
	        //TEMPO PROVA: tempo in cui effettivamente trova l'ottimo
        	String outSol = String.format("%s; %5.2f; %d; %4d\r\n" ,
	        		instance.getParameters().getInputFileName(), search.feasibleCost.total,
	        		duration.getMinutes()*60+duration.getSeconds(), routesNr);
	        maxRoutes=routesNr;
	        System.out.println(outSol);
	        
	        /*
	        FileWriter fw = new FileWriter(parameters.getOutputFileName(),true);
	        fw.write(outSol);
	        fw.close();*/
	        
        	}
	       // si je veux commenter      

		}

	}	
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try{
    	timer_best=duration.getMinutes()*60+duration.getSeconds();
	    MySolution s = (MySolution) bestGlobalSolution.tabuSearch.getBestSolution();
	    //System.out.println("Best Solution Cost: " + s.getCost().total +" in " +timer_best +" secondi");
	    String outSol = String.format("%s; %5.2f; %d; %4d\r\n" ,
	    			parameters.getInputFileName(), s.getCost().total,
	        		timer_best, maxRoutes);
	    
	   
	    
	    FileWriter fw = new FileWriter(parameters.getOutputFileName(),true);
        fw.write(outSol);
        fw.close();
        System.out.println(outSol + " " +tempo_prova);
        //Aggiunta
        FileWriter fw2 = new FileWriter(parameters.getCurrDir() + "/output/"+parameters.getInputFileName()+parameters.getRandomSeed()+".csv");
        fw2.write(outSol);
        fw2.close();
        System.out.println("Scritto su file media:"+outSol);
		}
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getChromWithoutDuplicates(String[] sChromosome, int number, String[] possibleGenes) 
	{
		int j=0, h=-1;
		String [] unused   = new String[number];
		String [] solution = new String[number];

		//Metto in unused tutte le stringhe che non sono in init
		for (int i=0; i<number; i++)
		{
			String tmp = possibleGenes[i];
			h = Arrays.asList(sChromosome).indexOf(tmp);
			if (h==-1)
			{
				unused[j]=tmp;
				j++;
			}
		}
		j=0;
		StringBuffer sb = new StringBuffer(number);

		//Scandisco init, prendo una stringa: è in solution? SI: metti available. NO: mettila
		for (int i=0; i<number; i++)
		{	
			String tmp = sChromosome[i];
			h = Arrays.asList(solution).indexOf(tmp);
			if (h==-1)
			{
				solution[i] = tmp;
			}
			else if (h>= 0 && h<number)
			{
				solution[i] = unused[j];
				j++;
			}
			else System.out.println("Errore");
			
			if (i==0)
				sb.append(solution[i]);
			else
				sb.append("|"+solution[i]);
		}

		String def = new String (sb);
		return def;
	}
}
