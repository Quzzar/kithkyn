"""Check replay scoring boundaries that could otherwise inflate model results."""
from __future__ import annotations

import unittest

from summarize import legal_inventory, reply


class ReplayScoringTest(unittest.TestCase):
    def test_missing_dialogue_and_ambiguous_actions_are_not_usable(self) -> None:
        for raw in [
            '{"action":"follow"}', '{"say":"", "action":"follow"}',
            '{"say":"Yes", "action":"teleport"}', '{"say":"Yes", "action":null}',
            '{"say":"Yes", "action":"follow", "action":"wait"}',
            '{"say":"Yes"} trailing', '{"say":"Yes", "x":NaN}',
        ]:
            self.assertIsNone(reply(raw), raw)

    def test_refusal_is_valid_dialogue_without_a_selected_action(self) -> None:
        self.assertEqual({"say": "I need to stay here."}, reply('{"say":"I need to stay here."}'))

    def test_minecraft_default_namespace_is_accepted(self) -> None:
        self.assertTrue(legal_inventory({"give": "bread", "give_count": 1}))
        self.assertTrue(legal_inventory({"give": "minecraft:bread", "give_count": 3}))

    def test_impossible_inventory_claims_never_pass(self) -> None:
        for value in [
            {"give": "minecraft:diamond"}, {"give": "bread", "give_count": 100},
            {"give": "bread", "give_count": -1}, {"give": "bread", "give_count": 1.5},
            {"give": "bread", "give_count": True}, {"give": "bread", "give_count": "1"},
        ]:
            self.assertFalse(legal_inventory(value), value)


if __name__ == "__main__":
    unittest.main()
