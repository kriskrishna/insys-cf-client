/*******************************************************************************
 *  Copyright 2017 ECS Team, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  this file except in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 ******************************************************************************/

package com.insys.cfclient.config;

import com.insys.cfclient.nozzle.BackoffPolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "influxdb.nozzle")
public class NozzleProperties {
	/**
	 * The Cloud Controller host. Should be in the form `api.{{SYSTEM_DOMAIN}}`
	 */

	private String organization;

	private String space;

	private String apiHost;

	/**
	 * An OAuth client id whose scopes include `doppler.firehose`
	 */
	private String clientId;

	/**
	 * The secret for the above client
	 */
	private String clientSecret;

	/**
	 * The UAA Admin Client Id
	 */
	private String adminClientId;

	/**
	 * The UAA Admin Client Secret
	 */
	private String adminClientSecret;

	/**
	 * A unique subscription ID used by the Firehose. Instances of a nozzle with
	 * the same subscription id will have messages evenly spread across them.
	 */
	private String subscriptionId = "influxdb-nozzle";

	/**
	 * If set, will add an extra tag "foundation={value}" to every measurement
	 */
	private String foundation;

	/**
	 * The InfluxDB host URL
	 */
	private String dbHost = "http://localhost:8086";

	/**
	 * The DB name (which must exist)
	 */
	private String dbName = "metrics";

	/**
	 * The Batch size to be sent to influxdb. Should be < 5000 per Influx documentation
	 */
	private int batchSize = 100;

	/**
	 * The policy to use when backing off retries (exponential, linear, random)
	 */
	private BackoffPolicy backoffPolicy = BackoffPolicy.exponential;

	/**
	 * The min backoff time in ms
	 */
	private long minBackoff = 100L;

	/**
	 * The max backoff time in ms
	 */
	private long maxBackoff = 30000L;

	/**
	 * max number of retries
	 */
	private int maxRetries = 10;

	/**
	 * Skip SSL validation when connecting to the firehose
	 */
	private boolean skipSslValidation = false;

	/**
	 * whether or not fields in the Envelope should be tagged.
	 */
	private final List<String> tagFields = new ArrayList<>();

	public void setTagFields(String fieldJson) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		tagFields.addAll(mapper.readValue(fieldJson, new TypeReference<List<String>>() {}));
	}
}
