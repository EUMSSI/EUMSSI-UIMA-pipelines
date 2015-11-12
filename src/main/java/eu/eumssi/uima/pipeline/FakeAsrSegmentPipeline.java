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

import eu.eumssi.uima.consumer.FakeAsrSegmentConsumer;
import eu.eumssi.uima.reader.AsrReader;
import eu.eumssi.uima.ts.AsrToken;
import eu.eumssi.uima.ts.SourceMeta;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class FakeAsrSegmentPipeline
{

	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(FakeAsrSegmentPipeline.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		//String mongoUri = "mongodb://localhost:1234";
		String mongoUri = "mongodb://localhost";
		String segmentMongoCollection ="segments";

		CollectionReaderDescription reader = createReaderDescription(AsrReader.class,
				AsrReader.PARAM_MAXITEMS,1000000,
				AsrReader.PARAM_MONGODB, mongoDb,
				AsrReader.PARAM_MONGOURI, mongoUri,
				AsrReader.PARAM_MONGOCOLLECTION, mongoCollection,
				AsrReader.PARAM_FIELDS, "processing.results.audio_transcript",
				AsrReader.PARAM_QUERY,"{'processing.available_data': 'audio_transcript'}",
				AsrReader.PARAM_LANG,"",
				AsrReader.PARAM_ONLYWORDS, true
				);

		AnalysisEngineDescription asrSegmentConsumer = createEngineDescription(FakeAsrSegmentConsumer.class,
				FakeAsrSegmentConsumer.PARAM_MONGOURI, mongoUri,
				FakeAsrSegmentConsumer.PARAM_MONGODB, mongoDb,
				FakeAsrSegmentConsumer.PARAM_MONGOCOLLECTION, mongoCollection,
				FakeAsrSegmentConsumer.PARAM_SEGMENT_MONGOCOLLECTION, segmentMongoCollection,
				FakeAsrSegmentConsumer.PARAM_QUEUE, "segments_fakeasr"
				);

		JCasIterable pipeline = new JCasIterable(
				reader,
				asrSegmentConsumer
				);

		// Run and show results in console
		for (JCas jcas : pipeline) {
			SourceMeta meta = selectSingle(jcas, SourceMeta.class);
			System.out.println("\n\n=========\n\n" + meta.getDocumentId() + ": " + jcas.getDocumentText() + "\n");
			
			for (AsrToken token : select(jcas, AsrToken.class)) {
				System.out.printf("  %-16s\t%-10d\t%-10d\t%-10s%n", 
						token.getCoveredText(),
						token.getBeginTime(),
						token.getEndTime(),
						token.getTokenType());
			}
		}
	}


}
