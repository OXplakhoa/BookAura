package com.bookaura.systemconfig.repository;

import com.bookaura.systemconfig.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
}
