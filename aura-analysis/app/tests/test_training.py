import sys
from pathlib import Path

from app.ml.training import train_rubert
from app.tests.conftest import FakeRubertEmbedder


def test_train_rubert_script_creates_artifacts(monkeypatch, sample_dataset_path, tmp_path: Path) -> None:
    artifacts_dir = tmp_path / "rubert_artifacts"
    reports_dir = tmp_path / "rubert_reports"

    monkeypatch.setattr(train_rubert, "RubertEmbedder", FakeRubertEmbedder)
    monkeypatch.setattr(
        sys,
        "argv",
        [
            "train_rubert",
            "--dataset",
            str(sample_dataset_path),
            "--output",
            str(artifacts_dir),
            "--reports-dir",
            str(reports_dir),
            "--model-name",
            "cointegrated/rubert-tiny2",
        ],
    )

    train_rubert.main()

    expected_files = {
        "rubert_sentiment_classifier.joblib",
        "rubert_topic_classifier.joblib",
        "rubert_metadata.json",
    }
    assert expected_files.issubset({path.name for path in artifacts_dir.iterdir()})
    assert (reports_dir / "rubert_sentiment_report.json").exists()
    assert (reports_dir / "rubert_topic_report.json").exists()
