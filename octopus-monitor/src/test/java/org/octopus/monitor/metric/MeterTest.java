package org.octopus.monitor.metric;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class MeterTest {
    public static void main(String[] args) {
        MetricsRegistryManager.getInstance().register(MetricRegistryType.RPC, "com.octopus.monitor.rpc");

        MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(MetricRegistryType.RPC.getName());
        Meter meter = metricRegistry.meter("rpc.request");
        meter.mark();
    }
}
