package integration.repository;

import com.practiq.domain.types.QuestionStatus;
import com.practiq.domain.projection.QuestionConceptLink;
import com.practiq.repository.QuestionConceptRepository;
import utils.IntegrationTest;
import utils.data.QuestionTestData;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@IntegrationTest
class QuestionConceptRepositoryIT {

    @Inject
    private QuestionTestData data;

    @Inject
    private QuestionConceptRepository questionConceptRepository;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    void findIdsByQuestionIdsReturnsTheConceptPairsForOnlyTheGivenQuestions() {
        long conceptA = 100L;
        long conceptB = 101L;
        data.concept(conceptA).insert();
        data.concept(conceptB).insert();

        long multiLinked = 1L;
        long singleLinked = 2L;
        long unlinked = 3L;
        long outsideTheSet = 4L;
        data.question(multiLinked).status(QuestionStatus.APPROVED).insert();
        data.question(singleLinked).status(QuestionStatus.APPROVED).insert();
        data.question(unlinked).status(QuestionStatus.APPROVED).insert();
        data.question(outsideTheSet).status(QuestionStatus.APPROVED).insert();
        data.link(multiLinked, conceptA).insert();
        data.link(multiLinked, conceptB).insert();
        data.link(singleLinked, conceptA).insert();
        data.link(outsideTheSet, conceptB).insert();

        List<QuestionConceptLink> links = questionConceptRepository.findLinksByQuestionIds(
                Set.of(multiLinked, singleLinked, unlinked));

        // Exactly the pairs for the requested questions: both of the multi-linked question's concepts and
        // the single-linked one's. The unlinked question contributes nothing, and outsideTheSet's link is
        // excluded because its question id isn't in the set — proving the IN parameter actually binds.
        assertThat(links, containsInAnyOrder(
                new QuestionConceptLink(multiLinked, conceptA),
                new QuestionConceptLink(multiLinked, conceptB),
                new QuestionConceptLink(singleLinked, conceptA)
        ));
    }

    @Test
    void findIdsByQuestionIdsReturnsEmptyWhenNoneOfTheQuestionsAreLinked() {
        long unlinkedOne = 1L;
        long unlinkedTwo = 2L;
        data.question(unlinkedOne).status(QuestionStatus.APPROVED).insert();
        data.question(unlinkedTwo).status(QuestionStatus.APPROVED).insert();

        List<QuestionConceptLink> links = questionConceptRepository.findLinksByQuestionIds(
                Set.of(unlinkedOne, unlinkedTwo));

        assertThat(links, empty());
    }
}
