package foo.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import foo.typesystems.Document;
import foo.typesystems.Token;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

	/** query text string **/
  public ArrayList<String> docTextList;
	
	/** a global word dictionary **/
	public HashSet<String> library;

	/** the string-frequency map for each query **/
	public ArrayList<Map<String, Integer>> sentPattern;
		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();
		relList = new ArrayList<Integer>();
		docTextList = new ArrayList<String>();
		library = new HashSet<String>();
		sentPattern = new ArrayList<Map<String, Integer>>();
		
	}

	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 * 1. Construct a library containing every word in the documents, kept in HashSet library.
	 * 2. Generate a HashMap<String, Integer> for each query from doc's tokenlist
	 *    kept all sentences' HashMap in an ArrayList of HashMaps named sentPattern
	 * 3. Other information like queryId, relevanceValue, text are all kept in the public
	 * ArrayList variable: qIdList, relList, docTextList
	 * 
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}
		
		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
		
		if (it.hasNext()) {
			Document doc = (Document) it.next();
			
			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			
			//Do something useful here	 //convert tokenlist to map		
      int i=0;
      Token tmp;     
      Map<String, Integer>  sent = new HashMap<String, Integer>();
			while (true) {        
        try {
            tmp= (Token) fsTokenList.getNthElement(i);
        } catch (Exception e) { break; }
        
        sent.put(tmp.getText(), tmp.getFrequency());
        library.add(tmp.getText());
        i++;
      }
			sentPattern.add(sent);
			docTextList.add(doc.getText());
			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
		}
		
	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 * 
	 * @param qIdList
	 * @param relList
	 * @param docTextList
	 * @param sentPattern
	 * @param library
	 *  
	 * The function did the following things:
	 * 1. calculate the cosine-similarity score between all the retrievals and corresponding queries
	 *   The score for query is set to 0.0 for convenience
	 * 2. rank the retrieval with the same qId and use the rank of correct retrieval to calculate MRR
	 * 3. for all the correct retrieval, print their information to stout
	 * 4. print the MRR for the whole document to stout
	 * 
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {
	  
		super.collectionProcessComplete(arg0);
		
		// before use the hashmap for each sentence, compensate each with the words in library
		for (int i=0; i<sentPattern.size(); i++) {
		  HashMap<String, Integer> tmp = (HashMap<String, Integer>) sentPattern.get(i);
		  for (String key:library) {
  		  if (!tmp.containsKey(key)) {
  		    tmp.put(key,0);
  		  }
  		}
	  }
		
		// TODO :: compute the cosine similarity measure
		int sentnum = qIdList.size(), set=0;
		ArrayList<Double> score = new ArrayList<Double>(sentnum);
		HashMap<String, Integer> query = null, docVec = null;
		for (int i=0; i<sentnum; i++){
		  if (relList.get(i)==99) {
		    query = (HashMap<String, Integer>) sentPattern.get(i);
		    set = qIdList.get(i);
		    score.add(0.0);
		  } else {
		    if (qIdList.get(i)==set) {
		      docVec = (HashMap<String, Integer>) sentPattern.get(i);
		      score.add(computeCosineSimilarity(query, docVec));
		    }
		  }
		}

		// TODO :: compute the rank of retrieved sentences
		ArrayList<Integer> rank = new ArrayList<Integer>();
		ArrayList<Integer> index = new ArrayList<Integer>(); //stores the index for all the relList==1 entry
		double markscore=0.0;
		ArrayList<Double> scoreInQuery = null;
		for (int i=0; i<sentnum+1; i++) {
  		  if (i==sentnum || relList.get(i)==99  ) {
  		    if (scoreInQuery!=null) {
  		      Collections.sort(scoreInQuery);
  		      for (int j=scoreInQuery.size()-1; j>=0; j--) {
  		        if (scoreInQuery.get(j)==markscore) {rank.add(scoreInQuery.size()-j); break;}
  		      }
  		    } 
  		    scoreInQuery = new ArrayList<Double>(); 
  		  } else {
  		    if (relList.get(i)==1) { 
  		      markscore = score.get(i);
  		      index.add(i);
  		    }
  		    scoreInQuery.add(score.get(i));
  		  }
		}

		/*
		//add the final rank
		Collections.sort(scoreInQuery);
    for (int j=0; j<scoreInQuery.size(); j++) {
      if (scoreInQuery.get(j)==markscore) {rank.add(scoreInQuery.size()-j);}
    }
*/
    //printout the result
    for (int i=0; i<rank.size(); i++) {
      int ind = index.get(i);
      System.out.format("Score:%f rank=%d  rel=%d qId=%d  %s\n",score.get(ind), rank.get(i), relList.get(ind), qIdList.get(ind), docTextList.get(ind));
    }
    
		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr(rank);
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * @param queryVector     a map<String, Integer> represent the word frequency vector for query
	 * @param docVector       a map<String, Integer> represent the word frequency vector for one of the retrieval results
	 * 
	 * The method calculate the cosine similarity between queryVector and docVector
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;
		
		// TODO :: compute cosine similarity between two sentences
		//COSINE SIMILARITY ****************************************
		
		int querySq=0, docSq=0, cross=0, Qval, Dval; 
		for (String key:queryVector.keySet()){
		  Qval=queryVector.get(key);
		  Dval=docVector.get(key);
		  cross+=Qval*Dval;
		  querySq+=Qval^2;
		  docSq+=Dval^2;
		}
		
		cosine_similarity=cross/Math.sqrt(querySq+docSq);
		return cosine_similarity;
		
		
		//Jaccard & DICE SIMILARITY ***********************************
		/*
    int Qval, Dval;
    int p=0, q=0, d=0;
    for (String key:queryVector.keySet()){
      Qval=queryVector.get(key);
      Dval=docVector.get(key);
      if (Qval>0 && Dval>0) {p++;}
      if (Qval>0 || Dval>0) {
        if (Qval>0) {q++;}
        if (Dval>0) {d++;}
      }
    }
    p=2*p; //apply when choose the dice similarity 
    cosine_similarity=(double) p/(double) (p+d+q);
    return cosine_similarity;
    */
		
	}

	/**
	 * @param rank         a list of ranks
	 * 
	 * The method calculates mrr given a list of rank. 
	 * For example, the rank list is [1,1,2], mrr = (1/1+1/1+1/2)/3=5/6
	 * @return mrr
	 * 
	 */
	private double compute_mrr(ArrayList<Integer> rank) {
		double metric_mrr=0.0;
		
		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
    for (int i=0; i<rank.size(); i++) {
      metric_mrr+=1.0/(double)(rank.get(i));
    }
    metric_mrr/=(double)rank.size();
		
		return metric_mrr;
	}

}
