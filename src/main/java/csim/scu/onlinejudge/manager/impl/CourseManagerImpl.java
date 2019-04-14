package csim.scu.onlinejudge.manager.impl;

import csim.scu.onlinejudge.common.exception.EntityNotFoundException;
import csim.scu.onlinejudge.dao.domain.assistant.Assistant;
import csim.scu.onlinejudge.dao.domain.course.Course;
import csim.scu.onlinejudge.dao.domain.feedback.Feedback;
import csim.scu.onlinejudge.dao.domain.judge.HistoryCode;
import csim.scu.onlinejudge.dao.domain.judge.Judge;
import csim.scu.onlinejudge.dao.domain.problem.Problem;
import csim.scu.onlinejudge.dao.domain.problem.TestCase;
import csim.scu.onlinejudge.dao.domain.student.BestCodeRank;
import csim.scu.onlinejudge.dao.domain.student.CorrectRank;
import csim.scu.onlinejudge.dao.domain.student.Student;
import csim.scu.onlinejudge.dao.domain.teacher.Teacher;
import csim.scu.onlinejudge.manager.CourseManager;
import csim.scu.onlinejudge.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CourseManagerImpl implements CourseManager {

    private CourseService courseService;
    private TeacherService teacherService;
    private StudentService studentService;
    private AssistantService assistantService;
    private ProblemService problemService;
    private JudgeService judgeService;
    private FeedbackService feedbackService;

    @Autowired
    public CourseManagerImpl(CourseService courseService,
                             TeacherService teacherService,
                             StudentService studentService,
                             AssistantService assistantService,
                             ProblemService problemService,
                             JudgeService judgeService,
                             FeedbackService feedbackService) {
        this.courseService = courseService;
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.assistantService = assistantService;
        this.problemService = problemService;
        this.judgeService = judgeService;
        this.feedbackService = feedbackService;
    }

    // 教授建立課程
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Course createCourse(String account, String courseName, String semester) throws EntityNotFoundException {
        Teacher teacher = teacherService.findByAccount(account);
        Course course = new Course(teacher, courseName, semester, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        return courseService.save(course);
    }

    // 教授刪除課程
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteCourseById(Long id) throws EntityNotFoundException {
        courseService.delete(id);
    }

    // 根據學號列表將一群學生加入課程
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void mapStudentListToCourse(Long courseId, List<String> accounts) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        for (String account : accounts) {
            Student student = studentService.findByAccount(account);
            List<Course> courses = student.getCourses();
            courses.add(course);
            student.setCourses(courses);
            studentService.save(student);
        }
    }

    /*
    根據學號列表，將一群學生退出課程
    須注意:要檢查課程下的所有題目的最佳代碼學號有無在退出學生的名單
    有的話，要找出下一位最佳代碼學號，並進行替換
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteStudentListFromCourse(Long courseId, List<String> accounts) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        List<Problem> problems = problemService.findByCourse(course);
        for (String account : accounts) {
            Student student = studentService.findByAccount(account);
            List<Course> courses = student.getCourses();
            courses.remove(course);
            student.setCourses(courses);
            for (Problem problem : problems) {
                if (problem.getBestStudentAccount().equals(account)) {
                    List<Judge> judges = judgeService.findByProblem(problem);
                    double bestRunTime = Double.MAX_VALUE;
                    String bestAccount = "";
                    for (Judge judge : judges) {
                        // 找出下一位最佳代碼持有者
                        if (!judge.getStudent().getAccount().equals(account)
                                && judge.getHistoryCodes().get(judge.getHistoryCodes().size() - 1).getScore() == 100) {
                            double tempRunTime = judge.getHistoryCodes().get(judge.getHistoryCodes().size() - 1).getRunTime();
                            if (tempRunTime < bestRunTime) {
                                bestRunTime = tempRunTime;
                                bestAccount = judge.getStudent().getAccount();
                            }
                        }
                    }
                    problem.setBestStudentAccount(bestAccount);
                    problemService.save(problem);
                }
            }
            studentService.save(student);
        }
    }

    // 根據學號列表將一群助教加入課程
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void mapAssistantListToCourse(Long courseId, List<String> accounts) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        for (String account : accounts) {
            Assistant assistant = assistantService.findByAccount(account);
            List<Course> courses = assistant.getCourses();
            courses.add(course);
            assistant.setCourses(courses);
            assistantService.save(assistant);
        }
    }

    // 根據學號列表將一群助教退出課程
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAssistantListFromCourse(Long courseId, List<String> accounts) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        for (String account : accounts) {
            Assistant assistant = assistantService.findByAccount(account);
            List<Course> courses = assistant.getCourses();
            courses.remove(course);
            assistant.setCourses(courses);
            assistantService.save(assistant);
        }
    }

    // 教授取得自身創建的所有課程資訊
    @Override
    public List<Map<String, String>> getCoursesInfo(String account) throws EntityNotFoundException {
        return teacherService.getCoursesInfo(account);
    }

    // 在課程中建立題目
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createProblem(Long courseId, String name, String type, String category, String[] tag, String description, String inputDesc, String outputDesc, List<TestCase> testCases, Date deadline) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        Problem problem = new Problem(course, name, type, category, tag, 0,  description, inputDesc, outputDesc, testCases, deadline, 0, 0, 0, "", null, null);
        problemService.save(problem);
    }

    // 利用課程Id找到該課程下的所有題目
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<Problem> findByCourseId(Long courseId) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        List<Problem> problems = problemService.findByCourse(course);
        return problems;
    }

    // 利用課程Id、學生account來找尋所有該學生做過的題目資訊
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, String> findStudentCourseInfo(Long courseId, String account) throws EntityNotFoundException {
        Student student = studentService.findByAccount(account);
        String name = student.getName();
        String studentClass = student.getStudentClass();
        List<Problem> problems = findByCourseId(courseId);
        String undoNum = "";
        String doneNum = "";
        String bestCodeNum = "";
        String correctNum = "";
        String incorrectNum = "";
        int judgeCount = 0;
        int bestCount = 0;
        int correctCount = 0;
        int incorrectCount = 0;
        for (Problem problem : problems) {
            judgeCount += judgeService.countByProblemAndStudent(problem, student);
            if (problem.getBestStudentAccount().equals(account)) {
                bestCount++;
            }

            boolean hasJudge = judgeService.existByProblemAndStudent(problem, student);
            if (hasJudge) {
                Judge judge = judgeService.findByProblemAndStudent(problem, student);
                List<HistoryCode> historyCodes = judge.getHistoryCodes();
                if (historyCodes.get(historyCodes.size() - 1).getScore() == 100) {
                    correctCount ++;
                }
                else {
                    incorrectCount++;
                }
            }
        }
        doneNum = String.valueOf(judgeCount);
        undoNum = String.valueOf(problems.size() - judgeCount);
        bestCodeNum = String.valueOf(bestCount);
        correctNum = String.valueOf(correctCount);
        incorrectNum = String.valueOf(incorrectCount);
        Map<String, String> map = new HashMap<>();
        map.put("account", account);
        map.put("name", name);
        map.put("studentClass", studentClass);
        map.put("undoNum", undoNum);
        map.put("doneNum", doneNum);
        map.put("bestCodeNum", bestCodeNum);
        map.put("correctNum", correctNum);
        map.put("incorrectNum", incorrectNum);
        return map;
    }

    // 在該課程下新增學生給予的回饋
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Feedback addFeedback(Long courseId, String account, String content) throws ParseException, EntityNotFoundException {
        Course course = courseService.findById(courseId);
        Student student = studentService.findByAccount(account);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String str = df.format(new Date());
        Date date = df.parse(str);
        Feedback feedback = new Feedback(course, student, date, content);
        feedbackService.save(feedback);
        return feedback;
    }

    // 利用課程Id取得所有該課程下的所有回饋資訊
    @Override
    public List<Feedback> findFeedbacksByCourseId(Long courseId) throws EntityNotFoundException {
        Course course = courseService.findById(courseId);
        return feedbackService.findByCourse(course);
    }

    // 利用學生學號account，取得該學生擁有的課程列表資訊
    @Override
    public List<Map<String, String>> getStudentCoursesInfo(String account) throws EntityNotFoundException {
        List<Course> courses = studentService.findCoursesByAccount(account);
        List<Map<String, String>> results = new ArrayList<>();
        for (Course course : courses) {
            Map<String, String> map = new HashMap<>();
            Teacher teacher = course.getTeacher();
            map.put("courseId", String.valueOf(course.getId()));
            map.put("courseName", course.getName());
            map.put("teacherName", teacher.getName());
            map.put("semester", course.getSemester());
            results.add(map);
        }
        return results;
    }

    // 取得目前所有課程列表資訊
    @Override
    public List<Map<String, String>> getCoursesInfo() {
        List<Course> courses = courseService.findAll();
        List<Map<String, String>> results = new ArrayList<>();

        for (Course course : courses) {
            Map<String, String> map = new HashMap<>();
            Teacher teacher = course.getTeacher();
            map.put("courseId", String.valueOf(course.getId()));
            map.put("courseName", course.getName());
            map.put("teacherName", teacher.getName());
            map.put("semester", course.getSemester());
            results.add(map);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getStudentHistoryScoreInfo(Long courseId, String account) throws EntityNotFoundException {
        return null;
    }

    @Override
    public List<CorrectRank> getCorrectRank(Long courseId) throws EntityNotFoundException {
        return null;
    }

    @Override
    public List<BestCodeRank> getBestCodeRank(Long courseId) throws EntityNotFoundException {
        return null;
    }
}