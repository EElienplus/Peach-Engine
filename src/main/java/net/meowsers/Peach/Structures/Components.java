package net.meowsers.Peach.Structures;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.Components.*;

public enum Components {
    Empty("", null),
    MeshRenderer(MeshRendererComponent.class.getSimpleName(), MeshRendererComponent.class),
    Camera(CameraComponent.class.getSimpleName(), CameraComponent.class),
    Light(LightComponent.class.getSimpleName(), LightComponent.class),
    CubeCollider(CubeColliderComponent.class.getSimpleName(), CubeColliderComponent.class),
    Transform(TransformComponent.class.getSimpleName(), TransformComponent.class);

    private final String componentName;
    private final Class<? extends Component> componentClass;

    Components(String componentName, Class<? extends Component> componentClass) {
        this.componentName = componentName;
        this.componentClass = componentClass;
    }

    public String getComponentString() {
        return componentName;
    }

    public Class<? extends Component> getComponentClass() {
        return componentClass;
    }
}
