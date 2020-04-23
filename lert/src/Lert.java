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
import java.util.TreeSet;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.action.search.SearchResponse;
import java.util.ArrayList;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.sort.SortOrder;


public class Lert
{
  public static void main(String args[]) throws Exception
  {
    new Lert(new ConfigFile(args[0]));
  }

  private final Config config;
  private final RestHighLevelClient es_client;

  public Lert(Config config) throws Exception
  {
    this.config = config;
    config.require("elasticsearch_url");

    es_client = openElasticSearchClient();

    System.out.println(getLatest("dropbox", 1));


    es_client.close();
    

  }

  protected RestHighLevelClient openElasticSearchClient()
    throws Exception
  {
    URL u = new URL(config.get("elasticsearch_url"));
    
    return new RestHighLevelClient(
      RestClient.builder(new HttpHost(u.getHost(),u.getPort()))
      );
  }

  protected SearchResponse getLatest(String index_base, int results)
    throws Exception
  {
    SearchRequest req = new SearchRequest(index_base+"-*");
    req.source(
      new SearchSourceBuilder().size(results).sort("timestamp", SortOrder.DESC)
      );

    return es_client.search(req,RequestOptions.DEFAULT);

    
    
    

  }

}
