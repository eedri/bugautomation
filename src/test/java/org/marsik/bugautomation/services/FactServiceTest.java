package org.marsik.bugautomation.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.marsik.bugautomation.cdi.WeldJUnit4Runner;
import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.BugzillaBug;
import org.marsik.bugautomation.facts.BugzillaBugFlag;
import org.marsik.bugautomation.facts.BugzillaPriorityLevel;
import org.marsik.bugautomation.facts.TrelloBoard;
import org.marsik.bugautomation.facts.TrelloCard;
import org.marsik.bugautomation.stats.Stats;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(WeldJUnit4Runner.class)
public class FactServiceTest {
    @Inject
    @KSession("bug-rules")
    KieSession kSession;

    @Mock
    BugzillaActions bugzillaActions;

    @Mock
    TrelloActions trelloActions;

    @Mock
    ConfigurationService configurationService;

    @Inject
    InternalActions internalActions;

    @Inject
    FactService factService;

    private static final String TRELLO_BOARD = "Sprint";
    private static final String TRELLO_BACKLOG = "todo";

    private TrelloBoard board;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(configurationService.getCached("cfg.board.sprint")).thenReturn(TRELLO_BOARD);
        when(configurationService.getCached("cfg.backlog")).thenReturn(TRELLO_BACKLOG);

        board = TrelloBoard.builder()
                .id("sprint")
                .name(TRELLO_BOARD)
                .build();

        // Clear session and populate with board fact
        factService.clear();
        factService.addFact(board);
    }

    private void trigger() {
        kSession.setGlobal("internal", internalActions);
        kSession.setGlobal("bugzilla", bugzillaActions);
        kSession.setGlobal("trello", trelloActions);
        kSession.setGlobal("config", configurationService);
        kSession.insert(new Stats());
        kSession.fireAllRules();
    }

    @Test
    public void testOrderWithAndWithoutRelease() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .bug(Bug.builder().id(1).build())
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .build();

        BugzillaBug bug2 = BugzillaBug.builder()
                .id("2")
                .targetMilestone("ovirt-4.0.6")
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .bug(Bug.builder().id(2).build())
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug2.getBug())
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        verify(trelloActions).switchCards(card1, card2);
    }

    @Test
    public void testDoneBugNoFlags() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .bug(Bug.builder().id(1).build())
                .status("modified")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, "documentation");
    }

    @Test
    public void testDoneBugNoDocFlag() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .bug(Bug.builder().id(1).build())
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .status("modified")
                .flags(Collections.singleton(new BugzillaBugFlag("requires_doc_text-")))
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).moveCard(card1, board, "done");
    }

    @Test
    public void testNeedsTriage() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .bug(Bug.builder().id(1).build())
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .status("modified")
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .build();


        factService.addFact(bug1);
        factService.addFact(card1);

        trigger();

        verify(trelloActions).assignLabelToCard(card1, "triage");
    }

    @Test
    public void testOrderWithBlocking() throws Exception {
        BugzillaBug bug1 = BugzillaBug.builder()
                .id("1")
                .targetMilestone(null)
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .blocks(new HashSet<>(Collections.singletonList("2")))
                .bug(Bug.builder().id(1).build())
                .build();

        BugzillaBug bug2 = BugzillaBug.builder()
                .id("2")
                .priority(BugzillaPriorityLevel.UNSPECIFIED)
                .severity(BugzillaPriorityLevel.UNSPECIFIED)
                .targetMilestone("ovirt-4.0.6")
                .bug(Bug.builder().id(2).build())
                .build();

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .bug(bug1.getBug())
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug2.getBug())
                .score(200)
                .build();

        factService.addFact(bug1);
        factService.addFact(bug2);
        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
    }

    @Test
    public void testOrderWithBlockingCard() throws Exception {
        Bug bug = new Bug(1);

        TrelloCard card1 = TrelloCard.builder()
                .id("a")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(1.0)
                .blocks(new HashSet<>(Collections.singletonList(bug)))
                .build();

        TrelloCard card2 = TrelloCard.builder()
                .id("b")
                .board(board)
                .status(TRELLO_BACKLOG)
                .pos(2.0)
                .bug(bug)
                .score(200)
                .build();

        factService.addFact(card1);
        factService.addFact(card2);

        trigger();

        assertThat(card2.getScore())
                .isNotNull()
                .isEqualTo(card1.getScore());
    }
}
