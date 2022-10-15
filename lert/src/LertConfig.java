package duckutil.lert;

import java.util.Map;
import com.google.common.collect.ImmutableList;
import java.util.List;

public abstract class LertConfig
{
   
  public long getReAlertTimeMs(){ return 4L * 3600L * 1000L;} // 4 hours 
  public long checkIntervalMs() {return 5L*60L*1000L; } //5 min 
  public long maxLookbackMs() {return 30L*60L*1000L; } //30 min

  public abstract String getIndexBase();
  public abstract String getID();

  // may be null
  public Map<String,String> getFilterTerms(){return null; }

  public List<String> getValuePathList()
  {
    return ImmutableList.of(getValuePath());
  }
  public abstract String getValuePath();

  // null indicates no limit set
  public Double getLowVal() {return null;}
  public Double getHighVal() {return null;}

	/**
   * Takes as input the thing at the getValuePath
   */
  public Double getValue(Object o)
  {
    if (o == null) return null;
    if (o instanceof Long){double v = (long)o; return v;}
    if (o instanceof Integer){double v = (int)o; return v;}
    if (o instanceof Double){double v = (double)o; return v;}
    if (o instanceof String)
    { 
      try
      { 
        double v = Double.parseDouble( (String)o);
        return v;
      }
      catch(Exception e)
      {
      }

    }
		return null;

  }

  // If true, use the latest value even if there is a good value
  // within the lookup time.
  public boolean useLatest(){return false; }

  private static String global_topic_arn;
  public static void setGlobalTopicArn(String arn){ global_topic_arn = arn; }

  public String getAlertTopicArn() {return global_topic_arn;}

}
