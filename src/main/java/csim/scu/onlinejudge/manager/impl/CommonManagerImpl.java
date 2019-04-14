package csim.scu.onlinejudge.manager.impl;

import csim.scu.onlinejudge.common.exception.EntityNotFoundException;
import csim.scu.onlinejudge.dao.domain.admin.Admin;
import csim.scu.onlinejudge.dao.domain.assistant.Assistant;
import csim.scu.onlinejudge.dao.domain.student.Student;
import csim.scu.onlinejudge.dao.domain.teacher.Teacher;
import csim.scu.onlinejudge.manager.CommonManager;
import csim.scu.onlinejudge.service.AdminService;
import csim.scu.onlinejudge.service.AssistantService;
import csim.scu.onlinejudge.service.StudentService;
import csim.scu.onlinejudge.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonManagerImpl implements CommonManager {

    private StudentService studentService;
    private TeacherService teacherService;
    private AssistantService assistantService;
    private AdminService adminService;

    @Autowired
    public CommonManagerImpl(StudentService studentService,
                             TeacherService teacherService,
                             AssistantService assistantService,
                             AdminService adminService) {
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.assistantService = assistantService;
        this.adminService = adminService;
    }

    // 比對各個User實體的account、password，並回傳User的Authority
    @Override
    public String findUserAuthority(String account, String password) throws EntityNotFoundException {
        boolean existStudent = studentService.existByAccount(account);
        boolean existTeacher = teacherService.existByAccount(account);
        boolean existAssistant = assistantService.existByAccount(account);
        boolean existAdmin = adminService.existByAccount(account);

        String authority = "";
        if (existStudent) {
            Student student = studentService.findByAccount(account);
            if (comparePassword(password, student.getPassword())) {
                authority = "student";
            }
        }
        else if (existTeacher) {
            Teacher teacher = teacherService.findByAccount(account);
            if (comparePassword(password, teacher.getPassword())) {
                authority = "teacher";
            }
        }
        else if (existAssistant) {
            Assistant assistant = assistantService.findByAccount(account);
            if (comparePassword(password, assistant.getPassword())) {
                authority = "assistant";
            }
        }
        else if (existAdmin) {
            Admin admin = adminService.findByAccount(account);
            if (comparePassword(password, admin.getPassword())) {
                authority = "admin";
            }
        }
        return authority;
    }

    private boolean comparePassword(String source, String destination) {
        return source.equals(destination);
    }

    // 比對User的Authority，並更新密碼
    @Transactional
    @Override
    public int updateUserPassword(String account, String oriPassword, String newPassword, String userType) {
        switch (userType) {
            case "student":
                return studentService.updatePasswordByAccount(account,
                        oriPassword, newPassword);
            case "teacher":
                return teacherService.updatePasswordByAccount(account, oriPassword, newPassword);
            case "assistant":
                return assistantService.updatePasswordByAccount(account, oriPassword, newPassword);
            case "admin":
                return adminService.updatePasswordByAccount(account, oriPassword, newPassword);
            default:
                return -1;
        }
    }
}
