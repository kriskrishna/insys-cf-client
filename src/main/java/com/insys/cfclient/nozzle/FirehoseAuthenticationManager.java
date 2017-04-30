/*
 * Copyright 2017 ECS Team, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.insys.cfclient.nozzle;

import com.insys.cfclient.config.NozzleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.uaa.UaaClient;
import org.cloudfoundry.uaa.clients.CreateClientRequest;
import org.cloudfoundry.uaa.clients.GetClientRequest;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.cloudfoundry.uaa.clients.UpdateClientRequest;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.CountDownLatch;

@RequiredArgsConstructor
@Slf4j
public class FirehoseAuthenticationManager implements SmartLifecycle {
	private boolean running = false;

	private final UaaClient uaaClient;
	private final NozzleProperties properties;

	private final CountDownLatch waiter = new CountDownLatch(1);

	private static final long ONE_YEAR_IN_SECONDS = 3600 * 24 * 365L;

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		callback.run();
		stop();
	}

	@Override
	public void start() {
		if (uaaClient == null) {
			return;
		}

		log.debug("Making sure that the requested client exists ...");

		GetClientResponse response;
		try {
			response = uaaClient.clients().get(
					GetClientRequest.builder()
							.clientId(properties.getClientId())
							.build()
			).block();
		} catch (Exception e) {
			createClient();
			return;
		}

		if (response != null) {
			updateClient(response);
		}

		try {
			waiter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		// needs to have a higher phase than FirehoseReader to ensure that the creds are created
		return -100;
	}

	private void createClient() {
		log.debug("This is likely because the client does not exist. Attempting to create...");
		uaaClient.clients().create(
				CreateClientRequest.builder()
						.clientId(properties.getClientId())
						.clientSecret(properties.getClientSecret())
						.authority("doppler.firehose", "cloud_controller.admin_read_only")
						.authorizedGrantType(GrantType.CLIENT_CREDENTIALS)
						.accessTokenValidity(ONE_YEAR_IN_SECONDS)
						.build()
		).doOnError(Throwable::printStackTrace).block();

		waiter.countDown();
	}

	private void updateClient(GetClientResponse response) {
		log.debug("The client was found, ensuring it has sufficient scope");
		uaaClient.clients().update(
				UpdateClientRequest.builder()
						.clientId(response.getClientId())
						.authority("doppler.firehose", "cloud_controller.admin_read_only")
						.authorizedGrantType(GrantType.CLIENT_CREDENTIALS)
						.build()
		).doOnError(Throwable::printStackTrace).block();

		waiter.countDown();
	}
}
