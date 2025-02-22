package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class TestPointRangeQuery {
    private Directory directory;

    {
        try {
            FileOperation.deleteFile("./data");
            directory = new MMapDirectory(Paths.get("./data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Analyzer analyzer = new WhitespaceAnalyzer();
    private IndexWriterConfig conf = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;

    public void doIndex() throws Exception {
        conf.setUseCompoundFile(false);
        SortField sortField = new SortedNumericSortField("content", SortField.Type.INT);
        Sort sort = new Sort(sortField);
        conf.setIndexSort(sort);
        indexWriter = new IndexWriter(directory, conf);

        Random random = new Random();
        Document doc;
        // 0
        doc = new Document();
        doc.add(new IntPoint("content", 1));
        doc.add(new SortedNumericDocValuesField("content", 2));
        doc.add(new StringField("content", "a", org.apache.lucene.document.Field.Store.YES));
        doc.add(new StringField("title", "文档0", org.apache.lucene.document.Field.Store.YES));
        indexWriter.addDocument(doc);
        // 1
        doc = new Document();
        doc.add(new IntPoint("content", 10));
        doc.add(new SortedNumericDocValuesField("content", 3));
        doc.add(new StringField("content", "a", org.apache.lucene.document.Field.Store.YES));
        doc.add(new StringField("title", "文档1", org.apache.lucene.document.Field.Store.YES));
        indexWriter.addDocument(doc);
        // 2
        doc = new Document();
        doc.add(new IntPoint("content", 10));
        doc.add(new SortedNumericDocValuesField("content", 1));
        doc.add(new StringField("content", "a", org.apache.lucene.document.Field.Store.YES));
        doc.add(new StringField("title", "文档2", org.apache.lucene.document.Field.Store.YES));
        indexWriter.addDocument(doc);


        doc = new Document();
//    doc.add(new IntPoint("title", 10, 55));
//        indexWriter.addDocument(doc);

//        int count = 0 ;
//        while (count++ < (2048 * 2)){
//            doc = new Document();
//            int a = random.nextInt(100);
//            a = a == 0 ? a + 1 : a;
//            int b = random.nextInt(100);
//            b = b == 0 ? b + 1 : b;
//            doc.add(new IntPoint("content", a));
//            doc.add(new SortedNumericDocValuesField("content", a));
//            indexWriter.addDocument(doc);
//        }

        indexWriter.commit();
        DirectoryReader r = DirectoryReader.open(indexWriter);
        IndexSearcher s = new IndexSearcher(r);

        int  lowValue = 3;
        int  upValue = 25;
//
//    int [] lowValue = {-1, -1};
//    int [] upValue = {100, 100};

//        int num;
//        num = s.count(IntField.newRangeQuery("content", lowValue, upValue));
//        System.out.println("result number : "+ num +"");
        Query query = new TermQuery(new Term("content", "a"));
        TopFieldDocs docs = s.search(query, 10, sort);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            System.out.println(scoreDoc.doc);
            String titleValue = r.document(scoreDoc.doc).getField("title").toString();
            System.out.println(titleValue);
        }
        TopDocs topDocs = s.search(query, 10);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            System.out.println(scoreDoc.doc);
            String titleValue = r.document(scoreDoc.doc).getField("title").toString();
            System.out.println(titleValue);
        }


        // Per-top-reader state:k
    }



    public static void main(String[] args) throws Exception{
        TestPointRangeQuery query = new TestPointRangeQuery();
        query.doIndex();
    }
}
