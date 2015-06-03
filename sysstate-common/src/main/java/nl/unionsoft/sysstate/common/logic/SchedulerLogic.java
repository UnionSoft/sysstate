package nl.unionsoft.sysstate.common.logic;

import java.util.List;

import nl.unionsoft.sysstate.common.dto.Task;

public interface SchedulerLogic {

    public List<Task> retrieveTasks();

    public int getCapacity();
    
    public int getLoad();

}
