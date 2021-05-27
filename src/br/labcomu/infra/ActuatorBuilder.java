package br.labcomu.infra;

import org.fog.application.Application;
import org.fog.entities.Actuator;

public class ActuatorBuilder {

    private String actuatorType;

    public ActuatorBuilder() {
        this("ACTION");
    }

    private ActuatorBuilder(String actuatorType) {
        this.actuatorType = actuatorType;
    }

    public ActuatorBuilder setActuatorType(String actuatorType) {
        this.actuatorType = actuatorType;
        return this;
    }

    public Actuator build(String name, Application application) {
        int userId = application.getUserId();
        String appId = application.getAppId();

        Actuator actuator = new Actuator(name, userId, appId, this.actuatorType, application);

        return actuator;
    }
}
