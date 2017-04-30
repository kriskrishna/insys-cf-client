# Cloud Foundry Firehose Nozzle for InfluxDB

This nozzle will take `ValueMetric` and `CounterEvent` metrics from the Cloud Foundry firehose and send them to an InfluxDB
Server for storage. It can be horizontally scaled, as long as the subscription ID (see below) is the same across all instances

## Build

`./mvnw clean package`

## Run

Accepted Environment Variables

Name | Desc | Type | Required | Default
--- | --- | --- | --- | ---
`INFLUXDB_NOZZLE_API_HOST` | The Cloud Countroller API Host | String | Y
`INFLUXDB_NOZZLE_CLIENT_ID` | The OAuth2 Client ID (must have `doppler.firehose` scope) | String | Y
`INFLUXDB_NOZZLE_CLIENT_SECRET` | The Secret for the above client | String | Y
`INFLUXDB_NOZZLE_FOUNDATION` | Identifying string to be added to all measurements as a tag | String | N | 
`INFLUXDB_NOZZLE_SKIP_SSL_VALIDATION` | Please don't | Boolean | Y | `false`
`INFLUXDB_NOZZLE_DB_NAME` | The Influx DB name (must exist) | String | Y | `metrics`
`INFLUXDB_NOZZLE_DB_HOST` | The Influx DB URL | String | Y | `http://localhost:8086`
`INFLUXDB_NOZZLE_BATCH_SIZE` | The batch size to be sent to Influx. Should be between 1 - 5000 | int | Y | 100
`INFLUXDB_NOZZLE_BACKOFF_POLICY` | How to backoff between retries (one of `exponential`, `linear`, or `random` | String | Y | `exponential`
`INFLUXDB_NOZZLE_MIN_BACKOFF` | Time in millis to wait between retries, at least | long | Y | 100
`INFLUXDB_NOZZLE_MAX_BACKOFF` | Time in millis to wait between retries, at most | long | Y | 30000
`INFLUXDB_NOZZLE_MAX_RETRIES` | Max number of retries before giving up | int | Y | 10
`INFLUXDB_NOZZLE_SUBSCRIPTION_ID` | The identifying string for the Cloud Foundry firehose. Must be the same across all instances, but must be unique from all other nozzles. | String | Y | `influxdb-nozzle`
`INFLUXDB_NOZZLE_TAG_FIELDS` | Metric fields that can be saved as InfluxDB tags. Current accepted values are `job`, `index`, `deployment`, `tags`, `delta`, `unit`. Must be sent as a JSON list. For example, `["job","index","unit"]` | String | Y | `[]`

**Note about Batch Size**: There is a direct correlation between the batch size and the amount of memory used. Running 
the app on Cloud Foundry, a batch size of 250 routinely uses around 550 MB RAM. A batch size of 500 will routinely go over
1 GB. 
