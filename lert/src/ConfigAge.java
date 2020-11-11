package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigAge extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected final String id;

  protected final String base;

  public ConfigAge(String base, String system)
  {
    this.base =base;
    filter_terms = ImmutableMap.of("system", system);
    id=getIndexBase() + ".age." + system;
  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 2L* 60L * 60L * 1000L;} // 2 hr
  @Override
  public long checkIntervalMs() {return 2L*60L*1000L; } //2 min 
  @Override
  public long maxLookbackMs() {return 60L*60L*1000L; } //60 min

  @Override
  public String getIndexBase(){return base;}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "age";}

  @Override
  public Double getLowVal(){return 0.0;}

  @Override
  public Double getHighVal(){return 1.2;}

}
