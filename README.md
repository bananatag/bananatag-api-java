Java Library for the Bananatag API 
==================================

### Usage

#### Get All Opened Tags in Date-Range
```java
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;

BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
HashMap<String, Object> params = new HashMap<String, Object>();
JSONObject result = new JSONObject();

// build parameters to map
params.put("rtn", "json");
params.put("start", "2015-01-01");
params.put("end", "2015-01-30");
params.put("isOpened", true);

do {
	result = btag.request("tags", params);
	System.out.println(result);
	TimeUnit.SECONDS.sleep(1);
} while (!result.isEmpty());

```

#### Get Aggregate Stats Over Date-Range
```java
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;

BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
HashMap<String, Object> params = new HashMap<String, Object>();
JSONObject result = new JSONObject();


// build parameters to map
params.put("rtn", "json");
params.put("start", "2015-01-01");
params.put("end", "2015-01-30");
params.put("aggregateData", true);

do {
	result = btag.request("stats", params);
	System.out.println(result);
	TimeUnit.SECONDS.sleep(1);
} while (!result.isEmpty());
```

### License
Licensed under the MIT License.
