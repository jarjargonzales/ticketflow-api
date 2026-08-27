

BEGIN;

-- ---------------------------------------------------------------------
-- Tabla: tickets
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(120)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    priority       VARCHAR(20)   NOT NULL,
    reporter_email VARCHAR(255)  NOT NULL,
    assignee_email VARCHAR(255),
    due_at         TIMESTAMP,
    resolved_at    TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT chk_tickets_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_tickets_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- ---------------------------------------------------------------------
-- Tabla: ticket_comments
-- ---------------------------------------------------------------------
CREATE TABLE ticket_comments (
    id           BIGSERIAL PRIMARY KEY,
    ticket_id    BIGINT       NOT NULL,
    author_email VARCHAR(255) NOT NULL,
    message      VARCHAR(2000) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT fk_ticket_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Índices
-- ---------------------------------------------------------------------
CREATE INDEX idx_tickets_status    ON tickets (status);
CREATE INDEX idx_tickets_priority  ON tickets (priority);
CREATE INDEX idx_tickets_assignee  ON tickets (assignee_email);
CREATE INDEX idx_tickets_due_at    ON tickets (due_at);
CREATE INDEX idx_comments_ticket   ON ticket_comments (ticket_id);

-- ---------------------------------------------------------------------
-- Permisos: la app conecta con auth_user, pero las tablas las crea el
-- superusuario postgres. SIEMPRE ejecutar al final del script.
-- ---------------------------------------------------------------------
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO auth_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO auth_user;

COMMIT;
