package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * For a room we expect to be a bit cool, but not freezing and not too hot
 */
public class ConfigGarageRoom extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigGarageRoom(String zone, String location)
  {
    filter_terms = ImmutableMap.of("zone", zone, "location", location);
    id=getIndexBase() + ".sroom." + zone + "." + location;

  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 2L * 60L * 60L * 1000L;} // 2 hr
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
  public Double getLowVal(){return 12.0;} 

  @Override
  public Double getHighVal(){return 31.0;} //90F

}
