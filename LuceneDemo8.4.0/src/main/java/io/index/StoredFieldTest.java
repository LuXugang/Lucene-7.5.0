package io.index;

import io.FileOperation;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Lu Xugang
 * @date 2020/10/12 10:14 上午
 */
public class StoredFieldTest {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    IndexWriter indexWriter;


    public void doSearch() throws Exception {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);

        conf.setUseCompoundFile(false);
        indexWriter = new IndexWriter(directory, conf);

        FieldType type = new FieldType();
        type.setStored(true);
//        type.setStoreTermVectors(true);
//        type.setStoreTermVectorPositions(true);
//        type.setStoreTermVectorPayloads(true);
//        type.setStoreTermVectorOffsets(true);
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        Document doc ;

        int count = 0;
        // 文档0
        doc = new Document();
        doc.add(new Field("content", "abc", type));
        doc.add(new Field("content", "cd", type));
        doc.add(new StringField("attachment", "cd", Field.Store.NO));
        doc.add(new StoredField("author", 3));
        indexWriter.addDocument(doc);
        // 文档1
        doc = new Document();
        doc.add(new Field("content", "abc", type));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new Field("content", "the name is name", type));
        indexWriter.addDocument(doc);
        indexWriter.commit();


        IndexReader reader = DirectoryReader.open(indexWriter);
        IndexSearcher searcher = new IndexSearcher(reader);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term("content", "abc")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "cd")), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(new Term("content", "name")), BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(2);

        ScoreDoc[]docs = searcher.search(builder.build(), 10).scoreDocs;
        for (ScoreDoc scoreDoc : docs) {
            System.out.println("docId: "+scoreDoc.doc+"");
        }
        Document document = reader.document(1);

        System.out.println("hah");
    }

    public static void main(String[] args) throws Exception{
        StoredFieldTest termVectorTest = new StoredFieldTest();
        termVectorTest.doSearch();
    }
}
