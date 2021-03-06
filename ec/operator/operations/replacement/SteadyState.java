/*******************************************************************************
 * Copyright (c) 2014, 2015 
 * Anthony Awuley - Brock University Computer Science Department
 * All rights reserved. This program and the accompanying materials
 * are made available under the Academic Free License version 3.0
 * which accompanies this distribution, and is available at
 * https://aawuley@bitbucket.org/aawuley/evolvex.git
 *
 * Contributors:
 *     ECJ                     MersenneTwister & MersenneTwisterFast (https://cs.gmu.edu/~eclab/projects/ecj)
 *     voidException      Tabu Search (http://voidException.weebly.com)
 *     Lucia Blondel       Simulated Anealing 
 *     
 *
 *        
 *******************************************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.operator.operations.replacement;

import ec.fitnessevaluation.FitnessExtension;
import ec.individuals.Chromosome;
import ec.individuals.Gene;
import ec.individuals.Individual;
import ec.individuals.fitnesspackage.PopulationFitness;
import ec.individuals.populations.Population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import ec.algorithms.alps.ALPSReplacement;
import ec.algorithms.alps.system.ALPSLayers;
import ec.algorithms.alps.system.Engine;
import ec.algorithms.ga.Evolve;
import ec.operator.CrossoverModule;
import ec.operator.MutationModule;
import ec.operator.operations.Operations;
import ec.operator.operations.ReplacementStrategy;
import ec.operator.operations.SelectionOperation;
import ec.parameter.Instance;
import ec.util.statistics.BasicStatistics;
import ec.util.statistics.StatisticsCollector;

/**
 * SteadState replacement strategy.
 * 
 * @author  Anthony Awuley
 */
public class SteadyState extends Operations  implements ReplacementStrategy {
	
    private final String replacmentType = "Steady state";
    private int populationCount;
    private double randomNumber;
    private ArrayList<Double> ages = new ArrayList<>();  
    public int evaluationCounter = 0;
  
 	@Override
	public String toString() 
 	{
 		return this.replacmentType;
 	}
	
	 /**
	  * TODO not completed
	  * GENERATE next generation
	  * 
	  * @param f - basic fitness
	  * @param crx - crossover module
	  * @param mtx - mutation module
	  * @param so - selection operator
	  * @param current - population <previous+current>
	  * @param generation - current generation count
	  * @param run - current run count
      * @param crossoverRate - crossover rate
	  * @param mutationRate - mutation rate
	  * @param tournamentSize - tournament size
	  * @return
      */
    @Override
	@SuppressWarnings({ "unchecked", "unused"})
	public Population nextGeneration(Evolve e,
			                         PopulationFitness f,
			                         CrossoverModule crx,
                                     MutationModule mtx,
                                     SelectionOperation so,
                                     StatisticsCollector stats,
    		                         final ArrayList<Population> current,
    		                         int generation,
                                     int run
                                     ) 
    { 
    	
    	Generational g = new Generational();
    	ArrayList<Integer> bestFitnessIndividualsOfGeneration;
    	ArrayList<Double> generationFitness;
    	
    	//set old pop to current generation
    	Population oldPop = current.get(generation -1);
    	//set new pop to evolved current generation
    	Population newPop = g.nextGeneration(
    			e,
    			f,crx,mtx,so,stats,current, 
    			generation, run);
          
    	Population newBreed = new Population();
    	/* merge the two generations
    	 * newPop.addAll(newPop, oldPop.getAll());
    	 */
    	newPop.addAll(oldPop.getAll());
    	
        ((FitnessExtension) f).calculateSimpleFitness(newPop,run,generation,(BasicStatistics) stats,e.properties);
        
        /* Individual bestIndividualOfPreviousGeneration = newPop.get(((FitnessExperimental) f).getBestIndividualsOfGeneration().get(0));
         * calculate fitness of new population
         */
        generationFitness = ((FitnessExtension) f).calcGenerationalFitness(newPop,e.properties);
        //sort fitness in ascending order
        ArrayList<Double> sortedFitness = (ArrayList<Double>) generationFitness.clone();
        Collections.sort(sortedFitness);
        
        //select top(half of combined pop) individuals from combined population
        bestFitnessIndividualsOfGeneration = f.minimumFitness(generationFitness,sortedFitness,oldPop.size());
        
        for(int i=0;i<oldPop.size();i++)
        {
        	newBreed.add(newPop.get(i)); //add top individuals to new pop
        }
        
    	return newBreed;
    
    }

	@SuppressWarnings("unchecked")
	@Override
	public Population nextGenerationALPS(
			Evolve e,
			PopulationFitness f,
            CrossoverModule crx,
            MutationModule mtx,
            SelectionOperation so,
            StatisticsCollector stats,
            ALPSReplacement alpsReplacment,
            final ArrayList<Population> current,
            ALPSLayers alpsLayers,
            int generation,
            int run
            ) 
	{
        Population  evolvingPopulation = new Population();
        Chromosome c1                  = new Chromosome();
        Chromosome c2                  = new Chromosome();
        ArrayList<Integer> tournamentIndividuals = new ArrayList<>();
        AbstractSSReplacement ssr = (new Instance()).ssReplacement(e.properties);
        
        /*
         * used for non-population fitness evaluation - this is done so as not to mix up 
         * data from entire population evaluatioin
         */
     	 PopulationFitness pf = (new Instance()).fitnessEvaluator(e.properties);
        
     	/* unset parent flag on all individuals in current layer */
 		alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation().unsetParent();
        this.populationCount = 0; //initialized from one because of addition of best individual
        
        /*
         * Console print
         */
        System.out.println("Layer #"+ alpsLayers.layers.get(alpsLayers.index).getId()+
                           " Layer Generation #"+ alpsLayers.layers.get(alpsLayers.index).layerEvaluationCount+
                           " Global Generation # "+Engine.completeGenerationalCount);
        
        /**
         * Getting population from layers
         */
        if(alpsLayers.layers.get(alpsLayers.index).getIsBottomLayer()) //bottom layer - new individuals will be generated
    	{ 
        	 evolvingPopulation = alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation();
    	}
    	else //get population of current layer
    	{  
    		/*
    		 * TODO Memory usage with clone()
    		 */
   		    //evolvingPopulation =  (Population) alpsLayers.layers.get(alpsLayers.index).         //current layer
   			//		 getEvolution().getCurrentPopulation().clone();
   		    evolvingPopulation.addAll(alpsLayers.layers.get(alpsLayers.index).         //current layer
   					 getEvolution().getCurrentPopulation().clone().getAll());
   		 
   		    evolvingPopulation.addAll(alpsLayers.layers.get(alpsLayers.index-1). //previous layer
				     getEvolution().getCurrentPopulation().clone().getAll());
    	} 
      
        //calculate fitness
        ((FitnessExtension) f).calculateSimpleFitness(
        		evolvingPopulation,
        		alpsLayers.layers.get(alpsLayers.index),
        		//generation,
        		//Engine.completeGenerationalCount,
        		Engine.completeEvaluationCount,
        		run,
        		(BasicStatistics) stats,
        		e.properties,true);
        
        /* 
		 * ELITISM 
		 * add best individual of previous generation
		 * nextGeneration.add(bestIndividualOfPreviousGeneration);
		 */
		Population best = performElitism(
				e, alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation(),f);
		evolvingPopulation = ssr.elitism(e,so, alpsLayers,evolvingPopulation,best);
		this.populationCount += best.size();
       
        /* 
         * old implementation assuming one best individual
         * Individual bestIndividualOfPreviousGeneration = current.get(generation-1).get(((FitnessExtension) f).getBestIndividualsOfGeneration().get(0));
         */
        while (this.populationCount < alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize() /* current.get(generation-1).size()*/ ) // best for nextGeneration.size()
        {  
            //this.randomNumber = randGen.nextDouble();
            this.randomNumber = e.random.nextDouble();
            
            // crossover?
            /*
             * Individuals that are created through variation, such as by 
             * mutation or recombination, take the age value of their oldest 
             * parent plus 1 since their genetic material comes from their 
             * parents and has now been in the population for one more generation 
             * than their parents. Each generation in which an individual is used 
             * as a parent to create an offspring its age is increases by 1 since 
             * its genetic material has been used in evolution in another generation
             */
            if ((this.randomNumber < e.crossoverRate) ) 
            { 
            	tournamentIndividuals.clear(); //clear content of tournament individuals
            	//conditional fitness evaluator for dynamic population
                dynamicPopulationFitnessEvaluator(evolvingPopulation,alpsLayers,f,generation,stats,e.properties,run);
                /*
                 * Perform two tournament selections and pick "best" from each using selection pressure
                 * add best individual to index 0 and 1 of tournamentIndividuals
                 *
            	 * SelectionOperation; - first tournament: select best individual
            	 */
                so.performTournamentSelection(e,alpsLayers);

                tournamentIndividuals.add(((FitnessExtension) f).
                		selectIndividualsBasedOnFitness(e,
                				so.getTournamentSelection(),e.selectionPressure).get(0)); 
                /*
            	 * SelectionOperation; - second tournament: select best individual
            	 * commented preferred; verify why evolvingPopulation.size() != f.getGenerationFitness().size() 
            	 */
            	//selectionOperation.performTournamentSelection(alpsLayers,f.getGenerationFitness().size(), tournamentSize);
            	so.performTournamentSelection(e,alpsLayers);
                tournamentIndividuals.add(((FitnessExtension) f).
                		selectIndividualsBasedOnFitness(e,
                				so.getTournamentSelection(),
                				e.selectionPressure).get(0)); 
                 
                c1.setGenes((ArrayList<Gene>) evolvingPopulation.
                		get(tournamentIndividuals.get(0)).getChromosome().getGenes().clone()); //clone individuals
                c2.setGenes((ArrayList<Gene>) evolvingPopulation.
                		get(tournamentIndividuals.get(1)).getChromosome().getGenes().clone());  
                
                /*
                 * ALPS: AGE OF parents and offsprings
                 * Offspring: age of oldest parent + 1
                 * parent   : increment by 1
                 */
                ages.clear();
                for(int i=0; i<2;i++)
            	     ages.add(evolvingPopulation.get(tournamentIndividuals.get(i)).getBirthEvaluations());
                
               
                /* 
                 * add new individuals to population
                 * pass two individuals for crossover
                 * automatically determines how many individuals to add based on current population size left
                 * nextGeneration.addAll(nextGeneration,crx.performCrossoverOperation(current.get(generation -1), c1, c2,tournamentIndividuals,(current.get(generation-1).size() - this.populationCount)));
                 */
                crx.performCrossoverOperation(
                		e,
                		evolvingPopulation, 
                		c1.clone(), 
                		c2.clone(),tournamentIndividuals,
                		(alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize() - this.populationCount),
                		ages); 
                
                this.evaluationCounter += crx.getOffsprings().size();
                this.populationCount   += crx.getOffsprings().size(); //increment population by number of children added
               
                //calculate fitness of new individuals
                ((FitnessExtension) pf).calculateSimpleFitness(
                		crx.getOffsprings(),
                		alpsLayers.layers.get(alpsLayers.index),
                		generation,
                		run,
                		(BasicStatistics) stats,
                		e.properties,false);
                
                //replace within individual layer
                evolvingPopulation = ssr.ssReplacements(e,so, alpsLayers,evolvingPopulation,crx.getOffsprings());
               
                
                /*
                 * calculate age of newly added individuals
                 * crx.getOffsprings().calculateAge((int) alpsLayers.layers.get(alpsLayers.index).getEvaluationCounter(),
                 *		alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize());
                 * 
                 * TODO 
                 * crx.getOffsprings().calculateAge(alpsLayers,(int) Engine.completeEvaluationCount);
                 * 
                 */
                crx.getOffsprings().calculateAge(Engine.completeEvaluationCount,
                		alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize());
                
                alpsLayers.evalCounter += crx.getOffsprings().size();
            }
            // mutation?
            if ((this.randomNumber < e.mutationRate) && (this.populationCount < alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize())) 
            {   //apply mutation policy to the chromosomes
            	tournamentIndividuals.clear(); //clear content of tournament individuals
            	//conditional fitness evaluator for dynamic population
                dynamicPopulationFitnessEvaluator(evolvingPopulation,alpsLayers,f,generation,stats,e.properties,run);
            	/* SelectionOperation;
            	 * commented preferred; verify why evolvingPopulation.size() != f.getGenerationFitness().size() 
            	 */
                so.performTournamentSelection(e,alpsLayers);
                /*
                 * selectionOperation.performTournamentSelection(alpsLayers,f.getGenerationFitness().size(),tournamentSize);
                 * select two best tournament individuals
                 */
                tournamentIndividuals.add(((FitnessExtension) f).
                		selectIndividualsBasedOnFitness(e,
                				so.getTournamentSelection(),
                				e.selectionPressure).get(0)); //select best individuals from tournament selection
                c1.setGenes((ArrayList<Gene>) evolvingPopulation.
                		get(tournamentIndividuals.get(0)).getChromosome().getGenes().clone()); //clone individuals
                /* 
                 * evaluation of parent
                 */
                ages.add(evolvingPopulation.get(tournamentIndividuals.get(0)).getBirthEvaluations());
               
               /* 
                * add new individual to population
                * nextGeneration.addAll(nextGeneration,mtx.performMutationOperation(current.get(generation -1), c1,tournamentIndividuals.get(0)));
                */
                mtx.performMutationOperation(
                		e,evolvingPopulation,c1.clone(),tournamentIndividuals.get(0),ages);
                
                this.evaluationCounter += mtx.getOffsprings().size();
                this.populationCount++;   //increment population
                
                //calculate fitness of new individuals
                ((FitnessExtension) pf).calculateSimpleFitness(
                		mtx.getOffsprings(),
                		alpsLayers.layers.get(alpsLayers.index),
                		generation,
                		run,
                		(BasicStatistics) stats,
                		e.properties,false);
                
                //use replacement strategy to replace new individual within  layer
                evolvingPopulation = ssr.ssReplacements(e,so,alpsLayers,evolvingPopulation,mtx.getOffsprings());
                
                //calculate age of newly added individuals
                mtx.getOffsprings().calculateAge(Engine.completeEvaluationCount,
                		       alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize());
                
                //keep count of total evaluations so far
                alpsLayers.evalCounter += mtx.getOffsprings().size();
            }
            
        } 
     
        /* select only first half of parsed population */
        alpsReplacment.consolidatePopulation(alpsLayers,evolvingPopulation);
		alpsLayers.layers.get(alpsLayers.index).getEvolution().setCurrentPopulation(evolvingPopulation);
   
		alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation().
		calculateAge(Engine.completeEvaluationCount,
                     alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize());
		
		
       return alpsReplacment.performAgeLayerMovements( e,alpsLayers,
    		                                           alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation(),
    		                                           so);
		
       //return alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation();
		
	}

	
	/**
	 * its necessary to perform multiple population fitness evluation for dynamic population in alps
	 * population size keep increasing when evaluating a layer with less individuals than the required population size
	 * its incremented and replacement begins when the entire population has been filled
	 * 
	 * @param evolvingPopulation
	 * @param alpsLayers
	 * @param f
	 * @param generation
	 * @param stats
	 * @param p
	 */
	private void dynamicPopulationFitnessEvaluator(Population evolvingPopulation, ALPSLayers alpsLayers, 
			PopulationFitness f, int generation, StatisticsCollector stats,Properties p,int run)
	{
		if(alpsLayers.layers.get(alpsLayers.index).getEvolution().getCurrentPopulation().size() < 
				alpsLayers.layers.get(alpsLayers.index).getParameters().getPopulationSize())
        { //System.out.println("I came here @:"+alpsLayers.layers.get(alpsLayers.index).getId());
        ((FitnessExtension) f).calculateSimpleFitness(
        		evolvingPopulation,
        		alpsLayers.layers.get(alpsLayers.index),
        		generation,
        		run,
        		(BasicStatistics) stats,
        		p,false);
        }
		
	}
	
	
    
}
