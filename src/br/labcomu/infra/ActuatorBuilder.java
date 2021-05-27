package br.labcomu.infra;

import org.fog.application.Application;
import org.fog.entities.Actuator;

public class ActuatorBuilder {

    private final Simulation simulation;

    private String actuatorType;

    public ActuatorBuilder(Simulation simulation) {
        this(simulation, "ACTION");
    }

    private ActuatorBuilder(Simulation simulation, String actuatorType) {
        this.simulation = simulation;
        this.actuatorType = actuatorType;
    }

    public ActuatorBuilder setActuatorType(String actuatorType) {
        this.actuatorType = actuatorType;
        return this;
    }

    public Actuator build(String name) {
        Application application = this.simulation.getApplication();

        int userId = application.getUserId();
        String appId = application.getAppId();

        Actuator actuator = new Actuator(name, userId, appId, this.actuatorType, application);

        this.simulation.addActuator(actuator);

        return actuator;
    }
}
