package foo.annotators;

import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.text.html.HTMLDocument.Iterator;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import foo.typesystems.Document;
import foo.typesystems.Token;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 * function: add FSList<Token> to doc.
	 * Given a document annotation contains text information,
	 * create a Token for each distinct word in text ( case insensitive, all converted to lowercase)
	 * Add tokens to FSList and use doc.setTokenList(fslist) to update doc annotation
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		String[] wordList = docText.split(" ");
		HashSet<String> rec = new HashSet<String>();
		
		//TO DO: construct a vector of tokens and update the tokenList in CAS
    ArrayList<Token> tList = new  ArrayList<Token>();
		int count=0;
		for (int i=0; i<wordList.length; i++) {
		  if (rec.add(wordList[i])) {
		    count = 1;
		    for (int j=i+1; j<wordList.length; j++) {
		      if (wordList[i].equalsIgnoreCase(wordList[j])) {
		        count++;
		      }
		    }
		    Token wordfreq = new Token(jcas);
		    wordfreq.setText(wordList[i].toLowerCase());
		    wordfreq.setFrequency(count);
		    tList.add((Token) wordfreq.clone());	      		    
		  }
		}
		
		//generate FSList from arraylist
    NonEmptyFSList head = new NonEmptyFSList(jcas);
    NonEmptyFSList list = head;
    
    for (int i=0; i<tList.size(); i++) {
		  head.setHead(tList.get(i));
		  
      if (i!=tList.size()-1) {
        head.setTail(new NonEmptyFSList(jcas));
        head = (NonEmptyFSList) head.getTail();
      } else { 
        head.setTail(new EmptyFSList(jcas));
      }
		}		
    
    //add the fslist of token  to doc's tokenlist.
		doc.setTokenList(list);
	}

}
