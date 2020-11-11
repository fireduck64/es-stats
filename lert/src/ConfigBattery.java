package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigBattery extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigBattery(String entity_id)
  {
    filter_terms = ImmutableMap.of("entity_id", entity_id);
    id=getIndexBase() + ".battery." + entity_id;

  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 120L * 60L * 1000L;} // 120-min
  @Override
  public long checkIntervalMs() {return 15L*60L*1000L; } //15 min 
  @Override
  public long maxLookbackMs() {return 60L*60L*1000L; } //60-min

  @Override
  public String getIndexBase(){return "zwave";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "state_number";}

  @Override
  public Double getLowVal(){return 50.0;}

  @Override
  public Double getHighVal(){return 100.0;}

}
