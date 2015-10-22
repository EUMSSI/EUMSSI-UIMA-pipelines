package eu.eumssi.uima.pipeline;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.dbpedia.spotlight.uima.SpotlightAnnotator;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import eu.eumssi.uima.consumer.OcrNerlConsumer;
import eu.eumssi.uima.reader.OcrReader;
import eu.eumssi.uima.ts.OcrSegment;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class OcrNerPipeline
{
	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(OcrNerPipeline.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		String mongoUri = "mongodb://localhost:1234"; // through ssh tunnel
		//String mongoUri = "mongodb://localhost:27017"; // default (local)

		CollectionReaderDescription reader = createReaderDescription(OcrReader.class,
				OcrReader.PARAM_MAXITEMS, 1000000,
				OcrReader.PARAM_MONGOURI, mongoUri,
				OcrReader.PARAM_MONGODB, mongoDb,
				OcrReader.PARAM_MONGOCOLLECTION, mongoCollection,
				OcrReader.PARAM_FIELDS, "processing.results.video_ocr",
//				OcrReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
//						+ "'processing.available_data': 'video_ocr',"
//						+ "'processing.available_data': {'$ne': 'ocr-nerl'}}",
				OcrReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
						+ "'processing.available_data': 'video_ocr'}", // reprocess everything
				OcrReader.PARAM_LANG,"{'$literal':'en'}",
				OcrReader.PARAM_ONLYBEST,false,
				OcrReader.PARAM_VERTICALLY_ALIGNED,false
				);

		// make sure sentences are limited to individual OCR hypotheses
		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class,
				LanguageToolSegmenter.PARAM_ZONE_TYPES, OcrSegment.class.getCanonicalName());

		AnalysisEngineDescription dbpedia = createEngineDescription(SpotlightAnnotator.class,
				SpotlightAnnotator.PARAM_ENDPOINT, "http://localhost:2222/rest",
				SpotlightAnnotator.PARAM_CONFIDENCE, 0.4f,
				SpotlightAnnotator.PARAM_ALL_CANDIDATES, false);

		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);

		AnalysisEngineDescription mongoWriter = createEngineDescription(OcrNerlConsumer.class,
				OcrNerlConsumer.PARAM_MONGOURI, mongoUri,
				OcrNerlConsumer.PARAM_MONGODB, mongoDb,
				OcrNerlConsumer.PARAM_MONGOCOLLECTION, mongoCollection
				);

		logger.info("starting pipeline");
		SimplePipeline.runPipeline(reader, segmenter, dbpedia, ner, mongoWriter);
	}


}
