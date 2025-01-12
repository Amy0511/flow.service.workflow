package io.boomerang.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.repository.FlowWorkflowActivityTaskRepository;

@Service
public class ActivityTaskServiceImpl implements ActivityTaskService {

  @Autowired
  private FlowWorkflowActivityTaskRepository repoisotry;

  @Override
  public TaskExecutionEntity findByTaskIdAndActivityId(String taskId, String activityId) {
    return repoisotry.findByActivityIdAndTaskId(activityId, taskId);

  }

  @Override
  public List<TaskExecutionEntity> findTaskActiivtyForActivity(String activityId) {
    return repoisotry.findByactivityId(activityId);
  }

  @Override
  public TaskExecutionEntity save(TaskExecutionEntity entity) {
    return repoisotry.save(entity);
  }

  @Override
  public TaskExecutionEntity findByTaskNameAndActivityId(String taskName, String activityId) {
    return repoisotry.findByActivityIdAndTaskName(activityId, taskName);
  }

  @Override
  public TaskExecutionEntity findById(String id) {
    return repoisotry.findById(id).get();
  }

}
