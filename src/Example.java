import java.util.HashMap;


public class Example {

	public static void main(String[] args) throws Exception {
		BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
		HashMap<String, Object> params = new HashMap<String, Object>();
		
		// build parameters to map
		params.put("rtn", "json");
		params.put("start", "2015-01-01");
		params.put("end", "2015-01-30");
		params.put("isOpened", true);
		
		String result = btag.request("tags", params);
		
		System.out.println(result);
	}

}
