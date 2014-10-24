package nl.unionsoft.sysstate.logic.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import nl.unionsoft.common.converter.BidirectionalConverter;
import nl.unionsoft.common.converter.Converter;
import nl.unionsoft.common.converter.ListConverter;
import nl.unionsoft.common.list.model.GroupRestriction;
import nl.unionsoft.common.list.model.ListRequest;
import nl.unionsoft.common.list.model.ListResponse;
import nl.unionsoft.common.list.model.ObjectRestriction;
import nl.unionsoft.common.list.model.Restriction;
import nl.unionsoft.common.list.model.Restriction.Rule;
import nl.unionsoft.common.list.worker.impl.BeanListRequestWorkerImpl;
import nl.unionsoft.common.param.ParamContextLogicImpl;
import nl.unionsoft.common.util.PropertiesUtil;
import nl.unionsoft.sysstate.common.dto.EnvironmentDto;
import nl.unionsoft.sysstate.common.dto.FilterDto;
import nl.unionsoft.sysstate.common.dto.InstanceDto;
import nl.unionsoft.sysstate.common.dto.ProjectEnvironmentDto;
import nl.unionsoft.sysstate.common.dto.PropertyMetaValue;
import nl.unionsoft.sysstate.common.dto.StateDto;
import nl.unionsoft.sysstate.common.enums.StateType;
import nl.unionsoft.sysstate.common.extending.ListOfValueResolver;
import nl.unionsoft.sysstate.common.logic.EnvironmentLogic;
import nl.unionsoft.sysstate.common.logic.InstanceLogic;
import nl.unionsoft.sysstate.common.logic.ProjectEnvironmentLogic;
import nl.unionsoft.sysstate.common.util.PropertyGroupUtil;
import nl.unionsoft.sysstate.dao.InstanceDao;
import nl.unionsoft.sysstate.dao.ListRequestDao;
import nl.unionsoft.sysstate.dao.PropertyDao;
import nl.unionsoft.sysstate.dao.StateDao;
import nl.unionsoft.sysstate.domain.Instance;
import nl.unionsoft.sysstate.domain.InstanceProperty;
import nl.unionsoft.sysstate.domain.ProjectEnvironment;
import nl.unionsoft.sysstate.domain.State;
import nl.unionsoft.sysstate.job.UpdateInstanceJob;
import nl.unionsoft.sysstate.logic.PluginLogic;
import nl.unionsoft.sysstate.logic.StateLogic;
import nl.unionsoft.sysstate.logic.StateResolverLogic;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("instanceLogic")
@DependsOn("projectEnvironmentLogic")
@Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class InstanceLogicImpl implements InstanceLogic, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceLogicImpl.class);

    @Inject
    @Named("instanceDao")
    private InstanceDao instanceDao;

    @Inject
    @Named("pluginLogic")
    private PluginLogic pluginLogic;

    @Inject
    @Named("listRequestDao")
    private ListRequestDao listRequestDao;

    @Inject
    @Named("stateDao")
    private StateDao stateDao;

    @Inject
    @Named("propertyDao")
    private PropertyDao propertyDao;

    @Inject
    @Named("stateLogic")
    private StateLogic stateLogic;

    @Inject
    @Named("scheduler")
    private Scheduler scheduler;

    @Inject
    @Named("instanceConverter")
    private Converter<InstanceDto, Instance> instanceConverter;

    @Inject
    @Named("stateResolverLogic")
    private StateResolverLogic stateResolverLogic;

    @Inject
    @Named("stateConverter")
    private Converter<StateDto, State> stateConverter;

    @Inject
    @Named("environmentLogic")
    private EnvironmentLogic environmentLogic;

    @Inject
    @Named("projectEnvironmentLogic")
    private ProjectEnvironmentLogic projectEnvironmentLogic;

    @Inject
    @Named("paramContextLogic")
    private ParamContextLogicImpl paramContextLogic;

    @Inject
    @Named("instancePropertiesConverter")
    private BidirectionalConverter<Properties, List<InstanceProperty>> instancePropertiesConverter;

    public void queueForUpdate(final Long instanceId) {
        updateTriggerJob(instanceDao.getInstance(instanceId));
    }

    private void updateTriggerJob(final Instance instance) {
        LOG.info("Creating or updating queue job for instance with id: {}", instance.getId());
        removeTriggerJob(instance.getId());
        addTriggerJob(instance.getId());
    }

    public void addTriggerJob(final long instanceId) {
        Instance instance = instanceDao.getInstance(instanceId);
        if (instance != null) {
            final long id = instance.getId();
            final String jobName = "instance-" + id + "-job";
            final String triggerName = "instance-" + id + "-trigger";
            final String groupName = "instances";

            final long refreshTimeout = instance.getRefreshTimeout();
            final SimpleTrigger trigger = new SimpleTrigger(triggerName, groupName);
            trigger.setRepeatCount(-1);
            trigger.setRepeatInterval(refreshTimeout < 30000 ? 30000 : refreshTimeout);
            trigger.setStartTime(new Date(System.currentTimeMillis() + 5000));

            final JobDetail jobDetail = new JobDetail(jobName, groupName, UpdateInstanceJob.class);
            final JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.put("instanceId", id);

            try {
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (final SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeTriggerJob(final long instanceId) {
        try {
            final String jobName = "instance-" + instanceId + "-job";
            final String groupName = "instances";
            scheduler.deleteJob(jobName, groupName);
        } catch (final SchedulerException e1) {
            e1.printStackTrace();
        }
    }

    public List<InstanceDto> getInstances() {
        return ListConverter.convert(instanceConverter, instanceDao.getInstances());
    }

    public InstanceDto getInstance(final Long instanceId) {
        return getInstance(instanceId, false);
    }

    public InstanceDto getInstance(final Long instanceId, final boolean states) {

        final InstanceDto result = instanceConverter.convert(instanceDao.getInstance(instanceId));

        if (states) {
            setLastStatesForInstance(result);
        }
        return result;

    }

    private void setLastStatesForInstance(final InstanceDto instance) {
        if (instance != null && instance.getId() != null) {
            final Long instanceId = instance.getId();
            instance.setState(stateConverter.convert(stateDao.getLastStateForInstance(instanceId)));
            instance.setLastStable(stateConverter.convert(stateDao.getLastStateForInstance(instanceId, StateType.STABLE)));
            instance.setLastUnstable(stateConverter.convert(stateDao.getLastStateForInstance(instanceId, StateType.UNSTABLE)));
            instance.setLastError(stateConverter.convert(stateDao.getLastStateForInstance(instanceId, StateType.ERROR)));
            instance.setLastPending(stateConverter.convert(stateDao.getLastStateForInstance(instanceId, StateType.PENDING)));
            instance.setLastDisabled(stateConverter.convert(stateDao.getLastStateForInstance(instanceId, StateType.DISABLED)));
        }
    }

    public void createOrUpdateInstance(final InstanceDto dto) {
        final Instance instance = new Instance();
        instance.setId(dto.getId());
        // instance.setConfiguration(dto.getConfiguration());
        instance.setEnabled(dto.isEnabled());
        instance.setHomepageUrl(dto.getHomepageUrl());
        instance.setName(dto.getName());
        instance.setPluginClass(dto.getPluginClass());
        final ProjectEnvironment projectEnvironment = new ProjectEnvironment();
        projectEnvironment.setId(dto.getProjectEnvironment().getId());
        instance.setProjectEnvironment(projectEnvironment);
        instance.setRefreshTimeout(dto.getRefreshTimeout());
        instance.setTags(dto.getTags());
        String reference = dto.getReference();
        if (StringUtils.isEmpty(reference)) {
            UUID uuid = UUID.randomUUID();
            reference = uuid.toString();
        }
        instanceDao.createOrUpdate(instance);

        Map<String, String> configuration = dto.getConfiguration();
        if (configuration != null) {
            for (Entry<String, String> entry : configuration.entrySet()) {
                propertyDao.setInstanceProperty(instance, entry.getKey(), entry.getValue());

            }
        }

        updateTriggerJob(instance);
    }

    public void delete(final Long instanceId) {
        instanceDao.delete(instanceId);
        removeTriggerJob(instanceId);
    }

    public List<InstanceDto> getInstancesForProjectAndEnvironment(final String projectPrefix, final String environmentPrefix) {
        return ListConverter.convert(instanceConverter, instanceDao.getInstancesForProjectAndEnvironment(projectPrefix, environmentPrefix));
    }

    public ListResponse<InstanceDto> getInstances(final ListRequest listRequest) {
        final ListResponse<InstanceDto> listResponse = listRequestDao.getResults(Instance.class, listRequest, instanceConverter);
        for (final InstanceDto instance : listResponse.getResults()) {
            instance.setState(stateConverter.convert(stateDao.getLastStateForInstance(instance.getId())));
        }
        return listResponse;
    }

    public ListResponse<InstanceDto> getInstances(final FilterDto filter) {
        final ListResponse<InstanceDto> listResponse = handleFilterData(filter);
        handleInstancesFilter(filter, listResponse);
        return listResponse;
    }

    private void handleInstancesFilter(final FilterDto filter, final ListResponse<InstanceDto> listResponse) {
        final List<?> results = listResponse.getResults();
        final BeanListRequestWorkerImpl beanListRequestWorkerImpl = new BeanListRequestWorkerImpl() {
            @Override
            @SuppressWarnings({ "unchecked", "hiding" })
            public <Object> List<Object> fetchData(final Class<Object> dtoClass, final ListRequest listRequest) {
                return (List<Object>) results;
            }
        };

        final List<Restriction> restrictions = new ArrayList<Restriction>();
        {
            final List<StateType> states = filter.getStates();
            if (states != null && states.size() > 0) {
                final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
                final List<Restriction> orReestrictions = groupRestriction.getRestrictions();
                for (final StateType state : states) {
                    orReestrictions.add(new ObjectRestriction(Rule.EQ, "state.state", state));
                }
                restrictions.add(groupRestriction);
            }
        }
        if (restrictions.size() > 0) {
            beanListRequestWorkerImpl.restrictions(listResponse.getResults(), restrictions);
        }
    }

    private ListResponse<InstanceDto> handleFilterData(final FilterDto filter) {
        final ListRequest listRequest = new ListRequest();
        // listRequest.addSort(new Sort("last.state", Direction.ASC));
        listRequest.setFirstResult(0);
        listRequest.setMaxResults(ListRequest.ALL_RESULTS);
        // if (StringUtils.isNotEmpty(sort)) {
        // listRequest.addSort(new Sort(sort));
        // }

        final String search = filter.getSearch();
        if (StringUtils.isNotEmpty(search)) {
            final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
            final List<Restriction> restrictions = groupRestriction.getRestrictions();
            for (final String element : StringUtils.split(search, ' ')) {
                restrictions.add(new ObjectRestriction(Rule.LIKE, "tags", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "name", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "homepageUrl", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "configuration", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "pluginClass", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "projectEnvironment.project.name", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "projectEnvironment.environment.name", element));
                restrictions.add(new ObjectRestriction(Rule.LIKE, "projectEnvironment.homepageUrl", element));
            }
            listRequest.addRestriction(groupRestriction);
        }

        final String tags = filter.getTags();
        if (StringUtils.isNotEmpty(tags)) {
            final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
            final List<Restriction> restrictions = groupRestriction.getRestrictions();
            for (final String element : StringUtils.split(tags, ' ')) {
                restrictions.add(new ObjectRestriction(Rule.LIKE, "tags", element));
            }
            listRequest.addRestriction(groupRestriction);
        }

        final List<Long> projects = filter.getProjects();
        if (projects != null && projects.size() > 0) {
            final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
            final List<Restriction> restrictions = groupRestriction.getRestrictions();
            for (final Long project : projects) {
                restrictions.add(new ObjectRestriction(Rule.EQ, "projectEnvironment.project.id", project));
            }

            listRequest.addRestriction(groupRestriction);

        }

        final List<Long> environments = filter.getEnvironments();
        if (environments != null && environments.size() > 0) {

            final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
            final List<Restriction> restrictions = groupRestriction.getRestrictions();
            for (final Long environment : environments) {
                restrictions.add(new ObjectRestriction(Rule.EQ, "projectEnvironment.environment.id", environment));
            }
            listRequest.addRestriction(groupRestriction);

        }

        final List<String> stateResolvers = filter.getStateResolvers();
        if (stateResolvers != null && stateResolvers.size() > 0) {
            final GroupRestriction groupRestriction = new GroupRestriction(Rule.OR);
            final List<Restriction> restrictions = groupRestriction.getRestrictions();
            for (final String stateResolver : stateResolvers) {
                restrictions.add(new ObjectRestriction(Rule.EQ, "pluginClass", stateResolver));
            }
            listRequest.addRestriction(groupRestriction);
        }

        return getInstances(listRequest);
    }

    public void afterPropertiesSet() throws Exception {
        List<Instance> instances = instanceDao.getInstances();
        for (Instance instance : instances) {

            // Add trigger
            addTriggerJob(instance.getId());
        }

    }

    public InstanceDto generateInstanceDto(String type) {
        return generateInstanceDto(type, null, null);
    }

    public InstanceDto generateInstanceDto(String type, Long projectId, Long environmentId) {

        final InstanceDto instance = new InstanceDto();
        instance.setPluginClass(type);
        instance.setEnabled(true);
        instance.setRefreshTimeout(10000);
        if (environmentId != null) {
            final EnvironmentDto environment = environmentLogic.getEnvironment(environmentId);
            if (environment != null) {
                instance.setRefreshTimeout(environment.getDefaultInstanceTimeout());
            }
            if (projectId != null) {
                final ProjectEnvironmentDto projectEnvironment = projectEnvironmentLogic.getProjectEnvironment(projectId, environmentId);
                if (projectEnvironment != null) {
                    instance.setProjectEnvironment(projectEnvironment);
                }
            }
        }
        return instance;
    }

    @Cacheable("propertyMetaTypeCache")
    public List<PropertyMetaValue> getPropertyMeta(String type) {

        Object component = pluginLogic.getComponent(type);

        Class<?> componentClass = component.getClass();

        Stack<Class<?>> classStack = new Stack<Class<?>>();
        Class<?> superClass = componentClass;
        while (!Object.class.equals(superClass)) {
            classStack.push(superClass);
            superClass = superClass.getSuperclass();
        }

        List<PropertyMetaValue> propertyMetas = new ArrayList<PropertyMetaValue>();
        while (!classStack.empty()) {
            Class<?> stackClass = classStack.pop();
            Map<String, Properties> instanceGroupProperties = PropertyGroupUtil.getGroupProperties(pluginLogic.getPropertiesForClass(stackClass), "instance");
            for (Entry<String, Properties> entry : instanceGroupProperties.entrySet()) {
                String id = entry.getKey();
                Properties properties = entry.getValue();
                PropertyMetaValue propertyMetaValue = new PropertyMetaValue();
                propertyMetaValue.setId(id);
                propertyMetaValue.setTitle(properties.getProperty("title", id));
                String lovResolver = properties.getProperty("resolver");
                if (StringUtils.isNotEmpty(lovResolver)) {
                    ListOfValueResolver listOfValueResolver = pluginLogic.getListOfValueResolver(lovResolver);
                    propertyMetaValue.setLov(listOfValueResolver.getListOfValues(propertyMetaValue));
                }
                propertyMetas.add(propertyMetaValue);
            }
        }
        return propertyMetas;
    }

}
