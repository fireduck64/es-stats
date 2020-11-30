package duckutil.lert;

import duckutil.Pair;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.List;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

public class LertAgent
{
  private final LertConfig l_config;
  private final Lert lert;

  public LertAgent(Lert lert, LertConfig l_config)
  {
    this.lert = lert;
    this.l_config = l_config;
  }

  public LertConfig getConfig(){return l_config;}
  public String getID(){return l_config.getID();}
  public Lert getLert(){return lert;}


  public Pair<LertState, String> getCurrentState()
    throws Exception
  {
    SearchResponse search = lert.getLatest(l_config.getIndexBase(), 250, l_config.getFilterTerms());
    SearchHits hits = search.getHits();

    long age_of_recent_value=1000000000L;
    long age_of_recent_good_value=1000000000L;

    Double most_recent_value = null;


    for(SearchHit hit : hits)
    {
      Map<String,Object> source_doc = hit.getSourceAsMap();
      String timestamp = (String)source_doc.get("timestamp");
      if (!timestamp.endsWith("Z"))
      {
        if (!timestamp.endsWith("-0700"))
        {
          timestamp = timestamp +"Z";
        }
      }

      Temporal z = null;
      
      try
      {
        z = ZonedDateTime.parse(timestamp);
      }
      catch(java.time.format.DateTimeParseException e)
      {
        SimpleDateFormat sdf_iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        z = sdf_iso.parse(timestamp).toInstant();

      }
      ZonedDateTime now = ZonedDateTime.now();
      long age = z.until(now, ChronoUnit.MILLIS);
      //System.out.println("" + z + " " + age);
      Double val = extractValue(source_doc, l_config.getValuePathList());
      if (val == null)
      {
        //System.out.println("No path: " + l_config.getValuePath() + " in " + source_doc);
      }
      if (val != null)
      {
        if (most_recent_value == null) most_recent_value = val;

        age_of_recent_value = Math.min(age, age_of_recent_value);
        if (isGood(val))
        {
          age_of_recent_good_value = Math.min(age, age_of_recent_good_value); 
        }
      }
    }

    if (age_of_recent_good_value < l_config.maxLookbackMs())
    {
      return new Pair(LertState.OK, ""+l_config.getValuePath()+": " + most_recent_value);
    }
    if (age_of_recent_value < l_config.maxLookbackMs())
    {
      return new Pair(LertState.BAD, ""+l_config.getValuePath()+": " + most_recent_value);
    }

    return new Pair(LertState.MISSING, "MISSING");

  }

  private Double extractValue(Map<String, Object> search_doc, List<String> path)
  {
    Map<String, Object> m = search_doc;
    Object o = null;

    for(String s : path)
    {
      if (m == null) return null;
      o = m.get(s);
      if (o instanceof Map) m = (Map)o;
      else
      {
        m = null;
      }
    }

    if (o == null) return null;
    if (o instanceof Long){double v = (long)o; return v;}
    if (o instanceof Integer){double v = (int)o; return v;}
    if (o instanceof Double){double v = (double)o; return v;}
    System.out.println("Val: " + o);
    return null;
  }

  private boolean isGood(double v)
  {
    Double low = l_config.getLowVal();
    if (low != null)
    {
      if (v < low) return false;
    }
    Double high = l_config.getHighVal();
    if (high != null)
    {
      if (v > high) return false;
    }
    return true;

  }

}
