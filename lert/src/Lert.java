package duckutil.lert;

import duckutil.Config;
import duckutil.ConfigFile;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import java.net.URL;
import org.elasticsearch.action.search.SearchRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.elasticsearch.search.SearchHit;
import java.util.TreeSet;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.action.search.SearchResponse;
import java.util.ArrayList;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateAction;
import java.net.HttpURLConnection;
import net.minidev.json.JSONObject;
import java.util.Date;
import java.time.ZonedDateTime;
import java.io.OutputStream;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.auth.BasicAWSCredentials;

public class Lert
{
  public static void main(String args[]) throws Exception
  {
    new Lert(new ConfigFile(args[0]));
  }

  private final Config config;
  private final RestHighLevelClient es_client;
  private final AmazonSNSClient sns;

  public Lert(Config config) throws Exception
  {
    this.config = config;
    config.require("elasticsearch_url");
    config.require("alert_topic_arn");
    config.require("aws_key_id");
    config.require("aws_secret");

    es_client = openElasticSearchClient();

    String arn = config.get("alert_topic_arn");
    LertConfig.setGlobalTopicArn(arn);

    sns = new AmazonSNSClient(new BasicAWSCredentials(config.get("aws_key_id"), config.get("aws_secret")));
    sns.setEndpoint("sns.us-west-2.amazonaws.com");

    new LertThread(new LertAgent(this, new ConfigFreezer("garage","freezer"))).start();
    new LertThread(new LertAgent(this, new ConfigFreezer("general_stores","freezer"))).start();
    
    new LertThread(new LertAgent(this, new ConfigHumidity("server_room","room_air"))).start();
    new LertThread(new LertAgent(this, new ConfigHumidity("garage","room_air"))).start();

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
    System.out.println(doc.toJSONString());

    System.out.println("POST code: " + connection.getResponseCode());

  }

  protected Map<String,Object> getTopDoc(SearchResponse search)
  {
    for(SearchHit hit: search.getHits())
    {
      return hit.getSourceAsMap();
    }
    return null;
  }

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
        bqb.filter( QueryBuilders.termQuery(me.getKey(), me.getValue() ) );
      }   
      qb = bqb;
    }
    req.source(
      new SearchSourceBuilder().size(results).sort("timestamp", SortOrder.DESC).query(qb)
      );

    return es_client.search(req,RequestOptions.DEFAULT);

  }


  protected void publish(String topic_arn, String subject, String msg)
  {
    sns.publish(topic_arn, msg, subject);

  }

}
