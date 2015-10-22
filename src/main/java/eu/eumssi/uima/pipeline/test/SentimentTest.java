package eu.eumssi.uima.pipeline.test;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import edu.upf.glicom.uima.opinion.DistanceBasedOpinionTargetExtractor;
import edu.upf.glicom.uima.opinion.OpinionExpressionAnnotator;
import edu.upf.glicom.uima.ts.opinion.OpinionExpression;


/**
 * In this pipeline, we use dbpedia-spotlight to annotate entities.
 * It is configured to use the public endpoint, but should preferably point to a local one.
 */
public class SentimentTest
{
	public static void main(String[] args) throws Exception
	{

		Logger logger = Logger.getLogger(SentimentTest.class.toString());

		String mongoDb = "eumssi_db";
		String mongoCollection = "content_items";
		String mongoUri = "mongodb://localhost"; // this is the default, so not needed
		
		CollectionReaderDescription reader = createReaderDescription(TextReader.class,
                TextReader.PARAM_SOURCE_LOCATION, "input/en/*", 
                TextReader.PARAM_LANGUAGE, "en");
		
//		CollectionReaderDescription reader = createReaderDescription(BaseCasReader.class,
//				BaseCasReader.PARAM_MAXITEMS,10,
//				BaseCasReader.PARAM_MONGODB, mongoDb,
//				BaseCasReader.PARAM_MONGOCOLLECTION, mongoCollection,
//				BaseCasReader.PARAM_FIELDS, "meta.source.headline,meta.source.title,meta.source.description,meta.source.text",
//				//BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en','processing.available_data': {'$ne': 'ner'}}",
//				BaseCasReader.PARAM_QUERY,"{'meta.source.inLanguage':'en'}",
//				BaseCasReader.PARAM_LANG,"{'$literal':'en'}"
//				);

		AnalysisEngineDescription segmenter = createEngineDescription(LanguageToolSegmenter.class);

		AnalysisEngineDescription posTagger = createEngineDescription(OpenNlpPosTagger.class);

		AnalysisEngineDescription lemmatizer = createEngineDescription(StanfordLemmatizer.class);

		AnalysisEngineDescription opinion = createEngineDescription(OpinionExpressionAnnotator.class,
				OpinionExpressionAnnotator.PARAM_POLAR_DICT_FILE, "edu/upf/glicom/dict/EN/compiled/dictOF.dic",
				OpinionExpressionAnnotator.PARAM_POLAR_DICT_TYPE, "lemma",
				OpinionExpressionAnnotator.PARAM_QUANTNEG_DICT_FILE, "edu/upf/glicom/dict/EN/compiled/QuantNeg_EN_v3.dic"
				);

		AnalysisEngineDescription targetExtractor = createEngineDescription(DistanceBasedOpinionTargetExtractor.class);


		AnalysisEngineDescription xmiWriter = createEngineDescription(XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "output",
				XmiWriter.PARAM_TYPE_SYSTEM_FILE, "output/TypeSystem.xml");

		JCasIterable pipeline = new JCasIterable(
				reader,
				segmenter,
				posTagger,
				lemmatizer,
				opinion,
				targetExtractor,
				xmiWriter
				);

		// Run and show results in console
		for (JCas jcas : pipeline) {
			//SourceMeta meta = selectSingle(jcas, SourceMeta.class);
			//System.out.println("\n\n=========\n\n" + meta.getDocumentId() + ": " + jcas.getDocumentText() + "\n");

			for (Token token : select(jcas, Token.class)) {
				System.out.printf("  %-16s %n", 
						token.getCoveredText());
			}

			for (OpinionExpression oe: select(jcas, OpinionExpression.class)) {
				String target = "";
				if (oe.getTarget() != null) {
					target = oe.getTarget().getCoveredText();
				}
				System.out.printf("  %-16s %-10s %-16s %n", 
						oe.getCoveredText(),
						oe.getPolarity(),
						target
						);
			}
		}
	}


}
