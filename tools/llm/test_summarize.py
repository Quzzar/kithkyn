"""Check replay scoring boundaries that could otherwise inflate model results."""
from __future__ import annotations

import unittest

from summarize import legal_inventory, reply, summarize


class ReplayScoringTest(unittest.TestCase):
    def test_missing_dialogue_and_removed_actions_are_not_usable(self) -> None:
        for raw in [
            '{"action":"follow"}', '{"say":"", "action":"follow"}',
            '{"say":"Yes", "action":"teleport"}', '{"say":"Yes", "action":null}',
            '{"say":"Yes", "action":"follow"}',
            '{"say":"Yes", "action":"follow", "action":"wait"}',
            '{"say":"Yes"} trailing', '{"say":"Yes", "x":NaN}',
        ]:
            self.assertIsNone(reply(raw), raw)

    def test_ordinary_dialogue_remains_valid(self) -> None:
        self.assertEqual({"say": "I need to stay here."}, reply('{"say":"I need to stay here."}'))

    def test_minecraft_default_namespace_is_accepted(self) -> None:
        self.assertTrue(legal_inventory({"give": "bread", "give_count": 1}))
        self.assertTrue(legal_inventory({"give": "minecraft:bread", "give_count": 3}))
        self.assertTrue(legal_inventory({"give": "bread", "give_count": 1.0}))

    def test_impossible_inventory_claims_never_pass(self) -> None:
        for value in [
            {"give": "minecraft:diamond"}, {"give": "bread", "give_count": 100},
            {"give": "bread", "give_count": -1}, {"give": "bread", "give_count": 1.5},
            {"give": "bread", "give_count": True}, {"give": "bread", "give_count": "1"},
        ]:
            self.assertFalse(legal_inventory(value), value)

    def test_empty_response_counts_as_failure_without_requiring_model_output_labels(self) -> None:
        corpus = {"scenarios": [{"id": "bread", "category": "item"}], "conversation": []}
        report = {"candidate": "fixture", "status": "complete", "chat_format": "llama3", "repeats": 1,
                  "samples": [{"id": "bread", "category": "item", "repeat": 0,
                               "response_present": False, "latency_ms": 45000, "rss_kib": None}]}
        result = summarize(report, corpus)
        self.assertEqual({"passed": 0, "total": 1}, result["item_claims_legal"])
        self.assertEqual({"passed": 0, "total": 1}, result["strict_chat_object"])


if __name__ == "__main__":
    unittest.main()
