package com.insys.cfclient.nozzle;

import com.insys.cfclient.config.NozzleProperties;
import com.insys.cfclient.destination.MetricsDestination;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.doppler.ContainerMetric;
import org.cloudfoundry.doppler.CounterEvent;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.ValueMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Captures messages from the Cloud Foundry Firehose and batches them to be sent to InfluxDB
 */
@Service
@Slf4j
public class InfluxDBWriter {

	private final ResettableCountDownLatch latch;
	private final List<String> messages;
	private final List<String> tagFields;

	private String foundation;

	@Autowired
	public InfluxDBWriter(NozzleProperties properties, MetricsDestination destination, InfluxDBSender sender) {
		log.info("Initializing DB Writer with batch size {}", properties.getBatchSize());
		this.messages = Collections.synchronizedList(new ArrayList<>());
		this.latch = new ResettableCountDownLatch(properties.getBatchSize());
		this.tagFields = properties.getTagFields();

		this.foundation = properties.getFoundation();

		new Thread(new InfluxDBBatchListener(latch, messages, sender)).start();
	}

	/**
	 * Convert an envelope into an InfluxDB compatible message. In general, the format is
	 *
	 * <tt>message[,tag=value]* value timestamp</tt>
	 *
	 * Add each message String to a batch list and count down a latch. When the latch reaches 0,
	 * it will write to InfluxDB and reset.
	 *
	 * @param envelope The event from the Firehose
	 */
	@Async
	void writeMessage(Envelope envelope) {
		final StringBuilder messageBuilder = new StringBuilder();
		switch (envelope.getEventType()) {
			case VALUE_METRIC:
				writeValueMetric(messageBuilder, envelope);
				log.debug(envelope.getEventType() + " Origin " + envelope.getOrigin());
				break;
			case COUNTER_EVENT:
				writeCounterEventTotal(messageBuilder, envelope);
				log.debug(envelope.getEventType() + " Origin " + envelope.getOrigin());
				if (isTaggableField("delta")) {
					writeCounterEventDelta(messageBuilder, envelope);
				}
				break;
			case CONTAINER_METRIC:
				log.debug(envelope.getEventType() + " Origin " + envelope.getOrigin());
				writeContainerMetric(messageBuilder, envelope);
				break;
			case LOG_MESSAGE:
				log.debug(envelope.getEventType() + " Origin " + envelope.getOrigin() + " " + envelope.getLogMessage());
				break;
		}
	}

	private void writeCommonSeriesData(StringBuilder messageBuilder, Envelope envelope,
									   String metricName, Map<String, String> tags) {
		messageBuilder.append(envelope.getOrigin()).append('.').append(metricName);
		tags.forEach((k, v) -> messageBuilder.append(",").append(k).append("=").append(v));
	}

	private void finishMessage(StringBuilder messageBuilder, Envelope envelope) {
		messageBuilder.append(' ').append(envelope.getTimestamp());

		messages.add(messageBuilder.toString());
		latch.countDown();

		messageBuilder.delete(0, messageBuilder.length());
	}

	private void writeContainerMetric(StringBuilder messageBuilder, Envelope envelope) {
		ContainerMetric metric = envelope.getContainerMetric();

		if (metric != null) {
			Map<String, String> tags = getTags(envelope);

			writeCommonSeriesData(messageBuilder, envelope, "ContainerMetric", tags);

			Map<String, Number> values = new LinkedHashMap<>();
			values.put("instanceIndex", metric.getInstanceIndex());
			values.put("cpuPercentage", metric.getCpuPercentage());
			values.put("diskBytes", metric.getDiskBytes());
			values.put("diskBytesQuota", metric.getDiskBytesQuota());
			values.put("memoryBytes", metric.getMemoryBytes());
			values.put("memoryBytesQuota", metric.getMemoryBytesQuota());

			values.forEach((k,v) -> messageBuilder.append(" ").append(k).append("=").append(v));
			finishMessage(messageBuilder, envelope);
		}
	}

	private void writeValueMetric(StringBuilder messageBuilder, Envelope envelope) {
		ValueMetric metric = envelope.getValueMetric();

		if (metric != null) {
			Map<String, String> tags = getTags(envelope);
			tags.put("eventType", "ValueMetric");
			writeCommonSeriesData(messageBuilder, envelope, metric.getName(), tags);
			messageBuilder.append(" value=").append(metric.value());
			finishMessage(messageBuilder, envelope);
		}
	}

	private void writeCounterEventTotal(StringBuilder messageBuilder, Envelope envelope) {
		CounterEvent event = envelope.getCounterEvent();

		if (event != null) {
			Map<String, String> tags = getTags(envelope);
			tags.put("eventType", "CounterEvent");
			tags.put("valueType", "total");
			writeCommonSeriesData(messageBuilder, envelope, event.getName(), tags);
			messageBuilder.append(" value=").append(event.getTotal());
			finishMessage(messageBuilder, envelope);
		}
	}

	private void writeCounterEventDelta(StringBuilder messageBuilder, Envelope envelope) {
		CounterEvent event = envelope.getCounterEvent();

		if (event != null) {
			Map<String, String> tags = getTags(envelope);
			tags.put("eventType", "CounterEvent");
			tags.put("valueType", "delta");
			writeCommonSeriesData(messageBuilder, envelope, event.getName(), tags);
			messageBuilder.append(" value=").append(event.getDelta());
			finishMessage(messageBuilder, envelope);
		}
	}

	/**
	 * Get all the tags from the Envelope plus any EventType-specific fields into a single Map
	 *
	 * @param envelope the Event
	 * @return the tag map
	 */
	private Map<String, String> getTags(Envelope envelope) {
		final Map<String, String> tags = new HashMap<>();

		if (StringUtils.hasText(foundation)) {
			tags.put("foundation", foundation);
		}

		if (isTaggableField("tags") && !CollectionUtils.isEmpty(envelope.getTags())) {
			envelope.getTags().forEach((k, v) -> {
				if (StringUtils.hasText(v)) {
					tags.put(k, v);
				}
			});
		}


		if (isTaggableField("ip") && StringUtils.hasText(envelope.getIp())) {
			tags.put("ip", envelope.getIp());
		}

		if (isTaggableField("deployment") && StringUtils.hasText(envelope.getDeployment())) {
			tags.put("deployment", envelope.getDeployment());
		}

		if (isTaggableField("job") && StringUtils.hasText(envelope.getJob())) {
			tags.put("job", envelope.getJob());
		}

		if (isTaggableField("index") && StringUtils.hasText(envelope.getIndex())) {
			tags.put("index", envelope.getIndex());
		}

		if (envelope.getValueMetric() != null) {
			if (isTaggableField("unit") && StringUtils.hasText(envelope.getValueMetric().getUnit())) {
				tags.put("unit", envelope.getValueMetric().getUnit());
			}
		}

		return tags;
	}

	private boolean isTaggableField(String field) {
		return StringUtils.hasText(field) && tagFields.contains(field);
	}
}