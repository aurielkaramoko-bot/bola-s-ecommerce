package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {}
