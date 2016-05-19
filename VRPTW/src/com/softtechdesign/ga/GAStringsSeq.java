package com.softtechdesign.ga;

import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * <pre>
 *  GAStringsSeq models chromosomes as arrays of strings.
 *
 *  Chromosome strings store chromosomeDim genes. Unlike GAString (where each gene is a
 *  single char), with GAStringsSeq each gene itself is a string. For example, one might look
 *  like:
 *  "dw|l80|nop|dn2" --4 genes, each separated by a pipe char (nop = special no operation gene)
 *
 *  This type of GA could be used for:
 *    determining a sequence for navigating a maze
 *    discovering a useful image processing algorithm (seq) for highlighting an image feature
 *    modeling any system where genes are naturally modeled as Strings
 * </pre>
 * @author Jeff Smith jeff@SoftTechDesign.com
*/
public abstract class GAStringsSeq extends GA
{
    /** the building material or pool "allowed" or "possible" gene values */
    protected String[] possGeneValues;
    
    /** the number of possible gene values */
    protected int numPossGeneValues;

    /**
     * Initialize the GAStringSeq
     * @param chromosomeDim
     * @param populationDim
     * @param crossoverProb
     * @param randomSelectionChance
     * @param maxGenerations
     * @param numPrelimRuns
     * @param maxPrelimGenerations
     * @param mutationProb
     * @param chromDecPts
     * @param possGeneValues
     * @param crossoverType
     * @param computeStatistics
     * @throws GAException
     */
    public GAStringsSeq(int chromosomeDim,
                        int populationDim,
                        double crossoverProb,
                        int randomSelectionChance,
                        int maxGenerations,
                        int numPrelimRuns,
                        int maxPrelimGenerations,
                        double mutationProb,
                        int chromDecPts,
                        String[] possGeneValues,
                        int crossoverType,
                        boolean computeStatistics) throws GAException
    {
        super(chromosomeDim, populationDim, crossoverProb, randomSelectionChance, maxGenerations,
          numPrelimRuns, maxPrelimGenerations, mutationProb, crossoverType, computeStatistics);

        if (possGeneValues.length < 2)
            throw new GAException("There must be at least 2 possible gene values");

        this.possGeneValues = possGeneValues;
        this.numPossGeneValues = possGeneValues.length;

          //create the chromosomes for this population
        for (int i=0; i < populationDim; i++)
        {
            this.chromosomes[i] = new ChromStrings(chromosomeDim);
            this.chromNextGen[i] = new ChromStrings(chromosomeDim);
            this.prelimChrom[i] = new ChromStrings(chromosomeDim);
        }

        initPopulation();
    }


    /**
     * Returns the chromosome given by index casted as a ChromStrings object
     * @param index
     * @return
     */
    protected ChromStrings getChromosome(int index)
    {
        return((ChromStrings)this.chromosomes[index]);
    }


    /**
     * Randomly pick and return a gene value
     * @return String
     */
    protected String getRandomGeneFromPossGenes()
    {
        int iRandomIndex = getRandom(numPossGeneValues);
        return(possGeneValues[iRandomIndex]);
    }

    /**
     * Randomly reassign a gene in the chromosome identified by the given index (iChromIndex)
     * to one of the possible gene values
     * @param iChromIndex
     */
    protected void doRandomMutation(int iChromIndex)
    {
        int geneIndex;
        char cTemp;

        geneIndex = getRandom(chromosomeDim);
        String gene = getRandomGeneFromPossGenes();

        setGeneValue(iChromIndex, geneIndex, gene);
    }

    /**
     * Sets the value of a gene for the given chromosome at the given geneIndex
     * @param iChromIndex
     * @param geneIndex
     * @param gene
     */
    private void setGeneValue(int iChromIndex, int geneIndex, String gene)
    {
        ChromStrings chromStrings = ((ChromStrings)this.chromosomes[iChromIndex]);
        chromStrings.setGene(gene, geneIndex);
    }


    /**
     * Create random chromosomes from the given gene space.
     */
    protected void initPopulation()
    {
        for (int i=0; i < populationDim; i++)
        {
          for (int j=0; j < chromosomeDim; j++)
                ((ChromStrings)this.chromosomes[i]).setGene(getRandomGeneFromPossGenes(), j);
          this.chromosomes[i].fitness = getFitness(i);
        }
    }


    /**
     * Genetically recombine the given chromosomes using a one point crossover technique.
     * <pre>
     * For example, if we have:
     *   chromosome A = { "x1", "x2", "x3", "x4" }
     *   chromosome B = { "y1", "y2", "y3", "y4" }
     * and we randomly choose the crossover point of 1, the new genes will be:
     *   new chromosome A = { "y1", "x2", "x3", "x4" }
     *   new chromosome B = { "x1", "y2", "y3", "y4" }
     * </pre>
     * @param Chrom1
     * @param Chrom2
     */
    protected void doOnePtCrossover(Chromosome Chrom1, Chromosome Chrom2)
    {
        int iCrossoverPoint = getRandom(chromosomeDim-2);
        String gene1 = ((ChromStrings)Chrom1).getGene(iCrossoverPoint);
        String gene2 = ((ChromStrings)Chrom2).getGene(iCrossoverPoint);

                // CREATE OFFSPRING ONE
        ((ChromStrings)Chrom1).setGene(gene2, iCrossoverPoint);

                // CREATE OFFSPRING TWO
        ((ChromStrings)Chrom2).setGene(gene1, iCrossoverPoint);
    }


    /**
     * Genetically recombine the given chromosomes using a two point crossover technique which
     * combines two chromosomes at two random genes (loci), creating two new chromosomes.
     * <pre>
     * For example, if we have:
     *   chromosome A = { "x1", "x2", "x3", "x4", "x5" }
     *   chromosome B = { "y1", "y2", "y3", "y4", "y5" }
     * and we randomly choose the crossover points of 1 and 3, the new genes will be:
     *   new chromosome A = { "y1", "x2", "y3", "x4", "x5" }
     *   new chromosome B = { "x1", "y2", "x3", "y4", "y5" }
     * </pre>
     * @param Chrom1
     * @param Chrom2
     */
    protected void doTwoPtCrossover(Chromosome Chrom1, Chromosome Chrom2)
    {
        String sNewChrom1, sNewChrom2;
        int iCrossoverPoint1, iCrossoverPoint2;
        String sChrom1, sChrom2;

        iCrossoverPoint1 = 1 + getRandom(chromosomeDim-2);
        iCrossoverPoint2 = iCrossoverPoint1 + 1 + getRandom(chromosomeDim-iCrossoverPoint1-1);

        if (iCrossoverPoint2 == (iCrossoverPoint1+1))
            doOnePtCrossover(Chrom1, Chrom2);
        else
        {
            String gene1_Chrom1 = ((ChromStrings)Chrom1).getGene(iCrossoverPoint1);
            String gene1_Chrom2 = ((ChromStrings)Chrom2).getGene(iCrossoverPoint1);
            String gene2_Chrom1 = ((ChromStrings)Chrom1).getGene(iCrossoverPoint2);
            String gene2_Chrom2 = ((ChromStrings)Chrom2).getGene(iCrossoverPoint2);

                // CREATE OFFSPRING ONE
            ((ChromStrings)Chrom1).setGene(gene1_Chrom2, iCrossoverPoint1);
            ((ChromStrings)Chrom1).setGene(gene2_Chrom2, iCrossoverPoint2);

                // CREATE OFFSPRING TWO
            ((ChromStrings)Chrom2).setGene(gene1_Chrom1, iCrossoverPoint1);
            ((ChromStrings)Chrom2).setGene(gene2_Chrom1, iCrossoverPoint2);
        }
    }

    /**
     * Genetically recombine the given chromosomes using a uniform crossover technique. 
     * This technique randomly swaps genes from one chromosome to another.
     * <pre>
     * For example, if we have:
     *   chromosome A = { "x1", "x2", "x3", "x4", "x5" }
     *   chromosome B = { "y1", "y2", "y3", "y4", "y5" }
     * our uniform (random) crossover might result in something like:
     *   chromosome A = { "y1", "x2", "x3", "x4", "x5" }
     *   chromosome B = { "x1", "y2", "y3", "y4", "y5" }
     * if only the first gene in the chromosome was swapped.
     * </pre>
     * @param Chrom1
     * @param Chrom2
     */
    protected void doUniformCrossover(Chromosome Chrom1, Chromosome Chrom2)
    {
        int iGeneToSwap;
        char cGene;

        for (int i=0; i < chromosomeDim; i++)
        {
            if (getRandom(100) > 50)
            {
                iGeneToSwap = getRandom(chromosomeDim);
                String gene1 = ((ChromStrings)Chrom1).getGene(iGeneToSwap);
                String gene2 = ((ChromStrings)Chrom2).getGene(iGeneToSwap);

                ((ChromStrings)Chrom1).setGene(gene2, iGeneToSwap);
                ((ChromStrings)Chrom2).setGene(gene1, iGeneToSwap);
            }
        }
       /* String c1 = new String(Chrom1.getGenesAsStr());
        String[] solc1 = new String[chromosomeDim];
    	StringTokenizer stx = new StringTokenizer(c1, "|", false);
       	String c2 = new String(Chrom2.getGenesAsStr());
      	String[] solc2 = new String[chromosomeDim];
     	StringTokenizer sty = new StringTokenizer(c2, "|", false);
      	int h=0;
        	while (stx.hasMoreTokens() && sty.hasMoreTokens())
        	{
        		solc1[h] = stx.nextToken();
        		solc2[h] = sty.nextToken();
        		h++;
        	}
        	//Ora ho i vettori di stringhe associati a quel cromosoma, solc1 e solc2.
        	//Rimuovo i duplicati

        String sChrom1 = getChromWithoutDuplicates(solc1, chromosomeDim);
        String sChrom2 = getChromWithoutDuplicates(solc2, chromosomeDim);
        ((ChromStrings)Chrom1).replaceChrom(sChrom1);
        ((ChromStrings)Chrom2).replaceChrom(sChrom2);*/
    }
    
 /*   
    public String getChromWithoutDuplicates (String[] sChromosome, int number)
	{	
		int j=0, h=-1;
		String [] unused   = new String[number];
		String [] solution = new String[number];

		//Metto in unused tutte le stringhe che non sono in init
		for (int i=0; i<number; i++)
		{
			String tmp = possGeneValues[i];
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
	}*/
}