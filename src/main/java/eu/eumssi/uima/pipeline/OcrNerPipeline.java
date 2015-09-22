package eu.eumssi.uima.pipeline;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.SpotlightAnnotator;
import org.dbpedia.spotlight.uima.types.DBpediaResource;
import org.dbpedia.spotlight.uima.types.TopDBpediaResource;

import com.iai.uima.analysis_component.KeyPhraseAnnotator;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import eu.eumssi.uima.consumer.NER2MongoConsumer;
import eu.eumssi.uima.consumer.OcrNerlConsumer;
import eu.eumssi.uima.reader.OcrReader;
import eu.eumssi.uima.ts.OcrSegment;
import eu.eumssi.uima.ts.SourceMeta;


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
		String mongoUri = "mongodb://localhost:1234";

		CollectionReaderDescription reader = createReaderDescription(OcrReader.class,
				OcrReader.PARAM_MAXITEMS,100,
				OcrReader.PARAM_MONGODB, mongoDb,
				OcrReader.PARAM_MONGOURI, mongoUri,
				OcrReader.PARAM_MONGOCOLLECTION, mongoCollection,
				OcrReader.PARAM_FIELDS, "processing.results.video_ocr",
//				OcrReader.PARAM_QUERY,"{'meta.source.inLanguage':'en',"
//						+ "'processing.available_data': 'video_ocr',"
//						+ "'processing.available_data': {'$ne': 'ocr-nerl'}}",
				OcrReader.PARAM_QUERY,"{'meta.source.inLanguage':'en','processing.available_data': 'video_ocr'}",
				OcrReader.PARAM_LANG,"{'$literal':'en'}",
				OcrReader.PARAM_ONLYBEST,false,
				OcrReader.PARAM_VERTICALLY_ALIGNED,false
				);

		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

		AnalysisEngineDescription dbpedia = createEngineDescription(SpotlightAnnotator.class,
				SpotlightAnnotator.PARAM_ENDPOINT, "http://localhost:2222/rest",
				SpotlightAnnotator.PARAM_CONFIDENCE, 0.6f,
				SpotlightAnnotator.PARAM_ALL_CANDIDATES, false);

		AnalysisEngineDescription mongoWriter = createEngineDescription(OcrNerlConsumer.class,
				OcrNerlConsumer.PARAM_MONGOURI, mongoUri,
				OcrNerlConsumer.PARAM_MONGODB, mongoDb,
				OcrNerlConsumer.PARAM_MONGOCOLLECTION, mongoCollection
				);

		logger.info("starting pipeline");
		SimplePipeline.runPipeline(reader, segmenter, dbpedia, mongoWriter);
	}


}
