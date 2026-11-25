package com.hbys.survey.infrastructure.security;

import com.hbys.survey.domain.entity.StaffRole;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public BootstrapAdminInitializer(
            StaffUserRepository staffUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${hbys.bootstrap.admin.username:}") String username,
            @Value("${hbys.bootstrap.admin.password:}") String password
    ) {
        this.staffUserRepository = staffUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || staffUserRepository.findByUsername(username).isPresent()) {
            return;
        }
        staffUserRepository.save(new StaffUser(
                username,
                passwordEncoder.encode(password),
                "System Administrator",
                StaffRole.ADMIN
        ));
    }
}