package com.neomango.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neomango.audit.entity.AuditAction;
import com.neomango.audit.entity.AuditLog;
import com.neomango.audit.entity.AuditResourceType;
import com.neomango.audit.repository.AuditLogRepository;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.comment.repository.CommentRepository;
import com.neomango.notice.dto.NoticeCreateRequest;
import com.neomango.notice.dto.NoticeUpdateRequest;
import com.neomango.notice.entity.Notice;
import com.neomango.notice.entity.NoticeStatus;
import com.neomango.notice.repository.NoticeRepository;
import com.neomango.post.repository.PostRepository;
import com.neomango.support.UserTestFixture;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminNoticeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private NoticeRepository noticeRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		deleteAll();
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void createNoticeSucceedsWhenAdminIsAuthenticated() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeCreateRequest request = new NoticeCreateRequest("notice", "content");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value("notice"))
			.andExpect(jsonPath("$.data.content").value("content"))
			.andExpect(jsonPath("$.data.authorName").value("관리자"))
			.andExpect(jsonPath("$.data", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data", not(hasKey("adminId"))))
			.andExpect(jsonPath("$.data", not(hasKey("email"))))
			.andExpect(jsonPath("$.data", not(hasKey("nickname"))))
			.andExpect(jsonPath("$.data", not(hasKey("author"))))
			.andExpect(jsonPath("$.data", not(hasKey("user"))));

		Notice notice = noticeRepository.findAll().get(0);
		assertThat(notice.getAuthor().getId()).isEqualTo(admin.getId());
		assertSingleAuditLog(admin, notice.getId(), AuditAction.CREATE);
	}

	@Test
	void createNoticeRejectsUserRole() throws Exception {
		User user = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user"));
		NoticeCreateRequest request = new NoticeCreateRequest("notice", "content");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(user, UserRole.USER))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeRejectsUnauthenticatedRequest() throws Exception {
		NoticeCreateRequest request = new NoticeCreateRequest("notice", "content");

		mockMvc.perform(post("/api/admin/notices")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeIgnoresInjectedAuthorIdentifiers() throws Exception {
		User admin = saveAdmin("admin@test.com");
		User otherAdmin = saveAdmin("other-admin@test.com");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "notice",
						"content": "content",
						"authorId": %d,
						"adminId": %d
					}
					""".formatted(otherAdmin.getId(), otherAdmin.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.authorName").value("관리자"))
			.andExpect(jsonPath("$.data", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data", not(hasKey("adminId"))));

		Notice notice = noticeRepository.findAll().get(0);
		assertThat(notice.getAuthor().getId()).isEqualTo(admin.getId());
		assertSingleAuditLog(admin, notice.getId(), AuditAction.CREATE);
	}

	@Test
	void updateNoticeSucceedsWhenAdminIsAuthenticated() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "old", "old content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(notice.getId()))
			.andExpect(jsonPath("$.data.title").value("updated"))
			.andExpect(jsonPath("$.data.content").value("updated content"))
			.andExpect(jsonPath("$.data.authorName").value("관리자"))
			.andExpect(jsonPath("$.data", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data", not(hasKey("adminId"))))
			.andExpect(jsonPath("$.data", not(hasKey("email"))))
			.andExpect(jsonPath("$.data", not(hasKey("nickname"))))
			.andExpect(jsonPath("$.data", not(hasKey("author"))))
			.andExpect(jsonPath("$.data", not(hasKey("user"))));

		Notice updatedNotice = noticeRepository.findById(notice.getId()).orElseThrow();
		assertThat(updatedNotice.getTitle()).isEqualTo("updated");
		assertThat(updatedNotice.getContent()).isEqualTo("updated content");
		assertSingleAuditLog(admin, notice.getId(), AuditAction.UPDATE);
	}

	@Test
	void updateNoticeRejectsUserRole() throws Exception {
		User admin = saveAdmin("admin@test.com");
		User user = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user"));
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(user, UserRole.USER))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsUnauthenticatedRequest() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeIgnoresInjectedAuthorIdentifiers() throws Exception {
		User admin = saveAdmin("admin@test.com");
		User otherAdmin = saveAdmin("other-admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "old", "old content"));

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(otherAdmin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "updated",
						"content": "updated content",
						"authorId": %d,
						"adminId": %d
					}
					""".formatted(otherAdmin.getId(), otherAdmin.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.authorName").value("관리자"))
			.andExpect(jsonPath("$.data", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data", not(hasKey("adminId"))));

		Notice updatedNotice = noticeRepository.findById(notice.getId()).orElseThrow();
		assertThat(updatedNotice.getAuthor().getId()).isEqualTo(admin.getId());
		assertSingleAuditLog(otherAdmin, notice.getId(), AuditAction.UPDATE);
	}

	@Test
	void deleteNoticeSucceedsWhenAdminIsAuthenticated() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(delete("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN)))
			.andExpect(status().isOk());

		Notice deletedNotice = noticeRepository.findById(notice.getId()).orElseThrow();
		assertThat(deletedNotice.getStatus()).isEqualTo(NoticeStatus.DELETED);
		assertThat(deletedNotice.getDeletedAt()).isNotNull();
		assertSingleAuditLog(admin, notice.getId(), AuditAction.DELETE);

		mockMvc.perform(get("/api/notices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(get("/api/notices/{noticeId}", notice.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"));
	}

	@Test
	void deleteNoticeRejectsUserRole() throws Exception {
		User admin = saveAdmin("admin@test.com");
		User user = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user"));
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(delete("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(user, UserRole.USER)))
			.andExpect(status().isForbidden());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void deleteNoticeRejectsUnauthenticatedRequest() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(delete("/api/admin/notices/{noticeId}", notice.getId()))
			.andExpect(status().isUnauthorized());

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsDeletedNotice() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "deleted", "content"));
		notice.softDelete();
		noticeRepository.saveAndFlush(notice);
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void deleteNoticeRejectsDeletedNotice() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "deleted", "content"));
		notice.softDelete();
		noticeRepository.saveAndFlush(notice);

		mockMvc.perform(delete("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsUnknownNotice() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", 999999L)
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void deleteNoticeRejectsUnknownNotice() throws Exception {
		User admin = saveAdmin("admin@test.com");

		mockMvc.perform(delete("/api/admin/notices/{noticeId}", 999999L)
				.header("Authorization", bearerToken(admin, UserRole.ADMIN)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeRejectsBlankTitle() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeCreateRequest request = new NoticeCreateRequest(" ", "content");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeRejectsTooLongTitle() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeCreateRequest request = new NoticeCreateRequest("a".repeat(101), "content");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeRejectsBlankContent() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeCreateRequest request = new NoticeCreateRequest("title", " ");

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void createNoticeRejectsTooLongContent() throws Exception {
		User admin = saveAdmin("admin@test.com");
		NoticeCreateRequest request = new NoticeCreateRequest("title", "a".repeat(5001));

		mockMvc.perform(post("/api/admin/notices")
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsBlankTitle() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest(" ", "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsTooLongTitle() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("a".repeat(101), "updated content");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsBlankContent() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", " ");

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	@Test
	void updateNoticeRejectsTooLongContent() throws Exception {
		User admin = saveAdmin("admin@test.com");
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		NoticeUpdateRequest request = new NoticeUpdateRequest("updated", "a".repeat(5001));

		mockMvc.perform(patch("/api/admin/notices/{noticeId}", notice.getId())
				.header("Authorization", bearerToken(admin, UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(auditLogRepository.count()).isZero();
	}

	private void deleteAll() {
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		auditLogRepository.deleteAll();
		noticeRepository.deleteAll();
		userRepository.deleteAll();
	}

	private User saveAdmin(String email) {
		return userRepository.save(UserTestFixture.admin(null, email, "admin"));
	}

	private String bearerToken(User user, UserRole role) {
		return "Bearer " + jwtTokenProvider.createAccessToken(user.getId(), role);
	}

	private void assertSingleAuditLog(User admin, Long noticeId, AuditAction action) {
		assertThat(auditLogRepository.findAll())
			.singleElement()
			.satisfies(auditLog -> {
				assertThat(auditLog.getAdminId()).isEqualTo(admin.getId());
				assertThat(auditLog.getResourceType()).isEqualTo(AuditResourceType.NOTICE);
				assertThat(auditLog.getResourceId()).isEqualTo(noticeId);
				assertThat(auditLog.getAction()).isEqualTo(action);
				assertThat(auditLog.getPerformedAt()).isNotNull();
			});
	}
}
