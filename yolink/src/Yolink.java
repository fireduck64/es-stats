
import duckutil.PeriodicThread;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class Yolink
{
  Random rnd=new Random();

  public static void main(String args[]) throws Exception
  {
    new Yolink();
  }

  private final String auth_uaid;
  private final String auth_key;

  private String token;
  private long token_expire;

  private JSONArray dev_list;
  private long dev_list_expire;

  public Yolink()
    throws Exception
  {

    auth_uaid = System.getenv("UAID");
    auth_key = System.getenv("KEY");
    if ((auth_uaid == null) || (auth_key == null))
    {
      throw new RuntimeException("Must set UAID and KEY");
    }
    token_expire=0L;
    dev_list_expire=0L;

    System.out.println("Starting gather thread");
    new GatherThread().start();

  }

  public static final long UPDATE_INTERVAL_MS=300L * 1000L;
  public static final long DEV_LIST_UPDATE_INTERVAL_MS=1800L * 1000L;
  public static final long TOKEN_UPDATE_WIGGLE=60 * 1000L;

  public class GatherThread extends PeriodicThread
  {
    public GatherThread()
    {
      super(UPDATE_INTERVAL_MS);
      setName("Yolink.GatherThread");

    }

    public void runPass() throws Exception
    {
      updateToken();
      updateDevList();

      for(Object o : dev_list)
      {
        JSONObject jo = (JSONObject) o;

        String type = (String) jo.get("type");
        if (type.equals("THSensor"))
        {
          recordDataTH(jo);
        }
      }
    }
  }

  void updateDevList()
    throws Exception
  {
    if (dev_list_expire > System.currentTimeMillis()) return;

    JSONObject list_req = new JSONObject();
    list_req.put("method", "Home.getDeviceList");
    JSONObject list = doYolinkQuery(list_req);
    //System.out.println(list.toString());

    JSONObject data = (JSONObject) list.get("data");

    dev_list = (JSONArray) data.get("devices");

    TreeMap<String, Integer> count_type = new TreeMap<>();
    for(Object o : dev_list)
    {
      JSONObject jo = (JSONObject) o;

      String type = (String) jo.get("type");
      if (!count_type.containsKey(type)) count_type.put(type, 0);
      count_type.put(type, count_type.get(type) + 1);
    }
    System.out.println("Device type counts: " + count_type);

    dev_list_expire = System.currentTimeMillis() + DEV_LIST_UPDATE_INTERVAL_MS;

  }


  void recordDataTH(JSONObject sensor_json)
    throws Exception
  {
    JSONObject th_req = new JSONObject();
    th_req.put("method", "THSensor.getState");
    th_req.put("targetDevice", (String) sensor_json.get("deviceId"));
    th_req.put("token", (String) sensor_json.get("token"));

    JSONObject th_data = doYolinkQuery(th_req);

    String name = (String)sensor_json.get("name");

    name = name.toLowerCase();
    name = name.replace(" ", "-");

    JSONObject data = (JSONObject) th_data.get("data");

    System.out.println("TH name: " + name);
    boolean online = (boolean) data.get("online");
    if (!online)
    {
      System.out.println("  Sensor is offline - not recording");
      return;
    }
    JSONObject state = (JSONObject) data.get("state");

    double temp_c = Double.parseDouble("" + state.get("temperature"));
    double hum = Double.parseDouble("" + state.get("humidity"));

    TreeMap<String, String> post_data = new TreeMap<>();

    post_data.put("name", name);
    post_data.put("temp_c", "" + temp_c);
    post_data.put("humidity", "" + hum);

    sendReportLocal(post_data);

    System.out.println("TH data: " + post_data);



  }

  JSONObject doYolinkQuery(JSONObject input)
    throws Exception
  {
    input.put("time", System.currentTimeMillis() / 1000);

    byte[] send = input.toString().getBytes("UTF-8");

    URL u = new URL("https://api.yosmart.com/open/yolink/v2/api");
    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setRequestProperty("Authorization", "Bearer " + token);
    connection.setRequestProperty("Content-Length", "" + Integer.toString(send.length));

    OutputStream wr = connection.getOutputStream ();
    wr.write(send);
    wr.flush();
    wr.close();

    StringBuilder sb = new StringBuilder();
    Scanner scan = new Scanner(connection.getInputStream());
    while(scan.hasNextLine())
    {
      sb.append(scan.nextLine());
      sb.append('\n');
    }

    JSONObject resp = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(sb.toString());

    return resp;

  }

  void updateToken()
    throws Exception
  {
    if (token_expire > System.currentTimeMillis() + TOKEN_UPDATE_WIGGLE)
    {
      return;
    }

    // curl -X POST -d "grant_type=client_credentials&client_id=${UAID}&client_secret=${KEY}" https://api.yosmart.com/open/yolink/token
    String post_data = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s", auth_uaid, auth_key);

    byte[] send = post_data.getBytes("UTF-8");

    URL u = new URL("https://api.yosmart.com/open/yolink/token");
    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Length", "" + Integer.toString(send.length));

    OutputStream wr = connection.getOutputStream ();
    wr.write(send);
    wr.flush();
    wr.close();

    StringBuilder sb = new StringBuilder();
    Scanner scan = new Scanner(connection.getInputStream());
    while(scan.hasNextLine())
    {
      sb.append(scan.nextLine());
      sb.append('\n');
    }

    JSONObject tok_json = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(sb.toString());

    token = (String) tok_json.get("access_token");
    long expire_interval = (int) tok_json.get("expires_in");
    token_expire = System.currentTimeMillis() + expire_interval * 1000L;
    System.out.println("Token updated");


  }


  int sendReportLocal(Map<String, String> query_map)
    throws Exception
  {
    JSONObject post = new JSONObject();

    post.putAll(query_map);

    String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());

    System.out.println("Report: " + post);

    byte[] send = post.toString().getBytes();

    URL u = new URL(String.format("http://metrics.1209k.com:9200/yolink-%s/_doc", date));
    HttpURLConnection connection = (HttpURLConnection) u.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestProperty("Content-Length", "" + Integer.toString(send.length));
    connection.setRequestProperty("Content-Type","application/json");


    OutputStream wr = connection.getOutputStream ();
    wr.write(send);
    wr.flush();
    wr.close();

    return connection.getResponseCode();

  }

}
