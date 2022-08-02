package rankingEvaluation;

import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.math.LongMath;
//This class calculates the evaluation metrics for each state
//Which means its calculates for EM for all proposals for that state
public class RankingEvaluation_MetricsCalculationForEachState {
	
	//may 2020
	Integer sumOfAllProposalsForCurrState=0;
	
	
	
	//may 2020 ..don't know we i did the calculations in categories as dcg and ndcg values need to be calculated and calculated at each rank
	//so i try do this in arrays now
	
	//dcg array values
	//ndcg array values
	

	//number of sentences matched at each rank...for each state, for each proposal there can be a number of reason sentences
	HashMap<Integer,Integer> smr = new HashMap<Integer,Integer>();    
    //System.out.println("Initial list of elements: "+hm);  
    
	public Integer getNumberOfSentencesMatchedAtThatRankForState(Integer r) {		
		Integer t = 0;		
		if(smr.get(r)==null) t=0;   //if no sentenecs matched at that rank, we return 0
		else t= smr.get(r); //else return the number matched
		return t;	
	}
	
	public void incrementNumberOfSentences_MatchedAtEachRank_ForState(Integer r) {	//r = rank	
		Integer t = 0;
		if(smr.get(r)==null) t=0; //if no sentenecs matched at that rank, we return 0
		else t = smr.get(r); //else return the number matched		
		smr.put(r, t+1);  
	}
	
	//may 2020...we store and diplay the evaluation metrics at each rank aslso
	ArrayList<RankingEvaluation_MetricsAtEachRank> ematkListForEachProposal = new ArrayList<RankingEvaluation_MetricsAtEachRank>();
	//for each proposal, every value at each rank at the above list is totalled and stored in this list below 
	ArrayList<RankingEvaluation_MetricsAtEachRank> ematkListForEachProposalInState = new ArrayList<RankingEvaluation_MetricsAtEachRank>();
	//we will create an empty list of 200 element and initialise values with 0
	
	//EvaluationMetricsAtEachRank ematk = new
	
	
	/*  
		hm.put(100,"Amit");    
            
      System.out.println("After invoking put() method ");  
      for(Map.Entry m:hm.entrySet()){    
       System.out.println(m.getKey()+" "+m.getValue());    
      }  
        
      hm.putIfAbsent(103, "Gaurav");  
      System.out.println("After invoking putIfAbsent() method ");  
      for(Map.Entry m:hm.entrySet()){    
           System.out.println(m.getKey()+" "+m.getValue());    
          }  
      HashMap<Integer,String> map=new HashMap<Integer,String>();  
      map.put(104,"Ravi");  
      map.putAll(hm);  
      System.out.println("After invoking putAll() method ");  
      for(Map.Entry m:map.entrySet()){    
           System.out.println(m.getKey()+" "+m.getValue());    
          }  
      
      System.out.println("hm " + hm.get(1001));  
      System.out.println("hm " + hm.get(105)); 
	*/
	
	
	
	//Evaluation metrics for all proposals for that state
	float sumPreAtTop1=0,sumPreAtTop2=0, sumPreAtTop3=0,sumPreAtTop4=0,
		  sumPreAtTop5=0,sumPreAtTop10=0,sumPreAtTop15=0,sumPreAtTop30=0,sumPreAtTop50=0,sumPreAtTop100=0,sumPreOutsideTop100=0; //precision at k
	//the previous values keep track for addition to the next proposal..this sis a constant value which will get added at different times in teh new proposal as te ranking can stop at 3
	//float previousSumPreAtTop5=0,previousSumPreAtTop10=0,previousSumPreAtTop15=0,previousSumPreAtTop30=0,previousSumPreAtTop50=0,previousSumPreAtTop100=0,previousSumPreOutsideTop100=0; //precision at k
	
	float sumRecAtTop1=0,sumRecAtTop2=0, sumRecAtTop3=0, sumRecAtTop4=0, 
		  sumRecAtTop5=0,sumRecAtTop10=0,sumRecAtTop15=0,sumRecAtTop30=0,sumRecAtTop50=0,sumRecAtTop100=0,sumRecOutsideTop100=0;	//recall at k
	//float previousSumRecAtTop5=0,previousSumRecAtTop10=0,previousSumRecAtTop15=0,previousSumRecAtTop30=0,previousSumRecAtTop50=0,previousSumRecAtTop100=0,previousSumRecOutsideTop100=0;	//recall at k
	
	float sumDcgAtTop1=0, sumDcgAtTop2, sumDcgAtTop3, sumDcgAtTop4,
		  sumDcgAtTop5=0,sumDcgAtTop10=0,sumDcgAtTop15=0,sumDcgAtTop30=0,sumDcgAtTop50,sumDcgAtTop100,sumDcgOutsideTop100=0;
	
	float dcgAtTop1=0,dcgAtTop2=0,dcgAtTop3=0,dcgAtTop4=0,
		  dcgAtTop5=0,dcgAtTop10=0,dcgAtTop15=0,dcgAtTop30=0,dcgAtTop50=0,dcgAtTop100=0,dcgOutsideTop100=0; //discounted cumulative gain
	//float previousSumDcgAtTop5=0,previousSumDcgAtTop10=0,previousSumDcgAtTop15=0,previousSumDcgAtTop30=0,previousSumDcgAtTop50=0,previousSumDcgAtTop100=0,previousSumDcgOutsideTop100=0;	
	
	float sumNdcgAtTop1=0,sumNdcgAtTop2=0,sumNdcgAtTop3=0,sumNdcgAtTop4=0,
		  sumNdcgAtTop5=0,sumNdcgAtTop10=0,sumNdcgAtTop15=0,sumNdcgAtTop30=0,sumNdcgAtTop50,sumNdcgAtTop100,sumNdcgOutsideTop100=0;
		
	float ndcgAtTop1=0,ndcgAtTop2=0,ndcgAtTop3=0,ndcgAtTop4=0,
	      ndcgAtTop5=0,   ndcgAtTop10=0,ndcgAtTop15=0,ndcgAtTop30=0,ndcgAtTop50=0,ndcgAtTop100=0,ndcgOutsideTop100=0; //normalised discounted cumulative gain
	//float previousSumNdcgAtTop5=0,previousSumNdcgAtTop10=0,previousSumNdcgAtTop15=0,previousSumNdcgAtTop30=0,previousSumNdcgAtTop50=0,previousSumNdcgAtTop100=0,previousSumNdcgOutsideTop100=0;	
	
	//april 2019..new way of calcuating pre and recall at k
	int countMatchedAtTop1=0,countMatchedAtTop2=0,countMatchedAtTop3=0,countMatchedAtTop4=0,
		countMatchedAtTop5=0,countMatchedAtTop10=0,countMatchedAtTop15=0,countMatchedAtTop30=0,countMatchedAtTop50=0,countMatchedAtTop100=0,countMatchedOutsideTop100=0;
	//int countRecAtTop5=0,countRecAtTop10=0,countRecAtTop15=0,countRecAtTop30=0,countRecAtTop50=0,countRecAtTop100=0,countRecOutsideTop100=0;
	
	float sum_dcg_ForAllProposals=0, sum_ndcg_ForAllProposals=0;
	
	//evaluation metrics for each proposal
	float preAtTop1=0,preAtTop2=0,preAtTop3=0,preAtTop4=0,preAtTop5=0,preAtTop10=0,preAtTop15=0,preAtTop30=0,preAtTop50=0,preAtTop100=0,preOutsideTop100=0; //precision at k
	float recAtTop1=0,recAtTop2=0,recAtTop3=0,recAtTop4=0,recAtTop5=0,recAtTop10=0,recAtTop15=0,recAtTop30=0,recAtTop50=0,recAtTop100=0,recOutsideTop100=0;	//recall at k
	
	//EM for a current particular proposal
	int matchedSent=0, rank=0,rankByTP=0,counterRecallChanged=0;			//double pre, rec;
	//for each proposal, find the 
	float totalOfPrecisions=0, maxRecall=0,finalAveragePrecisionForCurrProposal=0, dcg=0, idcg =0, ndcg= 0, ideal_dcg=0;
	
	//Variables for Recall and Precision at k
	float preAtK=0,  recAtK=0; //currentRecall=0,maxRecall=0,finalAveragePrecision=0, dcg=0, idcg =0, ndcg= 0, ideal_dcg=0;
	
	//Variables for Average Mean Precision
	float precision_ForAVGPreMean=0, previousRecall=0, currentPrecision_ForAVGPreMean=0, currentRecall_ForAVGPreMean=0; //applies to each proposal
	
//	System.out.println("\n\n##############\n  Ranking Evaluation \n\n");
	boolean outputOnce=false, matchedAtIndex=false;
	float relevanceValue= 0, irelevanceValue=0;
	float deno=0, deno2=0;
	Integer pNum;
	
	//Evaluation metrics for all proposals in that state
	static int top1=0,top2=0,top3=0,top4=0,top5=0,top10=0,top15=0,top30=0,top50=0,top100=0,outsideTop100=0, manualExtractedReasonSentenceCounterForAProposal=0;
	float sumOfAllPrecisionsForAllProposalsForCurrState=0;	//this will be incremented for all proposals	
	
	public void initEvaluationValuesForCurrStateToZero() {
		top1=top2=top3=top4=top5=top10=top15=top30=top50=top100=outsideTop100=0;
		sumPreAtTop1=sumPreAtTop2=sumPreAtTop3=sumPreAtTop4=sumPreAtTop5=sumPreAtTop10=sumPreAtTop15=sumPreAtTop30=sumPreAtTop50=sumPreAtTop100=sumPreOutsideTop100=0; //precision at k
		sumRecAtTop1=sumRecAtTop2=sumRecAtTop3=sumRecAtTop4=sumRecAtTop5=sumRecAtTop10=sumRecAtTop15=sumRecAtTop30=sumRecAtTop50=sumRecAtTop100=sumRecOutsideTop100=0;
		sumOfAllPrecisionsForAllProposalsForCurrState=0;
		sum_dcg_ForAllProposals=sum_ndcg_ForAllProposals=0;
		sumDcgAtTop1=sumDcgAtTop2=sumDcgAtTop3=sumDcgAtTop4=sumDcgAtTop5=sumDcgAtTop10=sumDcgAtTop15=sumDcgAtTop30=sumDcgAtTop50=sumDcgAtTop100=sumDcgOutsideTop100=0;
		sumNdcgAtTop1=sumNdcgAtTop2=sumNdcgAtTop3=sumNdcgAtTop4=sumNdcgAtTop5=sumNdcgAtTop10=sumNdcgAtTop15=sumNdcgAtTop30=sumNdcgAtTop50=sumNdcgAtTop100=sumNdcgOutsideTop100=0;
		
		//may 2020
		// we will add to values in this list for each proposal
		//we start at position 1, as ranking starts at that position
		System.out.println("\t\t\t Populating ematkListForEachProposalInState ");
		ematkListForEachProposalInState.clear();
		for (int e=1;e<2000;e++) { 
			RankingEvaluation_MetricsAtEachRank em0 = new RankingEvaluation_MetricsAtEachRank();
			em0.setRank(e); 			  em0.setMatched(0);  //initialise to 0 --matched = 0
			em0.setCountMatchedAtRank(0); em0.setDcg(0);	
			em0.setIdealDcg(0);			  em0.setNdcg(0);
			em0.setPrecisionatk(0);       em0.setRecallatk(0);			
			ematkListForEachProposalInState.add(em0);//add to list
		}	
		System.out.println("\t\t\t Populated ematkListForEachProposalInState ");
	}
	
	public void initEvaluationValuesForCurrProposalToZero() {
		preAtTop1=preAtTop2=preAtTop3=preAtTop4=preAtTop5=preAtTop10=preAtTop15=preAtTop30=preAtTop50=preAtTop100=preOutsideTop100=0; //precision at k
		recAtTop1=recAtTop2=recAtTop4=recAtTop5=recAtTop10=recAtTop15=recAtTop30=recAtTop50=recAtTop100=recOutsideTop100=0;	
		previousRecall=totalOfPrecisions=precision_ForAVGPreMean=currentRecall_ForAVGPreMean=maxRecall=finalAveragePrecisionForCurrProposal=dcg=idcg=ndcg=ideal_dcg=0;
		matchedSent=rank=counterRecallChanged=0;
		outputOnce=matchedAtIndex=false;
		relevanceValue=irelevanceValue=0;
		deno=deno2=0;
		pNum=null;
		preAtK=0;  recAtK=0;
		countMatchedAtTop1=countMatchedAtTop2=countMatchedAtTop3=countMatchedAtTop4=countMatchedAtTop5=countMatchedAtTop10=countMatchedAtTop15=countMatchedAtTop30=
				countMatchedAtTop50=countMatchedAtTop100=countMatchedOutsideTop100=0;
		dcgAtTop1=dcgAtTop2=dcgAtTop3=dcgAtTop4=dcgAtTop5=dcgAtTop10=dcgAtTop15=dcgAtTop30=dcgAtTop50=dcgAtTop100=dcgOutsideTop100=0; 
		ndcgAtTop1=ndcgAtTop2=ndcgAtTop3=ndcgAtTop4=ndcgAtTop5=ndcgAtTop10=ndcgAtTop15=ndcgAtTop30=ndcgAtTop50=ndcgAtTop100=ndcgOutsideTop100=0;
	}
	
	//NOTE FOR all queries for this proposal for that state, the ranking has already been done and assigned for multiple qu
    public void calculateEvaluationMetricsForEachProposal(ArrayList<ScoresToSentences> v_CurrProposalWithCurrStateList, Integer manualExtractedReasonSentenceCounterForAProposal, 
    		List<Integer> listTotalDistinctProposalsCommittedReasonSentenceManuallyEntered,
			Integer numManualEntertedSentencesForProposalState, Integer prNum, String state, Connection conn, String evalLevel, boolean outputForDebug) {
    	pNum=prNum;	//Some proposals wont have any results, so p num wont be assigned from within the loop ...so we assign it here ...For each proposal in that state
    	//System.out.println("@@@@@@ mmm pNum: " + pNum);
		Map<Integer, Integer> numberMatchedAtEachRankHashMap = new HashMap<Integer, Integer>(); //to keep track for num matched at each rank, since multiple sentences can be matched in each message
		
		boolean outputPreAtKAlready= false, reasonSentenceMatchedInProposal=false; //, preAtTop5Calculated=false,preAtTop10Calculated=false,preAtTop15Calculated=false,preAtTop30Calculated=false,preAtTop50Calculated=false,preAtTop100Calculated= false, preOutsideTop100Calculated=false; 
		
		// for each candidate results for each proposal
		// we have already gone over this list once setting the ranks at which the sentences have been matched
		// we now use the matched indexes in the lit to calculate evaluation metrics form each proposal
		// and in doing so we will use the values to update the values for the state as well
		boolean reachedEnd=false; 
		for (int t=0; t < v_CurrProposalWithCurrStateList.size()-1; t++) {
			
			//may 2020...we dont stop when the total reason sentences for a proposal 
			// but we go for the first 200
			//if(reachedEnd)	//stop loop as soon as we reached the last matched sentences in the results for the pep
			//	break;
			if(t>200)
				break;
				
			Integer totalMatchedForProposal=0, totalMatchedAtThatRank=0;
			
			rank = t+1; //v_CurrProposalWithCurrStateList.get(t).getRank(); //this is rank of the message in sentence level - meaning the first instance of message in sentence
			matchedAtIndex=false;
			pNum = v_CurrProposalWithCurrStateList.get(t).getProposalNum();	
			//System.out.println("@@@@@@ mmm pNum: " + pNum);
			int m = v_CurrProposalWithCurrStateList.get(t).getMatched();
			
			//may 2020 ..pep 345..2 sentences matched in the same message for mbs
			if(m==2) {
				System.out.println("\t More than one sentence matched in message");
			}
			//System.out.println("\trank here 123, t= "+t + " rank " + rank);
			
			
//			if(pNum==308 && m==1)
//				 System.out.println("@@@@@@ mmm pNum: " + pNum + " index: "+ t + " matched: "+ m + " rank: "+rank);
//			if(pNum==308) 
//			System.out.println("Proposal: "+ pNum + ", Index "+t+", rank using TP : "+ v_CurrProposalWithCurrStateList.get(t).getRankByTotalProbability() + " new Rank: " + rank + " matched: "
//						  	+ v_CurrProposalWithCurrStateList.get(t).getMatched() + " manualExtractedReasonSentenceCounterForAProposal: " +manualExtractedReasonSentenceCounterForAProposal);
			
			//may 2020..we update the list
			RankingEvaluation_MetricsAtEachRank em = new RankingEvaluation_MetricsAtEachRank();
			em.setRank(rank);
			
			Integer numMatchedAtrank = v_CurrProposalWithCurrStateList.get(t).getMatched();
			
			
			if(numMatchedAtrank ==1 || numMatchedAtrank== 2 ) {	 //pep 345 has two sentences matched at same message at the same position in MBS
				matchedSent = numMatchedAtrank;	matchedAtIndex=true;    reasonSentenceMatchedInProposal=true;  
				em.setMatched(matchedSent); em.setCountMatchedAtRank(em.getCountMatchedAtRank()+numMatchedAtrank); //first get those already matched at that rank
				totalMatchedAtThatRank=numMatchedAtrank;
				//may 2020
				//System.out.println("\t\t\t rank " + rank   + " numManualEntertedSentencesForProposalState: " + numManualEntertedSentencesForProposalState);
				//ematkListForEachProposalInState.get(rank-1).setMatched(      ematkListForEachProposalInState.get(rank-1).getMatched() + numMatchedAtrank );
				ematkListForEachProposalInState.get(t).setMatched(      ematkListForEachProposalInState.get(t).getMatched() + numMatchedAtrank );
			} else {
				em.setMatched(0);
				totalMatchedAtThatRank=0;	
				//ematkListForEachProposalInState.get(rank-1).setMatched(0);
			}
			//precision and recall at k..this value calculated here is for showing values at this row. Is not used for calculation - that is done at the end 
			//we divide by rank, instead of t because rank is indeed the ordering	
			
			//we have to consider cumulative values, so we have to add all the relevant ones up till that point, not only that point
			//get all matched until that rank ..for preatk
			Integer w = 0;
			for (int j=0; j < t; j++) 
				w = w + v_CurrProposalWithCurrStateList.get(j).getMatched();						
			Integer allmatchedUntilthatrank = w + numMatchedAtrank;
			
			//listTotalDistinctProposalsCommittedReasonSentenceManuallyEntered.size()
			preAtK = precisionAtK(allmatchedUntilthatrank/*matchedSent*/,1, rank);  //number of recommended items
				//System.out.println("\t\t\tpreAtK("+preAtK+")=matchedSent("+matchedSent+")/rank("+rank+")");
			recAtK = recallAtK(allmatchedUntilthatrank/*matchedSent*/,1, numManualEntertedSentencesForProposalState); 
				//System.out.println("\t\t\trecAtK("+preAtK+")=matchedSent("+matchedSent+")/numManualEntertedSentencesForProposalState("+numManualEntertedSentencesForProposalState+")");
			
			//list for each proposal
			if (Double.isNaN(preAtK))	preAtK = 0;
			if (Double.isNaN(recAtK))	recAtK = 0;
			//System.out.println(" Rank " + rank + " preAtK: " + preAtK + " rec@k: " +recAtK);
			em.setPrecisionatk(preAtK);
			em.setRecallatk(recAtK);
			//set /increment values in list for all proposals in state
			//System.out.println("\t\t\t rank " + rank);
			//at each position we will add the precsions and recall and then in the end divide by the number matched at that rank
//			if(rank != -1) {
//				ematkListForEachProposalInState.get(rank-1).setPrecisionatk( preAtK + ematkListForEachProposalInState.get(rank-1).getPrecisionatk()  );
//				ematkListForEachProposalInState.get(rank-1).setRecallatk(    recAtK + ematkListForEachProposalInState.get(rank-1).getRecallatk()     );
//			}
			// increment values with the ones already there
			if(t != -1) { 
				ematkListForEachProposalInState.get(t).setPrecisionatk( preAtK + ematkListForEachProposalInState.get(t).getPrecisionatk()  );
				ematkListForEachProposalInState.get(t).setRecallatk(    recAtK + ematkListForEachProposalInState.get(t).getRecallatk()     );
			}
			
			//add to list
			ematkListForEachProposal.add(em); 
			
			if(v_CurrProposalWithCurrStateList.get(t).getMatched()==1) {	
				Integer numMatched = numberMatchedAtEachRankHashMap.get(rank);//get how many are matched at that rank
				if(outputForDebug)
					System.out.println("\t\t\t index "+ t +" IF rank "+ rank + " manualExtractedReasonSentenceCounterForAProposal: "+manualExtractedReasonSentenceCounterForAProposal + " numMatched "+numMatched);
		        if(numMatched==null || numMatched==0) {
		        	numMatched=0;
		            numberMatchedAtEachRankHashMap.put(rank, numMatched+1); //increment and assign 'howmanymatched' at that rank back
		        }
		        if(outputForDebug)
		        	System.out.println("\t\t\t index "+ t +" IF numberMatchedAtEachRankHashMap.size " + numberMatchedAtEachRankHashMap.size());
		    }
			//we have to store values for idcg
			else {   // rank > 0 to revent -1's
				  if (rank> 0 && rank <= manualExtractedReasonSentenceCounterForAProposal) {
					  Integer numMatched = numberMatchedAtEachRankHashMap.get(rank);//get how many are matched at that rank
					  if(outputForDebug)
						  System.out.println("\t\t\t index "+ t +" ELSE rank "+ rank + " manualExtractedReasonSentenceCounterForAProposal: "+manualExtractedReasonSentenceCounterForAProposal + " numMatched "+numMatched);
					  if(numMatched==null)  //if no value stored there then we insert 0
						  numberMatchedAtEachRankHashMap.put(rank, 0);
					  if(outputForDebug)
						  System.out.println("\t\t\t index "+ t +" ELSE numberMatchedAtEachRankHashMap.size " + numberMatchedAtEachRankHashMap.size());
					  //else some value may be stored there from some rank value
				  }
			}
			
			//these are after the last matched row so we dont need this
			//very important point here. as soon as after last matched, we dont compute or store any more metrics
			//may 2020...we dont stop but compute for all first 200 ranks
			//if(rank== -1) {	
			//	continue;
			//} 
			
			if(rank==5 || rank==10 || rank==15 || rank==30 || rank==50 || rank==100) {
				outputPreAtKAlready=true;
			} 
		    
		    //final value
		    if(!outputOnce && pNum==308) {
//		        System.out.println("\t Final: " + t + " ndcg: "+ndcg + " sum_ndcg: "+ sum_ndcg);
		        outputOnce = true;
			}	
		    
		    //may 2020
		    Map<Integer,Integer> sortedNumberMatchedAtEachRankTreeMap = new TreeMap<Integer,Integer>(numberMatchedAtEachRankHashMap);
		    
		   
		    currentPrecision_ForAVGPreMean=currentRecall_ForAVGPreMean=totalOfPrecisions=0; //to zero
		    
			//we only consider matched rows for pre@k, rec@k and MAP. If we have a matched value, i.e more than zero, then we do calculation
			//note that number matched can be zero as well as we do inclde ummatched ranks below 'nummatched forthatproposal' for idcg calcultion
			if(totalMatchedAtThatRank > 0) { 
				totalMatchedForProposal += 1;
				
				//PRECISION AND RECALL AT K 
				//Here counts for each proposal are incremented with [number of sentences matched at that rank]. At the end of all rows for this proposal, the pre@k and rec@k are calculated
				//the lower vlaues also increment as soon as we increment upper values
		        //dont worry about idcg values we have stored as zero matched, as they will be stored as zero 
				if(rank == 1) {			countMatchedAtTop1  +=totalMatchedAtThatRank; 	countMatchedAtTop2  +=totalMatchedAtThatRank;   countMatchedAtTop3  +=totalMatchedAtThatRank;	countMatchedAtTop4  +=totalMatchedAtThatRank;}
				else if(rank == 2) {	countMatchedAtTop2  +=totalMatchedAtThatRank; 	countMatchedAtTop3  +=totalMatchedAtThatRank;	countMatchedAtTop4  +=totalMatchedAtThatRank;   }
				else if(rank == 3) { 	countMatchedAtTop3  +=totalMatchedAtThatRank;	countMatchedAtTop4  +=totalMatchedAtThatRank;	}
				else if(rank == 4) { 	countMatchedAtTop4  +=totalMatchedAtThatRank;	}
				
				//we count for all top 5, not just at rank == 5
				if(rank <= 5)    	{			countMatchedAtTop5  +=totalMatchedAtThatRank;	countMatchedAtTop10 +=totalMatchedAtThatRank;  countMatchedAtTop15 +=totalMatchedAtThatRank;	countMatchedAtTop30 +=totalMatchedAtThatRank; countMatchedAtTop50 +=totalMatchedAtThatRank; countMatchedAtTop100 +=totalMatchedAtThatRank; countMatchedOutsideTop100 +=totalMatchedAtThatRank;	 }	   
			    else if(rank> 5  && rank<=10 ){ countMatchedAtTop10 +=totalMatchedAtThatRank;  							  countMatchedAtTop15 +=totalMatchedAtThatRank;	countMatchedAtTop30 +=totalMatchedAtThatRank; countMatchedAtTop50 +=totalMatchedAtThatRank; countMatchedAtTop100 +=totalMatchedAtThatRank; countMatchedOutsideTop100 +=totalMatchedAtThatRank;	 }
			    else if(rank> 10 && rank<=15) { countMatchedAtTop15 +=totalMatchedAtThatRank;														countMatchedAtTop30 +=totalMatchedAtThatRank; countMatchedAtTop50 +=totalMatchedAtThatRank; countMatchedAtTop100 +=totalMatchedAtThatRank; countMatchedOutsideTop100 +=totalMatchedAtThatRank;   }
			    else if(rank> 15 && rank<=30) {	countMatchedAtTop30 +=totalMatchedAtThatRank;  																				 countMatchedAtTop50 +=totalMatchedAtThatRank; countMatchedAtTop100 +=totalMatchedAtThatRank; countMatchedOutsideTop100 +=totalMatchedAtThatRank;   }
			    else if(rank> 30 && rank<=50) { countMatchedAtTop50 +=totalMatchedAtThatRank;   																										  countMatchedAtTop100 +=totalMatchedAtThatRank; countMatchedOutsideTop100 +=totalMatchedAtThatRank;   }
			    else if(rank> 50 && rank<=100){	countMatchedAtTop100 +=totalMatchedAtThatRank;																															        countMatchedOutsideTop100 +=totalMatchedAtThatRank;   }
			    else if(rank> 100) 			  { countMatchedOutsideTop100 +=totalMatchedAtThatRank; }
				//in the end counts for each level below would be inclusive of the value just above 
				
				//Since we are only checking matched sentences (totalMatchedAtThatRank > 0), we do this for all matched ranks
		        if(!reachedEnd){
					//Below recall and precision values to calculate for Average Mean Precision 
					currentRecall_ForAVGPreMean = recallAtK(totalMatchedForProposal,1, numManualEntertedSentencesForProposalState); 
	//						System.out.println("\tIndex: " +t+" currentRecall_ForAVGPreMean("+currentRecall_ForAVGPreMean+")=matchedSent("+matchedSent+")/numManualEntertedSentencesForProposalState(" 
	//									+  numManualEntertedSentencesForProposalState + ")");
					//if recall is larger that previous recall
					//at the first index current recall == rec == 0  //,not current recall > rec, so we have to cater that  ...(currentRecall==0 && rec==0) 
					if(currentRecall_ForAVGPreMean > previousRecall ||  (rank==0) ){ //divided by number of reason sentences for that proposal for that state
						//precision and recall are divided by the message rank in sentence level
	//							System.out.println("#### AVGPreMean #### index: "+t + " Rank: "+ rank);
						precision_ForAVGPreMean = precisionAtK(totalMatchedForProposal,1,rank); // s+1   //divided by the rank not position ... to prevent divide by zero error, 1 passed as only 1 proposal/query is being tested here
	//							System.out.println("\t\tprecision_ForAVGPreMean("+precision_ForAVGPreMean+")=matchedSent(" + matchedSent + ")/rank("+rank+"), rank by TP: " + v_CurrProposalWithCurrStateList.get(t).getRankByTotalProbability());
	
						//this if for calculating average precision mean
						totalOfPrecisions = totalOfPrecisions + precision_ForAVGPreMean;					
						counterRecallChanged++;
						
						previousRecall = currentRecall_ForAVGPreMean;  //set recall as previous recall
						maxRecall = currentRecall_ForAVGPreMean; //make currentRecall as maxRecall everytime
	//							System.out.println("\t\ttotalOfPrecisions: " +totalOfPrecisions + " counterRecallChanged: " + counterRecallChanged + " currentRecall_ForAVGPreMean: "+ currentRecall_ForAVGPreMean + " maxRecall:" + maxRecall); 
	//							System.out.println("#### END AVGPreMean ####"); 
					}				
		        }
				//Average Mean Precision..finally once we have 
				//we aloso have to ready to do this for rank  is equal to one, as it can have only one manually entered reason sentence which can be matched at rank 1
		        if(totalMatchedForProposal == numManualEntertedSentencesForProposalState) { //if we have reached recall == 1
		        	finalAveragePrecisionForCurrProposal = totalOfPrecisions/counterRecallChanged; 
		        	reachedEnd=true; //break; //we break out of the loop
	//					System.out.println("####START FinalAveragePrecision ####");
		        		if(outputForDebug)
		        			System.out.println("\tFinalAveragePrecision ForEachProposal: ("+finalAveragePrecisionForCurrProposal+")=totalOfPrecisions("+totalOfPrecisions+")/("+counterRecallChanged+")");
	//					System.out.println("####END tFinalAveragePrecision ####");
				}
			} //end if total matched > 0 ...meaning ending list of results where matched
			
	        //DISCOUNTED CUMULATIVE GAIN
			//ideal dcg ... we set the ideal dcg values
		    //for the # of manual sentences found for this proposal for this state
			//int deno = 1;  //since there is only one relevant value, this will always be 1
			//equal to added in cases for rank ==1 and 'manualExtractedReasonSentenceCounterForAProposal' == 1
	        
	        //Calculation of Ideal Discounted Cumulative Gain
	        //if rank at this point is less than 'manualExtractedReasonSentenceCounterForAProposal', ideal relevance values will always be 1
	        if (rank <= manualExtractedReasonSentenceCounterForAProposal /* &&  (rank > 0) */  ) {	//For each manually extracted/stored reason sentence got current proposal //t or rank?
		    	//we can either have matched rows or just a zero (unmatched rows)
	        	if (totalMatchedAtThatRank==0)
		    		irelevanceValue=1;   //ideal relevance values are opposite, 
		    	else if(totalMatchedAtThatRank>0)
		    		irelevanceValue = 1;	//ideal relevance and dcg values
		    	if (rank==1) {   idcg+=irelevanceValue;	/* System.out.println("\t\t\t\t\t zzzz idcg = idcg ("+idcg+")" );   since log 1 = 0 */	}
		    	//else if(rank== -1) {   idcg=0;  }  //after the last matched row, we havent assigned rank , so lets give it a 0
		    	else {					
					deno2 = logBase2(rank);
					idcg = idcg + (irelevanceValue / deno2) ;  //value of 1 is used because we only checking for matched ones and this code is inside all matched ranks only
//$					System.out.println("\t\t B Rank : "+ rank + " idcg = idcg ("+idcg+") + (irelevanceValue ("+irelevanceValue+") / deno2 ("+deno2+")" );
				}
//		    	if(pNum==308) {        System.out.println("\t t: " + t + " ndcg: "+ndcg + " sum_ndcg: "+ sum_ndcg);		}
		    }
	        //else they would be zero, thats why we just add zero here as [idcg + 0/deno] is same as just idcg
		    else {   //the rest of the values will not be one thus be the value from the last summed value above
		    	idcg = idcg + 0 ;  //idcg remains the same
//		    	System.out.println("\t Proposal T : "+ pNum + " idcg " + idcg );
		    	//if(idcg==0)
		    		//System.out.println("\t ERROR idcg=0 : "+ idcg);
		    }
	        //may 2020 ..debug
	        if(idcg == 0)
	        	System.out.println("\t Proposal T : "+ pNum + " idcg " + idcg );
	        else if (idcg ==1) {
	        //	System.out.println("\t Proposal T : "+ pNum + " idcg " + idcg );
	        }
	        
		    
			//Calculation of Normalised Discounted Cumulative Gain
	        //at rank 1 is tricky
			if (rank == 1) {   //meaning we have matched row at rank 1
				if (totalMatchedAtThatRank==0)
		    		dcg = 0;   //ideal relevance values are opposite, 
		    	else if(totalMatchedAtThatRank>0)
		    		dcg = 1;	//ideal relevance and dcg values
				 ndcg = dcg/idcg; 
				//dcg=ndcg=1; // since the log function starts from rank 2 
				//just repeating this from the code block below. For pre and rec, we have same case as all other rank for rank 1. But for dcg its different.
				//System.out.println("\t\t\t\t\t ndcg("+ndcg+") = dcg("+dcg+")/idcg("+idcg+")  deno "+ deno + " relevanceValue: "+ relevanceValue);
			} 
			else if(rank== -1) {   System.out.println("\trank 877 " + rank);  }  //pre= rec=0;
			else {  //for all other index of that proposal, where sentence is matched, the dcg is accumulated usimng rank
	//			if(v_CurrProposalWithCurrStateList.get(t).getMatched()==1) {	
					//discounted cumulative gain...Note: for each proposal with specific state, we will have multiple queries, but it does not make a difference
					//since its is cumulative, we store the final value, we follow the book example on page 320
					//get two double numbers  //rank = s+1;//
					//relevanceValue = 1;
					if (totalMatchedAtThatRank==0)	  		relevanceValue = 0;
			    	else if(totalMatchedAtThatRank>0)  		relevanceValue = 1;
					//deno = (float) Math.log(rank);  System.out.println("\trank " + rank+ " deno "+ deno + " relevanceValue: "+ relevanceValue + " dcg: " + dcg);
					deno = logBase2(rank);  
					//System.out.println("\trank " + rank+ " deno "+ deno + " relevanceValue: "+ relevanceValue + " dcg: " + dcg);
					dcg =   dcg + (float) (relevanceValue/deno); 				
					//ndcg - normalised dcg..at every index would be achieved by dividing dcg/ideal dcg
				    ndcg = dcg/idcg; 
//$$			    System.out.println("\tndcg("+ndcg+") = dcg("+dcg+")/idcg("+idcg+"),  deno: "+ deno + ", relevanceValue: "+ relevanceValue);
	//			} 
	//			else { 
	//				relevanceValue = 0;  //pre=rec=0; 	
					//deno = (float) Math.log(rank);  
					//dcg =   0; //dcg + (float) (relevanceValue/deno) ;  //value of 1 is used because we only checking for matched ones and this code is inside all matched ranks only
	//			}
			}
			//assign dcg and ndcg at top5 for current proposal and all proposals
			//Note that the rank entries are sorted and since here we will need the accumulated values, as the rank can just end at rank 3 and wont go until 5th rank. 
			//Therefore we keep writing to that same dcgAtK and ndcgAtK values
			//since we need accumulated values, we keep adding. for pep 308, acccepted, its woudl be matched at rank 2 and 4
			if(rank == 1) {                        dcgAtTop1  = dcg;		     ndcgAtTop1	 = ndcg;    } 
			else if(rank == 2) {                   dcgAtTop2  = dcg;		     ndcgAtTop2	 = ndcg;    } 
			else if(rank == 3) {                   dcgAtTop3  = dcg;		     ndcgAtTop3	 = ndcg;    }
			else if(rank == 4) {                   dcgAtTop4  = dcg;		     ndcgAtTop4	 = ndcg;    }
			
			//here rank at 5 includes all above
			if(rank<=5 ) { 	                        dcgAtTop5  = dcg;		     ndcgAtTop5	 = ndcg;    }   	/*	System.out.println("\t\t\t\t ------------ Proposal: " + pNum +" Rank "+ rank + " new preAtTop5: "  + preAtTop5 + ", preAtK " + preAtK);   */ 
		    else if(rank> 5  && rank<=10  )  {      dcgAtTop10 = dcg;            ndcgAtTop10 = ndcg;    }
		    else if(rank> 10  && rank<=15 )  {  	dcgAtTop15 = dcg;        	 ndcgAtTop15 = ndcg;  	} 
		    else if(rank> 15  && rank<=30 )  {  	dcgAtTop30 = dcg;        	 ndcgAtTop30 = ndcg;  	} 
		    else if(rank> 30  && rank<=50 )  {  	dcgAtTop50 = dcg;        	 ndcgAtTop50 = ndcg;  	} 
		    else if(rank> 50  && rank<=100)  {     dcgAtTop100 = dcg;        	 ndcgAtTop100 = ndcg;  	} 
		    else if(rank > 100) {             dcgOutsideTop100 = dcg;  	     ndcgOutsideTop100= ndcg;  	} 
			
			if(rank==5 || rank==10 || rank==15 || rank==30 || rank==50 || rank==100) {
//##				System.out.println("\t\t Discounted Cumulative Gain at k=" + (rank) + " deno "+ deno + " (irelevanceValue("+irelevanceValue+") / deno2("+deno2+")) "+ " ndcg = dcg("+dcg+")/idcg("+idcg+") ");
			}
			//System.out.println("\tdcg " + dcg+ " idcg " + idcg  + " ndcg " + ndcg + "  rank " + rank);
			if(outputForDebug)
				System.out.println("\tProposal a: "+ pNum + ", rank: " + rank+ " preAtK: " +preAtK +  " recAtK: " +recAtK 
					+ " dcg: " +dcg + " ndcg: " +ndcg + ", idcg "+idcg + ", dcgAtTop5: " + dcgAtTop5 + ", dcgAtTop10: " + dcgAtTop10 
	        		+ " dcgAtTop15: " + dcgAtTop15 + ", dcgAtTop30: " + dcgAtTop30 + ", dcgAtTop50: " + dcgAtTop50 + ", dcgAtTop100: " + dcgAtTop100 + ", dcgOutsideTop100: " + dcgOutsideTop100
	        		+ " currentRecall_ForAVGPreMean:" +currentRecall_ForAVGPreMean + " precision_ForAVGPreMean: "+ precision_ForAVGPreMean + " deno "+deno + " deno2 "+deno2);
//			trackForEachRow(pNum,state, 0, rank, 0,0,
//					matchedAtIndex, 0,deno,deno2,preAtK, recAtK, dcg,idcg,ndcg,precision_ForAVGPreMean,currentRecall_ForAVGPreMean, conn);
			//System.out.println("\tcheck");
			
			////update values for each propsoal

			//may 2020
			//em.setRecallatk(recAtK);
			//add to list
			//Note, we have already added the element to list, here we just update the values, 
			//since the elements in list starts from 0 and our ranks start from 1, we minus 1, when referencing
			//System.out.println("\trank " + rank);
			ematkListForEachProposal.get(t).setDcg(dcg);   			//ematkListForEachProposal.get(rank-1).setDcg(dcg); 
			ematkListForEachProposal.get(t).setIdealDcg(idcg); 		//ematkListForEachProposal.get(rank-1).setIdealDcg(idcg); 
			ematkListForEachProposal.get(t).setNdcg(ndcg); 			//ematkListForEachProposal.get(rank-1).setNdcg(ndcg);
			//increment values in list for all proposals in state
			//this time we dont need to reference the '-1' as we already refeence at position 1, although the list starts at zero --- but element at -1 is never populated
			//ematkListForEachProposalInState.get(rank-1).setDcg( 		   dcg  + ematkListForEachProposalInState.get(rank-1).getDcg()  );
			double a = ematkListForEachProposalInState.get(t).getDcg(); 
			if (Double.isNaN(a))  a=0.0;				
			ematkListForEachProposalInState.get(t).setDcg( 		   dcg  +  a    );
			//ematkListForEachProposalInState.get(rank-1).setIdealDcg(     idcg + ematkListForEachProposalInState.get(rank-1).getIdealDcg()     );
			double b = ematkListForEachProposalInState.get(t).getIdealDcg(); 
			if (Double.isNaN(b))  b=0.0;	
			ematkListForEachProposalInState.get(t).setIdealDcg(     idcg +  b   );
			
			//may 2020..debug
			//if(ematkListForEachProposalInState.get(t).getIdealDcg() > 0 && ematkListForEachProposalInState.get(t).getIdealDcg() < 2) //== 0.0 || ematkListForEachProposalInState.get(t).getIdealDcg() <1)
			if(t<1) {	
				//System.out.println("\t state " +state+ " Proposal a: "+ pNum + " idcg > 0. IDCG value = " + ematkListForEachProposalInState.get(t).getIdealDcg() );
				trackForEachProposal(pNum,state,reasonSentenceMatchedInProposal, preAtTop5, preAtTop10,preAtTop15,preAtTop30,preAtTop50,preAtTop100,preOutsideTop100, 
						sumPreAtTop5,sumPreAtTop10,sumPreAtTop15,sumPreAtTop30,sumPreAtTop50,sumPreOutsideTop100,
						dcg, ndcg,sum_dcg_ForAllProposals,sum_ndcg_ForAllProposals,finalAveragePrecisionForCurrProposal,sumOfAllPrecisionsForAllProposalsForCurrState, evalLevel, rank, idcg,  conn);
			}
			
			//ematkListForEachProposalInState.get(rank-1).setNdcg(         ndcg + ematkListForEachProposalInState.get(rank-1).getNdcg()     );
			double c = ematkListForEachProposalInState.get(t).getNdcg() ; 
			if (Double.isNaN(c))  c=0.0;
			ematkListForEachProposalInState.get(t).setNdcg(         ndcg +   c  );
			
		} //end iteration for each ranked result
		
		//final dcg and ndcg values at k is inclusive of above values	. if below value is zero, we assume that that rank has not been encountered so we assign it
		
		//added code feb 13th
		if(dcgAtTop2==0) {			dcgAtTop2 =dcgAtTop1; 			ndcgAtTop2 = ndcgAtTop1;			}
		if(dcgAtTop3==0) {			dcgAtTop3 =dcgAtTop2; 			ndcgAtTop3 = ndcgAtTop2;			}
		if(dcgAtTop4==0) {			dcgAtTop4 =dcgAtTop3; 			ndcgAtTop4 = ndcgAtTop3;			}
		//end code added feb 13th
		
		if(dcgAtTop10==0) {			dcgAtTop10 =dcgAtTop5; 			ndcgAtTop10 = ndcgAtTop5;			}
		if(dcgAtTop15==0) { 		dcgAtTop15 =dcgAtTop10; 		ndcgAtTop15 = ndcgAtTop10;			}
		if(dcgAtTop30==0) {			dcgAtTop30 =dcgAtTop15;			ndcgAtTop30 += ndcgAtTop15;			}
		if(dcgAtTop50==0) {			dcgAtTop50 =dcgAtTop30; 		ndcgAtTop50 = ndcgAtTop30;			}
		if(dcgAtTop100==0){			dcgAtTop100 =dcgAtTop50; 		ndcgAtTop100 = ndcgAtTop50;			}
		if(dcgOutsideTop100==0) {	dcgOutsideTop100 =dcgAtTop100; ndcgOutsideTop100 = ndcgAtTop100;	}
		//NOW we assign the pre@k and rec@k based on the counts we reached to 5, 10, 15, 30, 50, etc from the results
				//these are values assigned for the next proposal
				
		//precision sum for current proposal. 
		//At different levels would be inclusive of the value from above
		
		//april - we only do for proposals where a manual reason sentence is matched - we can say entered?
		if(reasonSentenceMatchedInProposal) {
			
		/*	
		// 1st Parameter - total sentences matched at position k
		// 2nd Parameter - total proposals where reason sentences entered for that particular state
		// 3rd parameter - rank
		public static float precisionAtK(float numberCorrectAtK, int totalPRSEFTS, int position) { // dont need this as will be handled with mean average precision, int numberOfProposalswithManualSentences) {
			float a = numberCorrectAtK/totalPRSEFTS;
			return     a/position;
			//return (numberCorrectAtK/position); // * numberOfProposalswithManualSentences;
		}
		*/
			/* code before feb 13th 2020 - last parameter values dont seem right
		preAtTop1   	 = precisionAtK(countMatchedAtTop1,1,5);	preAtTop2   = precisionAtK(countMatchedAtTop2,1,5);	preAtTop3   = precisionAtK(countMatchedAtTop3,1,5);	preAtTop4 	 = precisionAtK(countMatchedAtTop4,1,5);
		preAtTop5   	 = precisionAtK(countMatchedAtTop5,1,5); 	preAtTop10  	 = precisionAtK(countMatchedAtTop10,1,10);		preAtTop15  	 = precisionAtK(countMatchedAtTop15,1,15); 
		preAtTop30  	 = precisionAtK(countMatchedAtTop30,1,30); 	preAtTop50  	 = precisionAtK(countMatchedAtTop50,1,5);		preAtTop100 	 = precisionAtK(countMatchedAtTop100,1,10); 
		preOutsideTop100 = precisionAtK(countMatchedOutsideTop100,1,15); 
		*/
		//new code on feb 13 2020, last parameter value corrected
		preAtTop1   	 = precisionAtK(countMatchedAtTop1,1,1);	preAtTop2   = precisionAtK(countMatchedAtTop2,1,2);	preAtTop3   = precisionAtK(countMatchedAtTop3,1,3);	preAtTop4 	 = precisionAtK(countMatchedAtTop4,1,4);
		preAtTop5   	 = precisionAtK(countMatchedAtTop5,1,5); 	preAtTop10  	 = precisionAtK(countMatchedAtTop10,1,10);		preAtTop15  	 = precisionAtK(countMatchedAtTop15,1,15); 
		preAtTop30  	 = precisionAtK(countMatchedAtTop30,1,30); 	preAtTop50  	 = precisionAtK(countMatchedAtTop50,1,50);		preAtTop100 	 = precisionAtK(countMatchedAtTop100,1,100); 
		preOutsideTop100 = precisionAtK(countMatchedOutsideTop100,1,100); 
		//totals for all proposals in this state adds the current proposal	
		sumPreAtTop1+=preAtTop1;    sumPreAtTop2+=preAtTop2;	    sumPreAtTop3+=preAtTop3;	sumPreAtTop4+=preAtTop4;	
		sumPreAtTop5+=preAtTop5;	sumPreAtTop10+=preAtTop10;		sumPreAtTop15+=preAtTop15;	sumPreAtTop30+=preAtTop30;
		sumPreAtTop50+=preAtTop50;	sumPreAtTop100+=preAtTop100;	sumPreOutsideTop100+=preOutsideTop100;
		
		//System.out.println("\t\t\tpreAtK("+preAtK+")=matchedSent("+matchedSent+")/rank("+rank+")");
		recAtTop1 		 = recallAtK(countMatchedAtTop1,1, numManualEntertedSentencesForProposalState);		recAtTop2 = recallAtK(countMatchedAtTop2,1, numManualEntertedSentencesForProposalState);	
		recAtTop3 		 = recallAtK(countMatchedAtTop3,1, numManualEntertedSentencesForProposalState);		recAtTop4 = recallAtK(countMatchedAtTop4,1, numManualEntertedSentencesForProposalState);	
		recAtTop5 		 = recallAtK(countMatchedAtTop5,1, numManualEntertedSentencesForProposalState);		recAtTop10 = recallAtK(countMatchedAtTop10,1, numManualEntertedSentencesForProposalState);
		recAtTop15 		 = recallAtK(countMatchedAtTop15,1, numManualEntertedSentencesForProposalState);	recAtTop30 = recallAtK(countMatchedAtTop30,1, numManualEntertedSentencesForProposalState);
		recAtTop50 		 = recallAtK(countMatchedAtTop50,1, numManualEntertedSentencesForProposalState);	recAtTop100= recallAtK(countMatchedAtTop100,1, numManualEntertedSentencesForProposalState);
		recOutsideTop100 = recallAtK(countMatchedOutsideTop100,1, numManualEntertedSentencesForProposalState);
		
		sumRecAtTop1+=recAtTop1;	sumRecAtTop2+=recAtTop2;	  sumRecAtTop3+=recAtTop3; 		sumRecAtTop4+=recAtTop4;
		sumRecAtTop5+=recAtTop5;	sumRecAtTop10+=recAtTop10;	  sumRecAtTop15+=recAtTop15;	sumRecAtTop30+=recAtTop30;
		sumRecAtTop50+=recAtTop50;	sumRecAtTop100+=recAtTop100;  sumRecOutsideTop100+=recOutsideTop100;
		
		//sum all precisions to calculate Average Mean Precision
		//Average mean precision for all Proposals..WILL BE DIVIDED BY #PROPOSALS
		
			sumOfAllPrecisionsForAllProposalsForCurrState = sumOfAllPrecisionsForAllProposalsForCurrState + finalAveragePrecisionForCurrProposal; //CurrProposalWithCurrStateList.get(s).getPrecision(); //increment the sumOfAllPrecisionsForAllProposalsForCurrState
		}
		if(outputForDebug)
			System.out.println("Proposal b: "+ pNum + "reasonSentenceMatchedInProposal "+ reasonSentenceMatchedInProposal+ " rank: " + rank+ " ndcg "+ndcg+" sumOfAllPrecisionsForAllProposalsForCurrState: "+ sumOfAllPrecisionsForAllProposalsForCurrState 
				+ " finalAveragePrecisionForCurrProposal " + finalAveragePrecisionForCurrProposal); 
		//rec = recallAtK(num matches till then,, manualLabelCounter);
		
		//sum of dcg and ndcg for all proposals will add the last entry in every criteria 
		sumDcgAtTop1 +=  dcgAtTop1;					sumNdcgAtTop1  += ndcgAtTop1;
		sumDcgAtTop2 +=  dcgAtTop2;					sumNdcgAtTop2  += ndcgAtTop2;
		sumDcgAtTop3 +=  dcgAtTop3;					sumNdcgAtTop3  += ndcgAtTop3;
		sumDcgAtTop4 +=  dcgAtTop4;					sumNdcgAtTop4  += ndcgAtTop4;
		
		sumDcgAtTop5 +=  dcgAtTop5;					sumNdcgAtTop5  += ndcgAtTop5;		
		sumDcgAtTop10 += dcgAtTop10; 				sumNdcgAtTop10 += ndcgAtTop10; 
		sumDcgAtTop15 += dcgAtTop15;				sumNdcgAtTop15 += ndcgAtTop15;		
		sumDcgAtTop30 += dcgAtTop30;				sumNdcgAtTop30 += ndcgAtTop30;	
		sumDcgAtTop50 += dcgAtTop50;   				sumNdcgAtTop50 += ndcgAtTop50;		
		sumDcgAtTop100 += dcgAtTop100;  			sumNdcgAtTop100 += ndcgAtTop100;
		sumDcgOutsideTop100 += dcgOutsideTop100; 	sumNdcgOutsideTop100 += ndcgOutsideTop100;
		
		//After each proposal, we increment the sum dcg, cumulated value for all proposals - just for storing..is not used in calculation
		sum_dcg_ForAllProposals = sum_dcg_ForAllProposals= + dcg;	
		sum_ndcg_ForAllProposals = sum_ndcg_ForAllProposals= + ndcg;	//cumulated value fo the proposal  //we dont need
		
//		System.out.println(" Proposal: "+ pNum + " dcg: " + dcg+ " ndcg "+ndcg+" sum_dcg: "+ sum_dcg + " dcg " + dcg + " rank " + rank + " sum_ndcg: "+ sum_ndcg); 

		//if(prNum==308) {
	    /*    System.out.println("\tProposal: "+ pNum + ", rank: " + rank+ " dcg: " +dcg + " ndcg: "+ ndcg + ", dcgAtTop5: " + dcgAtTop5 + ", dcgAtTop10: " + dcgAtTop10 
	        		+ ", dcgAtTop15: " + dcgAtTop15 + ", dcgAtTop30: " + dcgAtTop30 + ", dcgAtTop50: " + dcgAtTop50 + ", dcgAtTop100: " + dcgAtTop100 + ", dcgOutsideTop100: " + dcgOutsideTop100
	        		+ " sum_dcg_ForAllProposals " +sum_dcg_ForAllProposals + " sum_ndcg_ForAllProposals: "+ sum_ndcg_ForAllProposals 
	        		+ " preAtTop5 "+ preAtTop5 + " preAtTop15 "+preAtTop15 + " preAtTop30 "+ preAtTop30 + " preAtTop50 "+preAtTop50 + " preAtTop100 " + preAtTop100 +" preOutsideTop100 "+ preOutsideTop100
	        		+ " sumPreAtTop5 "+sumPreAtTop5+ " sumPreAtTop10 "+sumPreAtTop10 ); */
		//}
	        
	    System.out.println(pNum + state+ ", "+preAtTop5+ ", "+ preAtTop10+ ", "+preAtTop15+ ", "+preAtTop30+ ", "+preAtTop50+ ", "+preAtTop100+ ", "+preOutsideTop100+ ", "+ 
					sumPreAtTop5+ ", "+sumPreAtTop10+ ", "+sumPreAtTop15+ ", "+sumPreAtTop30+ ", "+sumPreAtTop50+ ", "+sumPreOutsideTop100+ ", "+
		dcg+ ", "+ ndcg+ ", "+sum_dcg_ForAllProposals+ ", "+sum_ndcg_ForAllProposals+ ", "+finalAveragePrecisionForCurrProposal+ ", "+sumOfAllPrecisionsForAllProposalsForCurrState+ ", ");
	        
	    
	    
		//trackForEachProposal(pNum,state,reasonSentenceMatchedInProposal, preAtTop5, preAtTop10,preAtTop15,preAtTop30,preAtTop50,preAtTop100,preOutsideTop100, 
		//								sumPreAtTop5,sumPreAtTop10,sumPreAtTop15,sumPreAtTop30,sumPreAtTop50,sumPreOutsideTop100,
		//					dcg, ndcg,sum_dcg_ForAllProposals,sum_ndcg_ForAllProposals,finalAveragePrecisionForCurrProposal,sumOfAllPrecisionsForAllProposalsForCurrState, evalLevel, rank, idcg,  conn);
	}
    
    public static float logBase2(double d) {
	      return (float) (Math.log(d)/Math.log(2.0));
    }
    
	public void printForAllProposalInThatState(String state, Integer proposalCounter, String evalLevel, List<Integer> listProposalReasonSentenceManuallyEntered, 
			List<Integer> listProposalReasonSentenceNotManuallyEntered, List<Integer> listOfProposalsNotRecorded, Integer totalDistinctProposalsCommittedForCurrentState, 
			Integer manualLabelCounter, Integer notMatched) {
		
		int totalDistinctProposalsCommittedForCurrentStateForWhichReasonSentencesManuallyRecorded = listProposalReasonSentenceManuallyEntered.size(); //total proposals where reason sentences entered for that particular state
		String  t1 = "\tTop 1:", t2 = "\tTop 2:", t3 = "\tTop 3:", t4 = "\tTop 4:",
				t5 = "\tTop 1-5:", t10 = "\tTop 10:", t15 = "\tTop 15:",t30 = "\tTop 30:",t50 = "\tTop 50:",t100 = "\tTop 100:",t100plus = "\tTop 100+:",  rank = "\tRank: ", 
					cnt= "Count:", avg = "AVG (%): ", pre = "Pre@k: ", rec = "Rec@k: ", dcg = "DCG@k", ndcg = "NCG@k", tab="\t",nn = "Na",
					a= "\tTotal DISTINCT Proposals COMMITTED:",
					b= "\tTotal DISTINCT Proposals COMMITTED FOR WHICH reason sentences Recorded Manually:",
					c= "\tTotal DISTINCT Proposals COMMITTED FOR WHICH reason sentences Not Recorded Manually:",
					d= "\tTotal DISTINCT Proposals NOT COMMITTED BUT with reason sentences Recorded Manually:",
					e= "\tTotal Reason Sentences for all DISTINCT Proposals AND STATES COMMITTED: ";
		//easier to assign it rather than change at all places below
		proposalCounter = totalDistinctProposalsCommittedForCurrentStateForWhichReasonSentencesManuallyRecorded; //April 2019, We divide by the number of proposals for which we have manual sentences , not by total number of proposals with state committed
		//System.out.println("\n\t\tShowing results for "+ state + ", " + evalLevel + " level.");
		System.out.printf("%-100s %-10s \n",a, totalDistinctProposalsCommittedForCurrentState );    //"%4d",
		//System.out.println("\t\tAbove minus Proposals with reason '"+evalLevel+"' not entered "+listProposalReasonSentenceNotManuallyEntered.size());
		System.out.printf("%-100s %-10s \n",b, totalDistinctProposalsCommittedForCurrentStateForWhichReasonSentencesManuallyRecorded);
		Set<Integer> uniqueProposalsForWhichReasonSentenceNotRecordedManually = new HashSet<Integer>(listOfProposalsNotRecorded); 		//retrieve unique ones from arraylist
		System.out.printf("%-100s %-10s \n",c,uniqueProposalsForWhichReasonSentenceNotRecordedManually.size()); //listOfProposalsNotRecorded.size() );
		System.out.printf("%-100s %-10s \n",d,t5);
		// + " : " + listOfProposalsNotRecorded.toString());
		System.out.printf("%-100s %-10s \n",e,manualLabelCounter);
		//WE dont consider sentences from PEPs which dont have states committed
		System.out.println("\tproposalCounter:"+proposalCounter + " preAtTop5=preAtTop5("+sumPreAtTop5+")/proposalCounter("+proposalCounter+"),  recAtTop5=recAtTop5("+sumRecAtTop5+")/proposalCounter("+proposalCounter+") "); 
		System.out.println("\tproposalCounter:"+proposalCounter + " preAtTop50=preAtTop50("+sumPreAtTop50+")/proposalCounter("+proposalCounter+"),  recAtTop50=recAtTop50("+sumRecAtTop50+")/proposalCounter("+proposalCounter+") ");
				
		System.out.println("\tThe value of Pre and Rec for each Proposal (and maybe for all proposals) should get closer to 1.");
		System.out.println();
		//System.out.println("ndcgAtTop5: "+ ndcgAtTop5);
		System.out.printf("%-10s %-10s  %-4s %-4s %-4s %-5s %-5s \n",rank,cnt,avg,pre,rec,dcg,ndcg);  // "%.2f"
		
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t1,  top1,  average(top1,manualLabelCounter), sumPreAtTop1/proposalCounter, sumRecAtTop1/proposalCounter, averageDouble(sumDcgAtTop1,proposalCounter), averageDouble(sumNdcgAtTop1,proposalCounter)    );    //recallAtK(top5, manualLabelCounter)  );
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t2,  top2,  average(top2,manualLabelCounter), sumPreAtTop2/proposalCounter, sumRecAtTop2/proposalCounter, averageDouble(sumDcgAtTop2,proposalCounter), averageDouble(sumNdcgAtTop2,proposalCounter)    );    //recallAtK(top5, manualLabelCounter)  );
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t3,  top3,  average(top3,manualLabelCounter), sumPreAtTop3/proposalCounter, sumRecAtTop3/proposalCounter, averageDouble(sumDcgAtTop3,proposalCounter), averageDouble(sumNdcgAtTop3,proposalCounter)    );    //recallAtK(top5, manualLabelCounter)  );
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t4,  top4,  average(top4,manualLabelCounter), sumPreAtTop4/proposalCounter, sumRecAtTop4/proposalCounter, averageDouble(sumDcgAtTop4,proposalCounter), averageDouble(sumNdcgAtTop4,proposalCounter)    );    //recallAtK(top5, manualLabelCounter)  );
				
		//System.out.println("\tRECALL: Top5/manualLabelCounter: " + top5/manualLabelCounter + " p: "+ (double) (top5/totalPRSEFTS)/5);  //precisionAtK(preAtTop5,totalPRSEFTS, 5) //recallAtK(recAtTop5,totalPRSEFTS, proposalCounter) 
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t5,  top5,  average(top5,manualLabelCounter), sumPreAtTop5/proposalCounter  , sumRecAtTop5/proposalCounter  , averageDouble(sumDcgAtTop5,proposalCounter), averageDouble(sumNdcgAtTop5,proposalCounter)    );    //recallAtK(top5, manualLabelCounter)  );
		//System.out.println("\tTop 5, Proposals: " + top5  + ", AVG: " + average(top5,manualLabelCounter) + "" + precisionAtK(top5, 5, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top5, manualLabelCounter) );
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t10,  top10, average(top10,manualLabelCounter),  sumPreAtTop10/proposalCounter , sumRecAtTop10/proposalCounter  ,  averageDouble(sumDcgAtTop10,proposalCounter), averageDouble(sumNdcgAtTop10,proposalCounter)  );
		//System.out.println("\tTop 10, Proposals: " + top10 + ", AVG:  " + average(top10,manualLabelCounter) + "%, Precision@k " + precisionAtK(top10, 10, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top10, manualLabelCounter));
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t15,  top15, average(top15,manualLabelCounter),  sumPreAtTop15/proposalCounter , sumRecAtTop15/proposalCounter ,  averageDouble(sumDcgAtTop15,proposalCounter), averageDouble(sumNdcgAtTop15,proposalCounter)  );
		//System.out.println("\tTop 15, Proposals: " + top15 + ", AVG: " + average(top15, manualLabelCounter) + "%, Precision@k " + precisionAtK(top15, 15, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top15, manualLabelCounter));
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t30,  top30,  average(top30,manualLabelCounter), sumPreAtTop30/proposalCounter , sumRecAtTop30/proposalCounter  ,  averageDouble(sumDcgAtTop30,proposalCounter), averageDouble(sumNdcgAtTop30,proposalCounter)   );
		//System.out.println("\tTop 30, Proposals: " + top30 + ", AVG: " + average(top30, manualLabelCounter) + "%, Precision@k " + precisionAtK(top30, 30, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top30, manualLabelCounter));
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t50,  top50,  average(top50,manualLabelCounter),  sumPreAtTop50/proposalCounter, sumRecAtTop50/proposalCounter  ,  averageDouble(sumDcgAtTop50,proposalCounter), averageDouble(sumNdcgAtTop50,proposalCounter)   );
		//System.out.println("\tTop 50, Proposals: " + top50 + ", AVG: " + average(top50, manualLabelCounter) + "%, Precision@k " + precisionAtK(top50, 50, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top50, manualLabelCounter));
		
		System.out.printf("%-10s %-10s  %.4f %.4f %.4f %.4f %.4f \n", t100,  top100,  average(top100,manualLabelCounter),  sumPreAtTop100/proposalCounter, sumRecAtTop100/proposalCounter  , averageDouble(sumDcgAtTop100,proposalCounter), averageDouble(sumNdcgAtTop100,proposalCounter)   );
		//System.out.println("\tTop 100, Proposals: " + top100 + ", AVG: " + average(top100, manualLabelCounter) + "%, Precision@k " + precisionAtK(top100, 100, listProposalReasonSentenceManuallyEntered.size()) + " Recall@k " + recallAtK(top100, manualLabelCounter));
		System.out.printf("%-10s %-10s  %.4f %-4s %-4s %-4s %-4s \n", t100plus,  outsideTop100,  average(outsideTop100,manualLabelCounter), nn, nn, nn, nn );// preOutsideTop100/proposalCounter , recOutsideTop100/proposalCounter , averageDouble(dcgOutsideTop100,proposalCounter) , averageDouble(ndcgOutsideTop100,proposalCounter)  );
		//System.out.println("\tOutside top  100, Proposals: " + outsideTop100 + ", 	%: " + (outsideTop100 * 100.0f)/ manualLabelCounter ); //top5+top10+top15+top30+top50+top100+
		
		System.out.println("\tMean Average Precision: sumOfAllPrecisionsForAllProposalsForCurrState("+sumOfAllPrecisionsForAllProposalsForCurrState+")/proposalCounter"+proposalCounter);
		System.out.println("\tMean Average Precision: " + sumOfAllPrecisionsForAllProposalsForCurrState/proposalCounter);
		
		System.out.println("\tTotal Sentences not found:	    " + notMatched + ", 	%: " + (notMatched * 100.0f) 	/ manualLabelCounter );
		System.out.println("\tTotal Proposals no manual entry:	" + listProposalReasonSentenceNotManuallyEntered.size());	//	 +  
				//", 	Percent: " + (listProposalReasonSentenceNotManuallyEntered.size() * 100.0f) 	/ manualLabelCounter );
		
		//April 2019.. We only consider couple of metrics, precision & recall @k, and average mean precision
		// other metrics like cumulative gain, normalised cumulative gain are for cases where there are more than two types of relevant documents, e.g bad, fair, good
		//average mean precision
		//first we find the mean precision for each proposal - precision and recall for all matched manual sentences
		//then we average them to get average mean precision
		
		//to find the mean precision and recall 
		//go over all rankings, check only those where matched == 1, calculate precision and recall at each rank
		//end when we have encountered the last of matched == 1, we calculate the average mean precision
		
		
		System.out.println("");
		
		//may 2020, we output evaluation at each rank
		String  t_r = "\tRank:", t_p = "\tPrecision: ", t_rec = "\tRecall: ", t_dcg = "\tDCG: ", t_ndcg = "\tnDCG: ", t_idealndcg = "\tIdealDCG: ", m = " Matched: ";
		Integer counter=0;
		//may 2020
		
		/*
		for (EvaluationMetricsAtEachRank em : ematkListForEachProposal) {
			counter++;
			if(counter>100)
				break;
			//System.out.println("\tRank: " + em.getRank() + " Precision: " + em.precisionatk  + " Recall: " + em.getRecallatk() + " DCG: " + em.getDcg() + " nDCG: " + em.getNdcg()); 
			//System.out.printf("%-10s %f  %-10s %.5f   %-10s %.5f   %-10s %.5f   %-10s %.5f \n", t_r, em.getRank(), t_p, em.getPrecisionatk(), t_r, em.getRecallatk(), t_dcg, em.getDcg(), t_ndcg, em.getNdcg() );
			System.out.printf("%-10s %d %-10s %d %-10s %.4f %-10s %.4f %-10s %.4f %-10s %.4f %-10s %.4f \n", 
					t_r, em.getRank(), m, em.getMatched(), t_p, em.getPrecisionatk(), t_rec, em.getRecallatk(), t_dcg, em.getDcg(),  t_idealndcg, em.getIdealDcg(), t_ndcg, em.getNdcg());
		} */
		
		System.out.println("\tproposalCounter: " + proposalCounter);
		Integer matchedTillThatRank=0;
		for (RankingEvaluation_MetricsAtEachRank em : ematkListForEachProposalInState) {
			counter++;
			Integer de = em.getMatched();  //we cant devide by 0
			if(de==0) de = 1;
			if(counter>100)
				break;
			
			//ore@k and rec@k are divided by the 'proposalCounter' as to find the average for after each rank
			
			//System.out.println("\tRank: " + em.getRank() + " Precision: " + em.precisionatk  + " Recall: " + em.getRecallatk() + " DCG: " + em.getDcg() + " nDCG: " + em.getNdcg()); 
			//System.out.printf("%-10s %f  %-10s %.5f   %-10s %.5f   %-10s %.5f   %-10s %.5f \n", t_r, em.getRank(), t_p, em.getPrecisionatk(), t_r, em.getRecallatk(), t_dcg, em.getDcg(), t_ndcg, em.getNdcg() );
//			System.out.printf("%-10s %d %-10s %d %-10s %.4d %-10s %.4f %-10s %.4f %-10s %.4f %-10s %.4f \n", 
					//for precision at k and recall at k, rather than dividing by the number of totakl proposals , we divid eby the number matched at that rank -
					//this is different and may seem strange but the accumuklated pre@k and rec@k are for all sentences smatched at that point, sio tahts why we divide by the number of sentences matched at that rank
					//old way
					//t_r, em.getRank(), m, em.getMatched(), t_p, em.getPrecisionatk()/proposalCounter, t_rec, em.getRecallatk()/proposalCounter, t_dcg, em.getDcg(),  t_idealndcg, em.getIdealDcg(), t_ndcg, em.getNdcg()/proposalCounter);   // em.getDcg()/em.getIdealDcg()
			float p = em.getRank();
			matchedTillThatRank += em.getMatched();
			float r = ((float) matchedTillThatRank/manualLabelCounter);
			System.out.println(t_r + " " +  p + " " + m + " " +  em.getMatched() + " " + matchedTillThatRank +  t_p + " " +  matchedTillThatRank/(p*proposalCounter) 
					+ " " +  t_rec + " " +  r + " " +  t_dcg + " " +  em.getDcg() + " " +   t_idealndcg + " " +  em.getIdealDcg() + " " +  t_ndcg + " " +  em.getNdcg()/proposalCounter);   // em.getDcg()/em.getIdealDcg()
			
		} //proposalCounter
		
	}
	
	//Relevance  values are two values, 0 or 1 only. Relevance is not any intermediate values.
	//For Accepted
	//Precision at k = number of correct captured / position * # of proposals
	
	//Precision: proportion of retrieved documents that are relevant
	//   number correct/ k * number of peps with manual reason entry - there are peps for which we could not find reason sentences
	//Recall: proportion of relevant documents that are retrieved
	//  number correct/total number of reason sentences manual entry
	
	//java division
	//int x = 85, y = 86;    
	//double t = ((double) x) / y;
    //System.out.println(t);
	
	//may 2020 ...rechecked below calculation is accurate
	//Precision at k = number of retrieved items at k that are relevant/ number of recommended items
	//recall at k = number of relevant at k / total relevant
	
	// 1st Parameter - total sentences matched at position k
	// 2nd Parameter - total proposals where reason sentences entered for that particular state
	// 3rd parameter - rank
	public static float precisionAtK(float numberCorrectAtK, int totalPRSEFTS, int position) { // dont need this as will be handled with mean average precision, int numberOfProposalswithManualSentences) {
		float a = numberCorrectAtK/totalPRSEFTS;
		return     a/position;
		//return (numberCorrectAtK/position); // * numberOfProposalswithManualSentences;
	}
	
	public static float recallAtK(float numberCorrectAtK, int totalPRSEFTS, int numberOfManualSentencesForThatProposal) { /* int position, int numberOfProposals, */
		float b = numberCorrectAtK/totalPRSEFTS;
		return     b/numberOfManualSentencesForThatProposal;
		//return (numberCorrectAtK/numberOfManualSentencesForThatProposal);
	}
	
	
	//March 2019 ..Ranking Evaluation
	//	/(top5  * 100.0f)/ manualLabelCounter
	public static double average (int k, int sentencesfound) {
		return (k  * 100.0f)/ sentencesfound;
	}
	public static double averageDouble (double dcg, int manualSentenceCounter) {
		//return (k  * 100.0f)/ sentencesfound;
		return     ((double)  dcg)/manualSentenceCounter;
	}
	
	public void trackForEachRow(Integer pnum, String state, Integer index, Integer rank, Integer rankByTP,Integer mid, boolean matchedAtIndex, 
			float previousSumPreAtTop10,float deno,float deno2, float preAtK, float recAtK, float dcg, float idcg, float ndcg,  float precision_ForAVGPreMean, float currentRecall_ForAVGPreMean, Connection conn) {
		
		  String query = "INSERT INTO TRACKFOREACHROW (proposal, state, ind, rank, rankByTP, matchedAtIndex, previousSumPreAtTop10, deno, deno2,"
		  		+ " preAtK, recAtK, dcg,idcg,ndcg,precision_ForAVGPreMean,currentRecall_ForAVGPreMean, messageid) "
		  		+ " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?)";
		  try {
		    PreparedStatement st = conn.prepareStatement(query);	// set all the preparedstatement parameters2
		    st.setInt(1, pnum);		    st.setString(2, state);  	st.setInt(3, index);		    	st.setInt(4, rank); 	    st.setInt(5, rankByTP);	   
		    st.setBoolean(6, matchedAtIndex); 	st.setFloat(7, previousSumPreAtTop10); 	st.setFloat(8, deno);	st.setFloat(9, deno2);
		    st.setFloat(10, preAtK); 	st.setFloat(11, recAtK);	 		st.setFloat(12, dcg); 		st.setFloat(13, idcg);		st.setFloat(14, ndcg);	
		    st.setFloat(15, precision_ForAVGPreMean); 		st.setFloat(16, currentRecall_ForAVGPreMean);		st.setInt(17, mid);		
		    st.executeUpdate();		    st.close();	// execute the preparedstatement insert
		  } 
		  catch (SQLException se) {
		    // log exception
			  System.out.println("Exception " + se.toString());  			System.out.println(StackTraceToString(se)  );
		  }
		
	}	
	
//	public void updatetrackForEachProposal(Integer pnum, String state, Integer rank, float idcg, String evalLevel, Connection conn) {
//		  // proposal INTEGER,	 state	 TEXT, 	
//		  //preAtK   DOUBLE,	    recAtK   DOUBLE,	 dcg   	  DOUBLE,	 ndcg     DOUBLE,	 precision_ForAVGPreMean DOUBLE,  currentRecall_ForAVGPreMean DOUBLE
//		  
//		String query = "update TRACKFOREACHPROPOSAL SET rank = ?, idcg = ? WHERE proposal = ? and state = ? and evalLevel = ?)";
//		  try {
//		    PreparedStatement st = conn.prepareStatement(query);	// set all the preparedstatement parameters
//		    st.setInt(1, rank);   		st.setFloat(2, idcg);	  st.setInt(3, pnum);	   st.setString(4, state); st.setString(5, evalLevel);	
//		    st.executeUpdate();		    st.close();	// execute the preparedstatement insert	
//		  } 
//		  catch (SQLException se) {
//		    // log exception
//			  System.out.println("Exception " + se.toString());  			System.out.println(StackTraceToString(se)  );
//		  }		
//	}	
	
	
	public void trackForEachProposal(Integer pnum, String state, boolean matchedInProposal, float preAtTop5, float preAtTop10, float preAtTop15, float preAtTop30, float preAtTop50, float preAtTop100, float preOutsideTop100,
			float sumPreAtTop5,float sumPreAtTop10,float sumPreAtTop15, float sumPreAtTop30, float sumPreAtTop50, float sumPreOutsideTop100,
			float dcg,float ndcg, float sum_dcg,float sum_ndcg, float finalAveragePrecision, float sumOfAllPrecisions, String evalLevel, Integer v_rank, float idcg, Connection conn) {
		  // proposal INTEGER,	 state	 TEXT, 	
		  //preAtK   DOUBLE,	    recAtK   DOUBLE,	 dcg   	  DOUBLE,	 ndcg     DOUBLE,	 precision_ForAVGPreMean DOUBLE,  currentRecall_ForAVGPreMean DOUBLE
		  
		String query = "INSERT INTO TRACKFOREACHPROPOSAL (proposal, state,preAtTop5,preAtTop10,preAtTop15,preAtTop30,preAtTop50,preAtTop100,"
				+ " recAtTop5, recAtTop10, recAtTop15, recAtTop30, recAtTop50, recAtTop100, " //recOutsideTop100, 
				+ " sumPreAtTop5,sumPreAtTop10,sumPreAtTop15,sumPreAtTop30, sumPreAtTop50,sumPreOutsideTop100, dcg, ndcg, sum_dcg, sum_ndcg,finalAvgPre, sumOfAllPre,matchedInProposal,evalLevel,idcg,rank) "
		  		+ " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?)";
		  try {
		    PreparedStatement st = conn.prepareStatement(query);	// set all the preparedstatement parameters
		    st.setInt(1, pnum);		    st.setString(2, state);  st.setFloat(3, preAtTop5);	st.setFloat(4, preAtTop10);	st.setFloat(5, preAtTop15);	st.setFloat(6, preAtTop30);	st.setFloat(7, preAtTop50);	st.setFloat(8, preAtTop100);
		    st.setFloat(9, preAtTop5);	st.setFloat(10, preAtTop10);	st.setFloat(11, preAtTop15);	st.setFloat(12, preAtTop30);	st.setFloat(13, preAtTop50);	st.setFloat(14, preAtTop100);
		    st.setFloat(15, sumPreAtTop5);	st.setFloat(16, sumPreAtTop10); 	st.setFloat(17, sumPreAtTop15); 	st.setFloat(18, sumPreAtTop30);  st.setFloat(19, sumPreAtTop50); 	st.setFloat(20, sumPreOutsideTop100); 
		    st.setFloat(21, dcg);		st.setFloat(22, ndcg); 	    	st.setFloat(23, sum_dcg); 		st.setFloat(24, sum_ndcg);   st.setFloat(25, finalAveragePrecision); 	    st.setFloat(26, sumOfAllPrecisions);
		    st.setBoolean(27,matchedInProposal);	 st.setString(28,evalLevel);  st.setFloat(29, idcg);	    st.setInt(30, v_rank); 
		    st.executeUpdate();		    st.close();	// execute the preparedstatement insert	
		  } 
		  catch (SQLException se) {
		    // log exception
			  System.out.println("Exception " + se.toString());  			System.out.println(StackTraceToString(se)  );
		  }		
	}	
	
	//may 2002 ...track for ewch state
	public void trackForEachState(Integer pnum, String state, boolean matchedInProposal, float preAtTop5, float preAtTop10, float preAtTop15, float preAtTop30, float preAtTop50, float preAtTop100, float preOutsideTop100,
			float sumPreAtTop5,float sumPreAtTop10,float sumPreAtTop15, float sumPreAtTop30, float sumPreAtTop50, float sumPreOutsideTop100,
			float dcg,float ndcg, float sum_dcg,float sum_ndcg, float finalAveragePrecision, float sumOfAllPrecisions,  Connection conn) {
		  // proposal INTEGER,	 state	 TEXT, 	
		  //preAtK   DOUBLE,	    recAtK   DOUBLE,	 dcg   	  DOUBLE,	 ndcg     DOUBLE,	 precision_ForAVGPreMean DOUBLE,  currentRecall_ForAVGPreMean DOUBLE
		  
		String query = "INSERT INTO TRACKFOREACHSTATE (proposal, state,preAtTop5,preAtTop10,preAtTop15,preAtTop30,preAtTop50,preAtTop100,"
				+ " recAtTop5, recAtTop10, recAtTop15, recAtTop30, recAtTop50, recAtTop100, " //recOutsideTop100, 
				+ " sumPreAtTop5,sumPreAtTop10,sumPreAtTop15,sumPreAtTop30, sumPreAtTop50,sumPreOutsideTop100, dcg, ndcg, sum_dcg, sum_ndcg,finalAvgPre, sumOfAllPre,matchedInProposal) "
		  		+ " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?)";
		  try {
		    PreparedStatement st = conn.prepareStatement(query);	// set all the preparedstatement parameters
		    st.setInt(1, pnum);		    st.setString(2, state);  st.setFloat(3, preAtTop5);	st.setFloat(4, preAtTop10);	st.setFloat(5, preAtTop15);	st.setFloat(6, preAtTop30);	st.setFloat(7, preAtTop50);	st.setFloat(8, preAtTop100);
		    st.setFloat(9, preAtTop5);	st.setFloat(10, preAtTop10);	st.setFloat(11, preAtTop15);	st.setFloat(12, preAtTop30);	st.setFloat(13, preAtTop50);	st.setFloat(14, preAtTop100);
		    st.setFloat(15, sumPreAtTop5);	st.setFloat(16, sumPreAtTop10); 	st.setFloat(17, sumPreAtTop15); 	st.setFloat(18, sumPreAtTop30);  st.setFloat(19, sumPreAtTop50); 	st.setFloat(20, sumPreOutsideTop100); 
		    st.setFloat(21, dcg);		st.setFloat(22, ndcg); 	    	st.setFloat(23, sum_dcg); 		st.setFloat(24, sum_ndcg);   st.setFloat(25, finalAveragePrecision); 	    st.setFloat(26, sumOfAllPrecisions);
		    st.setBoolean(27,matchedInProposal);
		    st.executeUpdate();		    st.close();	// execute the preparedstatement insert	
		  } 
		  catch (SQLException se) {
		    // log exception
			  System.out.println("Exception " + se.toString());  			System.out.println(StackTraceToString(se)  );
		  }		
	}	
	
	public static String StackTraceToString(Exception ex) {
		String result = ex.toString() + "\n";
		StackTraceElement[] trace = ex.getStackTrace();
		for (int i=0;i<trace.length;i++) {
			result += trace[i].toString() + "\n";
		}
		return result;
	}
		
	static void insertIntoDatabase(Connection connection, double incrementCounter0, double incrementCounter1, double incrementCounter2,double incrementCounter3, double incrementCounter4, double incrementCounter5, 
			double incrementCounter6, double incrementCounter7, double incrementCounter8, double incrementCounter9, double incrementCounter10, 	double incrementCounter11, double incrementCounter12, 
			/*double incrementCounter13, */ String state, double sentenceHintProbablity, double sentenceLocationHintProbability, double messageSubjectHintProbablityScore,
			double dateDiffProbability, double authorRoleProbability, double negationTermPenalty, double restOfParagraphProbabilityScore, double messageLocationProbabilityScore,
			double messageTypeIsReasonMessageProbabilityScore, double sentenceLocationInMessageProbabilityScore, double sameMsgSubAsStateTripleProbabilityScore, double reasonLabelFoundUsingTripleExtractionProbabilityScore,
			double messageContainsSpecialTermProbabilityScore, double prevParagraphSpecialTermProbabilityScore, int notMatched, String evalLevel) 
	{
		try {
			/*String query = "insert into dynamicweightallocation (label, " //1
			+ " sentenceHintProbablity, sentenceLocationHintProbability, restOfParagraphProbabilityScore, messageLocationProbabilityScore, messageSubjectHintProbablityScore, " //6 
			+ " dateDiffProbability, authorRoleProbability, messageTypeIsReasonMessageProbabilityScore, sentenceLocationInMessageProbabilityScore, sameMsgSubAsStateTripleProbabilityScore," //11
			+ " reasonLabelFoundUsingTripleExtractionProbabilityScore, messageContainsSpecialTermProbabilityScore , prevParagraphSpecialTermProbabilityScore, negationTermPenalty, " //15
			+ " top5, top10, top15, top30, top50, top100,outsidetop100,notfound,totalProbability) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			*/
			
			// create the java mysql update preparedstatement
			String query = "insert into dynamicweightallocation (label, " //1
					+ " sentenceHintProbablity, sentenceLocationInMessageProbabilityScore, restOfParagraphProbabilityScore, messageLocationProbabilityScore, messageSubjectHintProbablityScore, " //6 
					+ " dateDiffProbability, authorRoleProbability, messageTypeIsReasonMessageProbabilityScore, sameMsgSubAsStateTripleProbabilityScore," //10
					+ " reasonLabelFoundUsingTripleExtractionProbabilityScore, messageContainsSpecialTermProbabilityScore , prevParagraphSpecialTermProbabilityScore, negationTermPenalty, " //14
					+ " top5, top10, top15, top30, top50, top100,outsidetop100,notfound,totalProbability,evalLevel) " //24
					+ " VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?)";
			PreparedStatement preparedStmt;
			preparedStmt = connection.prepareStatement(query);
			preparedStmt.setString(1, state);
			
			//sentenceHintProbablity=sentenceLocationHintProbability=messageSubjectHintProbablityScore=dateDiffProbability=authorRoleProbability=negationTermPenalty=0.0;
			
			//Jan 2020..these are the fields in which order they are passed as variables and also inserted in the db 
			// incrementCounter0  - sentenceHintProbablity, 
			// incrementCounter1  - sentenceLocationInMessageProbabilityScore
			// incrementCounter2  - restOfParagraphProbabilityScore
			// incrementCounter3  - messageLocationProbabilityScore 
			// incrementCounter4  - messageSubjectHintProbablityScore 
			// incrementCounter5  - dateDiffProbability
			// incrementCounter6  - authorRoleProbability 
			// incrementCounter7  - messageTypeIsReasonMessageProbabilityScore 
			// incrementCounter8  - sameMsgSubAsStateTripleProbabilityScore
			// incrementCounter9  - reasonLabelFoundUsingTripleExtractionProbabilityScore
			// incrementCounter10 - messageContainsSpecialTermProbabilityScore 
			// incrementCounter11 - prevParagraphSpecialTermProbabilityScore
			// incrementCounter12 - negationTermPenalty
			
			//Dec 22 2019 Just to make sure the order...we have 13 variables 
			//0 setSentenceOrParagraphHintProbablity		//1 setSentenceLocationInMessageProbabilityScore		//2 setRestOfParagraphProbabilityScore		//3 setMessageLocationProbabilityScore
			//4 setMessageSubjectHintProbablityScore		//5 setDateDiffProbability			//6 setAuthorRoleProbability		//7 setMesssageTypeIsReasonMessageProbabilityScore
			// Repeated --- 8 setSentenceLocationInMessageProbabilityScore		
			//8 setSameMsgSubAsStateTripleProbabilityScore		//9 setReasonLabelFoundUsingTripleExtractionProbabilityScore
			//10 setMessageContainsSpecialTermProbabilityScore		//11 setPrevParagraphSpecialTermProbabilityScore		//12 setNegationTermPenalty
			
			//String fields2[] = {"sentenceHintProbablity","sentenceLocationInMessageProbabilityScore","restOfParagraphProbabilityScore","messageLocationProbabilityScore","messageSubjectHintProbablityScore", //5
			//		"dateDiffProbability","authorRoleProbability","messageTypeIsReasonMessageProbabilityScore","sameMsgSubAsStateTripleProbabilityScore", //10
			//		"reasonLabelFoundUsingTripleExtractionProbabilityScore","messageContainsSpecialTermProbabilityScore","prevParagraphSpecialTermProbabilityScore","negationTermPenalty"};
			
			Double totalProb = sentenceHintProbablity /* +sentenceLocationHintProbability */ +restOfParagraphProbabilityScore+messageLocationProbabilityScore+messageSubjectHintProbablityScore
					+dateDiffProbability+authorRoleProbability+
					+messageTypeIsReasonMessageProbabilityScore+sentenceLocationInMessageProbabilityScore+sameMsgSubAsStateTripleProbabilityScore
					+reasonLabelFoundUsingTripleExtractionProbabilityScore+messageContainsSpecialTermProbabilityScore+prevParagraphSpecialTermProbabilityScore-negationTermPenalty;
			//march 2019, store the incremented value for each field as well
			preparedStmt.setDouble(2, incrementCounter0);	preparedStmt.setDouble(3, incrementCounter1); 		preparedStmt.setDouble(4, incrementCounter2);
			preparedStmt.setDouble(5, incrementCounter3);	preparedStmt.setDouble(6, incrementCounter4);
			preparedStmt.setDouble(7, incrementCounter5);	preparedStmt.setDouble(8, incrementCounter6); 		preparedStmt.setDouble(9, incrementCounter7);	
			preparedStmt.setDouble(10, incrementCounter8);	preparedStmt.setDouble(11, incrementCounter9);
			preparedStmt.setDouble(12, incrementCounter10);	preparedStmt.setDouble(13, incrementCounter11); 	
			preparedStmt.setDouble(14, incrementCounter12);	//preparedStmt.setDouble(15, incrementCounter13);	
			
			
			
			preparedStmt.setInt(15, top5);			preparedStmt.setInt(16, top10); 		preparedStmt.setInt(17, top15);			preparedStmt.setInt(18, top30);
			preparedStmt.setInt(19, top50);			preparedStmt.setInt(20, top100); 	preparedStmt.setInt(21, outsideTop100);	preparedStmt.setInt(22, notMatched);
			preparedStmt.setDouble(23, totalProb); //CHANGE WITH TOTALPROBABILITY		
			preparedStmt.setString(24, evalLevel);
			// execute the java preparedstatement
			preparedStmt.executeUpdate();	
		//	if(outputForDebug)
				System.out.println("\tInserted Evaluation values for Result record id:");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public float getPreAtTop5() {		return sumPreAtTop5;	}
	public void setPreAtTop5(float preAtTop5) {		this.sumPreAtTop5 = preAtTop5;	}
	public float getPreAtTop10() {		return sumPreAtTop10;	}
	public void setPreAtTop10(float preAtTop10) {		this.sumPreAtTop10 = preAtTop10;	}
	public float getPreAtTop15() {		return sumPreAtTop15;	}
	public void setPreAtTop15(float preAtTop15) {		this.sumPreAtTop15 = preAtTop15;	}
	public float getPreAtTop30() {		return sumPreAtTop30;	}
	public void setPreAtTop30(float preAtTop30) {		this.sumPreAtTop30 = preAtTop30;	}
	public float getPreAtTop50() {		return sumPreAtTop50;	}
	public void setPreAtTop50(float preAtTop50) {		this.sumPreAtTop50 = preAtTop50;	}
	public float getPreAtTop100() {		return sumPreAtTop100;	}
	public void setPreAtTop100(float preAtTop100) {		this.sumPreAtTop100 = preAtTop100;	}
	public float getPreOutsideTop100() {		return sumPreOutsideTop100;	}
	public void setPreOutsideTop100(float preOutsideTop100) {		this.sumPreOutsideTop100 = preOutsideTop100;	}
	public float getRecAtTop5() {		return sumRecAtTop5;	}
	public void setRecAtTop5(float recAtTop5) {		this.sumRecAtTop5 = recAtTop5;	}
	public float getRecAtTop10() {		return sumRecAtTop10;	}
	public void setRecAtTop10(float recAtTop10) {		this.sumRecAtTop10 = recAtTop10;	}
	public float getRecAtTop15() {		return sumRecAtTop15;	}
	public void setRecAtTop15(float recAtTop15) {		this.sumRecAtTop15 = recAtTop15;	}
	public float getRecAtTop30() {		return sumRecAtTop30;	}
	public void setRecAtTop30(float recAtTop30) {		this.sumRecAtTop30 = recAtTop30;	}
	public float getRecAtTop50() {		return sumRecAtTop50;	}
	public void setRecAtTop50(float recAtTop50) {		this.sumRecAtTop50 = recAtTop50;	}
	public float getRecAtTop100() {		return sumRecAtTop100;	}
	public void setRecAtTop100(float recAtTop100) {		this.sumRecAtTop100 = recAtTop100;	}
	public float getRecOutsideTop100() {		return sumRecOutsideTop100;	}
	public void setRecOutsideTop100(float recOutsideTop100) {		this.sumRecOutsideTop100 = recOutsideTop100;	}
	public float getDcgAtTop5() {		return dcgAtTop5;	}
	public void setDcgAtTop5(float dcgAtTop5) {		this.dcgAtTop5 = dcgAtTop5;	}
	public float getDcgAtTop10() {		return dcgAtTop10;	}
	public void setDcgAtTop10(float dcgAtTop10) {		this.dcgAtTop10 = dcgAtTop10;	}
	public float getDcgAtTop15() {		return dcgAtTop15;	}
	public void setDcgAtTop15(float dcgAtTop15) {		this.dcgAtTop15 = dcgAtTop15;	}
	public float getDcgAtTop30() {		return dcgAtTop30;	}
	public void setDcgAtTop30(float dcgAtTop30) {		this.dcgAtTop30 = dcgAtTop30;	}
	public float getDcgAtTop50() {		return dcgAtTop50;	}
	public void setDcgAtTop50(float dcgAtTop50) {		this.dcgAtTop50 = dcgAtTop50;	}
	public float getDcgAtTop100() {		return dcgAtTop100;	}
	public void setDcgAtTop100(float dcgAtTop100) {		this.dcgAtTop100 = dcgAtTop100;	}
	public float getDcgOutsideTop100() {		return dcgOutsideTop100;	}
	public void setDcgOutsideTop100(float dcgOutsideTop100) {		this.dcgOutsideTop100 = dcgOutsideTop100;	}
	public float getNdcgAtTop5() {		return ndcgAtTop5;	}
	public void setNdcgAtTop5(float ndcgAtTop5) {		this.ndcgAtTop5 = ndcgAtTop5;	}
	public float getNdcgAtTop10() {		return ndcgAtTop10;	}
	public void setNdcgAtTop10(float ndcgAtTop10) {		this.ndcgAtTop10 = ndcgAtTop10;	}
	public float getNdcgAtTop15() {		return ndcgAtTop15;	}
	public void setNdcgAtTop15(float ndcgAtTop15) {		this.ndcgAtTop15 = ndcgAtTop15;	}
	public float getNdcgAtTop30() {		return ndcgAtTop30;	}
	public void setNdcgAtTop30(float ndcgAtTop30) {		this.ndcgAtTop30 = ndcgAtTop30;	}
	public float getNdcgAtTop50() {		return ndcgAtTop50;	}
	public void setNdcgAtTop50(float ndcgAtTop50) {		this.ndcgAtTop50 = ndcgAtTop50;	}
	public float getNdcgAtTop100() {		return ndcgAtTop100;	}
	public void setNdcgAtTop100(float ndcgAtTop100) {		this.ndcgAtTop100 = ndcgAtTop100;	}
	public float getNdcgOutsideTop100() {		return ndcgOutsideTop100;	}
	public void setNdcgOutsideTop100(float ndcgOutsideTop100) {		this.ndcgOutsideTop100 = ndcgOutsideTop100;	}
	public float getSum_dcg_ForAllProposals() {		return sum_dcg_ForAllProposals;	}
	public void setSum_dcg(float sum_dcg) {		this.sum_dcg_ForAllProposals = sum_dcg;	}
	public float getSum_ndcg_ForAllProposal() {		return sum_ndcg_ForAllProposals;	}
	public void setSum_ndcg_ForAllProposals(float sum_ndcg) {		this.sum_ndcg_ForAllProposals = sum_ndcg;	}
	public int getMatchedSent() {		return matchedSent;	}
	public void setMatchedSent(int matchedSent) {		this.matchedSent = matchedSent;	}
	public int getRank() {		return rank;	}
	public void setRank(int rank) {		this.rank = rank;	}
	public int getCounterRecallChanged() {		return counterRecallChanged;	}
	public void setCounterRecallChanged(int counterRecallChanged) {		this.counterRecallChanged = counterRecallChanged;	}
	
	public float getPreAtK() {		return preAtK;	}
	public void setPreAtK(float preAtK) {		this.preAtK = preAtK;	}
	public float getRecAtK() {		return recAtK;	}
	public void setRecAtK(float recAtK) {		this.recAtK = recAtK;	}
	public float getPrecision_ForAVGPreMean() {		return precision_ForAVGPreMean;	}
	public void setPrecision_ForAVGPreMean(float precision_ForAVGPreMean) {		this.precision_ForAVGPreMean = precision_ForAVGPreMean;	}
	public float getPreviousRecall() {		return previousRecall;	}
	public void setPreviousRecall(float previousPrecision) {		this.previousRecall = previousPrecision;	}
	public float getCurrentRecall_ForAVGPreMean() {		return currentRecall_ForAVGPreMean;	}
	public void setCurrentRecall_ForAVGPreMean(float currentRecall_ForAVGPreMean) {		this.currentRecall_ForAVGPreMean = currentRecall_ForAVGPreMean;	}
	public float getTotalOfPrecisions() {		return totalOfPrecisions;	}
	public void setTotalOfPrecisions(float totalOfPrecisions) {		this.totalOfPrecisions = totalOfPrecisions;	}
	
	public float getMaxRecall() {		return maxRecall;	}
	public void setMaxRecall(float maxRecall) {		this.maxRecall = maxRecall;	}
	public float getFinalAveragePrecisionForCurrProposal() {		return finalAveragePrecisionForCurrProposal;	}
	public void setFinalAveragePrecisionForCurrProposal(float finalAveragePrecision) {		this.finalAveragePrecisionForCurrProposal = finalAveragePrecision;	}
	public float getDcg() {		return dcg;	}
	public void setDcg(float dcg) {		this.dcg = dcg;	}
	public float getIdcg() {		return idcg;	}
	public void setIdcg(float idcg) {		this.idcg = idcg;	}
	public float getNdcg() {		return ndcg;	}
	public void setNdcg(float ndcg) {		this.ndcg = ndcg;	}
	public float getIdeal_dcg() {		return ideal_dcg;	}
	public void setIdeal_dcg(float ideal_dcg) {		this.ideal_dcg = ideal_dcg;	}
	public boolean isOutputOnce() {		return outputOnce;	}
	public void setOutputOnce(boolean outputOnce) {		this.outputOnce = outputOnce;	}
	public boolean isMatchedAtIndex() {		return matchedAtIndex;	}
	public void setMatchedAtIndex(boolean matchedAtIndex) {		this.matchedAtIndex = matchedAtIndex;	}
	public float getRelevanceValue() {		return relevanceValue;	}
	public void setRelevanceValue(float relevanceValue) {		this.relevanceValue = relevanceValue;	}
	public float getIrelevanceValue() {		return irelevanceValue;	}
	public void setIrelevanceValue(float irelevanceValue) {		this.irelevanceValue = irelevanceValue;	}
	public float getDeno() {		return deno;	}
	public void setDeno(float deno) {		this.deno = deno;	}
	public float getDeno2() {		return deno2;	}
	public void setDeno2(float deno2) {		this.deno2 = deno2;	}
	public Integer getpNum() {		return pNum;	}
	public void setpNum(Integer pNum) {		this.pNum = pNum;	}
	
	public  int getTop1() {		return top1;	}
	public  void setTop1(int top1) {		this.top1 = top1;	}	public  void incrementTop1() {		this.top1 = top1+1;	}
	public  int getTop2() {		return top2;	}
	public  void setTop2(int top2) {		this.top2 = top2;	}	public  void incrementTop2() {		this.top2 = top2+1;	}
	public  int getTop3() {		return top3;	}
	public  void setTop3(int top3) {		this.top3 = top3;	}	public  void incrementTop3() {		this.top3 = top3+1;	}
	public  int getTop4() {		return top4;	}
	public  void setTop4(int top4) {		this.top4 = top4;	}	public  void incrementTop4() {		this.top4 = top4+1;	}
	
	public  int getTop5() {		return top5;	}
	public  void setTop5(int top5) {		this.top5 = top5;	}	public  void incrementTop5() {		this.top5 = top5+1;	}
	public  int getTop10() {		return top10;	}
	public  void setTop10(int top10) {		this.top10 = top10;	}	public  void incrementTop10() {		this.top10 = top10+1;	}
	public  int getTop15() {		return top15;	}
	public  void setTop15(int top15) {		this.top15 = top15;	}	public  void incrementTop15() {		this.top15 = top15+1;	}
	public  int getTop30() {		return top30;	}
	public  void setTop30(int top30) {		this.top30 = top30;	}	public  void incrementTop30() {		this.top30 = top30+1;	}
	public  int getTop50() {		return top50;	}
	public  void setTop50(int top50) {		this.top50 = top50;	}	public  void incrementTop50() {		this.top50 = top50+1;	}
	public  int getTop100() {		return top100;	}
	public  void setTop100(int top100) {		this.top100 = top100;	}	public  void incrementTop100() {		this.top100 = top100+1;	}
	public  int getOutsideTop100() {		return outsideTop100;	}
	public  void setOutsideTop100(int outsideTop100) {		this.outsideTop100 = outsideTop100;	}	public  void incrementOutside100() {		this.top100 = top100+1;	}
	
	public  int getManualExtractedReasonSentenceCounterForAProposal() {		return manualExtractedReasonSentenceCounterForAProposal;	}
	public  void setManualExtractedReasonSentenceCounterForAProposal(			int manualExtractedReasonSentenceCounterForAProposal) {
		this.manualExtractedReasonSentenceCounterForAProposal = manualExtractedReasonSentenceCounterForAProposal;
	}
	public float getSumOfAllPrecisionsForAllProposalsForCurrState() {		return sumOfAllPrecisionsForAllProposalsForCurrState;	}
	public void setSumOfAllPrecisionsForAllProposalsForCurrState(float sumOfAllPrecisionsForAllProposalsForCurrState) {
		this.sumOfAllPrecisionsForAllProposalsForCurrState = sumOfAllPrecisionsForAllProposalsForCurrState;
	}
	//may 2020
	public Integer getSumOfAllProposalsForCurrState() {		return sumOfAllProposalsForCurrState;	}
	public void setSumOfAllProposalsForCurrState(Integer v_sumOfAllProposalsForCurrState) {		this.sumOfAllProposalsForCurrState = v_sumOfAllProposalsForCurrState;	}
}
