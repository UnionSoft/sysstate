package nl.unionsoft.sysstate.common.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ProjectDto {
    private Long id;

    @NotNull()
    @Size(min = 1, max = 15)
    private String name;
    private int order;
    private boolean enabled;
    
    private final List<ProjectEnvironmentDto> projectEnvironments;

    public ProjectDto() {
        enabled = true;
        projectEnvironments = new ArrayList<ProjectEnvironmentDto>();
    }

    public List<ProjectEnvironmentDto> getProjectEnvironments() {
        return projectEnvironments;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
