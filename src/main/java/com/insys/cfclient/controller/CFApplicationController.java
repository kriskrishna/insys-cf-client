package com.insys.cfclient.controller;


import com.insys.cfclient.config.CxpCFApp;
import com.insys.cfclient.config.JsonMessage;
import com.insys.cfclient.config.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.clients.GetClientRequest;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CFApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(CFApplicationController.class);

    private final ReactorDopplerClient dopplerClient;
    private final ReactorUaaClient reactorUaaClient;
    private final ReactorCloudFoundryClient reactorCloudFoundryClient;
    private final DefaultCloudFoundryOperations defaultCloudFoundryOperations;

    /*
     * This endpoint receives GET requests with two optional parameters appId and appName.
     */
    @GetMapping("/cfapp")
    public ResponseEntity<JsonMessage> getApplication(
            @RequestParam(value = "appId", required = false) String appId,
            @RequestParam(value = "appName", required = false) String appName) {

        logger.debug("Enter: getAppById()********");
        if (StringUtils.isEmpty(appId) && StringUtils.isEmpty(appName)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new TextMessage("Please provide appId or appName as query parameter."));
        }
        Optional<CxpCFApp> app;
        if (!StringUtils.isEmpty(appId)) {
            app = Optional.of(new CxpCFApp(appId, reactorCloudFoundryClient.applicationsV2()
                    .get(GetApplicationRequest.builder().applicationId(appId).build())
                    .block()
                    .getEntity().getName(), 0));
        } else {
            app = getAllApplications().stream().filter(a -> a.getName().equals(appName)).findFirst();
        }
        if (app.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(app.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new TextMessage("Application not found"));
    }


    public List<CxpCFApp> getAllApplications() {
        logger.debug("Enter: getAllApplications()");
        List<CxpCFApp> apps =
                defaultCloudFoundryOperations.applications()
                        .list()
                        .collectList()
                        .block()
                        .stream()
                        .map(app -> new CxpCFApp(app.getId(), app.getName(), app.getInstances())).collect(Collectors.toList());

        logger.debug("Return: getAllApplications()" + apps);
        return apps;
    }

    @GetMapping("/cfapps")
    public ResponseEntity<List<CxpCFApp>> getApplications() {
        logger.debug("Get all apps");
        List<CxpCFApp> cxpCFApps = getAllApplications();
        return ResponseEntity.status(HttpStatus.OK).body(cxpCFApps);
    }


    @GetMapping("/uaaclient")
    public GetClientResponse  getUAAClients() {
        GetClientResponse response;
        logger.debug("Enter: getUser()");

        response = reactorUaaClient.clients().get(
                GetClientRequest.builder()
                        .clientId("insysadmin")
                        .build()
        ).block();

        logger.debug("Return: getUser()" + response);
        return response;
    }



}
