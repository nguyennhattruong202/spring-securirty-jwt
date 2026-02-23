package com.accolite.pru.health.authapp.initializer;

import com.accolite.pru.health.authapp.model.Role;
import com.accolite.pru.health.authapp.model.RoleName;
import com.accolite.pru.health.authapp.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class RoleDataInitializer implements ApplicationRunner {
  private final RoleRepository roleRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    initRole(RoleName.ROLE_ADMIN);
    initRole(RoleName.ROLE_USER);
  }

  private void initRole(RoleName roleName) {
    if (roleRepository.existsByRoleName(roleName)) {
      log.info("Role already: [{}]", roleName.name());
      return;
    }
    Role role = new Role();
    role.setRoleName(roleName);
    roleRepository.save(role);
    log.info("Role created: [{}]", roleName.name());
  }
}
