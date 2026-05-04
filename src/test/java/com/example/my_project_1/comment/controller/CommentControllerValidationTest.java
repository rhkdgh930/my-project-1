package com.example.my_project_1.comment.controller;

import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CommentCommandService commentCommandService = mock(CommentCommandService.class);
        CommentQueryService commentQueryService = mock(CommentQueryService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentCommandService, commentQueryService))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("댓글 작성 요청 body validation을 적용한다.")
    void write_validatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대댓글 작성 요청 body validation을 적용한다.")
    void reply_validatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments/100/replies")
                        .contentType("application/json")
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
