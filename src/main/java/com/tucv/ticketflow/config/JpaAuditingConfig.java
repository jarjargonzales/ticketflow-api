package com.tucv.ticketflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita la auditoría automática de created_at / updated_at
 * (ver {@link com.tucv.ticketflow.entity.BaseEntity}).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
