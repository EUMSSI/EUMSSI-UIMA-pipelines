package eu.eumssi.uima.pipeline.test;


import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectSingle;

import java.util.logging.Logger;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.dbpedia.spotlight.uima.types.DBpediaResource;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.upf.glicom.uima.ts.VerifiedDBpediaResource;
import edu.upf.glicom.uima.types.BabelfyResource;
import eu.eumssi.uima.reader.XmiMongoReader;
import eu.eumssi.uima.ts.SourceMeta;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class MongoReadTest
{
	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(MongoReadTest.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		String mongoUri = "mongodb://localhost"; // this is the default, so not needed
		
		
		CollectionReaderDescription reader = createReaderDescription(XmiMongoReader.class,
				XmiMongoReader.PARAM_MAXITEMS,10,
				XmiMongoReader.PARAM_MONGODB, mongoDb,
				XmiMongoReader.PARAM_MONGOCOLLECTION, mongoCollection,
				XmiMongoReader.PARAM_FIELDS, "meta.cas.text_nerl",
				XmiMongoReader.PARAM_QUERY,"{'meta.source.inLanguage':'en','processing.queues.text_nerl': 'processed'}",
				//XmiMongoReader.PARAM_QUERY,"{'meta.source.inLanguage':'en','processing.available_data':'text_nerl'}",
				XmiMongoReader.PARAM_LANG,"{'$literal':'en'}"
				);

				
		JCasIterable pipeline = new JCasIterable(
				reader
				);

		// Run and show results in console
		for (JCas jcas : pipeline) {
			SourceMeta meta = selectSingle(jcas, SourceMeta.class);
			System.out.println("\n\n=========\n\n" + meta.getDocumentId() + ": " + jcas.getDocumentText() + "\n");

			for (Token token : select(jcas, Token.class)) {
				System.out.printf("  %-16s %n", 
						token.getCoveredText());
			}
			
			System.out.printf("%n  -- DBpedia --%n");
			for (DBpediaResource resource : select(jcas, DBpediaResource.class)) {
				System.out.printf("  %-16s\t%-10s\t%-10s%n", 
						resource.getCoveredText(),
						resource.getUri(),
						resource.getTypes());
			}

			System.out.printf("%n  -- DBpedia (verified) --%n");
			for (DBpediaResource resource : select(jcas, VerifiedDBpediaResource.class)) {
				System.out.printf("  %-16s\t%-10s\t%-10s%n", 
						resource.getCoveredText(),
						resource.getUri(),
						resource.getTypes());
			}

			System.out.printf("%n  -- Babelfy --%n");
			for (BabelfyResource babel : select(jcas, BabelfyResource.class)) {
				System.out.printf("  %-16s\t%-10s\t%-10s%n", 
						babel.getCoveredText(),
						babel.getBabelNetURL(),
						babel.getDBpediaURL());
			}

			System.out.printf("%n  -- Stanford NER --%n");
			for (NamedEntity entity : select(jcas, NamedEntity.class)) {
				System.out.printf("  %-16s %-10s %n", 
						entity.getCoveredText(),
						entity.getValue());
			}
		}
	}


}
