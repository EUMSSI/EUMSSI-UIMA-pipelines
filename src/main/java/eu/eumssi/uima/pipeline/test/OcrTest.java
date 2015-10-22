package eu.eumssi.uima.pipeline.test;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.SpotlightAnnotator;
import org.dbpedia.spotlight.uima.types.DBpediaResource;
import org.dbpedia.spotlight.uima.types.TopDBpediaResource;

import com.iai.uima.analysis_component.KeyPhraseAnnotator;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer;
import eu.eumssi.uima.consumer.OcrNerlConsumer;
import eu.eumssi.uima.reader.OcrReader;
import eu.eumssi.uima.ts.OcrSegment;
import eu.eumssi.uima.ts.SourceMeta;
import eu.eumssi.uima.ts.TopOcrSegment;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class OcrTest
{
	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(OcrTest.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		String mongoUri = "mongodb://localhost:1234"; // through ssh tunnel
		//String mongoUri = "mongodb://localhost:27017"; // default (local)

		CollectionReaderDescription reader = createReaderDescription(OcrReader.class,
				OcrReader.PARAM_MAXITEMS,10000,
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
				SpotlightAnnotator.PARAM_CONFIDENCE, 0.6f,
				SpotlightAnnotator.PARAM_ALL_CANDIDATES, false);

		AnalysisEngineDescription key = createEngineDescription(KeyPhraseAnnotator.class,
				KeyPhraseAnnotator.PARAM_LANGUAGE, "en",
				KeyPhraseAnnotator.PARAM_KEYPHRASE_RATIO, 10);

		AnalysisEngineDescription ner = createEngineDescription(StanfordNamedEntityRecognizer.class);

		AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "output",
				XmiWriter.PARAM_TYPE_SYSTEM_FILE, "output/TypeSystem.xml");

		AnalysisEngineDescription mongoWriter = createEngineDescription(OcrNerlConsumer.class,
				OcrNerlConsumer.PARAM_MONGOURI, mongoUri,
				OcrNerlConsumer.PARAM_MONGODB, mongoDb,
				OcrNerlConsumer.PARAM_MONGOCOLLECTION, mongoCollection
				);

		JCasIterable pipeline = new JCasIterable(
				reader,
				segmenter,
				dbpedia,
				//key,
				ner,
				xmiWriter,
				mongoWriter
				);

		// Run and show results in console
		for (JCas jcas : pipeline) {
			SourceMeta meta = selectSingle(jcas, SourceMeta.class);
			System.out.println("\n\n=========\n\n" + meta.getDocumentId() + ":\n" + jcas.getDocumentText() + "\n");
			for (OcrSegment ocrSegment : select(jcas, TopOcrSegment.class)) {
				logger.info(String.format("  %-16s\t%-16s\t%-10d\t%-10d\t%-10d\t%-10d\t\n", 
						ocrSegment.getCoveredText(),
						ocrSegment.getText(),
						ocrSegment.getBeginTime(),
						ocrSegment.getEndTime(),
						ocrSegment.getBegin(),
						ocrSegment.getEnd()
						));
				for (DBpediaResource resource : selectCovered(TopDBpediaResource.class, ocrSegment)) {
					logger.info(String.format("  %-16s\t%-10s\t%-10s%n", 
							resource.getCoveredText(),
							resource.getUri(),
							resource.getTypes()
							));
				}
				for (NamedEntity entity : selectCovered(jcas, NamedEntity.class, ocrSegment)) {
					logger.info(String.format("  %-16s %-10s %n", 
							entity.getCoveredText(),
							entity.getValue()));
				}
			}
			logger.info("=== everything ===");
			for (NamedEntity entity : select(jcas, NamedEntity.class)) {
				logger.info(String.format("  %-16s %-10s %n", 
						entity.getCoveredText(),
						entity.getValue()));
			}

		}
	}
}