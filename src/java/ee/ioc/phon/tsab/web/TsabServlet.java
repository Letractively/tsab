package ee.ioc.phon.tsab.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ee.ioc.phon.tsab.common.TsabException;
import ee.ioc.phon.tsab.dao.TsabDaoService;
import ee.ioc.phon.tsab.domain.Category;
import ee.ioc.phon.tsab.domain.Transcription;
import ee.ioc.phon.tsab.domain.TranscriptionFragment;
import ee.ioc.phon.tsab.domain.TranscriptionTopic;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class TsabServlet extends FreemarkerServlet {

  private final static Logger log = Logger.getLogger(TsabServlet.class);

  private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

  private final static String PARAM_TSAB_LANGUAGE="TsabLanguage";
  
  private ResourceBundle bundle;
  
  public TsabServlet() {
    super();
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    
    doGet(request, response);
  }
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    
    request.setCharacterEncoding("UTF-8"); 
    
    String path = request.getPathInfo();
    log.debug("Handling page: " + path);

    try {
      handleMainLayout(request, response);

      if ("/index".equals(path)) {
        handleIndex(request, response);
      } else if ("/category".equals(path)) {
        handleCategory(request, response);
      } else if ("/search".equals(path)) {
        handleSearch(request, response);
      } else if ("/play".equals(path)) {
        handlePlay(request, response);
      } else if ("/loadAudio".equals(path)) {
        handleLoadAudio(request, response);
      } else if ("/calendar".equals(path)) {
        handleCalendar(request, response);
      } else if ("/rssRecent".equals(path)) {
        handleRSSRecent(request, response);
      }else if ("/login".equals(path)) {
        handleLogin(request, response);
      }

      request.setAttribute("ctxpath", request.getContextPath());
      
      StringBuffer url = request.getRequestURL();
      //strip last segment after /
      String reqUrl = url.substring(0, url.lastIndexOf("/"));
      request.setAttribute("requestUrl", reqUrl);
      

    } catch (Exception e) {
      log.warn("Failed to process request!", e);
      throw new ServletException("Failed to process request for path:" + path, e);
    }

    super.doGet(request, response);
  }

  private void handleCalendar(HttpServletRequest request, HttpServletResponse response) throws TsabException,
      ParseException {

    String cal = request.getParameter("day");

    Date date = sdf.parse(cal);

    List<Transcription> trans = TsabDaoService.getDao().getTranscriptionsByDate(date);

    request.setAttribute("trans", trans);
    request.setAttribute("date", date);

  }

  private void handleLoadAudio(HttpServletRequest request, HttpServletResponse response) throws TsabException {
    // Play page
    Long transId = new Long(request.getParameter("audio"));
    Transcription playTrans;
    List<TranscriptionFragment> speech;
    
    //List<TranscriptionTopic> topics;

    try {
      playTrans = TsabDaoService.getDao().getTranscriptionById(transId);
      speech = TsabDaoService.getDao().getTranscriptionFragments(playTrans);
      //topics = TsabDaoService.getDao().getTranscriptionTopics(playTrans);
      
      // Instrument speech TranscriptionFragments with transient topicId
      int topicSeq = 0;
      Set<String> topicSet = new HashSet<String>();
      Iterator<TranscriptionFragment> iter = speech.iterator();
      while (iter.hasNext()) {
        TranscriptionFragment f = (TranscriptionFragment) iter.next();
        TranscriptionTopic t = f.getTopic();
        
        if (t!=null && !topicSet.contains(t.getTopicId())) {
          topicSeq++;
          topicSet.add(t.getTopicId());
        }
        
        f.setTransientTopicSeq(t==null?0:topicSeq);
        f.setTransientTopicDesc(t==null?"":t.getTopicName());
      }

    } catch (TsabException e) {
      log.warn(e);
      throw new RuntimeException("Unable to load transcription or related recordings for transcription id '" + transId
          + "'!", e);
    }
    request.setAttribute("t", playTrans);
    request.setAttribute("speech", speech);
    
    /*if (topics.size()>0){
      request.setAttribute("topics", topics);  
    }
*/
    try {
      request.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new TsabException("UTF-8 not supported!", e);
    }
    response.setContentType("application/json"); //"application/json; charset=UTF-8"

  }

  private void handleMainLayout(HttpServletRequest request, HttpServletResponse response) throws TsabException {
    // Provide i18n support
    request.setAttribute("loc", bundle);

    request.setAttribute("statics", BeansWrapper.getDefaultInstance().getStaticModels());

    // Main layout
    List<Category> mainCategories;
    mainCategories = TsabDaoService.getDao().getRootCategories();
    request.setAttribute("mainCategories", mainCategories);

    Map<String, List<Category>> subCategories = new HashMap<String, List<Category>>();
    Iterator<Category> it = mainCategories.iterator();
    while (it.hasNext()) {
      Category mainCat = (Category) it.next();
      List<Category> subList = TsabDaoService.getDao().getSubCategories(mainCat.getId());
      subCategories.put(mainCat.getId().toString(), subList);
    }

    request.setAttribute("subCats", subCategories);

    // attempt to detect active main category to keep the tab open
    Category activeRootCategory = null;

    String cat = request.getParameter("cat");
    String trans = request.getParameter("trans");
    if (cat != null && cat.length() > 0) {
      activeRootCategory = TsabDaoService.getDao().getCategoryById(new Long(cat));
    } else if (trans != null && trans.length() > 0) {
      activeRootCategory = TsabDaoService.getDao().getTranscriptionById(new Long(trans)).getCategory();
      if (activeRootCategory != null) {
        request.setAttribute("currentCategoryId", activeRootCategory.getId());
      }
    }

    activeRootCategory = traverseToRootCategory(activeRootCategory);

    if (activeRootCategory != null) {
      request.setAttribute("activeRootCategory", activeRootCategory);
    }

    if (cat != null && cat.length() > 0) {
      request.setAttribute("currentCategoryId", new Long(cat));
    }

    
    
  }

  private Category traverseToRootCategory(Category activeMainCategory) {
    if (activeMainCategory != null && activeMainCategory.getParent() != null) {
      return traverseToRootCategory(activeMainCategory.getParent());
    }
    return activeMainCategory;
  }

  private void handlePlay(HttpServletRequest request, HttpServletResponse response) {
    
    String debugStr = request.getParameter("zebug");
    
    boolean debugEnabled = debugStr!=null && debugStr.length()>0;
    
    List<TranscriptionTopic> topics = null; 
    
    // Play page    
    Long transId = new Long(request.getParameter("trans"));
    Transcription playTrans;
    String relatedRecordings;
    try {
      playTrans = TsabDaoService.getDao().getTranscriptionById(transId);
      relatedRecordings = TsabDaoService.getDao().getRelatedRecordings(transId);

      if (playTrans != null) {
        TsabDaoService.getDao().increaseViewCount(transId);
        topics = TsabDaoService.getDao().getTranscriptionTopics(playTrans);
      }

    } catch (TsabException e) {
      log.warn(e);
      throw new RuntimeException("Unable to load transcription or related recordings for transcription id '" + transId
          + "'!", e);
    }
    
    request.setAttribute("transcription", playTrans);
    
    if (topics!=null && topics.size()>0) {
      //iterate through and assign transients
      
      boolean showHour = false;
      if (topics.size()>0) {
        long lastMs = topics.get(topics.size()-1).getTime();
        showHour = lastMs>1000*60*60;
      }
      
      Iterator<TranscriptionTopic> ttit = topics.iterator();
      int seq = 0;
      while (ttit.hasNext()) {
        TranscriptionTopic t = (TranscriptionTopic) ttit.next();
        
        long ms = t.getTime();
        
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        long mins = TimeUnit.MILLISECONDS.toMinutes(ms)-TimeUnit.HOURS.toMinutes(hours);
        long secs = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(mins) - TimeUnit.HOURS.toSeconds(hours);
        
        String timeStr = (showHour?hours+":":"")+(mins < 10 ? "0" : "")+mins+":"+(secs < 10 ? "0" : "")+secs;
        
        t.setTransientSeq(++seq);
        t.setTransientTimeStr(timeStr);
        
      }
      request.setAttribute("topics", topics);
    }
    
    request.setAttribute("playCount", Math.floor(playTrans.getAudioLength().longValue()/60f)+1);
    request.setAttribute("playLength", playTrans.getAudioLength().longValue());
    request.setAttribute("relatedRecordings", relatedRecordings);    
    
    if (debugEnabled) {
      request.setAttribute("zebug", Boolean.TRUE);
    }
    
  }

  private void handleSearch(HttpServletRequest request, HttpServletResponse response) throws TsabException {
    
    String debugStr = request.getParameter("zebug");
    
    boolean debugEnabled = debugStr!=null && debugStr.length()>0;

    
    // Search page
    String queryString = request.getParameter("q");
    
    String searchResult = "";
    
    if (queryString==null || queryString.length()==0) {      
      searchResult = bundle.getString("search_query_not_specified");
    } else {    

      log.debug("Searching for "+queryString);

      searchResult = TsabDaoService.getDao().searchTranscriptions(queryString);
      if (searchResult!=null) {
        String playstr = bundle.getString("control_play");  
        searchResult = searchResult.replaceAll("CONTROL_PLAY", playstr);
        
      }
    }
    
    request.setAttribute("searchResult", searchResult);
    
    if (debugEnabled) {
      request.setAttribute("zebug", Boolean.TRUE);
    }
    
  }

  private void handleCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    // Category page
    Long catId = new Long(request.getParameter("cat"));
    Category cat;
    try {
      cat = TsabDaoService.getDao().getCategoryById(catId);

      request.setAttribute("category", cat);

      List<Category> subCatList = TsabDaoService.getDao().getSubCategories(catId);
      if (subCatList.size() > 0) {
        request.setAttribute("subCatList", subCatList);
      }

      List<Transcription> transList = TsabDaoService.getDao().getTranscriptionsByCategoryId(catId);
      if (transList.size() > 0) {
        request.setAttribute("transList", transList);
      }

    } catch (TsabException e) {
      log.warn(e);
      throw new ServletException("Failed to load category!" + catId, e);
    }

  }

  private void handleIndex(HttpServletRequest request, HttpServletResponse response) throws TsabException {
    // Index page
    List<Transcription> recentlyAdded = TsabDaoService.getDao().getRecentlyAdded();
    List<Transcription> mostPopular = TsabDaoService.getDao().getMostPopular();

    request.setAttribute("recentlyAdded", recentlyAdded);
    request.setAttribute("mostPopular", mostPopular);
  }

  @Override
  protected String requestUrlToTemplatePath(HttpServletRequest request) {
    String path = super.requestUrlToTemplatePath(request);
    path = path + ".ftl";
    return path;
  }

  @Override
  protected Configuration createConfiguration() {
    return new Configuration() {
      @Override
      public void setSetting(String key, String value) throws TemplateException {
        if (key!=null && PARAM_TSAB_LANGUAGE.equalsIgnoreCase(key)) {
          initLanguage(this, value);
        } else {
          super.setSetting(key, value);
        }
        
      }

    };
  }

  private void initLanguage(Configuration cfg, String value) {
    Locale locale = Locale.ENGLISH;
    if (value!=null && value.length()>0) {
      log.info("Configuring servlet instance for language code: "+value);
      locale = new Locale(value);
    }
    bundle = ResourceBundle.getBundle("messages", locale);
    cfg.setLocale(locale);
  }

  private void handleRSSRecent(HttpServletRequest request, HttpServletResponse response) throws TsabException {
      List<Transcription> recentlyAdded = TsabDaoService.getDao().getRecentlyAdded(10);
      request.setAttribute("recentlyAdded", recentlyAdded);
  }

  private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws TsabException {
  }

  
}
