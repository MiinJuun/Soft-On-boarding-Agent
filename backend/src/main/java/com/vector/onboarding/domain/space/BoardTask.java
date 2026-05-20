package com.vector.onboarding.domain.space;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BoardTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardTaskStatus status;

    private String assignee;

    private String label;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardTask(Long spaceId, String title, BoardTaskStatus status, String assignee, String label) {
        this.spaceId = spaceId;
        this.title = title;
        this.status = status;
        this.assignee = assignee;
        this.label = label;
    }

    public void update(String title, BoardTaskStatus status, String assignee, String label) {
        if (title != null) this.title = title;
        if (status != null) this.status = status;
        if (assignee != null) this.assignee = assignee;
        if (label != null) this.label = label;
    }
}
