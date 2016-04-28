package eu.eumssi.uima.pipeline.test;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;

import com.iai.uima.analysis_component.KeyPhraseAnnotator;
import com.iai.uima.analysis_component.OfficeHolderAnnotator;
import com.iai.uima.analysis_component.QuoteAnnotator;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.mallet.topicmodel.MalletTopicModelEstimator;
import de.tudarmstadt.ukp.dkpro.core.matetools.MateLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.matetools.MatePosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.stopwordremover.StopWordRemover;
import eu.eumssi.uima.mallet.lda.MalletTopicInferer;
import eu.eumssi.uima.pipeline.BasicNerPipeline;
import eu.eumssi.uima.reader.BaseCasReader;

public class CompletePipelineTest {
	
	static String lang = "en";
	static String source = "input/en";
//	static String source = "W:\\susanne\\EUMSSI\\quote_finder\\text";
//	static String source = "W:\\susanne\\EUMSSI\\quote_finder\\text\\coreference";
//	static String source = "W:\\susanne\\ASR\\text_small";
	static String propsLocation = "input/pipeline.properties";
	static String endpoint = "http://spotlight.dbpedia.org/rest";
	static String target = "output";
	
	static String mongoDb = "eumssi_db";
	static String mongoCollection = "content_items  ";
	//String mongoUri = "mongodb://localhost:1234"; // through ssh tunnel
	static String mongoUri = "mongodb://localhost:27017"; // default (local)
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
						case 'm' : estimateModel = true; i++; break;
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
		
		AnalysisEngineDescription swr = createEngineDescription(StopWordRemover.class,
				StopWordRemover.PARAM_MODEL_LOCATION,"input/mallet/stopwords/en.txt");
		
		AnalysisEngineDescription estimator = createEngineDescription(
                MalletTopicModelEstimator.class,
                MalletTopicModelEstimator.PARAM_N_THREADS, 4,
                MalletTopicModelEstimator.PARAM_TARGET_LOCATION, "input/models/mallet/"+lang+"/model",
                MalletTopicModelEstimator.PARAM_N_ITERATIONS, 100,
                MalletTopicModelEstimator.PARAM_N_TOPICS, 13,
                MalletTopicModelEstimator.PARAM_DISPLAY_N_TOPIC_WORDS, 50,
                MalletTopicModelEstimator.PARAM_USE_LEMMA, false);
		
        SimplePipeline.runPipeline(reader, segmenter, swr ,estimator);
	}
	
	public static void main(String[] args) throws Exception
	{
		// Don't forget to set System Property HEIDELTIME_HOME
		if (System.getProperty("HEIDELTIME_HOME")==null)
			System.setProperty("HEIDELTIME_HOME", "../heideltime/");
		// Don't forget to set System Property KEA_HOME
		if (System.getProperty("KEA_HOME")==null)
			System.setProperty("KEA_HOME","../KEA/");
		
		parseArgs(args);
		
		if (estimateModel){
			estimateMalletModel();
			System.exit(0);
		}
		
		Logger logger = Logger.getLogger(BasicNerPipeline.class.toString());
		
//		CollectionReaderDescription reader = createReaderDescription(BaseCasReader.class,
//				BaseCasReader.PARAM_MAXITEMS, 10,
//				BaseCasReader.PARAM_MONGOURI, mongoUri,
//				BaseCasReader.PARAM_MONGODB, mongoDb,
//				BaseCasReader.PARAM_MONGOCOLLECTION, mongoCollection,
//				BaseCasReader.PARAM_FIELDS, "meta.source.headline,meta.source.title,meta.source.description,meta.source.text",
//				BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
//						+ "'processing.available_data': {'$ne': 'text_nerl'}}",
//				//BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en'}", // reprocess everything
//				BaseCasReader.PARAM_LANG,"{'$literal':'en'}"
//				);
		
		CollectionReaderDescription reader = createReaderDescription(TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION,source,
				TextReader.PARAM_PATTERNS,new String[] { "[+]*.txt" },
				TextReader.PARAM_LANGUAGE,"en");
		
		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class,
				LanguageToolSegmenter.PARAM_LANGUAGE,"en");
		
//		AnalysisEngineDescription mallet = createEngineDescription(MalletTopicInferer.class,
//				MalletTopicInferer.PARAM_MODEL_LOCATION,"input/models/mallet/"+lang+"/model");
		
// XXX Needs heavy re configuration in the descriptor file if you want to avoid copy pasting everything
//		AnalysisEngineDescription jTextTile = createEngineDescription(
//								"src/main/resources/desc/wst-snowball-C99-JTextTileAAE");
		
		AnalysisEngineDescription lemma = createEngineDescription(MateLemmatizer.class);
		
		AnalysisEngineDescription pos = createEngineDescription(MatePosTagger.class,
				MatePosTagger.PARAM_LANGUAGE,"en");
		
//		AnalysisEngineDescription pos1 = createEngineDescription(TreeTaggerWrapper.class,
//				TreeTaggerWrapper.PARAM_LANGUAGE,"english",
//				TreeTaggerWrapper.PARAM_ANNOTATE_PARTOFSPEECH,true,
//				TreeTaggerWrapper.PARAM_IMPROVE_GERMAN_SENTENCES,true,
//				TreeTaggerWrapper.PARAM_ANNOTATE_TOKENS,true,
//				TreeTaggerWrapper.PARAM_ANNOTATE_SENTENCES,true);
		
// XXX This SHOULD work and replace the second pos tagger, but sadly it does not work for some reason
//		AnalysisEngineDescription trans = createEngineDescription(AnnotationTranslator.class,
//				AnnotationTranslator.PARAM_DKPRO_TO_HEIDELTIME, true,
//				AnnotationTranslator.PARAM_HEIDELTIME_TO_DKPRO, true,
//				AnnotationTranslator.PARAM_IMPROVE_SENTENCE_DE, false);
		
//		AnalysisEngineDescription heidel = createEngineDescription(HeidelTime.class,
//				"Language","english",
//				"Date",true,
//				"Time",true,
//				"Duration",true,
//				"Set",true,
//				"ConvertDurations",true,
//				"Type","narratives");
		
// XXX May not be stable
//		AnalysisEngineDescription inteval = createEngineDescription(IntervalTagger.class,
//				IntervalTagger.PARAM_LANGUAGE,"english");
		
//		AnalysisEngineDescription dbpedia = createEngineDescription(SpotlightAnnotator.class,
//				//SpotlightAnnotator.PARAM_ENDPOINT, "http://localhost:2222/rest",
//				//SpotlightAnnotator.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2222/rest",
//				//SpotlightAnnotator.PARAM_ENDPOINT, "http://de.dbpedia.org/spotlight/rest",
//				SpotlightAnnotator.PARAM_ENDPOINT,endpoint,
//				SpotlightAnnotator.PARAM_CONFIDENCE, conf,
//				SpotlightAnnotator.PARAM_ALL_CANDIDATES, true);

		AnalysisEngineDescription key = createEngineDescription(KeyPhraseAnnotator.class,
				KeyPhraseAnnotator.PARAM_LANGUAGE, "en",
				KeyPhraseAnnotator.PARAM_KEYPHRASE_RATIO, 80
				//KeyPhraseAnnotator.PARAM_STOPWORDLIST,System.getProperty("KEA_HOME")+"/data/stopwords/stopwords_en.txt"
				);

		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class,
				StanfordNamedEntityRecognizer.PARAM_LANGUAGE,"en",
				StanfordNamedEntityRecognizer.PARAM_VARIANT,"all.3class.caseless.distsim.crf");
		
		AnalysisEngineDescription chunk = createEngineDescription(OpenNlpChunker.class,
				OpenNlpChunker.PARAM_LANGUAGE,"en");
		
		AnalysisEngineDescription officeholder = createEngineDescription(OfficeHolderAnnotator.class);
		
		AnalysisEngineDescription quote = createEngineDescription(QuoteAnnotator.class);
		
		AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "output",
				XmiWriter.PARAM_TYPE_SYSTEM_FILE, "output/TypeSystem.xml");
		
		logger.info("starting pipeline");
//		SimplePipeline.runPipeline(reader, segmenter, pos1, chunk, ner, key, xmiWriter);
//		SimplePipeline.runPipeline(reader, segmenter, lemma, pos1, chunk, ner, quote, xmiWriter); //quote Finder
//		SimplePipeline.runPipeline(reader, segmenter, lemma, pos1, chunk, quote, xmiWriter); //with chunk since chunk does not work in Stanford annotator???
//		SimplePipeline.runPipeline(reader, segmenter,quote, xmiWriter); //minimal without chunk
//		SimplePipeline.runPipeline(reader, segmenter, pos1, chunk, ner, officeholder, xmiWriter);
		
		// regular text pipeline (without dbpedia spotlight and heideltime)
		SimplePipeline.runPipeline(reader, segmenter, lemma, pos, chunk, ner, key, quote, xmiWriter); //complete regular text pipeline
	}
}
