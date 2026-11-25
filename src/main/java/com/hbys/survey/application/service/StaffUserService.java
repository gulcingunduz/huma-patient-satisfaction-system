package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.staff.StaffUserCreateRequestDto;
import com.hbys.survey.application.dto.staff.StaffUserResponseDto;
import com.hbys.survey.application.dto.staff.StaffUserUpdateRequestDto;

import java.util.List;

public interface StaffUserService {

    List<StaffUserResponseDto> listUsers();

    StaffUserResponseDto createUser(StaffUserCreateRequestDto request);

    StaffUserResponseDto updateUser(Long userId, StaffUserUpdateRequestDto request, String actorUsername);

    StaffUserResponseDto setUserActive(Long userId, boolean active, String actorUsername);
}
