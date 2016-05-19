package com.TabuSearch;

import java.util.*;

import com.mdvrp.Customer;
import com.mdvrp.Instance;
import com.mdvrp.Route;
import com.softtechdesign.ga.Chromosome;
import com.softtechdesign.ga.GA;

public class GaOnRoutes 
{
	MySolution[] v;
	final int SolutionNr=50;   //number of chromosome in the initial popuolation
	int iterazione=0;
	Instance instance;
	
	public GaOnRoutes(MySolution[] v, Instance instance, int iterazione)
	{
		this.v=v;
		this.iterazione=iterazione;
		this.instance=instance;
		//In v ho il vettore di soluzioni.
		/**
		 * Numero di soluzioni: SolutionNr
		 * Numero di route in una soluzione: v[0].getRoutes().length;
		 * Numero di customer in una route: routes[0][i].length
		 **/
	}
	
	public MySolution[] shuffle ()
	{
		Random random = new Random();
		MySolution sol1;
		MySolution sol2;
		Route r1;
		Route r2;
		Customer c1;
		Customer c2;
		
		int randomSol1 = random.nextInt(iterazione+1);
		if (randomSol1==SolutionNr)
			randomSol1--;
		int randomSol2 = random.nextInt(iterazione+1);
		if (randomSol2==SolutionNr)
			randomSol2--;
		
		sol1=v[randomSol1];
		sol2=v[randomSol2];
		//System.out.println(randomSol1 +" " +randomSol2);
		while (sol1.equals(sol2))
		{
			//System.out.println(randomSol1 +" CCCC " +randomSol2);
			randomSol2 = random.nextInt(iterazione+1);
			if (randomSol2==SolutionNr)
				randomSol2--;
			sol2=v[randomSol2];
		}
		//System.out.println("PRIMA: " +sol2.getCost().getDuration());
		
		//Abbiamo selezionato due soluzioni diverse
		//System.out.println("Route di sol2: " +sol2.getRoutes()[0].length);
		int nRoute=sol1.getRoutes()[0].length;
		int randomRoute1 = random.nextInt(nRoute);
		int randomRoute2 = random.nextInt(nRoute);
//		System.out.println("Numero di route 1: " +randomRoute1);
//		System.out.println("Numero di route 2: " +randomRoute2);
		if (randomRoute1==nRoute)
			randomRoute1--;
		if (randomRoute2==nRoute)
			randomRoute2--;
		
		r1 = sol1.getRoute(0, randomRoute1);
		r2 = sol2.getRoute(0, randomRoute2);
		
		//Abbiamo selezionato due route a casaccio di sol1 e sol2
		//System.out.println();
		int indexC=r1.getCustomersLength();
		
		//Selezioniamo un cliente a caso di tale route:
		if (indexC>0){
		int randomC1 = random.nextInt(indexC);
		Customer c = r1.getCustomer(randomC1);
		
		//Questo cliente appartiene alla route r2? Se si è inutile metterlo, cambia cliente!
		int count=0;
		while (r2.getCustomers().contains(c))
		{
			count ++;
			if (count == 3)
				break;
			randomC1 = random.nextInt(indexC);
			c = r1.getCustomer(randomC1);
		}
		//Elimina c dalla route di appartenenza di sol2
		int ii=0;
		int kk=0;
		for (Route rl:sol2.getRoutes()[0] )
		{
			kk=0;
			int flag=0;
			for (Customer cu:rl.getCustomers())
			{
				if (cu.equals(c))
				{
					flag=1;
				}
				if (flag==1)
				{
					break;
				}
				kk++;
			}
			if (flag==1)
				break;
			ii++;
		}
		
		if (c.getCapacity() + r2.getCost().load <= r2.getLoadAdmited() 
				&& c.getServiceDuration() + r2.getDuration()  <= r2.getDurationAdmited()){
		//Devo eliminare l'elemento kk della route numero ii di sol2
		sol2.getRoute(0, ii).removeCustomer(kk);
		sol2.evR(sol2.getRoute(0, ii));
		
		
		//Ristampo le route di sol2 VERIFICATO
		
		sol2.insertBestTravel(instance, r2, c);
		sol2.evR(r2);
	
		v[randomSol2]=sol2;
		}
		//System.out.println("DOPO: " +sol2.getCost().getDuration());
		}
		return v;	
	}

}
