package eu.eumssi.uima.pipeline;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;

import eu.eumssi.uima.consumer.OcrSegmentConsumer;
import eu.eumssi.uima.reader.OcrReader;
import eu.eumssi.uima.ts.AsrToken;
import eu.eumssi.uima.ts.OcrSegment;
import eu.eumssi.uima.ts.SourceMeta;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class OcrSegmentPipeline
{

	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(OcrSegmentPipeline.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		String mongoUri = "mongodb://localhost:1234";
		//String mongoUri = "mongodb://localhost";
		String segmentMongoCollection ="segments";

		CollectionReaderDescription reader = createReaderDescription(OcrReader.class,
				OcrReader.PARAM_MAXITEMS,1000000,
				OcrReader.PARAM_MONGODB, mongoDb,
				OcrReader.PARAM_MONGOURI, mongoUri,
				OcrReader.PARAM_MONGOCOLLECTION, mongoCollection,
				OcrReader.PARAM_FIELDS, "processing.results.video_ocr",
				OcrReader.PARAM_QUERY,"{'processing.available_data': 'video_ocr'}",
				OcrReader.PARAM_LANG,"{'$literal':'en'}",
				OcrReader.PARAM_ONLYBEST, false,
				OcrReader.PARAM_VERTICALLY_ALIGNED, true
				);

		AnalysisEngineDescription ocrSegmentConsumer = createEngineDescription(OcrSegmentConsumer.class,
				OcrSegmentConsumer.PARAM_MONGOURI, mongoUri,
				OcrSegmentConsumer.PARAM_MONGODB, mongoDb,
				OcrSegmentConsumer.PARAM_MONGOCOLLECTION, mongoCollection,
				OcrSegmentConsumer.PARAM_SEGMENT_MONGOCOLLECTION, segmentMongoCollection,
				OcrSegmentConsumer.PARAM_QUEUE, "segments_ocr"
				);

		JCasIterable pipeline = new JCasIterable(
				reader,
				ocrSegmentConsumer
				);

		// Run and show results in console
		for (JCas jcas : pipeline) {
			SourceMeta meta = selectSingle(jcas, SourceMeta.class);
			System.out.println("\n\n=========\n\n" + meta.getDocumentId() + ": " + jcas.getDocumentText() + "\n");
			
//			for (OcrSegment token : select(jcas, OcrSegment.class)) {
//				System.out.printf("  %-16s\t%-10d\t%-10d%n", 
//						token.getText(),
//						token.getBeginTime(),
//						token.getEndTime());
//			}
		}
	}


}
