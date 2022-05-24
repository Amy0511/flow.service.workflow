package net.boomerangplatform.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.WorkFlowRevisionAggr;

public interface RevisionService {

  void deleteWorkflow(RevisionEntity flowWorkflowVersionEntity);

  Optional<RevisionEntity> getRevision(String id);
  
  RevisionEntity getLatestWorkflowVersion(String workflowId);

  RevisionEntity getLatestWorkflowVersion(String workflowId, long version);

  long getWorkflowCount(String workFlowId);

  RevisionEntity getWorkflowlWithId(String id);

  RevisionEntity insertWorkflow(RevisionEntity flowWorkflowVersionEntity);

  RevisionEntity updateWorkflow(RevisionEntity flowWorkflowVersionEntity);

  Page<RevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable);
  
  List<WorkFlowRevisionAggr> getWorkflowRevisionCount(List<String> workFlowIds);
  
  List<WorkFlowRevisionAggr> getWorkflowRevisionCountAndLatestVersion(List<String> workFlowIds);
}
