package eu.eumssi.uima.pipeline;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.dbpedia.spotlight.uima.SpotlightAnnotator;

import com.iai.uima.analysis_component.KeyPhraseAnnotator;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import edu.upf.glicom.uima.ae.ConfirmLinkAnnotatorTweet;
import eu.eumssi.uima.consumer.NER2MongoConsumer;
import eu.eumssi.uima.reader.BaseCasReader;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class TwitterNerPipeline {

	public static void main(String[] args) throws Exception {

		Logger logger = Logger.getLogger(TwitterNerPipeline.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "tweets";
		String mongoUri = "mongodb://localhost:1234"; // through ssh tunnel
		//String mongoUri = "mongodb://localhost:27017"; // default (local)

		CollectionReaderDescription reader = createReaderDescription(BaseCasReader.class,
				BaseCasReader.PARAM_MAXITEMS, 10000000,
				BaseCasReader.PARAM_MONGOURI, mongoUri,
				BaseCasReader.PARAM_MONGODB, mongoDb,
				BaseCasReader.PARAM_MONGOCOLLECTION, mongoCollection,
				BaseCasReader.PARAM_FIELDS, "meta.source.headline,meta.source.title,meta.source.description,meta.source.text",
				BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
						+ "'processing.available_data': {'$ne': 'ner'}}",
				//BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en'}", // reprocess everything
				BaseCasReader.PARAM_LANG,"{'$literal':'en'}"
				);

		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

		AnalysisEngineDescription dbpedia = createEngineDescription(SpotlightAnnotator.class,
				SpotlightAnnotator.PARAM_ENDPOINT, "http://localhost:2222/rest",
				//SpotlightAnnotator.PARAM_ENDPOINT, "http://spotlight.sztaki.hu:2222/rest",
				SpotlightAnnotator.PARAM_CONFIDENCE, 0.35f,
				SpotlightAnnotator.PARAM_ALL_CANDIDATES, true);

		AnalysisEngineDescription key = createEngineDescription(KeyPhraseAnnotator.class,
				KeyPhraseAnnotator.PARAM_LANGUAGE, "en",
				KeyPhraseAnnotator.PARAM_KEYPHRASE_RATIO, 10);

		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);

		AnalysisEngineDescription validate = createEngineDescription(ConfirmLinkAnnotatorTweet.class);

		AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "output",
				XmiWriter.PARAM_TYPE_SYSTEM_FILE, "output/TypeSystem.xml");

		AnalysisEngineDescription mongoWriter = createEngineDescription(NER2MongoConsumer.class,
				NER2MongoConsumer.PARAM_MONGOURI, mongoUri,
				NER2MongoConsumer.PARAM_MONGODB, mongoDb,
				NER2MongoConsumer.PARAM_MONGOCOLLECTION, mongoCollection
				);

		logger.info("starting pipeline");
		SimplePipeline.runPipeline(reader, segmenter, dbpedia, ner, validate, mongoWriter);
	}


}
