import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

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
	 * A copy of the previous params map
	 * @type Map<String, Object>
	 * @access private
	 */
	private Map<String, Object> currentMap;
	
	/**
	 * The last endpoint that was requested
	 * @type String
	 * @access private
	 */
	private String currentEndpoint;
	
	/**
	 * The next pagination link from the previous request
	 * @type String
	 * @access private 
	 */
	private String nextUrl;
	
	/**
	 * Flag to indicate if its the same request as the last
	 * @type boolean
	 * @access private
	 */
	private boolean useNext;
	
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
		this.base_url = "http://ec2-52-11-177-28.us-west-2.compute.amazonaws.com:8080/";//"https://api.bananatag.com/";
		this.currentMap = new HashMap<String, Object>();
		this.currentEndpoint = "";
		this.nextUrl = "";
		this.useNext = false;
	}
	
	/**
	 * Make API request and get JSON object back
	 * @param endpoint
	 * @param params
	 * @return JSONObject
	 * @throws Exception 
	 */
	public JSONObject request(String endpoint, Map<String, Object> params) throws Exception {
		String sig = "";
		String data_string = "";
		String result = "";
		
		this.useNext = false;
		
		if (this.currentEndpoint == endpoint) {
			if (this.currentMap.equals(params)) {
				this.useNext = true;
			} else {
				this.currentMap = params;
			}
		} else {
			this.currentEndpoint = endpoint;
			this.currentMap = params;
		}
		
		if (this.useNext) {
			String[] parts = this.nextUrl.split("\\?");
			data_string = parts[1];
		} else {
			this.checkData(params);
			data_string = this.buildDatastring(params);
		}
		
		sig = this.generateSignature(data_string);
		result = this.makeRequest(endpoint, sig, data_string);
		
		try {
			JSONObject json = (JSONObject) new JSONParser().parse(result);
			JSONObject paging = (JSONObject) json.get("paging");					
			this.nextUrl = (String) paging.get("nextURL");
					
			return json;
		} catch (Exception e) {
			// no data was found, reached the end of the result set.
			return (JSONObject) new JSONParser().parse("{}");
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	@SuppressWarnings({ "unchecked", "unchecked" })
	public String buildMessage(Map<String, Object> params) throws IOException, MessagingException {
		boolean hasAttachments = false;
				
		if (!params.containsKey("from")) {
			throw new IOException("You must include the 'from' parameter for each attachment.");
		}
		
		if (!params.containsKey("to")) {
			throw new IOException("You must include the 'to' parameter for each attachment.");
		}
		
		if (!params.containsKey("html")) {
			throw new IOException("You must include the 'html' parameter for each attachment.");
		}
		
		// if there are attachments
		if (params.containsKey("attachments")) {
			// check if attachments were passed as a list
			if (!(params.get("attachments") instanceof ArrayList)) {
				throw new IOException("The attachments parameter must be an ArrayList of 'HashMap<String, String>' objects.");
			} else {
				hasAttachments = true;
			}
		}
		
		Properties props = new Properties();
		Session mailConnection = Session.getInstance(props, null);
		MimeMessage message = new MimeMessage(mailConnection);
				
		// add recipients to message
		String[] to = (String[]) params.get("to");
		for (int i = 0; i < to.length; i++) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress((String) to[i]));
		}
		
		// add from address to message
		Address from = new InternetAddress((String) params.get("from"));
		message.setFrom(from);
		
		// create the message body part 
	    MimeBodyPart messageBodyPart = new MimeBodyPart();
	    messageBodyPart.setContent(message, "text/html");
	    messageBodyPart.setText((String) params.get("html"), "utf8", "html");
	    messageBodyPart.setHeader("Content-Transfer-Encoding", "base64");
	    Multipart multipart = new MimeMultipart("mixed");
	    multipart.addBodyPart(messageBodyPart);
		
	    // add subject if provided
		if (params.containsKey("subject")) {
			message.setSubject((String) params.get("subject"));
		}
		
		// add any cc if provided
		if (params.containsKey("cc")) {
			String[] cc = (String[]) params.get("cc");
			
			for (int j = 0; j < cc.length; j++) {
				message.addRecipient(Message.RecipientType.CC, new InternetAddress((String) cc[j]));
			}			
		}
		
		// add any bcc if provided
		if (params.containsKey("bcc")) {
			String[] bcc = (String[]) params.get("bcc");
			
			for (int k = 0; k < bcc.length; k++) {
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress((String) bcc[k]));
			}			
		}
		
		// add all custom headers if any are provided
		if (params.containsKey("headers")) {
			HashMap<String, String> headers = (HashMap<String, String>) params.get("headers");
			for (Map.Entry<String, String> header : headers.entrySet()) 
			{ 
				message.addHeader(header.getKey(), header.getValue());
			}
		}
		
		if (hasAttachments) {			
			ArrayList<HashMap<String, String>> attachments = (ArrayList<HashMap<String, String>>) params.get("attachments");
			for (HashMap<String, String> attachment: attachments) {
				
				messageBodyPart = new MimeBodyPart();
			   			    			    
			    if (!attachment.containsKey("disposition")) {
			    	throw new IOException("You must specify a disposition (attachment or inline) for each attachment.");
			    }
			    
			    File attachedFile = new File(attachment.get("filepath"));
			    messageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(new FileInputStream(attachedFile), "application/octet-stream")));
			    
			    if (attachment.get("disposition") == "inline") {
				    if (!attachment.containsKey("contentID")) {
				    	throw new IOException("You must provide a value for the 'contentID' parameter for inline attachments.");
				    }
			    	
				    messageBodyPart.setContentID("<" + attachment.get("contentID") + ">");
			    	messageBodyPart.setDisposition(MimeBodyPart.INLINE);
			    } else {
				    if (attachment.containsKey("contentID")) {
				    	messageBodyPart.setContentID("<" + attachment.get("contentID") + ">");
				    }
			    	
			    	messageBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
			    }
			   
			    messageBodyPart.setFileName(attachedFile.getName());
			    multipart.addBodyPart(messageBodyPart);	
			}
		}
		
	    // Put parts in message
	    message.setContent(multipart);
	    
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    message.writeTo(byteArrayOutputStream);
	    
	    return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());	    
	}
	
	/**
	 * 
	 * @param endpoint
	 * @param sig
	 * @param data_string
	 * @return
	 * @throws Exception
	 */
	private String makeRequest(String endpoint, String sig, String data_string) throws Exception {
		String method = getMethod(endpoint);
		String authorization_header = Base64.getEncoder().encodeToString((this.auth_id + ":" + sig).getBytes());
		
		if (method == "POST") {
			return this.sendPost(authorization_header, endpoint, data_string);			
		} else {
			return this.sendGet(authorization_header, endpoint, data_string);
		}
	}
	
	/**
	 * Make GET Request
	 * @param authorization_header
	 * @param endpoint
	 * @param data_string
	 * @return String
	 * @throws Exception 
	 */
	private String sendGet(String authorization_header, String endpoint, String data_string) throws Exception {
		String inputLine;
		StringBuffer response = new StringBuffer();
		URL obj;
		
		if (this.useNext) {
			obj = new URL(this.nextUrl);
		} else {
			obj = new URL(this.base_url + endpoint + "?" + data_string);	
		}

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
	
	/**
	 * 
	 * @param authorization_header
	 * @param endpoint
	 * @param data_string
	 * @return
	 * @throws Exception
	 */
	private String sendPost(String authorization_header, String endpoint, String data_string) throws Exception { 
		String inputLine;
		StringBuffer response = new StringBuffer();
		URL obj = new URL(this.base_url + endpoint);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 
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
	 * Validate provided request parameters
	 * @param params
	 * @return void
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
	
	/**
	 * Validate dates
	 * @param dateToValidate
	 * @return boolean
	 * @throws IOException
	 */
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
	 * Generate authorization signature using provided data string.
	 * @param data_string
	 * @return String
	 * @throws java.security.SignatureException
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
	 * Build date string from provided parameters
	 * @param params
	 * @return String
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
		case "tags/send": return "POST";
		default: return "GET";
		}
	}
}
