package duckutil.lert;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ConfigSnow extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;


  public ConfigSnow(String network, String host)
  {
    filter_terms = ImmutableMap.of("network", network, "host", host);
    id = "snow." + network + "." + host;
  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 2L* 60L * 60L * 1000L;} // 2 hr
  @Override
  public long checkIntervalMs() {return 2L*60L*1000L; } //2 min 
  @Override
  public long maxLookbackMs() {return 15L*60L*1000L; } //30 min

  @Override
  public String getIndexBase(){return "snowblossom-node";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return "connectedPeers";}

  @Override
  public Double getLowVal(){return 2.0;}

  @Override
  public Double getHighVal(){return 100.0;}

}
