package eu.eumssi.uima.pipeline.test;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.dbpedia.spotlight.uima.SpotlightAnnotator;

import com.iai.uima.analysis_component.KeyPhraseAnnotator;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.mallet.topicmodel.MalletTopicModelEstimator;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import eu.eumssi.uima.mallet.lda.MalletTopicInferer;
import eu.eumssi.uima.pipeline.BasicNerPipeline;

public class CompletePipelineTest {
	
	static String lang = "en";
	static String source = "input/en";
	static String propsLocation = "input/pipeline.properties";
	static String endpoint = "http://spotlight.dbpedia.org/rest";
	static String target = "output";
	
	static boolean estimateModel = false;
	static float conf = .3f;
	static int ratio = 5;
	
	private static void parseArgs(String [] args){
		int i=0;
		while (i<args.length){
			String current = args[i];
			if (current.startsWith("-"))
				if (current.length()==2){
					switch (current.charAt(1)) {
						case 'l' : lang = args[++i]; break;
						case 'i' : source = args[++i]; break;
						case 'e' : endpoint = args[++i]; break;
						case 'o' : target = args[++i]; break;
						case 'm' : estimateModel = Boolean.valueOf(args[++i]); break;
						case 'c' : conf = Float.valueOf(args[++i]); break;
						case 'r' : ratio = Integer.valueOf(args[++i]); break;
						default : 	System.err.println("Unrecognized Option: "
														+current);
									System.exit(-1); break;
					}
				}
			else {
				System.err.println("Wrong Parameter: "+current);
				System.exit(-1);
			}
		i++;
		}
	}
	
	public static void estimateMalletModel() throws UIMAException, IOException {
		
		CollectionReaderDescription reader = createReaderDescription(TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION,source,
				TextReader.PARAM_PATTERNS,new String[] { "[+]*.txt" },
				TextReader.PARAM_LANGUAGE,lang);
		
		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class,
				LanguageToolSegmenter.PARAM_LANGUAGE,lang);
		
		AnalysisEngineDescription estimator = createEngineDescription(
                MalletTopicModelEstimator.class,
                MalletTopicModelEstimator.PARAM_N_THREADS, 2,
                MalletTopicModelEstimator.PARAM_TARGET_LOCATION, "input/models/mallet/"+lang+"/model.mallet",
                MalletTopicModelEstimator.PARAM_N_ITERATIONS, 100,
                MalletTopicModelEstimator.PARAM_N_TOPICS, 10,
                MalletTopicModelEstimator.PARAM_USE_LEMMA, true);
		
        SimplePipeline.runPipeline(reader, segmenter, estimator);
	}
	
	
	public static void main(String[] args) throws Exception
	{
		// Don't forget to set System Property KEA_HOME
		parseArgs(args);
		
		if (estimateModel){
			estimateMalletModel();
			System.exit(0);
		}
		
		Logger logger = Logger.getLogger(BasicNerPipeline.class.toString());
		
		CollectionReaderDescription reader = createReaderDescription(TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION,source,
				TextReader.PARAM_PATTERNS,new String[] { "[+]*.txt" },
				TextReader.PARAM_LANGUAGE,"en");
		
		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class,
				LanguageToolSegmenter.PARAM_LANGUAGE,"en");
		
		AnalysisEngineDescription mallet = createEngineDescription(MalletTopicInferer.class,
				MalletTopicInferer.PARAM_MODEL_LOCATION,"input/models/mallet/"+lang+"/model.mallet");
		
		// XXX Needs heavy re configuration in the descriptor file if you want to avoid copy pasting everything
		AnalysisEngineDescription jTextTile = createEngineDescription(
								"src/main/resources/desc/wst-snowball-C99-JTextTileAAE");
		
		AnalysisEngineDescription dbpedia = createEngineDescription(SpotlightAnnotator.class,
				//SpotlightAnnotator.PARAM_ENDPOINT, "http://localhost:2222/rest",
				//SpotlightAnnotator.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2222/rest",
				//SpotlightAnnotator.PARAM_ENDPOINT, "http://de.dbpedia.org/spotlight/rest",
				SpotlightAnnotator.PARAM_ENDPOINT,endpoint,
				SpotlightAnnotator.PARAM_CONFIDENCE, conf,
				SpotlightAnnotator.PARAM_ALL_CANDIDATES, true);

		AnalysisEngineDescription key = createEngineDescription(KeyPhraseAnnotator.class,
				KeyPhraseAnnotator.PARAM_LANGUAGE, "en",
				KeyPhraseAnnotator.PARAM_KEYPHRASE_RATIO, 10);

		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);

		AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "output",
				XmiWriter.PARAM_TYPE_SYSTEM_FILE, "output/TypeSystem.xml");

//		AnalysisEngineDescription mongoWriter = createEngineDescription(NER2MongoConsumer.class,
//				NER2MongoConsumer.PARAM_MONGODB, mongoDb,
//				NER2MongoConsumer.PARAM_MONGOCOLLECTION, mongoCollection
//				);

		logger.info("starting pipeline");
		SimplePipeline.runPipeline(reader, segmenter, jTextTile, mallet, key, ner, xmiWriter);
	}
}
