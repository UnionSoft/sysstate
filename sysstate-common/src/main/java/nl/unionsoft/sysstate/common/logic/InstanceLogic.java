package nl.unionsoft.sysstate.common.logic;

import java.util.List;
import java.util.Optional;

import nl.unionsoft.common.list.model.ListRequest;
import nl.unionsoft.common.list.model.ListResponse;
import nl.unionsoft.sysstate.common.dto.FilterDto;
import nl.unionsoft.sysstate.common.dto.InstanceDto;
import nl.unionsoft.sysstate.common.dto.PropertyMetaValue;

public interface InstanceLogic {

    public InstanceDto generateInstanceDto(String type);
    
    public InstanceDto generateInstanceDto(String type, Long projectId, Long environmentId);

    public List<InstanceDto> getInstances();

    public Optional<InstanceDto> getInstance(Long instanceId);

    public Long createOrUpdateInstance(InstanceDto instance);

    public void delete(Long instanceId);

    public void queueForUpdate(final Long instanceId);

    public List<InstanceDto> getInstancesForProjectAndEnvironment(String projectPrefix, String environmentPrefix);
    
    public List<InstanceDto> getInstancesForProjectEnvironment(Long projectEnvironmentId);
    
    public List<InstanceDto> getInstancesForEnvironment(Long environmentId);

    public List<Long> getInstancesKeys(FilterDto filter);

    public void addTriggerJob(final long instanceId);
    
    public void removeTriggerJob(final long instanceId);

    public List<PropertyMetaValue> getPropertyMeta(String type);

}
