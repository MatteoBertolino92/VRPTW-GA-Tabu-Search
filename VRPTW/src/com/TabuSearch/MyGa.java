package com.TabuSearch;

import java.util.*;

import com.softtechdesign.ga.ChromStrings;
import com.softtechdesign.ga.GAException;
import com.softtechdesign.ga.GAStringsSeq;
import com.mdvrp.*;

public class MyGa extends GAStringsSeq 
{
	//String[] possibleGenes;
	String[] solution;
	List<Customer> customerList;
	Instance instance;
	int flag=0;
	
	public MyGa(int chromosomeDim, int populationDim, double crossoverProb,
			int randomSelectionChance, int maxGenerations, int numPrelimRuns,
			int maxPrelimGenerations, double mutationProb, int chromDecPts,
			String[] possibleGenes, int crossoverType,
			boolean computeStatistics,String[] initialSolGa,
			List<Customer> customerList,Instance instance) throws GAException 
	{
		
		super(chromosomeDim, populationDim, crossoverProb, randomSelectionChance,
				maxGenerations, numPrelimRuns, maxPrelimGenerations, mutationProb,
				chromDecPts, possibleGenes, crossoverType, computeStatistics);
		
		this.instance=instance;
		//this.possibleGenes=possibleGenes;
		solution= new String[instance.getCustomersNr()];
		solution= initialSolGa;
		this.customerList=customerList;
		
		this.possGeneValues=possibleGenes;
		
	}
	
	
	@Override
	protected double getFitness(int iChromIndex) 
	{
		// TODO Auto-generated method stub
		double fitness=0;
		int i=0, geneIndex1=0, geneIndex2=0;
		double rDist=0,cust1,cust2;
        ChromStrings chromosome = (ChromStrings)this.getChromosome(iChromIndex); //soluzione di stringhe matteo
        
        
        
        String genes[] = this.getChromosome(iChromIndex).getGenes();
        int lenChromosome = genes.length;
   
        if (solution!=null)
        {
        	String ga = new String(chromosome.getGenesAsStr());
        	String[] solGa = new String[instance.getCustomersNr()];
        	StringTokenizer stx = new StringTokenizer(ga, "|", false);
        	int h=0;
        	while (stx.hasMoreTokens())
        	{
        		solGa[h] = stx.nextToken();
        		h++;
        	}
        	
        //MAT: La Super del costruttore accede a questa. La prima volta non esiste ancora ed è critica
        geneIndex1 = Integer.parseInt(solGa[0]); //1 customer
        geneIndex2 = instance.getCustomersNr(); //2 deposito
        rDist = instance.distances[geneIndex1][geneIndex2]; //!!!!!!!!!!!!!!!!!!

        for (i = 0; i < instance.getCustomersNr()-1; i++)
        {
        	geneIndex1 = Integer.parseInt(solGa[i]);       
            geneIndex2 = Integer.parseInt(solGa[i+1]);
            //System.out.println("DEBUG " +i +" " +geneIndex1 +" " +geneIndex2);
            rDist += instance.distances[geneIndex1][geneIndex2];
        }
        
        geneIndex1 = instance.getCustomersNr(); //deposito
        rDist += instance.distances[geneIndex1][geneIndex2];
	}

        if (Math.abs(rDist) > 1e-12) //1/rDist = fitness
            return (1 / rDist);
        else
            return (1 / 1e-12);
	}
	
	
	@Override
	public String toString() 
	{
		return "MyGa [possGeneValues=" + Arrays.toString(possGeneValues)
				+ ", solution=" + Arrays.toString(solution) + "]";
	}	
	
}
