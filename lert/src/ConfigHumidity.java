package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigHumidity extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigHumidity(String zone, String location)
  {
    filter_terms = ImmutableMap.of("zone", zone, "location", location);
    id=getIndexBase() + ".freezer." + zone + "." + location;

  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 4L *60L * 60L * 1000L;} // 4 hr
  @Override
  public long checkIntervalMs() {return 5L*60L*1000L; } //5 min 
  @Override
  public long maxLookbackMs() {return 20L*60L*1000L; } //20 min

  @Override
  public String getIndexBase(){return "airsense";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "humidity";}

  @Override
  public Double getLowVal(){return 20.0;}

  @Override
  public Double getHighVal(){return 65.0;}

}
