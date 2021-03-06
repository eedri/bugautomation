package org.marsik.bugautomation.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.marsik.bugautomation.services.ConfigurationService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TrelloRefreshJobTest {
    @Mock
    ConfigurationService configurationService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(configurationService.getCached("release.future.prefix")).thenReturn("ovirt-4.1.");
        when(configurationService.getCached("release.future.release")).thenReturn("ovirt-4.1.0");
    }

    @Test
    public void empty() throws Exception {
        String testDoc = "Test description with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void noValues() throws Exception {
        String testDoc = "Test description {{}} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void multipleNoValues() throws Exception {
        String testDoc = "Test description {{}} with {{}} suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void simple() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");
    }

    @Test
    public void simpleIdWithDashes() throws Exception {
        String testDoc = "Test description {{ id:test-with-dashes }} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("id"))
                .isNotNull()
                .isEqualTo("test-with-dashes");
    }

    @Test
    public void simpleColon() throws Exception {
        String testDoc = "Test description {{ score:400 }} with suffix";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");
    }

    @Test
    public void multipleSame() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix {{ score=300 }}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(1);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("300");
    }

    @Test
    public void multipleDiff() throws Exception {
        String testDoc = "Test description {{ score=400 }} with suffix {{ score2:300 }}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(2);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");

        assertThat(values.get("score2"))
                .isNotNull()
                .isEqualTo("300");
    }

    @Test
    public void complex() throws Exception {
        String testDoc = "Test description {{ score=400   test:mail@admin.cz}} with suffix {{score2=300}}";
        Map<String, String> values = TrelloRefreshJob.getCustomFields(testDoc);
        assertThat(values)
                .isNotNull()
                .hasSize(3);

        assertThat(values.get("score"))
                .isNotNull()
                .isEqualTo("400");

        assertThat(values.get("score2"))
                .isNotNull()
                .isEqualTo("300");

        assertThat(values.get("test"))
                .isNotNull()
                .isEqualTo("mail@admin.cz");

    }
}
