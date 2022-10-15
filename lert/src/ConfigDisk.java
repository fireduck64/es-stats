package duckutil.lert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.List;

public class ConfigDisk extends LertConfig
{
  protected ImmutableMap<String, String> filter_terms;
  protected String id;

  private final String host;
  private final String mount;
  private final double min_free_space;


  public ConfigDisk(String host, String mount, double min_free_space)
  {
    this.host = host;
    this.mount = mount;
    this.min_free_space = min_free_space;

    filter_terms = ImmutableMap.of("host", host);
    id=getIndexBase() + "." + host + "." + mount;

  }

  @Override
  public String getID(){ return id; }

  @Override
  public long getReAlertTimeMs(){ return 120L * 60L * 1000L;} // 120-min
  @Override
  public long checkIntervalMs() {return 2L*60L*1000L; } //5 min 
  @Override
  public long maxLookbackMs() {return 15L*60L*1000L; } //15-min

  @Override
  public String getIndexBase(){return "diskspace";}

  @Override
  public Map<String,String> getFilterTerms(){return filter_terms;}

  @Override
  public String getValuePath(){return mount +".spaceavail";}

  @Override
  public List<String> getValuePathList(){return ImmutableList.of(mount,"spaceavail");}

  @Override
  public Double getLowVal(){return min_free_space * 1e6;}
  
  @Override
  public Double getHighVal(){return 100.0 * 1000.0 * 1e6;}

  @Override
  public boolean useLatest(){return true; }
}
