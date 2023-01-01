package duckutil.lert;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import duckutil.Config;
import duckutil.ConfigFile;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.apache.http.HttpHost;
import duckutil.PeriodicThread;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.index.query.Operator;
import com.google.common.collect.ImmutableList;

public class Lert
{
  public static void main(String args[]) throws Exception
  {
    new Lert(new ConfigFile(args[0]));
  }

  private final Config config;
  private final RestHighLevelClient es_client;
  private final AmazonSNSClient sns;
  private final StatusRegistry reg;

  public Lert(Config config) throws Exception
  {
    this.config = config;
    config.require("elasticsearch_url");
    config.require("alert_topic_arn");
    config.require("aws_key_id");
    config.require("aws_secret");
    config.require("web_port");

    es_client = openElasticSearchClient();

    reg = new StatusRegistry();

    new WebServer(reg, config.getInt("web_port"));

    String arn = config.get("alert_topic_arn");
    LertConfig.setGlobalTopicArn(arn);

    sns = new AmazonSNSClient(new BasicAWSCredentials(config.get("aws_key_id"), config.get("aws_secret")));
    sns.setEndpoint("sns.us-west-2.amazonaws.com");

    new LertThread(new LertAgent(this, new ConfigAirSense("crab.garage.h", "crabshack", "garage", "humidity", 15.0, 75.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("crab.sub.h", "crabshack", "sub", "humidity", 30.0, 75.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("crab.clawsub.h", "crabshack", "clawsub", "humidity", 30.0, 92.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("crab.hole.h", "crabshack", "hole", "humidity", 30.0, 100.2))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("crab.hole.t", "crabshack", "hole", "temperature", -5.0, 35.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("crab.claw.h", "crabshack", "claw", "humidity", 30.0, 77.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("crab.garage.t", "crabshack", "garage", "temperature", 2.0, 32.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("crab.claw.t", "crabshack", "claw", "temperature", 15.0, 27.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("crab.office.h", "crabshack", "office", "humidity", 20.0, 60.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("crab.office.t", "crabshack", "office", "temperature", 16.6, 25.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("attic.humidity", "attic", "room_air", "humidity", 20.0, 85.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("attic.temp", "attic", "room_air", "temperature", 2.0, 40.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("hall.temp", "hall", "room_air", "temperature", 5.0, 32.0))).start();
    //new LertThread(new LertAgent(this, new ConfigAirSense("hall.humidity", "hall", "room_air", "humidity", 20.0, 75.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("garage.temp", "garage", "room_air", "temperature", 5.0, 35.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("garage.humidity", "garage", "room_air", "humidity", 15, 85))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("server_room.humidity", "server_room", "room_air", "humidity", 20, 75))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("server_room.temp", "server_room", "room_air", "temperature", 10, 32))).start();
    
    new LertThread(new LertAgent(this, new ConfigAirSense("archive.temp", "archive", "room_air", "temperature", 5.0, 35.0))).start();
    new LertThread(new LertAgent(this, new ConfigAirSense("archive.humidity", "archive", "room_air", "humidity", 15, 88))).start();

    //new LertThread(new LertAgent(this, new ConfigFreezer("garage","freezer"))).start();
    new LertThread(new LertAgent(this, new ConfigFreezer("general_stores","freezer"))).start();
    new LertThread(new LertAgent(this, new ConfigFreezer("crabshack","garage_freezer"))).start();
    
    
    new LertThread(new LertAgent(this, new ConfigWeb("fireduck.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("explorer.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("snow-b"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("total.explorer.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("wiki.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("chan-relay.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("ipv4-lookup.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("ipv6-lookup.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("covid19-local"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("covid19-data.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("snow-testshard.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("trashbrain.org"))).start();

    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snowblossom"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snow-test-a"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snow-test-b"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snowblossom.hamster.science"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-a.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-b.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-tx1.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-de1.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","ryzen"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","ogog"))).start();
    new LertThread(new LertAgent(this, new ConfigSnowPool("hippo","a",3))).start();
    //new LertThread(new LertAgent(this, new ConfigSnowPool("hippo","b",3))).start();
    new LertThread(new LertAgent(this, new ConfigSnowPool("snow-testshard","d",1))).start();
    
    new LertThread(new LertAgent(this, new ConfigProcess("bitcoin-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("bitcoin-cash-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("ethereum-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("snowblossom-price-notify"))).start();

    new LertThread(new LertAgent(this, new ConfigProcess("stock-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-tracker"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-watcher"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-topicload"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("snow-faucet"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("sign-update"))).start();
    
    new LertThread(new LertAgent(this, new ConfigGood("backup","lamp"))).start();
    new LertThread(new LertAgent(this, new ConfigAge("backup","lamp"))).start();
    //new LertThread(new LertAgent(this, new ConfigBattery("sensor.test_sensor_a_battery_level"))).start();
    
    new LertThread(new LertAgent(this, new ConfigGeth("eth2"))).start();
    new LertThread(new LertAgent(this, new ConfigGeth("eth2b"))).start();
    new LertThread(new LertAgent(this, new ConfigGeth("hippo"))).start();
    new LertThread(new LertAgent(this, new ConfigGeth("orange"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Validator("eth2b",18))).start();
    new LertThread(new LertAgent(this, new ConfigEth2VCNodes("eth2b",3))).start();
    //new LertThread(new LertAgent(this, new ConfigEth2Attest("eth2",18))).start();
    //new LertThread(new LertAgent(this, new ConfigEth2Peers("eth2"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Peers("eth2b"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Peers("hippo"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Peers("orange"))).start();
    //new LertThread(new LertAgent(this, new ConfigEth2Sync("eth2"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Sync("eth2b"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Sync("hippo"))).start();
    new LertThread(new LertAgent(this, new ConfigEth2Sync("orange"))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("mastodon","/",16.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("mastodon","/home",80.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("ogog","/t4",200.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("ogog","/var/nvme_intel",200.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("ogog","/var/virt",200.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("zogbe","/var/virt",500.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("zogbe","/var/virt2",500.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("hippo","/var",800.0))).start();
    //new LertThread(new LertAgent(this, new ConfigDisk("hippo","/stack",5000.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("noface","/var/virt",200.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("noface","/",50.0))).start();
    //new LertThread(new LertAgent(this, new ConfigDisk("eth2","/var/lib/docker",100.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("eth2","/var/lib/docker",100.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("eth2b","/var/lib/docker",100.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("orange","/",200.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("orange","/bulk",1000.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("ryzen","/bulk",4000.0))).start();
    new LertThread(new LertAgent(this, new ConfigDisk("snowblossom","/home",15.0))).start();
    new LertThread(new LertAgent(this, new ConfigZFS("bucket","stack"))).start();

    new LertThread(new LertAgent(this, new ConfigAirQCo2("studio",850))).start();
    new LertThread(new LertAgent(this, new ConfigAirQTemp("studio",10.0,26.66))).start();
    new LertThread(new LertAgent(this, new ConfigAirQCo2("indoor",850))).start();
    new LertThread(new LertAgent(this, new ConfigAirQCo2("crabshack",850))).start();
    new LertThread(new LertAgent(this, new ConfigAirQ("crabshack",25))).start();
    new LertThread(new LertAgent(this, new ConfigAirQ("indoor",25))).start();
    new LertThread(new LertAgent(this, new ConfigAirQ("studio",25))).start();
    //new LertThread(new LertAgent(this, new ConfigAirQ("outdoor",100))).start();
    new LertThread(new LertAgent(this, new ConfigAirQ("crabshack-outdoor",100))).start();
    new LertThread(new LertAgent(this, new ConfigMD("ryzen", "md127", 6))).start();
    new LertThread(new LertAgent(this, new ConfigMem("mastodon", 2000))).start();
    new LertThread(new LertAgent(this, new ConfigMem("noface", 8000))).start();
    new LertThread(new LertAgent(this, new ConfigMem("zogbe", 8000))).start();
    new LertThread(new LertAgent(this, new ConfigMem("eth2b", 4000))).start();
    new LertThread(new LertAgent(this, new ConfigMem("orange", 4000))).start();


    new LertThread(new LertAgent(this, new ConfigCactiBandwidth("sw0","uplink",200.0))).start();

    //new LertThread(new LertAgent(this, new ConfigZwaveValue("side_door.temp","zwave/side_door/2/49/1/1", 0, 80))).start();
    //new LertThread(new LertAgent(this, new ConfigZwaveValue("archive.temp","zwave/archive/9/49/1/1", 50, 80))).start();
    //new LertThread(new LertAgent(this, new ConfigZwaveValue("archive.humidity","zwave/archive/9/49/1/5", 20, 80))).start();

    for(String algo : ImmutableList.of("HelloJ", "HelloIPA", "HelloGPU")) //HelloTest
    for(String op : ImmutableList.of("run","pull"))
    {
      //new LertThread(new LertAgent(this, new ConfigAlgoCanaryError(op, algo))).start();
    }

    //new LertThread(new LertAgent(this, new ConfigAge("dropbox","ogog"))).start();
    new CloudMetricThread().start();

  }

  protected RestHighLevelClient openElasticSearchClient()
    throws Exception
  {
    URL u = new URL(config.get("elasticsearch_url"));
    
    return new RestHighLevelClient(
      RestClient.builder(new HttpHost(u.getHost(),u.getPort()))
      );
  }

  // Not using the "high level client" because
  // it makes pretty much zero sense
  protected void saveDoc(String index_base, Map<String, Object> values)
    throws Exception
  {
    SimpleDateFormat sdf=new SimpleDateFormat("yyyy.MM.dd");
    String url = String.format("%s%s-%s/_doc",config.get("elasticsearch_url"),index_base,sdf.format(new Date()));
    URL u = new URL(url);

    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setInstanceFollowRedirects(false);

    connection.setRequestProperty("Content-Type", "application/json");

    JSONObject doc = new JSONObject();

    SimpleDateFormat sdf_iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    doc.put("timestamp", sdf_iso.format(new Date()));
    doc.put("time_ms", System.currentTimeMillis());
    doc.putAll(values);

    OutputStream wr = connection.getOutputStream ();
    wr.write(doc.toJSONString().getBytes());
    wr.flush();
    wr.close();

    int code =  connection.getResponseCode();
    //System.out.println(doc.toJSONString());
    //System.out.println("POST code: " + connection.getResponseCode());

  }

  protected Map<String,Object> getTopDoc(SearchResponse search)
  {
    for(SearchHit hit: search.getHits())
    {
      return hit.getSourceAsMap();
    }
    return null;
  }

  public StatusRegistry getReg(){return reg;}

  protected SearchResponse getLatest(String index_base, int results)
    throws Exception
  {
    return getLatest(index_base, results, null);

  }
  protected SearchResponse getLatest(String index_base, int results, Map<String, String> filter_terms)
    throws Exception
  {
    SearchRequest req = new SearchRequest(index_base+"-*");

    QueryBuilder qb = null;
    if (filter_terms !=null)
    {
      BoolQueryBuilder bqb =  QueryBuilders.boolQuery();
      for(Map.Entry<String, String> me : filter_terms.entrySet())
      {
        // I can't get the term search to actually work.
        // Also match query as keyword, should work but I can't get that to work either
        // So we are saying it has to have all of our terms (as it splits) or string
        // into segments and low fuzz.  Bah.
        // ok, keywork works when you set the query to be on ".keyword"

        bqb.must( QueryBuilders.matchQuery(me.getKey() +".keyword", me.getValue() )
          .operator( Operator.AND)
          .analyzer("keyword"));

        // Using term to get exact match
        //bqb.must( QueryBuilders.termQuery(me.getKey(), me.getValue() ) );
      }   
      qb = bqb;
    }
    //System.out.println(qb);
    req.source(
      new SearchSourceBuilder().size(results).sort("timestamp", SortOrder.DESC).query(qb)
      );
    //System.out.println(req);

    SearchResponse resp = es_client.search(req,RequestOptions.DEFAULT);

    //System.out.println(resp);
    return resp;

  }


  protected void publish(String topic_arn, String subject, String msg)
  {
    System.out.println("Publish: " + subject);
    sns.publish(topic_arn, msg, subject);

  }


  public class CloudMetricThread extends PeriodicThread
  {
    CWMetric cw_metric;

    public CloudMetricThread()
    {
      super(120000L);
      setName("CloudMetricThread");
      setDaemon(true);

      cw_metric = new CWMetric(config);

    }

    public void runPass()
    {
      cw_metric.saveMetric("lert","processing","1.0","Count");

    }


  }
}
