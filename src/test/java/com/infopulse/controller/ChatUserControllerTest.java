package com.infopulse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infopulse.Main;
import com.infopulse.dto.ChatUserDto;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql("/test-data.sql")
public class ChatUserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static final String testTenant = "testTenant1";

    public static final KeycloakSecurityContext SECURITY_CONTEXT_WILCO =
            new KeycloakSecurityContext(StringUtils.EMPTY, createAccessToken(testTenant) ,StringUtils.EMPTY, null);

    private static AccessToken createAccessToken(String tenantId){
        AccessToken accessToken =new AccessToken();
        accessToken.issuer(tenantId);
        accessToken.expiration(1590326346);
        Map<String, AccessToken.Access> resourceAccess = new HashMap<>();
        AccessToken.Access access = new AccessToken.Access();
        access.addRole("ROLE_ADMIN");
        resourceAccess.put("chatservice", access);
        accessToken.setResourceAccess(resourceAccess);
        return accessToken;
    }

    @Test
    public void saveUserTest() throws Exception {
        ChatUserDto chatUserDto = new ChatUserDto();
        chatUserDto.setName("testuser");
        chatUserDto.setLogin("chatuserLogin");
        chatUserDto.setPassword("password");
        String url = "/registration";
        ObjectMapper mapper = new ObjectMapper();
        MockHttpServletRequestBuilder msb = post(url)
                .content(mapper.writeValueAsString(chatUserDto))
                .contentType(MediaType.APPLICATION_JSON);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/"))
                .build();

        ResultActions resultActions = mvc.perform(msb);
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void saveUserTest_DuplicateLogin() throws Exception {
        ChatUserDto chatUserDto = new ChatUserDto();
        chatUserDto.setName("testuser");
        chatUserDto.setLogin("qqq");
        chatUserDto.setPassword("password");
        String url = "/registration";
        ObjectMapper mapper = new ObjectMapper();
        MockHttpServletRequestBuilder msb = post(url)
                .content(mapper.writeValueAsString(chatUserDto))
                .contentType(MediaType.APPLICATION_JSON);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/"))
                .build();

        ResultActions resultActions = mvc.perform(msb);
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void getAllUsersExceptAdminsTest() throws Exception {
        ChatUserDto chatUserDto = new ChatUserDto();
        chatUserDto.setName("testuser11");
        chatUserDto.setLogin("chatuserLogin11");
        chatUserDto.setPassword("password11");
        String url = "/registration";
        ObjectMapper mapper = new ObjectMapper();
        MockHttpServletRequestBuilder msb = post(url)
                .content(mapper.writeValueAsString(chatUserDto))
                .contentType(MediaType.APPLICATION_JSON);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/"))
                .build();

        ResultActions resultActions = mvc.perform(msb);
        resultActions.andExpect(status().isOk());

        String urlGet = "/users";

       mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/")
                        .requestAttr(KeycloakSecurityContext.class.getName(), SECURITY_CONTEXT_WILCO))
                .build();

       resultActions = mvc.perform(get(urlGet));

        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.[0].name", is("qqq")))
                .andExpect(jsonPath("$.[0].login", is("qqq")))
                .andExpect(jsonPath("$.[1].name", is("testuser11")))
                .andExpect(jsonPath("$.[1].login", is("chatuserLogin11")))
                .andExpect(jsonPath("$", hasSize(2)));
    }


}
