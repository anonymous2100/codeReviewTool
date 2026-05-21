CREATE TABLE IF NOT EXISTS review_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      VARCHAR(255)  NOT NULL,
    mr_iid          BIGINT,
    commit_sha      VARCHAR(40)   NOT NULL,
    source_branch   VARCHAR(255),
    target_branch   VARCHAR(255),
    total_files     INT           NOT NULL DEFAULT 0,
    reviewed_files  INT           NOT NULL DEFAULT 0,
    skipped_files   INT           NOT NULL DEFAULT 0,
    critical_count  INT           NOT NULL DEFAULT 0,
    warning_count   INT           NOT NULL DEFAULT 0,
    suggestion_count INT          NOT NULL DEFAULT 0,
    duration_ms     BIGINT        NOT NULL,
    has_blocking    BOOLEAN       NOT NULL DEFAULT FALSE,
    review_status   VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    error_message   TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_created (project_id, created_at),
    INDEX idx_commit_sha (commit_sha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_file_details (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id       BIGINT        NOT NULL,
    file_path       VARCHAR(500)  NOT NULL,
    added_lines     INT           NOT NULL DEFAULT 0,
    removed_lines   INT           NOT NULL DEFAULT 0,
    issues_json     LONGTEXT,
    summary         LONGTEXT,
    is_skipped      BOOLEAN       NOT NULL DEFAULT FALSE,
    skip_reason     VARCHAR(100),
    diff_content    LONGTEXT,
    FOREIGN KEY (review_id) REFERENCES review_records(id) ON DELETE CASCADE,
    INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
