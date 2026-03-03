package com.jobboard.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@test.com");
    }

    @Test
    void sendEmail_success_callsMailSender() {
        // Use a real MimeMessage so MimeMessageHelper can operate on it normally
        MimeMessage realMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(realMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body>Test</body></html>");

        emailService.sendEmail(
                "recipient@test.com",
                "Test Subject",
                "job-created",
                Map.of("title", "Dev Job")
        );

        verify(mailSender).send(realMessage);
    }

    @Test
    void sendEmail_messagingException_logsAndDoesNotThrow() throws MessagingException {
        // Mocked MimeMessage whose setContent() throws MessagingException,
        // causing MimeMessageHelper constructor to fail — caught by EmailService
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");
        doThrow(new MessagingException("setContent failed"))
                .when(mockMessage).setContent(any(Multipart.class));

        // Should not propagate — EmailService catches and logs the MessagingException
        emailService.sendEmail(
                "recipient@test.com",
                "Test Subject",
                "job-created",
                Map.of()
        );

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
