package project.spms.spms.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignRequest {
    private Integer projectId;
    private List<Integer> userIds;
    private String roleInProject;
    private Integer assignedBy;
}