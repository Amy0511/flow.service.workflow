package net.boomerangplatform.service.runner.misc;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.model.controller.TaskConfiguration;
import net.boomerangplatform.model.controller.TaskCustom;
import net.boomerangplatform.model.controller.TaskDeletion;
import net.boomerangplatform.model.controller.TaskTemplate;
import net.boomerangplatform.model.controller.Workflow;
import net.boomerangplatform.model.controller.WorkflowStorage;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;
import net.boomerangplatform.mongo.entity.FlowTeamConfiguration;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowGlobalConfigService;
import net.boomerangplatform.mongo.service.FlowSettingsService;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;
import net.boomerangplatform.service.refactor.TaskClient;


@Service
@Primary
public class ControllerClientImpl implements ControllerClient {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowActivityService activityService;

  @Value("${controller.createtask.url}")
  public String createTaskURL;

  @Value("${controller.createworkflow.url}")
  private String createWorkflowURL;

  @Value("${flow.feature.team.properties}")
  private boolean enabledTeamProperites;

  @Autowired
  private FlowGlobalConfigService flowGlobalConfigService;

  @Autowired
  private FlowSettingsService flowSettinigs;

  @Autowired
  private TaskClient flowTaskClient;

  @Autowired
  private FlowTeamService flowTeamService;

  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Autowired
  private RevisionService revisionService;

  @Autowired
  public ActivityTaskService taskService;

  @Value("${controller.terminateworkflow.url}")
  private String terminateWorkflowURL;
  
  @Autowired
  private WorkflowService workflowService;

  @Override
  public boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage, Map<String, String> properties) {

    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);
    request.setProperties(properties);
    
    final WorkflowStorage storage = new WorkflowStorage();
    storage.setEnable(enableStorage);
    request.setWorkflowStorage(storage);
    
    try {
      restTemplate.postForObject(createWorkflowURL, request, String.class);
    } catch (RestClientException ex) {
      return false;
    }
    return true;
  }
  
  @Override
  public boolean terminateFlow(String workflowId, String workflowName, String activityId) {
    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);
    final WorkflowStorage storage = new WorkflowStorage();
    storage.setEnable(true);
    request.setWorkflowStorage(storage);

    restTemplate.postForObject(terminateWorkflowURL, request, String.class);
    return true;
  }
  
  @Override
  @Async
  public void submitCustomTask(Task task, String activityId, String workflowName) {

    TaskResult taskResult = new TaskResult();
    TaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);

    taskResult.setNode(task.getTaskId());

    final TaskCustom request = new TaskCustom();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());

   
    ControllerRequestProperties controllerProperties =
        buildRequestPropertyLayering(task, activityId);
   
    Map<String, String> map = controllerProperties.getMap();
    
    String image = controllerProperties.getLayeredProperty("image");
    request.setImage(image);
    
   
    String command = controllerProperties.getLayeredProperty("image");
    request.setCommand(command);
  
    List<String> args = new LinkedList<>();
    if (map.get("arguments") != null) {
      String arguments = controllerProperties.getLayeredProperty("arguments");
      if (!arguments.isBlank()) {
        String[] lines = arguments.split("\\r?\\n");
        args = new LinkedList<>();
        for (String line : lines) {
          String newValue = this.replaceValueWithProperty(line, activityId, map);
          args.add(newValue);
        }
      }
    }
    
    request.setArguments(args);

    final Date startDate = new Date();
    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);

    TaskConfiguration taskConfiguration = buildTaskConfiguration();
    request.setConfiguration(taskConfiguration);

    logPayload("Create Task Request", request);
    
    try {
      TaskResponse response =
          restTemplate.postForObject(createTaskURL, request, TaskResponse.class);
      if (response != null) {
        taskExecution.setOutputs(response.getOutput());
      }

      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();
      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(TaskStatus.completed);

      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(TaskStatus.failure);
      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }
    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);
    }

    taskService.save(taskExecution);
    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(taskExecution.getFlowTaskStatus());
    flowTaskClient.endTask(response);
  }


  @Override
  @Async
  public void submitTemplateTask(Task task, String activityId, String workflowName) {

    TaskResult taskResult = new TaskResult();
    TaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);

    taskResult.setNode(task.getTaskId());

    final TaskTemplate request = new TaskTemplate();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());

    ControllerRequestProperties applicationProperties =
        buildRequestPropertyLayering(task, activityId);

    Map<String, String> map = applicationProperties.getMap();
    request.setProperties(map);
    
    TaskConfiguration taskConfiguration = buildTaskConfiguration();
    request.setConfiguration(taskConfiguration); 
    
    if (task.getRevision() != null) {
      Revision revision = task.getRevision();
      request.setArguments(revision.getArguments());

      if (revision.getImage() != null && !revision.getImage().isBlank()) {
        request.setImage(revision.getImage());
      } else {
        String workerImage =
            this.flowSettinigs.getConfiguration("controller", "worker.image").getValue();
        request.setImage(workerImage);
      }

      if (revision.getCommand() != null && !revision.getCommand().isBlank()) {
        request.setCommand(revision.getCommand());
      }
    } else {
      taskResult.setStatus(TaskStatus.invalid);
    }

    final Date startDate = new Date();

    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);

    logPayload("Create Task Request", request);
    try {
      TaskResponse response =
          restTemplate.postForObject(createTaskURL, request, TaskResponse.class);
      if (response != null) {
        taskExecution.setOutputs(response.getOutput());
        logPayload("Create Task Response", response);
      }

      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();

      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(TaskStatus.completed);
      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(TaskStatus.failure);
        taskResult.setStatus(TaskStatus.failure);
      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }
      LOGGER.info("Task result: {}", taskResult.getStatus());
    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
    }
    taskService.save(taskExecution);

    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(taskExecution.getFlowTaskStatus());
    flowTaskClient.endTask(response);
  }



  private TaskConfiguration buildTaskConfiguration() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    TaskDeletion taskDeletion = TaskDeletion.Never;
    String settingsPolicy =
        this.flowSettinigs.getConfiguration("controller", "job.deletion.policy").getValue();
    if (settingsPolicy != null) {
      taskDeletion = TaskDeletion.valueOf(settingsPolicy);
    }
    boolean enableDebug = false;
    String enableDebugFlag =
        this.flowSettinigs.getConfiguration("controller", "enable.debug").getValue();
    if (settingsPolicy != null) {
      enableDebug = Boolean.valueOf(enableDebugFlag).booleanValue();
    }
    
    taskConfiguration.setDeletion(taskDeletion);
    taskConfiguration.setDebug(Boolean.valueOf(enableDebug));
    return taskConfiguration;
  }


  private void buildTeamProperties(Map<String, Object> teamProperties, String workflowId) {
    FlowTeamEntity flowTeamEntity =
        this.flowTeamService.findById(workflowService.getWorkflow(workflowId).getFlowTeamId());
    List<FlowTeamConfiguration> teamConfig = null;

    if (flowTeamEntity.getSettings() != null) {
      teamConfig = flowTeamEntity.getSettings().getProperties();
    }

    if (teamConfig != null) {
      for (FlowTeamConfiguration config : teamConfig) {
        teamProperties.put(config.getKey(), config.getValue());
      }
    }
  }

  private void logPayload(String payloadName, Object request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
      LOGGER.info("Received Request: {}", payloadName);
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

  private String replaceValueWithProperty(String value, String activityId, Map<String, String> executionProperties) {
    
    String regex = "\\$\\{p:(.*?)\\}";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      String group = matcher.group(1);
      String[] components = group.split("/");
      if (components.length == 1) {
        if (executionProperties.get(components[0]) != null) {
          return executionProperties.get(components[0]);
        }
      } else if (components.length == 2) {
        String taskName = components[0];
        String outputProperty = components[1];
        TaskExecutionEntity taskExecution =
            taskService.findByTaskNameAndActiityId(taskName, activityId);
        if (taskExecution != null && taskExecution.getOutputs() != null
            && taskExecution.getOutputs().get(outputProperty) != null) {
          return taskExecution.getOutputs().get(outputProperty);
        }
      }
    }
    
    return value;
  }


  private ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId) {
    ControllerRequestProperties applicationProperties =
        new ControllerRequestProperties();
    Map<String, Object> systemProperties = applicationProperties.getSystemProperties();
    Map<String, Object> globalProperties = applicationProperties.getGlobalProperties();
    Map<String, Object> teamProperties = applicationProperties.getTeamProperties();
    Map<String, Object> workflowProperties = applicationProperties.getWorkflowProperties();

    buildGlobalProperties(globalProperties);
    buildSystemProperties(task, activityId, task.getWorkflowId(), systemProperties);
    
    if (enabledTeamProperites) {
      buildTeamProperties(teamProperties, task.getWorkflowId());
    }
    buildWorkflowProperties(workflowProperties, task, activityId);
    
    buildTaskInputProperties(applicationProperties, task, activityId);

    return applicationProperties;
  }
  
  private void buildTaskInputProperties(ControllerRequestProperties applicationProperties , Task task, String activityId) {
    
    Map<String, Object> workflowInputProperties = applicationProperties.getWorkflowProperties();
    
    Map<String, String> finalLayering = applicationProperties.getMap();
    
    final Map<String, String> map = task.getInputs();
    for (final Map.Entry<String, String> pair : map.entrySet()) {
      String key = pair.getKey();
      String value = pair.getValue();
      String newValue = this.replaceValueWithProperty(value, activityId,finalLayering);
      workflowInputProperties.put(key, newValue);
    }
  }
  
  private void buildWorkflowProperties(Map<String, Object> workflowProperties, Task task, String activityId) {
    
    ActivityEntity activity = activityService.findWorkflowActivity(activityId);
    List<CoreProperty> properties = activity.getProperties();
    for (CoreProperty property : properties) {
      workflowProperties.put(property.getKey(), property.getValue());
    }
  }
  
  private void buildGlobalProperties(Map<String, Object> globalProperties) {
    List<FlowGlobalConfigEntity> globalConfigs = this.flowGlobalConfigService.getGlobalConfigs();
    for (FlowGlobalConfigEntity entity : globalConfigs) {
      if (entity.getValue() != null) {
        globalProperties.put(entity.getKey(), entity.getValue());
      }
    }
  }

  private void buildSystemProperties(Task task, String activityId, String workflowId,
      Map<String, Object> systemProperties) {

    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    ActivityEntity activity = activityService.findWorkflowActivity(activityId);
    systemProperties.put("workflow.name", workflow.getName());
    systemProperties.put("workflow.activity.id", activityId);
    systemProperties.put("workflow.id", workflow.getId());
    systemProperties.put("task.name", task.getTaskName());
    systemProperties.put("task.type", task.getTaskType());
    systemProperties.put("workflow.version",
        revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid()).getVersion());
    systemProperties.put("trigger.type", activity.getTrigger());
    systemProperties.put("workflow.activity.initiator", activity.getInitiatedByUserId());
  }
}
