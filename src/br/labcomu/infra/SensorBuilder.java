package br.labcomu.infra;

import org.fog.application.Application;
import org.fog.entities.Sensor;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class SensorBuilder {
    public interface DistributionFactory {
        public Distribution create(double transmissionInterval);
    }

    public static class DeterministicDistributionFactory implements DistributionFactory {
        @Override
        public Distribution create(double transmissionInterval) {
            return new DeterministicDistribution(transmissionInterval);
        }
    }


    private String tupleType;
    private double transmissionInterval;
    private DistributionFactory distributionFactory;

    public SensorBuilder() {
        this("SENSED_DATA", new DeterministicDistributionFactory(), 100);
    }

    private SensorBuilder(String tupleType, DistributionFactory distributionFactory, int transmissionInterval) {
        this.tupleType = tupleType;
        this.distributionFactory = distributionFactory;
        this.transmissionInterval = transmissionInterval;
    }

    public SensorBuilder setTupleType(String tupleType) {
        this.tupleType = tupleType;
        return this;
    }

    public SensorBuilder setTransmissionInterval(double transmissionInterval) {
        this.transmissionInterval = transmissionInterval;
        return this;
    }

    public SensorBuilder setDistributionFactory(DistributionFactory distributionFactory) {
        this.distributionFactory = distributionFactory;
        return this;
    }

    public Sensor build(String name, Application application) {
        int userId = application.getUserId();
        String appId = application.getAppId();

        Sensor sensor = new Sensor(name, this.tupleType, userId, appId, this.distributionFactory.create(this.transmissionInterval), application);
        return sensor;
    }
}
