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

import com.insys.cfclient.destination.StandaloneInfluxDbDestination;
import com.insys.cfclient.destination.TileDeployedInfluxDbDestination;
import com.insys.cfclient.destination.MetricsDestination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by josh on 2/18/17.
 */
@Configuration
@EnableConfigurationProperties(NozzleProperties.class)
public class DestinationConfig {

	/**
	 * When an app is deployed in a tile context, it always has the ${CC_HOST} environment variable set
	 *
	 * @param apiEndpoint
	 * @return
	 */
	@Bean
	@Profile("tile")
	@Autowired
	MetricsDestination tileDestination(@Value("${cc.host}") String apiEndpoint) {
		return new TileDeployedInfluxDbDestination(apiEndpoint);
	}

	@Bean
	@Profile("!tile")
	@Autowired
	MetricsDestination standaloneDestination(NozzleProperties properties) {
		return new StandaloneInfluxDbDestination(properties);
	}
}
