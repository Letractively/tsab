package ee.ioc.phon.tsab.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ee.ioc.phon.tsab.common.Constants;
import ee.ioc.phon.tsab.common.Search;
import ee.ioc.phon.tsab.common.Tools;
import ee.ioc.phon.tsab.common.TsabException;
import ee.ioc.phon.tsab.domain.Category;
import ee.ioc.phon.tsab.domain.Transcription;
import ee.ioc.phon.tsab.domain.TranscriptionFragment;
import ee.ioc.phon.tsab.domain.TranscriptionTopic;

public class TsabDao {

  private final static Logger log = Logger.getLogger(TsabDao.class);
  private static final int MAX_RECENTLY_ADDED = 5;
  private static final int MAX_MOST_POPULAR = 5;

  /**
   * Searches for the {@link Category} by given <code>title</code>
   * 
   * @param title
   * @return found category. <code>null</code> if not found.
   * @throws TsabException
   */
  public Category getCategoryByTitle(String title) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      List<Category> res = sess.createCriteria(Category.class).add(Restrictions.ilike("title", title)).list();

      if (res.size() > 1) {
        log.warn("Found more than one matching Category name for '" + title + "'. Returning first.");
      }

      Iterator<Category> it = res.iterator();
      try {
        return (Category) (it.hasNext() ? it.next() : null);
      } finally {
        tx.commit();
      }

    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find cateogry for title'" + title + "'", e);
      }
    }
  }

  public Transcription getTranscriptionById(Long id) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return (Transcription) sess.get(Transcription.class, id);
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcription for code '" + id + "'", e);
      }

    }
  }

  public List<TranscriptionFragment> getTranscriptionFragments(Transcription current) throws TsabException {

    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      try {
        return sess.createCriteria(TranscriptionFragment.class).add(Restrictions.eq("transcription", current))
            .addOrder(Order.asc("time")).list();
      } finally {
        tx.commit();
      }
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcription fragments (speech) for transcription '" + current + "'",
            e);
      }
    }

  }

  public List<TranscriptionTopic> getTranscriptionTopics(Transcription current) throws TsabException {

    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      try {
        return sess.createCriteria(TranscriptionTopic.class).add(Restrictions.eq("transcription", current))
            .addOrder(Order.asc("time")).list();
      } finally {
        tx.commit();
      }
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcription topics for transcription '" + current + "'",
            e);
      }
    }

  }

  public void deleteFragment(TranscriptionFragment speechLine) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      sess.delete(speechLine);
      tx.commit();
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to delete transcription fragment (speech) '" + speechLine + "'", e);
      }
    }
  }

  public Category addCategory(String categoryName) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      Category cat = new Category();
      cat.setTitle(categoryName);
      sess.persist(cat);
      sess.flush();
      tx.commit();
      return cat;
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to create category named '" + categoryName + "'", e);
      }
    }
  }

  public Transcription addTranscription(Transcription newt, List<TranscriptionFragment> fragments,
      Hashtable<String, String> topics) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      sess.save(newt);

      sess.saveOrUpdate(newt.getCategory());

      Map<String, TranscriptionTopic> topicMap = new HashMap<String, TranscriptionTopic>();

      Iterator<Entry<String, String>> topicIter = topics.entrySet().iterator();
      while (topicIter.hasNext()) {
        Map.Entry<java.lang.String, java.lang.String> t = (Map.Entry<java.lang.String, java.lang.String>) topicIter
            .next();
        String topicId = t.getKey();
        String topicDesc = t.getValue();
        TranscriptionTopic tt = new TranscriptionTopic();
        tt.setTopicId(topicId);
        tt.setTopicName(topicDesc);
        tt.setTranscription(newt);
        sess.save(tt);
        topicMap.put(topicId, tt);
      }

      Iterator<TranscriptionFragment> fit = fragments.iterator();
      while (fit.hasNext()) {
        TranscriptionFragment transcriptionFragment = (TranscriptionFragment) fit.next();
        transcriptionFragment.setTranscription(newt);
        
        TranscriptionTopic topic = topicMap.get(transcriptionFragment.getTransientTopicId());
        if (topic!=null && topic.getTime()==null) {
          topic.setTime(transcriptionFragment.getTime());
          sess.update(topic);
        }
        
        transcriptionFragment.setTopic(topic);
        
        sess.save(transcriptionFragment);
      }


      tx.commit();
      return newt;
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to create transcription!", e);
      }

    }
  }

  public Transcription addFragments(Transcription newt, List<TranscriptionFragment> fragments) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      Iterator<TranscriptionFragment> fit = fragments.iterator();
      while (fit.hasNext()) {
        TranscriptionFragment transcriptionFragment = (TranscriptionFragment) fit.next();
        transcriptionFragment.setTranscription(newt);
        sess.save(transcriptionFragment);
      }

      tx.commit();
      return newt;
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to create transcription fragments!", e);
      }

    }
  }

  public List<String> getTranscriptionLines(Transcription trans) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      try {

        List<String> res = new ArrayList<String>();

        Iterator<TranscriptionFragment> it = sess.createCriteria(TranscriptionFragment.class)
            .add(Restrictions.eq("transcription", trans)).addOrder(Order.asc("time")).list().iterator();
        while (it.hasNext()) {
          TranscriptionFragment t = (TranscriptionFragment) it.next();
          res.add(t.getText());
        }

        return res;
      } finally {
        tx.commit();
      }
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to search for transcription lines for transcription '" + trans + "'", e);
      }
    }
  }

  public void updateTranscription(Transcription current) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      sess.update(current);
      tx.commit();
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to update transcription '" + current + "'", e);
      }

    }
  }

  public void deleteTranscriptionById(final Long id) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {
      Transcription trans = (Transcription) sess.get(Transcription.class, id);
      if (trans == null) {
        throw new TsabException("Unable to delete Transcription, does not exist! id:)" + id);
      }

      List<TranscriptionFragment> frags = (List<TranscriptionFragment>) sess
          .createCriteria(TranscriptionFragment.class).add(Restrictions.eq("transcription", trans))
          .addOrder(Order.asc("time")).list();

      Iterator<TranscriptionFragment> fragit = frags.iterator();

      //log.debug("Removing fragments. Count:"+frags.size());

      while (fragit.hasNext()) {
        TranscriptionFragment f = (TranscriptionFragment) fragit.next();
        sess.delete(f);
      }
      sess.delete(trans);
      tx.commit();
    } catch (Exception e) {
      try {
        e.printStackTrace();
        tx.rollback();
      } finally {
        throw new TsabException("Failed to delete transcription for Id:" + id);
      }
    }
  }

  /**
   * @param fn
   * @return Found Transcription. <code>null</code> if not found.
   * @throws TsabException
   */
  public Transcription getTranscriptionByFn(String fn) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        Iterator<Transcription> it = sess.createCriteria(Transcription.class).add(Restrictions.eq("fn", fn)).list()
            .iterator();

        return it.hasNext() ? it.next() : null;
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcription for fn field '" + fn + "'", e);
      }

    }
  }

  public List<Transcription> getAllTranscriptions() throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Transcription.class).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcriptions!", e);
      }

    }
  }

  public Category getCategoryById(Long id) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return (Category) sess.get(Category.class, id);
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find category for code '" + id + "'", e);
      }

    }
  }

  public String searchTranscriptions(String queryString) throws TsabException {

    if (queryString == null || queryString.length() == 0) {
      throw new TsabException("Search query not specified!");
    }

    String search2 = "";

    String[] qs = queryString.split(" ");

    for (String qq : qs) {
      search2 += (search2.length() > 0 ? " " : "") + qq + "*";
    }

    Query q = Search.getQuery(search2);
    Searcher s = Search.getLuceneSearcher();

    TopDocs hits;
    try {
      hits = s.search(q, 50);
    } catch (IOException e) {
      throw new TsabException("Failed to search!", e);
    }

    int playbuttons = 0;

    StringBuffer finalResult = new StringBuffer();

    log.debug("Found " + hits.totalHits + " matches to the search!");

    Session sess = null;
    try {
      sess = HibernateUtil.getSessionFactory().openSession();

      for (int i = 0; i < hits.totalHits; i++) {

        Document doc;
        try {
          doc = s.doc(hits.scoreDocs[i].doc);
        } catch (CorruptIndexException e) {
          throw new TsabException("Corrupt Exception, unable to search!", e);
        } catch (IOException e) {
          throw new TsabException("Failed to search!", e);
        }

        String content = doc.get("contents");
        String title = doc.get("title");
        String category = doc.get("category");
        String uid = doc.get("uid");

        log.debug("Query hit title:" + title);

        String[] lines = content.split("\n");
        String results = "";
        int j = 0;
        int entrylines = 0;

        //log.debug("Content: "+content);

        //log.debug("Total number of lines:"+lines.length);

        for (String line : lines) {

          j++;

          String[] words = line.split(" ");

          boolean include = false;

          for (String word : words) {
            for (String qq : qs) {
              if (word.toLowerCase().startsWith(qq.toLowerCase())) {
                include = true;
                break;
              }
            }
          }

          if (include) {
            Long tid = new Long(uid);

            //log.debug("Search match found for row "+(j-1));

            List<TranscriptionFragment> fraglist = sess.createCriteria(TranscriptionFragment.class)
                .add(Restrictions.eq("transcription.id", tid)).addOrder(Order.asc("time")).list();
            TranscriptionFragment frag = fraglist.get(j - 1);

            results += "<a href='javascript:donothing();' id='playbutton_" + playbuttons + "' onclick='playOneLine("
                + playbuttons + "," + uid + ", " + frag.getTime() + ")'>CONTROL_PLAY</a> ..."
                + Tools.toQueryResult(queryString, line) + "...<br/>";
            playbuttons++;
            entrylines++;
          }
          if (entrylines == Constants.linesPerSearchEntry) {
            break;
          }

        }

        finalResult.append("<h3><a href='play?trans=" + uid + "'>" + (i + 1) + ". " + title + "</a></h3>");

        finalResult.append(results);
      }

    } finally {
      if (sess != null) {
        sess.close();
      }
    }

    return finalResult.toString();
  }

  public String getRelatedRecordings(Long transId) throws TsabException {
    String code = transId.toString();

    int doc = Search.getDocumentId(code);

    StringBuffer res = new StringBuffer();

    MoreLikeThis mlt = new MoreLikeThis(Search.getLuceneReader());
    mlt.setFieldNames(new String[] { "title", "category", "contents" });
    mlt.setMinWordLen(3);
    mlt.setBoost(true);

    try {
      Query q = mlt.like(doc);

      Searcher s = Search.getLuceneSearcher();

      TopDocs hits = s.search(q, 5);

      log.info("Found related:" + hits.totalHits);

      for (int i = 0; i < (hits.totalHits > 5 ? 5 : hits.totalHits); i++) {

        Document doc2 = s.doc(hits.scoreDocs[i].doc);

        String title = doc2.get("title");
        //String category = doc2.get("category");
        String uid = doc2.get("uid");

        if (!uid.equals(code)) {
          res.append("<a href='play?trans=" + uid + "'>" + title + "</a><br/>");
        }
      }
    } catch (Exception e) {
      throw new TsabException("Failed to find related recordings!", e);
    }
    return res.toString();
  }

  public List<Category> getRootCategories() throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Category.class).add(Restrictions.isNull("parent")).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find root categories!", e);
      }

    }

  }

  public List<Category> getSubCategories(Long parentCategoryId) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Category.class).add(Restrictions.eq("parent.id", parentCategoryId)).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find sub-categories for parent:" + parentCategoryId, e);
      }

    }
  }

  public List<Transcription> getRecentlyAdded() throws TsabException {
    return getRecentlyAdded(MAX_RECENTLY_ADDED);
  }

  public List<Transcription> getMostPopular() throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Transcription.class).setMaxResults(MAX_MOST_POPULAR)
            .addOrder(Order.desc("viewCount")).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find Most Popular transcriptions!", e);
      }

    }
  }

  public void increaseViewCount(Long transId) throws TsabException {
    Transcription trans = getTranscriptionById(transId);
    Long currentCount = trans.getViewCount();
    if (currentCount == null) {
      currentCount = new Long(0);
    }
    trans.setViewCount(currentCount.longValue() + 1);
    updateTranscription(trans);
  }

  public void deleteCategory(String catId) throws NumberFormatException, TsabException {

    Session sess = HibernateUtil.getSessionFactory().openSession();

    Transaction tx = sess.beginTransaction();
    try {

      try {

        internalDeleteCategory(sess, new Long(catId));

      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to delete category!", e);
      }

    }

  }

  private void internalDeleteCategory(Session sess, Long catId) throws TsabException {
    Category cat = (Category) sess.get(Category.class, catId);
    int transSize = getTranscriptionsByCategoryId(catId).size();
    if (transSize > 0) {
      throw new TsabException("Unalbe to delete category with Id " + catId + "! There are " + transSize
          + " transcriptions in  that category!");
    }

    Iterator subcatlist = sess.createCriteria(Category.class).add(Restrictions.eq("parent", cat)).list().iterator();
    while (subcatlist.hasNext()) {
      Category subcat = (Category) subcatlist.next();
      internalDeleteCategory(sess, subcat.getId());
    }

    sess.delete(cat);
  }

  public Category addCategory(String catName, String parentCatId) throws NumberFormatException, TsabException {
    Category subCat = null;

    if (parentCatId != null && parentCatId.length() > 0) {
      subCat = getCategoryById(new Long(parentCatId));
      if (subCat == null) {
        throw new TsabException("SubCategory with ID not found! " + parentCatId);
      }
    }

    Session sess = HibernateUtil.getSessionFactory().openSession();

    Transaction tx = sess.beginTransaction();
    try {

      try {

        Category newCat = new Category();
        if (subCat != null) {
          newCat.setParent(subCat);
        }

        newCat.setTitle(catName);
        sess.persist(newCat);
        sess.flush();
        return newCat;
      } finally {
        tx.commit();
      }

    } catch (Exception e) {
      log.error("Failed to add new category!", e);
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to add new category!", e);
      }

    }

  }

  public List<Transcription> getTranscriptionsByCategoryId(Long catId) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Transcription.class).add(Restrictions.eq("category.id", catId))
            .addOrder(Order.asc("title")).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcriptions for category " + catId, e);
      }

    }
  }

  public List<Transcription> getTranscriptionsByDate(Date date) throws TsabException {

    Calendar startDate = Calendar.getInstance();
    startDate.setTime(date);

    Calendar endDate = Calendar.getInstance();
    endDate.setTime(date);
    endDate.add(Calendar.DAY_OF_MONTH, 1);

    log.debug("Finding transcriptions between " + startDate.getTime() + " and " + endDate.getTime());

    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Transcription.class)
            .add(Restrictions.between("recorded", startDate.getTime(), endDate.getTime())).addOrder(Order.asc("title"))
            .list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find transcriptions for date " + date, e);
      }

    }
  }

  public void deleteFragments(Transcription newt) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      Iterator it = sess.createCriteria(TranscriptionFragment.class).add(Restrictions.eq("transcription", newt)).list()
          .iterator();
      while (it.hasNext()) {
        TranscriptionFragment frag = (TranscriptionFragment) it.next();
        sess.delete(frag);
      }

      tx.commit();
    } catch (Exception e) {
      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to delete transcription fragment for transcription'" + newt.getId() + "'", e);
      }
    }

  }

  public List<Transcription> getRecentlyAdded(int maxResults) throws TsabException {
    Session sess = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = sess.beginTransaction();
    try {

      try {
        return sess.createCriteria(Transcription.class).setMaxResults(maxResults)
            .addOrder(Order.desc("recorded")).list();
      } finally {
        tx.commit();
      }

    } catch (Exception e) {

      try {
        tx.rollback();
      } finally {
        throw new TsabException("Failed to find Recently Added transcriptions!", e);
      }

    }
  }

}
