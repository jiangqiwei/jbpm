/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.task.commands;

import java.util.List;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.seam.transaction.Transactional;
import org.jbpm.task.events.AfterTaskClaimedEvent;
import org.jbpm.task.events.BeforeTaskClaimedEvent;
import org.jbpm.task.exception.PermissionDeniedException;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.model.OrganizationalEntity;
import org.kie.internal.task.api.model.Status;
import org.kie.internal.task.api.model.Task;
import org.kie.internal.task.api.model.User;

/**
 * Operation.Claim 
        : [ new OperationCommand().{ 
                status = [ Status.Ready ],
                allowed = [ Allowed.PotentialOwner, Allowed.BusinessAdministrator ],    
                setNewOwnerToUser = true,           
                newStatus = Status.Reserved
            } ],
 */

@Transactional
public class ClaimTaskCommand extends TaskCommand {

    public ClaimTaskCommand(long taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public Void execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        Task task = context.getTaskQueryService().getTaskInstanceById(taskId);
        User user = context.getTaskIdentityService().getUserById(userId);
        context.getTaskEvents().select(new AnnotationLiteral<BeforeTaskClaimedEvent>() {}).fire(task);
        // CHeck for potential Owner allowed (decorator?)
        boolean potOwnerAllowed = CommandsUtil.isAllowed(user, getGroupsIds(), (List<OrganizationalEntity>) task.getPeopleAssignments().getPotentialOwners());
        boolean adminAllowed = CommandsUtil.isAllowed(user, getGroupsIds(), (List<OrganizationalEntity>) task.getPeopleAssignments().getBusinessAdministrators());
        
        if (!potOwnerAllowed && !adminAllowed) {
            String errorMessage = "The user" + user + "is not allowed to Start the task "+task.getId();
            throw new PermissionDeniedException(errorMessage);
        }
        if (task.getTaskData().getStatus().equals(Status.Ready)) {
            
            task.getTaskData().setStatus(Status.Reserved);
            task.getTaskData().setActualOwner(user);
        }
        context.getTaskEvents().select(new AnnotationLiteral<AfterTaskClaimedEvent>() {}).fire(task);

        return null;
    }

   
}
