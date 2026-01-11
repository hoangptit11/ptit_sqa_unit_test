package ptit.com.enghub.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ptit.com.enghub.dto.UserLearningSettingsDto;
import ptit.com.enghub.dto.request.*;
import ptit.com.enghub.dto.response.ApiResponse;
import ptit.com.enghub.dto.response.UserResponse;
import ptit.com.enghub.dto.response.dashboard.AdminDashboardResponse;
import ptit.com.enghub.dto.response.dashboard.UserDashboardResponse;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.enums.Level;
import ptit.com.enghub.enums.NotificationType;
import ptit.com.enghub.service.NotificationService;
import ptit.com.enghub.service.UserService;
import ptit.com.enghub.service.dashboard.DashboardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    NotificationService noti;
    DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/info")
    public ResponseEntity<UserResponse> getYourInfo(){
        return ResponseEntity.ok(userService.getYourInfo());
    }


    @PostMapping("/update-user")
    public ResponseEntity<UserResponse> updateUser(@RequestBody UserUpdateRequest request){
        return ResponseEntity.ok(userService.updateUser(request));
    }

    @PostMapping("/update-level-placement")
    public ResponseEntity<UserResponse> updateLevel(@RequestBody UserUpdateRequest request){

        User user = userService.getCurrentUser();

        NotificationRequest n = new NotificationRequest();
        n.setUserId(user.getId().toString());
        n.setType(NotificationType.SYSTEM_MESSAGE);
        n.setTitle("Chào mừng bạn đến với EngHub!");
        n.setContent(
                "Hãy bắt đầu hành trình học tiếng Anh với các kỹ năng Listening, Speaking, Reading và Writing.\n\n" +
                        "Chúc bạn học tập hiệu quả và đạt được mục tiêu của mình!"
        );
        noti.create(n);
        return ResponseEntity.ok(userService.updateUser(request));
    }


//    @DeleteMapping("/delete-user")
//    public ResponseEntity<String> deleteUser(){
//        User user = userService.getUser();
//        userService.deleteUser(user.getId());
//        return ResponseEntity.noContent().build();
//    }

    @PatchMapping("/status")
    public ResponseEntity<Void> updateMyStatus(
            @RequestBody UserStatusRequest request
    ) {
        userService.updateUserStatus(request.getStatus());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<Void> updateMyStatus(
            @RequestBody UserStatusRequest request,
            @PathVariable Long id
    ) {
        userService.updateUserStatus(request.getStatus(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/learning-settings")
    public ResponseEntity<UserLearningSettingsDto> updateLearningSettings(
            @RequestBody UserLearningSettingsDto request
    ) {
        return ResponseEntity.ok(userService.updateSettings(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(1000)
                        .message("Đổi mật khẩu thành công")
                        .build()
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<UserDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardUser());

    }

}
