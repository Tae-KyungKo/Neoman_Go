package com.neomango.notice.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.comment.repository.CommentRepository;
import com.neomango.notice.entity.Notice;
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
class NoticeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private NoticeRepository noticeRepository;

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
	void getNoticesReturnsActiveNoticesWithoutAuthentication() throws Exception {
		User admin = saveAdmin();
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(get("/api/notices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].id").value(notice.getId()))
			.andExpect(jsonPath("$.data.content[0].title").value("notice"))
			.andExpect(jsonPath("$.data.content[0].authorName").value("관리자"))
			.andExpect(jsonPath("$.data.content[0].createdAt").exists())
			.andExpect(jsonPath("$.data.content[0].content").doesNotExist())
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("adminId"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("email"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("nickname"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("author"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("user"))));
	}

	@Test
	void getNoticeReturnsActiveNoticeWithoutAuthentication() throws Exception {
		User admin = saveAdmin();
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(get("/api/notices/{noticeId}", notice.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(notice.getId()))
			.andExpect(jsonPath("$.data.title").value("notice"))
			.andExpect(jsonPath("$.data.content").value("content"))
			.andExpect(jsonPath("$.data.authorName").value("관리자"))
			.andExpect(jsonPath("$.data.createdAt").exists())
			.andExpect(jsonPath("$.data.updatedAt").exists())
			.andExpect(jsonPath("$.data", not(hasKey("authorId"))))
			.andExpect(jsonPath("$.data", not(hasKey("adminId"))))
			.andExpect(jsonPath("$.data", not(hasKey("email"))))
			.andExpect(jsonPath("$.data", not(hasKey("nickname"))))
			.andExpect(jsonPath("$.data", not(hasKey("author"))))
			.andExpect(jsonPath("$.data", not(hasKey("user"))));
	}

	@Test
	void getNoticesReturnsActiveNoticesForUser() throws Exception {
		User admin = saveAdmin();
		User user = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user"));
		noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(get("/api/notices")
				.header("Authorization", "Bearer " + accessToken(user, UserRole.USER)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void getNoticeReturnsActiveNoticeForUser() throws Exception {
		User admin = saveAdmin();
		User user = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "user@test.com", "encoded-password", "user"));
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));

		mockMvc.perform(get("/api/notices/{noticeId}", notice.getId())
				.header("Authorization", "Bearer " + accessToken(user, UserRole.USER)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(notice.getId()));
	}

	@Test
	void getNoticesAndNoticeReturnActiveNoticesForAdmin() throws Exception {
		User admin = saveAdmin();
		Notice notice = noticeRepository.save(Notice.create(admin, "notice", "content"));
		String accessToken = accessToken(admin, UserRole.ADMIN);

		mockMvc.perform(get("/api/notices")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1));

		mockMvc.perform(get("/api/notices/{noticeId}", notice.getId())
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(notice.getId()));
	}

	@Test
	void getNoticesExcludesDeletedNotices() throws Exception {
		User admin = saveAdmin();
		Notice activeNotice = noticeRepository.save(Notice.create(admin, "active", "content"));
		Notice deletedNotice = noticeRepository.save(Notice.create(admin, "deleted", "content"));
		deletedNotice.softDelete();
		noticeRepository.saveAndFlush(deletedNotice);

		mockMvc.perform(get("/api/notices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(1))
			.andExpect(jsonPath("$.data.content[0].id").value(activeNotice.getId()));
	}

	@Test
	void getNoticeRejectsDeletedNotice() throws Exception {
		User admin = saveAdmin();
		Notice notice = noticeRepository.save(Notice.create(admin, "deleted", "content"));
		notice.softDelete();
		noticeRepository.saveAndFlush(notice);

		mockMvc.perform(get("/api/notices/{noticeId}", notice.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 공지사항입니다."));
	}

	@Test
	void getNoticeRejectsUnknownNotice() throws Exception {
		mockMvc.perform(get("/api/notices/{noticeId}", 999999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("N001"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 공지사항입니다."));
	}

	@Test
	void getNoticesReturnsLatestOrderByCreatedAtDescAndIdDesc() throws Exception {
		User admin = saveAdmin();
		Notice olderNotice = noticeRepository.save(Notice.create(admin, "older", "content"));
		Notice lowerIdNotice = noticeRepository.save(Notice.create(admin, "lower id", "content"));
		Notice higherIdNotice = noticeRepository.save(Notice.create(admin, "higher id", "content"));

		LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
		setCreatedAt(olderNotice, olderCreatedAt);
		setCreatedAt(lowerIdNotice, sameCreatedAt);
		setCreatedAt(higherIdNotice, sameCreatedAt);
		noticeRepository.saveAndFlush(olderNotice);
		noticeRepository.saveAndFlush(lowerIdNotice);
		noticeRepository.saveAndFlush(higherIdNotice);

		mockMvc.perform(get("/api/notices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].id").value(higherIdNotice.getId()))
			.andExpect(jsonPath("$.data.content[1].id").value(lowerIdNotice.getId()))
			.andExpect(jsonPath("$.data.content[2].id").value(olderNotice.getId()));
	}

	private void deleteAll() {
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		noticeRepository.deleteAll();
		userRepository.deleteAll();
	}

	private User saveAdmin() {
		return userRepository.save(UserTestFixture.admin(null, "admin@test.com", "admin"));
	}

	private String accessToken(User user, UserRole role) {
		return jwtTokenProvider.createAccessToken(user.getId(), role);
	}

	private void setCreatedAt(Notice notice, LocalDateTime createdAt) {
		ReflectionTestUtils.setField(notice, "createdAt", createdAt);
	}
}
