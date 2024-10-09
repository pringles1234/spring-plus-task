package org.example.expert.domain.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Test
    void todo_단건_조회에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        String title = "title";
        String nickname = "star";
        AuthUser authUser = new AuthUser(1L, "email", UserRole.ROLE_USER, nickname);
        User user = User.fromAuthUser(authUser);
        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail());
        TodoResponse response = new TodoResponse(
            todoId,
            title,
            "contents",
            "Sunny",
            userResponse,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // when
        when(todoService.getTodo(todoId)).thenReturn(response);

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(todoId))
            .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long todoId = 1L;

        // when
        when(todoService.getTodo(todoId))
            .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
            .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value("Todo not found"));
    }

    @Test
    void todo_다건_조회_시_weather_조건으로_검색_테스트() throws Exception {
        //given
        List<TodoResponse> todos = new ArrayList<>();
        todos.add(
            new TodoResponse(1L, "Title", "contents", "sunny",
                new UserResponse(1L, "user@email.com"),
                LocalDateTime.now(), LocalDateTime.now()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<TodoResponse> mockResponse = new PageImpl<>(todos, pageable, todos.size());

        when(todoService.getTodos(anyInt(), anyInt(), eq("sunny"), any(LocalDateTime.class),
            any(LocalDateTime.class)))
            .thenReturn(mockResponse);

        //테스트용 날짜 설정
        String startDate = "2024-10-01T12:00:00";
        String endDate = "2024-10-03T17:00:00";

        //when & then
        mockMvc.perform(get("/todos")
                .param("page", "1")
                .param("size", "10")
                .param("weather", "sunny")
                .param("startDate", startDate)
                .param("endDate", endDate))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content[0].title").value("Title"))
            .andExpect(jsonPath("$.content[0].weather").value("sunny"));
    }

}
