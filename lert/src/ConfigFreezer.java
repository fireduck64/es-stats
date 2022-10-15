package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigFreezer extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigFreezer(String zone, String location)
  {
    filter_terms = ImmutableMap.of("zone", zone, "location", location);
    id=getIndexBase() + ".freezer." + zone + "." + location;

  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 60L * 15L * 1000L;} // 15-min
  @Override
  public long checkIntervalMs() {return 1L*60L*1000L; } //1 min 
  @Override
  public long maxLookbackMs() {return 10L*60L*1000L; } //10 min

  @Override
  public String getIndexBase(){return "airsense";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "temperature";}

  @Override
  public Double getLowVal(){return -30.0;}

  @Override
  public Double getHighVal(){return -7.0;}

  @Override
  public boolean useLatest(){return false; }
}
