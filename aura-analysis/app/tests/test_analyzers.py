from app.services.analyzers.rubert_embeddings_analyzer import RubertEmbeddingsAnalyzer
from app.services.analyzers.rule_based_analyzer import RuleBasedAnalyzer
from app.services.keyword_service import KeywordService
from app.services.preprocess_service import PreprocessService
from app.tests.conftest import FakeRubertEmbedder


def test_rule_based_analyzer_handles_negative_prefix_without_false_positive() -> None:
    preprocess_service = PreprocessService()
    analyzer = RuleBasedAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
    )

    result = analyzer.analyze("Это нехорошо организованный процесс")

    assert result.sentiment.value != "POSITIVE"
    assert 1 <= len(result.keywords) <= 5
    assert 0.0 <= result.confidence <= 1.0


def test_rubert_analyzer_loads_from_artifacts(rubert_artifacts_dir) -> None:
    preprocess_service = PreprocessService()
    analyzer = RubertEmbeddingsAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
        model_name="cointegrated/rubert-tiny2",
        sentiment_classifier_path=str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
        topic_classifier_path=str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
        metadata_path=str(rubert_artifacts_dir / "rubert_metadata.json"),
        embedder=FakeRubertEmbedder(model_name="cointegrated/rubert-tiny2"),
    )

    result = analyzer.analyze("Очень хорошие преподаватели и интересные лекции")

    assert analyzer.mode == "RUBERT_EMBEDDINGS"
    assert analyzer.model_version == "rubert-embeddings-test"
    assert result.modelVersion == "rubert-embeddings-test"
    assert result.sentiment.value == "POSITIVE"
    assert result.topic.value == "TEACHERS"
    assert 1 <= len(result.keywords) <= 5
    assert 0.0 <= result.confidence <= 1.0


def test_rubert_keywords_filter_out_russian_stopwords(rubert_artifacts_dir) -> None:
    preprocess_service = PreprocessService()
    analyzer = RubertEmbeddingsAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
        model_name="cointegrated/rubert-tiny2",
        sentiment_classifier_path=str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
        topic_classifier_path=str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
        metadata_path=str(rubert_artifacts_dir / "rubert_metadata.json"),
        embedder=FakeRubertEmbedder(model_name="cointegrated/rubert-tiny2"),
    )

    result = analyzer.analyze(
        "В общежитии грязно и шумно, но на кухне есть место, и в корпус идти недалеко."
    )

    forbidden = {"на", "за", "не", "то", "и", "в"}
    assert forbidden.isdisjoint(set(result.keywords))


def test_keyword_service_extracts_keyphrases_without_embedder() -> None:
    preprocess_service = PreprocessService()
    keyword_service = KeywordService(preprocess_service)

    keywords = keyword_service.extract_keywords(
        "Преподаватели дают обратная связь, а учебный процесс построен понятно.",
        topic=None,
    )

    assert any(" " in keyword for keyword in keywords)
    assert any(keyword in keywords for keyword in {"обратная связь", "учебный процесс"})


def test_rubert_keyword_reranking_prefers_topic_aware_phrases(rubert_artifacts_dir) -> None:
    preprocess_service = PreprocessService()
    analyzer = RubertEmbeddingsAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
        model_name="cointegrated/rubert-tiny2",
        sentiment_classifier_path=str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
        topic_classifier_path=str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
        metadata_path=str(rubert_artifacts_dir / "rubert_metadata.json"),
        embedder=FakeRubertEmbedder(model_name="cointegrated/rubert-tiny2"),
    )

    result = analyzer.analyze(
        "Преподаватели дают понятную обратную связь, а критерии оценивания остаются прозрачными."
    )

    assert result.topic.value == "TEACHERS"
    assert any("обрат" in keyword or "оцен" in keyword for keyword in result.keywords)


def test_keyword_service_normalizes_common_word_forms() -> None:
    preprocess_service = PreprocessService()
    keyword_service = KeywordService(preprocess_service)

    keywords = keyword_service.extract_keywords(
        "Преподавателей часто хвалят, а преподаватели помогают после занятий.",
        topic=None,
    )

    assert "преподаватели" in keywords
    assert "преподавателей" not in keywords


def test_keyword_service_filters_lonely_adjectives() -> None:
    preprocess_service = PreprocessService()
    keyword_service = KeywordService(preprocess_service)

    keywords = keyword_service.extract_keywords(
        "Очень жалею, что пошла в этот вуз, хотя кампус очень красивый и современный.",
        topic=None,
    )

    assert "красивый" not in keywords


def test_keyword_service_filters_generic_runtime_tokens() -> None:
    preprocess_service = PreprocessService()
    keyword_service = KeywordService(preprocess_service)

    keywords = keyword_service.extract_keywords(
        "В этом деле учусь уже второй год, но со стипендией и расписанием все равно много путаницы.",
        topic=None,
    )

    assert {"этом", "деле", "учусь", "год"}.isdisjoint(set(keywords))
    assert any(keyword in keywords for keyword in {"стипендия", "расписание"})


def test_rubert_topic_correction_promotes_administration_from_other(rubert_artifacts_dir) -> None:
    preprocess_service = PreprocessService()
    analyzer = RubertEmbeddingsAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
        model_name="cointegrated/rubert-tiny2",
        sentiment_classifier_path=str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
        topic_classifier_path=str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
        metadata_path=str(rubert_artifacts_dir / "rubert_metadata.json"),
        embedder=FakeRubertEmbedder(model_name="cointegrated/rubert-tiny2"),
    )

    result = analyzer.analyze(
        "Со справками и заявлениями тянут до последнего, а сроки подачи постоянно меняются."
    )

    assert result.topic.value == "ADMINISTRATION"
    assert any("справ" in keyword or "срок" in keyword for keyword in result.keywords)


def test_rubert_topic_correction_promotes_dormitory_from_other(rubert_artifacts_dir) -> None:
    preprocess_service = PreprocessService()
    analyzer = RubertEmbeddingsAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=KeywordService(preprocess_service),
        model_name="cointegrated/rubert-tiny2",
        sentiment_classifier_path=str(rubert_artifacts_dir / "rubert_sentiment_classifier.joblib"),
        topic_classifier_path=str(rubert_artifacts_dir / "rubert_topic_classifier.joblib"),
        metadata_path=str(rubert_artifacts_dir / "rubert_metadata.json"),
        embedder=FakeRubertEmbedder(model_name="cointegrated/rubert-tiny2"),
    )

    result = analyzer.analyze(
        "Заселение затянули, на кухне тесно, а соседи шумят почти каждую ночь."
    )

    assert result.topic.value == "DORMITORY"
    assert any("засел" in keyword or "сосед" in keyword or "кух" in keyword for keyword in result.keywords)
