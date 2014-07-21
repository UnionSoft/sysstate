package nl.unionsoft.sysstate.common.logic;

import java.util.List;

import nl.unionsoft.common.list.model.ListRequest;
import nl.unionsoft.common.list.model.ListResponse;
import nl.unionsoft.sysstate.common.dto.FilterDto;
import nl.unionsoft.sysstate.common.dto.InstanceDto;
import nl.unionsoft.sysstate.common.dto.PropertyMetaValue;

public interface InstanceLogic {

    public InstanceDto generateInstanceDto(String type);
    
    public InstanceDto generateInstanceDto(String type, Long projectId, Long environmentId);

    public List<InstanceDto> getInstances();

    public InstanceDto getInstance(Long instanceId);

    public InstanceDto getInstance(Long instanceId, boolean states);

    public void createOrUpdateInstance(InstanceDto instance);

    public void delete(Long instanceId);

    public void queueForUpdate(final Long instanceId);

    public List<InstanceDto> getInstancesForPrefixes(String projectPrefix, String environmentPrefix);

    public ListResponse<InstanceDto> getInstances(ListRequest listRequest);

    public ListResponse<InstanceDto> getInstances(FilterDto filter);

    public void addTriggerJob(final long instanceId);
    
    public void removeTriggerJob(final long instanceId);

    public List<PropertyMetaValue> getPropertyMeta(String type);

}
