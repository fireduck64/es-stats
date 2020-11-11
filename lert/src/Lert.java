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

    new LertThread(new LertAgent(this, new ConfigFreezer("garage","freezer"))).start();
    new LertThread(new LertAgent(this, new ConfigFreezer("general_stores","freezer"))).start();
    
    new LertThread(new LertAgent(this, new ConfigHumidity("server_room","room_air"))).start();
    new LertThread(new LertAgent(this, new ConfigHumidity("garage","room_air"))).start();
    new LertThread(new LertAgent(this, new ConfigServerRoom("server_room","room_air"))).start();
    new LertThread(new LertAgent(this, new ConfigGarageRoom("garage","room_air"))).start();
    
    new LertThread(new LertAgent(this, new ConfigWeb("fireduck.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("explorer.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("wiki.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigWeb("chan-relay.snowblossom.org"))).start();

    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snowblossom"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snow-test-a"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("teapot","snow-test-b"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-a.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-b.1209k.com"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-tx1.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","snow-de1.snowblossom.org"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","ryzen"))).start();
    new LertThread(new LertAgent(this, new ConfigSnow("snowblossom","ogog"))).start();
    
    new LertThread(new LertAgent(this, new ConfigProcess("bitcoin-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("bitcoin-cash-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("ethereum-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("snowblossom-price-notify"))).start();

    new LertThread(new LertAgent(this, new ConfigProcess("stock-price-notify"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-tracker"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-watcher"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("allseeingeye-topicload"))).start();
    new LertThread(new LertAgent(this, new ConfigProcess("snow-faucet"))).start();
    
    new LertThread(new LertAgent(this, new ConfigGood("backup","lamp"))).start();
    new LertThread(new LertAgent(this, new ConfigAge("backup","lamp"))).start();
    new LertThread(new LertAgent(this, new ConfigBattery("sensor.test_sensor_a_battery_level"))).start();

    //new LertThread(new LertAgent(this, new ConfigAge("dropbox","ogog"))).start();

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

}
