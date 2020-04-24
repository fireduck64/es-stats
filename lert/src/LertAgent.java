package duckutil.lert;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchHit;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

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

  public LertState getCurrentState()
    throws Exception
  {
    SearchResponse search = lert.getLatest(l_config.getIndexBase(), 250, l_config.getFilterTerms());
    SearchHits hits = search.getHits();

    long age_of_recent_value=1000000000L;
    long age_of_recent_good_value=1000000000L;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");

    for(SearchHit hit : hits)
    {
      Map<String,Object> source_doc = hit.getSourceAsMap();
      String timestamp = (String)source_doc.get("timestamp");
      if (!timestamp.endsWith("Z")) timestamp = timestamp +"Z";

      ZonedDateTime z = ZonedDateTime.parse(timestamp);
      ZonedDateTime now = ZonedDateTime.now();
      long age = z.until(now, ChronoUnit.MILLIS);
      //System.out.println("" + z + " " + age);
      Double val = extractValue(source_doc, l_config.getValuePath());
      if (val != null)
      {
        age_of_recent_value = Math.min(age, age_of_recent_value);
        if (isGood(val))
        {
          age_of_recent_good_value = Math.min(age, age_of_recent_good_value); 
        }
      }
    }
    if (age_of_recent_good_value < l_config.maxLookbackMs()) return LertState.OK;
    if (age_of_recent_value < l_config.maxLookbackMs()) return LertState.BAD;
    return LertState.MISSING;

  }

  private Double extractValue(Map<String, Object> search_doc, String path)
  {
    Object o = search_doc.get(path);
    if (o == null) return null;
    if (o instanceof Long){double v = (long)o; return v;}
    if (o instanceof Double){double v = (double)o; return v;}
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
