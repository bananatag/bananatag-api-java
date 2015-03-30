import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;


public class Example {

	public static void main(String[] args) throws Exception {
		JSONObject result = new JSONObject();
		BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
		HashMap<String, Object> params = new HashMap<String, Object>();
		
		//build parameters to map
		params.put("isOpened", true);
		
		do {
			result = btag.request("tags", params);
			System.out.println(result);
			TimeUnit.SECONDS.sleep(1);
		} while (!result.isEmpty());
		

	}
}
