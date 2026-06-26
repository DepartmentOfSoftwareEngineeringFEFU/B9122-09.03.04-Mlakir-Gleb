from app.domain.enums import Topic
from app.services.analysis_result import TopicResult
from app.services.topic.base import BaseTopicAnalyzer


class RuleBasedTopicAnalyzer(BaseTopicAnalyzer):
    topic_markers: dict[Topic, tuple[str, ...]] = {
        Topic.TEACHERS: ("преподавател",),
        Topic.EDUCATION: ("лекц", "пара", "семинар", "курс", "предмет", "обучен", "экзамен", "зачет"),
        Topic.DORMITORY: ("общежит", "комната", "сосед", "кухн", "душ", "комендант"),
        Topic.INFRASTRUCTURE: ("кампус", "столов", "аудитор", "корпус", "wifi", "библиотек", "здание"),
        Topic.ADMINISTRATION: ("деканат", "документ", "справк", "заявлен", "бюрократ", "расписан", "оформлен"),
        Topic.STUDENT_LIFE: ("мероприят", "клуб", "студенческ", "активност", "событ", "организац"),
    }
    tie_break_priority: tuple[Topic, ...] = (
        Topic.DORMITORY,
        Topic.ADMINISTRATION,
        Topic.INFRASTRUCTURE,
        Topic.STUDENT_LIFE,
        Topic.TEACHERS,
        Topic.EDUCATION,
    )

    def analyze(self, text: str) -> TopicResult:
        scores = {
            topic: sum(text.count(marker) for marker in markers)
            for topic, markers in self.topic_markers.items()
        }
        top_score = max(scores.values())

        if top_score == 0:
            return TopicResult(topic=Topic.OTHER, confidence=0.55)

        top_topics = [topic for topic, score in scores.items() if score == top_score]
        top_topic = next(
            (topic for topic in self.tie_break_priority if topic in top_topics),
            top_topics[0],
        )
        sorted_scores = sorted(scores.values(), reverse=True)
        runner_up = sorted_scores[1] if len(sorted_scores) > 1 else 0
        confidence = 0.6 + min(0.2, top_score * 0.08) + min(0.1, (top_score - runner_up) * 0.05)
        return TopicResult(topic=top_topic, confidence=round(min(confidence, 0.95), 2))
