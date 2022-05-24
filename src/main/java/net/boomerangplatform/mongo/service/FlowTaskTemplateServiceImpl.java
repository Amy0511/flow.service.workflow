package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.mongo.repository.FlowTaskTemplateRepository;

@Service
public class FlowTaskTemplateServiceImpl implements FlowTaskTemplateService {

  @Autowired
  private FlowTaskTemplateRepository flowTaskTemplateRepository;

  @Override
  @NoLogging
  public List<FlowTaskTemplateEntity> getAllTaskTemplates() {
    return flowTaskTemplateRepository.findAll();
  }

  @Override
  @NoLogging
  public FlowTaskTemplateEntity getTaskTemplateWithId(String id) {
    return flowTaskTemplateRepository.findById(id).orElse(null);
  }
  
  @Override
  public List<FlowTaskTemplateEntity> getTaskTemplateWithIds(List<String> ids){
	return  flowTaskTemplateRepository.findByIdIn(ids);
  }

  @Override
  @NoLogging
  public FlowTaskTemplateEntity insertTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);

    return flowTaskTemplateRepository.insert(flowTaskTemplateEntity);
  }

  @Override
  @NoLogging
  public FlowTaskTemplateEntity updateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity) {
    return flowTaskTemplateRepository.save(flowTaskTemplateEntity);
  }

  @Override
  @NoLogging
  public void deleteTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.inactive);
    flowTaskTemplateRepository.save(flowTaskTemplateEntity);
  }

  @Override
  @NoLogging
  public void activateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);
    flowTaskTemplateRepository.save(flowTaskTemplateEntity);
  }

  @Override
  public List<FlowTaskTemplateEntity> getAllTaskTemplatesforTeamId(String teamId) {
    return flowTaskTemplateRepository.findAllByFlowTeamId(teamId);
  }

  @Override
  public List<FlowTaskTemplateEntity> getTaskTemplatesforTeamId(String teamId) {
    return flowTaskTemplateRepository.findByFlowTeamId(teamId);
  }

  @Override
  public List<FlowTaskTemplateEntity> getAllSystemTasks() {
    return flowTaskTemplateRepository.findAllSystemTasks();
  }

  @Override
  public List<FlowTaskTemplateEntity> getAllTaskTemplatesForSystem() {
    return flowTaskTemplateRepository.findAllForSystemTasks();
  }

  @Override
  public List<FlowTaskTemplateEntity> getAllGlobalTasks() {
    return flowTaskTemplateRepository.findAllGlobalTasks();
  }
}
