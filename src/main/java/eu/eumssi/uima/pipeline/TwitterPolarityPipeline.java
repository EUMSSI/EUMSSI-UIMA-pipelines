package eu.eumssi.uima.pipeline;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import edu.upf.glicom.uima.opinion.DistanceBasedOpinionTargetExtractor;
import edu.upf.glicom.uima.opinion.OpinionExpressionAnnotator;
import eu.eumssi.uima.consumer.Polar2MongoConsumer;
import eu.eumssi.uima.reader.BaseCasReader;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class TwitterPolarityPipeline {

	public static void main(String[] args) throws Exception {

		Logger logger = Logger.getLogger(TwitterPolarityPipeline.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "tweets";
		//String mongoUri = "mongodb://localhost:1234"; // through ssh tunnel
		String mongoUri = "mongodb://localhost:27017"; // default (local)

		CollectionReaderDescription reader = createReaderDescription(BaseCasReader.class,
				BaseCasReader.PARAM_MAXITEMS, 10000000,
				BaseCasReader.PARAM_MONGOURI, mongoUri,
				BaseCasReader.PARAM_MONGODB, mongoDb,
				BaseCasReader.PARAM_MONGOCOLLECTION, mongoCollection,
				BaseCasReader.PARAM_FIELDS, "meta.source.headline,meta.source.title,meta.source.description,meta.source.text",
				//BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
				//		+ "'processing.available_data': {'$ne': 'text_polarity'}}",
				BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en'}", // reprocess everything
				BaseCasReader.PARAM_LANG,"{'$literal':'en'}"
				);

		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

		AnalysisEngineDescription posTagger = createEngineDescription(OpenNlpPosTagger.class);

		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);

		AnalysisEngineDescription opinion = createEngineDescription(OpinionExpressionAnnotator.class,
				OpinionExpressionAnnotator.PARAM_POLAR_DICT_FILE, "edu/upf/glicom/dict/EN/compiled/dictMiniPolar.dic",
				OpinionExpressionAnnotator.PARAM_POLAR_DICT_TYPE, "lemma",
				OpinionExpressionAnnotator.PARAM_QUANTNEG_DICT_FILE, "edu/upf/glicom/dict/EN/compiled/QuantNeg_EN_v3.dic"
				);

		AnalysisEngineDescription targetExtractor = createEngineDescription(DistanceBasedOpinionTargetExtractor.class);

		AnalysisEngineDescription polarWriter = createEngineDescription(Polar2MongoConsumer.class,
				Polar2MongoConsumer.PARAM_MONGOURI, mongoUri,
				Polar2MongoConsumer.PARAM_MONGODB, mongoDb,
				Polar2MongoConsumer.PARAM_MONGOCOLLECTION, mongoCollection,
				Polar2MongoConsumer.PARAM_QUEUE, "text_polarity"
				);

		logger.info("starting twitter pipeline");
		SimplePipeline.runPipeline(
				reader, segmenter, lemmatizer, posTagger,
				opinion, targetExtractor, 
				polarWriter);
	}


}
