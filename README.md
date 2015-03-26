Java Library for the Bananatag API 
==================================

### Usage

#### Get All Opened Tags in Date-Range
```java
BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
HashMap<String, Object> params = new HashMap<String, Object>();

// build parameters to map
params.put("rtn", "json");
params.put("start", "2015-01-01");
params.put("end", "2015-01-30");
params.put("isOpened", true);

String result = btag.send("tags", params);

System.out.println(result);
```

#### Get Aggregate Stats Over Date-Range
```java
BtagAPI btag = new BtagAPI("Your AuthID", "Your Access Key");
HashMap<String, Object> params = new HashMap<String, Object>();

// build parameters to map
params.put("rtn", "json");
params.put("start", "2015-01-01");
params.put("end", "2015-01-30");
params.put("aggregateData", true);

String result = btag.send("stats", params);

System.out.println(result);
```

### License
Licensed under the MIT License.
