import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

public class BtagAPI {

	/**
	 * The users unique authentication id
	 * @type String
	 * @access private
	 */
	private String auth_id;
	
	/**
	 * The users unique assess key
	 * @type String
	 * @access private
	 */
	private String access_key;
	
	/**
	 * The the base URL where the REST API lives
	 * @type String
	 * @access private
	 */
	private String base_url;
	
	
	/**
	 * 
	 * @param id
	 * @param key
	 * @param debug
	 * @throws IOException
	 */
	public BtagAPI(String id, String key) throws IOException {
		if (id == null || key == null) {
			throw new IOException("You must provide both an authID and access key.");
		}
		
		this.auth_id = id;
		this.access_key = key;
		this.base_url = "https://api.bananatag.com/";
	}
	
	/**
	 * 
	 * @param endpoint
	 * @param params
	 * @return 
	 * @throws Exception 
	 */
	public String request(String endpoint, Map<String, Object> params) throws Exception {
		this.checkData(params);
		
		String data_string = this.buildDatastring(params);
		String sig = this.generateSignature(data_string);
		String method = this.getMethod(endpoint);
		
		return this.makeRequest(endpoint, method, sig, data_string);
	}
	
	private String makeRequest(String endpoint, String method, String sig, String data_string) throws Exception {
		String result = "";
		String authorization_header = Base64.getEncoder().encodeToString((this.auth_id + ":" + sig).getBytes());
		
		if (method == "GET") {
			result = this.sendGet(authorization_header, endpoint, data_string);
		} else {
			result = this.sendPost(authorization_header, endpoint, data_string);
		}
		
		return result;
		
	}
	
	private String sendGet(String authorization_header, String endpoint, String data_string) throws Exception {
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		URL obj = new URL(this.base_url + endpoint + "?" + data_string);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		con.setRequestMethod("GET");
		con.setRequestProperty("authorization", authorization_header);
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		in.close();
 
		return response.toString();

	}
 
	private String sendPost(String authorization_header, String endpoint, String data_string) throws Exception { 
		String inputLine;
		StringBuffer response = new StringBuffer();
		URL obj = new URL(this.base_url + endpoint);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
 
		con.setRequestMethod("POST");
		con.setRequestProperty("authorization", authorization_header);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
		String urlParameters = data_string;
 
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
  
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		in.close();
 
		return response.toString();

	}
	
	/**
	 * 
	 * @param params
	 * @throws IOException 
	 */
	private void checkData(Map<String, Object> params) throws IOException {
		if (params.containsKey("start")) {
			this.validateDate((String) params.get("start"));
		}
		
		if (params.containsKey("end")) {
			this.validateDate((String) params.get("end"));
		}
	}
	
	private boolean validateDate(String dateToValidate) throws IOException { 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
		sdf.setLenient(false);
 
		try {
			// if not valid, it will throw error
			sdf.parse(dateToValidate);
		} catch (ParseException e) {
			throw new IOException("Error with provided parameters: Date string must be in format yyyy-mm-dd.");
		}
 
		return true;
	}
	
	/**
	 * 
	 * @param params
	 * @return
	 */	
	private String generateSignature(String data_string) throws java.security.SignatureException {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(this.access_key.getBytes(), "HmacSHA1");	
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
	
			byte[] rawHmac = mac.doFinal(data_string.getBytes());
	
			return javax.xml.bind.DatatypeConverter.printHexBinary(rawHmac).toLowerCase();
		} catch (Exception e) {
			throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
		}		
	}
	
	/**
	 * 
	 * @param params
	 * @return
	 */
	private String buildDatastring(Map<String, Object> params) {
		String data_string = "";
		
		if (params.size() > 0) {
			int count = 0;
			
			for (Map.Entry<String, Object> entry : params.entrySet()) 
			{ 
				if (count == 0) {
					data_string = entry.getKey() + "=" + entry.getValue();
					count += 1;
				} else {
					data_string += "&" + entry.getKey() + "=" + entry.getValue();
				}
			}
		}
				
		return data_string;
	}
	
	/**
	 * 
	 * @param endpoint
	 * @return
	 */
	private String getMethod(String endpoint) {
		switch(endpoint) {
			case "": return "PUT";
			default: return "GET";
		}
	}
	
	
	
}
