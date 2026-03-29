package org.copperforge.mog.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mogApiFailure_rendersErrorViewWithApiContext() {
        ModelAndView mav = handler.handleMogApi(new MogApiClientException(
                "Failed to fetch list from /api/reports",
                new IllegalStateException("Connection refused"),
                HttpStatus.BAD_GATEWAY,
                "{\"error\":\"downstream unavailable\"}"));

        assertThat(mav.getViewName()).isEqualTo("ui/error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(mav.getModel())
                .containsEntry("errorTitle", "MOG API call failed")
                .containsEntry("errorMessage", "Failed to fetch list from /api/reports")
                .containsEntry("errorDetails", "{\"error\":\"downstream unavailable\"}");
    }

    @Test
    void genericFailure_logsAndRendersErrorView(CapturedOutput output) {
        ModelAndView mav = handler.handleGeneric(new IllegalStateException("boom"));

        assertThat(mav.getViewName()).isEqualTo("ui/error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mav.getModel())
                .containsEntry("errorTitle", "Something went wrong")
                .containsEntry("errorMessage", "boom")
                .containsEntry("errorDetails", "java.lang.IllegalStateException: boom");
        assertThat(output).contains("Unhandled UI exception");
        assertThat(output).contains("java.lang.IllegalStateException: boom");
    }

    @Test
    void missingResource_renders404WithoutErrorNoise(CapturedOutput output) {
        ModelAndView mav = handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "favicon.ico"));

        assertThat(mav.getViewName()).isEqualTo("ui/error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mav.getModel())
                .containsEntry("errorTitle", "Page not found")
                .containsEntry("errorMessage", "The requested page or resource does not exist.");
        assertThat(output).doesNotContain("Unhandled UI exception");
    }
}
