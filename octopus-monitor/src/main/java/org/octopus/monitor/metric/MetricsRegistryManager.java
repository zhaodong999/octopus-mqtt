package org.octopus.monitor.metric;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class MetricsRegistryManager {

    private static volatile MetricsRegistryManager instance;

    private MetricsRegistryManager() {
    }

    private static final ConcurrentMap<MetricRegistryType, ScheduledReporter> metricRegisterReporters = new ConcurrentHashMap<>();

    public static MetricsRegistryManager getInstance() {
        if (instance == null) {
            synchronized (MetricsRegistryManager.class) {
                if (instance == null) {
                    instance = new MetricsRegistryManager();
                }
            }
        }
        return instance;
    }

    public void register(MetricRegistryType metricRegisterType, String logName) {
        if (metricRegisterReporters.containsKey(metricRegisterType)) {
            return;
        }

        MetricRegistry registry = new MetricRegistry();
        SharedMetricRegistries.add(metricRegisterType.getName(), registry);
        final Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
                .outputTo(LoggerFactory.getLogger(logName))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
    }

    public void close() {
        metricRegisterReporters.values().forEach(
                ScheduledReporter::close
        );

        SharedMetricRegistries.clear();
    }

    public MetricRegistry getRegistry(MetricRegistryType metricRegisterType) {
        if (!metricRegisterReporters.containsKey(metricRegisterType)) {
            return null;
        }

        return SharedMetricRegistries.getOrCreate(metricRegisterType.getName());
    }
}

