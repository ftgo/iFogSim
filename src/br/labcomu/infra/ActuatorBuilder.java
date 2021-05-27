package br.labcomu.infra;

import org.fog.application.Application;
import org.fog.entities.Actuator;

import static br.labcomu.infra.Simulation.ACTUATOR_TYPE;

public class ActuatorBuilder {
    private final Simulation simulation;

    private String actuatorType;

    public ActuatorBuilder(Simulation simulation) {
        this(simulation, ACTUATOR_TYPE);
    }

    private ActuatorBuilder(Simulation simulation, String actuatorType) {
        this.simulation = simulation;
        this.actuatorType = actuatorType;
    }

    public ActuatorBuilder setActuatorType(String actuatorType) {
        this.actuatorType = actuatorType;
        return this;
    }

    public Actuator build(String suffix) {
        Application application = this.simulation.getApplication();

        int userId = application.getUserId();
        String appId = application.getAppId();

        Actuator actuator = new Actuator("A-" + suffix, userId, appId, this.actuatorType, application);

        this.simulation.addActuator(actuator);

        return actuator;
    }
}
