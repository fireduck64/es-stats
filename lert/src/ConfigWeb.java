package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigWeb extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigWeb(String host)
  {
    filter_terms = ImmutableMap.of("host", host);
    id=getIndexBase() + "." + host;
  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 2L* 60L * 60L * 1000L;} // 2 hr
  @Override
  public long checkIntervalMs() {return 2L*60L*1000L; } //2 min 
  @Override
  public long maxLookbackMs() {return 15L*60L*1000L; } //15 min

  @Override
  public String getIndexBase(){return "webcheck";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "code";}

  @Override
  public Double getLowVal(){return 199.98;}

  @Override
  public Double getHighVal(){return 200.02;}

}
